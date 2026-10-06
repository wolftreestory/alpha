package com.alpha.base.support.batch.context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
public class TaskLogContextOnOracle extends AbstractTaskLogContext {

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/*
	 *  Oracle DB에 로그를 관리하기 위한 설정
	 *  com.alpha.base.config.batch.BatchConfig.java에서 필요시 적용됨
	 */	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private TaskExecutionMapper taskExecutionMapper;
        
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void setTaskExecutionMapper(TaskExecutionMapper taskExecutionMapper) {this.taskExecutionMapper =  taskExecutionMapper;}	
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public TaskLogContextOnOracle(BatchUtil batchUtil) {
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

		TaskExecutionVo paramVo =  new TaskExecutionVo();
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
		
		Map<String,Object> item =  taskExecutionContext.get(executionNo);
		if(item == null || item.isEmpty()) {return;}
		
		TaskExecutionVo vo =  this.getBatchUtil().getObjectMapperUtil().convert(item, TaskExecutionVo.class);
		this.taskExecutionMapper.insertTaskExecutionInfo(vo);
		
		this.getBatchUtil().getTransactionUtil().commit(txStatus);
		this.getBatchUtil().getTransactionUtil().end(txStatus);  
		
		return;
	}

	@Override
	public synchronized void saveContext(String executionNo) {
		log.debug(">> batch.taskLogContext save(db-update)");

		TransactionStatus txStatus = this.getBatchUtil().getTransactionUtil().start(TransactionDefinition.PROPAGATION_REQUIRES_NEW);  
		
		Map<String,Object> item =  taskExecutionContext.get(executionNo);
		if(item == null || item.isEmpty()) {return;}
		
		TaskExecutionVo vo =  this.getBatchUtil().getObjectMapperUtil().convert(item, TaskExecutionVo.class);
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

    	Map<String,Object> map =  this.getTaskLogFileData(vo);
    	if(map != null) {return map;}
    	
        String logType = vo.getLogType();
        
    	boolean isEnable = true;
    	if(isEnable && (logType == null || logType.equals(""))) {isEnable = false;}  
    	if(isEnable) {map = this.getTaskExecutionInfo(vo.getExecutionNo());}
        if(isEnable && map == null) {isEnable = false;}            
        if(isEnable && !map.containsKey(logType)) {isEnable = false;}  
        if(isEnable && !(map.get(logType) instanceof Clob)) {isEnable = false;}           
        if(isEnable) {
	        long executionLogSize = 0L;
	        if(logType.equals("executionLog") && map.get("executionLogSize") != null) {executionLogSize = Long.parseLong(map.get("executionLogSize").toString());}
	        if(logType.equals("exceptionLog") && map.get("exceptionLogSize") != null) {executionLogSize = Long.parseLong(map.get("exceptionLogSize").toString());} 
	        if(executionLogSize>LOG_PRINT_BLOCK_SIZE) {map.put("logData",this.getBatchUtil().getLobConverterUtil().doClobToString(map.get(logType)));}
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
	            if(buffer.length() > LOG_READ_BLOCK_SIZE) {
	                log.debug("buffer.length(): {}",buffer.length());
	                this.storeTaskLogToDb(executionNo,logType,buffer.toString());                
	                buffer = null;
	            }
	        }
	        if(buffer != null) {
		        log.debug("buffer.length(): {}",buffer.length());        
		        this.storeTaskLogToDb(executionNo,logType,buffer.toString());
	        }
		} catch(Exception e) {
			throw new RuntimeException(e);
   		}
        
        log.debug(">> elapsed : {}(ns)",(new DecimalFormat("#,###")).format(System.nanoTime()-nanoTimeStart)); 
        log.debug(">> storeTaskLog end");

        return;
    }

	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void storeTaskLogToDb(String executionNo,String logType,String data) throws Exception {
        if(executionNo == null || executionNo.equals("")) {return;}
        if(data == null || data.equals("")) {return;}
        
		TransactionStatus txStatus = this.getBatchUtil().getTransactionUtil().start(TransactionDefinition.PROPAGATION_REQUIRES_NEW);    

        Map<String,Object> taskExecutionInfo = this.getTaskExecutionInfo(executionNo);
        
        if(taskExecutionInfo.get(logType) == null) {
            TaskExecutionVo vo = new TaskExecutionVo(executionNo);
            vo.setExecutionLogEmptyYn("Y");
            this.taskExecutionMapper.updateTaskExecutionInfo(vo);
            this.getBatchUtil().getTransactionUtil().commit(txStatus);
            
            this.storeTaskLogToDb(executionNo,logType,data);
            return;
        }
        
        try {
        	
			byte[] dataBytes =  data.getBytes("UTF-8");
			
    		String key = "";
	        if(logType.equals("executionLog")) {key = "executionLogSize";}
	        if(logType.equals("exceptionLog")) {key = "exceptionLogSize";}	        
	        int baseSize = taskExecutionContext.get(executionNo).get(key) == null?0:Integer.parseInt(taskExecutionContext.get(executionNo).get(key).toString());
	        taskExecutionContext.get(executionNo).put(key,baseSize+dataBytes.length);
	        
            this.taskExecutionMapper.updateTaskExecutionInfo(new TaskExecutionVo(executionNo));	            
            Clob clob = (Clob)taskExecutionInfo.get(logType);           
            BufferedWriter writer = new BufferedWriter(clob.setCharacterStream(clob.length() == 0?0L:clob.length()+1));
            writer.write(data);
            writer.close();
            
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