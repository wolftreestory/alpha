package com.alpha.scheduler.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.alpha.base.BaseUtil;
import com.alpha.base.config.SchedulerConfig.SchedulerProperties;
import com.alpha.base.context.SchedulerContext;
import com.alpha.base.context.SchedulerContext.SchedulerStatus;
import com.alpha.base.context.SchedulerContext.Task;
import com.alpha.base.context.SchedulerContext.TaskStatus;
import com.alpha.base.context.SchedulerContext.TriggerType;
import com.alpha.base.exception.SystemException;
import com.alpha.base.support.util.TimeUtilPack;
import com.alpha.base.support.util.TimeUtilPack.TimeUtil;
import com.alpha.scheduler.service.AgentService;
import com.alpha.scheduler.service.SchedulerService;
import com.alpha.scheduler.vo.ExecuteInfoPack.ExecuteRequest;
import com.alpha.scheduler.vo.ExecuteInfoPack.ExecuteResponse;
import com.alpha.scheduler.vo.ScedulerInfoPack.AgentWork;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SchedulerServiceImpl implements SchedulerService {
		
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Value("${spring.profiles.active:local}")
	private String springProfilesActive;

	@Autowired
	private SchedulerContext context;

	@Autowired
	private SchedulerProperties props;

	private TimeUtil timeUtil;

	private ObjectMapper mapper;
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@PostConstruct
	public void postConstruct(){
		this.timeUtil = TimeUtilPack.getTimeUtil();
		
		this.mapper = new ObjectMapper();
		this.mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);	
		this.mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);

		try {
			this.initSchedulerContext();
			log.info(">> scheduler.task.load:{}",this.context.getTaskCount(TaskStatus.load));
			
			this.startScheduler();
			log.info(">> scheduler.task.ready:{}",this.context.getTaskCount(TaskStatus.ready));
			
		}catch(Exception e) {
			log.error(">> {}",e.getMessage());
		}	
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
	@Scheduled(cron="${alpha.scheduler.optimize.cron}")
	private void optimize() {
    	if(!this.props.getOptimize().isEnabled()) {return;}
        this.context.optimizeTaskList();
        //this.build();
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public int setAgentWorkList(List<AgentWork> list) {
		return this.storeAgentWorkList(list);
	}

	@Override
	public int build(){
		this.stopScheduler();
		this.initSchedulerContext();
		this.startScheduler();
		return this.context.getTaskCount(TaskStatus.ready);
	}
	
	@Override
	public int start() {
		this.startScheduler();
		return this.context.getTaskCount(TaskStatus.ready);
	}
	
	@Override
	public int stop() {
		this.stopScheduler();
		return this.context.getTaskCount(TaskStatus.block);
	}
	
	@Override
	public SchedulerStatus status() {
		return this.context.getSchedulerStatus();
	}
		
	@Override
	public List<Task> getTaskList() {
		return this.context.getTaskList();
	}

	@Override
	public Task getTask(String taskId) {
		return this.context.getTask(taskId);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int storeAgentWorkList(List<AgentWork> worklist) {
		if(worklist==null || worklist.isEmpty()) {return 0;}
		
		String worknoteDirectory = this.props.getWorknoteDirectory();

		File dir = new File(this.props.getWorknoteDirectory());
		if(!dir.exists()) {dir.mkdirs();}
		
		int workCount=0;
		
		String workFile = worknoteDirectory + "/"+this.timeUtil.getCurrentTime()+".work";
		log.debug(">> workFile:{}",workFile);	
		
		try(FileWriter fileWriter = new FileWriter(FilenameUtils.normalize(workFile))){
			for(AgentWork work : worklist) {
				fileWriter.write(BaseUtil.getJsonUtil().toJson(work));
				fileWriter.write("\n");
				workCount++;
			}			
		} catch (IOException e) {
			throw new SystemException(e.getMessage());
		}		
		log.debug(">> workCount:{}",workCount);
		
		// 3일지난 파일 삭제(현재 선택된 파일 제외)
		Date current = new Date(System.currentTimeMillis());
		long oneDayMillis = (24*60*60*1000);		
		File[] fileList = dir.listFiles();
		for(int i=0 ; i<fileList.length; i++){
			if(workFile.contains(fileList[i].getName())) {continue;}
			Date fileDate = new Date(fileList[i].lastModified());
			long gap = (current.getTime() - fileDate.getTime())/oneDayMillis;
			if(gap>3) {fileList[i].delete();}
		}
		
		return workCount;
	}
	
	private void initSchedulerContext(){
		if(this.context==null) {throw new SystemException("schedulerContext is null");}
		
		this.context.setSchedulerStatus(SchedulerStatus.load);

		// step1: 최신 work 파일
		File targetFile = null ;	
		File path = new File(this.props.getWorknoteDirectory());
		File[] files = path.listFiles(File::isFile);
		long lastModifiedTime = Long.MIN_VALUE;
		if(files!=null && files.length > 0) {
			for(File file : files) {
				if (file.lastModified() > lastModifiedTime) {
					targetFile = file;
					lastModifiedTime = file.lastModified();
				}				
			}
		}
		if(targetFile == null) {
			throw new SystemException("스케쥴링 작업파일이 없습니다.");
		}

		// step2: work 파일 읽음
		List<String> jsonList = new ArrayList<>();
		try(BufferedReader bufferedReader = new BufferedReader(new FileReader(targetFile))){
			String line = null;
			while((line = bufferedReader.readLine()) != null){jsonList.add(line);}
		}catch(Exception e) {
			throw new SystemException(e.getMessage());
		}
		if(jsonList.size() == 0 ) {
			throw new SystemException("스케쥴링 작업파일에 구성할 내용이 없습니다.");
		}

		// step3: json -> work 변환						
		List<AgentWork> agentWorkList = new ArrayList<>();	
		jsonList.forEach(json->{
			try {
				agentWorkList.add(mapper.readValue(json, AgentWork.class));
			} catch(Exception e){
				throw new SystemException(e.getMessage());
			}	
		});
		
		agentWorkList.sort(new Comparator<AgentWork>() {
			@Override
			public int compare(AgentWork o1, AgentWork o2) {
				String value1 = o1.getTriggerType()+":"+o1.getTriggerValue();
				String value2 = o2.getTriggerType()+":"+o2.getTriggerValue();
				return value1.compareTo(value2);
			}
        });        
		List<Task> taskList = new ArrayList<>();

		int taskNo=0;
		String currentTime = this.timeUtil.getCurrentTime();
		
		for(AgentWork work : agentWorkList) {
			
			// 필터1: 수행일 유효성 체크 (work.getTriggerValue() - currentTime < 0)
			if(work.getTriggerType().equals(TriggerType.fixedTime)) {
				if(this.timeUtil.getDifferenceSeconds(currentTime,work.getTriggerValue()) < 0) {continue;}
			}
			
			// 필터2: 트리거 종료일 체크 (work.getTriggerEndDate() - currentTime < 0)
			if(!StringUtils.isEmpty(work.getTriggerEndDate())) {
				if(this.timeUtil.getDifferenceSeconds(currentTime,work.getTriggerEndDate()) < 0) {continue;}
			}
			
			Task task = new Task();
			task.setTaskId("task"+(++taskNo));
			task.setName(work.getName());
			task.setDescription(work.getDescription());
			task.setStatus(TaskStatus.load);
			task.setTriggerType(work.getTriggerType());
			task.setTriggerMemo(work.getTriggerType()+"("+work.getTriggerValue()+")");
			if(work.getTriggerType().equals(TriggerType.cron)) {task.setCronExpression(work.getTriggerValue());}
			if(work.getTriggerType().equals(TriggerType.fixedRate)) {task.setFixedRate(work.getTriggerValue());}
			if(work.getTriggerType().equals(TriggerType.fixedDelay)) {task.setFixedDelay(work.getTriggerValue());}
			if(work.getTriggerType().equals(TriggerType.fixedTime)) {task.setFixedTime(work.getTriggerValue());}
			task.setTriggerStartDate(work.getTriggerStartDate());
			task.setTriggerEndDate(work.getTriggerEndDate());
			task.setExecuteRequest(work.getExecuteRequest());
			taskList.add(task);
		}

		// step4: context 등록
		this.context.setTaskList(taskList);
	}	
	
	private void startScheduler() {
		if(!this.props.isEnabled()) {log.debug(">> property(\"alpha.scheduler.enabled\") is false"); return;}
		if(this.context==null) {throw new SystemException("schedulerContext is null");}	

		this.context.setSchedulerStartTime(null);
		this.context.setSchedulerStopTime(null);		
		this.context.setSchedulerStatus(null);
		if(this.context.getTaskList().size()==0) {return;}

		ThreadPoolTaskScheduler scheduler = this.context.buildScheduler(this.props.getPoolSize(),this.props.getThreadNamePrefix());
        
		this.context.getTaskList().forEach(task->{
			if(task.getTriggerType()==null) {log.debug("TriggeType is null");}
			else if(task.getTriggerType().equals(TriggerType.cron)) {this.doSchedulerCronTrigger(scheduler,task);}
			else if(task.getTriggerType().equals(TriggerType.fixedDelay)) {this.doSchedulerWithFixedDelay(scheduler,task);}
			else if(task.getTriggerType().equals(TriggerType.fixedRate)) {this.doSchedulerAtFixedRate(scheduler,task);}
			else if(task.getTriggerType().equals(TriggerType.fixedTime)) {this.doSchedulerAtFixedTime(scheduler,task);}	
			task.setStatus(TaskStatus.ready);
		});
		this.context.setSchedulerStartTime(this.timeUtil.getCurrentTime());
		this.context.setSchedulerStatus(SchedulerStatus.start);
		
        log.info(">> scheduler.staring...");
        log.info(">> scheduler.poolSize: {}",scheduler.getPoolSize());
        log.info(">> scheduler.threadNamePrefix: {}",scheduler.getThreadNamePrefix());
        this.context.getTaskList().forEach(task->{
        	log.info(">> scheduler.task: {}",task.toString());
        });
		log.info(">> scheduler.status: {}",this.context.getSchedulerStatus());
	}
	
	private void stopScheduler() {
		if(this.context==null) {throw new SystemException("schedulerContext is null");}
		
        log.info(">> scheduler.stopping...");
		int runningTaskCount=0;
		if(this.context.getTaskList()!=null) {
			this.context.getTaskList().forEach(task->this.doBlockTask(task));
			runningTaskCount = this.context.getTaskCount(TaskStatus.ready);		
		}

		if(runningTaskCount>0) {
			try {Thread.sleep(this.props.getStopCheckTerm());}
			catch (InterruptedException e) {log.debug(e.toString());}
			this.stopScheduler();
		} else {
			ThreadPoolTaskScheduler scheduler = this.context.getScheduler();
			if(scheduler!=null) {
				scheduler.setWaitForTasksToCompleteOnShutdown(false);
				scheduler.shutdown();
			}
			this.context.setSchedulerStopTime(this.timeUtil.getCurrentTime());
			this.context.setSchedulerStatus(SchedulerStatus.stop);
			if(this.context.getTaskList()!=null) {
		        this.context.getTaskList().forEach(task->{
		        	log.debug(">> scheduler.task: {}",task.toString());
		        });
			}
			log.debug(">> scheduler.status: {}",this.context.getSchedulerStatus());
		}
	}
	
	private void doBlockTask(Task task) {
		if(task==null || task.getScheduledFuture()==null) {return;}
		task.getScheduledFuture().cancel(true);
		task.setStatus(TaskStatus.block);
		log.info(">> taskId:{}, taskName:{}, taskStatus:{}",task.getTaskId(),task.getName(),task.getStatus());
	}
		
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void doSchedulerCronTrigger(ThreadPoolTaskScheduler scheduler,Task task) {
		TaskRunnable taskRunnable = new TaskRunnable(task);
		CronTrigger trigger = new CronTrigger(task.getCronExpression());
		ScheduledFuture<?> future = scheduler.schedule(taskRunnable,trigger);
		task.setScheduledFuture(future);
	}

	private void doSchedulerWithFixedDelay(ThreadPoolTaskScheduler scheduler,Task task) {
		TaskRunnable taskRunnable = new TaskRunnable(task);
		long delay = Long.parseLong(task.getFixedDelay());
		Date startTime = new Date(System.currentTimeMillis()+Long.parseLong(task.getFixedDelay()));		
		ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(taskRunnable,startTime,delay);
		task.setScheduledFuture(future);
	}	
	
	private void doSchedulerAtFixedRate(ThreadPoolTaskScheduler scheduler,Task task) {
		TaskRunnable taskRunnable = new TaskRunnable(task);
		long period = Long.parseLong(task.getFixedRate());
		Date startTime = new Date(System.currentTimeMillis()+Long.parseLong(task.getFixedRate()));
		ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(taskRunnable,startTime,period);
		task.setScheduledFuture(future);
	}
	
	private void doSchedulerAtFixedTime(ThreadPoolTaskScheduler scheduler,Task task) {
		try {
			TaskRunnable taskRunnable = new TaskRunnable(task);
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
			Date fixedTime = format.parse(task.getFixedTime());	
			ScheduledFuture<?> future = scheduler.schedule(taskRunnable, fixedTime);
			task.setScheduledFuture(future);
		}catch(Exception e) {
			throw new SystemException(e.getMessage());
		}
	}	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class TaskRunnable implements Runnable {
    	private Task task;

    	public TaskRunnable(Task task) {
    		this.task = task;
    		this.task.setRunnableWrapper(this);
    	}

		@Override
		public void run() {
			if(!this.task.getStatus().equals(TaskStatus.ready)) {
				log.info(">> task({},{})는 ready상태에서만 수행 가능 합니다.",this.task.getTaskId(),this.task.getName());
				return;
			}

			String currentTime = timeUtil.getCurrentTime();
			
			// 체크1 (this.task.getTriggerStartDate()-currentTime > 0)			 
			if(!StringUtils.isEmpty(this.task.getTriggerStartDate())) {
				if(timeUtil.getDifferenceSeconds(currentTime,this.task.getTriggerStartDate()) > 0) {
					log.info(">> task({},{}) 수행 시작일:{} 이후에 수행할 수 있습니다.",this.task.getTaskId(),this.task.getName(),this.task.getTriggerStartDate());
					return;
				}
			}
			
			// 체크2 (this.task.getTriggerEndDate()-currentTime < 0)					
			if(!StringUtils.isEmpty(this.task.getTriggerEndDate())) {
				if(timeUtil.getDifferenceSeconds(currentTime,this.task.getTriggerEndDate()) < 0) {
					log.info(">> task({},{}) 수행 종요일:{} 이전에 수행할 수 있습니다.",this.task.getTaskId(),this.task.getName(),this.task.getTriggerEndDate());
					this.task.getScheduledFuture().cancel(true);
					this.task.setStatus(TaskStatus.cancel);
					return;
				}				
			}
			
			log.info(">> start: {}(hashCode:{}) [name: {}, status: {}, trigger: {}, threadPoolSize: {}, thread: {}]",this.task.getTaskId(),this.hashCode(),this.task.getName(),this.task.getStatus(),this.task.getTriggerMemo(),context.getScheduler().getPoolSize(),Thread.currentThread().getName());						
			long startTime = System.nanoTime();			
			this.task.setStatus(TaskStatus.running);

			try {
				this.pushResponse(this.task,this.agentExecute());
			}catch(Exception e) {
				log.debug(">> error:{}",e.getMessage());
			}
			
			this.task.setExecuteCount(this.task.getExecuteCount()+1);			
			this.task.setStatus(this.task.getTriggerType().equals(TriggerType.fixedTime)?TaskStatus.completed:TaskStatus.ready);			

			long executeTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-startTime);
			log.info(">> end: {}(hashCode:{}) [name: {}, status: {}, executeTime: {}(ms)]",this.task.getTaskId(),this.hashCode(),this.task.getName(),this.task.getStatus(),executeTime);
		}
		
		private ExecuteResponse agentExecute() {
			AgentService agent = BaseUtil.getBean(AgentService.class);
			if(agent==null) {agent = new AgentServiceImpl();}				
			ExecuteRequest request = mapper.convertValue(this.task.getExecuteRequest(), ExecuteRequest.class);			
			return agent.execute(request);
		}
		
		private void pushResponse(Task task,ExecuteResponse response) {
			if(task.getExecuteResponseList()==null) {task.setExecuteResponseList(new ArrayList<>());}			
			List<Object> list = task.getExecuteResponseList();			
			list.add(0,response);
			if(list.size()>10) {list.remove(list.size()-1);}			
		}
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}