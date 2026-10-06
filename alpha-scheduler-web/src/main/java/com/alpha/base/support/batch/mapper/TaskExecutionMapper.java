package com.alpha.base.support.batch.mapper;

import java.util.List;
import java.util.Map;

import com.alpha.base.support.batch.vo.TaskExecutionVo;

public interface TaskExecutionMapper {
	
	//////////////////////////////////////////////////////////////////////////////////////

	public String selectTaskExecutionNo();

	public List<Map<String,Object>> selectTaskExecutionList(TaskExecutionVo vo);
	public void insertTaskExecutionInfo(TaskExecutionVo vo);
	public void updateTaskExecutionInfo(TaskExecutionVo vo);
	public void deleteTaskExecutionInfo(TaskExecutionVo vo);

	public Map<String,Object> selectDownloadChunkInfo(TaskExecutionVo vo);

	//////////////////////////////////////////////////////////////////////////////////////
	
	public List<Map<String,Object>> selectTaskLogList(TaskExecutionVo vo);	
	public void insertTaskExecutionLogInfo(TaskExecutionVo vo);
	public void deleteTaskExecutionLogInfo(TaskExecutionVo vo);

	//////////////////////////////////////////////////////////////////////////////////////

}