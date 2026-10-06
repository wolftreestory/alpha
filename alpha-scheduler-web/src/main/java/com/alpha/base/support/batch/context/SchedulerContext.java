package com.alpha.base.support.batch.context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.alpha.base.support.batch.TaskPack;
import com.alpha.base.support.batch.SchedulerManagerPack.SchedulerManager;
import com.alpha.base.support.batch.TaskPack.Status;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchedulerContext implements Serializable {

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final long serialVersionUID=1L;
	
	private String schedulerChangeStamp;
	private String schedulerContextStartTime;
	private String schedulerStatus;
	private String schedulerStartTime;
	private String schedulerStopTime;

	private List<TaskPack.Task> taskList;
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean isEnableSchedulerChangeStamp(String schedulerChangeStamp) {
		if(this.getSchedulerChangeStamp().equals(schedulerChangeStamp)) {return true;}
		return false;
	}
	
    public String getSchedulerChangeStamp() {
        if(this.schedulerChangeStamp==null) {this.setSchedulerChangeStamp(String.valueOf(System.currentTimeMillis()));}
        return this.schedulerChangeStamp;
    }
    
    public String getNewSchedulerChangeStamp() {
        this.setSchedulerChangeStamp(String.valueOf(System.currentTimeMillis()));
        return this.schedulerChangeStamp;
    }

    public void setSchedulerChangeStamp(String schedulerChangeStamp) {
        this.schedulerChangeStamp=schedulerChangeStamp;
    }

    public void clearschedulerChangeStamp() {
        this.setSchedulerChangeStamp(null);
    }
    
    public void clearChangeStamp() {
        if(this.taskList!=null) {this.taskList.forEach(i->i.clearTaskChangeStamp());}
    }
            
	public String getSchedulerStatus() {
		return this.schedulerStatus;
	}

	public void setSchedulerStatus(String schedulerStatus) {
		this.schedulerStatus=schedulerStatus;
	}

	public String getSchedulerStartTime() {
		return this.schedulerStartTime;
	}

	public void setSchedulerStartTime(String schedulerStartTime) {
		this.schedulerStartTime=schedulerStartTime;
	}

	public String getSchedulerStopTime() {
		return this.schedulerStopTime;
	}

	public void setSchedulerStopTime(String schedulerStopTime) {
		this.schedulerStopTime=schedulerStopTime;
	}

    public String getSchedulerContextStartTime() {
    	return this.schedulerContextStartTime;
    }
    
	public void setSchedulerContextStartTime(String schedulerContextStartTime) {
		this.schedulerContextStartTime=schedulerContextStartTime;
	}

	public synchronized void addTask(TaskPack.Task task) {
		if(this.taskList == null) {this.taskList = new ArrayList<>();}
		this.taskList.add(task);
		this.taskList.sort(Comparator.comparing(TaskPack.Task::getGroup).thenComparing(TaskPack.Task::getName));
	}

	public List<TaskPack.Task> getSubList(List<TaskPack.Task> taskList, Map<String,String> pageInfo) {
		if(taskList==null || taskList.isEmpty()) {return Collections.emptyList();}		
		
		int listSize = taskList.size();
        int pageNo = Integer.parseInt(pageInfo.get("taskListPageNo"));
        int pageSize = Integer.parseInt(pageInfo.get("taskListPageSize"));
        int fromIndex = Math.max(0, (pageNo - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, listSize);
        int totalPages = (int) Math.ceil((double) listSize / pageSize);

        pageInfo.put("pageInfo", pageNo+"/"+totalPages);
        pageInfo.put("firstYn", pageNo==1?"Y":"N");
        pageInfo.put("lastYn", fromIndex + pageSize >= listSize ? "Y" : "N");
        
		return taskList.subList(fromIndex, toIndex);
	}

	public boolean isExistHidden() {
		List<TaskPack.Task> list = this.getTaskList();
		return list.stream().anyMatch(t -> t.getHiddenYn().equals("Y"));
	}

	public List<TaskPack.Task> getTaskList(String filterValue, String filterType, String hiddenFilterYn) {
		return this.getTaskList("group", filterValue, filterType, hiddenFilterYn);
	}
	
	public List<TaskPack.Task> getTaskList(String filterArea, String filterValue, String filterType, String hiddenFilterYn) {
		List<TaskPack.Task> rawList = this.getTaskList();
		if(rawList==null || rawList.isEmpty()) {return null;}

		if(hiddenFilterYn.equals("Y")) {
			rawList = rawList.stream().filter(item -> item.getHiddenYn().equals("Y")?false:true).collect(Collectors.toList());
		}
		
		if(filterValue==null || filterValue.equals("")){return rawList;}

		String[] filterValues = filterValue.indexOf(",")!=-1?filterValue.split(","):new String[]{filterValue};
		filterValues = Arrays.stream(filterValues).map(String::trim).toArray(String[]::new);
		
		Set<TaskPack.Task> taskSet = new TreeSet<>(Comparator.comparing(TaskPack.Task::getGroup).thenComparing(TaskPack.Task::getName));
		for (String value : filterValues) {
			if(!StringUtils.hasText(value)) {continue;}
			if(filterType.equals("equals")) {
				if(filterArea.equals("group")) {
					taskSet.addAll(rawList.stream().filter(t -> t.getGroup()==null?false:t.getGroup().equals(value)).collect(Collectors.toSet()));			
					taskSet.addAll(rawList.stream().filter(t -> t.getGroupCode()==null?false:t.getGroupCode().equals(value)).collect(Collectors.toSet()));			
					taskSet.addAll(rawList.stream().filter(t -> t.getGroupLabel()==null?false:t.getGroupLabel().equals(value)).collect(Collectors.toSet()));
				}
				if(filterArea.equals("name")) {
					taskSet.addAll(rawList.stream().filter(t -> t.getName()==null?false:t.getName().equals(value)).collect(Collectors.toSet()));			
				}
				if(filterArea.equals("description")) {
					taskSet.addAll(rawList.stream().filter(t -> t.getDescription()==null?false:t.getDescription().equals(value)).collect(Collectors.toSet()));								
				}
			} 
			if(filterType.equals("contains")) {
				if(filterArea.equals("group")) {
					taskSet.addAll(rawList.stream().filter(t -> t.getGroup()==null?false:t.getGroup().contains(value)).collect(Collectors.toSet()));							
					taskSet.addAll(rawList.stream().filter(t -> t.getGroupCode()==null?false:t.getGroupCode().contains(value)).collect(Collectors.toSet()));			
					taskSet.addAll(rawList.stream().filter(t -> t.getGroupLabel()==null?false:t.getGroupLabel().contains(value)).collect(Collectors.toSet()));
				}
				if(filterArea.equals("name")) {
					taskSet.addAll(rawList.stream().filter(t -> t.getName()==null?false:t.getName().contains(value)).collect(Collectors.toSet()));			
				}
				if(filterArea.equals("description")) {
					taskSet.addAll(rawList.stream().filter(t -> t.getDescription()==null?false:t.getDescription().contains(value)).collect(Collectors.toSet()));								
				}
			}
		}
		List<TaskPack.Task> taskList = new ArrayList<>(taskSet);

		return taskList;
	}

	public List<TaskPack.Task> getTaskList() {
		if(this.taskList==null) {this.taskList = new ArrayList<>();}
		return this.taskList;
	}
	
	public void setTaskList(List<TaskPack.Task> taskList) {
		this.taskList=taskList;
		if(this.taskList==null) {return;}

		for(int i=0;i<this.taskList.size();i++) {
			this.taskList.get(i).setTaskId("task"+i);		    
		    this.taskList.get(i).setName(this.taskList.get(i).getName().trim());
		    
		    //check1.nameDup
		    int dupCount=0;
		    for(int j=0;j<this.taskList.size();j++) {		    	
		        if(this.taskList.get(j).getName().equals(this.taskList.get(i).getName())) {dupCount++;if(dupCount>1) {break;}}
		    }
	        this.taskList.get(i).setNameDupYn(dupCount==1?"N":"Y");
	        
		    //check2.executionServer
	        this.taskList.get(i).setRunnableServerYn(this.taskList.get(i).getTaskConfig().isRunnableServer()?"Y":"N");

	        boolean isEnable=true;
            if(isEnable && this.taskList.get(i).getNameDupYn().equals("Y")) {isEnable=false;}
            if(isEnable && this.taskList.get(i).getRunnableServerYn().equals("N")) {isEnable=false;}            
	        if(!isEnable) {
                this.taskList.get(i).setStatus(Status.block);
	        } else {
	            this.taskList.get(i).setStatus(Status.ready);	            
	        }
		}
		return;
	}
	
    public TaskPack.Task getTask(String taskId) {
		List<TaskPack.Task> taskList=this.getTaskList();
		for(int i=0;i<taskList.size();i++) {     
	        if(taskList.get(i).getTaskId().equals(taskId)) {return taskList.get(i);}			
		}
		return null;
	}
	
    public TaskPack.Task getTaskByName(String taskName) {
        List<TaskPack.Task> taskList=this.getTaskList();
        for(int i=0;i<taskList.size();i++) {   
            if(taskList.get(i).getName().equals(taskName)) {return taskList.get(i);}         
        }
        return null;
    }	
    
    public int getRunningTaskCount() {
        int count=0;
        for(int i=0;i<this.taskList.size();i++) {
            if(Status.running.equals(this.taskList.get(i).getStatus())) {count++;}
        }        
    	return count;
    }
    
    public boolean isExistRunningTask() {
        if(this.taskList==null) {return false;}        
        boolean isExist=false;
        for(int i=0;i<this.taskList.size();i++) {
            if(Status.running.equals(this.taskList.get(i).getStatus())) {isExist=true;break;}
        }        
        return isExist;
    }   
   
    public boolean isExistEnableTask() {
        if(this.taskList==null) {return false;}
        
        boolean isExist=false;
        for(int i=0;i<this.taskList.size();i++) {
            if(this.taskList.get(i).getEnableYn().equals("Y")) {isExist=true;break;}
        }
        
        return isExist;
    } 
    
    public boolean isSchedulerStart() {
        if(SchedulerManager.SCHEDULER_STATUS_START.equals(this.getSchedulerStatus())) {return true;}        
        return false;   
    }
    
    public boolean isSchedulerStop() {
        if(SchedulerManager.SCHEDULER_STATUS_STOP.equals(this.getSchedulerStatus())) {return true;}
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}