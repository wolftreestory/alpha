package com.alpha.base.support.batch.vo;

import java.io.InputStream;

import lombok.Data;

@Data
public class TaskExecutionVo {
	
	private String executionNo;
	private String executionName;
	private String executionConfig;
	
	private String executionType; // inner,outter
	
	private String executionStartTime;
	private String executionStartTimeFormat;	
	
	private String executionEndTime;
	private String executionEndTimeFormat;	
	
	private String executionServer;
	private String executionTime;
	private String executionTimeFlow;
	
	private String executionPostProcess;
	
	private String executionLog;
    private String executionLogSize;
    private String executionLogEmptyYn;
    
	private String exceptionYn;
    private String exceptionLog;
    private String exceptionLogSize;
    private String exceptionLogEmptyYn;
	private String exceptionType;	

	private int metaLogFileCount;
	private String metaLogFileInfo;

	private String errorYn;
	private String interruptYn;
	
	private long logSeq=0L;
	private String logType;
	private String logPath;
	private InputStream logData;
	
	private String logExtractMode;
	private long logExtractSize=0L;	
	private long logChunkStartPosition=0L;
	private long logChunkSize=0L;

	private String sysCreationDate;
	private String sysUpdateDate;

	private String clearBaseDays;
	private String clearBaseRows;
	private String deleteAllYn;
	
	private String selectMode;
	private long rownum=0L;
	
	public TaskExecutionVo() {}
    public TaskExecutionVo(String executionNo) {this.setExecutionNo(executionNo);}
}