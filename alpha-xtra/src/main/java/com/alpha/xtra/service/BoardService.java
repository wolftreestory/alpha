package com.alpha.xtra.service;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.alpha.xtra.vo.BoardVoPack.BoardVo;

public interface BoardService {
	
    public List<BoardVo> getList(BoardVo vo);
    
    public List<BoardVo> getList(List<?> foreach);
    
    public BoardVo get(int boardNo);

    public BoardVo getBoardInfo(int boardNo);

    public int create(BoardVo vo);
        
    public int update(BoardVo vo);
    
    public int delete(int boardNo);

    public List<?> upsert(List<BoardVo> list);

    public void processTx(List<ConsumerRecord<String,String>> list);
}
