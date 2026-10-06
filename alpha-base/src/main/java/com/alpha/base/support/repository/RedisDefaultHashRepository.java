package com.alpha.base.support.repository;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.alpha.base.support.util.RedisUtilPack.RedisHashRepository;
import com.alpha.base.support.util.RedisUtilPack.RedisOpsRepository;

//@Slf4j
@Repository(RedisOpsRepository.REPO_HASH)
@ConditionalOnExpression("'${spring.redis.enabled:false}'.equals('true')")
public class RedisDefaultHashRepository implements RedisHashRepository {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private RedisTemplate<String, Object> redisTemplate;
	
	private HashOperations<String, String, Object> opsForHash;

	public RedisDefaultHashRepository(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.opsForHash = redisTemplate.opsForHash();
		//log.info(">> redis-opsForHash:{},{}",redisTemplate.hashCode(),this.opsForHash.hashCode());		
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
    public RedisTemplate<String, Object> getRedisTemplate(){
    	return this.redisTemplate;
    }
	
	@Override
	public RedisOperations<String, ?> getRedisOperations() {
		return this.opsForHash.getOperations();
	}

	@Override
	public Map<String, Object> get(String redisKey) {
		return this.opsForHash.entries(this.getGlobalRedisKey(redisKey));
	}

	@Override
	public Object get(String redisKey, String valueKey) {
		Object obj = this.opsForHash.get(this.getGlobalRedisKey(redisKey), valueKey);
		return (obj==null || obj.equals("null"))?null:obj;
	}

	@Override
	public void put(String redisKey, String valueKey, Object value) {
		this.opsForHash.put(this.getGlobalRedisKey(redisKey), valueKey, value);
	}

	@Override
	public Long delete(String redisKey, String valueKey) {
		return this.opsForHash.delete(this.getGlobalRedisKey(redisKey), valueKey);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


}