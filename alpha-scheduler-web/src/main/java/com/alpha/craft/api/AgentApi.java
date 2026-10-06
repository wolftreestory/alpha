package com.alpha.craft.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alpha.base.support.batch.TaskMetaAgentPack.AgentRequest;
import com.alpha.base.support.batch.TaskMetaAgentPack.AgentResponse;
import com.alpha.base.support.batch.TaskMetaAgentPack.BatchApplication;
import com.alpha.base.support.batch.TaskMetaAgentPack.BatchExecuteParameter;
import com.alpha.base.support.batch.TaskMetaAgentPack.TaskMetaAgent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping(path="/api/agent/bypass/v1")
public class AgentApi {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private TaskMetaAgent taskMetaAgent;
	
    public AgentApi(TaskMetaAgent taskMetaAgent) {
    	this.taskMetaAgent = taskMetaAgent;
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
	@GetMapping("/batchApplicationList")
    @Operation(summary="배치어플리케이션 목록", description="시스템에 구성된 배치어플리케이션 정보를 출력")
    public List<BatchApplication> batchApplicationList() throws Exception {

    	return this.taskMetaAgent.getBatchApplicationList();
	}
    
    @GetMapping("/batchProcessExecuteParameterHist")
    @Operation(summary="배치프로세스 수행 파라메터 이력", description="수행된 배치잡파라메터 이력을 출력")
    public List<BatchExecuteParameter> batchProcessExecuteParameterHist (
    		@Parameter(description="배치어플리케이션이름") @RequestParam(required=true) final String batchAppName) throws Exception {

    	return this.taskMetaAgent.getBatchExecuteParameterHist(batchAppName);
	}
    
    @PostMapping("/execute")
	@Operation(summary="프로세스 수행", description="프로세스 수행")
    public AgentResponse execute (
    		@Parameter(description="실행요청") @RequestBody(required=true) AgentRequest executeRequest) throws Exception {

    	return this.taskMetaAgent.execute(executeRequest);
    }    
    
    @GetMapping("/status")
    @Operation(summary="프로세스 상태", description="프로세스 상태")
    public AgentResponse.ProcessStatus status (
    		@Parameter(description="프로세스수행번호") @RequestParam(required=true) final long processExecuteNo) throws Exception {

    	return this.taskMetaAgent.status(processExecuteNo);
	}

    @GetMapping("/stop")
    @Operation(summary="프로세스 중지", description="프로세스 중지")
    public int stop (
    		@Parameter(description="프로세스수행번호") @RequestParam(required=true) final long processExecuteNo) throws Exception {

    	return this.taskMetaAgent.stop(processExecuteNo);
	}
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
