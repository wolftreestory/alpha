package com.alpha.xtra.subscriber;

import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.alpha.base.BaseUtil;
import com.alpha.xtra.service.BoardService;
import com.alpha.xtra.vo.BoardVoPack.BoardInDto;
import com.alpha.xtra.vo.BoardVoPack.BoardVo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name="spring.kafka.enabled", havingValue="true", matchIfMissing=false)
public class BoardKafkaBatchSubscriber {

	private BoardService boardService;

	public BoardKafkaBatchSubscriber(BoardService boardService) {
		this.boardService = boardService;
	}
	
	@KafkaListener(topics="testBatchTopic", groupId="testGroup", containerFactory="kafkaBatchListenerContainerFactoryBoard")
    public void batchHandler1(List<ConsumerRecord<String,String>> recordList) throws Exception {
        log.debug("KafkaBatch Consumer handler1");
        log.debug("recordList size:{}",recordList.size());
        
        for(int i=0;i<recordList.size();i++) {
        	ConsumerRecord<String,String> record = recordList.get(i);
        	
	        log.debug("topic:{}",record.topic());
	        log.debug("key:{}",record.key());        
	        log.debug("headers:{}",BaseUtil.getKafkaUtil().convertMap(record.headers()));
	        log.debug("payload:{}", record.value());  
	
	        log.debug("partition:{}",record.partition());                
	        log.debug("offset:{}",record.offset());
	        log.debug("timestamp:{}",BaseUtil.getKafkaUtil().convertFormatDate(record.timestamp()));  
	
	        String method=BaseUtil.getKafkaUtil().extract(record.headers(),"method");
	    	log.debug(">> method:{}",method);

	    	if(method.equals("upsert")) {
	    		BoardInDto inDto = BaseUtil.getKafkaUtil().readValue(record.value(), BoardInDto.class);
		        log.debug("boardInDto:{}",inDto);        
		        
				List<BoardVo> list = new ArrayList<>();
				list.add(inDto.getBoard1());
				list.add(inDto.getBoard2());
		        this.boardService.upsert(list);
	    	}else {
		        BoardVo boardVo = BaseUtil.getKafkaUtil().readValue(record.value(), BoardVo.class);
		        log.debug("boardVo:{}",boardVo);        
		        
		        if(method==null || method.equals("")) {return;}
		        else if(method.equals("create")) {this.boardService.create(boardVo);}
		        else if(method.equals("update")) {this.boardService.update(boardVo);}
		        else if(method.equals("delete")) {this.boardService.delete(boardVo.getNo());}
	    	}	    	
        }
	}
	
}