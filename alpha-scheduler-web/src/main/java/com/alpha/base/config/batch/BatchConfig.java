package com.alpha.base.config.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.alpha.base.config.batch.BatchProperties.TaskLogHistoryType;
import com.alpha.base.support.batch.BatchUtil;
import com.alpha.base.support.batch.SchedulerManagerPack;
import com.alpha.base.support.batch.TaskConfigPack;
import com.alpha.base.support.batch.TaskLogFileDownload;
import com.alpha.base.support.batch.TaskLogHandlerPack;
import com.alpha.base.support.batch.TaskMetaAgentPack;
import com.alpha.base.support.batch.TaskMetaLoaderPack;
import com.alpha.base.support.batch.SchedulerManagerPack.SchedulerManager;
import com.alpha.base.support.batch.TaskConfigPack.TaskConfig;
import com.alpha.base.support.batch.TaskLogContextPack.TaskLogContext;
import com.alpha.base.support.batch.TaskLogHandlerPack.TaskLogHandler;
import com.alpha.base.support.batch.TaskMetaAgentPack.TaskMetaAgent;
import com.alpha.base.support.batch.TaskMetaLoaderPack.TaskMetaLoader;
import com.alpha.base.support.batch.context.SchedulerContext;
import com.alpha.base.support.batch.context.TaskLogContextOnFile;
import com.alpha.base.support.batch.context.TaskLogContextOnMysql;
import com.alpha.base.support.batch.context.TaskLogContextOnOracle;
import com.alpha.base.support.batch.mapper.TaskExecutionMapper;
import com.alpha.base.support.util.Base64UtilPack.Base64Util;
import com.alpha.base.support.util.JsonUtilPack.JsonUtil;
import com.alpha.base.support.util.LobConverterUtilPack.LobConverterUtil;
import com.alpha.base.support.util.ObjectMapperUtilPack.ObjectMapperUtil;
import com.alpha.base.support.util.SystemUtilPack.SystemUtil;
import com.alpha.base.support.util.TimeUtilPack.TimeUtil;
import com.alpha.base.support.util.TransactionUtilPack.TransactionUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnExpression("T(com.alpha.base.BatchConfig).isEnabled()")
@ConditionalOnBean(com.alpha.base.config.AidBaseConfig.class)
public class BatchConfig {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static final String BATCH_UTIL = "batch.util";

	public static final String DEFAULT_TASK_CONFIG = "default.alpha.taskConfig";
	public static final String DEFAULT_TASK_EXECUTOR = "default.alpha.taskExecutor";
	public static final String DEFAULT_TASK_SCHEDULER = "default.alpha.taskScheduler";

	public static final String TASK_META_AGENT = "alpha.taskMetaAgent";
	public static final String TASK_META_LOADER = "alpha.taskMeskLoader";

	public static final String TASK_LOG_CONTEXT = "alpha.taskLogContext";
	public static final String TASK_LOG_HANDLER = "alpha.taskLogHandler";
	public static final String TASK_LOG_ASPECT = "alpha.taskLogAspect";

	public static final String TASK_LOG_FILE_DOWNLOAD = "alpha.taskLogFileDownload";

	public static final String SCHEDULER_MANAGER = "alpha.schedulerManager";
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Value("${spring.profiles.active:local}")
	private String springProfilesActive;
		
	@Value("${logging.file.name}")
	private String loggingFileName;

	@Autowired
	private BatchProperties batchProperties;

	@Autowired
	private Base64Util base64Util;

	@Autowired
	private ObjectMapperUtil objectMapperUtil;

	@Autowired
	private JsonUtil jsonUtil;

	@Autowired
	private LobConverterUtil lobConverterUtil;
	
	@Autowired
	private SystemUtil systemUtil;

	@Autowired
	private TimeUtil timeUtil;

	@Autowired(required=false)
	private TransactionUtil transactionUtil;
	
	@Autowired(required=false)
	@Qualifier("taskExecutionMapper")
    private TaskExecutionMapper taskExecutionMapper;
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean(name=BATCH_UTIL)
	BatchUtil batchUtil() {
    	return BatchUtil.builder()
    			.batchProperties(batchProperties)
    			.base64Util(base64Util)
    			.jsonUtil(jsonUtil)
    			.objectMapperUtil(objectMapperUtil)
    			.lobConverterUtil(lobConverterUtil)
    			.systemUtil(systemUtil)
    			.timeUtil(timeUtil)
    			.transactionUtil(transactionUtil)
    			.build();
	}
	
    @Bean(name=DEFAULT_TASK_CONFIG)
    @ConditionalOnBean(name={BATCH_UTIL})
    TaskConfig taskConfig(BatchUtil batchUtil){
    	log.info(">> {}",DEFAULT_TASK_CONFIG);
    	
    	List<String> logExceptPatternList = new ArrayList<>();
    	logExceptPatternList.add("*.mapper.*");
    	logExceptPatternList.add("jdbc.sqlonly");
    	logExceptPatternList.add("jdbc.resultsettable");
    	
    	Properties logPolicyProps = new Properties();
    	logPolicyProps.setProperty("globalPolicyYn", "Y");

    	List<String> runnableServerList = new ArrayList<>();
    	runnableServerList.add(springProfilesActive.equals("local")?systemUtil.getServerIpPort():""); // local
    	if(batchProperties.getTask().getRunnableServerList()!=null) {
    		runnableServerList.addAll(batchProperties.getTask().getRunnableServerList());
    	}
   
    	TaskConfig taskConfig = TaskConfigPack.getTaskConfig(batchUtil);
    	taskConfig.setSaveImmediatelyYn("Y");
    	taskConfig.setConfigLocation(batchProperties.getTask().getConfigLocation());    	
    	taskConfig.setExecutionLogLevel("debug");
    	taskConfig.setExecutionLogFilePathPattern(batchProperties.getScheduler().getExecutionLogFilePathPattern());
    	taskConfig.setExceptionLogLevel("error");
    	taskConfig.setExceptionLogFilePathPattern(batchProperties.getScheduler().getExceptionLogFilePathPattern());
    	taskConfig.setScheduleTermEnableYn("N");
    	taskConfig.setLogEnableYn("Y");
    	taskConfig.setLogExceptEnableYn("N");
    	taskConfig.setLogExceptPatternList(logExceptPatternList);
    	taskConfig.setLogPolicyProps(logPolicyProps);
    	taskConfig.setRunnableServerList(runnableServerList);

    	return taskConfig;
    }
    
    @Bean(name=DEFAULT_TASK_EXECUTOR)
    ThreadPoolTaskExecutor taskExecutor() {
    	log.info(">> {}",DEFAULT_TASK_EXECUTOR);
    	
    	//작업을 core 사이즈만큼의 스레드로 수행. 작업량을 넘어서는 경우 queueCapacity에서 설정한 크기의 LinkedBlockingQueue를 생성하여 대기
    	//만약 queueCapacity의 queue를 넘어서면, setMaxPoolSize 에서 설정한 만큼 스레드를 추가 생성    	
    	//기본 20개 스레드에서 처리하다가 작업량을 넘어서는 경우 크기가 10인 queue 에서 대기하고 큐 크기보다도 초과한다면 최대 50개까지 스레드를 생성
    	
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20); //생성해서 사용할 스레드 풀에 속한 기본 스레드 갯수 (default:1)
        executor.setQueueCapacity(20); //이벤트 대기 큐 크기 (default:Integer.MAX_VALUE (약 21억))
        executor.setMaxPoolSize(50); //최대 스레드 갯수 (default:Integer.MAX_VALUE (약 21억))
        executor.setThreadNamePrefix("default.taskExecutor-");
        executor.initialize();
        return executor;
    }
    
    @Bean(name=DEFAULT_TASK_SCHEDULER)
    ThreadPoolTaskScheduler taskScheduler() {
    	log.info(">> {}",DEFAULT_TASK_SCHEDULER);
    	
    	//쓰레드는 한번 생성 시키고 소멸하면 굉장히 많은 리소스를 소모하게 되기때문에 매번 쓰레드를 생성시키고 없애기 보다는
    	//Thread Pool 에서는 설정된 크기의 쓰레드를 만들어 놓고 해당 쓰레드들을 계속해서 재사용할 수 있도록 관리를 한다.
    	//일반적으로 CPU의 코어 개수에 따라 유동적으로 생성될 수 있도록 해주는 것이 좋다. 

    	int poolSize = batchProperties.getScheduler().getSchedulerPoolSize();
    	if(poolSize==0) {poolSize = Runtime.getRuntime().availableProcessors();}
        log.info(">> default.schedulerPoolSize.poolSize:{}",poolSize);

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();        
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("default.taskScheduler-");
        scheduler.initialize();        
        return scheduler;
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
	@Bean(name=TASK_LOG_CONTEXT)
	@ConditionalOnBean(name={BATCH_UTIL})
	TaskLogContext taskLogContext(@Qualifier(BATCH_UTIL) BatchUtil batchUtil) {
    	log.info(">> {}",TASK_LOG_CONTEXT);
    	
		TaskLogContext taskLogContext = null;
		
		if(TaskLogHistoryType.file.equals(batchProperties.getTask().getLogStoreType())) {
		    TaskLogContextOnFile context = new TaskLogContextOnFile(batchUtil);
		    context.setContextLocation(batchProperties.getTask().getContextLocation()); // logContext위치
		    context.setLogFileDbStoreYn("N"); // 로그파일DB저장여부
		    context.setLogFileRemoveYn("N"); // 로그파일삭제여부
		    context.setLogHistoryShrinkYn(batchProperties.getTask().getLogPolicy().getLogHistoryShrinkYn()); // 로그이력(로그파일+수행컨텍스트)축소여부
		    context.setLogHistoryShrinkBaseDays(batchProperties.getTask().getLogPolicy().getLogHistoryShrinkBaseDays()); // 로그이력(로그파일+수행컨텍스트)축소기준 days
		    context.setLogHistoryShrinkBaseRows(batchProperties.getTask().getLogPolicy().getLogHistoryShrinkBaseRows()); // 로그이력(로그파일+수행컨텍스트)축소기준 rows
		    taskLogContext = (TaskLogContext)context;
		}
		
		if(TaskLogHistoryType.mysql.equals(batchProperties.getTask().getLogStoreType())) {
	        TaskLogContextOnMysql context = new TaskLogContextOnMysql(batchUtil);
	        context.setTaskExecutionMapper(taskExecutionMapper);
	        context.setLogFileDbStoreYn(batchProperties.getTask().getLogPolicy().getLogFileDbStoreYn()); // 로그파일DB저장여부
	        context.setLogFileRemoveYn(batchProperties.getTask().getLogPolicy().getLogFileRemoveYn());  // 로그파일삭제여부 (DB저장 후)
		    context.setLogHistoryShrinkYn(batchProperties.getTask().getLogPolicy().getLogHistoryShrinkYn()); // 로그이력(로그파일+수행컨텍스트)축소여부
		    context.setLogHistoryShrinkBaseDays(batchProperties.getTask().getLogPolicy().getLogHistoryShrinkBaseDays()); // 로그이력(로그파일+수행컨텍스트)축소기준 days
		    context.setLogHistoryShrinkBaseRows(batchProperties.getTask().getLogPolicy().getLogHistoryShrinkBaseRows()); // 로그이력(로그파일+수행컨텍스트)축소기준 rows
		    taskLogContext = (TaskLogContext)context;
		}
		
		if(TaskLogHistoryType.oracle.equals(batchProperties.getTask().getLogStoreType())) {
			TaskLogContextOnOracle context = new TaskLogContextOnOracle(batchUtil);
	        context.setTaskExecutionMapper(taskExecutionMapper);			
	        context.setLogFileDbStoreYn(batchProperties.getTask().getLogPolicy().getLogFileDbStoreYn()); // 로그파일DB저장여부
	        context.setLogFileRemoveYn(batchProperties.getTask().getLogPolicy().getLogFileRemoveYn());  // 로그파일삭제여부 (DB저장 후)
		    context.setLogHistoryShrinkYn(batchProperties.getTask().getLogPolicy().getLogHistoryShrinkYn()); // 로그이력(로그파일+수행컨텍스트)축소여부
		    context.setLogHistoryShrinkBaseDays(batchProperties.getTask().getLogPolicy().getLogHistoryShrinkBaseDays()); // 로그이력(로그파일+수행컨텍스트)축소기준 days
		    context.setLogHistoryShrinkBaseRows(batchProperties.getTask().getLogPolicy().getLogHistoryShrinkBaseRows()); // 로그이력(로그파일+수행컨텍스트)축소기준 rows
		    taskLogContext = (TaskLogContext)context;
		}
		
	    return taskLogContext;
	}
	
    @Bean(name=TASK_LOG_HANDLER)
    @ConditionalOnBean(name={TASK_LOG_CONTEXT})
    TaskLogHandler taskLogHandler(
    		@Qualifier(BATCH_UTIL) BatchUtil batchUtil,
    		@Qualifier(TASK_LOG_CONTEXT) TaskLogContext taskLogContext) {
    	log.info(">> {}",TASK_LOG_HANDLER); 
        
    	TaskLogHandler taskLogHandler = TaskLogHandlerPack.getTaskLogHandler(batchUtil);
        taskLogHandler.setTaskLogContext(taskLogContext);
        taskLogHandler.setMetaTaskToken(TaskLogHandler.TASK_TOKEN); // logback.xml siftingAppender의 discriminatorKey와 동일해야 함
        taskLogHandler.setMetaExecutionLogFilePath(TaskLogHandler.EXECUTION_LOG_FILE_PATH); // logback.xml taskExecutionSiftingAppender/filter/executionToken과 동일해야 함
        taskLogHandler.setMetaExecutionLogLevel(TaskLogHandler.EXECUTION_LOG_LEVEL); // logback.xml taskExecutionSiftingAppender/filter/executionLogLevel과 동일해야 함
        taskLogHandler.setMetaExceptionLogFilePath(TaskLogHandler.EXCEPTION_LOG_FILE_PATH); // logback.xml taskExceptionSiftingAppender/filter/exceptionToken과 동일해야 함
        taskLogHandler.setMetaExceptionLogLevel(TaskLogHandler.EXCEPTION_LOG_LEVEL); // logback.xml taskExecutionSiftingAppender/filter/exceptionLogLevel과 동일해야 함        
        taskLogHandler.setLogStampPrintYn("Y");
        taskLogHandler.setLogStampStartPrintYn("Y");
        taskLogHandler.setLogStampExceptionPrintYn("Y");
        taskLogHandler.setLogStampAroundPrintYn("Y");
        taskLogHandler.setLogStampEndPrintYn("Y");
        
        return taskLogHandler;
    }	
	
	@Bean(name=TASK_LOG_ASPECT)
    @ConditionalOnBean(name={TASK_LOG_HANDLER})
	TaskLogAspect taskLogAspect(
			@Qualifier(TASK_LOG_HANDLER) TaskLogHandler taskLogHandler) {
    	log.info(">> {}",TASK_LOG_ASPECT); 
    	
		TaskLogAspect taskLogAspect = new TaskLogAspect(taskLogHandler);
		
		return taskLogAspect;
	}

    @Aspect
    public class TaskLogAspect  {
        private final TaskLogHandler taskLogHandler;
        
        public TaskLogAspect(TaskLogHandler taskLogHandler) {
            this.taskLogHandler = taskLogHandler;
        }

        @Pointcut("execution(void com.alpha.base.support.batch.TaskPack$Task.executeTask())")
        public void pointcut() {}

        @Before("pointcut()")
        public void startLog(JoinPoint joinPoint) throws Exception {
        	taskLogHandler.startLog(joinPoint);
        }

        @Around("pointcut()")
        public Object aroundLog(ProceedingJoinPoint joinPoint) throws Throwable {
            return taskLogHandler.aroundLog(joinPoint);
        }

        @AfterThrowing(pointcut = "pointcut()", throwing = "exception")
        public void exceptionLog(JoinPoint joinPoint,Exception exception) throws Exception {
           taskLogHandler.exceptionLog(joinPoint, exception);
        }

        @AfterReturning(pointcut = "pointcut()", returning = "returnObj")
        public void endLog(JoinPoint joinPoint,Object returnObj) throws Exception {
            taskLogHandler.endLog(joinPoint, returnObj);
        }
    }    
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Bean(name=TASK_LOG_FILE_DOWNLOAD)
    @ConditionalOnBean(name={TASK_LOG_HANDLER})	
	TaskLogFileDownload taskLogFileDownload(@Qualifier(TASK_LOG_HANDLER) TaskLogHandler taskLogHandler) {
		return new TaskLogFileDownload(loggingFileName,taskLogHandler);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean(name=TASK_META_AGENT)
    @ConditionalOnBean(name={BATCH_UTIL})
    TaskMetaAgent getTaskMetaAgent (@Qualifier(BATCH_UTIL) BatchUtil batchUtil) {
    	log.info(">> {}",TASK_META_AGENT);
    	
    	TaskMetaAgent taskMetaAgent = TaskMetaAgentPack.getTaskMetaAgent(batchUtil);
    	taskMetaAgent.setAppBase(batchProperties.getAgent().getAppBase());
    	taskMetaAgent.setAppMeta(batchProperties.getAgent().getAppMeta());
    	taskMetaAgent.setAppPrefixList(batchProperties.getAgent().getAppPrefixList());
    	taskMetaAgent.setSpringProfilesActive(this.springProfilesActive);
    	
    	return taskMetaAgent;    	
    }
    
    @Bean(name=TASK_META_LOADER)
    @ConditionalOnBean(name={TASK_LOG_HANDLER,TASK_META_AGENT})
    TaskMetaLoader taskMetaLoader (
    		@Qualifier(BATCH_UTIL) BatchUtil batchUtil,
    		@Qualifier(TASK_LOG_HANDLER) TaskLogHandler taskLogHandler,
    		@Qualifier(TASK_META_AGENT) TaskMetaAgent taskMetaAgent
    		) {
    	log.info(">> {}",TASK_META_LOADER);
    	
    	TaskMetaLoader taskMetaLoader = TaskMetaLoaderPack.getTaskMetaLoader(batchUtil,taskLogHandler);
    	taskMetaLoader.setScanPatternList(batchProperties.getScanConfig().getScanPatternList());
    	taskMetaLoader.setTaskMetaAgent(taskMetaAgent);

    	return taskMetaLoader;
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    @Bean(name=SCHEDULER_MANAGER)
    @ConditionalOnBean(name={BATCH_UTIL,DEFAULT_TASK_EXECUTOR,DEFAULT_TASK_SCHEDULER,DEFAULT_TASK_CONFIG,TASK_LOG_ASPECT})
    SchedulerManager schedulerManager(
    		@Qualifier(BATCH_UTIL) BatchUtil batchUtil,
    		@Qualifier(DEFAULT_TASK_EXECUTOR) ThreadPoolTaskExecutor taskExecutor,
    		@Qualifier(DEFAULT_TASK_SCHEDULER) ThreadPoolTaskScheduler taskScheduler,
    		@Qualifier(DEFAULT_TASK_CONFIG) TaskConfig taskConfig,
    		@Qualifier(TASK_LOG_ASPECT) TaskLogAspect taskLogAspect
    		) {
    	log.info(">> {}",SCHEDULER_MANAGER);	
    	
    	SchedulerContext schedulerContext = new SchedulerContext();    	
    	schedulerContext.setSchedulerContextStartTime(batchUtil.getTimeUtil().getCurrentTime("yyyy.MM.dd HH:mm:ss"));

    	SchedulerManager schedulerManager = SchedulerManagerPack.getSchedulerManager(taskExecutor,taskScheduler);   	
    	schedulerManager.setSchedulerRequestPath(batchProperties.getScheduler().getSchedulerRequestPath()); //접근주소 http://[~]/scheduler/[schedulerRequestPath]/
    	schedulerManager.setSchedulerAutoStartYn(batchProperties.getScheduler().getSchedulerAutoStartYn()); // 서버기동후 스케쥴러자동시작여부(Y|N)
    	schedulerManager.setSchedulerAutoStartDelay(batchProperties.getScheduler().getSchedulerAutoStartDelay()); // 서버기동후 스케쥴러자동시작지연(ms)
    	schedulerManager.setSchedulerStopCheckTerm(batchProperties.getScheduler().getSchedulerStopCheckTerm()); // 스케쥴러중지시 수행중인 작업 체크시간(ms)
    	
    	schedulerManager.setDefaultTaskConfig(taskConfig);
    	schedulerManager.setTaskAspect(taskLogAspect);    	   
    	schedulerManager.setSchedulerContext(schedulerContext);
    	
    	return schedulerManager;
    };

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    @ConditionalOnBean(name={TASK_META_LOADER,SCHEDULER_MANAGER})    
    String doAction (
    		@Qualifier(SCHEDULER_MANAGER) SchedulerManager schedulerManager,
    		@Qualifier(TASK_META_LOADER) TaskMetaLoader taskMetaLoader
    		) {
	    	
    	taskMetaLoader.setSchedulerManager(schedulerManager);
    	taskMetaLoader.doScanLoad();
    	
    	schedulerManager.setTaskMetaLoader(taskMetaLoader);
    	schedulerManager.doSchedulerAutoStart();
    	
    	return schedulerManager.getSchedulerContext().getSchedulerStatus();
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}