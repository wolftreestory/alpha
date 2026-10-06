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
import com.alpha.scheduler.service.AgentService;
import com.alpha.scheduler.vo.BatchInfoPack.BatchApplication;
import com.alpha.scheduler.vo.BatchInfoPack.BatchExecuteParameter;
import com.alpha.scheduler.vo.ExecuteInfoPack.ExecuteRequest;
import com.alpha.scheduler.vo.ExecuteInfoPack.ExecuteResponse;
import com.alpha.scheduler.vo.ExecuteInfoPack.ExecuteStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping(path="/scheduler/agent/v1")
public class AgentController extends DefaultBinder {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Autowired
	private AgentService agentService;
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
	@GetMapping("/batchApplicationList")
    @Operation(summary="배치어플리케이션 목록", description="시스템에 구성된 배치어플리케이션 정보를 출력")
    public List<BatchApplication> batchApplicationList() throws Exception {

    	return this.agentService.getBatchApplicationList();
	}
    
    @GetMapping("/batchProcessExecuteParameterHist")
    @Operation(summary="배치프로세스 수행 파라메터 이력", description="수행된 배치잡파라메터 이력을 출력")
    public List<BatchExecuteParameter> batchProcessExecuteParameterHist (
    		@Parameter(description="배치어플리케이션이름") @RequestParam(required=true) final String batchAppName) throws Exception {

    	return this.agentService.getBatchExecuteParameterHist(batchAppName);
	}
    
    @PostMapping("/execute")
	@Operation(summary="프로세스 수행", description="프로세스 수행")
    public ExecuteResponse execute (
    		@Parameter(description="실행요청") @RequestBody(required=true) ExecuteRequest executeRequest) throws Exception {

    	return this.agentService.execute(executeRequest);
    }    
    
    @GetMapping("/status")
    @Operation(summary="프로세스 상태", description="프로세스 상태")
    public ExecuteStatus status (
    		@Parameter(description="프로세스수행번호") @RequestParam(required=true) final long processExecuteId) throws Exception {

    	return this.agentService.status(processExecuteId);
	}

    @GetMapping("/stop")
    @Operation(summary="프로세스 중지", description="프로세스 중지")
    public int stop (
    		@Parameter(description="프로세스수행번호") @RequestParam(required=true) final long processExecuteId) throws Exception {

    	return this.agentService.stop(processExecuteId);
	}
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
