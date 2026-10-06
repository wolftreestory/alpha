package com.alpha.base.support.batch;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.util.StringUtils;

import com.alpha.base.exception.SystemException;
import com.alpha.base.support.batch.TaskConfigPack.TaskConfig;
import com.alpha.base.support.batch.TaskConfigPack.Term;
import com.alpha.base.support.batch.TaskMetaAgentPack.AgentProcess;
import com.alpha.base.support.batch.TaskMetaAgentPack.AgentRequest;
import com.alpha.base.support.batch.TaskMetaAgentPack.AgentResponse;
import com.alpha.base.support.batch.TaskMetaAgentPack.TaskMetaAgent;
import com.alpha.base.support.batch.TaskMetaLoaderPack.TaskMetaObject;
import com.alpha.base.support.batch.context.TaskContext;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static enum Domain {inner,outter};
	public static enum Status {block,ready,running};
	public static enum TriggerType {cron,fixedRate,fixedDelay};
	public static enum ExecutionType {trigger,command};
	public static enum ExceptionType {error,interrupt};

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static interface Base {

		public Domain getDomain();
		public void setDomain(Domain domain);

		public String getName();
		public void setName(String name);
		
		public String getDescription();
		public void setDescription(String description);

		public String getGroup();
		public void setGroup(String group);

		public String getGroupLabel();
		public void setGroupLabel(String groupLabel);

		public String getGroupCode();
		public void setGroupCode(String groupCode);
		
		public void setEnableYn(String enableYn);
		public String getEnableYn();

		public void setHiddenYn(String hiddenYn);
		public String getHiddenYn();
		
	    public String getNameDupYn();
	    public void setNameDupYn(String nameDupYn);	    

	    public String getDeleteYn();
	    public void setDeleteYn(String deleteYn);	    

		public String getRunnableServerYn();
		public void setRunnableServerYn(String runnableServerYn);		

		public TriggerType getTriggerType();
		public String getTriggerAnalysis();
		
		public String getCronExpression() ;
		public void setCronExpression(String cronExpression);
		public List<Map<String,String>> getCronExpressionTimeList(int pageNo, int pageSize);
		
		public String getFixedRate();
		public void setFixedRate(String fixedRate);
		
		public String getFixedDelay();
		public void setFixedDelay(String fixedDelay);	    
		
		public Status getStatus();
		public void setStatus(Status status);

	    public boolean isBlock();
	    public boolean isReady();
	    public boolean isRunning();		

	    public String getCreateDate();
	    public void setCreateDate(String createDate);
	    
	    public String getUpdateDate();
	    public void setUpdateDate(String updateDate);	    
	    
	    public UpdateProcess getUpdateProcess();
	    public void setUpdateProcess(UpdateProcess updateProcess);

	    public DeleteProcess getDeleteProcess();
	    public void setDeleteProcess(DeleteProcess deleteProcess);
	}

	private static interface Config {
	    public String getTaskChangeStamp();
	    public void setTaskChangeStamp(String taskChangeStamp);

	    public TaskConfig getTaskConfig();
	    public void setTaskConfig(TaskConfig taskConfig);
	    public void loadTaskConfig(Set<String> configKeySet);
	    public void saveTaskConfig(Set<String> configKeySet);
	    public void initTaskConfig();
	    public void buildTaskConfig();
	    
	    public String touchTaskConfig(long timeMills);
	    
	    public Object getTaskConfigBizProps(String key);
	    public String getTaskConfigBizProperty(String key);
   
	    default public String getNewChangeStamp() {this.clearTaskChangeStamp(); return this.getTaskChangeStamp();} 
	    default public void clearTaskChangeStamp() {this.setTaskChangeStamp(null);}
		
	}
    
	private static interface Concurrent {	
	    public ScheduledFuture<?> getScheduledFuture();
	    public void setScheduledFuture(ScheduledFuture<?> scheduledFuture);  

		public Future<?> getExecutedFuture();
		public void setExecutedFuture(Future<?> executorFuture);
		
		public Runnable getTaskRunnable();
		public void setTaskRunnable(Runnable runnable);	
	}
		
	public static interface Handler {
		public static interface LogHandler {
			public Logger getLogger();
	        public void print(StringBuffer logBuf);
	        public void print(Logger logger,StringBuffer logBuf);
	        public void init(TaskPack.Task task);
	        public void finish(TaskPack.Task task);
	    } 		
		public LogHandler getLogHandler();	
		public void setLogHandler(LogHandler logHandler);

		public static interface InterruptHandler {		
			public boolean isEnable();
			public String getMessage();
			public void setMessage(String message);
	        public void action() throws Exception;
	    }
		public InterruptHandler getInterruptHandler();	
		public void setInterruptHandler(InterruptHandler interruptHandler);	
	}
	
	private static interface Linker {

		public TaskMetaObject getTaskMetaObject();
		public void setTaskMetaObject(TaskMetaObject metaObject);
		
		public TaskMetaAgent getTaskMetaAgent();
		public void setTaskMetaAgent(TaskMetaAgent taskMetaAgent);

		public AgentRequest getAgentRequest();
		public void setAgentRequest(AgentRequest agentRequest);

		public AgentResponse getAgentResponse();
		public void setAgentResponse(AgentResponse agentResponse);
		
	    public AgentProcess getAgentProcess();
	    public void setAgentProcess(AgentProcess agentProcess);
	}
	
	public static interface Task extends Base, Config, Concurrent, Handler , Linker  {
				
		public String getTaskId();
		public void setTaskId(String taskId);
			
		public int getTaskSeq();
		public void setTaskSeq(int taskSeq);

		public String getTaskToken();
	    public void setTaskToken(String taskToken);
				    
		public String getExecutionNo();
	    public void setExecutionNo(String executionNo);
	    	
		public String getExceptionInfo();
		public void setExceptionInfo(String exceptionInfo);

		public String getExceptionType();
		public void setExceptionType(String exceptionType);

		public String getCountBaseTime();
		public void setCountBaseTime(String countBaseTime);

		public long getExecutionCount();
		public void increaseExecutionCount();

		public long getExceptionCount();
		public void increaseExceptionCount();

		public long getInterruptCount();
		public void increaseInterruptCount();
		
	    public void resetCount();
	    public void printStamp(StringBuffer buf);

	    public void executeTrigger(TaskPack.Task task);
	    public void executeCommand(TaskPack.Task task);
	    public void executeTask();

		public void task() throws Exception;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@FunctionalInterface
	public interface UpdateProcess {public abstract void execute(TaskPack.Task task);}

	@FunctionalInterface
	public interface DeleteProcess {public abstract void execute(TaskPack.Task task);}

	@Getter @Setter
	public static abstract class AbstractTask implements TaskPack.Task {
		
		// for Base
	    private Domain domain = Domain.inner; //default
		private String name;
		private String description;			
		private String group;
		private String groupCode;
		private String groupLable;
		private String cronExpression;
		private String fixedRate;
		private String fixedDelay;	
	    private Status status;
	    private String enableYn = "Y";
	    private String hiddenYn = "N";
	    private String deleteYn = "N";
	    private String runnableServerYn;
	    private String nameDupYn;
	    private String createDate;
	    private String updateDate;
	    private UpdateProcess updateProcess;
	    private DeleteProcess deleteProcess;

	    // for Config & Origin(backup)
	    private TaskConfig taskConfig;
	    private TaskConfig originTaskConfig;
	    private String taskChangeStamp;
		private String originCronExpression;
		private String originFixedRate;
		private String originFixedDelay;
		
	    // for Concurrent
		private ScheduledFuture<?> scheduledFuture;
		private Future<?> executedFuture;
		private Runnable taskRunnable;

		// for Handler
	    private LogHandler logHandler;
	    private InterruptHandler interruptHandler;
		
	    // for Linker
	    private TaskMetaAgent taskMetaAgent;
	    private TaskMetaObject taskMetaObject;
	    private AgentRequest agentRequest;
	    private AgentResponse agentResponse;
	    private AgentProcess agentProcess;

	    // for Task
	    private String taskToken;
	    private String taskId;
	    private int taskSeq;	    		    	
	    private String executionNo;
	    private String countBaseTime;
	    private long executionCount = 0L; 
	    private long exceptionCount = 0L; 
	    private long interruptCount = 0L; 		
	    private String exceptionInfo;
	    private String exceptionType;

	    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		@Override
		public String getGroupLabel() {
			return this.groupLable;
		}
		
		@Override
		public void setGroupLabel(String groupLabel) {
			this.groupLable = groupLabel;
		}

		@Override
		public void setCronExpression(String cronExpression) {
			if(StringUtils.hasText(cronExpression) && !CronExpression.isValidExpression(cronExpression)) {throw new SystemException("Cron표현식이 올바르지 않습니다.");}
			this.cronExpression = cronExpression;
		}

		@Override
		public void setFixedRate(String fixedRate) {
			if(StringUtils.hasText(fixedRate)){
				String value = fixedRate;
				if("smh".contains(value.substring(value.length()-1))) {value = value.substring(0, value.length()-1);}
				if(!NumberUtils.isCreatable(value)) {throw new SystemException("스케쥴러타입 FixedRate는 스케쥴값으로 숫자를 입력해야 합니다.");}
			}
			this.fixedRate = fixedRate;
		}

		@Override
		public void setFixedDelay(String fixedDelay) {
			if(StringUtils.hasText(fixedDelay)) {
				String value = fixedDelay;
				if("smh".contains(value.substring(value.length()-1))) {value = value.substring(0, value.length()-1);}			
				if(!NumberUtils.isCreatable(value)) {throw new SystemException("스케쥴러타입 FixedDelay는 스케쥴값으로 숫자를 입력해야 합니다.");}				
			}
			this.fixedDelay = fixedDelay;		
		}

	    @Override
	    public String getEnableYn() {
	    	if(this.isBlock()) {this.setEnableYn("N");}
	    	return this.enableYn;
	    }

		@Override
		public TriggerType getTriggerType() {
			TriggerType triggerType = null;
			if(triggerType == null && this.getCronExpression() !=  null && !this.getCronExpression().equals("")) {triggerType = TriggerType.cron;}
			if(triggerType == null && this.getFixedRate() !=  null && !this.getFixedRate().equals("")) {triggerType = TriggerType.fixedRate;}
			if(triggerType == null && this.getFixedDelay() !=  null && !this.getFixedDelay().equals("")) {triggerType = TriggerType.fixedDelay;}
			return triggerType;
		}

		@Override
		public String getTriggerAnalysis() {			
			TriggerType triggerType = this.getTriggerType();
			if(triggerType == TriggerType.cron) {return this.getTranslateCron(this.getCronExpression());}
			if(triggerType == TriggerType.fixedDelay) {return this.getTranslateFixedValue(this.getFixedDelay());}
			if(triggerType == TriggerType.fixedRate) {return this.getTranslateFixedValue(this.getFixedRate());}			
			return null;
		}

	    @Override
	    public List<Map<String,String>> getCronExpressionTimeList(int pageNo, int pageSize) {
	        String cronExpression = this.getCronExpression();
	        if (!StringUtils.hasText(cronExpression)) return Collections.emptyList();

	        CronExpression cron = CronExpression.parse(cronExpression);
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
	        LocalDateTime now = LocalDateTime.now();	        

	        // Term 범위를 LocalDateTime으로 변환
	        List<Pair<LocalDateTime, LocalDateTime>> termRanges = null;
	        if(this.getTaskConfig().getScheduleTermEnableYn().equals("Y")) {
		        List<Term> termList = this.getTaskConfig().getScheduleTermList();
		        if(termList!=null && !termList.isEmpty()) {
			        termRanges = termList.stream()
			        		.filter(term -> term.isAlive())
			        		.map(term -> Pair.of(
			        				Instant.ofEpochMilli(term.getStartTimestamp()).atZone(ZoneId.systemDefault()).toLocalDateTime(),
			        				Instant.ofEpochMilli(term.getEndTimestamp()).atZone(ZoneId.systemDefault()).toLocalDateTime()))
			        		.collect(Collectors.toList());
		        }
	        }
	        if(termRanges==null || termRanges.isEmpty()) {
	        	termRanges = new ArrayList<>();
	        	termRanges.add(Pair.of(now,now.plusYears(1)));
	        }
	        termRanges.sort(Comparator.comparing(Pair::getLeft));
	        
	        if(termRanges.get(0).getLeft().getDayOfYear() <= now.getDayOfYear() && termRanges.get(0).getLeft().getHour()!=now.getHour()) {
	        	termRanges.set(0,Pair.of(now,termRanges.get(0).getRight()));    	
	        }
	        //termRaalpha.forEach(i->log.debug(">> term:{}", i));
	        
	        List<Map<String,String>> timeList = new ArrayList<>();
	        int startIndex = (pageNo - 1) * pageSize;
	        int endIndex = startIndex + pageSize;

	        log.debug(">> startIndex:{}",startIndex);
	        log.debug(">> endIndex:{}",endIndex);
	        
	        int count = 0;
	        for(Pair<LocalDateTime, LocalDateTime> pair : termRanges) {
	        	LocalDateTime nextExecutionTime = pair.getLeft();
	        	LocalDateTime endExecutionTime = pair.getRight();
	        	log.debug(">> pair:{}",pair);
	        	
		        while(nextExecutionTime.isBefore(endExecutionTime) && count < endIndex) {
		            nextExecutionTime = cron.next(nextExecutionTime);
		            if(count == 0) {
		                Map<String, String> timeMap = new HashMap<>();
		                timeMap.put("executionTime", now.format(formatter));
		                timeMap.put("remainingTime", "now");
		                timeList.add(timeMap);
		            }
		            if(count >= startIndex) {
		                Map<String, String> timeMap = new HashMap<>();
		                timeMap.put("executionTime", nextExecutionTime.format(formatter));
		                timeMap.put("remainingTime",this.getRemainingTime(Duration.between(now, nextExecutionTime)));
		                timeList.add(timeMap);
		            }	            
		            count++;
		        }
	        }
	        
	        return timeList;
	    }
		
	    @Override
	    public boolean isBlock() {
	    	return Status.block.equals(this.status)?true:false;
	    }
	    
	    @Override
	    public boolean isReady() {
	    	return Status.ready.equals(this.status)?true:false;
	    }
	    
	    @Override
	    public boolean isRunning() {
	    	return Status.running.equals(this.status)?true:false;
	    }

	    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	    @Override
	    public String getTaskChangeStamp() {
	    	if(!StringUtils.hasText(this.taskChangeStamp)) {this.setTaskChangeStamp(String.valueOf(System.currentTimeMillis()));}
	    	return this.taskChangeStamp;
	    }

	    @Override
	    public void setTaskConfig(TaskConfig taskConfig) {	    	
	    	if(!StringUtils.hasText(this.cronExpression+this.fixedRate+this.fixedRate)) {throw new SystemException("수행스케쥴 정보가 없습니다.");}
			if(StringUtils.hasText(this.cronExpression) && !CronExpression.isValidExpression(this.cronExpression)) {throw new SystemException("Cron표현식이 올바르지 않습니다.");}
			if(StringUtils.hasText(this.fixedRate) && !NumberUtils.isCreatable(this.fixedRate)) {throw new SystemException("스케쥴러타입 FixedRate는 스케쥴값으로 숫자를 입력해야 합니다.");}
			if(StringUtils.hasText(this.fixedRate) && !NumberUtils.isCreatable(this.fixedRate)) {throw new SystemException("스케쥴러타입 FixedRate는 스케쥴값으로 숫자를 입력해야 합니다.");}

			// 초기화시 활용됨
	    	this.originCronExpression = this.cronExpression;
	    	this.originFixedDelay = this.fixedDelay;
	    	this.originFixedRate = this.fixedRate;	
	    	this.originTaskConfig = taskConfig;
	    	
	    	this.taskConfig = taskConfig.clone();	    	
	    }
	    
	    @Override
	    public void initTaskConfig() {	    	
	    	this.taskConfig.deleteConfigData(this.name);
	    	this.taskConfig = this.originTaskConfig.clone();	    	
	    	this.cronExpression = this.originCronExpression;
	    	this.fixedDelay = this.originFixedDelay;
	    	this.fixedRate = this.originFixedRate;
	    	this.buildTaskConfig();
	    }
	    
	    @Override
	    public void loadTaskConfig(Set<String> configKeySet) {
	    	this.taskConfig.loadConfigData(this.getName(), configKeySet);
	    	this.setCronExpression(null);
	    	this.setFixedDelay(null);
	    	this.setFixedRate(null);    	
	    	if(this.getTaskConfig().getScheduleType().equals("cronExpression")) {this.setCronExpression(this.getTaskConfig().getScheduleValue());}
	    	if(this.getTaskConfig().getScheduleType().equals("fixedDelay")) {this.setFixedDelay(this.getTaskConfig().getScheduleValue());}
	    	if(this.getTaskConfig().getScheduleType().equals("fixedRate")) {this.setFixedRate(this.getTaskConfig().getScheduleValue());}	    	
	    }
	   
	    @Override
	    public void saveTaskConfig(Set<String> configKeySet) {
	    	this.taskConfig.saveConfigData(this.getName(), configKeySet);
	    }

	    @Override
	    public String touchTaskConfig(long timeMills) {
	    	return this.getTaskConfig().touchConfigData(this.name, timeMills);
	    }
	    
	    @Override
	    public Object getTaskConfigBizProps(String key) {
	    	return StringUtils.hasText(key)?this.getTaskConfigProperty().get(key):null;
	    }
	   
	    @Override
	    public String getTaskConfigBizProperty(String key) {
	    	return StringUtils.hasText(key)?this.getTaskConfigProperty().getProperty(key):null;
	    }
	        
	    @Override	    
	    public void buildTaskConfig() {
	    	TaskConfig _taskConfig = this.taskConfig.clone();

	    	// 소스코드에 설정한 스케쥴정보로 부터 taskConfig의 scheduleType, scheduleValue를 설정
	    	if(StringUtils.hasText(this.getCronExpression())) {_taskConfig.setScheduleType("cronExpression");_taskConfig.setScheduleValue(this.getCronExpression());}
	    	if(StringUtils.hasText(this.getFixedDelay())) {_taskConfig.setScheduleType("fixedDelay");_taskConfig.setScheduleValue(this.getFixedDelay());}
	    	if(StringUtils.hasText(this.getFixedRate())) {_taskConfig.setScheduleType("fixedRate");_taskConfig.setScheduleValue(this.getFixedRate());}

	    	// 저장된 설정정보(최초이면, 설정을 저장함)를 실제 적용되는 this.taskConfig로 지정
	    	_taskConfig.loadConfigData(this.getName());
	    	this.taskConfig = _taskConfig;
	    	
	    	// 저장된 scheduleType, scheduleValue 로 스케쥴정보 등록
	    	this.setCronExpression(null);
	    	this.setFixedDelay(null);
	    	this.setFixedRate(null);
	    	if(StringUtil.isNotBlank(this.taskConfig.getScheduleType())) {
		    	if(this.taskConfig.getScheduleType().equals("cronExpression")) {this.setCronExpression(this.taskConfig.getScheduleValue());}
		    	if(this.taskConfig.getScheduleType().equals("fixedDelay")) {this.setFixedDelay(this.taskConfig.getScheduleValue());}
		    	if(this.taskConfig.getScheduleType().equals("fixedRate")) {this.setFixedRate(this.taskConfig.getScheduleValue());}
	    	}
	    }
	    
	    private Properties getTaskConfigProperty() {
	        if(this.getTaskConfig().getBizProps() == null) {return null;}        
	        return this.getTaskConfig().getBizProps();
	    }

	    private String getTranslateFixedValue(String value) {
	    	StringBuilder builder = new StringBuilder();
	        DecimalFormat formatter = new DecimalFormat("#,###");

	        char lastChar = value.charAt(value.length() - 1);

	        if (Character.isDigit(lastChar)) {
                long number = Long.parseLong(value);
                builder.append(formatter.format(number)).append(" 밀리초(1/1000)");
	        } else {
	            String digitPart = value.substring(0, value.length() - 1);
                long number = Long.parseLong(digitPart);
                if (lastChar == 's') {builder.append(formatter.format(number)).append("초");}
                else if (lastChar == 'm') {builder.append(formatter.format(number)).append("분");}
                else if (lastChar == 'h') {builder.append(formatter.format(number)).append("시간");}
                else builder.append("변환오류");
	        }
	        return builder.toString();
	    }
	    
	    private String getRemainingTime(Duration duration) {
	    	StringBuilder builder = new StringBuilder();	    	
            if(duration.toMinutes()>60) {
                  if(duration.toHours()>24) {
                	  builder.append(duration.toDays()).append("일 ");
                	  builder.append(duration.toHours()%24).append("시간 ");
                	  builder.append(duration.toMinutes()%60).append("분 ");	                    	  
                  } else {
                	  builder.append(duration.toHours()).append("시간 ");
                	  builder.append(duration.toMinutes()%60).append("분 ");                   	  
                  }
            } else {
          	  	builder.append(duration.toMinutes()).append("분 ");                	
            }
            return builder.toString();
	    }
	    	    
        private String getTranslateCron(String cron) {
            String[] parts = cron.split(" ");
            if (parts.length < 6 || parts.length > 7) {return "변환오류";}

            Map<String, String> weekdays = new HashMap<>();
            weekdays.put("0", "일요일");
            weekdays.put("1", "월요일");
            weekdays.put("2", "화요일");
            weekdays.put("3", "수요일");
            weekdays.put("4", "목요일");
            weekdays.put("5", "금요일");
            weekdays.put("6", "토요일");            
            weekdays.put("7", "일요일");            
            weekdays.put("SUN", "일요일");
            weekdays.put("MON", "월요일");
            weekdays.put("TUE", "화요일");
            weekdays.put("WED", "수요일");
            weekdays.put("THU", "목요일");
            weekdays.put("FRI", "금요일");
            weekdays.put("SAT", "토요일");

            String seconds = parseField(parts[0], "초");
            String minutes = parseField(parts[1], "분");
            String hours = parseField(parts[2], "시");
            String day = parseField(parts[3], "일");
            String month = parseField(parts[4], "월");
            String weekday = weekdays.getOrDefault(parts[5], this.parseField(parts[5], "요일"));
            String year = (parts.length == 7) ? parseField(parts[6], "년") : "";

            return (parts.length == 7) 
                ? String.format("%s, %s, %s, %s, %s, %s, %s", seconds, minutes, hours, day, month, weekday, year) 
                : String.format("%s, %s, %s, %s, %s, %s", seconds, minutes, hours, day, month, weekday);
        }

        private String parseField(String field, String unit) {
            if (field.equals("*")) return unit.equals("요일") ? "모든 요일" : "매 " + unit;
            if (field.contains("/")) return field.replace("/", unit+" 부터 시작하여 ") + unit + "마다";
            if (field.contains("-")) return field.replace("-", "부터 ") + unit;
            if (field.contains(",")) return field.replace(",", " 또는 ") + unit;
            if (field.equals("?")) return "특정 " + unit + " 없음";
            if (field.equals("L")) return "마지막 " + unit;
            
            return field + unit;
        }
   		
	    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	    @Override
	    public String getExecutionNo() {
	    	if(this.executionNo == null) {this.executionNo = "";}
	    	return this.executionNo;
	    }
	    
	    @Override
	    public void setExecutionNo(String executionNo) {
	    	this.executionNo = executionNo;
	    	this.setTaskToken(executionNo+"."+this.getName());
	    }

		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	    @Override
	    public void increaseExecutionCount() {
	    	this.checkCountBaseTime();
	    	this.executionCount++;
	    }

		@Override
		public void increaseExceptionCount() {
			this.checkCountBaseTime();
			this.exceptionCount++;
		}

		@Override
		public void increaseInterruptCount() {
			this.checkCountBaseTime();
			this.interruptCount++;
		}	

		@Override
		public void resetCount() {
		    this.setCountBaseTime(null);
		    this.executionCount = 0;
		    this.exceptionCount = 0;
		    this.interruptCount = 0;
		}

	    @Override
	    public void printStamp(StringBuffer buf) {
	    	if(this.logHandler != null) {
	    		this.logHandler.print(LoggerFactory.getLogger(this.getClass()),buf);
	    	}
	    }

		private void checkCountBaseTime() {
			if(this.countBaseTime == null || this.countBaseTime.equals("")) {
		        Calendar nowTime = Calendar.getInstance();
		        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		        this.setCountBaseTime(sd.format(nowTime.getTime()));
			}
		}

		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	        
		@Override
	    public void executeTrigger(TaskPack.Task task) {
	        boolean isEnable = true;
	        if(isEnable && task == null) {isEnable = false;}
	        if(isEnable && this.isBlock()) {isEnable = false; log.debug(">> isBlock: {}",isEnable);}
	        if(isEnable && this.isRunning()) {isEnable = false; log.debug(">> isRunning: {}",isEnable);}
	        if(isEnable && !this.getEnableYn().equals("Y")) {isEnable = false; log.debug(">> isEnable: {}",isEnable);}
	        if(isEnable && !this.getTaskConfig().isRunnableServer()) {isEnable = false; log.debug(">> isRunnableServer: {}",isEnable);}
	        if(isEnable && this.getTaskConfig().getScheduleTermEnableYn().equals("N")) {
	        	if(isEnable && !this.getTaskConfig().isScheduleTermAlive()) {
	        		isEnable = false;
	        		this.setEnableYn("N");
	        		log.debug(">> isScheduleTermAlive: {}",isEnable);
	        	}
	        	if(isEnable && !this.getTaskConfig().isScheduleTermAlive()) {
	        		isEnable = false;
	        		log.debug(">> isScheduleTermAlive: {}",isEnable);
	        	}
	        }
	        
	        if(isEnable) {
	        	task.executeTask();
	        }else {
	        	log.debug(">> [TASK] {}, isExecutable: {}",this.getName(),isEnable);
	        }
	        return;
	    }

	    @Override
	    public void executeCommand(TaskPack.Task task) {
	        boolean isEnable = true;
	        if(isEnable && task == null) {isEnable = false;}
	        if(isEnable && this.isBlock()) {isEnable = false; log.debug(">> isBlock: {}",isEnable);}
	        if(isEnable && !this.getTaskConfig().isRunnableServer()) {isEnable = false; log.debug(">> isRunnableServer: {}",isEnable);}
	        if(isEnable) {
	        	task.executeTask();
	        } else {
	        	log.debug(">> [TASK] {}, isExecutable: {}",this.getName(),isEnable);
	        }
	        return;
	    }

	    @Override
	    public void executeTask() {
	        try {
	        	this.getLogHandler().init(this);      	
	        	log.info(">> [TASK] start:{}, hashCode:{}",this.getName(),this.hashCode());
	            this.setStatus(TaskPack.Status.running);
	            TaskContext.setThreadTask(Thread.currentThread().hashCode(),this);
	            this.setExceptionType(null);
	            this.setExceptionInfo(null);
	            this.increaseExecutionCount();
	            this.task();
	        } catch(InterruptedException te) {
	            if(this.getInterruptHandler() != null && this.getInterruptHandler().isEnable()) {
	            	try {
	            		this.getInterruptHandler().action();
	            	}catch(Exception ae) {
	            		log.error(">> [TASK] exception:{}, hashCode:{}",this.getName(),this.hashCode(),ae);
	            		this.setExceptionType(ae.getClass().toString());
	            		this.setExceptionInfo(ae.toString());
	            		this.increaseExceptionCount();
	            		throw new RuntimeException(ae);
	            	}
	            	log.error(">> [TASK] interrupt:{}, hashCode:{}",this.getName(),this.hashCode(),te);
	        		this.setExceptionType(te.getClass().toString());
	        		this.setExceptionInfo(te.toString());
	            	this.increaseInterruptCount();
	            	throw new RuntimeException(te);
	            }
	        } catch(Exception te) {
	        	log.error(">> [TASK] exception:{}, hashCode:{}",this.getName(),this.hashCode(),te);
	    		this.setExceptionType(te.getClass().toString());	
	    		this.setExceptionInfo(te.toString());
	        	this.increaseExceptionCount();
	        	throw new RuntimeException(te);
	        } finally {
	            TaskContext.removeThreadTask(Thread.currentThread().hashCode());
	            this.setStatus(Status.ready);
	            log.info(">> [TASK] end:{}, hashCode:{}",this.getName(),this.hashCode());
	            if(this.getUpdateProcess()!=null) {this.getUpdateProcess().execute(this);}
	            if(this.getDeleteProcess()!=null) {this.getDeleteProcess().execute(this);}
	            		
	            this.getLogHandler().finish(this);
	        }
	        
	        return;
	    }
	    
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	    @Override
		public abstract void task() throws Exception;
	    
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	}	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}