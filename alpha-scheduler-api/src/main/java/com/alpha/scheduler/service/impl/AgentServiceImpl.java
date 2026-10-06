package com.alpha.scheduler.service.impl;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.alpha.base.BaseUtil;
import com.alpha.base.exception.SystemException;
import com.alpha.base.support.util.BashProcessUtilPack.BashProcessMeta;
import com.alpha.base.support.util.BashProcessUtilPack.BashProcessResponse;
import com.alpha.base.support.util.BashProcessUtilPack.BashProcessStatus;
import com.alpha.scheduler.service.AgentService;
import com.alpha.scheduler.vo.BatchInfoPack.BatchApplication;
import com.alpha.scheduler.vo.BatchInfoPack.BatchExecuteParameter;
import com.alpha.scheduler.vo.ExecuteInfoPack.ExecuteRequest;
import com.alpha.scheduler.vo.ExecuteInfoPack.ExecuteResponse;
import com.alpha.scheduler.vo.ExecuteInfoPack.ExecuteStatus;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Value("${alpha.agent.appMeta}")
	private String appMeta;

	@Value("${alpha.agent.appBase}")
	private String appBase;
	
	@Value("${alpha.agent.appPrefix}")
	private String appPrefix;

	@Value("${spring.profiles.active:local}")
	private String springProfilesActive;
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public ExecuteResponse execute(ExecuteRequest executeReq) {
		if(!this.isEnableProfile()) {return null;}
		
		if(executeReq==null) {return null;}

		// 배치잡 스크립트(shell)
		String script = this.getBatchExecuteScript(executeReq.getJobName());	
		if(StringUtils.isEmpty(script)) {throw new SystemException("script is null or blank.");}
		
		// 배치잡 파라메터 : static 
		Map<String,Object> paramMap = executeReq.getJobParameters();
		
		// 배치잡 파라메터 : dynamic (api호출에 의한 파라메터 구성시 static 으로 요청한 파라메터를 무시함)
		if(!StringUtils.isEmpty(executeReq.getJobParametersApiUrl())) {
			Map<String,Object> _paramMap = this.getBatchJobParametersFromApi(executeReq.getJobParametersApiUrl());
			if(_paramMap!=null && !_paramMap.isEmpty()) {paramMap=_paramMap;}
		}
		
		// 배치잡 파라메터 : (필수) domainScope (배치수행시 사용할 DataSource의 domain 이름으로 콤마(,)로 여러개 가능, domainScope=* 는 디폴트구성을 의미 함.)
		if(paramMap==null) {paramMap = new HashMap<>();}
		if(!paramMap.containsKey("domainScope")) {paramMap.put("domainScope","*");}
		
		List<String> parameters = this.getBatchJobScriptParamList(paramMap);		
		log.debug(">> parameters:{}",parameters);
		
		// 배치잡 실행
		BashProcessResponse bashResponse = BaseUtil.getBashProcessUtil().execute(script, parameters, ()->{
			if(StringUtils.isEmpty(executeReq.getCallbackApiUrl())) {return null;}
			
			Map<String,Object> message = new HashMap<>();
			message.put("jobName", executeReq.getJobName());
			message.put("callbackApiUrl", executeReq.getCallbackApiUrl());
			
			BashProcessMeta meta = new BashProcessMeta();
			meta.setDirectory(appMeta);	
			meta.setMessage(BaseUtil.getJsonUtil().toJson(message));
			
			return meta;
		});
		
		ExecuteResponse response = new ExecuteResponse();
		response.setJobName(executeReq.getJobName());
		response.setProcessExecuteId(bashResponse.getProcessExecuteNo());
		response.setProcessExecuteDate(bashResponse.getProcessExecuteDate());
		response.setConsole(bashResponse.getConsole());
		response.setError(bashResponse.getError());
		response.setExitValue(bashResponse.getExitValue());
		response.setProcessStatus(this.status(bashResponse.getProcessExecuteNo()));

		return response;
	}

	@Override
	public ExecuteStatus status(long processExecuteId) {		
		if(!this.isEnableProfile()) {return null;}
		
		log.debug(">> processExecuteId:{}",processExecuteId);
		
		BashProcessStatus ref = BaseUtil.getBashProcessUtil().status(processExecuteId);
		if(ref==null) {return null;}
		
		ExecuteStatus batchStatus = this.getBatchProcessCmdInfo(ref.getCmd());		
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
	public int stop(long executeId) {
		if(!this.isEnableProfile()) {return -1;}
		
		return BaseUtil.getBashProcessUtil().stop(executeId);
	}	
	
	@Override
	public List<BatchApplication> getBatchApplicationList(){
		if(!this.isEnableProfile()) {return null;}

		log.debug(">> appBase:{}",appBase);
		log.debug(">> appPrefix:{}",appPrefix);

		String dirInfo = BaseUtil.getBashProcessUtil().command("basename -a " + appBase + "/" + appPrefix + "*");
		log.debug(">> dirInfo:{}",dirInfo);
		
		List<BatchApplication> list = new ArrayList<>();
		
		String dir="", script="", content="";
		String[] lines=null, segments=null;
		
		String[] dirNames = dirInfo.split("\n");
		for(int i=0 ; i<dirNames.length ; i++) {
			dir = appBase + "/" + dirNames[i];
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
				//if(segments.length<7) {continue;}
				
				BatchApplication application = new BatchApplication();
				application.setAppName(segments[1]);
				application.setJobName(segments[2]);
				application.setProfile(segments[3]);
				application.setMemory(segments[4]+" "+segments[5]);
				application.setScript(script);
				
				list.add(application);
			}
		}

		return list;
	}	
	
	@Override
	public List<BatchExecuteParameter> getBatchExecuteParameterHist(String appName){
		if(!this.isEnableProfile()) {return null;}
		
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
		if(this.springProfilesActive.contains("local")) {
			log.debug(">> 로컬환경 수행 불가");
			return false;
		}
		return true;
	}
	
	private String getBatchExecuteScript(String batchJobName) {
		String script="";
		List<BatchApplication> list = this.getBatchApplicationList();
		for(BatchApplication application:list) {
			if(application.getJobName().equals(batchJobName)) {
				script=application.getScript();
			}
		}		
		return script;
	}
	
    private ExecuteStatus getBatchProcessCmdInfo(String data) {
    	String[] segments = (data+" |end|").split(" ");
    	
    	ExecuteStatus info = new ExecuteStatus();
    	
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

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}