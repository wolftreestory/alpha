package com.alpha.base.support.repository;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import com.alpha.base.support.util.RedisUtilPack.RedisObjectRepository;
import com.alpha.base.support.util.RedisUtilPack.RedisOpsRepository;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

//@Slf4j
@Repository(RedisOpsRepository.REPO_OBJECT)
@ConditionalOnExpression("'${spring.redis.enabled:false}'.equals('true')")
public class RedisDefaultObjectRepository implements RedisObjectRepository {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private ObjectMapper mapper;
	
	private RedisTemplate<String, Object> redisTemplate;
		
	private ValueOperations<String, Object> opsForObject;

	public RedisDefaultObjectRepository(RedisTemplate<String, Object> redisTemplate) {		
		this.redisTemplate = redisTemplate;
		this.opsForObject = redisTemplate.opsForValue();
		//log.info(">> redis-opsForObject:{},{}",redisTemplate.hashCode(),this.opsForObject.hashCode());	
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public RedisTemplate<String, Object> getRedisTemplate() {
		return this.redisTemplate;
	}
	
	@Override
	public RedisOperations<String, ?> getRedisOperations() {
		return this.opsForObject.getOperations();
	}

	@Override
	public void put(String redisKey, Object value) {
		this.opsForObject.set(this.getGlobalRedisKey(redisKey), value);
	}
	
	@Override
	public void put(String redisKey, Object value, Duration duration) {
		this.opsForObject.set(this.getGlobalRedisKey(redisKey), value, duration);
	}

	@Override
	public Object get(String redisKey) {
		Object obj = this.opsForObject.get(this.getGlobalRedisKey(redisKey));	
		return (obj==null || obj.equals("null"))?null:obj;
	}

	@Override
	public <T> T get(String redisKey, Class<T> clazz) {
		try {
			return this.getObjectMapper().convertValue(this.get(redisKey), clazz);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	
	@Override
	public <T> T get(String redisKey, TypeReference<T> typeReference) {
		try {
			return this.getObjectMapper().convertValue(this.get(redisKey), typeReference);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	private synchronized ObjectMapper getObjectMapper() {
		if(this.mapper == null) {
			mapper = new ObjectMapper();
			mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
			mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);	
			mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);	
		}
		return this.mapper;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}