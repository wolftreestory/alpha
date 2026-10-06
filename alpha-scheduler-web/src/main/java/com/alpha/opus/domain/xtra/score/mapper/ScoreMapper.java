package com.alpha.opus.domain.xtra.score.mapper;

import com.alpha.opus.domain.xtra.score.vo.ScoreVo;

public interface ScoreMapper {		
	public ScoreVo select(String name);	    
	public int insert(ScoreVo scoreVo);	 
	public int update(ScoreVo scoreVo);	 
	public int delete(ScoreVo scoreVo);
	public int deleteAll();		
}