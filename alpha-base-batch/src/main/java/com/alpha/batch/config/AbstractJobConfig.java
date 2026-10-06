package com.alpha.batch.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.alpha.base.BaseUtil;
import com.alpha.base.exception.SystemException;
import com.alpha.base.support.util.TransactionUtilPack;
import com.alpha.base.support.util.TransactionUtilPack.TransactionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("deprecation")
public abstract class AbstractJobConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@FunctionalInterface
	public interface ProcessBlock1 {public abstract void execute();}
	
	@FunctionalInterface
	public interface ProcessBlock2<I> {public abstract void execute(I input);}

	@FunctionalInterface
	public interface ProcessBlock3<R> {public abstract R execute();}

	@FunctionalInterface
	public interface ProcessBlock4<I, R> {public abstract R execute(I input);}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private Map<String,Long> repeatCountContext = null;
	
	private Map<String,Map<String,Object>> paramsContext = null;
	
	private Set<String> baseDirPathList = null;
	
	private String discriminateKey = null ;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	final public RepeatStatus repeatContinuable(ChunkContext chunkContext, long limit,long sleepMs) {
		return this.repeatContinuable(chunkContext, limit, sleepMs, null);
	}
	
	final public RepeatStatus repeatContinuable(ChunkContext chunkContext, long limit, long sleepMs, RuntimeException customException) {
		
		String stepName = chunkContext.getStepContext().getStepName();
		
		if(this.repeatCountContext==null) {this.repeatCountContext = new HashMap<>();}
		if(!this.repeatCountContext.containsKey(stepName)) {this.repeatCountContext.put(stepName, 0L);}
		
		long checkCount = this.repeatCountContext.get(stepName)+1;		
		if(checkCount>limit) {
			if(customException==null) {
				throw new SystemException(stepName+" repeatCount("+checkCount+") is over the limit("+limit+")");
			}else {
				throw customException;
			}
		}
		
		try {
			log.debug(">> repeatContinuable {}, count: {}/{} Sleep({})...",stepName,checkCount,limit,sleepMs);
			Thread.sleep(sleepMs);
			this.repeatCountContext.put(stepName, checkCount);
		}catch(Exception e) {
			this.repeatCountContext.remove(stepName);
			throw new SystemException(e.getMessage());
		}
		
		return RepeatStatus.CONTINUABLE;		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	final public String getDiscriminatePath(String dirPath, String fileName) {
		if(StringUtils.isBlank(dirPath)) {throw new SystemException("dirPath is null or empty");}
		if(StringUtils.isBlank(fileName)) {throw new SystemException("fileName is null or empty");}
		if(StringUtils.isBlank(this.discriminateKey)) {throw new SystemException("discriminateKey is null or empty");}

		StringBuilder builder = new StringBuilder();
		builder.append(dirPath).append(File.separator);
		builder.append(this.discriminateKey).append(File.separator);
		
		String path = FilenameUtils.normalize(builder.toString());
		File dir = new File(path);
		if(!dir.exists()) {dir.mkdirs();}
		
		if(this.baseDirPathList==null) {this.baseDirPathList = new HashSet<>();}
		this.baseDirPathList.add(path);
		
		String discriminatePath = FilenameUtils.normalize(builder.append(fileName).toString());
    	log.debug(">> discriminatePath: {}",discriminatePath);
    	
		return discriminatePath;
	}
	
	final public void cleanDiscriminatePath() {
		if(baseDirPathList==null || baseDirPathList.isEmpty()) {return;}
		baseDirPathList.forEach(cleanPath->_deleteDir(cleanPath));
	}
	
	final public void setDiscriminateKey(JobExecution jobExecution) {
    	StringBuilder builder = new StringBuilder();
    	builder.append(jobExecution.getJobInstance().getJobName()).append("-");
    	builder.append(jobExecution.getId()).append("-");
    	builder.append(BaseUtil.getTimeUtil().getCurrentTime());
    	
		this.discriminateKey = builder.toString();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	final public PlatformTransactionManager getChainedTransactionManager(PlatformTransactionManager... transactionManagers) {
		return new ChainedTransactionManager(transactionManagers);
	}

	final public TransactionUtil getTransactionUtil(PlatformTransactionManager... transactionManagers) {
		PlatformTransactionManager chainedTxmanager = this.getChainedTransactionManager(transactionManagers);
		return TransactionUtilPack.getTransactionUtil(chainedTxmanager);
	}

	final public void executeTransaction(ProcessBlock1 processBlock, PlatformTransactionManager... transactionManagers) {
		TransactionUtil transactionUtil = this.getTransactionUtil(transactionManagers);
		TransactionStatus txStatus = transactionUtil.start();
		try {
			processBlock.execute();
		}catch(Exception e) {
			transactionUtil.rollback(txStatus);
			throw new RuntimeException(e.getMessage());
		}
		transactionUtil.commit(txStatus);
	}
	
	final public <I> void executeTransaction(ProcessBlock2<I> processBlock, I input, PlatformTransactionManager... transactionManagers) {
		TransactionUtil transactionUtil = this.getTransactionUtil(transactionManagers);
		TransactionStatus txStatus = transactionUtil.start();
		try {
			processBlock.execute(input);
		}catch(Exception e) {
			transactionUtil.rollback(txStatus);
			throw new RuntimeException(e.getMessage());
		}
		transactionUtil.commit(txStatus);
	}

	
	final public <R> R executeTransaction(ProcessBlock3<R> processBlock, PlatformTransactionManager... transactionManagers) {
		R result;
		TransactionUtil transactionUtil = this.getTransactionUtil(transactionManagers);
		TransactionStatus txStatus = transactionUtil.start();
		try {
			result = processBlock.execute();
		}catch(Exception e) {
			transactionUtil.rollback(txStatus);
			throw new RuntimeException(e.getMessage());
		}
		transactionUtil.commit(txStatus);
		return result;
	}
	
	final public <I, R> R executeTransaction(ProcessBlock4<I, R> processBlock, I input, PlatformTransactionManager... transactionManagers) {
		R result;
		TransactionUtil transactionUtil = this.getTransactionUtil(transactionManagers);
		TransactionStatus txStatus = transactionUtil.start();
		try {
			result = processBlock.execute(input);
		}catch(Exception e) {
			transactionUtil.rollback(txStatus);
			throw new RuntimeException(e.getMessage());
		}
		transactionUtil.commit(txStatus);
		return result;
	}	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	final public Map<String,Object> extractMap(String data){
		if(this.paramsContext == null) {
			this.paramsContext = new HashMap<>();
			this.paramsContext.put("null",new HashMap<>());
		}
		
		if(StringUtils.isEmpty(data)) {return this.paramsContext.get("null");}
		if(this.paramsContext.containsKey(data)) {return this.paramsContext.get(data);}
		
		try {
			String temp = this.extractString(data);
			if(!this.isJson(temp)) {new RuntimeException("json(map)으로 변환할 수 없습니다.");}
			this.paramsContext.put(data, BaseUtil.getJsonUtil().getMap(temp));		
		}catch(Exception e) {
			throw new SystemException(e.getMessage());
		}

		return this.paramsContext.get(data);
	}

	final public String extractString(Object data){
		String value=null;
		try {
			String temp = String.valueOf(data).trim();
			if(this.isUrlEncoded(temp)) {
					temp = URLDecoder.decode(temp,StandardCharsets.UTF_8.name());
					if(this.isBase64Encoded(temp)) {temp = BaseUtil.getBase64Util().getDecodeBase64String(temp);}
			}
			value = temp.replaceAll("[\\n\\r]", "");
		}catch(Exception e) {
			throw new SystemException(e.getMessage());
		}
		return value;
	}
	
	final public boolean isUrlEncoded(String data) {
        try {
            String decoded = URLDecoder.decode(data, StandardCharsets.UTF_8.name());
            return !data.equals(decoded);
        } catch (Exception e) {
            return false;
        }
    }

    final public boolean isBase64Encoded(String data) {
        return BaseUtil.getBase64Util().isBase64(data);
    }
    
    final public boolean isJson(String data) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.readTree(data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }	
    
    final public void printJobParameters(JobExecution jobExecution) {
		Map<String, Object> map = new TreeMap<>(getParameters(jobExecution));
		
		log.info("--[ jobParameters.decode ]-----------------------------");	
		map.forEach((key, value) -> log.info("    {} = {}",key,value));
		
		log.info("--[ jobParameters.encode ]-----------------------------");	
		map.forEach((key, value) -> log.info("    {} = {}",key,extractString(value)));		
		
		log.info("-------------------------------------------------------");
	}

    final public Map<String, Object> getParameters(JobExecution jobExecution) {
        JobParameters jobParameters = jobExecution.getJobParameters();        
        Map<String, Object> parameterMap = new HashMap<>();
        for (Map.Entry<String, JobParameter> entry : jobParameters.getParameters().entrySet()) {
            parameterMap.put(entry.getKey(), entry.getValue().getValue());
        }
        return parameterMap;
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//	final public static void doLogSifting(JobExecution jobExecution) {
	//		String logSiftingName="";
	//		String jvmAppName=BaseUtil.getSystemUtil().getEnvironment().getProperty("alpha.jvm.app.name");
	//		if(!StringUtils.isEmpty(jvmAppName)) {
	//			String projectName=BaseUtil.getPropertiesUtil().getProperty("project.name");
	//			if(jvmAppName.indexOf(projectName)>-1) {
	//				logSiftingName=jvmAppName.replace(projectName,"").replace("-","");			
	//			}
	//		}
	//		if(StringUtils.isEmpty(logSiftingName)) {
	//			logSiftingName=jobExecution.getJobInstance().getJobName();
	//		}
	//		
	//		MDC.put("logSiftingName",logSiftingName+"-"+jobExecution.getId());
	//		log.debug(">> logSiftingName:{}",MDC.get("logSiftingName"));
	//	}
		
	
	final public void doCallBack(JobExecution jobExecution,Object result){
		try {
			this._doCallBack(jobExecution,result);
		}catch(Exception e) {
			throw new SystemException(e.getMessage());
		}
	}
		
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private void _deleteDir(String dirPath) {
		File dir = new File(FilenameUtils.normalize(dirPath));
		if(!dir.exists() || !dir.isDirectory()) {return;}
		
		File[] list = dir.listFiles();
		for(File f : list) {
			if(f.isDirectory()) {this._deleteDir(f.getPath());}
			f.delete();
			log.info(">> cleanDiscriminatePath: {}",dir.getPath());
		}
		dir.delete();
	}

	private void _doCallBack(JobExecution jobExecution,Object result) throws Exception {
		//jvm 실행 옵션(-Dalpha.agent.trace.token)
		String agentTraceToken = System.getProperty("alpha.agent.trace.token");
		
		// application.yml에 설정되어야 함
		String agentAppMeta = BaseUtil.getPropertiesUtil().getProperty("alpha.agent.appMeta");

		// check1 : agentTraceToken, metaLocation
		boolean isEnable=true; String msg="";
		if(isEnable && StringUtils.isEmpty(agentAppMeta)) {isEnable=false; msg="agentAppMeta(alpha.agent.appMeta) is not exist or empty.";}
		if(isEnable && StringUtils.isEmpty(agentTraceToken)) {isEnable=false; msg="agentTraceToken is not exist or empty.";}
		if(isEnable && agentTraceToken.indexOf(":")==-1) {isEnable=false; msg="agentTraceToken format(processExecuteNo:[nanoTime]) mismatch.";}
		if(!isEnable) {
			log.debug(">> {}",msg);
			return;
		}

		// check2 : metaFile
		String processExecuteNo = agentTraceToken.split(":")[1];
		File metaFile = new File(FilenameUtils.normalize(agentAppMeta+"/"+processExecuteNo));
		if(!metaFile.exists() && metaFile.length() == 0 ) {
			log.debug(">> metaFile({}) is not exist or empty.",metaFile.getCanonicalPath());
			return;
		}
		
		// check3 : metaData
		StringBuilder builder = new StringBuilder();
		try(BufferedReader bufferedReader = new BufferedReader(new FileReader(metaFile))){
			String line = null;
			while((line = bufferedReader.readLine()) != null){builder.append(line);}		
			bufferedReader.close();
		}
		String metaData = builder.toString();				
		if(StringUtils.isEmpty(metaData)) {
			log.debug(">> metaData is null or blank.");
			return;
		}
		
		// check4 : metaMap
		Map<String,Object> metaMap = BaseUtil.getObjectMapperUtil().readValue(metaData, new TypeReference<Map<String,Object>>(){});
		//log.debug(">> metaMap: {}",metaMap);
		if(metaMap == null || metaMap.isEmpty() || !metaMap.containsKey("callbackApiUrl")) {
			log.debug(">> metaMap is null or empty");
			return;
		}
		
		// check5 : callbackApiUrl		
		String callbackApiUrl = String.valueOf(metaMap.get("callbackApiUrl"));
		log.info(">> callbackApiUrl: {}",callbackApiUrl);		
		if(StringUtils.isEmpty(callbackApiUrl)) {
			log.debug(">> callbackApiUrl is null or empty, or key(callbackApiUrl) is missing.");
			return;
		}		

		// callback 전달 데이타
    	long executeTime = jobExecution.getEndTime().getTime()-jobExecution.getStartTime().getTime();
    	
		Map<String,Object> requestData = new HashMap<>();
		requestData.put("jobName", jobExecution.getJobInstance().getJobName());
		requestData.put("jobId", jobExecution.getJobId());
		requestData.put("jobExecuteTime", executeTime);
		requestData.put("jobResult", result);

		BaseUtil.getRestTemplateUtil().postForObject(new URI(callbackApiUrl), requestData, Integer.class);
		
		// metaFile 삭제
		metaFile.delete();

		// 프로세스 단절로 미삭제된 파일 정리
		Date current = new Date(System.currentTimeMillis());
		long oneDayMillis = (24*60*60*1000);		
		File path = new File(agentAppMeta);
		File[] list = path.listFiles();
		for(int i=0 ; i < list.length; i++){
			Date fileDate = new Date(list[i].lastModified()) ;
			long gap = (current.getTime()-fileDate.getTime())/oneDayMillis;
			if(gap>3) {list[i].delete();}
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	문제: SpringBatch에서 파라미터가 없는 경우 이전 실행의 파라미터 값이 재사용되는 버그 발생.
	원인: SpringBatch4부터 존재한 파라미터 처리 관련 문제.
	해결: RunIdIncrementer를 커스터마이징하여 AidRunIdIncrementer 생성
	**/	
	public static class AidRunIdIncrementer extends RunIdIncrementer {
	    private static final String RUN_ID = "run.id";

	    @Override
	    public JobParameters getNext(JobParameters parameters) {
	        JobParameters params = (parameters == null) ? new JobParameters() : parameters;
	        return new JobParametersBuilder()
	                .addLong(RUN_ID, params.getLong(RUN_ID, 0L) + 1)
	                .toJobParameters();
	    }
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 일반적으로 복잡한 처리나 대용량 데이터를 다루는 작업일 경우 전체 소요 시간 및 성능상의 이점을 가져오기 위해 멀티스레드 방식을 사용
	 멀티스레드 처리시 데이터 동기화 이슈가 존재하기 때문에 주의해야 함.
	 
	 스프링병렬처리
	 
	 AsyncItemProcessor/AsyncItemWriter : 
	 	ItemProcessor에게 별도의 스레드가 할당되어 작업을 처리하는 방식
	 	AsyncItemProcessor로부터 AsyncItemWriter가 받는 최종 결과값은 List<Future<T>> 타입이며 비동기 실행이 완료될 때까지 대기.

	 Multi-threaded step : 
	 	단일 step을 chunk단위로 thread를 생성해 분할 처리
		step의 ItemReader는 반드시 thread-safe인지 확인 (JdbcPagingItemReader, JpaPagingItemReader가 thread-safe)
		thread간 Chunk를 공유하지 않고 스레드마다 새로운 chunk가 할당되어 데이터 동기화가 보장
	 
	 Parallel Steps : 
	 	Step마다 스레드가 할당되어 여러 개의 Step을 병렬로 실행하는 방법, flow(step1,step2,step3) -> step4
	 	
	 Partitioning : 
	 	Master/Slave 방식으로 Master가 데이터를 파티셔닝 한 다음 각 파티션에게 스레드를 할당하여 Slave가 독립적으로 작동하는 방식
	 	 	
	 Remote Chunking : 
	 	일종의 분산환경처럼 Step 처리가 여러 프로세스로 분할되어 외부의 다른 서버로 전송되어 처리
	 **/
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}