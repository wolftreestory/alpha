package com.alpha.opus.domain.xtra.board.mapper;

import java.util.List;

import com.alpha.opus.domain.xtra.board.vo.BoardVo;

public interface BoardMapper {	    
	public int selectTotalCount(BoardVo boardVo, List<?> foreach);		
	public List<BoardVo> select(BoardVo boardVo, List<?> foreach);	    
	public int insert(BoardVo boardVo);	    
	public int update(BoardVo boardVo);	    
	public int delete(BoardVo boardVo);
	public int deleteAll();
}