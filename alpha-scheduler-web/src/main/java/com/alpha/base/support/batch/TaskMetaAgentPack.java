package com.alpha.base.support.batch;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;

import com.alpha.base.BaseUtil;
import com.alpha.base.exception.SystemException;
import com.alpha.base.support.batch.TaskPack.Task;
import com.alpha.base.support.util.BashProcessUtilPack.BashProcessMeta;
import com.alpha.base.support.util.BashProcessUtilPack.BashProcessRequestOption;
import com.alpha.base.support.util.BashProcessUtilPack.BashProcessResponse;
import com.alpha.base.support.util.BashProcessUtilPack.BashProcessStatus;
import com.fasterxml.jackson.core.type.TypeReference;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskMetaAgentPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Data
	@Schema(description="배치어플리이션 정보 객체 : 어플리이션이 구성된 디렉토리를 scan한 정보")
	public static class BatchApplication implements Serializable {
		private static final long serialVersionUID = 1L;
		
		@Schema(description="배치어플리이션 이름")
		private String appName;

		@Schema(description="배치어플리이션 잡이름")
		private String jobName;
		
		@Schema(description="배치어플리이션 패키지타입(jar|war)")
		private String packageType;

		@Schema(description="배치어플리이션 프로파일(dev|prod)")
		private String profile;
	
		@Schema(description="배치어플리이션 구동 메모리")
		private String memory;
	
		@Schema(description="배치어플리이션 수행 스크립트(bash)")
		private String script;
	}

	@Data
	@Schema(description="배치어플리이션 실행파라메터 정보 객체 : 배치어플리이션이 수행된 파라메터정보를 가짐")
	public static class BatchExecuteParameter implements Serializable {
		private static final long serialVersionUID = 1L;			
		private String appName;
		private String jobName;	
		private String executeDate;	
		private String executeParameters;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Data
	@Schema(description="에이전트요청정보 객체")
	public static class AgentRequest implements Serializable {
		private static final long serialVersionUID = 1L;

		@Schema(description="어플리케이션 이름", requiredMode = Schema.RequiredMode.REQUIRED)
		private String appName;	

		@Schema(description="잡파라메터 static")
		private Map<String,Object> jobParameters;

		@Schema(description="잡파라메터 dynamic")
		private String jobParametersApiUrl;

		@Schema(description="잡파라메터 dynamic, overlapYn")
		private String jobParametersApiUrlOverlapYn = "N";

		@Schema(description="수행결과 콜백")
		private String callbackApiUrl;

		@Schema(description="수행 식별번호", hidden = true)
		private String executeNo;

		@Schema(description="수행 동기여부", hidden = true)
		private String executeSyncYn = "N";

		@Schema(description="수행 노트 파일경로", hidden = true)
		private String executeNotePath = "";		
	}

	@Data
	@Schema(description="에이전트응답정보 객체")
	public static class AgentResponse implements Serializable {
		private static final long serialVersionUID = 1L;
		
		@Schema(description="어플리케이션 이름")
		private String appName;
		
		@Schema(description="프로세스 실행번호 : agent를 통해 수행된 jvm 프로세스 구별 값")	
		private long processExecuteNo;	
		
		@Schema(description="프로세스 실행일시")
		private Date processExecuteDate;

		@Schema(description="프로세스 실행노트", hidden = true)
		private String processExecuteNote;

		@Schema(description="프로세스 실행로그파일", hidden = true)
		private Set<String> processExecuteLogFile;
		
		@Schema(description="콘솔 출력 : agent를 통해 수행된 프로세스의 콘솔 출력 값")
		private String console;	
		
		@Schema(description="오류 출력 : agent를 통해 수행된 프로세스의 오류 출력 값")
		private String error;	
			
		@Schema(description="종료 값 : agent를 통해 수행된 프로세스의 종료 값, if(exitValue==0) 정상처리 else 비정상처리")
		private String exitValue;
		
		@Schema(description="프로세스 실행상태 객체")
		private AgentResponse.ProcessStatus processStatus;
		
		@Data
		@Schema(description="메타실행 상태 객체")
		public static class ProcessStatus implements Serializable {
			private static final long serialVersionUID = 1L;
			
			@Schema(description="프로세스 user")
			private String uid;	
			
			@Schema(description="프로세스 번호")
			private String pid;	
			
			@Schema(description="프로세스 부모번호")
			private String ppid;	
			
			@Schema(description="프로세스 cpu사용률")
			private String cpu;	
			
			@Schema(description="프로세스 시작시간")
			private String stime;	
			
			@Schema(description="프로세스 수행터미널번호")
			private String tty;	
			
			@Schema(description="프로세스 수행시간")
			private String time;	
			
			@Schema(description="프로세스 명령어")
			private String cmd;	
			
			@Schema(description="프로세스 jvm")
			private String jvm;	
			
			@Schema(description="프로세스 jvm에서 실행되는 jar어플리케이션(스프링배치) 패키지")
			private String jar;	
			
			@Schema(description="프로세스 jvm에서 할당 메모리")
			private String memory;
			
			@Schema(description="프로세스 jvm에서 실행되는 jar어플리케이션(스프링배치) spring.profiles.active")
			private String profile;	
			
			@Schema(description="프로세스 jvm에서 실행되는 jar어플리케이션(스프링배치) spring.batch.job.names")
			private String jobName;

			@Schema(description="프로세스 jvm에서 실행되는 jar어플리케이션(스프링배치) jobParameters-encode")
			private String jobParameters;
			
			@Schema(description="프로세스 jvm에서 실행되는 jar어플리케이션(스프링배치) jobParameters-decode")
			private String jobParametersDecode;
		}
		
	}

	public static class AgentExecuteException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public AgentExecuteException(String message) {super(message);}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@FunctionalInterface
	public interface AgentProcess {public abstract void execute(Task task);}
	
	public interface TaskMetaAgent {
		public void setAppMeta(String appMeta);
		public void setAppBase(String appBase);
		public void setAppPrefixList(List<String> appPrefixList);
		public void setSpringProfilesActive(String springProfilesActive);
				
		public AgentResponse execute(AgentRequest request);		
		public AgentResponse execute(AgentRequest request, String executeScript);		
		public AgentResponse.ProcessStatus status(long executeId);
		public int stop(long executeNo);

		public List<BatchApplication> getBatchApplicationList();
		public List<BatchExecuteParameter> getBatchExecuteParameterHist(String appName);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static TaskMetaAgent getTaskMetaAgent(BatchUtil batchUtil) {
		
		return new TaskMetaAgent () {

		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////			

			private String appBase;
			private String appMeta;
			private List<String> appPrefixList;
			private String springProfilesActive;
			
		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////			

			@Override
			public void setAppBase(String appBase) {this.appBase = appBase;}

			@Override
			public void setAppMeta(String appMeta) {this.appMeta = appMeta;}

			@Override
			public void setAppPrefixList(List<String> appPrefixList) {this.appPrefixList = appPrefixList;}

			@Override
			public void setSpringProfilesActive(String springProfilesActive) {this.springProfilesActive = springProfilesActive;}
			
			@Override
			public AgentResponse execute(AgentRequest executeReq) {
				if(!this.isEnableProfile()) {return null;}
				if(executeReq==null) {return null;}
				return this.execute(executeReq,this.getBatchExecuteScript(executeReq.getAppName()));
			}
			
			@Override
			public AgentResponse execute(AgentRequest executeReq, String script) {
				if(!this.isEnableProfile()) {throw new RuntimeException("로컬환경 수행 불가");}
				if(executeReq==null) {return null;}

				// 스크립트(shell)
				if(StringUtils.isEmpty(script)) {throw new SystemException("script is null or blank.");}
				
				// 파라메터 : static 
				Map<String,Object> paramMap = executeReq.getJobParameters();
				
				// 파라메터 : dynamic
				if(!StringUtils.isEmpty(executeReq.getJobParametersApiUrl())) {
					Map<String,Object> _paramMap = this.getBatchJobParametersFromApi(executeReq.getJobParametersApiUrl());
					if(_paramMap!=null && !_paramMap.isEmpty()) {
						if(executeReq.getJobParametersApiUrlOverlapYn().equals("Y")) {
							paramMap.putAll(_paramMap);
						} else {
							paramMap=_paramMap;							
						}
					}
				}
				
				// 파라메터 : mandatory, (domainScope : 배치수행시 사용할 dataSource의 domain 이름으로 콤마(,)로 여러개 가능, domainScope=* 는 디폴트구성을 의미 함.)
				if(paramMap==null) {paramMap = new HashMap<>();}
				if(!paramMap.containsKey("domainScope")) {paramMap.put("domainScope","*");}
				
				List<String> parameters = this.getBatchJobScriptParamList(paramMap);	
				//log.debug(">> parameters:{}",parameters);
				
				// 타스크수행 식별번호 : 스케쥴러에서 발행한 유일한 번호			
				long metaExecuteNo=-1L;
				if(!StringUtils.isEmpty(executeReq.getExecuteNo())){
					metaExecuteNo = Long.parseLong(executeReq.getExecuteNo());
				}
				
				// BashProcess 실행옵션
				BashProcessRequestOption option = new BashProcessRequestOption();
				option.setMetaExecuteNo(metaExecuteNo);
				option.setMetaExecuteSyncYn(executeReq.getExecuteSyncYn());
				option.setMetaExecuteNotePath(executeReq.getExecuteNotePath());
				
				// BashProcess 실행
				BashProcessResponse bashResponse = BaseUtil.getBashProcessUtil().execute(script, parameters, option, ()->{
					if(StringUtils.isEmpty(executeReq.getCallbackApiUrl())) {return null;}
					
					Map<String,Object> message = new HashMap<>();
					message.put("appName", executeReq.getAppName());
					message.put("callbackApiUrl", executeReq.getCallbackApiUrl());
					
					BashProcessMeta meta = new BashProcessMeta();
					meta.setDirectory(appMeta);	
					meta.setMessage(BaseUtil.getJsonUtil().toJson(message));
					
					return meta;
				});
				
				AgentResponse response = new AgentResponse();
				response.setAppName(executeReq.getAppName());
				response.setProcessExecuteNo(bashResponse.getProcessExecuteNo());
				response.setProcessExecuteDate(bashResponse.getProcessExecuteDate());
				response.setConsole(bashResponse.getConsole());				
				response.setError(bashResponse.getError());				
				response.setExitValue(bashResponse.getExitValue());
				response.setProcessStatus(this.status(bashResponse.getProcessExecuteNo()));
				
				return response;
			}

			@Override
			public AgentResponse.ProcessStatus status(long processExecuteNo) {		
				if(!this.isEnableProfile()) {throw new RuntimeException("로컬환경 수행 불가");}
				
				//log.debug(">> processExecuteNo:{}",processExecuteNo);
				
				BashProcessStatus ref = BaseUtil.getBashProcessUtil().status(processExecuteNo);
				if(ref==null) {return null;}
				
				AgentResponse.ProcessStatus batchStatus = this.getBatchProcessCmdInfo(ref.getCmd());		
				batchStatus.setUid(ref.getUid());
				batchStatus.setPid(ref.getPid());
				batchStatus.setPpid(ref.getPpid());
				batchStatus.setCpu(ref.getCpu());
				batchStatus.setStime(ref.getStime());
				batchStatus.setTty(ref.getTty());
				batchStatus.setTime(ref.getTime());
				batchStatus.setCmd(ref.getCmd());

				return batchStatus;
			}
			
			@Override
			public int stop(long executeNo) {
				if(!this.isEnableProfile()) {throw new RuntimeException("로컬환경 수행 불가");}			
				return BaseUtil.getBashProcessUtil().stop(executeNo);
			}	
			
			@Override
			public List<BatchApplication> getBatchApplicationList(){
				if(!this.isEnableProfile()) {throw new RuntimeException("로컬환경 수행 불가");}

				//log.debug(">> appBase:{}",appBase);
				
				List<String> dirNames = new ArrayList<>();
				this.appPrefixList.forEach(prefix->{
					String dirInfo = BaseUtil.getBashProcessUtil().command("basename -a " + appBase + "/" + prefix + "*");
					Collections.addAll(dirNames, dirInfo.split("\n"));
				});
				
				List<BatchApplication> list = new ArrayList<>();
				
				String dir="", script="", content="";
				String[] lines=null, segments=null;
				
				for(String path : dirNames) {
					dir = appBase + "/" + path;
					script = BaseUtil.getBashProcessUtil().command("basename -a " + dir + "/*.execute.sh");			
					script = dir + "/" + script.replace("\n","");
					log.debug(">> script:{}",script);
					
					content = BaseUtil.getBashProcessUtil().command("cat " + script);	
					if(StringUtils.isEmpty(content)) {continue;}
					log.debug(">> content:{}",content);
					
					lines = content.split("\n");
					for(int j=0 ; j<lines.length ; j++) {	
						if(!lines[j].contains("fnc_execute")) {continue;}
						
						segments = lines[j].replace("\"","").split(" ");

						BatchApplication application = new BatchApplication();
						application.setAppName(segments[1]);
						application.setPackageType(segments[2]);				
						application.setJobName(segments[3]);
						application.setProfile(segments[4]);
						application.setMemory(this.getDefaultMemory(content));
						application.setScript(script);
						
						list.add(application);
					}
				}

				list.forEach(i->log.debug(">> {}",i));
				
				return list;
			}	
			
			@Override
			public List<BatchExecuteParameter> getBatchExecuteParameterHist(String appName){
				if(!this.isEnableProfile()) {throw new RuntimeException("로컬환경 수행 불가");}		
				if(StringUtils.isEmpty(appName)) {return null;}
				
				String workDir = appBase + "/"+appName + "/parameter";
				String parameterInfo = BaseUtil.getBashProcessUtil().command("basename -a " + workDir +"/*.parameter");
				if(StringUtils.isEmpty(parameterInfo)) {return null;}
				
				List<BatchExecuteParameter> list = new ArrayList<>();
				
				String content="", jobName="", executeDate="", executParameters="";
				
				String[] parameterFiles = parameterInfo.split("\n");

				for(int i=0 ; i<parameterFiles.length ; i++) {
					content = BaseUtil.getBashProcessUtil().command("cat " + workDir + "/" + parameterFiles[i]);
					//log.debug(">> content: {}",content);
					
					if(StringUtils.isEmpty(content)) {continue;}
					
					if(content.indexOf(" ")>-1) {
						jobName = content.substring(0,content.indexOf(" ")).replace("[","").replace("]","");				
						executParameters = content.substring(content.indexOf(" ")).trim().replace("\"","");
					}else {
						jobName = content.replace("[","").replace("]","");
					}
					
					executeDate = parameterFiles[i].replace(".parameter","").replace(appName,"").replace("-","");			

					BatchExecuteParameter parameter = new BatchExecuteParameter();
					parameter.setAppName(appName);
					parameter.setJobName(jobName);
					parameter.setExecuteDate(executeDate);
					parameter.setExecuteParameters(executParameters);
					
					list.add(parameter);
				}

				return list;
			}	
			
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			
			private boolean isEnableProfile() {
				return this.springProfilesActive.contains("local")?false:true;
			}
			
			private String getBatchExecuteScript(String appName) {
				String script="";
				List<BatchApplication> list = this.getBatchApplicationList();
				for(BatchApplication application:list) {
					if(application.getAppName().equals(appName)) {
						script=application.getScript();
					}
				}		
				return script;
			}
			
		    private String getDefaultMemory(String data) {
		    	
		        String xms="-Xms128m";
		        Matcher matcherXms = Pattern.compile("-Xms(\\d+)(m|g)").matcher(data);        
		        while (matcherXms.find()) {xms = "-Xms"+matcherXms.group(1)+matcherXms.group(2);}

		        String xmx="-Xmx256m";
		        Matcher matcherXmx = Pattern.compile("-Xmx(\\d+)(m|g)").matcher(data);        
		        while (matcherXmx.find()) {xmx = "-Xmx"+matcherXmx.group(1)+matcherXmx.group(2);}

		        String jvmMemory = xms+" "+xmx;
		        
		        return jvmMemory;
		    }
		    	
		    private AgentResponse.ProcessStatus getBatchProcessCmdInfo(String data) {
		    	String[] segments = (data+" |end|").split(" ");
		    	
		    	AgentResponse.ProcessStatus info = new AgentResponse.ProcessStatus();
		    	
		    	for(int i=0;i<segments.length;i++) {    			
		        	if(segments[i].indexOf("java")>-1) {
		        		info.setJvm(segments[i]);
		        	}
		        	if(segments[i].indexOf("-jar")>-1) {
		        		info.setJar(segments[i+1]);
		        	}
		        	if(segments[i].indexOf("-Xms")>-1) {
		        		String memory = StringUtils.isEmpty(info.getMemory())?"":info.getMemory()+" ";
		        		info.setMemory(memory+segments[i]);
		        	}
		        	if(segments[i].indexOf("-Xmx")>-1) {
		        		String memory = StringUtils.isEmpty(info.getMemory())?"":info.getMemory()+" ";
		        		info.setMemory(memory+segments[i]);
		        	}
		        	if(segments[i].indexOf("--spring.profiles.active")>-1) {
		        		info.setProfile(segments[i].substring(segments[i].indexOf("=")+1));
		        	}
		        	if(segments[i].indexOf("--spring.batch.job.names")>-1) {
		        		info.setJobName(segments[i].substring(segments[i].indexOf("=")+1));
		        	}
		        	if(segments[i].indexOf("--spring.batch.job.names")>-1) {
		        		String jobParameters="";
		        		for(int j=(i+1);j<segments.length;j++) {
		        			if(!segments[j].trim().equals("|end|")) {jobParameters+=segments[j]+" ";}
		        		}
		        		jobParameters=jobParameters.trim();        		
		        		info.setJobParameters(jobParameters);
		        		info.setJobParametersDecode(this.decodeBatchParam(jobParameters));
		        	}
		    	}

		    	return info;
		    }
		    
		    private Map<String,Object> getBatchJobParametersFromApi(String apiUrl){
		    	if(StringUtils.isEmpty(apiUrl)) {return null;}		    	
		    	Map<String,Object> result = null;		    	
				try {
					URI apiUri = new URI(apiUrl);			
			    	ResponseEntity<Object> responseEntity = BaseUtil.getRestTemplateUtil().getForEntity(apiUri, Object.class);
					if(responseEntity!=null) {
						result = BaseUtil.getObjectMapperUtil().convert(responseEntity.getBody(), new TypeReference<Map<String,Object>>() {});
					}
				} catch (URISyntaxException e) {
					throw new SystemException(e.getMessage());
				}
		    	return result;
		    }
		    
		    
			private List<String> getBatchJobScriptParamList(Map<String,Object> paramMap) {
				List<String> paramList = new ArrayList<>();
				paramList.add("-c"); //배치스크립트 confirm 입력 skip
						
				paramList.add("-p"); //batch 어플리케이션 실행 스크립트(shell)에서 -p 다음에 오는 json을 jobParametrs로 전달 함.
				if(paramMap==null || paramMap.isEmpty()) {
					paramList.add("-"); //파라메터가 없을 경우, "-"로 입력한다 실행 스크립트(shell)에서 -p 다음 "-"가 오면 파라메터가 없는 것으로 처리
					return paramList;
				}

				try {
					String value="";
					Map<String,String> map = new HashMap<>();
					
					for(String key : paramMap.keySet()) {
						Object obj = paramMap.get(key);

						boolean isPlain=false;
						if(!isPlain && obj instanceof String) {isPlain=true;}
						if(!isPlain && obj instanceof Boolean) {isPlain=true;}
						if(!isPlain && obj instanceof Integer) {isPlain=true;}
						if(!isPlain && obj instanceof Long) {isPlain=true;}				
						if(isPlain) {
							value = String.valueOf(obj);
						} else {
							value = BaseUtil.getJsonUtil().toJson(obj);
							value = BaseUtil.getBase64Util().getEncodeBase64String(value);	
							value = URLEncoder.encode(value,"UTF-8");					
						}
						map.put(key, value);
					}
					value = BaseUtil.getJsonUtil().toJson(map);
					value = value.replace("\"","\\\"");
					paramList.add(value);
					
				} catch (Exception e) {
					throw new SystemException(e.getMessage());
				}
				
				return paramList;
			}
			
		    private String decodeBatchParam(String params){
		    	StringBuilder builder = new StringBuilder();
		    	
		    	String[] segments = params.trim().split(" ");    	
		    	for(int i=0;i<segments.length;i++) {
		    		if(segments[i].indexOf("=")==-1) {continue;}

		    		String[] unit = segments[i].split("=");
		    		String key = unit.length>0?unit[0]:"";    		
		    		String value = unit.length>1?unit[1]:"";

					try {
						if(!value.equals("")) {

							String decodedValue = BaseUtil.getBase64Util().getDecodeBase64String(URLDecoder.decode(value,"UTF-8"));
							String encodedValue = URLEncoder.encode(BaseUtil.getBase64Util().getEncodeBase64String(decodedValue),"UTF-8");

							boolean isDecode = true;
							if(isDecode && decodedValue.equals("")) {isDecode=false;}
							if(isDecode && decodedValue.equals(encodedValue)) {isDecode=false;}
							if(isDecode && !value.equals(encodedValue)) {isDecode=false;}
							if(isDecode) {
								value = decodedValue;
							}
						}
					} catch (UnsupportedEncodingException e) {
						throw new SystemException(e.getMessage());
					}
		    		
					builder.append(key).append("=").append(value).append(" ");
		    	}
		    	
		    	String result = builder.toString().trim();
		    	
				return result;
		    } 			
		};

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////			

	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}