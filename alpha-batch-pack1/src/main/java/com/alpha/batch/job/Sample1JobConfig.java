package com.alpha.batch.job;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.separator.SimpleRecordSeparatorPolicy;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.alpha.base.BaseUtil;
import com.alpha.base.context.PageContext;
import com.alpha.base.exception.SystemException;
import com.alpha.base.support.util.FileUtilPack.StoreInfo;
import com.alpha.batch.config.AbstractJobConfig;
import com.alpha.batch.config.BatchProperties.BatchJobProperties;
import com.alpha.batch.meta.AbstractSchedulerMetaTaskInfoPack.ScheduleType;
import com.alpha.batch.meta.SchedulerMetaPack.SchedulerMetaInfo;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration("sample1JobConfig")
@ConditionalOnExpression("#{'${spring.batch.job.names}'.contains('sample1Job')}")
@SchedulerMetaInfo(jobName = "sample1Job", description = "파일 읽음 -> 처리(암호화) -> 파일저장, 파일 -> 업로드(RestAPI)", scheduleType = ScheduleType.FIXED_DELAY, scheduleValue = "5000")
public class Sample1JobConfig extends AbstractJobConfig {

	private final long defaultThread = 20;

	private final long defaultMultiple = 100;

	private final long defaultChunk = 100;

	private final String workFileName = "sample1Job-workFile";

	private final String sendFileName = "sample1Job-sendFile";

	@Value("${alpha.batch.fileBase}")
	private String batchFileBase;

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Autowired
	private BatchJobProperties batchJobProperties;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String getWorkFilePath() {
		// 배치 수행시 마다 다른 파일을 만듬
		return this.getDiscriminatePath(this.batchFileBase, this.workFileName);
	}

	private String getSendFilePath() {
		return this.getDiscriminatePath(this.batchFileBase, this.sendFileName);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Spel bean ref.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Bean(name = "sample1JobDefaultThread")
	Long sample1JobDefaultThread() {
		return this.defaultThread;
	}

	@Bean(name = "sample1JobDefaultChunk")
	Long sample1JobDefaultChunk() {
		return this.defaultChunk;
	}

	@Bean(name = "sample1JobDefaultMultiple")
	Long sample1JobDefaultMultiple() {
		return this.defaultMultiple;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// job
	// sample1Step1 : RestAPI(http://[server]/xtra/boards/v1/)를 호출하여 jobParameters인 multiple만큼 데이타 증폭하여 파일생성
	// sample1Step2 : 파일 읽음 -> 처리(암호화) -> 파일저장, jobParameters인 chunk 단위로 처리
	// sample1Step3 : 파일 -> 업로드(RestAPI), 생성된 파일을 RestAPI(http://[server]/xtra/file/v1/attach/fileUpload)를 사용하여 업로드
	// sample1Step9 : sample1Step1 실패시 처리할 step (예제는 별다른 처리 없이 로그만 출력)
	//
	// 로컬 : [server] = 127.0.0.1:9970
	// 개발 : [server] = dev.apollo.co.kr
	//
	// 스프링배치는 동일 JobParameter에 대한 실행 이력이 있으면 실행하지 않느다.(중복 데이터가 쌓일 수가 있기 때문에)
	// 만약, 동일 JobParameter로 계속 실행을 해야 할 경우 incrementer에 new RunIdIncrementer()를 적용한다.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Bean(name = "sample1Job")
	Job sample1Job() {
		// 같은 @Configuration 클래스 안에서 Bean 메서드를 this로 호출해도, CGLIB 프록시 덕분에 DI 컨테이너를 통해 Bean을 가져오기 때문에 싱글턴 보장이 유지.
		// @Configuration(proxyBeanMethods = false)로 설정하면 프록시를 생성하지 않아, 직접 호출 시 새 객체가 반환.
		// this.sample1Step1(null, null)처럼 명시적으로 null을 넘겨도 컨테이너에서 가져온 Bean 인스턴스는 이미 완성된 상태이므로 별 영향 없음

		Step sample1Step1 = this.sample1Step1(null, null);

		return this.jobBuilderFactory.get("sample1Job")
				.incrementer(new AidRunIdIncrementer()) // 임의의 파라미터를 추가로 사용해 매번 run.id 값을 변경.
				.listener(this.jobExecutionListener())
				.start(sample1Step1).on(BatchStatus.COMPLETED.toString())
					.to(this.sample1Step2(null, null, null))
					.next(this.sample1Step3())
				.from(sample1Step1).on(BatchStatus.FAILED.toString())
					.to(this.sample1Step9()).end()
				.build();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// step
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Bean(name = "sample1Step1")
	@JobScope
	Step sample1Step1(
			@Value("#{jobParameters[params]}") String params,
			@Value("#{jobParameters[multiple]==null?sample1JobDefaultMultiple:jobParameters[multiple]}") Long multiple) {

		log.info(">> params:{}", params);

		// 인자params(json을 base64로 디코딩한 문자열)에서 추출하여 적용
		Map<String, Object> paramMap = this.extractMap(params);
		log.info(">> paramMap:{}", paramMap);

		if (paramMap.containsKey("multiple")) {
			multiple = Long.valueOf(paramMap.get("multiple").toString());
		}

		// RestAPI(http://localhost:9970//xtra/boards/v1/)를 호출하여 jobParameters인
		// multiple만큼 데이타 증폭하여 파일생성
		return this.stepBuilderFactory.get("sample1Step1").listener(this.stepExecutionListener())
				.tasklet(this.createFileTasklet(multiple)).build();
	}

	@Bean(name = "sample1Step2")
	@JobScope
	Step sample1Step2(
			@Value("#{jobParameters[params]}") String params,
			@Value("#{jobParameters[chunk]==null?sample1JobDefaultChunk:jobParameters[chunk]}") String chunk,
			@Value("#{jobParameters[thread]==null?sample1JobDefaultThread:jobParameters[thread]}") String thread) {

		// 인자params(json을 base64로 디코딩한 문자열)에서 추출하여 적용
		if (this.extractMap(params).containsKey("chunk")) {
			chunk = this.extractMap(params).get("chunk").toString();
		}

		// 멀티쓰레드 주의사항
		// 1. Reader와 Writer가 멀티쓰레드를 지원하는지 확인 (Reader와 Writer의 Javadoc에 항상 저 thread-safe
		// 문구가 있는지 확인)
		// 2. 실패 지점에서 재시작하는 것은 불가능
		// 3. 일반적으로 corePoolSize, maximumPoolSize, throttleLimit 를 모두 같은 값
		// 4. 코어 스레드 타임아웃을 허용할 것인지에 대한 설정 메서드. true로 설정할 경우 코어 쓰레드를 10으로 설정했어도
		// 일정시간(keepAliveSeconds)이 지나면 코어 스레드 개수가 줄어 듬.
		// 5. 코어 스레드 타임아웃을 허용했을 경우 사용되는 설정값으로, 여기 설정된 시간이 지날 때까지 코어 스레드 풀의 스레드가 사용되지 않을
		// 경우 해당 스레드는 terminate된다
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(Integer.parseInt(thread)); // 기본 사이즈
		executor.setMaxPoolSize(Integer.parseInt(thread)); // 최대 사이즈
		executor.setThreadNamePrefix("step2-thread-");
		executor.setAllowCoreThreadTimeOut(true);
		executor.setKeepAliveSeconds(10);
		executor.initialize();

		// 파일읽음 -> 처리(암호화) -> 파일저장, jobParameters인 chunk 단위로 처리
		return this.stepBuilderFactory.get("sample1Step2").listener(this.stepExecutionListener())
				.<ComentVo, ComentEncVo>chunk(Integer.parseInt(chunk)).reader(this.sample1Reader()) // Step을 멀티쓰레드로 수행시 saveState = false 로 사용해야 함.
				.processor(this.sample1Processor()).writer(this.sample1Writer()).taskExecutor(executor) // Step을 멀티쓰레드로 수행 : ThreadPoolTaskExecutor
				.throttleLimit(Integer.parseInt(thread)) // Step을 멀티쓰레드로 수행 : 생성된 쓰레드 중 몇개를 실제 작업에 사용할지를 결정
				.build();
	}

	@Bean(name = "sample1Step3")
	@JobScope
	Step sample1Step3() {
		// 파일 -> 업로드, 생성된 파일을 RestAPI(http://[server]/xtra/file/v1/attach/fileuUpload)를
		// 사용하여 업로드
		return this.stepBuilderFactory.get("sample1Step3").listener(this.stepExecutionListener())
				.tasklet(new Tasklet() {
					@Override
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
							throws Exception {

						// 파일업로드를 처리하는 uploadApi의 요청spec(method,parameter)에 적합한 구성을 한다.
						URI remoteURI = new URI(batchJobProperties.getSample1Job().getUploadApi());
						String fileParamName = "mFiles";
						MultipartFile file = BaseUtil.getFileUtil().getMultipartFile(getSendFilePath());
						log.debug(">> file: {}", file);

						List<StoreInfo> list = BaseUtil.getRestTemplateUtil().attachStream(remoteURI, fileParamName,
								file, new TypeReference<List<StoreInfo>>() {
								});

						list.forEach(i -> log.info(">> storeInfo:{}", i.toString()));
						return RepeatStatus.FINISHED;
					}
				}).build();
	}

	@Bean(name = "sample1Step9")
	@JobScope
	Step sample1Step9() {
		// sample1Step1 실패시 처리할 step (예제는 별다른 처리 없이 로그만 출력)
		return this.stepBuilderFactory.get("sample1Step9").listener(this.stepExecutionListener())
				.tasklet(new Tasklet() {
					@Override
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
							throws Exception {
						log.info(">> execute:{}", chunkContext.getStepContext().getStepName());
						log.info(">> sample1Step1 수행중 오류");
						return RepeatStatus.FINISHED;
					}
				}).build();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// ItemReader, ItemProcessor, ItemWriter
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Bean(name = "sample1Reader")
	@StepScope
	FlatFileItemReader<ComentVo> sample1Reader() {
		// 파일읽음
		return new FlatFileItemReaderBuilder<ComentVo>().name("sample1Reader")
				.resource(new FileSystemResource(this.getWorkFilePath())).delimited().delimiter(",")
				.names("no", "title", "content", "name", "regDate", "modDate").targetType(ComentVo.class)
				.recordSeparatorPolicy(new SimpleRecordSeparatorPolicy() {
					@Override
					public String postProcess(String record) {
						return record.trim();
					}
				}).saveState(false) // 실패하면 무조건 처음부터 다시 실행될 수 있도록 해당 옵션은 false(멀티쓰레드 환경에서 사용할 경우 필수적으로 사용해야할 옵션)
				.build();
	}

	@Bean(name = "sample1Processor")
	@StepScope
	ItemProcessor<ComentVo, ComentEncVo> sample1Processor() {
		// 처리(암호화)
		return new ItemProcessor<ComentVo, ComentEncVo>() {

			@Override
			public ComentEncVo process(ComentVo vo) throws Exception {

				String json = BaseUtil.getJsonUtil().toJson(vo);
				String enc = BaseUtil.getCryptoUtil().getAES256().encryption(json);

				ComentEncVo encInfo = BaseUtil.getObjectMapperUtil().convert(vo, ComentEncVo.class);
				encInfo.setModDate(BaseUtil.getTimeUtil().getCurrentTime("yyyy-MM-dd HH:mm:ss"));
				encInfo.setEnc(enc);

				// log.info(">> process enc:{}",enc);
				return encInfo;
			}

		};
	}

	@Bean(name = "sample1Writer")
	@StepScope
	FlatFileItemWriter<ComentEncVo> sample1Writer() {
		// 파일저장
		return new FlatFileItemWriterBuilder<ComentEncVo>().name("sample1Writer").encoding("UTF-8")
				.resource(new FileSystemResource(this.getSendFilePath())).delimited().delimiter(",")
				// .names("no", "title", "content", "name", "regDate", "modDate", "enc")
				.names("enc").build();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Tasklet
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Tasklet createFileTasklet(long multiple) {

		return new Tasklet() {
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

				// Map<String, Object> jobParameters =
				// chunkContext.getStepContext().getJobParameters();
				// log.info(">> jobParameters:{}",jobParameters);

				log.info(">> multiple:{}", multiple);

				try {
					File file = new File(getWorkFilePath());
					if (file.exists()) {
						file.delete();
					}
					file.createNewFile();

					// FileOutputStream fos = new FileOutputStream(file);
					// OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
					// BufferedWriter writer = new BufferedWriter(osw);

					PrintWriter writer = new PrintWriter(new FileWriter(file));

					PageContext pageContext = new PageContext();
					pageContext.setRowSize(10);
					pageContext.setPageNo(1);

					long seqNo = 1;
					while (true) {
						List<ComentVo> list = getWorkList(new ComentVo(), pageContext);
						if (list == null || list.isEmpty()) {
							break;
						}

						for (int i = 0; i < multiple; i++) {
							StringBuilder builder = new StringBuilder();
							for (ComentVo item : list) {
								item.setNo(seqNo++);
								builder.append(item.getCsvLine()).append(System.lineSeparator());
							}
							// log.info(">> {}",builder.toString());
							writer.write(builder.toString());
						}

						pageContext.setPageNo(pageContext.getPageNo() + 1);
						if (pageContext.getTotalPage() < pageContext.getPageNo()) {
							break;
						}
					}

					writer.close();

				} catch (Exception e) {
					throw new SystemException(e.getMessage());
				}

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
				// job수행시 독립적인 파일디렉토리를 위한 구별키 설정
				setDiscriminateKey(jobExecution);

				// ExecutionContext context = jobExecution.getExecutionContext();
				// context.put("startTime", System.nanoTime());
				log.info(">> beforeJob:{}", jobExecution.getStatus());
			}

			@Override
			public void afterJob(JobExecution jobExecution) {
				if (jobExecution.getStatus() == BatchStatus.FAILED) {
					log.error(">> afterJob:{}", jobExecution.getStatus());
				}

				Map<String, Object> data = new HashMap<>();
				data.put("message", "정상완료");

				doCallBack(jobExecution, data);

				// ExecutionContext context = jobExecution.getExecutionContext();
				// long startTime = (Long)context.get("startTime");
				// long executeTime =
				// TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-startTime);

				// job수행시 독립적인 파일디렉토리 청소
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
	// vo
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Getter
	@Setter
	public static class ComentEncVo extends ComentVo {
		private static final long serialVersionUID = 1L;

		private String enc;
	}

	@Getter
	@Setter
	public static class ComentVo implements Serializable {
		private static final long serialVersionUID = 1L;

		private long no;
		private String title;
		private String content;
		private String name;
		private String regDate;
		private String modDate;

		public ComentVo() {
		}

		public ComentVo(int no) {
			this.no = no;
		}

		public String getCsvLine() {
			StringBuilder builder = new StringBuilder();
			builder.append(no).append(",");
			builder.append(title).append(",");
			builder.append(content).append(",");
			builder.append(name).append(",");
			builder.append(regDate).append(",");
			builder.append(modDate);
			return builder.toString();
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private List<ComentVo> getWorkList(ComentVo searchInfo, PageContext pageContext) {
		if (searchInfo == null) {
			return null;
		}

		long no = searchInfo.getNo();
		String title = searchInfo.getTitle();
		String content = searchInfo.getContent();
		String name = searchInfo.getName();
		String pageNo = String.valueOf(pageContext.getPageNo());
		String rowSize = String.valueOf(pageContext.getRowSize());

		// workApi의 요청spec(method,parameter)에 적합한 구성을 한다.
		URI apiUri = UriComponentsBuilder.fromUriString(batchJobProperties.getSample1Job().getWorkApi())
				.queryParam("no", no).queryParam("title", title).queryParam("content", content).queryParam("name", name)
				.queryParam("pageNo", pageNo).queryParam("rowSize", rowSize).encode().build().toUri();

		log.debug(">> apiUri: {}", apiUri);

		// GET 메서드를 이용해 요청한 URL로부터 ResponseEntity로 응답, ResponseEntity 표준은 String이며,
		// Jackson을 이용해 파싱해 사용.
		ResponseEntity<Object> responseEntity = BaseUtil.getRestTemplateUtil().getForEntity(apiUri, Object.class);

		List<ComentVo> result = null;
		if (responseEntity != null) {
			PageContext responsePageContext = BaseUtil.getPageContext(responseEntity.getHeaders());
			pageContext.setTotalCount(responsePageContext.getTotalCount());
			pageContext.setTotalPage(responsePageContext.getTotalPage());

			result = BaseUtil.getObjectMapperUtil().convert(responseEntity.getBody(),
					new TypeReference<List<ComentVo>>() {
					});
		}

		return result;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}