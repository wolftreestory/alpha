package com.alpha.opus.domain.xtra.board;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;

import com.alpha.base.advice.ExceptionAdvice;
import com.alpha.base.exception.BusinessException;
import com.alpha.opus.domain.xtra.board.mapper.BoardMapper;
import com.alpha.opus.domain.xtra.board.vo.BoardVo;

//@Slf4j
@Configuration
@Profile({"local","dev"})
public class BoardServicePack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static interface BoardService {		
	    public List<BoardVo> getList(BoardVo vo);	    
	    public List<BoardVo> getList(List<?> foreach);	    
	    public BoardVo get(int boardNo);
	    public BoardVo getBoardInfo(int boardNo);
	    public int create(BoardVo vo);
	    public int update(BoardVo vo);
	    public int delete(int boardNo);
	    public int deleteAll();
	    public List<?> upsert(List<BoardVo> list);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    BoardService getBoardService(BoardMapper mapper) {

    	return new BoardService() {

    		@Override
    		public List<BoardVo> getList(BoardVo vo) {
    			//int totalCount = mapper.selectTotalCount(vo,null);
    			return mapper.select(vo,null);
    		}
    		
    		@Override
    		public List<BoardVo> getList(List<?> foreach){
    			//int totalCount = mapper.selectTotalCount(null,foreach);
    			return mapper.select(null,foreach);
    		}
    		    
    		@Override
    		public BoardVo get(int boardNo) {
    			List<BoardVo> list = this.getList(new BoardVo(boardNo));
    			return (list==null || list.isEmpty())?null:list.get(0);
    		}
    		
    		@Override
    	    public BoardVo getBoardInfo(int boardNo) {
    			List<BoardVo> list = mapper.select(new BoardVo(boardNo),null);
    			return (list==null || list.isEmpty())?null:list.get(0);
    		}
    		
    		@Override
    		public int create(BoardVo vo) {	
    			if(this.isExist(vo)) {
    				throw new BusinessException(()->ExceptionAdvice.message("biz.board.001"),HttpStatus.CONFLICT);
    			}
    			mapper.insert(vo);
    			return vo.getNo();
    		}
    		
    		@Override
    		public int update(BoardVo vo) {		
    			return mapper.update(vo);
    		}
    		
    		@Override
    		public int delete(int boardNo) {
    			return mapper.delete(new BoardVo(boardNo));
    		}
    		
    		@Override
    		public int deleteAll() {
    			return mapper.deleteAll();
    		}
    		
    		@Override
    		public List<?> upsert(List<BoardVo> list) {
    			if(list==null || list.isEmpty()) {return null;}
    			
    			List<Integer> createPkList = new ArrayList<>();
    			for(BoardVo vo:list) {
    				if(this.isExist(new BoardVo(vo.getNo()))) {this.update(vo);}
    				else {createPkList.add(this.create(vo));}
    			}
    					
    			return createPkList;
    		}

    		private boolean isExist(BoardVo vo) {
    			List<BoardVo> list = this.getList(vo);
    			return (list==null || list.isEmpty())?false:true;
    		}
    	};
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}