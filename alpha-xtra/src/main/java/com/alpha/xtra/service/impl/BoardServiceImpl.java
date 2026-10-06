package com.alpha.xtra.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.alpha.base.BaseUtil;
import com.alpha.base.advice.ExceptionAdvice;
import com.alpha.base.exception.BusinessException;
import com.alpha.xtra.mapper.BoardMapper;
import com.alpha.xtra.service.BoardService;
import com.alpha.xtra.vo.BoardVoPack.BoardInDto;
import com.alpha.xtra.vo.BoardVoPack.BoardVo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BoardServiceImpl implements BoardService {
		
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final BoardMapper boardMapper;

	public BoardServiceImpl(BoardMapper boardMapper) {
		this.boardMapper = boardMapper;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public List<BoardVo> getList(BoardVo vo) {
		int totalCount = this.boardMapper.selectTotalCount(vo,null);
		return this.boardMapper.select(vo,null,BaseUtil.getPageContext(totalCount));
	}
	
	@Override
	public List<BoardVo> getList(List<?> foreach){
		int totalCount = this.boardMapper.selectTotalCount(null,foreach);
		return this.boardMapper.select(null,foreach,BaseUtil.getPageContext(totalCount));
	}
	    
	@Override
	public BoardVo get(int boardNo) {
		List<BoardVo> list = this.getList(new BoardVo(boardNo));
		return (list==null || list.isEmpty())?null:list.get(0);
	}
	
	@Override
    public BoardVo getBoardInfo(int boardNo) {
		List<BoardVo> list = this.boardMapper.select(new BoardVo(boardNo),null,null);
		return (list==null || list.isEmpty())?null:list.get(0);
	}
	
	@Override
	public int create(BoardVo vo) {	
		if(this.isExist(vo)) {
			throw new BusinessException(()->ExceptionAdvice.message("biz.board.001"),HttpStatus.CONFLICT);
		}
		this.boardMapper.insert(vo);
		return vo.getNo();
	}
	
	@Override
	public int update(BoardVo vo) {		
		return this.boardMapper.update(vo);
	}
	
	@Override
	public int delete(int boardNo) {
		return this.boardMapper.delete(new BoardVo(boardNo));
	}
	
	@Override
	public List<?> upsert(List<BoardVo> list) {
		if(list==null || list.isEmpty()) {return null;}
		
		List<Integer> createPkList = new ArrayList<>();
		for(BoardVo vo:list) {
			log.debug(">> vo.getNo():{}",vo.getNo());
			if(this.isExist(new BoardVo(vo.getNo()))) {this.update(vo);}
			else {createPkList.add(this.create(vo));}
		}
				
		return createPkList;
	}

	@Override
	public void processTx(List<ConsumerRecord<String, String>> list) {
        for(int i=0;i<list.size();i++) {
        	ConsumerRecord<String,String> record = list.get(i);
        	
	        String method=BaseUtil.getKafkaUtil().extract(record.headers(),"method");

	    	
	    	if(method.equals("upsert")) {
	    		BoardInDto inDto = BaseUtil.getKafkaUtil().readValue(record.value(), BoardInDto.class);
		        log.debug("boardInDto:{}",inDto);        
		        
				List<BoardVo> temp = new ArrayList<>();
				temp.add(inDto.getBoard1());
				temp.add(inDto.getBoard2());
		        this.upsert(temp);
		        
	    	}else {
		        BoardVo boardVo = BaseUtil.getKafkaUtil().readValue(record.value(), BoardVo.class);
		        log.debug("boardVo:{}",boardVo);        
		        
		        if(method==null || method.equals("")) {return;}
		        else if(method.equals("create")) {this.create(boardVo);}
		        else if(method.equals("update")) {this.update(boardVo);}
		        else if(method.equals("delete")) {this.delete(boardVo.getNo());}
	    	}	    	
        }
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean isExist(BoardVo vo) {
		List<BoardVo> list = this.getList(vo);
		return (list==null || list.isEmpty())?false:true;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
