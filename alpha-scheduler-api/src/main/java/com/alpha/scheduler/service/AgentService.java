package com.alpha.scheduler.service;

import java.util.List;

import com.alpha.scheduler.vo.BatchInfoPack.BatchApplication;
import com.alpha.scheduler.vo.BatchInfoPack.BatchExecuteParameter;
import com.alpha.scheduler.vo.ExecuteInfoPack.ExecuteRequest;
import com.alpha.scheduler.vo.ExecuteInfoPack.ExecuteResponse;
import com.alpha.scheduler.vo.ExecuteInfoPack.ExecuteStatus;

public interface AgentService {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public ExecuteResponse execute(ExecuteRequest request);
	
	public ExecuteStatus status(long executeId);

	public int stop(long executeId);

	public List<BatchApplication> getBatchApplicationList();

	public List<BatchExecuteParameter> getBatchExecuteParameterHist(String appName);
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
	

