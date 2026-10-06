package com.alpha.scheduler.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alpha.Application.DefaultBinder;
import com.alpha.base.context.SchedulerContext.SchedulerStatus;
import com.alpha.base.context.SchedulerContext.Task;
import com.alpha.scheduler.service.SchedulerService;
import com.alpha.scheduler.vo.ScedulerInfoPack.AgentWork;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping(path="/scheduler/v1")
public class SchedulerController extends DefaultBinder {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Autowired
	private SchedulerService schedulerService;
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@PostMapping("/agentWork")
	@Operation(summary="에이전트작업목록 등록", description="에이전트작업목록 등록")
	public int agentWork(@Parameter(description="에이전트작업 목록") @RequestBody final List<AgentWork> workNote) {

		return this.schedulerService.setAgentWorkList(workNote);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/build")
	@Operation(summary="스케쥴러 빌드", description="스케쥴러 빌드")
	public int build() {

		return this.schedulerService.build();
	}	

	@GetMapping("/start")
	@Operation(summary="스케쥴러 시작", description="스케쥴러 시작")
	public int start() {

		return this.schedulerService.start();
	}	

	@GetMapping("/stop")
	@Operation(summary="스케쥴러 중지", description="스케쥴러 중지")
	public int stop() {

		return this.schedulerService.stop();
	}	

	@GetMapping("/status")
	@Operation(summary="스케쥴러 상태", description="스케쥴러 상태")
	public SchedulerStatus status() {

		return this.schedulerService.status();
	}	
		
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@GetMapping("/taskList")
	@Operation(summary="타스크 목록", description="타스크 목록")
	public List<Task> taskList() {

		return this.schedulerService.getTaskList();
	}
	
	@GetMapping("/task")
	@Operation(summary="타스크 정보", description="타스크 정보")
	public Task task(@Parameter(description="타스크Id") @RequestParam(required=true) final String taskId) {

		return this.schedulerService.getTask(taskId);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}