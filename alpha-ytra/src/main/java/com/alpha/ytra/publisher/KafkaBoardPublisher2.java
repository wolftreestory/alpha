package com.alpha.ytra.publisher;


import java.util.Map;

import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.alpha.base.BaseUtil;
import com.alpha.base.config.KafkaConfig.KafkaPublisher;
import com.alpha.ytra.vo.BoardVoPack.BoardInDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaBoardPublisher2 implements KafkaPublisher<BoardInDto> {

	@Override
	public void doSend(String topic, String key, BoardInDto payload, Map<String, String> header, ListenableFutureCallback<? super SendResult<String, BoardInDto>> callback) {
		this.kafkaTemplateJsonValue().send(this.makeProducerRecord(topic, key, payload, header)).addCallback(callback==null?this.callback():callback);
	}
	
	@Override
	public ListenableFutureCallback<SendResult<String,BoardInDto>> callback(){
		return new ListenableFutureCallback<SendResult<String,BoardInDto>>() {
            @Override
            public void onSuccess(SendResult<String, BoardInDto> result) {
                log.debug(">> topic:{}",result.getProducerRecord().topic());
                log.debug(">> key:{}",result.getProducerRecord().key());
                //log.debug("headers:{}",result.getProducerRecord().headers());
                log.debug(">> headers:{}",BaseUtil.getKafkaUtil().convertMap(result.getProducerRecord().headers()));
                log.debug(">> payload:{}",result.getProducerRecord().value());	                
                
                log.debug(">> partition:{}",result.getRecordMetadata().partition());                
                log.debug(">> offset:{}",result.getRecordMetadata().offset());
                log.debug(">> timestamp:{}",result.getRecordMetadata().timestamp());           
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("error:{}",ex.getMessage());
            }
		};
	}
}