package com.alpha.xtra.subscriber;

import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.alpha.base.BaseUtil;
import com.alpha.base.support.util.KafkaUtilPack.KafkaUtil;
import com.alpha.xtra.service.BoardService;
import com.alpha.xtra.vo.BoardVoPack.BoardInDto;
import com.alpha.xtra.vo.BoardVoPack.BoardVo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name="spring.kafka.enabled", havingValue="true", matchIfMissing=false)
public class BoardKafkaSubscriber {

	private BoardService boardService;
	
	public BoardKafkaSubscriber(BoardService boardService) {
		this.boardService = boardService;
	}
	
	public static String getTopic() {return "testTopic";}
	public static String getGroupId() {return "testGroup";}
	
	@KafkaListener(topics="#{T(com.alpha.xtra.subscriber.BoardKafkaSubscriber).getTopic()}", groupId="#{T(com.alpha.xtra.subscriber.BoardKafkaSubscriber).getGroupId()}", containerFactory="kafkaListenerContainerFactoryBoard")
	//@KafkaListener(topics="testTopic", groupId="testGroup", containerFactory="kafkaListenerContainerFactoryBoard")
    public void handler1(ConsumerRecord<String,String> record) throws Exception {
        log.debug("Kafka Consumer handler1");
        
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
	
	//@KafkaListener(topics="testTopic", groupId="testGroup", containerFactory="kafkaListenerContainerFactoryBoard")
    public void handler2(@Payload String payload, @Headers MessageHeaders headers) throws Exception {
        log.debug("Kafka Consumer handler2");

        log.debug("topic:{}",headers.get(KafkaHeaders.RECEIVED_TOPIC,String.class));
        log.debug("key:{}",headers.get(KafkaHeaders.RECEIVED_KEY,String.class)); 
        log.debug("headers:{}",BaseUtil.getKafkaUtil().convertMap(headers));
        log.debug("payload:{}",payload);
        
        log.debug("partition:{}",headers.get(KafkaHeaders.RECEIVED_PARTITION));                
        log.debug("offset:{}",headers.get(KafkaHeaders.OFFSET));
        log.debug("timestamp:{}",BaseUtil.getKafkaUtil().convertFormatDate(headers.get(KafkaHeaders.RECEIVED_TIMESTAMP,Long.class)));
        
        BoardVo boardVo = BaseUtil.getKafkaUtil().readValue(payload, BoardVo.class);
        log.debug("boardVo:{}",boardVo);
    }

	//@KafkaListener(topics="testTopic", groupId="testGroup", containerFactory="kafkaListenerContainerFactoryBoard")
    public void handler3(@Payload String payload,
    		@Header(KafkaHeaders.OFFSET) Long offset,
            @Header(KafkaHeaders.CONSUMER) KafkaConsumer<String, String> consumer,
            @Header(KafkaHeaders.TIMESTAMP_TYPE) String timestampType,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partitionId,
            @Header(KafkaHeaders.RECEIVED_KEY) @Nullable String messageKey,
            @Header(KafkaHeaders.RECEIVED_TIMESTAMP) Long timestamp,
            @Header(KafkaUtil.DEFAULT_HEADER_KEY_UUID) @Nullable String uuid,
            @Header(KafkaUtil.DEFAULT_HEADER_KEY_SERVICE_NAME) @Nullable String serviceName,
            @Header(KafkaUtil.DEFAULT_HEADER_KEY_SERVER_IP) @Nullable String serviceIp) throws Exception {
		
        log.debug("Kafka Consumer handler3");

        log.debug("consumer:{}",consumer);

        log.debug("topic:{}",topic);
        log.debug("key:{}",messageKey); 
		log.debug("DEFAULT_HEADER_KEY_UUID:{}",uuid);
		log.debug("DEFAULT_HEADER_KEY_SERVICE_NAME:{}",serviceName);
		log.debug("DEFAULT_HEADER_KEY_SERVER_IP:{}",serviceIp);
		log.debug("payload:{}",payload);
		
		log.debug("partition:{}",partitionId);                
		log.debug("offset:{}",offset);
		log.debug("timestamp:{}",BaseUtil.getKafkaUtil().convertFormatDate(timestamp));
        
        BoardVo boardVo = BaseUtil.getKafkaUtil().readValue(payload, BoardVo.class);
        log.debug("boardVo:{}",boardVo);
    }

}