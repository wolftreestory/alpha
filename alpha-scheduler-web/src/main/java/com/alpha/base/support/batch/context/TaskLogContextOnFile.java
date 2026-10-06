package com.alpha.base.support.batch.context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.util.StringUtils;

import com.alpha.base.exception.SystemException;
import com.alpha.base.support.batch.BatchUtil;
import com.alpha.base.support.batch.TaskLogContextPack.AbstractTaskLogContext;
import com.alpha.base.support.batch.vo.TaskExecutionVo;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskLogContextOnFile extends AbstractTaskLogContext {

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/*
	 *  Disk 저장소(file)에 로그를 관리하기 위한 설정
	 *  com.alpha.base.config.batch.BatchConfig.java에서 필요시 적용됨
	 */	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String contextLocation;	
	private String contextFileExt = "context"; //default

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void setContextLocation(String contextLocation) {this.contextLocation = contextLocation;}
	public void setContextFileExt(String contextFileExt) {this.contextFileExt = contextFileExt;}

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public TaskLogContextOnFile(BatchUtil batchUtil) {
		super(batchUtil);
	}	
	
	@Override
	public String getLogFileDbStoreConfigEnableYn() {return "N";}
		
	@Override
	public String getLogFileRemoveConfigEnableYn() {return "Y";}
	
	@Override
	public String getLogHistoryShirnkConfigEnableYn() {return "Y";}
	
	@Override
	public void loadContext() {
		log.debug(">> batch.taskLogContext load");

		this.setLogFileDbStoreYn("N");
		
		File[] files = this.getContextFiles();
		if(files == null || files.length == 0) {return;}
		
		for(File file:files) {
			try(BufferedReader bufReader = new BufferedReader(new FileReader(file))){
				String readData = bufReader.readLine();
				if(readData == null || readData.equals("")) {continue;}
				
				//반드시 Map<String,String>으로 mapping해야 자동형변환방지 됨
				Map<String,String> readContext = this.getBatchUtil().getObjectMapperUtil().readValue(readData, new TypeReference<Map<String,String>>() {});
				if(readContext == null || readContext.isEmpty()) {continue;}
				
				//log.debug(">> readContext:{}",readContext);
				taskExecutionContext.put(readContext.get("executionNo").toString(), new HashMap<>(readContext));
			} catch(Exception e) {
				throw new RuntimeException(e.getMessage());
			}
		}
	}

	@Override
	public synchronized void initContext(String executionNo) {/*처리없음*/}

	@Override
	public synchronized void saveContext(String executionNo) {
		log.debug(">> batch.taskLogContext save(file-create)");

		String writeData = this.getBatchUtil().getJsonUtil().toJson(taskExecutionContext.get(executionNo));
		if(writeData == null || writeData.equals("")) {return;}

		try(BufferedWriter bufWriter = new BufferedWriter(new FileWriter(this.getContextFilePath(executionNo),true))) {
			bufWriter.write(writeData);
			bufWriter.newLine();			
		} catch(Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	
	@Override
	public synchronized void shrinkContext(TaskExecutionVo vo) {
		if(vo == null) {return;}

		log.debug(">> batch.taskLogContext shrink");
		
		if(vo.getExecutionNo() == null) {vo.setExecutionNo("");}
		if(vo.getExecutionName() == null) {vo.setExecutionName("");}
		if(vo.getDeleteAllYn() == null) {vo.setDeleteAllYn("N");}
		
		// 메인화면 '수행이력 삭제'에 대한 처리
		if(vo.getDeleteAllYn().equals("Y") && vo.getExecutionNo().equals("") && vo.getExecutionName().equals("")) {
			this.removeTaskHistory(new ArrayList<>(taskExecutionContext.values()));
			return;
		}

		long currentTimeMills = System.currentTimeMillis();
		
		long clearBaseTimeMills = 0;
		if(StringUtils.hasText(vo.getClearBaseDays())) {
			clearBaseTimeMills = Long.parseLong(vo.getClearBaseDays())*86400000; // 1 day (24*60*60*1000)
		}
		
		long clearBaseExecutionNo = 0;
		if(StringUtils.hasText(vo.getClearBaseRows())) {
			vo.setSelectMode("delete");
			vo.setRownum(Long.parseLong(vo.getClearBaseRows()));
			List<Map<String, Object>> tempList = this.getTaskExecutionList(vo);
			if(tempList != null && tempList.size() == Integer.parseInt(vo.getClearBaseRows())) {
				clearBaseExecutionNo = Long.parseLong(tempList.get(tempList.size()-1).get("executionNo").toString());
			}
		}
		log.debug(">> clearBaseDays:{}, clearBaseTimeMills:{}",vo.getClearBaseDays(),clearBaseTimeMills);
		log.debug(">> clearBaseRows:{}, clearBaseExecutionNo:{}",vo.getClearBaseRows(),clearBaseExecutionNo);

		Map<String, Object> item = null;
		List<Map<String, Object>> removeList = new ArrayList<>();
		Iterator<String> keys = taskExecutionContext.keySet().iterator();
		int cnt=1;
		boolean isRemove = true;
		while(keys.hasNext()) {
			cnt=cnt+1;
			item = taskExecutionContext.get(keys.next());
			isRemove = true;
			if(isRemove && (!vo.getExecutionNo().equals("") && !vo.getExecutionNo().equals(item.get("executionNo")))) {isRemove = false;}
			if(isRemove && (!vo.getExecutionName().equals("") && !vo.getExecutionName().equals(item.get("executionName")))) {isRemove = false;}
			if(isRemove && (!vo.getDeleteAllYn().equals("Y"))) {
				long checkExecutionNo = Long.parseLong(item.get("executionNo").toString());
				long sysCreationDate = Long.parseLong(item.get("sysCreationDate").toString());
				long checkTime = currentTimeMills-sysCreationDate;
				//log.debug(">> checkExecutionNo:{}, sysCreationDate:{}, checkTime:{}",checkExecutionNo,sysCreationDate,checkTime);
				
				if((clearBaseTimeMills > 0 && clearBaseExecutionNo == 0) && checkTime <= clearBaseTimeMills) {isRemove = false; log.debug(">> case1");}
				if((clearBaseTimeMills == 0 && clearBaseExecutionNo > 0) && checkExecutionNo >= clearBaseExecutionNo) {isRemove = false; log.debug(">> case2");}
				if((clearBaseTimeMills > 0 && clearBaseExecutionNo > 0) && checkTime <= clearBaseTimeMills && checkExecutionNo >= clearBaseExecutionNo) {isRemove = false; log.debug(">> case3");}
			}
			
			//log.debug(">> executionNo:{}, isRemove:{}",item.get("executionNo"),isRemove);
			if(isRemove) {removeList.add(item);}
		}

		this.removeTaskHistory(removeList);
		
		return;
	}	
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public Map<String, Object> extractTaskLog(TaskExecutionVo vo) {return this.getTaskLogFileData(vo);}
	
	@Override
	public void storeTaskLog(String executionNo, String logType, String filePath) {/*처리없음*/}   
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private synchronized void removeTaskHistory(List<Map<String, Object>> list) {
		if(list == null || list.isEmpty()) {return;}
		
        List<String> filePathList = new ArrayList<>();

        // 삭제1 : logFile
        for(Map<String, Object> target : list) {
			if(target.get("executionPostProcess") == null) {continue;}			
			JSONObject jsonObj = this.getBatchUtil().getJsonUtil().getJsonObject(target.get("executionPostProcess").toString());
			if(jsonObj == null) {continue;}
			if(jsonObj.get("executionLogFilePath") != null) {filePathList.add(jsonObj.get("executionLogFilePath").toString());}
			if(jsonObj.get("exceptionLogFilePath") != null) {filePathList.add(jsonObj.get("exceptionLogFilePath").toString());}				
        }
        this.deleteTaskLogFile(filePathList);
        
        // 삭제2 : taskExecutionContext, contextFile
    	for(Map<String, Object> target : list) {
    		log.debug(">> remove target:{}",target.get("executionNo"));    		
    		taskExecutionContext.remove(target.get("executionNo"));
    		this.deleteTaskLogFile(this.getContextFilePath(target.get("executionNo").toString()));
    	}
    	
        return;
	}

	private File[] getContextFiles() {
	    String serverInstanceCode = this.getBatchUtil().getSystemUtil().getServerInstanceCode();
	    Path targetDir = Paths.get(this.contextLocation, serverInstanceCode);
	    try {Files.createDirectories(targetDir);} catch (IOException e) {throw new SystemException(e.getMessage());}
	    File[] files = targetDir.toFile().listFiles((dir, name) -> name.startsWith(serverInstanceCode) && name.endsWith(this.contextFileExt));
	    if(files == null) {files = new File[0];}	    
	    return files;
	}

	private String getContextFilePath(String executionNo) {
	    String serverInstanceCode = this.getBatchUtil().getSystemUtil().getServerInstanceCode();
	    Path targetDir = Paths.get(this.contextLocation, serverInstanceCode);
	    try {Files.createDirectories(targetDir);} catch (IOException e) {throw new SystemException(e.getMessage());}
	    String filePath = targetDir.resolve(String.format("%s-%s.%s", serverInstanceCode, executionNo, this.contextFileExt)).toString();	    
	    return filePath;
	}

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}