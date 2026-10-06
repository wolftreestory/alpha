package com.alpha.base.support.batch;

import java.util.List;

import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;

import com.alpha.base.BaseUtil;
import com.alpha.base.config.batch.BatchConfig;
import com.alpha.base.support.batch.SchedulerManagerPack.SchedulerManager;
import com.alpha.base.support.batch.TaskConfigPack.TaskConfig;
import com.alpha.base.support.batch.TaskLogHandlerPack.TaskLogHandler;
import com.alpha.base.support.batch.context.SchedulerContext;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class TaskLogFilter extends Filter<ILoggingEvent> {

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// devtools 사용시 클래스로딩이 달라서 정상동작 하지 않을 수 있음
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Level level = null;
    private String enableYn = "Y"; //default:Y
    private String metaTaskToken = TaskLogHandler.TASK_TOKEN;
    private String metaTaskLogLevel = null;
    private String schedulerManagerBeanName = BatchConfig.SCHEDULER_MANAGER;
    private SchedulerContext schedulerContext = null;    
    private AntPathMatcher antPathMatcher = null;
    
    public void setLevel(Level level) {this.level=level;}
    public void setEnableYn(String enableYn) {this.enableYn=enableYn;}
    public void setMetaTaskToken(String metaTaskToken) {this.metaTaskToken=metaTaskToken;}
    public void setMetaTaskLogLevel(String metaTaskLogLevel) {
    	this.metaTaskLogLevel = metaTaskLogLevel;
    	if(this.metaTaskLogLevel.equals(TaskLogHandler.EXCEPTION_LOG_LEVEL) && this.level == null) {this.level = Level.ERROR;}
    }
    public void setSchedulerManagerBeanName(String schedulerManagerBeanName) {this.schedulerManagerBeanName=schedulerManagerBeanName;}
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if(this.enableYn!=null && this.enableYn.equals("N")) {return FilterReply.NEUTRAL;}        
        if(this.metaTaskToken!=null) {
	        TaskConfig taskConfig=this.getTaskConfig(MDC.get(this.metaTaskToken));
	        if(taskConfig==null) {return this.isDenyLevel(event.getLevel())?FilterReply.DENY:FilterReply.NEUTRAL;}
	        if(taskConfig.getLogEnableYn()!=null && taskConfig.getLogEnableYn().equals("N")) {return FilterReply.DENY;}
	       
	        if(taskConfig.getLogExceptEnableYn().equals("Y")) {
	            List<String> jobLogExceptList=taskConfig.getLogExceptPatternList();
	            if(jobLogExceptList==null) {return this.isDenyLevel(event.getLevel())?FilterReply.DENY:FilterReply.NEUTRAL;}           
	            if(this.isDenyLogger(event.getLoggerName(),jobLogExceptList)) {return FilterReply.DENY;}
	        }
        }
        return this.isDenyLevel(event.getLevel())?FilterReply.DENY:FilterReply.NEUTRAL;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isDenyLogger(String logger, List<String> pattrenList) {
        if(pattrenList==null) {return false;}
        if(this.antPathMatcher==null) {this.antPathMatcher=new AntPathMatcher();}        
        for(int i=0;i<pattrenList.size();i++) {
            if(pattrenList.get(i)!=null && !pattrenList.get(i).equals("")) {
                if(this.antPathMatcher.match(pattrenList.get(i),logger)) {return true;}
            }
        }
        return false;
    }
    private boolean isDenyLevel(Level eventLevel) {
    	String levelValue=this.metaTaskLogLevel==null?null:MDC.get(this.metaTaskLogLevel);
    	if(levelValue!=null && !levelValue.equals("")) {return eventLevel.isGreaterOrEqual(Level.toLevel(levelValue))?false:true;}    	
    	if(this.level==null) {return false;}
    	return eventLevel.isGreaterOrEqual(this.level)?false:true;    	
    }
        
    private TaskConfig getTaskConfig(String executionToken) {
        if(executionToken==null || executionToken.equals("")) {return null;}
        if(executionToken.indexOf(".")==-1) {return null;}

        String taskId=executionToken.split("[.]")[1];
        if(taskId==null || taskId.equals("")) {return null;}

        SchedulerContext schedulerContext=this.getSchedulerContext();
        if(schedulerContext==null) {return null;}

        TaskPack.Task task=schedulerContext.getTaskByName(taskId);
        if(task==null) {return null;}

        return task.getTaskConfig();
    }
    
    private SchedulerContext getSchedulerContext() {
        if(this.schedulerContext!=null) {return this.schedulerContext;}
        if(this.schedulerManagerBeanName==null || this.schedulerManagerBeanName.equals("")) {return null;}
        
        SchedulerManager schedulerManager=null;
        try {schedulerManager=BaseUtil.getBean(this.schedulerManagerBeanName,SchedulerManager.class);}
        catch(Exception e) {e.printStackTrace();}
        if(schedulerManager==null) {return null;}

        this.schedulerContext=schedulerManager.getSchedulerContext();
        if(this.schedulerContext==null) {return null;}

        return this.schedulerContext;
    }
   
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}