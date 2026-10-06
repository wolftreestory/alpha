package com.alpha.xtra.mapper;

import java.util.List;

import com.alpha.base.context.PageContext;
import com.alpha.xtra.vo.BoardVoPack.BoardVo;

public interface BoardMapper {
    
	public int selectTotalCount(BoardVo boardVo, List<?> foreach);
	
	public List<BoardVo> select(BoardVo boardVo, List<?> foreach, PageContext pageContext);
    
	public int insert(BoardVo boardVo);
    
	public int update(BoardVo boardVo);
    
	public int delete(BoardVo boardVo);
}