package com.alpha.base.support.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;

import com.alpha.base.config.KafkaConfig.KafkaProperties;
import com.alpha.base.support.util.JsonUtilPack.JsonUtil;
import com.alpha.base.support.util.ObjectMapperUtilPack.ObjectMapperUtil;
import com.alpha.base.support.util.SystemUtilPack.SystemUtil;
import com.alpha.base.support.util.TimeUtilPack.TimeUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Builder;
import lombok.Data;

//@Slf4j
public class KafkaUtilPack {
	
	public interface KafkaUtil {
		
		public static final String OFFSET = KafkaHeaders.OFFSET;
		public static final String CONSUMER = KafkaHeaders.CONSUMER;
		public static final String TIMESTAMP_TYPE = KafkaHeaders.TIMESTAMP_TYPE;
		public static final String RECEIVED_TOPIC = KafkaHeaders.RECEIVED_TOPIC;
		public static final String RECEIVED_PARTITION = KafkaHeaders.RECEIVED_PARTITION;
		public static final String RECEIVED_KEY = KafkaHeaders.RECEIVED_KEY;
		public static final String RECEIVED_TIMESTAMP = KafkaHeaders.RECEIVED_TIMESTAMP;

		public static final String KAFKA_PREFIX = "kafka_";
		public static final String DEFAULT_PREFIX = "x-alpha-";
		public static final String DEFAULT_HEADER_KEY_UUID = DEFAULT_PREFIX+"uuid";	
		public static final String DEFAULT_HEADER_KEY_SERVICE_NAME = DEFAULT_PREFIX+"publisherService";
		public static final String DEFAULT_HEADER_KEY_SERVER_IP = DEFAULT_PREFIX+"publisherServerIp";	
        
		public String getBootstrapServers();
		
		public Map<String,String> buildHeader();	
		public Map<String,String> addDefault(Map<String,String> header);		
		public Map<String,String> convertMap(Headers headers);
		public Map<String,String> convertMap(MessageHeaders headers);
		public String extract(Headers headers,String key);
		public String extract(MessageHeaders headers,String key);
		public String convertFormatDate(long timestamp);

		public String toJson(Object payload);
		public <T> T readValue(String payload, Class<T> type);
		public <T> T readValue(String content, TypeReference<T> valueTypeRef);		
	}

	@Data @Builder
	public static class KafkaMeta {
		private String applicationName;
		private KafkaProperties properties;
		private ObjectMapperUtil objectMapperUtil;
		private JsonUtil jsonUtil;
		private TimeUtil timeUtil;
		private SystemUtil systemUtil;
	}
	
	public static KafkaUtil getKafkaUtil(KafkaMeta meta) {
		
		return new KafkaUtil() {

			@Override
			public String getBootstrapServers() {return meta.getProperties().getBootstrapServers();}
			
			@Override
			public Map<String, String> buildHeader() {
				Map<String,String> map = new HashMap<>();
				map.put(DEFAULT_HEADER_KEY_UUID, UUID.randomUUID().toString());
				map.put(DEFAULT_HEADER_KEY_SERVICE_NAME, meta.getApplicationName());
				map.put(DEFAULT_HEADER_KEY_SERVER_IP, meta.getSystemUtil().getServerIp());				
				return map;
			}
						
			@Override
			public Map<String,String> addDefault(Map<String,String> header){
				if(header==null) {return this.buildHeader();}
				Map<String,String> map = new HashMap<>(header);	
				for(String key:map.keySet()) {
					if(key.equals(DEFAULT_HEADER_KEY_UUID)||key.equals(DEFAULT_HEADER_KEY_SERVICE_NAME)||key.equals(DEFAULT_HEADER_KEY_SERVER_IP)) {continue;}
					if(key.startsWith(KAFKA_PREFIX) || key.startsWith(DEFAULT_PREFIX)) {throw new RuntimeException("header key can't start with ["+KAFKA_PREFIX+","+DEFAULT_PREFIX+"]");}
				}			
				if(!header.containsKey(DEFAULT_HEADER_KEY_UUID)) {map.put(DEFAULT_HEADER_KEY_UUID, UUID.randomUUID().toString());}
				if(!header.containsKey(DEFAULT_HEADER_KEY_SERVICE_NAME)) {map.put(DEFAULT_HEADER_KEY_SERVICE_NAME, meta.getApplicationName());}
				if(!header.containsKey(DEFAULT_HEADER_KEY_SERVER_IP)) {map.put(DEFAULT_HEADER_KEY_SERVER_IP, meta.getSystemUtil().getServerIp());}
				return header;
			}	

			@Override
			public Map<String, String> convertMap(Headers headers) {
				if(headers==null) {return null;}
                Map<String,String> map = new HashMap<>();                
                headers.forEach(h->map.put(h.key(),new String(h.value())));
				return map;
			}
			
			@Override
			public Map<String,String> convertMap(MessageHeaders headers){
				if(headers==null) {return null;}
		        Map<String,String> map = new HashMap<>();
		        headers.keySet().forEach(key->{
		        	Object value = headers.get(key);
		        	map.put(key, key.startsWith(KAFKA_PREFIX)?String.valueOf(value):new String((byte[])value));		        	
		        });		        
				return map;				
			}
			
			@Override
			public String extract(Headers headers,String key) {
				if(headers==null) {return null;}
				try{headers.forEach(h->{if(h.key().equals(key)) {throw new RuntimeException(new String((byte[])h.value()));}});}
				catch(Exception e) {return e.getMessage();}
				return null;
			}
			
			@Override
			public String extract(MessageHeaders headers,String key) {
				boolean isEnable=true;
				if(isEnable && headers==null) {isEnable=false;}
				if(isEnable && (key==null || key.equals(""))) {isEnable=false;}	
				if(isEnable && !headers.containsKey(key)) {isEnable=false;}
				return isEnable?new String(headers.get(key,byte[].class)):null;
			}

			@Override 
			public String convertFormatDate(long timestamp) {return meta.getTimeUtil().getFormatDate(timestamp,"yyyy-MM-dd HH:mm:ss.SSS");}

			@Override
			public String toJson(Object obj) {
				return meta.getJsonUtil().toJson(obj);
			}

			@Override
			public <T> T readValue(String payload, Class<T> type) {
				try{return meta.getObjectMapperUtil().getMapper().readValue(payload, type);}
				catch(Exception e){throw new RuntimeException(e.getCause());}
			}

			@Override
			public <T> T readValue(String payload,TypeReference<T> valueTypeRef) {
				try{return meta.getObjectMapperUtil().getMapper().readValue(payload, valueTypeRef);}
				catch(Exception e){throw new RuntimeException(e.getCause());}
			}
		};
	};

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	
}