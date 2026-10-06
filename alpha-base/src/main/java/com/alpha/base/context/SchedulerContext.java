package com.alpha.base.context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnExpression("T(com.alpha.base.SchedulerConfig).isEnabled()")
@ConditionalOnProperty(name="alpha.scheduler.enabled", havingValue="true", matchIfMissing=false)
public class SchedulerContext {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public enum SchedulerStatus {load,start,stop};
	
	public enum TaskStatus {load,ready,running,completed,block,cancel};
	
	public enum TriggerType {cron,fixedRate,fixedDelay,fixedTime};
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Data
	public static class Task implements Serializable {
		private static final long serialVersionUID = 1L;

		@Schema(description="스케쥴 id : 스케쥴 등록시 자동구성되는 id")
		private String taskId;
		
		@Schema(description="이름")
		private String name;
		
		@Schema(description="설명")
		private String description;
		
		@Schema(description="타스크 상태")
		private TaskStatus status;
		
		@Schema(description="타스크 스케쥴 객체")
		private ScheduledFuture<?> scheduledFuture;

		@Schema(description="타스크 수행한 runnable객체")
		private Runnable runnableWrapper;	
		
		@Schema(description="트리거 타입")
		private TriggerType triggerType;

		@Schema(description="트리거 시작일(yyyyMMddHHmmss)")
		private String triggerStartDate;

		@Schema(description="트리거 종료일(yyyyMMddHHmmss)")
		private String triggerEndDate;
		
		@Schema(description="트리거 메모")
		private String triggerMemo;

		@Schema(description="cron표현식 : triggerType이 cron인 경우 cron표현식")
		private String cronExpression;
		
		@Schema(description="수행후 지정한 시간마다 실행 : triggerType이 fixedRate인 단위:ms")
		private String fixedRate;
		
		@Schema(description="수행후 지정한 시간이후 실행 : triggerType이 fixedDelay인 단위:ms")
		private String fixedDelay;
		
		@Schema(description="지정한 시간에 수행 : triggerType이 fixedTime인 단위:ms")
		private String fixedTime;	
		
		@Schema(description="프로세스 실행 횟수")
		private int executeCount=0;
		
		@Schema(description="프로세스 실행요청정보 객체")
		private Object executeRequest;
		
		@Schema(description="프로세스 실행응답정보 객체목록")
		private List<Object> executeResponseList;		
	}
		
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ThreadPoolTaskScheduler scheduler;
	
	private SchedulerStatus schedulerStatus;

	private String schedulerStartTime;
	
	private String schedulerStopTime;
	
	private List<Task> taskList;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public ThreadPoolTaskScheduler buildScheduler(int poolSize,String threadNamePrefix) {
		this.scheduler = new ThreadPoolTaskScheduler();
		this.scheduler.setPoolSize(poolSize);
		this.scheduler.setThreadNamePrefix(threadNamePrefix);
		this.scheduler.setWaitForTasksToCompleteOnShutdown(true);
		this.scheduler.initialize();
		return this.getScheduler();
	}
	
	public ThreadPoolTaskScheduler getScheduler() {
		return this.scheduler;
	}

	public void setScheduler(ThreadPoolTaskScheduler scheduler) {
		this.scheduler = scheduler;
	}

	public SchedulerStatus getSchedulerStatus() {
		return this.schedulerStatus;
	}

	public void setSchedulerStatus(SchedulerStatus schedulerStatus) {
		this.schedulerStatus = schedulerStatus;
	}

	public String getSchedulerStartTime() {
		return this.schedulerStartTime;
	}

	public void setSchedulerStartTime(String schedulerStartTime) {
		this.schedulerStartTime = schedulerStartTime;
	}

	public String getSchedulerStopTime() {
		return this.schedulerStopTime;
	}

	public void setSchedulerStopTime(String schedulerStopTime) {
		this.schedulerStopTime = schedulerStopTime;
	}

	public List<Task> getTaskList() {
		return this.taskList;
	}

	public void setTaskList(List<Task> taskList) {
		this.taskList = taskList;
	}

	public Task getTask(String taskId) {
		if(taskId==null || taskId.equals("")) {return null;}
		if(this.taskList==null || this.taskList.size()==0) {return null;}
		
		Task targetTask=null;
		for(Task task : this.taskList) {
			if(task.getTaskId().equals(taskId)) {targetTask=task;break;}
		}
		return targetTask;
	}
	
    public int getTaskCount(TaskStatus status) {
    	if(this.taskList==null) {return 0;}
        int count=0;
        for(Task task : this.taskList) {
        	if(task.getStatus().equals(status)) {count++;}
        }  
    	return count;
    }

    public void optimizeTaskList() {
    	if(this.taskList==null || this.taskList.isEmpty()) {return;}
    	
    	List<Task> newlist = new ArrayList<>();
    	
    	log.debug(">> before-optimize:{}",this.getTaskList().size());    	
    	for(Task task : this.taskList) {
    		 boolean isRemove=false;
    		 if(!isRemove && task.getScheduledFuture()==null) {isRemove=true;}
    		 if(!isRemove && task.getScheduledFuture().isCancelled()) {isRemove=true;}
    		 if(!isRemove && task.getScheduledFuture().isDone()) {isRemove=true;}
    		 if(!isRemove && task.getStatus().equals(TaskStatus.cancel)) {isRemove=true;}
    		 if(isRemove) {
    			 log.debug(">> gabage:{}",task.toString());
    			 continue;
    		 }
    		 newlist.add(task);
    	 }
    	 this.setTaskList(newlist);
    	 log.debug(">> afteroptimize:{}",this.getTaskList().size());    	 
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
