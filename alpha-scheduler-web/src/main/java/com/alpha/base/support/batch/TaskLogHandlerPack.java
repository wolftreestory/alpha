package com.alpha.base.support.batch;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.StopWatch;

import com.alpha.base.support.batch.TaskLogContextPack.TaskLogContext;
import com.alpha.base.support.batch.TaskPack.Domain;
import com.alpha.base.support.batch.TaskPack.ExceptionType;
import com.alpha.base.support.batch.vo.TaskExecutionVo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskLogHandlerPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static interface TaskLogHandler {
		
		public static final String TASK_TOKEN = "taskToken";
		public static final String EXECUTION_LOG_FILE_PATH = "executionLogFilePath";	
		public static final String EXECUTION_LOG_LEVEL = "executionLogLevel";
		public static final String EXCEPTION_LOG_FILE_PATH = "exceptionLogFilePath";	
		public static final String EXCEPTION_LOG_LEVEL = "exceptionLogLevel";
		    
		public void setEnableYn(String enableYn);
		public void setMetaTaskToken(String metaTaskToken);
		public void setMetaExecutionLogFilePath(String metaExecutionLogFilePath);
		public void setMetaExecutionLogLevel(String metaExecutionLogLevel);
		public void setMetaExceptionLogFilePath(String metaExceptionLogFilePath);
		public void setMetaExceptionLogLevel(String metaExceptionLogLevel);
		public void setLogStampPrintYn(String logStampPrintYn);
		public void setLogStampStartPrintYn(String logStampStartPrintYn);
		public void setLogStampExceptionPrintYn(String logStampExceptionPrintYn);
		public void setLogStampAroundPrintYn(String logStampAroundPrintYn);
		public void setLogStampEndPrintYn(String logStampEndPrintYn);
		public void setTaskLogContext(TaskLogContext taskLogContext);
		
		public void startLog(JoinPoint joinPoint) throws Exception;
	    public Object aroundLog(ProceedingJoinPoint joinPoint) throws Throwable;
		public <T> void exceptionLog(JoinPoint joinPoint,Exception exception) throws Exception;  
	    public void endLog(JoinPoint joinPoint,Object returnObj) throws Exception;    
		
	    public TaskLogContext getTaskLogContext();
	    public boolean isNowExecutionPostProcessEnd(TaskPack.Task task,List<Map<String,Object>> list);
	    public boolean isOldExecutionPostProcessEnd(TaskPack.Task task,List<Map<String,Object>> list);
	    
	    public Properties getConfigEnableProps();
	    public Properties getGlobalLogPolicyProps();
	    public Properties getActiveLogPolicyProps(TaskPack.Task task);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	public static TaskLogHandler getTaskLogHandler(BatchUtil batchUtil) {
		
		return new TaskLogHandler () {

		    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			private String enableYn = "Y";
			private String metaTaskToken = TaskLogHandler.TASK_TOKEN;	
			private String metaExecutionLogFilePath = TaskLogHandler.EXECUTION_LOG_FILE_PATH;
			private String metaExecutionLogLevel = TaskLogHandler.EXECUTION_LOG_LEVEL;
			private String metaExceptionLogFilePath = TaskLogHandler.EXCEPTION_LOG_FILE_PATH;
			private String metaExceptionLogLevel = TaskLogHandler.EXCEPTION_LOG_LEVEL;
			private String logStampPrintYn = "Y";
			private String logStampStartPrintYn = "Y";
			private String logStampExceptionPrintYn = "Y";
			private String logStampAroundPrintYn = "Y";
			private String logStampEndPrintYn = "Y";
			private TaskLogContext taskLogContext;
		
			public void setEnableYn(String enableYn) {this.enableYn = enableYn;}		
			public void setMetaTaskToken(String metaTaskToken) {this.metaTaskToken = metaTaskToken;}
			public void setMetaExecutionLogFilePath(String metaExecutionLogFilePath) {this.metaExecutionLogFilePath = metaExecutionLogFilePath;}
			public void setMetaExecutionLogLevel(String metaExecutionLogLevel) {this.metaExecutionLogLevel = metaExecutionLogLevel;}
			public void setMetaExceptionLogFilePath(String metaExceptionLogFilePath) {this.metaExceptionLogFilePath = metaExceptionLogFilePath;}
			public void setMetaExceptionLogLevel(String metaExceptionLogLevel) {this.metaExceptionLogLevel = metaExceptionLogLevel;}
			public void setLogStampPrintYn(String logStampPrintYn) {this.logStampPrintYn = logStampPrintYn;}
			public void setLogStampStartPrintYn(String logStampStartPrintYn) {this.logStampStartPrintYn = logStampStartPrintYn;}
			public void setLogStampExceptionPrintYn(String logStampExceptionPrintYn) {this.logStampExceptionPrintYn = logStampExceptionPrintYn;}
			public void setLogStampAroundPrintYn(String logStampAroundPrintYn) {this.logStampAroundPrintYn = logStampAroundPrintYn;}
			public void setLogStampEndPrintYn(String logStampEndPrintYn) {this.logStampEndPrintYn = logStampEndPrintYn;}	
			public void setTaskLogContext(TaskLogContext taskLogContext) {this.taskLogContext = taskLogContext;}
				
		    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			
		    @Override    
			public void startLog(JoinPoint joinPoint) throws Exception {
		    	if(this.enableYn == null || !this.enableYn.equals("Y")) {return;}
				
		    	Class<? extends Object> clazz = joinPoint.getTarget().getClass();
				Object[] args = joinPoint.getArgs();
		
				TaskPack.Task task = (TaskPack.Task)joinPoint.getThis();	
		
				String executionNo = this.taskLogContext.getNewExecutionNo(task);				
				task.setExecutionNo(executionNo);
				task.setLogHandler(new TaskPack.Task.LogHandler() {
					@Override public Logger getLogger() {return LoggerFactory.getLogger(clazz);}
					@Override public void print(StringBuffer buf) {this.print(this.getLogger(),buf);}
					@Override public void print(Logger logger, StringBuffer buf) {
						if(logger == null || buf == null) {return;}
						MDC.put(metaExecutionLogLevel,"INFO");
						MDC.put(metaExecutionLogFilePath,getTaskLogFilePath(task,"executionLog"));
						logger.info(buf.toString());
						MDC.remove(metaExecutionLogLevel);				
						MDC.remove(metaExecutionLogFilePath);
						logger.debug(ch.qos.logback.classic.ClassicConstants.FINALIZE_SESSION_MARKER,null);					
					}
					@Override public void init(TaskPack.Task task) {
						MDC.put(metaTaskToken,task.getTaskToken());
						MDC.put(metaExecutionLogLevel,task.getTaskConfig().getExecutionLogLevel());
						MDC.put(metaExecutionLogFilePath,getTaskLogFilePath(task,"executionLog"));
						MDC.put(metaExceptionLogLevel,task.getTaskConfig().getExceptionLogLevel());
						MDC.put(metaExceptionLogFilePath,getTaskLogFilePath(task,"exceptionLog"));			
					}
					@Override public void finish(TaskPack.Task task) {
						MDC.remove(metaTaskToken);
						MDC.remove(metaExecutionLogLevel);
						MDC.remove(metaExecutionLogFilePath);
						MDC.remove(metaExceptionLogLevel);
						MDC.remove(metaExceptionLogFilePath);
						if(this.getLogger() != null) {
							this.getLogger().debug(ch.qos.logback.classic.ClassicConstants.FINALIZE_SESSION_MARKER,null);
							this.getLogger().error(ch.qos.logback.classic.ClassicConstants.FINALIZE_SESSION_MARKER,null);
						}
					}
				});
		
				String currentTime = batchUtil.getTimeUtil().getCurrentTime("yyyyMMddHHmmssSSS");
		
				StringBuffer stampBuf = new StringBuffer();
				if(task.getDomain() == Domain.inner) {
					stampBuf.append("\n");
					stampBuf.append("\n ─── 수행시작 : "+task.getName()+" ───");
					stampBuf.append("\n * executionNo : "+executionNo);
					stampBuf.append("\n * executionStartTime : "+currentTime);
					stampBuf.append("\n * method : "+joinPoint.getSignature().toString());
					if(args != null) {for(int i = 0;i<args.length;i++) {stampBuf.append("\n * arg"+(i+1)+" : "+(args[i] == null?"null":args[i].toString()));}}
					stampBuf.append("\n ────────────────────────────────────────────────────────────────────────────────────────────────────");
					stampBuf.append("\n");
				}
				if(task.getDomain() == Domain.outter) {
					stampBuf.append("\n");
					stampBuf.append("\n ─── 수행시작 : "+task.getName()+" ───");
					stampBuf.append("\n * executionNo : "+executionNo);
					stampBuf.append("\n * executionStartTime : "+currentTime);
					stampBuf.append("\n * jobName : "+task.getTaskMetaObject().getJobName());
					stampBuf.append("\n * jobConfigClass : "+task.getTaskMetaObject().getAppName()+"/"+task.getTaskMetaObject().getJobConfigClass());
					stampBuf.append("\n ────────────────────────────────────────────────────────────────────────────────────────────────────");
					stampBuf.append("\n");
				}
		    	Map<String,Object> map = batchUtil.getJsonUtil().getMap(task.getTaskConfig().getConfigDataJson());
		    	map.put("globalLogPolicyProps",this.getGlobalLogPolicyProps());
		    	String executionConfig = batchUtil.getJsonUtil().toJson(map);
		    	
				TaskExecutionVo vo = new TaskExecutionVo();
				vo.setExecutionNo(executionNo);
				vo.setExecutionName(task.getName());
				vo.setExecutionType(task.getDomain().toString());
				vo.setExecutionConfig(executionConfig);		
				vo.setExecutionServer(batchUtil.getSystemUtil().getServerIpPort());
				vo.setExecutionStartTime(currentTime);		
				vo.setExceptionYn("N");
				this.taskLogContext.insertTaskExecutionInfo(vo);
		
				if(this.isPropertyValue(task,"logStampPrintYn","logStampStartPrintYn")) {
					task.printStamp(stampBuf);
				}
				
		        return;
		    }
		    
		    @Override
			public void exceptionLog(JoinPoint joinPoint,Exception exception) throws Exception {
		    	if(this.enableYn == null || !this.enableYn.equals("Y")) {throw exception;}
				
				TaskPack.Task task = (TaskPack.Task)joinPoint.getThis();
				String executionNo = task.getExecutionNo();
		
				ByteArrayOutputStream exceptionOut = new ByteArrayOutputStream();
				Throwable throwable = exception.getCause();
				if(throwable.getCause() != null) {throwable = throwable.getCause();}
				StackTraceElement ste = throwable.getStackTrace()[0];
				throwable.printStackTrace(new PrintStream(exceptionOut));

				String exceptionType = throwable.getClass().toString().indexOf("InterruptedException") == -1?ExceptionType.error.toString():ExceptionType.interrupt.toString();
				
				StringBuffer stampBuf = new StringBuffer();
				if(task.getDomain() == Domain.inner) {
					stampBuf.append("\n");
					stampBuf.append("\n ─── 수행오류 : "+task.getName()+" ───");
					stampBuf.append("\n * executionNo : "+executionNo);
					stampBuf.append("\n * class : "+ste.getClassName());
					stampBuf.append("\n * method : "+ste.getMethodName());
					stampBuf.append("\n * lineNumber : "+ste.getLineNumber());
					stampBuf.append("\n * exception : "+throwable);		
					stampBuf.append("\n ────────────────────────────────────────────────────────────────────────────────────────────────────");
					stampBuf.append("\n");
				}
				if(task.getDomain() == Domain.outter) {
					stampBuf.append("\n");
					stampBuf.append("\n ─── 수행오류 : "+task.getName()+" ───");
					stampBuf.append("\n * executionNo : "+executionNo);
					stampBuf.append("\n * jobName : "+task.getTaskMetaObject().getJobName());
					stampBuf.append("\n * jobConfigClass : "+task.getTaskMetaObject().getAppName()+"/"+task.getTaskMetaObject().getJobConfigClass());
					stampBuf.append("\n * exception : "+throwable.toString().split("\n")[0]);
					stampBuf.append("\n ────────────────────────────────────────────────────────────────────────────────────────────────────");
					stampBuf.append("\n");
				}

				String exceptionInfo = exceptionOut.toString();
				Set<String> metaLogFileSet = null;
				if(task.getDomain() == Domain.outter && task.getAgentResponse()!=null) {					
					exceptionInfo = task.getAgentResponse().getError();
					metaLogFileSet = task.getAgentResponse().getProcessExecuteLogFile();
					if(metaLogFileSet!=null && metaLogFileSet.isEmpty()) {metaLogFileSet=null;}
				}

				TaskExecutionVo vo = new TaskExecutionVo();
				vo.setExecutionNo(executionNo);
				vo.setExecutionEndTime(batchUtil.getTimeUtil().getCurrentTime("yyyyMMddHHmmssSSS"));
				vo.setExceptionType(exceptionType);
				vo.setExceptionYn("Y");
				vo.setLogType("exceptionLog");
				vo.setMetaLogFileCount(metaLogFileSet==null?0:metaLogFileSet.size());
				vo.setMetaLogFileInfo(metaLogFileSet==null?"":String.join(",", metaLogFileSet));
				this.taskLogContext.updateTaskExecutionInfo(vo);
		
				
				task.setExceptionType(exceptionType);
				task.setExceptionInfo(exceptionInfo);
				
				if(this.isPropertyValue(task,"logStampPrintYn","logStampExceptionPrintYn")) {
					task.printStamp(stampBuf);
				}
				
				this.taskLogContext.postProcess(task,this.getActiveLogPolicyProps(task));
		
		        return;
			}
		    
			@Override	
			public Object aroundLog(ProceedingJoinPoint joinPoint) throws Throwable {
		    	if(this.enableYn == null || !this.enableYn.equals("Y")) {return joinPoint.proceed();}
		
				//Class<? extends Object> clazz = joinPoint.getTarget().getClass();	
				StopWatch stopWatch = new StopWatch(joinPoint.getSignature().toString());
		
				try {
					stopWatch.start(joinPoint.toShortString());
					return joinPoint.proceed();
					
				} finally {
					stopWatch.stop();
		
					TaskPack.Task task = (TaskPack.Task)joinPoint.getThis();
					String executionNo = task.getExecutionNo();
		
					StringBuffer stampBuf = new StringBuffer();
					if(task.getDomain() == Domain.inner) {
						stampBuf.append("\n");			
						stampBuf.append("\n ─── 수행시간 : "+task.getName()+" ───");
						stampBuf.append("\n * executionNo : "+executionNo);
						stampBuf.append("\n * executionTime(ms) : "+stopWatch.getTotalTimeMillis());
						stampBuf.append("\n * method : "+joinPoint.getSignature().toString());
						stampBuf.append("\n ────────────────────────────────────────────────────────────────────────────────────────────────────");
						stampBuf.append("\n");
					}
					if(task.getDomain() == Domain.outter) {
						stampBuf.append("\n");
						stampBuf.append("\n ─── 수행시간 : "+task.getName()+" ───");
						stampBuf.append("\n * executionNo : "+executionNo);
						stampBuf.append("\n * executionTime(ms) : "+stopWatch.getTotalTimeMillis());
						stampBuf.append("\n * jobName : "+task.getTaskMetaObject().getJobName());
						stampBuf.append("\n * jobConfigClass : "+task.getTaskMetaObject().getAppName()+"/"+task.getTaskMetaObject().getJobConfigClass());
						stampBuf.append("\n ────────────────────────────────────────────────────────────────────────────────────────────────────");
						stampBuf.append("\n");
					}					
					TaskExecutionVo vo = new TaskExecutionVo();
					vo.setExecutionNo(executionNo);
					vo.setExecutionTime(String.valueOf(stopWatch.getTotalTimeMillis()));
					this.taskLogContext.updateTaskExecutionInfo(vo);
		
		            if(this.isPropertyValue(task,"logStampPrintYn","logStampAroundPrintYn")) {
		            	task.printStamp(stampBuf);
		            }
				}
			}

			@Override
		    public void endLog(JoinPoint joinPoint,Object returnObj) throws Exception {
				if(this.enableYn == null || !this.enableYn.equals("Y")) {return;}
		    	
				//Class<? extends Object> clazz = joinPoint.getTarget().getClass();	
				Object[] args = joinPoint.getArgs();
				
				TaskPack.Task task = (TaskPack.Task)joinPoint.getThis();
				String executionNo = task.getExecutionNo();
				String currentTime = batchUtil.getTimeUtil().getCurrentTime("yyyyMMddHHmmssSSS");
				
				StringBuffer stampBuf = new StringBuffer();
				if(task.getDomain() == Domain.inner) {
					stampBuf.append("\n");
					stampBuf.append("\n ─── 수행종료 : "+task.getName()+" ───");
					stampBuf.append("\n * executionNo : "+executionNo);
					stampBuf.append("\n * executionEndTime : "+currentTime);
					stampBuf.append("\n * method : "+joinPoint.getSignature().toString());		
					if(args != null) {for(int i = 0;i<args.length;i++) {stampBuf.append("\n * arg"+(i+1)+" : "+(args[i] == null?"null":args[i].toString()));}}
					stampBuf.append("\n * return : "+(returnObj == null?"null":returnObj.toString()));				
					stampBuf.append("\n ────────────────────────────────────────────────────────────────────────────────");
					stampBuf.append("\n");
				}
				if(task.getDomain() == Domain.outter) {
					stampBuf.append("\n");
					stampBuf.append("\n ─── 수행종료 : "+task.getName()+" ───");
					stampBuf.append("\n * executionNo : "+executionNo);
					stampBuf.append("\n * executionEndTime : "+currentTime);
					stampBuf.append("\n * jobName : "+task.getTaskMetaObject().getJobName());
					stampBuf.append("\n * jobConfigClass : "+task.getTaskMetaObject().getAppName()+"/"+task.getTaskMetaObject().getJobConfigClass());
					stampBuf.append("\n ────────────────────────────────────────────────────────────────────────────────");
					stampBuf.append("\n");
				}
				
				Set<String> metaLogFileSet = null;
				if(task.getDomain() == Domain.outter && task.getAgentResponse()!=null) {					
					metaLogFileSet = task.getAgentResponse().getProcessExecuteLogFile();
					if(metaLogFileSet!=null && metaLogFileSet.isEmpty()) {metaLogFileSet=null;}
				}
								
				TaskExecutionVo vo = new TaskExecutionVo();
				vo.setExecutionNo(executionNo);
				vo.setExecutionEndTime(currentTime);
				vo.setMetaLogFileCount(metaLogFileSet==null?0:metaLogFileSet.size());
				vo.setMetaLogFileInfo(metaLogFileSet==null?"":String.join(",", metaLogFileSet));

				this.taskLogContext.updateTaskExecutionInfo(vo);
				
		        if(this.isPropertyValue(task,"logStampPrintYn","logStampEndPrintYn")) {
		        	task.printStamp(stampBuf);
		        }
		        
		        this.taskLogContext.postProcess(task,this.getActiveLogPolicyProps(task));
		
				return;
		    }

			@Override
			public TaskLogContext getTaskLogContext() {return this.taskLogContext;}
		 
			@Override
			public boolean isNowExecutionPostProcessEnd(TaskPack.Task task,List<Map<String,Object>> list) {
				boolean isEnable = false;
				if(!isEnable && task.getExecutionNo() == null) {isEnable = true;}
				if(!isEnable && (list == null || list.isEmpty())) {isEnable = true;}
				if(!isEnable) {
					Map<String,Object> refContext = null;
					for(Map<String,Object> item:list) {
						if(item.get("executionNo").toString().equals(task.getExecutionNo())){refContext = item;break;}
					}
					if(!isEnable && (refContext == null || refContext.isEmpty())){isEnable = true;}
					if(!isEnable && (refContext.get("executionPostProcess") != null && !refContext.get("executionPostProcess").equals(""))){isEnable = true;}
				}
				return isEnable;
			}
			
			@Override
			public boolean isOldExecutionPostProcessEnd(TaskPack.Task task,List<Map<String,Object>> list) {
				boolean isEnable = false;
				if(!isEnable && (list == null || list.isEmpty())) {isEnable = true;}
				if(!isEnable) {isEnable = true;
			        for(Map<String,Object> item:list) {
			        	if(item.get("executionPostProcess") == null || item.get("executionPostProcess").equals("")) {isEnable = false;break;}
			        }
				}
		        return isEnable;
		    }
		   
			@Override
			public Properties getConfigEnableProps() {
				Properties props = this.taskLogContext.getConfigEnableProps();
		    	props.setProperty("logStampPrintYn","Y");    	
		    	return props;
			}
			
		    @Override
		    public Properties getGlobalLogPolicyProps() {
		    	Properties props = this.taskLogContext.getGlobalLogInitPolicyProps();
		    	props.setProperty("logStampPrintYn", this.logStampPrintYn);
		    	props.setProperty("logStampStartPrintYn", this.logStampStartPrintYn);
		    	props.setProperty("logStampExceptionPrintYn", this.logStampExceptionPrintYn);
		    	props.setProperty("logStampAroundPrintYn", this.logStampAroundPrintYn);
		    	props.setProperty("logStampEndPrintYn", this.logStampEndPrintYn);
		    	return props;
		    }
		        
		    @Override
		    public Properties getActiveLogPolicyProps(TaskPack.Task task) {
		    	if(task == null || task.getTaskConfig() == null) {return null;}
		    	boolean isGlobal = false;
		    	Properties props = task.getTaskConfig().getLogPolicyProps();
		    	if(!isGlobal && props == null) {isGlobal = true;}
		    	if(!isGlobal && props.getProperty("globalPolicyYn") == null) {isGlobal = true;}
		    	if(!isGlobal && props.getProperty("globalPolicyYn").equals("Y")) {isGlobal = true;}
		    	if(isGlobal) {props = this.getGlobalLogPolicyProps();}    
		    	return props;
		    }

		    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
		    private boolean isPropertyValue(TaskPack.Task task,String key1,String key2) {
		    	Properties props = this.getActiveLogPolicyProps(task);
				boolean isEnable = true;
				if(isEnable && (props.getProperty(key1) == null || props.getProperty(key2).equals("N"))) {isEnable = false;}
				if(isEnable && (props.getProperty(key2) == null || props.getProperty(key2).equals("N"))) {isEnable = false;}
				return isEnable;
		    }
		
		    private String getTaskLogFilePath(TaskPack.Task task,String logType) {
		        if(task == null || task.getTaskConfig() == null) {return null;}
		        String logFilePathPattern = null;
		        if(logType.equals("executionLog")) {logFilePathPattern = task.getTaskConfig().getExecutionLogFilePathPattern();}
		        if(logType.equals("exceptionLog")) {logFilePathPattern = task.getTaskConfig().getExceptionLogFilePathPattern();}
		        return logFilePathPattern.replaceAll("[*]",task.getTaskToken());
		    }  

		    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		};
	
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}