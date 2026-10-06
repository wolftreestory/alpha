package com.alpha.xtra.subscriber;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(name="spring.kafka.enabled", havingValue="true", matchIfMissing=false)
public class BoardKafkaBatchListenerConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;
	
	@Bean
	ConcurrentKafkaListenerContainerFactory<String, String> kafkaBatchListenerContainerFactoryBoard() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        configs.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024*1024);
        configs.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 10000); //MAX_POLL_RECORDS 또는 FETCH_MIN_BYTES 가 채워지지 않았다면 최대 10초를 기다림

        ConcurrentKafkaListenerContainerFactory<String,String> factory = new ConcurrentKafkaListenerContainerFactory<>();
	    factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(configs));	
	    
	    factory.setBatchListener(true);
	    
	    factory.setRecordFilterStrategy(new RecordFilterStrategy<String, String>() {
			@Override public boolean filter(ConsumerRecord<String, String> record) {
				log.debug("kafkaBatchListenerContainerFactoryBoard:filter");
				boolean isFilter=true;
				if(record.key().equals("kafka-key1")) {isFilter=false;}
				return isFilter;
			}	    	
	    });
	    
	    factory.setRecordInterceptor(new RecordInterceptor<String, String>(){
			@Override public ConsumerRecord<String, String> intercept(ConsumerRecord<String, String> record) {
				log.debug("kafkaBatchListenerContainerFactoryBoard:intercept");
				return record;
			}	    	
	    });
	    
	    return factory;
	}
}
