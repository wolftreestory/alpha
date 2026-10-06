package com.alpha.ytra.service;

import java.util.List;

import com.alpha.ytra.vo.BoardVoPack.BoardVo;

public interface BoardService {
	
	public enum InterfaceType {restTemplate,kafka};
	
    public List<BoardVo> getList(BoardVo vo);
    
    public List<BoardVo> getList(List<?> foreach);
    
    public BoardVo get(int boardNo);

    default public int create(BoardVo vo) {return this.create(vo,InterfaceType.restTemplate);}
    default public int update(BoardVo vo) {return this.update(vo,InterfaceType.restTemplate);}
    default public int delete(int boardNo) {return this.delete(boardNo,InterfaceType.restTemplate);}
    default public List<?> upsert(List<BoardVo> list) {return this.upsert(list,InterfaceType.restTemplate);}

    public int create(BoardVo vo, InterfaceType sendType);
    public int update(BoardVo vo, InterfaceType sendType);
    public int delete(int boardNo, InterfaceType sendType);
    public List<?> upsert(List<BoardVo> list, InterfaceType sendType);

    public void publishData(String topicName,BoardVo vo);

}
