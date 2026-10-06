package com.alpha.base.support.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.json.JsonSanitizer;

public final class ObjectMapperUtilPack {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface ObjectMapperUtil {
				
		public ObjectMapper getMapper();
		
		public <T> T convert(Object obj,Class<T> type);
		public <T> T convert(Object obj,TypeReference<T> typeReference);
		
		public <T> List<T> convertList(List<?> list, Class<T> type);
		
		public <T> T readValue(String content, Class<T> type) throws Exception;
		public <T> T readValue(String content, TypeReference<T> typeReference) throws Exception;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static ObjectMapperUtil getObjectMapperUtil() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.setTimeZone(Calendar.getInstance().getTimeZone());		
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);	
		mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
		
		return new ObjectMapperUtil() {

			@Override
			public ObjectMapper getMapper() {
				return mapper;
			}

			@Override
			public <T> T convert(Object obj,Class<T> type) {
				if(obj==null) {return null;}
				return this.getMapper().convertValue(obj, type);
			}

			@Override
			public <T> T convert(Object obj,TypeReference<T> typeReference) {
				if(obj==null) {return null;}
				return this.getMapper().convertValue(obj, typeReference);
			}

			@Override
			public <T> List<T> convertList(List<?> list, Class<T> type) {
				if(list==null) {return null;}
				List<T> result =new ArrayList<>();
				for(int i=0;i<list.size();i++) {result.add(this.getMapper().convertValue(list.get(i), type));}
				return result;
			}

			@Override
			public <T> T readValue(String content, Class<T> type) throws Exception {
				//To prevent server-side JSON injections, sanitize all data before serializing it to JSON
				return this.getMapper().readValue(JsonSanitizer.sanitize(content), type);
			}
			
			@Override
			public <T> T readValue(String content, TypeReference<T> typeReference) throws Exception {
				//To prevent server-side JSON injections, sanitize all data before serializing it to JSON
				return this.getMapper().readValue(JsonSanitizer.sanitize(content), typeReference);
			}
			
		};
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}