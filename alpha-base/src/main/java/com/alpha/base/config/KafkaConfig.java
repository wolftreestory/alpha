package com.alpha.base.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.alpha.base.support.AbstractMeta;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableKafka
@Configuration
@ConditionalOnExpression("T(com.alpha.base.KafkaConfig).isEnabled()")
@ConditionalOnProperty(name="spring.kafka.enabled", havingValue="true", matchIfMissing=false)
public class KafkaConfig extends AbstractMeta {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Component
	@ConfigurationProperties(prefix="spring.kafka")
	@Data
	public static class KafkaProperties{
		private boolean enabled;
		private String applicationName;
		private String bootstrapServers;	
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Autowired
	private KafkaProperties properties;

    @Bean
    KafkaPublisher<String> kafkaPublisherString() {
		return new KafkaPublisher<String>() {
			@Override
			public void doSend(String topic, String key, String payload, Map<String, String> header, ListenableFutureCallback<? super SendResult<String, String>> callback) {
				this.kafkaTemplateStringValue().send(this.makeProducerRecord(topic, key, payload, header)).addCallback(callback!=null?callback:this.callback());
			}
		};
	}

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.properties.getBootstrapServers());
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, "10485760"); // 50 * 1024 * 1024

        ConcurrentKafkaListenerContainerFactory<String,String> factory = new ConcurrentKafkaListenerContainerFactory<>();
	    factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(configs));	    

	    factory.setRecordFilterStrategy(new RecordFilterStrategy<String,String>() {
	    	@Override
	    	public boolean filter(ConsumerRecord<String,String> record) {
	    		log.debug(">> kafkaListenerContainerFactory:filter");
	    		return false;
	    	}
	    });      
	    factory.setRecordInterceptor(new RecordInterceptor<String,String>(){
			@Override
			public ConsumerRecord<String,String> intercept(ConsumerRecord<String,String> record) {
				log.debug(">> kafkaListenerContainerFactory:intercept");
				return record;
			}
	    });
	    
        return factory;
    }	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static class KafkaTemplateContext {
    	private static Map<Object,KafkaTemplate<String,?>> context = new HashMap<>();

    	public static boolean isExist(Object obj){
    		return context.containsKey(obj);
    	}
    	
    	public static KafkaTemplate<String,?> getKafkaTemplate(Object obj){
    		if(!isExist(obj)) {return null;}
    		KafkaTemplate<String,?> kafkaTemplate = context.get(obj);
    		log.debug(">> obj.hashCode():{},kafkaTemplate.hashCode():{}",obj.hashCode(),kafkaTemplate.hashCode());
    		return kafkaTemplate;    		
    	}
    	
    	public static synchronized void setKafkaTemplate(Object obj,KafkaTemplate<String,?> kafkaTemplate){
			context.put(obj, kafkaTemplate);
    	}
    	
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface KafkaPublisher<T> {
	    public void doSend(String topic, String key, T payload, Map<String, String> header, ListenableFutureCallback<? super SendResult<String, T>> callback);

	    default public void send(String topic, T payload) {this.doSend(topic, null, payload, null, null);}		    
	    default public void send(String topic, T payload, Map<String,String> header) {this.doSend(topic, null, payload, header, null);} 		    
	    default public void send(String topic, T payload, ListenableFutureCallback<? super SendResult<String, T>> callback) {this.doSend(topic, null, payload, null, callback);}
	    default public void send(String topic, T payload, Map<String, String> header, ListenableFutureCallback<? super SendResult<String, T>> callback) {this.doSend(topic, null, payload, header, callback);}

	    default public void send(String topic, String key, T payload) {this.doSend(topic, key, payload, null, null);}			
	    default public void send(String topic, String key, T payload, Map<String,String> header) {this.doSend(topic, key, payload, header, null);} 			
	    default public void send(String topic, String key, T payload, ListenableFutureCallback<? super SendResult<String, T>> callback) {this.doSend(topic, key, payload, null, callback);}	    
	    default public void send(String topic, String key, T payload, Map<String,String> header, ListenableFutureCallback<? super SendResult<String, T>> callback) {this.doSend(topic, key, payload, header, callback);}

	    default public Map<String,String> addDefaultHeader(Map<String,String> header) {
	    	return MetaUtil.getKafkaUtil().addDefault(header);
	    }
	    
//	    default public KafkaProducer<String, T> kafkaProducerJsonValue(){
//	    	KafkaProducer<String, T> producer = new KafkaProducer<>(this.getPublisherConfigMap(JsonSerializer.class));
//	    	return producer;
//	    }
//
//	    default public KafkaProducer<String, String> kafkaProducerStringValue(){
//	    	KafkaProducer<String, String> producer = new KafkaProducer<>(this.getPublisherConfigMap(StringSerializer.class));
//	    	return producer;
//	    }

	    @SuppressWarnings("unchecked")
		default public KafkaTemplate<String,T> kafkaTemplateJsonValue(){
	    	if(!KafkaTemplateContext.isExist(this)) {
		    	ProducerFactory<String,T> producerFactory = new DefaultKafkaProducerFactory<>(this.getPublisherConfigMap(JsonSerializer.class));
		    	KafkaTemplate<String,T> kafkaTemplate = new KafkaTemplate<>(producerFactory);
		    	KafkaTemplateContext.setKafkaTemplate(this,kafkaTemplate);
		    	return kafkaTemplate;
	    	}
	    	return (KafkaTemplate<String, T>) KafkaTemplateContext.getKafkaTemplate(this);
	    }
	    
	    @SuppressWarnings("unchecked")
	    default public KafkaTemplate<String,String> kafkaTemplateStringValue(){
	    	if(!KafkaTemplateContext.isExist(this)) {
	    		ProducerFactory<String,String> producerFactory = new DefaultKafkaProducerFactory<>(this.getPublisherConfigMap(StringSerializer.class));
		    	KafkaTemplate<String,String> kafkaTemplate = new KafkaTemplate<>(producerFactory);
		    	KafkaTemplateContext.setKafkaTemplate(this,kafkaTemplate);
		    	return kafkaTemplate;
	    	}
	    	return (KafkaTemplate<String, String>) KafkaTemplateContext.getKafkaTemplate(this);
	    }	
	    
	    default public ProducerRecord<String,T> makeProducerRecord(String topic, String key, T payload, Map<String, String> header){
			ProducerRecord<String,T> producerRecord = StringUtils.isBlank(key)?new ProducerRecord<>(topic,payload):new ProducerRecord<>(topic,key,payload);		 				
			this.addDefaultHeader(header).forEach((k,v)->producerRecord.headers().add(new RecordHeader(k,v.getBytes())));
			return producerRecord;
	    }

	    default public Map<String, Object> getPublisherConfigMap(Object valueClass){
			Map<String, Object> configMap = new HashMap<>();
			
			configMap.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, MetaUtil.getKafkaUtil().getBootstrapServers());					
			configMap.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);			
			configMap.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueClass);			
			configMap.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);
			configMap.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, "10485760");
			configMap.put(ProducerConfig.ACKS_CONFIG,"1"); // ack=0, ack=1, ack=all
			//configMap.put(ProducerConfig.RETRIES_CONFIG,  1);
			//configMap.put(ProducerConfig.BATCH_SIZE_CONFIG, 20000);
			//configMap.put(ProducerConfig.LINGER_MS_CONFIG, 1);
			//configMap.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 24568545);	
			return configMap;
	    }

	    default public ListenableFutureCallback<SendResult<String,T>> callback(){
			return new ListenableFutureCallback<SendResult<String,T>>() {
	            @Override
	            public void onSuccess(SendResult<String, T> result) {
	                log.debug(">> topic:{}",result.getProducerRecord().topic());
	                log.debug(">> key:{}",result.getProducerRecord().key());
	                //log.debug("headers:{}",result.getProducerRecord().headers());
	                log.debug(">> headers:{}",MetaUtil.getKafkaUtil().convertMap(result.getProducerRecord().headers()));
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
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}


