package com.alpha.base.support.batch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.StringUtils;

import com.alpha.base.exception.SystemException;
import com.alpha.base.support.batch.TaskConfigPack.TaskConfig;
import com.alpha.base.support.batch.TaskMetaLoaderPack.TaskMetaLoader;
import com.alpha.base.support.batch.TaskPack.Domain;
import com.alpha.base.support.batch.TaskPack.ExecutionType;
import com.alpha.base.support.batch.TaskPack.Status;
import com.alpha.base.support.batch.TaskPack.TriggerType;
import com.alpha.base.support.batch.context.SchedulerContext;
import com.alpha.base.support.util.ObjectMapperUtilPack.ObjectMapperUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public final class SchedulerManagerPack {
		
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static interface SchedulerManager extends ApplicationListener<ContextClosedEvent> {

		public static final String EXECUTE_SYNC = "sync";
		public static final String EXECUTE_ASYNC = "async";
		public static final String SCHEDULER_STATUS_START =  "start";
		public static final String SCHEDULER_STATUS_STOP =  "stop";	
		public static final String SCHEDULER_STATUS_WAIT =  "wait";	
	    public static final String SCHEDULER_AUTO_START_Y = "Y";
	    public static final String SCHEDULER_AUTO_START_N = "N";
		public static final String SCHEDULER_AUTO_START_DELAY = "0";
		public static final String SCHEDULER_STOP_CHECK_TERM = "100";

		public TaskExecutor getExecutor();
		public void setExecutor(ThreadPoolTaskExecutor executor);
		
		public ThreadPoolTaskScheduler getScheduler();
		public void setScheduler(ThreadPoolTaskScheduler scheduler);

		public String getSchedulerRequestPath();
		public void setSchedulerRequestPath(String schedulerRequestPath);
		
		public String getSchedulerAutoStartYn();
		public void setSchedulerAutoStartYn(String schedulerAutoStartYn);
		
		public String getSchedulerAutoStartDelay();
		public void setSchedulerAutoStartDelay(String schedulerAutoStartDelay);
		public void doSchedulerAutoStart();
		
		public String getSchedulerStopCheckTerm();
		public void setSchedulerStopCheckTerm(String schedulerStopCheckTerm);
		
		public SchedulerContext getSchedulerContext();
		public void setSchedulerContext(SchedulerContext schedulerContext);

		public int getSchedulerTaskCount();
		
		public TaskConfig getDefaultTaskConfig();
		public void setDefaultTaskConfig(TaskConfig defaultTaskConfig);

		public TaskMetaLoader getTaskMetaLoader();
		public void setTaskMetaLoader(TaskMetaLoader metaTaskManager);
				
		public void asyncStartScheduler();
		public void asyncStopScheduler();
		
		public void startScheduler();
		public void stopScheduler();
		public void setEnable(String taskId,String enableYn);

		public void doExecute(String taskId);
		public void doInterrupt(String taskId);
		
		public void addTask(TaskPack.Task task);
		
		public void setTaskAspect(Object object);
		public Object getTaskAspect();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static SchedulerManager getSchedulerManager(ThreadPoolTaskExecutor _executor, ThreadPoolTaskScheduler _scheduler) {
		
		return new SchedulerManager() {

		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			private ThreadPoolTaskExecutor executor = _executor;
			private ThreadPoolTaskScheduler scheduler = _scheduler;
			
			private String schedulerRequestPath;
			private String schedulerAutoStartYn = SchedulerManager.SCHEDULER_AUTO_START_Y;
			private String schedulerAutoStartDelay = SchedulerManager.SCHEDULER_AUTO_START_DELAY;
			private String schedulerStopCheckTerm = SchedulerManager.SCHEDULER_STOP_CHECK_TERM;

			private TaskConfig defaultTaskConfig;
			private Object taskAspect;

			private SchedulerContext schedulerContext;
			private TaskMetaLoader taskMetaLoader;
			
			private List<Map<String,String>> groupList;
			
		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			
			@Override 
			public TaskExecutor getExecutor() {return executor;}

			@Override 
			public void setExecutor(ThreadPoolTaskExecutor executor) {this.executor = executor;}

			@Override 
			public ThreadPoolTaskScheduler getScheduler() {return this.scheduler;}

			@Override 
			public void setScheduler(ThreadPoolTaskScheduler scheduler) {this.scheduler = scheduler;}

			@Override 
			public String getSchedulerRequestPath() {return this.schedulerRequestPath;}

			@Override 
			public void setSchedulerRequestPath(String schedulerRequestPath) {this.schedulerRequestPath = schedulerRequestPath;}

			@Override 
			public String getSchedulerAutoStartYn() {return this.schedulerAutoStartYn;}

			@Override 
			public void setSchedulerAutoStartYn(String schedulerAutoStartYn) {this.schedulerAutoStartYn = schedulerAutoStartYn;}

			@Override 
			public String getSchedulerAutoStartDelay() {return this.schedulerAutoStartDelay;}

			@Override 
			public void setSchedulerAutoStartDelay(String schedulerAutoStartDelay) {this.schedulerAutoStartDelay = schedulerAutoStartDelay;}

			@Override 
			public String getSchedulerStopCheckTerm() {return this.schedulerStopCheckTerm;}

			@Override 
			public void setSchedulerStopCheckTerm(String schedulerStopCheckTerm) {this.schedulerStopCheckTerm = schedulerStopCheckTerm;}

			@Override 
			public TaskConfig getDefaultTaskConfig() {return this.defaultTaskConfig;}

			@Override 
			public void setDefaultTaskConfig(TaskConfig defaultTaskConfig) {this.defaultTaskConfig = defaultTaskConfig;}

			@Override 
			public void setTaskAspect(Object object) {this.taskAspect=object;}

			@Override 
			public Object getTaskAspect() {return this.taskAspect;}

			@Override 
			public SchedulerContext getSchedulerContext() {return this.schedulerContext;}

			@Override 
			public TaskMetaLoader getTaskMetaLoader() {return this.taskMetaLoader;}

			@Override 
			public void setTaskMetaLoader(TaskMetaLoader taskMetaLoader) {this.taskMetaLoader = taskMetaLoader;}
			
			@Override 
			public synchronized void addTask(TaskPack.Task task) {
				if(this.schedulerContext == null) {return;}

			    String currentDate = (new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date(System.currentTimeMillis()))).toString();

			    List<TaskPack.Task> taskList = this.schedulerContext.getTaskList();

				// [step1] 타스크설정이 없을 경우 디폴트타스크컨피그 적용
				if(task.getTaskConfig()==null) {				
					task.setTaskConfig(TaskConfigPack.getTaskConfig(this.getDefaultTaskConfig()));
				}
				
				// [step2] 타스크추가정보 구성(그룹관련정보)
				task.setGroupCode(this.extractGroupInfo(task,"groupCode"));
				task.setGroupLabel(this.extractGroupInfo(task,"groupLabel"));
				
			    // [step3] 빌드업 taskConfig
			    task.buildTaskConfig();

				// [step4] taskSeq, taskId, taskType, createDate, updateDate
			    task.setTaskSeq(taskList==null?1:taskList.size()+1);			    
				task.setTaskId("task-"+task.getTaskSeq());
				task.setDomain(task.getDomain()==null?TaskPack.Domain.inner:task.getDomain());
				task.setName(task.getName().trim());
			    task.setCreateDate(currentDate);
			    task.setUpdateDate(currentDate);
				task.setUpdateProcess(null);
				task.setDeleteProcess(null);
				
			    // [step5] 중복체크
			    task.setNameDupYn("N");
			    if(taskList!=null) {
			    	String checkName = task.getName();
			    	if(taskList.stream().anyMatch(t -> t.getName().equals(checkName))) {task.setNameDupYn("Y");}
			    }
			    
			    // [step6] 이름중복이 없으면 taskId 수정
			    if(task.getNameDupYn().equals("N")) {
					task.setTaskId("task-"+task.getName());
			    }
			    
			    // [step7] 실행가능 서버체크
			    task.setRunnableServerYn(task.getTaskConfig().isRunnableServer()?"Y":"N");
			    			    
			    // [step8] 타스크 상태				    
		        boolean isEnable=true;
	            if(isEnable && task.getNameDupYn().equals("Y")) {isEnable=false;}
	            if(isEnable && task.getRunnableServerYn().equals("N")) {isEnable=false;}            
		        if(isEnable) {
		        	task.setStatus(Status.ready);	            
		        } else {
		        	task.setStatus(Status.block);
		        }

		        // [step9] addAspect
				if(this.getTaskAspect()!=null) {
			        AspectJProxyFactory factory = new AspectJProxyFactory(task);				        
			        factory.addAspect(this.getTaskAspect());
			        task = factory.getProxy();
				}
				
				this.schedulerContext.addTask(task);
			}
			
			@Override
			public void setSchedulerContext(SchedulerContext schedulerContext) {
				this.schedulerContext = schedulerContext;
				this.schedulerContext.setSchedulerStopTime(this.getNowTime());
				this.schedulerContext.setSchedulerStatus(SchedulerManager.SCHEDULER_STATUS_STOP);
			}

			@Override
			public int getSchedulerTaskCount() {
				int schedulerTaskCount = 0;

				List<TaskPack.Task> taskList = this.schedulerContext.getTaskList();
				if(taskList == null || taskList.size() == 0) {return schedulerTaskCount;}		
				
				for(int i = 0;i<taskList.size();i++) {
					TaskPack.Task task = taskList.get(i);
					if(this.isEffectiveTask(task)) {
						if(task.getTriggerType() == null) {log.debug(">> Trigger not found");}
						else if(task.getTriggerType().equals(TriggerType.cron)) {schedulerTaskCount++;}
						else if(task.getTriggerType().equals(TriggerType.fixedRate)) {schedulerTaskCount++;}
						else if(task.getTriggerType().equals(TriggerType.fixedDelay)) {schedulerTaskCount++;}
					}
				}

				return schedulerTaskCount;
			}

			@Override
			public void doSchedulerAutoStart() {
				if(this.getSchedulerAutoStartYn().equals("N") || this.schedulerContext == null) {return;}				
				this.executor.execute(new SchedulerRunnable(this,SchedulerManager.SCHEDULER_STATUS_START,this.getSchedulerAutoStartDelay()));
				long count = 0, max = 10, sleepMs = 2000;
		    	while(!this.schedulerContext.getSchedulerStatus().equals(SchedulerManager.SCHEDULER_STATUS_START) && count < max) {
					try {Thread.sleep(sleepMs);} catch (InterruptedException e) {log.debug(e.toString());}
		    		count++;
		    	}
			}	
			
			@Override
			public void asyncStartScheduler() {
				this.executor.execute(new SchedulerRunnable(this,SchedulerManager.SCHEDULER_STATUS_START));
			}

			@Override
			public void asyncStopScheduler() {
				this.executor.execute(new SchedulerRunnable(this,SchedulerManager.SCHEDULER_STATUS_STOP));
			}

		    @Override
		    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
		        log.debug(">> contextClosedEvent: {}",contextClosedEvent.getTimestamp());
		        this.scheduler.setWaitForTasksToCompleteOnShutdown(true);
		        this.scheduler.shutdown();
		        log.debug(">> scheduler shutdown");

		        this.executor.setWaitForTasksToCompleteOnShutdown(true);
		        this.executor.shutdown();
		        log.debug(">> executor shutdown");
		    }   
		    
			@Override
			public void startScheduler() {
				if(this.scheduler == null || this.schedulerContext == null) {return;}
				if(this.getSchedulerTaskCount() == 0) {return;}
				if(this.schedulerContext.getSchedulerStatus().equals(SchedulerManager.SCHEDULER_STATUS_START)) {return;}
												
				this.scheduler.initialize();
		        this.scheduler.setWaitForTasksToCompleteOnShutdown(true); //현재 실행 중인 작업을 중단하지 않고 완료될 때까지 기다립
		        
				this.schedulerContext.setSchedulerStartTime("");
				this.schedulerContext.setSchedulerStopTime("");
		        
		        this.schedulerContext.setSchedulerStatus(SchedulerManager.SCHEDULER_STATUS_WAIT);		        
				List<TaskPack.Task> taskList = this.schedulerContext.getTaskList();				
				for(int i = 0;i<taskList.size();i++) {
					TaskPack.Task task = taskList.get(i);
		            task.resetCount();
					if(this.isEffectiveTask(task,true)) {
						if(task.getTriggerType() == null) {log.debug("Trigger not found");}
						else if(task.getTriggerType().equals(TriggerType.cron)) {this.doSchedulerCronTrigger(task);}
						else if(task.getTriggerType().equals(TriggerType.fixedRate)) {this.doSchedulerAtFixedRate(task);}
						else if(task.getTriggerType().equals(TriggerType.fixedDelay)) {this.doSchedulerWithFixedDelay(task);}
					}
				}				
		        log.info(">> scheduler.poolSize: {}",this.scheduler.getPoolSize());
		        log.info(">> scheduler.threadNamePrefix: {}",this.scheduler.getThreadNamePrefix());

				this.schedulerContext.setSchedulerStartTime(this.getNowTime());
				this.schedulerContext.setSchedulerStatus(SchedulerManager.SCHEDULER_STATUS_START);
				log.info(">> scheduler.status: {}",this.schedulerContext.getSchedulerStatus());
			}

			@Override
			public void stopScheduler() {
				if(this.scheduler == null || this.schedulerContext == null) {return;}
				if(this.getSchedulerTaskCount() == 0) {return;}
				if(this.schedulerContext.getSchedulerStatus().equals(SchedulerManager.SCHEDULER_STATUS_STOP)) {return;}

		        log.info(">> scheduler.poolSize: {}",this.scheduler.getPoolSize());
		        log.info(">> scheduler.activeCount: {}",this.scheduler.getActiveCount());
		        
				int runningTaskCount = this.schedulerContext.getRunningTaskCount();
				log.info(">> scheduler.stopCheck.runningTaskCount: {}",runningTaskCount);
				
				this.schedulerContext.setSchedulerStatus(SchedulerManager.SCHEDULER_STATUS_WAIT);				
				if(runningTaskCount>0) {
					try {Thread.sleep(Long.valueOf(this.getSchedulerStopCheckTerm()));}
					catch (InterruptedException e) {log.debug(e.toString());}
					this.stopScheduler();
				} else {
					this.scheduler.setWaitForTasksToCompleteOnShutdown(false);
					this.scheduler.shutdown();
					
					this.schedulerContext.setSchedulerStopTime(this.getNowTime());
					this.schedulerContext.setSchedulerStatus(SchedulerManager.SCHEDULER_STATUS_STOP);
					log.info(">> scheduler.status: {}",this.schedulerContext.getSchedulerStatus());
				}
			}
			
			@Override
			public void setEnable(String taskId,String enableYn) {
				if(this.scheduler == null || this.schedulerContext == null) {return;}
						
				if(taskId == null || taskId.equals("")) {return;}
				if(enableYn == null || enableYn.equals("")) {enableYn = "N";}		

				List<TaskPack.Task> taskList = this.schedulerContext.getTaskList();
				if(taskList == null) {return;}

				TaskPack.Task targetTask = null;
				for(int i = 0;i<taskList.size();i++) {
					if(taskList.get(i).getRunnableServerYn().equals("Y") && taskList.get(i).getTaskId().equals(taskId)) {targetTask = taskList.get(i);break;}
				}
				if(targetTask == null) {return;}

				String schedulerStatus = this.schedulerContext.getSchedulerStatus();
		        if(schedulerStatus == null) {return;}

		        if(schedulerStatus.equals(SchedulerManager.SCHEDULER_STATUS_STOP)) {
		            targetTask.setEnableYn(enableYn);
		            return;
		        }

				if(schedulerStatus.equals(SchedulerManager.SCHEDULER_STATUS_START)) {
		            targetTask.setEnableYn(enableYn);                            
			        if(enableYn.equals("N")) {
			            if(targetTask.getScheduledFuture()!=null) {targetTask.getScheduledFuture().cancel(true);}
			            log.debug(">> scheduler poolSize: {}",this.scheduler.getPoolSize());
			        }
		            if(enableYn.equals("Y")) {
		                if(targetTask.getTriggerType() == null) {log.debug("Trigger not found");}
		                else if(targetTask.getTriggerType().equals(TriggerType.cron)) {this.doSchedulerCronTrigger(targetTask);}
		                else if(targetTask.getTriggerType().equals(TriggerType.fixedRate)) {this.doSchedulerAtFixedRate(targetTask);}
		                else if(targetTask.getTriggerType().equals(TriggerType.fixedDelay)) {this.doSchedulerWithFixedDelay(targetTask);} 
		                log.debug(">> scheduler poolSize: {}",this.scheduler.getPoolSize());
		            }
				}
			}

			@Override
			public void doExecute(String taskId) {
		        if(taskId == null || taskId.equals("")) {return;}
				if(this.scheduler == null || this.schedulerContext == null) {return;}		
				
				List<TaskPack.Task> taskList = this.schedulerContext.getTaskList();
				if(taskList == null) {return;}
				
				TaskPack.Task targetTask = this.schedulerContext.getTaskList().stream()
						.filter(i->i.getRunnableServerYn().equals("Y"))
						.filter(i->i.getTaskId().equals(taskId))
						.findFirst()
						.orElse(null);
				if(targetTask == null) {return;}
				
				targetTask.setStatus(Status.running);

		        Future<?> future = this.executor.submit(new TaskRunnable(targetTask,ExecutionType.command));
		        targetTask.setExecutedFuture(future);

		        log.debug(">> executor corePoolSize: {},poolSize: {}",this.executor.getCorePoolSize(),this.executor.getPoolSize());
		        log.debug(">> executor activeCount: {}",this.executor.getActiveCount());
			}

			@Override
			public void doInterrupt(String taskId) {
		        if(taskId == null || taskId.equals("")) {return;}
				if(this.scheduler == null || this.schedulerContext == null) {return;}
				
				TaskPack.Task targetTask = this.schedulerContext.getTaskList().stream()
						.filter(i->i.getRunnableServerYn().equals("Y"))
						.filter(i->i.getTaskId().equals(taskId))
						.findFirst()
						.orElse(null);
				if(targetTask == null) {return;}
				
				boolean isInterrupted = false;
				if(targetTask.getScheduledFuture() != null) {isInterrupted = true;targetTask.getScheduledFuture().cancel(true);}
				if(targetTask.getExecutedFuture() != null) {isInterrupted = true;targetTask.getExecutedFuture().cancel(true);}
				if(targetTask.getDomain() == Domain.outter) {
					String executeNo = targetTask.getAgentRequest().getExecuteNo();
					if(StringUtils.hasText(executeNo)) {targetTask.getTaskMetaAgent().stop(Long.parseLong(executeNo));}
				}
				if(isInterrupted) {targetTask.setEnableYn("N");}
			}

		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			
			private String extractGroupInfo(TaskPack.Task task,String key) {
				if(this.groupList==null) {
					ObjectMapperUtil mapper = task.getTaskConfig().getBatchUtil().getObjectMapperUtil();					
					this.groupList = Optional.ofNullable(task.getTaskConfig().getBatchUtil().getBatchProperties().getWebConsole().getGroupList())
							.orElse(Collections.emptyList())
							.stream()
							.filter(item -> item != null && !item.trim().isEmpty())
		                    .map(item -> {
		                        try {return mapper.readValue(item, new TypeReference<Map<String, String>>() {});} 
		                        catch (Exception e) {throw new SystemException(e.getMessage());}
		                    })
		                    .collect(Collectors.toList());
				}

				if(this.groupList.isEmpty()) {return null;}
				if(!StringUtils.hasText(task.getGroup())) {return null;}
				
		        String value = this.groupList.stream()
		                .filter(m -> task.getGroup().equals(m.get("group")))
		                .map(m -> m.get(key)).findFirst().orElse("none");
		        
				return value;
			}

			private boolean isEffectiveTask(TaskPack.Task task) {
				return this.isEffectiveTask(task,false);
			}
			
		    private boolean isEffectiveTask(TaskPack.Task task,boolean isLog) {
		        boolean isEnable = true;
		        if(isEnable && task == null) {isEnable = false;}
		        if(isEnable && task.isBlock()) {isEnable = false;}
		        if(isEnable && !task.getTaskConfig().isRunnableServer()) {isEnable = false;}
		        if(isEnable && !task.getEnableYn().equals("Y")) {isEnable = false;}      
		        if(isLog) {
		        	log.info(">> task: {}, isExecutable: {}",task.getName(),isEnable);
		        }
		        return isEnable;
		    }

			private void doSchedulerCronTrigger(TaskPack.Task task) {
				TaskRunnable taskRunnable = new TaskRunnable(task,this);
				CronTrigger trigger = new CronTrigger(task.getCronExpression());
				ScheduledFuture<?> future = this.scheduler.schedule(taskRunnable,trigger); 
				task.setScheduledFuture(future);
			}
			
			private void doSchedulerAtFixedRate(TaskPack.Task task) {
				TaskRunnable taskRunnable = new TaskRunnable(task,this);
				long period = this.convertToMilliseconds(task.getFixedRate());				
				Date startTime = new Date(System.currentTimeMillis()+period);
				ScheduledFuture<?> future = this.scheduler.scheduleAtFixedRate(taskRunnable,startTime,period);
		        task.setScheduledFuture(future);
			}

			private void doSchedulerWithFixedDelay(TaskPack.Task task) {
				TaskRunnable taskRunnable = new TaskRunnable(task,this);
				long delay = this.convertToMilliseconds(task.getFixedDelay());
				Date startTime = new Date(System.currentTimeMillis()+delay);		
				ScheduledFuture<?> future = this.scheduler.scheduleWithFixedDelay(taskRunnable,startTime,delay);
		        task.setScheduledFuture(future);
			}
			
		    private long convertToMilliseconds(String value) {
		        if (value.endsWith("s")) {
		            return Long.parseLong(value.substring(0, value.length() - 1)) * 1000;
		        } else if (value.endsWith("m")) {
		            return Long.parseLong(value.substring(0, value.length() - 1)) * 60 * 1000;
		        } else if (value.endsWith("h")) {
		            return Long.parseLong(value.substring(0, value.length() - 1)) * 60 * 60 * 1000;
		        } else {
		        	return Long.parseLong(value);
		        }
		    }
	        
			private String getNowTime() {
		        Calendar nowTime = Calendar.getInstance();
		        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		        return sd.format(nowTime.getTime());        
		    }	
			
		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			   
		    class TaskRunnable implements Runnable {
		    	private TaskPack.Task task;
		    	private SchedulerManager schedulerManager;
		    	private ExecutionType executionType;
		    		    	
		    	public TaskRunnable(TaskPack.Task task,SchedulerManager schedulerManager) {this.setPrivate(task,schedulerManager,ExecutionType.trigger);}		    	
		        public TaskRunnable(TaskPack.Task task,ExecutionType executeType) {this.setPrivate(task,null,executeType);}		        
		        private void setPrivate(TaskPack.Task task,SchedulerManager schedulerManager,ExecutionType executeType) {
		            this.task = task;
		            this.schedulerManager = schedulerManager;
		            this.executionType = executeType;
		            this.task.setTaskRunnable(this);
		        }
		        
				@Override
				public void run() {
				    if(this.executionType == null) {return;}
				    if(this.schedulerManager !=null && this.schedulerManager.getSchedulerContext().getSchedulerStatus().equals(SchedulerManager.SCHEDULER_STATUS_WAIT)) {return;}
			        if(this.executionType.equals(ExecutionType.command)) {this.task.executeCommand(this.task);return;}			        
			        if(this.executionType.equals(ExecutionType.trigger)) {
			        	if(this.task.getTaskConfig().getScheduleTermEnableYn().equals("Y") && !this.task.getTaskConfig().isScheduleTermValid()) {return;}
			        	this.task.executeTrigger(this.task);
			        	return;
			        }
				}
		    }	
		    
		    class SchedulerRunnable implements Runnable {
		    	private SchedulerManager schedulerManager;
		    	private String mode;
		    	private String delay;
		    	
		    	public SchedulerRunnable(SchedulerManager schedulerManager,String mode) {this.setPrivate(schedulerManager,mode,null);}
		    	public SchedulerRunnable(SchedulerManager schedulerManager,String mode,String delay) {this.setPrivate(schedulerManager,mode,delay);}
		    	private void setPrivate(SchedulerManager schedulerManager,String mode,String delay) {
		    		this.schedulerManager = schedulerManager;
		    		this.mode = mode;
		    		this.delay = delay;
		    	}
		    	
				@Override
				public void run() {
					if(this.mode == null || this.mode.equals("")) {return;}			
					this.doDelay();
					if(this.mode.equals(SchedulerManager.SCHEDULER_STATUS_START)) {schedulerManager.startScheduler();}
					if(this.mode.equals(SchedulerManager.SCHEDULER_STATUS_STOP)) {schedulerManager.stopScheduler();}
					return;
				}

			    private void doDelay() {
					if(this.delay != null && !this.delay.equals("")) {
						try {Thread.sleep(Long.parseLong(this.delay));} 
						catch (InterruptedException e) {log.debug(e.toString());}
					}
					
					long count = 0, max = 10, sleepMs = 1000;
			    	while(schedulerManager.getSchedulerTaskCount()==0 && count < max) {
			    		log.debug(">> schedulerTaskCount():{}",schedulerManager.getSchedulerTaskCount());
						try {Thread.sleep(sleepMs);} catch (InterruptedException e) {log.debug(e.toString());}
			    		count++;
			    	}
			    }
			}

		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		};		
		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}