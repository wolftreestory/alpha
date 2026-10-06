package com.alpha.base.support.batch;

import java.sql.Clob;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.view.AbstractView;

import com.alpha.base.BaseUtil;
import com.alpha.base.support.batch.TaskLogContextPack.TaskLogContext;
import com.alpha.base.support.batch.TaskLogHandlerPack.TaskLogHandler;
import com.alpha.base.support.batch.vo.TaskExecutionVo;
import com.alpha.base.support.util.FileUtilPack.FileUtil.FetchDataFunction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskLogFileDownload extends AbstractView {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private TaskExecutionVo taskExecutionInfo;

	private final String logFileName;  

    private final TaskLogHandler taskLogHandler;

    public TaskLogFileDownload(String logFileName, TaskLogHandler taskLogHandler) {
		this.logFileName = logFileName;
		this.taskLogHandler = taskLogHandler;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
	@Override
    protected void renderMergedOutputModel(Map<String,Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        String executionNo=(request.getAttribute("executionNo")==null?"":request.getAttribute("executionNo").toString());
        if(executionNo==null || executionNo.equals("")) {return;}
        request.removeAttribute("executionNo");
        log.debug(">> executionNo:{}",executionNo);

        String logType=(request.getAttribute("logType")==null?"":request.getAttribute("logType").toString());
        if(logType==null || logType.equals("")) {return;}
        request.removeAttribute("logType");
        log.debug(">> logType:{}",logType);
        
        boolean isEnable=false;
        if(!isEnable && logType.startsWith("executionLog")) {isEnable=true;}
        if(!isEnable && logType.startsWith("exceptionLog")) {isEnable=true;}
        if(!isEnable && logType.startsWith("metaLog")) {isEnable=true;}
        if(!isEnable) {return;}
        
		Map<String,Object> taskExecutionInfo=this.taskLogHandler.getTaskLogContext().getTaskExecutionInfo(executionNo);
		if(taskExecutionInfo==null) {return;}
		                
        String executionPostProcess=(taskExecutionInfo.get("executionPostProcess")==null?"":taskExecutionInfo.get("executionPostProcess").toString());        
        log.debug(">> executionPostProcess:{}",executionPostProcess);
        
        if(executionPostProcess!=null && !executionPostProcess.equals("")) {
			JSONObject jsonObj=BaseUtil.getJsonUtil().getJsonObject(executionPostProcess);
			
			isEnable=true;
			if(isEnable && jsonObj.get("logFileDbStoreYn").toString().equals(TaskLogContext.LOG_FILE_DB_STORE_Y)) {isEnable=false;}
			if(isEnable && jsonObj.get("logFileRemoveYn").toString().equals(TaskLogContext.LOG_FILE_REMOVE_Y)) {isEnable=false;}
			if(isEnable) {
				String filePath=null;
		        if(logType.startsWith("executionLog")) {filePath=jsonObj.get("executionLogFilePath").toString();}
		        if(logType.startsWith("exceptionLog")) {filePath=jsonObj.get("exceptionLogFilePath").toString();}
		        if(logType.startsWith("metaLog")) {
					String metaLogFileInfo = String.valueOf(jsonObj.get("metaLogFileInfo"));
					if(StringUtils.hasText(metaLogFileInfo)) {
						String[] metaLogs = Arrays.stream(metaLogFileInfo.split(",")).sorted().toArray(String[]::new);						
						filePath = metaLogs[Integer.parseInt(logType.replace("metaLog", ""))-1];								
					}
		        }
		        if(filePath!=null) {
		        	BaseUtil.getFileUtil().pushout(filePath, request, response);		        
		        }
		        return;
			}
        }
     
        String fileName="";
        if(logType.startsWith("executionLog")) {fileName=this.logFileName+"."+executionNo+"."+taskExecutionInfo.get("executionName")+".log";}
        if(logType.startsWith("exceptionLog")) {fileName=this.logFileName+"."+executionNo+"."+taskExecutionInfo.get("executionName")+".error.log";}    
        if(logType.startsWith("metaLog")) {fileName=this.logFileName+"."+executionNo+"."+taskExecutionInfo.get("executionName")+".log";}

	    if(taskExecutionInfo.get(logType) instanceof String) {
	    	TaskLogFileDownload refInstance=this;
	    	refInstance.taskExecutionInfo=new TaskExecutionVo(executionNo);
	    	refInstance.taskExecutionInfo.setLogType(logType);
	    	BaseUtil.getFileUtil().pushout(new FetchDataFunction() {
	    		@Override
				public byte[] getData(long fetchNo) throws Exception {
	    			refInstance.taskExecutionInfo.setLogSeq(++fetchNo);
	    			Map<String,Object> executionLogInfo=refInstance.taskLogHandler.getTaskLogContext().extractTaskLog(refInstance.taskExecutionInfo);
					Object logData=(executionLogInfo!=null?executionLogInfo.get("logData"):null);
					if(logData instanceof String) {return logData.toString().getBytes("UTF-8");}
					if(logData instanceof byte[]) {return (byte[])logData;}
					return null;
				}
	    	},fileName, request, response);
			return;	    	
	    }
	    
		if(taskExecutionInfo.get(logType) instanceof Clob) {
			BaseUtil.getFileUtil().pushout((Clob)taskExecutionInfo.get(logType), fileName, request, response);
			return;
        }
     
        return;
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
