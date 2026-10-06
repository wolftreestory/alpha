package com.alpha.batch.job;

import java.util.HashMap;
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
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alpha.batch.config.AbstractJobConfig;
import com.alpha.batch.meta.AbstractSchedulerMetaTaskInfoPack.ScheduleType;
import com.alpha.batch.meta.SchedulerMetaPack.SchedulerMetaInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration("dummyJobConfig")
@ConditionalOnExpression("#{'${spring.batch.job.names}'.contains('dummyJob')}")
@SchedulerMetaInfo(jobName = "dummyJob", description = "dummyJob 설명", scheduleType = ScheduleType.CRON_EXPRESSION, scheduleValue = "0 0/1 * * * ?")
public class DummyJobConfig extends AbstractJobConfig {

	@Value("${alpha.batch.fileBase}")
	private String batchFileBase;

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Spel bean ref.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Bean(name = "dummyJobDefaultLoopCount")
	String dummyJobDefaultLoopCount() {
		return "100";
	}

	@Bean(name = "dummyJobDefaultSleep")
	String dummyJobDefaultSleep() {
		return "50";
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// job
	// dummyStep : dummyTasklet 수행
	//
	// 스프링배치는 동일 JobParameter에 대한 실행 이력이 있으면 실행하지 않느다.(중복 데이터가 쌓일 수가 있기 때문에)
	// 만약, 동일 JobParameter로 계속 실행을 해야 할 경우 incrementer에 new RunIdIncrementer()를 적용한다.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Bean(name = "dummyJob")
	Job dummyJob(@Qualifier("dummyStep") Step step) {
		return this.jobBuilderFactory.get("dummyJob").incrementer(new AidRunIdIncrementer()) // 임의의 파라미터를 추가로 사용해 매번
																								// run.id 값을 변경.
				.listener(this.jobExecutionListener()).start(step).build();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// step
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Bean(name = "dummyStep")
	@JobScope
	Step dummyStep(@Value("#{jobParameters[params]}") String params,
			@Value("#{jobParameters[loopCount]==null?dummyJobDefaultLoopCount:jobParameters[loopCount]}") String loopCount,
			@Value("#{jobParameters[sleep]==null?dummyJobDefaultSleep:jobParameters[sleep]}") String sleep) {

		Map<String, Object> paramMap = this.extractMap(params);

		return this.stepBuilderFactory.get("dummyStep1").listener(this.stepExecutionListener())
				.tasklet(this.dummyTasklet(paramMap, loopCount, sleep)).build();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Tasklet
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Tasklet dummyTasklet(Map<String, Object> paramMap, String loopCount, String sleep) {

		return new Tasklet() {

			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

				log.info(">> loopCount:{}", loopCount);
				log.info(">> sleep:{}", sleep);

				int max = Integer.parseInt(loopCount);
				int interval = Integer.parseInt(sleep);

				for (int i = 0; i < max; i++) {
					Thread.sleep(interval);
					if (i > 50) {
						throw new RuntimeException("오류 발생");
					}
					log.debug("sleep: {}", i);
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
				printJobParameters(jobExecution);
				log.info(">> beforeJob:{}", jobExecution.getStatus());
			}

			@Override
			public void afterJob(JobExecution jobExecution) {
				if (jobExecution.getStatus() == BatchStatus.FAILED) {
					log.error(">> afterJob:{}", jobExecution.getStatus());
				}

				Map<String, Object> data = new HashMap<>();
				data.put("message", "정상완료");

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

}