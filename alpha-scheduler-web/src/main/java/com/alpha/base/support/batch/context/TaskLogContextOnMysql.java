package com.alpha.base.support.batch.context;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import com.alpha.base.support.batch.BatchUtil;
import com.alpha.base.support.batch.TaskLogContextPack.AbstractTaskLogContext;
import com.alpha.base.support.batch.mapper.TaskExecutionMapper;
import com.alpha.base.support.batch.vo.TaskExecutionVo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskLogContextOnMysql extends AbstractTaskLogContext {

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/*
	 *  MySQL DB에 로그를 관리하기 위한 설정
	 *  com.alpha.base.config.batch.BatchConfig.java에서 필요시 적용됨
	 */	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private TaskExecutionMapper taskExecutionMapper;
        
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void setTaskExecutionMapper(TaskExecutionMapper taskExecutionMapper) {this.taskExecutionMapper = taskExecutionMapper;}	
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public TaskLogContextOnMysql(BatchUtil batchUtil) {
		super(batchUtil);
	}	
	
	
	@Override
	public String getLogFileDbStoreConfigEnableYn() {return "Y";}
	
	@Override
	public String getLogFileRemoveConfigEnableYn() {return "Y";}
	
	@Override
	public String getLogHistoryShirnkConfigEnableYn() {return "Y";}

	@Override
	public void loadContext() {
		log.debug(">> batch.taskLogContext load");

		TaskExecutionVo paramVo = new TaskExecutionVo();
		paramVo.setSelectMode("load");
		List<Map<String,Object>> list = this.taskExecutionMapper.selectTaskExecutionList(paramVo);		
		for(Map<String,Object> context : list) {
			//log.debug(">> readContext:{}",context);
			taskExecutionContext.put(context.get("executionNo").toString(), context);
		}
		return;
	}
	
	@Override
	public synchronized void initContext(String executionNo) {
		log.debug(">> batch.taskLogContext init(db-insert)");

		TransactionStatus txStatus = this.getBatchUtil().getTransactionUtil().start(TransactionDefinition.PROPAGATION_REQUIRES_NEW);  
		
		Map<String,Object> item = taskExecutionContext.get(executionNo);
		if(item == null || item.isEmpty()) {return;}
		
		TaskExecutionVo vo = this.getBatchUtil().getObjectMapperUtil().convert(item, TaskExecutionVo.class);
		this.taskExecutionMapper.insertTaskExecutionInfo(vo);
		
		this.getBatchUtil().getTransactionUtil().commit(txStatus);
		this.getBatchUtil().getTransactionUtil().end(txStatus);  
	}	

	@Override
	public synchronized void saveContext(String executionNo) {
		log.debug(">> batch.taskLogContext save(db-update)");

		TransactionStatus txStatus = this.getBatchUtil().getTransactionUtil().start(TransactionDefinition.PROPAGATION_REQUIRES_NEW);  
		
		Map<String,Object> item = taskExecutionContext.get(executionNo);
		if(item == null || item.isEmpty()) {return;}
		
		TaskExecutionVo vo = this.getBatchUtil().getObjectMapperUtil().convert(item, TaskExecutionVo.class);
		this.taskExecutionMapper.updateTaskExecutionInfo(vo);
		
		this.getBatchUtil().getTransactionUtil().commit(txStatus);
		this.getBatchUtil().getTransactionUtil().end(txStatus);  
		
		return;
	}
	
	@Override
	public synchronized void shrinkContext(TaskExecutionVo vo) {
		if(vo == null) {return;}

		log.debug(">> batch.taskLogContext shrink");
		
	    vo.setSelectMode("delete");
	    List<Map<String,Object>> list = this.taskExecutionMapper.selectTaskExecutionList(vo);
	    
	    List<String> filePathList = new ArrayList<>();
	    for(int i = 0;i<list.size();i++) {
			if(list.get(i).get("executionPostProcess") == null) {continue;}			
			JSONObject jsonObj = this.getBatchUtil().getJsonUtil().getJsonObject(list.get(i).get("executionPostProcess").toString());
			if(jsonObj == null) {continue;}
			if(jsonObj.get("executionLogFilePath") != null) {filePathList.add(jsonObj.get("executionLogFilePath").toString());}
			if(jsonObj.get("exceptionLogFilePath") != null) {filePathList.add(jsonObj.get("exceptionLogFilePath").toString());}				
	    }
	    this.deleteTaskLogFile(filePathList);
	    
		//log.debug(">> list:{}",list);
    	for(Map<String, Object> target : list) {
    		log.debug(">> remove target:{}",target.get("executionNo"));
    		taskExecutionContext.remove(target.get("executionNo"));
    	}
    	
	    this.taskExecutionMapper.deleteTaskExecutionInfo(vo);
		this.taskExecutionMapper.deleteTaskExecutionLogInfo(vo);
    	
		return;
	}

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public Map<String, Object> extractTaskLog(TaskExecutionVo vo) {
		if(vo == null) {return null;}

    	Map<String,Object> map = this.getTaskLogFileData(vo);
    	if(map != null) {return map;}

    	if(vo.getLogExtractMode() == null || vo.getLogExtractMode().equals("")) {
    		vo.setLogExtractMode(LOG_EXTRACT_MODE_BLOCK);
    	}
    	if(vo.getLogExtractMode().equals(LOG_EXTRACT_MODE_CHUNK)) {
        	vo.setLogChunkStartPosition(vo.getLogExtractSize()*(vo.getLogSeq()-1));
        	vo.setLogChunkSize(vo.getLogExtractSize());
    	}

        List<Map<String,Object>> logList = this.taskExecutionMapper.selectTaskLogList(vo);
        if(logList == null || logList.size() == 0) {return null;}

        if(vo.getLogExtractMode().equals(LOG_EXTRACT_MODE_BLOCK)) {
        	map = logList.get(0);
        } 
        
        if(vo.getLogExtractMode().equals(LOG_EXTRACT_MODE_CHUNK)) {
        	StringBuffer sBuffer = null;            	
        	ByteArrayOutputStream bStream = null;
        	
        	try {
	            for(int i = 0;i<logList.size();i++) {
	            	Object logData = logList.get(i).get("logData");
	            	if(logData == null) {continue;}                	
					if(logData instanceof String) {
						if(sBuffer == null) {sBuffer = new StringBuffer();}						
						sBuffer.append(logData.toString());
					}
					if(logData instanceof byte[]) {
						if(bStream == null) {bStream = new ByteArrayOutputStream();}
						bStream.write((byte[])logData);
					}
	            }                 
	        	map = new HashMap<>();         
	        	if(sBuffer != null && bStream == null) {map.put("logData",sBuffer.toString());}
	        	if(sBuffer == null && bStream != null) {map.put("logData",bStream.toString());}
	            if(bStream != null) {bStream.close();}
    		} catch(Exception e) {
   			 	throw new RuntimeException(e);
	   		} finally {
	   			try {if(bStream != null) {bStream.close();}}
	   			catch(IOException e) {e.printStackTrace();}
	   		}
        }
        
        return map;
	}
	
	@Override
    public void storeTaskLog(String executionNo,String logType,String filePath) {
        log.debug(">> storeTaskLog start");        

        if(logType == null || logType.equals("")) {return;}
        if(filePath == null || filePath.equals("")) {return;}
        
        File file = new File(filePath);
        if(!file.exists()) {return;}

        long nanoTimeStart = System.nanoTime();

        try(BufferedReader bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
	
	        StringBuffer buffer = null;
	        String line = null;
	        while((line = bufReader.readLine()) != null) {
	            if(buffer == null) {buffer = new StringBuffer();}
	            buffer.append(line).append(System.lineSeparator());
	            if(buffer.length()>LOG_READ_BLOCK_SIZE) {
	                log.debug(">> buffer: {}",buffer.length());
	                this.storeTaskLogToDb(executionNo,logType,buffer.toString());                
	                buffer = null;
	            }
	        }
	        if(buffer != null) {
		        log.debug(">> buffer: {}",buffer.length());        
		        this.storeTaskLogToDb(executionNo,logType,buffer.toString());
	        }
		} catch(Exception e) {
			 throw new RuntimeException(e);
		}

        log.debug(">> elapsed:{}(ns)",(new DecimalFormat("#,###")).format(System.nanoTime()-nanoTimeStart)); 
        log.debug(">> storeTaskLog end");

        return;
    }

	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void storeTaskLogToDb(String executionNo,String logType,String data) {
        if(executionNo == null || executionNo.equals("")) {return;}
        if(data == null || data.equals("")) {return;}
        
		TransactionStatus txStatus = this.getBatchUtil().getTransactionUtil().start(TransactionDefinition.PROPAGATION_REQUIRES_NEW);    
            
		try {
			byte[] dataBytes = data.getBytes("UTF-8");
			
    		String key = "";
	        if(logType.equals("executionLog")) {key = "executionLogSize";}
	        if(logType.equals("exceptionLog")) {key = "exceptionLogSize";}	        
			long baseSize = taskExecutionContext.get(executionNo).get(key) == null?0:Long.parseLong(taskExecutionContext.get(executionNo).get(key).toString());
	        taskExecutionContext.get(executionNo).put(key,baseSize+dataBytes.length);

    		TaskExecutionVo vo = new TaskExecutionVo(executionNo);
    		vo.setExecutionName(taskExecutionContext.get(executionNo).get("executionName").toString());
    		vo.setLogType(logType);
    		vo.setLogData(new ByteArrayInputStream(dataBytes));
    		this.taskExecutionMapper.insertTaskExecutionLogInfo(vo);
	            
        }catch(Exception e) {
        	this.getBatchUtil().getTransactionUtil().rollback(txStatus);
            throw new RuntimeException(e.getCause());
        }

		this.getBatchUtil().getTransactionUtil().commit(txStatus);
		this.getBatchUtil().getTransactionUtil().end(txStatus);    	
  
        return;
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}