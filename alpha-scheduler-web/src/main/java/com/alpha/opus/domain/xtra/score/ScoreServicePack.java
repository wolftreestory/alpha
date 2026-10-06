package com.alpha.opus.domain.xtra.score;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;

import com.alpha.base.advice.ExceptionAdvice;
import com.alpha.base.exception.BusinessException;
import com.alpha.opus.domain.xtra.score.mapper.ScoreMapper;
import com.alpha.opus.domain.xtra.score.vo.ScoreVo;

//@Slf4j
@Configuration
@Profile({"local","dev"})
public class ScoreServicePack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface ScoreService {		
	    public ScoreVo get(String name);
	    public int create(ScoreVo vo);
	    public int update(ScoreVo vo);
	    public int delete(int no);	    
	    public int deleteAll();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    ScoreService getScoreService(ScoreMapper mapper) {

    	return new ScoreService() {

			@Override
			public ScoreVo get(String name) {
				return mapper.select(name);
			}

			@Override
			public int create(ScoreVo vo) {
    			if(this.isExist(vo.getName())) {
    				throw new BusinessException(()->ExceptionAdvice.message("biz.board.001"),HttpStatus.CONFLICT);
    			}
    			mapper.insert(vo);
    			return vo.getNo();
			}

    		@Override
    		public int update(ScoreVo vo) {		
    			return mapper.update(vo);
    		}
    		
    		@Override
    		public int delete(int no) {
    			return mapper.delete(new ScoreVo(no));
    		}
    		
    		@Override
    		public int deleteAll() {
    			return mapper.deleteAll();
    		}

    		private boolean isExist(String name) {
    			ScoreVo vo = this.get(name);
    			return (vo==null)?false:true;
    		}			
    	};
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}