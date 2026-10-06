package com.alpha.base.support.batch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskConfigPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static interface TaskConfig extends Cloneable {

	    public static final String CONFIG_FILE_EXT_DEFAULT = "cfg";

		public TaskConfig clone();	    
		public void setLock(String lockYn);
		public TaskConfig doDefault(TaskConfig defaultConfig);

		public BatchUtil getBatchUtil();
		public void setBatchUtil(BatchUtil batchUtilMeta);

		public String getSaveImmediatelyYn();
		public void setSaveImmediatelyYn(String saveImmediatelyYn);

		public String getConfigDataJson();
		public void setConfigDataJson(String configDataJson);

		public String getConfigLocation();
		public void setConfigLocation(String configLocation);

		public String getConfigFileExt();
		public void setConfigFileExt(String configFileExt);

		public Properties getBizProps();
		public void setBizProps(Properties bizProps);
		public void updateBizProps(String key,String value);

		public Properties getBizPropsDesc();
		public void setBizPropsDesc(Properties bizPropsDesc);

		public String getJobParametersApiUrl();
		public void setJobParametersApiUrl(String jobParametersApiUrl);
		public String getJobParametersApiUrlEnableYn();
		public void setJobParametersApiUrlEnableYn(String jobParametersApiUrlEnableYn);
		public String getJobParametersApiUrlOverlapYn();
		public void setJobParametersApiUrlOverlapYn(String jobParametersApiUrlOverlapYn);

		public String getCallbackApiUrl();
		public void setCallbackApiUrl(String callbackApiUrl);
		public String getCallbackApiUrlEnableYn();
		public void setCallbackApiUrlEnableYn(String callbackApiUrlEnableYn);

		public String getJvmMemory();
		public void setJvmMemory(String jvmMemory);
		public String getJvmMemoryEnableYn();
		public void setJvmMemoryEnableYn(String jvmMemoryEnableYn);

		public String getScheduleTermEnableYn();
		public void setScheduleTermEnableYn(String scheduleTermEnableYn);

		public String getScheduleTermDesc();
		public boolean isScheduleTermAlive();
		public boolean isScheduleTermValid();
		
		public List<Term> getScheduleTermList();
		public void setScheduleTermList(List<Term> scheduleTermList);
		
		public void addScheduleTerm(String start,String end);
		public void removeScheduleTerm(String start,String end);
		
		public String getScheduleType();
		public void setScheduleType(String scheduleType);

		public String getScheduleValue();
		public void setScheduleValue(String scheduleValue);

		public String getLogEnableYn();
		public void setLogEnableYn(String logEnableYn);

		public String getLogExceptEnableYn();
		public void setLogExceptEnableYn(String logExceptEnableYn);

		public List<String> getLogExceptPatternList();
		public void setLogExceptPatternList(List<String> logExceptPatternList);
		public void addLogExceptPattern(String value);
		public void removeLogExceptPattern(String value);

		public Properties getLogPolicyProps();
		public void setLogPolicyProps(Properties logPolicyProps);
		public void updateLogPolicyProps(String key,String value);

		public String getExecutionLogFilePathPattern();
		public void setExecutionLogFilePathPattern(String executionLogFilePathPattern);

		public String getExecutionLogLevel();
		public void setExecutionLogLevel(String executionLogLevel);

		public String getExceptionLogFilePathPattern();
		public void setExceptionLogFilePathPattern(String exceptionLogFilePathPattern);

		public String getExceptionLogLevel();
		public void setExceptionLogLevel(String exceptionLogLevel);

		public List<String> getRunnableServerList();    
		public void setRunnableServerList(List<String> executionServerList);     
		public boolean isRunnableServer();

		public String getRunnableServerCheckEnableYn();
		public void setRunnableServerCheckEnableYn(String runnableServerCheckEnableYn);

		public void loadConfigData(String taskName);
		public void loadConfigData(String taskName, Set<String> configKeySet);

		public void saveConfigData(String taskName);
		public void saveConfigData(String taskName,Set<String> configKeySet);

		public void deleteConfigData(String taskName);
		public String touchConfigData(String taskName, long timeMills);

	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Data
	public static class ConfigData implements Serializable {
		private static final long serialVersionUID=1L;

		private String saveImmediatelyYn = "Y";
		
		private Properties bizProps;
		private Properties bizPropsDesc;

		private String jobParametersApiUrl;
		private String jobParametersApiUrlOverlapYn = "N";
		private String jobParametersApiUrlEnableYn = "N";

		private String callbackApiUrl;
		private String callbackApiUrlEnableYn = "N";
		
		private String jvmMemory;
		private String jvmMemoryEnableYn = "N";

		private String scheduleTermEnableYn = "N";	
		private String scheduleTermDesc = "";		
		private List<Term> scheduleTermList;
		
		private String scheduleType;
		private String scheduleValue;
		
		private String configFileExt = TaskConfig.CONFIG_FILE_EXT_DEFAULT;
		private String configLocation;
		
		private String executionLogFilePathPattern;
		private String executionLogLevel;
		
		private String exceptionLogFilePathPattern;
		private String exceptionLogLevel;

		private String logEnableYn = "Y";
		private String logExceptEnableYn = "N";	
		private List<String> logExceptPatternList;
		
		private Properties logPolicyProps;
		
		private String runnableServerCheckEnableYn = "Y";
		private List<String> runnableServerList;	
	
	}

	@Data
	public static class Term implements Serializable {
		private static final long serialVersionUID=1L;
		
		private String start;
		private String end;
		private String value;
		private long startTimestamp;
		private long endTimestamp;
		
		public Term(String start,String end) {
			this.start=start;
			this.end=end;
			this.value=start+"|"+end;	
			this.startTimestamp=this.getTimestamp(start);
			this.endTimestamp=this.getTimestamp(end)+(24*60*60*1000L)-1;
		}
		public String getStart() {return this.start;}		
		public String getEnd() {return this.end;}
		public String getValue() {return this.value;}
		
		public String getStatus() {
			long currentTimestamp = Instant.now().toEpochMilli();
			if(this.startTimestamp <= currentTimestamp && currentTimestamp <= this.endTimestamp) {return "적용";}	        
			else if(this.startTimestamp > currentTimestamp) {return "대기";}
			else if(this.endTimestamp < currentTimestamp) {return "만료";}
			return null;
		}
		public boolean isAlive() {
			long currentTimestamp = Instant.now().toEpochMilli();
			return this.endTimestamp <= currentTimestamp?false:true;
		}			
		public boolean isValid() {
			long currentTimestamp = Instant.now().toEpochMilli();
			return (this.startTimestamp <= currentTimestamp && currentTimestamp <= this.endTimestamp)?true:false;				
		}
		private long getTimestamp(String value) {
	        return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
		}
	}		

	public static class TaskProperties extends Properties {
		private static final long serialVersionUID = 1L;

		private Properties desc = new Properties();
		
		public void setProperty(String key,String value,String description) {
			this.put(key, value);
			this.desc.put(key, description);
		}
		
		public Properties getBizProps() {return this;}		
		public Properties getBizPropsDesc() {return this.desc;}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static TaskConfig getTaskConfig(TaskConfig taskConfig) {
		return getTaskConfig(taskConfig, null);
	}

	public static TaskConfig getTaskConfig(TaskConfig taskConfig, Properties props) {
		Properties bizProps = props;
		Properties bizPropsDesc = null;		
		if(props instanceof TaskProperties) {
			TaskProperties taskProperties = (TaskProperties)props;
			bizProps = taskProperties.getBizProps();
			bizPropsDesc = taskProperties.getBizPropsDesc();
		}
		return getTaskConfig(taskConfig, bizProps, bizPropsDesc);		
	}

	public static TaskConfig getTaskConfig(TaskConfig taskConfig, Properties bizProps, Properties bizPropsDesc) {		
		TaskConfig newTaskConfig = getTaskConfig(taskConfig.getBatchUtil());
		newTaskConfig.doDefault(taskConfig);
		if(bizProps!=null) {
			newTaskConfig.setBizProps(bizProps);
			newTaskConfig.setBizPropsDesc(bizPropsDesc);
		}
		return newTaskConfig;
	}
	
	public static TaskConfig getTaskConfig(BatchUtil _batchUtil) {
		
		return new TaskConfig() {
			
		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			private BatchUtil batchUtil = _batchUtil;
			private ConfigData configData;
			private String configDataJson;	
			private String lockYn = "N";

		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			@Override
		    public TaskConfig doDefault(TaskConfig refTaskConfig) {
				// 참조할 refTaskConfig는 다른 task의 스케쥴에 적용되어 있을 수 있으며, 속성(프로퍼티,로그정책등)이 언제든 변경 될 수 있다.
				// refTaskConfig을 default로 간주하여 this(생성된 TaskConfig)객체에 속성을 복제시 무결성을 보장하기 위해 clone객체를 사용하고 사용 후 null처리 한다.
				// String객체는 clone시 deepCopy되며, 복재할 속성인 configDataJson만 해당한다.
				// ConfigData는 configDataJson 사용하여 새로 재구성된다.
				// batchUtil는 이전객체(refTaskConfig)로 부터 참조하여 set한다. (deepCopy할 필요없음)
				// lock처리를 통하여 configData구성시 getConfigDataJson() 호출시 판단처리(configDataJson or configData->configDataJson)
				
		        TaskConfig config = refTaskConfig.clone();
		        String configDataJson = config.getConfigDataJson();
		        
		        config.setLock("Y");
		        this.setBatchUtil(config.getBatchUtil());
		        this.setConfigData(configDataJson);
		        config.setLock("N");
		        config = null;
		        
		        return this;
		    }

		    @Override
		    public TaskConfig clone() {
		    	TaskConfig cloneObj=null;  
		        try {cloneObj=(TaskConfig)super.clone();} 
		        catch (CloneNotSupportedException e) {return null;}
		        return cloneObj;
		    }
		    
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		    @Override
		    public void setLock(String lockYn) {
		    	this.lockYn = lockYn;
		    }

		    @Override
		    public BatchUtil getBatchUtil() {
		    	return this.batchUtil;
		    }

		    @Override
		    public void setBatchUtil(BatchUtil batchUtil) {
		    	this.batchUtil = batchUtil;
		    }

		    @Override
		    public String getConfigDataJson() {
		    	return this.lockYn.equals("Y")?this.configDataJson:this.batchUtil.getJsonUtil().toJson(this.getConfigData());
		    }

		    @Override
		    public void setConfigDataJson(String configDataJson) {
		    	this.configDataJson = configDataJson;
		    }

		    @Override
		    public String getConfigLocation() {
		    	return this.getConfigData().getConfigLocation();
		    }	

		    @Override
		    public void setConfigLocation(String configLocation) {
		    	this.getConfigData().setConfigLocation(configLocation);
		    }

		    @Override
		    public String getConfigFileExt() {
		    	return this.getConfigData().getConfigFileExt();
		    }	

		    @Override
		    public void setConfigFileExt(String configFileExt) {
		    	this.getConfigData().setConfigFileExt(configFileExt);
		    }

		    @Override
		    public Properties getBizProps() {
		    	return this.getConfigData().getBizProps();
		    }    

		    @Override
		    public void setBizProps(Properties bizProps) {
		    	this.getConfigData().setBizProps(bizProps);
		    }

		    @Override
		    public void updateBizProps(String key,String value) {
		    	if(key != null && !key.equals("")) {this.getBizProps().put(key,(value.equals("null")?"":value));}
		    }   

		    @Override
		    public Properties getBizPropsDesc() {
		    	return this.getConfigData().getBizPropsDesc();
		    }   

		    @Override
		    public void setBizPropsDesc(Properties bizPropsDesc) {
		    	this.getConfigData().setBizPropsDesc(bizPropsDesc instanceof TaskProperties?((TaskProperties)bizPropsDesc).getBizPropsDesc():bizPropsDesc);
		    }

		    @Override
		    public String getJobParametersApiUrl() {
		    	return this.getConfigData().getJobParametersApiUrl();
		    }

		    @Override
		    public void setJobParametersApiUrl(String jobParametersApiUrl) {
		    	this.getConfigData().setJobParametersApiUrl(jobParametersApiUrl);
		    }
		    
		    @Override
		    public String getJobParametersApiUrlEnableYn() {
		    	return this.getConfigData().getJobParametersApiUrlEnableYn();
		    }

		    @Override
		    public void setJobParametersApiUrlEnableYn(String jobParametersApiUrlEnableYn) {
		    	this.getConfigData().setJobParametersApiUrlEnableYn(jobParametersApiUrlEnableYn);
		    }		    	

		    @Override
		    public String getJobParametersApiUrlOverlapYn() {
		    	return this.getConfigData().getJobParametersApiUrlOverlapYn();
		    }
		    
		    @Override
		    public void setJobParametersApiUrlOverlapYn(String jobParametersApiUrlOverlapYn) {
		    	this.getConfigData().setJobParametersApiUrlOverlapYn(jobParametersApiUrlOverlapYn);		    	
		    }

		    @Override
		    public String getCallbackApiUrl() {
		    	return this.getConfigData().getCallbackApiUrl();
		    }
		    
		    @Override
		    public void setCallbackApiUrl(String callbackApiUrl) {
		    	this.getConfigData().setCallbackApiUrl(callbackApiUrl);		    	
		    }
		    
		    @Override
		    public String getCallbackApiUrlEnableYn() {
		    	return this.getConfigData().getCallbackApiUrlEnableYn();		    
		    }

		    @Override
		    public void setCallbackApiUrlEnableYn(String callbackApiUrlEnableYn) {
		    	this.getConfigData().setCallbackApiUrlEnableYn(callbackApiUrlEnableYn);		    		    	
		    }
  
		    @Override
		    public String getJvmMemory() {
		    	return this.getConfigData().getJvmMemory();
		    }
		    
		    @Override
		    public void setJvmMemory(String jvmMemory) {
		    	this.getConfigData().setJvmMemory(jvmMemory);
		    }

		    @Override
		    public String getJvmMemoryEnableYn() {
		    	return this.getConfigData().getJvmMemoryEnableYn();		    	
		    }

		    @Override
		    public void setJvmMemoryEnableYn(String jvmMemoryEnableYn) {
		    	this.getConfigData().setJvmMemoryEnableYn(jvmMemoryEnableYn);		    	
		    }

		    @Override
			public String getScheduleTermEnableYn() {
				return this.getConfigData().getScheduleTermEnableYn();
			}
		    
		    @Override
			public boolean isScheduleTermAlive() {
		    	List<Term> list = this.getScheduleTermList();
		    	boolean isEnable = false;
		    	if(!isEnable && this.getScheduleTermEnableYn().equals("N")) {isEnable=true;}
		    	if(!isEnable && (list==null || list.isEmpty())) {isEnable=true;}
		    	if(!isEnable) {for(Term term : list) {if(term.isAlive()) {isEnable=true;break;}}}
		    	return isEnable;
			}
			
		    @Override
			public boolean isScheduleTermValid() {
		    	List<Term> list = this.getScheduleTermList();
		    	boolean isEnable = false;
		    	if(!isEnable && this.getScheduleTermEnableYn().equals("N")) {isEnable=true;}
		    	if(!isEnable && (list==null || list.isEmpty())) {isEnable=true;}
		    	if(!isEnable) {for(Term term : list) {if(term.isValid()) {isEnable=true;break;}}}
		    	
		    	return isEnable;
			}
			
		    @Override
			public void setScheduleTermEnableYn(String scheduleTermEnableYn) {
				this.getConfigData().setScheduleTermEnableYn(scheduleTermEnableYn);				
			}

		    @Override
			public String getScheduleTermDesc() {
		    	return this.getConfigData().getScheduleTermDesc();
		    }
		    
		    @Override
			public List<Term> getScheduleTermList(){		    	
		    	return this.getConfigData().getScheduleTermList();
			}
			
		    @Override
			public void setScheduleTermList(List<Term> scheduleTermList) {
		    	this.getConfigData().setScheduleTermList(scheduleTermList);				
			}
			
		    @Override
			public void addScheduleTerm(String start,String end) {
		    	List<Term> list = this.getScheduleTermList()==null?new ArrayList<>():this.getScheduleTermList();	
		    	list = this.getAddOnList(list,new Term(start,end));
		    	list.sort(Comparator.comparing(t -> t.value));		    	
		    	this.setScheduleTermList(list);
		    	
		    	StringBuilder builder = new StringBuilder();
		    	list.forEach(i->builder.append(i.getStart().replace("-",".")).append(" - ").append(i.getEnd().replace("-",".")).append("\n"));
		    	String info = builder.toString().trim();
		    	this.getConfigData().setScheduleTermDesc(info);		    	
		    }
		    
		    @Override
			public void removeScheduleTerm(String start,String end) {
		    	List<Term> list = this.getScheduleTermList()==null?new ArrayList<>():this.getScheduleTermList();
		    	if(list==null || list.isEmpty()) {return;}
		    	list = this.getRemoveOnList(list, new Term(start,end));
		    	list.sort(Comparator.comparing(t -> t.value));
		    	this.setScheduleTermList(list);		    	

		    	String info="";
		    	if(!list.isEmpty()) {
			    	StringBuilder builder = new StringBuilder();
			    	list.forEach(i->builder.append(i.getStart().replace("-",".")).append(" - ").append(i.getEnd().replace("-",".")).append("\n"));
			    	info = builder.toString().trim();	    		
		    	}
		        
		    	this.getConfigData().setScheduleTermDesc(info);	    	
		    }
		    
		    @Override
		    public String getScheduleType() {
		    	return this.getConfigData().getScheduleType();
		    }

		    @Override
		    public void setScheduleType(String scheduleType) {
		    	this.getConfigData().setScheduleType(scheduleType);
		    }

		    @Override
		    public String getScheduleValue() {
		    	return this.getConfigData().getScheduleValue();
		    }

		    @Override
		    public void setScheduleValue(String scheduleValue) {
		    	this.getConfigData().setScheduleValue(scheduleValue);
		    }

		    @Override
		    public String getExecutionLogFilePathPattern() {
		    	return this.getConfigData().getExecutionLogFilePathPattern();
		    }

		    @Override
		    public void setExecutionLogFilePathPattern(String executionLogFilePathPattern) {
		    	this.getConfigData().setExecutionLogFilePathPattern(executionLogFilePathPattern);
		    }

		    @Override
		    public String getExecutionLogLevel() {
		    	return this.getConfigData().getExecutionLogLevel();
		    }

		    @Override
		    public void setExecutionLogLevel(String executionLogLevel) {
		    	this.getConfigData().setExecutionLogLevel(executionLogLevel);
		    }

		    @Override
		    public String getExceptionLogFilePathPattern() {
		    	return this.getConfigData().getExceptionLogFilePathPattern();
		    }

		    @Override
		    public void setExceptionLogFilePathPattern(String exceptionLogFilePathPattern) {
		    	this.getConfigData().setExceptionLogFilePathPattern(exceptionLogFilePathPattern);
		    }

		    @Override
		    public String getExceptionLogLevel() {
		    	return this.getConfigData().getExceptionLogLevel();
		    }

		    @Override
		    public void setExceptionLogLevel(String exceptionLogLevel) {
		    	this.getConfigData().setExceptionLogLevel(exceptionLogLevel);
		    }

		    @Override
		    public String getLogEnableYn() {
		    	return this.getConfigData().getLogEnableYn();
		    }

		    @Override
		    public void setLogEnableYn(String logEnableYn) {
		    	this.getConfigData().setLogEnableYn(logEnableYn);
		    }

		    @Override
		    public String getLogExceptEnableYn() {
		    	return this.getConfigData().getLogExceptEnableYn();
		    }

		    @Override
		    public void setLogExceptEnableYn(String logExceptEnableYn) {
		    	this.getConfigData().setLogExceptEnableYn(logExceptEnableYn);
		    }

		    @Override
		    public List<String> getLogExceptPatternList() {
		    	return this.getConfigData().getLogExceptPatternList();}	

		    @Override
		    public void setLogExceptPatternList(List<String> logExceptPatternList) {
		    	this.getConfigData().setLogExceptPatternList(this.getEffectiveList(logExceptPatternList));
		    }

		    @Override
		    public void addLogExceptPattern(String value) {
		    	List<String> list = this.getLogExceptPatternList() == null?new ArrayList<>():this.getLogExceptPatternList();
		    	this.setLogExceptPatternList(this.getAddOnList(list,value));
		    }	

		    @Override
		    public void removeLogExceptPattern(String value) {
		    	this.setLogExceptPatternList(this.getRemoveOnList(this.getLogExceptPatternList(), value));
		    }

		    @Override
		    public Properties getLogPolicyProps() {
		    	return this.getConfigData().getLogPolicyProps();
		    }

		    @Override
		    public void setLogPolicyProps(Properties logPolicyProps) {
		    	this.getConfigData().setLogPolicyProps(logPolicyProps);
		    }

		    @Override
		    public void updateLogPolicyProps(String key,String value) {
		    	this.updateProperties(this.getLogPolicyProps(), key, value);
		    }

		    @Override
		    public String getRunnableServerCheckEnableYn() {
		    	return this.getConfigData().getRunnableServerCheckEnableYn();
		    }

		    @Override
		    public void setRunnableServerCheckEnableYn(String runnableServerCheckEnableYn) {
		    	this.getConfigData().setRunnableServerCheckEnableYn(runnableServerCheckEnableYn);
		    }

		    @Override
		    public List<String> getRunnableServerList() {
		    	return this.getConfigData().getRunnableServerList();
		    }

		    @Override
		    public void setRunnableServerList(List<String> runnableServerList) {
		    	this.getConfigData().setRunnableServerList(this.getEffectiveList(runnableServerList));
		    }

		    @Override
		    public boolean isRunnableServer() {
		    	return this.isExistRuntimeServer(this.getRunnableServerList(), this.getRunnableServerCheckEnableYn());
		    }

		    @Override
		    public void loadConfigData(String taskName) {
		    	this.loadConfigData(taskName, null);
		    }
		    
		    @Override
		    public void loadConfigData(String taskName, Set<String> configKeySet) {
		    	if(taskName == null || taskName.equals("")) {return;}
				String configDataJson = this.readConfigFile(taskName);
				
				if(configKeySet != null && !configKeySet.isEmpty()) { 						
					Map<String,Object> currentMap = this.batchUtil.getJsonUtil().getMap(this.getConfigData());
					Map<String,Object> readMap = this.batchUtil.getJsonUtil().getMap(configDataJson);
		
			    	for(String key:configKeySet) {
			    		if(currentMap.containsKey(key) && readMap.containsKey(key)) {
			    			currentMap.put(key,readMap.get(key));
			    		}
			    	}	
			    	configDataJson = this.batchUtil.getJsonUtil().toJson(currentMap);
				}
				
				this.setConfigData(configDataJson);
				this.setConfigDataJson(configDataJson);
		    }
		
		    @Override
		    public void saveConfigData(String taskName) {
		    	this.saveConfigData(taskName, null);
		    }
		    
		    @Override
		    public void saveConfigData(String taskName, Set<String> configKeySet) {
		    	if(taskName == null || taskName.equals("")) {return;}

		    	String configDataJson = this.batchUtil.getJsonUtil().toJson(this.getConfigData());

		    	if(configKeySet != null && !configKeySet.isEmpty()) {   		
		        	Map<String,Object> currentMap = this.batchUtil.getJsonUtil().getMap(configDataJson);
		        	
		        	Map<String,Object> configFileMap = this.batchUtil.getJsonUtil().getMap(this.readConfigFile(taskName));
		        	for(String key:configKeySet) {
		        		if(currentMap.containsKey(key)) {
		        			configFileMap.put(key,currentMap.get(key));
			    		}		        		
		        	}
		        	configDataJson = this.batchUtil.getJsonUtil().toJson(configFileMap);    		
		    	}
		
		    	this.writeConfigFile(taskName,configDataJson);
		    	this.setConfigDataJson(configDataJson);
		    	log.debug(">> writeConfigDataJson: {}",configDataJson);	
		    }
		
		    @Override
		    public void deleteConfigData(String taskName) {
				String targetDir = this.getConfigLocation();
				if(targetDir.charAt(targetDir.length()-1) != '/') {targetDir = targetDir+"/";}
				targetDir = targetDir + this.batchUtil.getSystemUtil().getServerInstanceCode()+"/";
				String targetFilePath = targetDir + taskName + "." + this.getConfigFileExt();				
				log.debug(">> deleteConfigFile:{}",targetFilePath);
				
				File file = new File(targetFilePath);
				if(file.exists()) {file.delete();}
		    }
		    
		    @Override
			public String touchConfigData(String taskName, long timeMills) {
				String targetDir = this.getConfigLocation();
				if(targetDir.charAt(targetDir.length()-1) != '/') {targetDir = targetDir+"/";}
				targetDir = targetDir + this.batchUtil.getSystemUtil().getServerInstanceCode()+"/";
				String targetFilePath = targetDir + taskName + "." + this.getConfigFileExt();				
				log.debug(">> touchConfigFile: {}",targetFilePath);
	            
	            File file = new File(targetFilePath);
	            if(file.exists()) {file.setLastModified(timeMills);}

	            return targetDir;
			}
					    
		    @Override
		    public String getSaveImmediatelyYn() {
		    	return this.getConfigData().getSaveImmediatelyYn();
		    }
		    
		    @Override
		    public void setSaveImmediatelyYn(String saveImmediatelyYn) {
		    	this.getConfigData().setSaveImmediatelyYn(saveImmediatelyYn);
		    }

		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					    
			private void setConfigData(String configDataJson) {
				this.configData = this.batchUtil.getJsonUtil().fromJson(configDataJson,ConfigData.class);
			}
			
			private ConfigData getConfigData() {
				if(this.configData == null) {this.configData = new ConfigData();}
				return this.configData;
			}

			private List<Term> getAddOnList(List<Term> list, Term term) {
				if(list == null || term == null) {return list;}
				boolean isEnable = true;
				for(Term _term : list) {if(_term.getValue().equals(term.getValue())) {isEnable = false;break;}}
				if(isEnable) {list.add(term);}
				return list;
			}
			
			private List<Term> getRemoveOnList(List<Term> list, Term term) {
				 if(list == null || term == null) {return list;}
				 for(int i = 0;i<list.size();i++) {if(list.get(i).getValue().equals(term.getValue())) {list.remove(i);break;}}
				 return list;
			}
			
			private List<String> getAddOnList(List<String> list, String value) {
			    if(list == null || value == null || value.equals("")) {return list;}
			    boolean isEnable = true;
			    for(String _value : list) {if(_value.equals(value)) {isEnable = false;break;}}
			    if(isEnable) {list.add(value);}
			    return list;
			}
			
			private List<String> getRemoveOnList(List<String> list, String value) {
			    if(list == null || value == null || value.equals("")) {return list;}
			    for(int i = 0;i<list.size();i++) {if(list.get(i).equals(value)) {list.remove(i);break;}}	    
			    return list;
			}
		    
		    private void updateProperties(Properties props,String key, String value) {
		        if(props == null || key == null || key.equals("")) {return;}
		        props.put(key,(value.equals("null")?"":value));
		    }
		
		    private List<String> getEffectiveList(List<String> list) {
		    	if(list == null || list.size() == 0) {return list;}
		        for(int i = 0;i<list.size();i++) {
		        	if(list.get(i) == null || list.get(i).equals("")) {
		        		list.remove(i); 
		        		list = this.getEffectiveList(list);
		        	}        	
		        }
		        return list;
		    }
		    
		    private boolean isExistRuntimeServer(List<String> list,String checkEnableYn) {
		        boolean isEnable = checkEnableYn.equals("Y")?true:false;        
		        if(isEnable && (list == null || list.size() == 0)) {isEnable = false;}
		        if(!isEnable) {return false;}
		
		    	String runtimeServerInstanceCode = this.batchUtil.getSystemUtil().getServerInstanceCode();
		    	String runtimeServerIpPort = this.batchUtil.getSystemUtil().getServerIpPort();
		    	String runtimeServerIp = this.batchUtil.getSystemUtil().getServerIp();
		        
		        String item = null,checkValue = null;
		        
		        isEnable = false;
		        for(int i = 0;i<list.size();i++) {
		        	item = list.get(i);
		        	if(checkValue == null && (item.indexOf(":") == -1 && item.indexOf(".") == -1)) {checkValue = runtimeServerInstanceCode;}
		        	if(checkValue == null && (item.indexOf(":") == -1 && item.indexOf(".") != -1 && item.split("\\.").length == 4)) {checkValue = runtimeServerIp;}
		        	if(checkValue == null && (item.indexOf(":") != -1 && item.indexOf(".") != -1 && item.indexOf(":")>item.indexOf("."))) {checkValue = runtimeServerIpPort;}
		        	if(list.get(i).equals(checkValue)) {isEnable = true;break;}
		        	checkValue = null;
		        }
		        
		        return isEnable;
		    }
		
		    private String readConfigFile(String taskName) {
				String targetDir = this.getConfigLocation();
				if(targetDir.charAt(targetDir.length()-1) != '/') {targetDir = targetDir+"/";}
				targetDir = targetDir + this.batchUtil.getSystemUtil().getServerInstanceCode()+"/";
				File file = new File(targetDir);
				if(!file.exists()) {file.mkdirs();}
				
				String taskConfigDataJson = null;
				String targetFilePath = targetDir + taskName + "." + this.getConfigFileExt();

				boolean isEnable = true;
				File targetFile = new File(targetFilePath);
				if(isEnable && !targetFile.exists()) {isEnable = false;}
				if(isEnable) {
					try(BufferedReader bufReader = new BufferedReader(new FileReader(targetFilePath))){
						String readData = bufReader.readLine();
						if(isEnable && readData.indexOf("serverName") == -1) {isEnable = false;}					
						if(isEnable && readData.indexOf("serverInstanceCode") == -1) {isEnable = false;}
						if(isEnable && readData.indexOf("taskName") == -1) {isEnable = false;}
						if(isEnable && readData.indexOf("taskConfig") == -1) {isEnable = false;}
						if(isEnable && readData.indexOf("timestamp") == -1) {isEnable = false;}
						if(isEnable) {
							Map<String,Object> data = this.batchUtil.getObjectMapperUtil().readValue(readData, new TypeReference<Map<String,Object>>(){});
							if(isEnable && !String.valueOf(data.get("serverInstanceCode")).equals(this.batchUtil.getSystemUtil().getServerInstanceCode())) {isEnable = false;}
							if(isEnable && !String.valueOf(data.get("taskName")).equals(taskName)) {isEnable = false;}
							if(isEnable) {
								taskConfigDataJson = this.batchUtil.getBase64Util().getDecodeBase64String(String.valueOf(data.get("taskConfig")));
								//taskConfigDataJson = String.valueOf(data.get("taskConfig"));
							}
						}
					} catch(Exception e) {
						 throw new RuntimeException(e.getMessage());
					}					
				}
				if(!isEnable) {
					taskConfigDataJson = this.batchUtil.getJsonUtil().toJson(this.getConfigData());
					this.writeConfigFile(taskName,taskConfigDataJson);				
				}

				return taskConfigDataJson;
		    }
		    
		    private void writeConfigFile(String taskName,String configDataJson) {
				Map<String,Object> map = new HashMap<>();
				map.put("serverName", this.batchUtil.getSystemUtil().getServerName());
				map.put("serverInstanceCode", this.batchUtil.getSystemUtil().getServerInstanceCode());
				map.put("taskName",taskName);
				map.put("taskConfig",this.batchUtil.getBase64Util().getEncodeBase64String(configDataJson));
				//map.put("taskConfig",configDataJson);
				map.put("timestamp",System.currentTimeMillis());

				String data = this.batchUtil.getJsonUtil().toJson(map);
				
				String targetDir = this.getConfigLocation();
				if(targetDir.charAt(targetDir.length()-1) != '/') {targetDir = targetDir+"/";}
				targetDir = targetDir + this.batchUtil.getSystemUtil().getServerInstanceCode() + "/";
				File dir = new File(targetDir);		
				if(!dir.exists()) {dir.mkdirs();}	
				this.cleanDirectory(dir.listFiles(),this.getConfigFileExt());
				
				String targetFilePath = targetDir+taskName+"."+this.getConfigFileExt();
				try(BufferedWriter writerBuf = new BufferedWriter(new FileWriter(targetFilePath))) {
					writerBuf.write(data);
					writerBuf.flush();
				} catch(Exception e) {
					 throw new RuntimeException(e.getMessage());
				}
		    }
		    
			private void cleanDirectory(File[] fileList,String fileExt) {
				if(fileList == null || fileList.length == 0) {return;}
				// 확장자 fileExt가 아닌 파일을 찾아서 삭제				
				List<File> deleteFileList = null;
				for(File file:fileList) {
					if(file.getName().lastIndexOf("."+fileExt) == -1) {
						if(deleteFileList == null) {deleteFileList = new ArrayList<>();}
						deleteFileList.add(file);
					}
				}
				if(deleteFileList != null) {
					deleteFileList.forEach(f-> f.delete());
				}
			}

		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		};
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}