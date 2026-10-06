package com.alpha.base.support.batch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.json.simple.JSONObject;
import org.springframework.util.StringUtils;

import com.alpha.base.support.batch.TaskPack.Domain;
import com.alpha.base.support.batch.TaskPack.ExceptionType;
import com.alpha.base.support.batch.vo.TaskExecutionVo;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskLogContextPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public interface TaskLogContext {

	    public static final String LOG_EXTRACT_MODE_LINE="line";
	    public static final String LOG_EXTRACT_MODE_BLOCK="block";
	    public static final String LOG_EXTRACT_MODE_CHUNK="chunk";
	    
	    public static final long LOG_READ_LINE_SIZE=10000;
	    public static final long LOG_READ_BLOCK_SIZE=2*1024*1024;   //2MB
	    public static final long LOG_READ_CHUNK_SIZE=2*1024*1024;  //2MB
	    
	    public static final long LOG_PRINT_LINE_SIZE=10000;
	    public static final long LOG_PRINT_BLOCK_SIZE=2*1024*1024;  //2MB    
	    public static final long LOG_PRINT_CHUNK_SIZE=2*1024*1024;  //2MB
	    
	    public static final String LOG_STAMP_PRINT_Y="Y";
	    public static final String LOG_STAMP_PRINT_N="N";

	    public static final String LOG_FILE_DB_STORE_Y="Y";
	    public static final String LOG_FILE_DB_STORE_N="N";
	    
	    public static final String LOG_FILE_REMOVE_Y="Y";   
	    public static final String LOG_FILE_REMOVE_N="N"; 
	    
	    public static final String LOG_HISTORY_SHRINK_Y="Y";
	    public static final String LOG_HISTORY_SHRINK_N="N";
	        
	    public static final String LOG_HISTORY_SHRINK_BASE_DAYS_DEFAULT="10";
	    public static final String LOG_HISTORY_SHRINK_BASE_ROWS_DEFAULT="10"; 

	    public BatchUtil getBatchUtil();
	    
	    public Properties getConfigEnableProps();
	    public Properties getGlobalLogInitPolicyProps();
		
	    public String getNewExecutionNo(TaskPack.Task task);

	    public List<Map<String,Object>> getTaskExecutionList(TaskExecutionVo vo);    
	    public Map<String,Object> getTaskExecutionInfo(String executionNo);

	    public void insertTaskExecutionInfo(TaskExecutionVo vo);
	    public void updateTaskExecutionInfo(TaskExecutionVo vo);
	    
	    public void postProcess(TaskPack.Task task,Properties taskLogPolicyProps) throws Exception;
	    
	    public Map<String,Object> extractTaskLog(TaskExecutionVo vo);
	    public void storeTaskLog(String executionNo,String logType,String filePath);
	    public void clearTaskLog(TaskExecutionVo vo);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static abstract class AbstractTaskLogContext implements TaskLogContext {

		protected static Set<String> dupCheck;
		protected static Map<String,Map<String,Object>> taskExecutionContext;
				
		private String logFileDbStoreYn = LOG_FILE_DB_STORE_Y;
		private String logFileRemoveYn = LOG_FILE_REMOVE_Y;
	    private String logHistoryShrinkYn = LOG_HISTORY_SHRINK_Y;
	    private String logHistoryShrinkBaseDays = LOG_HISTORY_SHRINK_BASE_DAYS_DEFAULT;
	    private String logHistoryShrinkBaseRows = LOG_HISTORY_SHRINK_BASE_ROWS_DEFAULT;
		
		private BatchUtil batchUtil;
		
		public AbstractTaskLogContext(BatchUtil batchUtil) {
			this.batchUtil = batchUtil;
		}
		
		@PostConstruct
		public void postConstruct() {
			synchronized(this){
				if(taskExecutionContext==null) {
					taskExecutionContext = new TreeMap<>(Collections.reverseOrder());
					loadContext();
				}			
			}
		}

		public void setLogFileDbStoreYn(String logFileDbStoreYn) {this.logFileDbStoreYn = logFileDbStoreYn;}
		public void setLogFileRemoveYn(String logFileRemoveYn) {this.logFileRemoveYn = logFileRemoveYn;}
		public void setLogHistoryShrinkYn(String logHistoryShrinkYn) {this.logHistoryShrinkYn = logHistoryShrinkYn;}
		public void setLogHistoryShrinkBaseDays(String logHistoryShrinkBaseDays) {this.logHistoryShrinkBaseDays = logHistoryShrinkBaseDays;}
		public void setLogHistoryShrinkBaseRows(String logHistoryShrinkBaseRows) {this.logHistoryShrinkBaseRows = logHistoryShrinkBaseRows;}

		public abstract String getLogFileDbStoreConfigEnableYn();
		public abstract String getLogFileRemoveConfigEnableYn();
		public abstract String getLogHistoryShirnkConfigEnableYn();

		public abstract void loadContext();		
		public abstract void initContext(String executionNo);
		public abstract void saveContext(String executionNo);
		public abstract void shrinkContext(TaskExecutionVo vo);

	    public abstract Map<String,Object> extractTaskLog(TaskExecutionVo vo);
		public abstract void storeTaskLog(String executionNo,String logType,String filePath);

		@Override
	    public BatchUtil getBatchUtil() {
	    	return this.batchUtil;
	    }
	    
		@Override
		public Properties getConfigEnableProps() {
			Properties props=new Properties();
	    	props.setProperty("logFileDbStoreYn",this.getLogFileDbStoreConfigEnableYn());
	    	props.setProperty("logFileRemoveYn",this.getLogFileRemoveConfigEnableYn());    	
	    	props.setProperty("logHistoryShrinkYn", this.getLogHistoryShirnkConfigEnableYn());
	    	return props;
		}
		
		@Override
		public Properties getGlobalLogInitPolicyProps() {
	    	Properties props=new Properties();
	    	props.setProperty("logFileDbStoreYn",this.logFileDbStoreYn);
	    	props.setProperty("logFileRemoveYn",this.logFileRemoveYn);
	    	props.setProperty("logHistoryShrinkYn",this.logHistoryShrinkYn);
	    	props.setProperty("logHistoryShrinkBaseDays",this.logHistoryShrinkBaseDays);        
	    	props.setProperty("logHistoryShrinkBaseRows",this.logHistoryShrinkBaseRows); 
	    	return props;
	    }

		@Override
		public synchronized String getNewExecutionNo(TaskPack.Task task) {
			String newExecutionNo=String.valueOf(System.currentTimeMillis());
			if(dupCheck==null || !dupCheck.contains(newExecutionNo)) {
				if(dupCheck==null) {dupCheck=new HashSet<>();}
				if(dupCheck.size()==100) {dupCheck.clear();}
				dupCheck.add(newExecutionNo);
				return newExecutionNo;
			}			
			//중복방지를 위해 1~100 까지 난수발생하여 sleep (지연시간<0.1s)
			try {Thread.sleep((int) (Math.random()*100)+1);} catch (InterruptedException e) {log.debug(">> skip:{}",e.getMessage());}			
			return getNewExecutionNo(task);
		}

		@Override
		public List<Map<String, Object>> getTaskExecutionList(TaskExecutionVo vo) {
			if(vo==null || vo.getExecutionName()==null) {return null;}

			long max = vo.getRownum();
			
			Map<String, Object> item = null;
			List<Map<String, Object>> list = new ArrayList<>();
					
			Iterator<String> keys = taskExecutionContext.keySet().iterator();
			
			boolean isEnable=true;
			while(keys.hasNext()) {
				item = taskExecutionContext.get(keys.next());
				//log.debug(">> exceptionYn:{},exceptionType:{}",item.get("exceptionYn"),item.get("exceptionType"));		
				isEnable=true;
				if(isEnable && (StringUtils.hasText(vo.getExecutionNo()) && !vo.getExecutionNo().equals(item.get("executionNo")))) {isEnable=false;}
				if(isEnable && (StringUtils.hasText(vo.getExecutionName()) && !vo.getExecutionName().equals(item.get("executionName")))) {isEnable=false;}
				if(isEnable && (StringUtils.hasText(vo.getExceptionYn()) && !vo.getExceptionYn().equals(item.get("exceptionYn")))) {isEnable=false;}
				if(isEnable) {isEnable=false;
					if(!isEnable && (StringUtils.hasText(vo.getErrorYn()) && vo.getErrorYn().equals("Y") && ExceptionType.error.equals(item.get("exceptionType")))) {isEnable=true;}
					if(!isEnable && (StringUtils.hasText(vo.getInterruptYn()) && vo.getInterruptYn().equals("Y") && ExceptionType.interrupt.equals(item.get("exceptionType")))) {isEnable=true;}
					if(!isEnable && !StringUtils.hasText(vo.getExceptionYn())) {isEnable=true;}
				}
				//log.debug(">> isEnable:{},step:{}",isEnable,step);
				
				if(isEnable) {list.add(item);}
				if(list.size()==max) {break;}
			}
			if(vo.getSelectMode()==null || !vo.getSelectMode().equals("delete")) {
				for(Map<String, Object> map : list) {
					if(map.get("executionLog")!=null && map.get("exceptionLog")!=null) {continue;}
					if(map.get("executionPostProcess")==null) {continue;}
					
					JSONObject jsonObj=batchUtil.getJsonUtil().getJsonObject(map.get("executionPostProcess").toString());
					if(jsonObj==null) {continue;}
					
					if(map.get("executionLog")==null) {			
						long size=jsonObj.get("executionLogSize")==null?0:Long.valueOf(jsonObj.get("executionLogSize").toString());
						map.put("executionLogSize",size);
						map.put("executionLog",size>0?"*":null);
					}
					
					if(map.get("exceptionLog")==null) {
						long size=jsonObj.get("exceptionLogSize")==null?0:Long.valueOf(jsonObj.get("exceptionLogSize").toString());
						map.put("exceptionLogSize",size);
						map.put("exceptionLog",size>0?"*":null);
					}
				}
				if(!list.isEmpty() && list.get(0).get("executionTime")==null) {
					list.get(0).put("executionTimeFlow",this.getDiffTimeStamp(list.get(0).get("executionStartTime").toString()));
				}
			}
			
			return list;
		}

		@Override
		public Map<String, Object> getTaskExecutionInfo(String executionNo) {
			return taskExecutionContext.get(executionNo);
		}

		@Override
		public synchronized void insertTaskExecutionInfo(TaskExecutionVo vo) {
			if(vo!=null && !taskExecutionContext.containsKey(vo.getExecutionNo())){
				
				Map<String,Object> item = new HashMap<>();
				item.put("executionNo",vo.getExecutionNo()); //수행번호
				item.put("executionName",vo.getExecutionName()); //수행이름
				item.put("executionType",vo.getExecutionType()); //수행타입(inner,outter)
				item.put("executionConfig",vo.getExecutionConfig()); //수행설정
				item.put("executionPostProcess",vo.getExecutionPostProcess()); //수행후처리정보
				item.put("executionStartTime",vo.getExecutionStartTime());	//수행시작일시
				item.put("executionStartTimeFormat",this.getTimeFormat(vo.getExecutionStartTime()));	//수행시작일시포멧			
				item.put("executionEndTime",vo.getExecutionEndTime()); //수행종료일시
				item.put("executionEndTimeFormat",this.getTimeFormat(vo.getExecutionEndTime()));	//수행종료일시포멧			
				item.put("executionServer",vo.getExecutionServer()); //수행서버
				item.put("executionTime",vo.getExecutionTime()); //수행시간
				item.put("executionTimeFlow",null); //수행시간흐름
				item.put("executionLogSize",null); //수행로그크기
				item.put("executionLog",null); //수행로그
				item.put("exceptionLogSize",null); //예외로그크기
				item.put("exceptionLog",null); //예외로그
				item.put("exceptionType",vo.getExceptionType()); //예외구분
				item.put("exceptionYn",vo.getExceptionYn()); //예외여부
				item.put("metaLogFileCount",vo.getMetaLogFileCount()); //수행타입(outter)인 경우 값이 있음							
				item.put("metaLogFileInfo",vo.getMetaLogFileInfo()); //수행타입(outter)인 경우 값이 있음
				item.put("sysCreationDate",System.currentTimeMillis());
				item.put("sysUpdateDate",null);
				taskExecutionContext.put(vo.getExecutionNo(),item);
			}
			return;
		}

		@Override
		public synchronized void updateTaskExecutionInfo(TaskExecutionVo vo) {
			if(vo==null || !taskExecutionContext.containsKey(vo.getExecutionNo())) {return;}
			Map<String,Object> tobe = batchUtil.getObjectMapperUtil().getMapper().convertValue(vo,new TypeReference<Map<String,Object>>() {});
			Map<String,Object> asis = taskExecutionContext.get(vo.getExecutionNo());
			for(String key:tobe.keySet()) {
				if(tobe.get(key)==null || tobe.get(key).equals("")) {continue;}
				if(tobe.get(key)!=asis.get(key)) {
					if(key.equals("executionEndTime") && !tobe.get(key).equals("")) {asis.put("executionEndTimeFormat",this.getTimeFormat(tobe.get(key).toString()));}
					asis.put(key,tobe.get(key));
				}
			}
			asis.put("sysUpdateDate",System.currentTimeMillis());
			taskExecutionContext.put(vo.getExecutionNo(),asis);
			return;
		}

		@Override
		public synchronized void clearTaskLog(TaskExecutionVo vo) {
			this.shrinkContext(vo);
		}
	    
		@Override
		public void postProcess(TaskPack.Task task,Properties taskLogPolicyProps) throws Exception {
			if(task==null || taskLogPolicyProps==null) {return;}
			this.doPostProcess("Y",task,taskLogPolicyProps);
			return;
		}

		public String getDiffTimeStamp(String basedate) {
			Date date = null;
			try {date = batchUtil.getTimeUtil().getDate(basedate,"yyyyMMddHHmmssSSS");}catch(Exception e) {}
			return String.valueOf(System.currentTimeMillis()-date.getTime());
		}
		
		public String getTimeFormat(String dateStr) {
			if(dateStr==null || dateStr.equals("")) {return null;}
			return batchUtil.getTimeUtil().getFormatDate(dateStr,"yyyyMMddHHmmssSSS","yyyy.MM.dd HH:mm:ss SSS");
		}

		public long getTaskLogFileSize(String path) {
	        File file=new File(path);
	        return file.exists()?file.length():-1L;
	    }

		public void deleteTaskLogFile(List<String> list) {
	        if(list==null) {return;}        
	        for(int i=0;i<list.size();i++) {this.deleteTaskLogFile(list.get(i));}
	        return;
	    }

		public void deleteTaskLogFile(String path) {
	        if(path==null || path.equals("")) {return;}
	        File file=new File(path);
	        if(file.exists()) {log.debug(">> delete : {}", path);file.delete();}        
	        return;
	    }  
	    
		public String getTaskLogFilePath(TaskPack.Task task,String logType) {
	        if(task==null || task.getTaskConfig()==null) {return null;}
	        String logFilePathPattern=null;
	        if(logType.equals("executionLog")) {logFilePathPattern=task.getTaskConfig().getExecutionLogFilePathPattern();}
	        if(logType.equals("exceptionLog")) {logFilePathPattern=task.getTaskConfig().getExceptionLogFilePathPattern();}
	        return logFilePathPattern.replaceAll("[*]",task.getTaskToken());
	    } 

		public Map<String, Object> getTaskLogFileData(TaskExecutionVo vo) {
			if(vo==null || vo.getExecutionPostProcess()==null) {return null;}

			JSONObject jsonObj=batchUtil.getJsonUtil().getJsonObject(vo.getExecutionPostProcess());
			log.debug(">> jsonObj:{}",jsonObj);
			if(jsonObj==null) {return null;}

			boolean isEnable=true;
			if(isEnable && jsonObj.get("logFileDbStoreYn").toString().equals(LOG_FILE_DB_STORE_Y)) {isEnable=false;}
			if(isEnable && jsonObj.get("logFileRemoveYn").toString().equals(LOG_FILE_REMOVE_Y)) {isEnable=false;}
			if(!isEnable) {return null;}

	   		String logFilePath=null;
			if(vo.getLogType().startsWith("executionLog")) {logFilePath=jsonObj.get("executionLogFilePath")==null?null:jsonObj.get("executionLogFilePath").toString();}
			if(vo.getLogType().startsWith("exceptionLog")) {logFilePath=jsonObj.get("exceptionLogFilePath")==null?null:jsonObj.get("exceptionLogFilePath").toString();}
			if(vo.getLogType().startsWith("metaLog")) {
				String metaLogFileInfo = String.valueOf(jsonObj.get("metaLogFileInfo"));
				if(StringUtils.hasText(metaLogFileInfo)) {
					String[] metaLogs = Arrays.stream(metaLogFileInfo.split(",")).sorted().toArray(String[]::new);
					logFilePath = metaLogs[Integer.parseInt(vo.getLogType().replace("metaLog", ""))-1];
				}
			}
			if(logFilePath==null) {return null;}
			
			long logSeq=vo.getLogSeq();
			long logExtractSize=(vo.getLogExtractSize()<=0?LOG_READ_LINE_SIZE:vo.getLogExtractSize());
			
			Map<String,Object> map=new HashMap<>();
			map.put("logData",this.getTaskLogFileBlock(logFilePath,logSeq,logExtractSize));
			
			return map;
		}
		
	    public String getTaskLogFileBlock(String path,long blockNo,long blockSize) {    	
	        if(path==null || path.equals("") || blockNo<=0 || blockSize<=0) {return null;}

	        File file=new File(path);
	        if(!file.exists()) {return null;}

	        long readCount=0;
			long readSkip=(blockNo-1)*blockSize;		
			
			StringBuffer buffer=null;
			BufferedReader bufReader=null;
			try {
				boolean isEnable=true;
		        bufReader=new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8));
				for(long i=0;i<readSkip;i++) {if(bufReader.readLine()==null) {isEnable=false;break;}}	
			
				while(isEnable) {
		        	String readLine=bufReader.readLine();        	
		        	if(readLine==null) {break;}
		        	
		            if(buffer==null) {buffer=new StringBuffer();}   
		            buffer.append(readLine).append(System.lineSeparator());
		            
		            readCount++;
		            if(readCount==blockSize) {break;}
		        } 
			}catch(Exception e){
				throw new RuntimeException(e);
			}finally{
				try {if(bufReader!=null) {bufReader.close();}}
				catch(IOException e) {e.printStackTrace();}
			}
			
	        String logData=(buffer==null?null:buffer.toString());
	        return logData;
	    }
	    
		private void doPostProcess(String postProcessAsyncYn,TaskPack.Task task,Properties taskLogPolicyProps) {

			if(postProcessAsyncYn.equals("Y")) {
				new Thread(new Runnable() {
			        public void run() {try{doPostProcess("N",task,taskLogPolicyProps);} catch (Exception e) {throw new RuntimeException(e);}}
			    }).start();
				return;
			}
			
			if(postProcessAsyncYn.equals("N")) {

		    	long postProcessStartTime=System.currentTimeMillis();

		    	String executionNo=task.getExecutionNo();

				this.initContext(executionNo);

		    	String executionLogFilePath=this.getTaskLogFilePath(task,"executionLog");
		    	long executionLogSize=this.getTaskLogFileSize(executionLogFilePath);
		    	if(executionLogSize<0) {executionLogFilePath=null;}
		    	
		    	String exceptionLogFilePath=this.getTaskLogFilePath(task,"exceptionLog");
		    	long exceptionLogSize=this.getTaskLogFileSize(exceptionLogFilePath);
		    	if(exceptionLogSize<0) {exceptionLogFilePath=null;}
		    	
		    	log.debug(">> taskLogPolicyProps:{}",taskLogPolicyProps);
		    	
		    	boolean isEnable=true;
		    	
		    	String logFileDbStoreYn="N";
		        if(isEnable && TaskLogContext.LOG_FILE_DB_STORE_Y.equals(taskLogPolicyProps.getProperty("logFileDbStoreYn"))) {	            
		            try {
		            	Thread.sleep(500);
			            this.storeTaskLog(executionNo,"executionLog",executionLogFilePath);
			            this.storeTaskLog(executionNo,"exceptionLog",exceptionLogFilePath);
		            }catch(Exception e) {
		            	isEnable=false;
		            	log.error(">> logFileDbStore-exception-message:{}",e.getMessage());
		            	//throw new RuntimeException(e);
		            }
	                if(isEnable) {
			            Map<String,Object> refContext = taskExecutionContext.get(executionNo);
			            executionLogSize=refContext.get("executionLogSize")==null?0:Long.parseLong(refContext.get("executionLogSize").toString());
			            exceptionLogSize=refContext.get("exceptionLogSize")==null?0:Long.parseLong(refContext.get("exceptionLogSize").toString());	            	            
			            logFileDbStoreYn="Y";
		            }
		    	}
		
		    	String logFileRemoveYn="N";
		        if(isEnable && TaskLogContext.LOG_FILE_REMOVE_Y.equals(taskLogPolicyProps.getProperty("logFileRemoveYn"))) {
		        	try {
			            Thread.sleep(500);
			            this.deleteTaskLogFile(executionLogFilePath);
			            this.deleteTaskLogFile(exceptionLogFilePath);
		        	}catch(Exception e) {
		            	isEnable=false;
		            	log.error(">> logFileRemove-exception-message:{}",e.getMessage());
		            	//throw new RuntimeException(e);
		            }
		        	if(isEnable) {
		        		logFileRemoveYn="Y";
		        	}
		        }
		
		        String logHistoryShrinkYn="N";
		        if(isEnable && TaskLogContext.LOG_HISTORY_SHRINK_Y.equals(taskLogPolicyProps.getProperty("logHistoryShrinkYn"))) { 
		        	try {
			        	TaskExecutionVo vo=new TaskExecutionVo();
			            vo.setExecutionName(task.getName());
			            vo.setClearBaseDays(taskLogPolicyProps.getProperty("logHistoryShrinkBaseDays"));
			            vo.setClearBaseRows(taskLogPolicyProps.getProperty("logHistoryShrinkBaseRows"));
			            this.shrinkContext(vo);
		        	}catch(Exception e) {
		            	isEnable=false;
		            	log.error(">> logHistoryShrink-exception-message:{}",e.getMessage());
		            	//throw new RuntimeException(e);
		            }
		        	if(isEnable) {
		        		logHistoryShrinkYn="Y";
		        	}
		        }
		
		    	long postProcessEndTime=System.currentTimeMillis();

				Set<String> metaLogFileSet = null;
				if(task.getDomain() == Domain.outter && task.getAgentResponse()!=null) {					
					metaLogFileSet = task.getAgentResponse().getProcessExecuteLogFile();
					if(metaLogFileSet!=null && metaLogFileSet.isEmpty()) {metaLogFileSet=null;}
				}
				
		    	Map<String,Object> executionPostProcess=new HashMap<>();
		        executionPostProcess.put("taskToken",task.getTaskToken());
		        executionPostProcess.put("taskType",task.getDomain());
		        executionPostProcess.put("executionLogFilePath",executionLogFilePath);
		        executionPostProcess.put("executionLogSize",executionLogSize);
		        executionPostProcess.put("executionLogLevel",task.getTaskConfig().getExecutionLogLevel());
		        executionPostProcess.put("exceptionLogFilePath",exceptionLogFilePath);
		        executionPostProcess.put("exceptionLogSize",exceptionLogSize);
		        executionPostProcess.put("exceptionLogLevel",task.getTaskConfig().getExceptionLogLevel());
		        executionPostProcess.put("metaLogFileCount",metaLogFileSet==null?0:metaLogFileSet.size());
		        executionPostProcess.put("metaLogFileInfo",metaLogFileSet==null?"":String.join(",", metaLogFileSet));
		        executionPostProcess.put("logFileDbStoreYn",logFileDbStoreYn);
		        executionPostProcess.put("logFileRemoveYn",logFileRemoveYn);
		        executionPostProcess.put("logHistoryShrinkYn",logHistoryShrinkYn);
		        executionPostProcess.put("postProcessStartTime",batchUtil.getTimeUtil().getFormatDate(postProcessStartTime,"yyyyMMddHHmmssSSS"));
		        executionPostProcess.put("postProcessEndTime",batchUtil.getTimeUtil().getFormatDate(postProcessEndTime,"yyyyMMddHHmmssSSS"));
		        executionPostProcess.put("postProcessTime",postProcessEndTime-postProcessStartTime);
		        
		        
				if(!taskExecutionContext.containsKey(executionNo)) {return;}
				taskExecutionContext.get(executionNo).put("executionPostProcess",batchUtil.getJsonUtil().toJson(executionPostProcess));
				taskExecutionContext.get(executionNo).put("sysUpdateDate",System.currentTimeMillis());
				
				this.saveContext(executionNo);

			}
	       return;
		}
		
	
	};
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
