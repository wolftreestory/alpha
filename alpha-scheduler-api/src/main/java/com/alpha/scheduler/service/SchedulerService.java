package com.alpha.scheduler.service;

import java.util.List;

import com.alpha.base.context.SchedulerContext.SchedulerStatus;
import com.alpha.base.context.SchedulerContext.Task;
import com.alpha.scheduler.vo.ScedulerInfoPack.AgentWork;

public interface SchedulerService {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public int setAgentWorkList(List<AgentWork> list);

	public int build();
	
	public int start();
	
	public int stop();

	public SchedulerStatus status();

	public List<Task> getTaskList();
	
	public Task getTask(String taskId);
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}	
