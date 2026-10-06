package com.alpha.batch.job;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.io.FilenameUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.mybatis.spring.batch.builder.MyBatisPagingItemReaderBuilder;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.alpha.base.BaseUtil;
import com.alpha.base.support.util.SftpUtilPack.Channel;
import com.alpha.batch.config.AbstractJobConfig;
import com.alpha.batch.config.BatchProperties.BatchJobProperties;
import com.alpha.batch.domain.sample.board.BoardServicePack.BoardService;
import com.alpha.batch.domain.sample.board.vo.BoardVo;
import com.alpha.batch.domain.sample.score.ScoreServicePack.ScoreService;
import com.alpha.batch.domain.sample.score.vo.ScoreVo;
import com.alpha.batch.meta.AbstractSchedulerMetaTaskInfoPack.ScheduleType;
import com.alpha.batch.meta.SchedulerMetaPack.SchedulerMetaInfo;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.sftp.FileAttributes;

@Slf4j
@Configuration("sample2JobConfig")
@ConditionalOnExpression("#{'${spring.batch.job.names}'.contains('sample2Job')}")
@SchedulerMetaInfo(jobName = "sample2Job", description = "파일읽음 -> 처리(이름마스킹,카운팅) -> DB저장, DB읽음 -> 파일저장", scheduleType = ScheduleType.FIXED_RATE, scheduleValue = "5000")
public class Sample2JobConfig extends AbstractJobConfig {

	private final long defaultChunk = 100;

	private final String workFileName = "sample2Job-workFile";

	private final String sendFileName = "sample2Job-sendFile";

	@Value("${alpha.batch.fileBase}")
	private String batchFileBase;

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Autowired
	private BatchJobProperties batchJobProperties;

	@Autowired
	private BoardService boardService;

	@Autowired
	private ScoreService scoreService;

	@Autowired
	@Qualifier("board.txManager")
	private PlatformTransactionManager boardTxManager;

	@Autowired
	@Qualifier("score.txManager")
	private PlatformTransactionManager scoreTxManager;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String getWorkFilePath() {
		return this.getDiscriminatePath(this.batchFileBase, this.workFileName);
	}

	private String getSendFilePath() {
		return this.getDiscriminatePath(this.batchFileBase, this.sendFileName);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Spel bean ref.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Bean(name = "sample2JobDefaultChunk")
	Long sample2JobDefaultChunk() {
		return this.defaultChunk;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// job
	// sample2Step1 : 다운로드(RestAPI) -> 파일, RestAPI(http://[server]/xtra/file/v1/fileDownload/recent)를 호출하여 최근에 생성된 파일 다운로드
	// sample2Step2 : DB데이타 삭제
	// sample2Step3 : 파일읽음 -> 처리(이름마스킹,카운팅) -> DB저장, jobParameters인 chunk 단위로 처리
	// sample2Step4 : DB읽음 -> 파일저장, jobParameters인 chunk 단위로 처리
	// sample2Step5 : 파일 -> SFTP, 생성된 파일을 SFTP에 PUT
	// sample2Setp9 : step1 실패시 처리할 step (예제는 별다른 처리 없이 로그만 출력)
	//
	// 로컬 : [server] = 127.0.0.1:9970
	// 개발 : [server] = dev.apollo.co.kr
	//
	// 스프링배치는 동일 JobParameter에 대한 실행 이력이 있으면 실행하지 않느다.(중복 데이터가 쌓일 수가 있기 때문에)
	// 만약, 동일 JobParameter로 계속 실행을 해야 할 경우 incrementer에 new RunIdIncrementer()를 적용한다.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Bean(name = "sample2Job")
	Job sample2Job(
			@Qualifier("sample2Step1") Step step1,
			@Qualifier("sample2Step2") Step step2,
			@Qualifier("sample2Step3") Step step3,
			@Qualifier("sample2Step4") Step step4,
			@Qualifier("sample2Step5") Step step5,
			@Qualifier("sample2Step9") Step step9) {
		return jobBuilderFactory.get("sample2Job")
				.incrementer(new AidRunIdIncrementer()) // 임의의 파라미터를 추가로 사용해 매번 run.id 값을 변경.
				.listener(jobExecutionListener())
				.start(step1).on(BatchStatus.COMPLETED.toString())
					.to(step2).next(step3).next(step4).next(step5)
				.from(step1).on(BatchStatus.FAILED.toString())
					.to(step9).end()
				.build();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// step
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Bean(name = "sample2Step1")
	@JobScope
	Step sample2Step1() {
		// 다운로드(RestAPI) -> 파일,
		// RestAPI(http://[server]/xtra/file/v1/fileDownload/recent)를 호출하여 최근에 생성된 파일
		// 다운로드
		return this.stepBuilderFactory.get("sample2Step1").listener(this.stepExecutionListener())
				.tasklet(new Tasklet() {

					@Override
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

						String param = batchJobProperties.getSample2Job().getDownloadApiParam();
						Map<String, String> paramMap = BaseUtil.getObjectMapperUtil().readValue(param,new TypeReference<Map<String, String>>() {});
						MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
						paramMap.forEach((k, v) -> multiValueMap.add(k, v));

						URI apiUri = UriComponentsBuilder
								.fromUriString(batchJobProperties.getSample2Job().getDownloadApi())
								.queryParams(multiValueMap).encode().build().toUri();

						File file = BaseUtil.getRestTemplateUtil().execute(apiUri, HttpMethod.GET, null, clientHttpResponse -> {
									File _file = new File(FilenameUtils.normalize(getWorkFilePath()));
									StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(_file));
									return _file;
								});
						log.debug(">> downloadFile: {}", file);

						return RepeatStatus.FINISHED;
					}
				})
				.build();
	}

	@Bean(name = "sample2Step2")
	@JobScope
	Step sample2Step2() {
		// DB데이타 삭제
		PlatformTransactionManager chainedTxManager = this.getChainedTransactionManager(boardTxManager, scoreTxManager);
		return this.stepBuilderFactory.get("sample2Step2").listener(this.stepExecutionListener())
				.tasklet(this.deleteDataTasklet())
				.transactionManager(chainedTxManager)
				.build();
	}

	@Bean(name = "sample2Step3")
	@JobScope
	Step sample2Step3(
			@Value("#{jobParameters[chunk]==null?sample2JobDefaultChunk:jobParameters[chunk]}") String chunk,
			@Qualifier("sample2FileReader") FlatFileItemReader<DataEncVo> fileReader,
			@Qualifier("sample2JdbcWriter") JdbcBatchItemWriter<BoardVo> jdbcWriter,
			@Qualifier("sample2MyBatisWriter") MyBatisBatchItemWriter<BoardVo> myBatisWriter,
			@Qualifier("board.txManager") PlatformTransactionManager txManager) {

		// 파일읽음 -> 처리(이름 마스킹) -> DB저장, jobParameters인 chunk 단위로 처리
		return stepBuilderFactory.get("sample2Step3").listener(this.stepExecutionListener())
				.<DataEncVo, BoardVo>chunk(Integer.parseInt(chunk)).reader(fileReader).processor(sample2Processor())
				.writer(jdbcWriter) // .writer(myBatisWriter)
				.transactionManager(txManager)
				.build();
	}

	@Bean(name = "sample2Step4")
	@JobScope
	Step sample2Step4(
			@Value("#{jobParameters[chunk]==null?sample2JobDefaultChunk:jobParameters[chunk]}") String chunk,
			@Qualifier("sample2JdbcReader") JdbcPagingItemReader<BoardVo> jdbcReader,
			@Qualifier("sample2MyBatisReader") MyBatisPagingItemReader<BoardVo> myBatisReader,
			@Qualifier("sample2FileWriter") FlatFileItemWriter<BoardVo> fileWriter) {

		// DB읽음 -> 파일저장, jobParameters인 chunk 단위로 처리
		return stepBuilderFactory.get("sample2Step4").listener(this.stepExecutionListener())
				.<BoardVo, BoardVo>chunk(Integer.parseInt(chunk)) // .reader(jdbcReader)
				.reader(myBatisReader).writer(fileWriter)
				.build();
	}

	@Bean(name = "sample2Step5")
	@JobScope
	Step sample2Step5() {

		// 파일 -> SFTP, 생성된 파일을 SFTP에 PUT
		return this.stepBuilderFactory.get("sample2Step5").listener(this.stepExecutionListener())
				.tasklet(new Tasklet() {
					@Override
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
						String remoteHost = batchJobProperties.getSample2Job().getRemoteHost();
						String username = batchJobProperties.getSample2Job().getUsername();
						String keyPath = batchJobProperties.getSample2Job().getKeyPath();
						String remotePath = batchJobProperties.getSample2Job().getRemotePath();

						Channel channel = BaseUtil.getSftpUtil().build(remoteHost, username,
								new File(FilenameUtils.normalize(keyPath)));
						FileAttributes result = channel.stage((sftp, util) -> {
							String _source = getSendFilePath();
							String _remotePath = remotePath;

							// if(util.isDirectory(_remotePath)) {
							// String _fileName = FilenameUtils.getName(_source);
							// _remotePath=_remotePath+"/"+_fileName+"-"+BaseUtil.getTimeUtil().getCurrentTime();
							// _remotePath=_remotePath.replace("//","/");
							// }
							log.debug(">> remotePath: {}", _remotePath);

							sftp.put(_source, _remotePath);
							FileAttributes attributes = sftp.lstat(_remotePath);

							return attributes;
						}, FileAttributes.class);

						log.info(">> result:{}", result);

						return RepeatStatus.FINISHED;
					}
				}).build();
	}

	@Bean(name = "sample2Step9")
	@JobScope
	Step sample2Step9() {
		// step1 실패시 처리할 step (예제는 별다른 처리 없이 로그만 출력)
		return this.stepBuilderFactory.get("sample2Step9").listener(this.stepExecutionListener())
				.tasklet(new Tasklet() {
					@Override
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
						log.info(">> execute:{}", chunkContext.getStepContext().getStepName());
						log.info(">> sample2Step1 수행중 오류");
						return RepeatStatus.FINISHED;
					}
				}).build();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// ItemReader, ItemProcessor, ItemWriter
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Bean(name = "sample2FileReader")
	@StepScope
	FlatFileItemReader<DataEncVo> sample2FileReader() {
		return new FlatFileItemReaderBuilder<DataEncVo>().name("sample2FileReader")
				.resource(new FileSystemResource(this.getWorkFilePath())).delimited().delimiter(",").names("enc")
				.targetType(DataEncVo.class).build();
	}

	@Bean(name = "sample2JdbcReader")
	@StepScope
	JdbcPagingItemReader<BoardVo> sample2JdbcReader(
			@Value("#{jobParameters[chunk]==null?sample2JobDefaultChunk:jobParameters[chunk]}") String chunk,
			@Qualifier("board.dataSource") DataSource dataSource,
			@Qualifier("board.pagingQueryProvider") PagingQueryProvider queryProvider) {

		Map<String, Object> parameterValues = new HashMap<>();
		parameterValues.put("index", 0);

		return new JdbcPagingItemReaderBuilder<BoardVo>().name("sample2JdbcReader").pageSize(Integer.parseInt(chunk)) 
				// Paging은 실제 쿼리를 limit, offset을 이용해서 분할 처리, SQL에 offset과 limit을 자동 처리 함.
				.fetchSize(Integer.parseInt(chunk)) // Database에서 한번에 가져올 데이터 size
				.dataSource(dataSource).queryProvider(queryProvider).parameterValues(parameterValues)
				// .rowMapper(new BeanPropertyRowMapper<>(BoardVo.class))
				.rowMapper(new RowMapper<BoardVo>() {
					@Override
					public BoardVo mapRow(ResultSet rs, int rowNum) throws SQLException {
						BoardVo vo = new BoardVo();
						vo.setNo(rs.getInt("idx"));
						vo.setTitle(rs.getString("title"));
						vo.setContent(rs.getString("content"));
						vo.setName(rs.getString("reg_name"));
						vo.setRegDate(rs.getString("reg_date"));
						vo.setModDate(rs.getString("mod_date"));
						return vo;
					}
				}).build();
	}

	@Bean(name = "sample2MyBatisReader")
	@StepScope
	MyBatisPagingItemReader<BoardVo> sample2MyBatisReader(
			@Value("#{jobParameters[chunk]==null?sample2JobDefaultChunk:jobParameters[chunk]}") String chunk,
			@Qualifier("board.sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {

		Map<String, Object> parameterValues = new HashMap<>();
		parameterValues.put("boardVo", null);
		parameterValues.put("foreach", null);

		return new MyBatisPagingItemReaderBuilder<BoardVo>().sqlSessionFactory(sqlSessionFactory)
				.pageSize(Integer.parseInt(chunk))
				.queryId("com.alpha.batch.domain.sample.board.mapper.BoardMapper.select")
				// .parameterValues(parameterValues)
				.build();
	}

	@Bean(name = "sample2Processor")
	@StepScope
	ItemProcessor<DataEncVo, BoardVo> sample2Processor() {
		return new ItemProcessor<DataEncVo, BoardVo>() {
			@Override
			public BoardVo process(final DataEncVo vo) throws Exception {

				String decStr = BaseUtil.getCryptoUtil().getAES256().decryption(vo.getEnc());
				BoardVo boardVo = BaseUtil.getObjectMapperUtil().readValue(decStr, BoardVo.class);
				String name = boardVo.getName();

				boardVo.setName(BaseUtil.getMaskUtil().maskName(name));
				boardVo.setModDate(BaseUtil.getTimeUtil().getCurrentTime("yyyy-MM-dd"));

				ScoreVo scoreVo = scoreService.get(name);

				if (scoreVo == null) {
					scoreVo = new ScoreVo();
					scoreVo.setName(name);
					scoreVo.setScore(1);
					scoreService.create(scoreVo);
				} else {
					scoreVo.setScore(scoreVo.getScore() + 1);
					scoreService.update(scoreVo);
				}

				log.debug(">> scoreVo:{}", scoreVo.toString());

				return boardVo;
			}
		};
	}

	@Bean(name = "sample2JdbcWriter")
	@StepScope
	JdbcBatchItemWriter<BoardVo> sample2JdbcWriter(@Qualifier("board.dataSource") DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<BoardVo>()
				.itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
				.sql("INSERT INTO TB_BOARD (TITLE, CONTENT, REG_NAME, REG_DATE, MOD_DATE) VALUES (:title, :content, :name, :regDate, :modDate)")
				.dataSource(dataSource)
				.build();
	}

	@Bean(name = "sample2MyBatisWriter")
	@StepScope
	MyBatisBatchItemWriter<BoardVo> sample2MyBatisWriter(@Qualifier("board.sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
		return new MyBatisBatchItemWriterBuilder<BoardVo>().sqlSessionFactory(sqlSessionFactory)
				.statementId("com.alpha.batch.domain.sample.board.mapper.BoardMapper.insert")
				.build();
	}

	@Bean(name = "sample2FileWriter")
	@StepScope
	FlatFileItemWriter<BoardVo> sample2FileWriter() {
		return new FlatFileItemWriterBuilder<BoardVo>().name("sample2FileWriter").encoding("UTF-8")
				.resource(new FileSystemResource(this.getSendFilePath()))
				.delimited().delimiter(",")
				.names("no", "title", "content", "name", "regDate", "modDate")
				.build();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// PagingQueryProvider, rowMapper
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Bean(name = "board.pagingQueryProvider")
	@StepScope
	PagingQueryProvider createQueryProvider(@Qualifier("board.dataSource") DataSource dataSource) throws Exception {
		SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();

		queryProvider.setDataSource(dataSource); // Database에 맞는 PagingQueryProvider를 선택하기 위해
		queryProvider.setSelectClause("SELECT IDX, TITLE, CONTENT, REG_NAME, REG_DATE, MOD_DATE");
		queryProvider.setFromClause("FROM TB_BOARD");
		queryProvider.setWhereClause("WHERE idx >= :index");

		Map<String, Order> sortKeys = new HashMap<>(1);
		sortKeys.put("idx", Order.ASCENDING);

		queryProvider.setSortKeys(sortKeys);

		return queryProvider.getObject();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Tasklet
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Tasklet deleteDataTasklet() {
		return new Tasklet() {

			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

				boardService.deleteAll();
				scoreService.deleteAll();

				/*
				 * //멀티DB 트렌젝션 PlatformTransactionManager chainedTxManager =
				 * getChainedTransactionManager(boardTxManager,scoreTxManager); TransactionUtil
				 * transactionUtil = TransactionUtilPack.getTransactionUtil(chainedTxManager);
				 * TransactionStatus txStatus = transactionUtil.start(); try { int i=1;
				 * 
				 * boardService.deleteAll(); scoreService.deleteAll();
				 * 
				 * log.info(">> divide:{}",100/i); }catch(Exception e) {
				 * transactionUtil.rollback(txStatus); throw new
				 * RuntimeException(e.getMessage()); } transactionUtil.commit(txStatus);
				 */

				return RepeatStatus.FINISHED;
			}

		};
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Listener
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JobExecutionListener jobExecutionListener() {

		return new JobExecutionListener() {

			@Override
			public void beforeJob(JobExecution jobExecution) {

				// job수행시 독립적인-파일디렉토리를 위한 구별키 설정
				setDiscriminateKey(jobExecution);

				log.info(">> beforeJob:{}", jobExecution.getStatus());
			}

			@Override
			public void afterJob(JobExecution jobExecution) {
				if (jobExecution.getStatus() == BatchStatus.FAILED) {
					log.error(">> afterJob:{}", jobExecution.getStatus());
				}

				// job수행시 독립적인-파일디렉토리 청소
				// cleanDiscriminatePath();

				long executeTime = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();

				log.info(">> afterJob:{}", jobExecution.getStatus());
				log.info(">> executeTime:{}ms", executeTime);
			}
		};
	}

	private StepExecutionListener stepExecutionListener() {

		return new StepExecutionListener() {

			@Override
			public void beforeStep(StepExecution stepExecution) {
				log.info(">> step:{}, beforeStep:{}", stepExecution.getStepName(), stepExecution.getStatus());
			}

			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				log.info(">> step:{}, afterStep:{}", stepExecution.getStepName(), stepExecution.getStatus());
				return stepExecution.getExitStatus();
			}
		};
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Vo
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Getter
	@Setter
	public static class DataEncVo implements Serializable {
		private static final long serialVersionUID = 1L;

		private String enc;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
