package com.alpha.base.support.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.alpha.base.support.util.RedisUtilPack.RedisListRepository;
import com.alpha.base.support.util.RedisUtilPack.RedisOpsRepository;

//@Slf4j
@Repository(RedisOpsRepository.REPO_LIST)
@ConditionalOnExpression("'${spring.redis.enabled:false}'.equals('true')")
public class RedisDefaultListRepository implements RedisListRepository {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private RedisTemplate<String, Object> redisTemplate;
	
	private ListOperations<String, Object> opsForList;

	public RedisDefaultListRepository(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.opsForList = redisTemplate.opsForList();
		//log.info(">> redis-opsForList:{},{}",redisTemplate.hashCode(),this.opsForList.hashCode());		
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
    public RedisTemplate<String, Object> getRedisTemplate(){
    	return this.redisTemplate;
    }
	
	@Override
	public RedisOperations<String, ?> getRedisOperations() {
		return this.opsForList.getOperations();
	}

	@Override
	public Long size(String redisKey) {
		return this.opsForList.size(this.getGlobalRedisKey(redisKey));
	}

	@Override
	public void set(String redisKey, long index, Object value) {
		this.opsForList.set(this.getGlobalRedisKey(redisKey), index, value);
	}

	@Override
	public void trim(String redisKey, long start, long end) {
		this.opsForList.trim(this.getGlobalRedisKey(redisKey), start, end);
	}

	@Override
	public Long remove(String redisKey, long index, Object value) {
		return this.opsForList.remove(this.getGlobalRedisKey(redisKey), index, value);
	}

	@Override
	public List<Object> get(String redisKey) {		
		return this.range(redisKey, 0, this.size(redisKey));
	}

	@Override
	public List<Object> range(String redisKey, long start, long end) {
		return this.opsForList.range(this.getGlobalRedisKey(redisKey), start, end);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public Object leftPop(String redisKey) {	
		Object obj = this.opsForList.leftPop(this.getGlobalRedisKey(redisKey));
		return (obj==null || obj.equals("null"))?null:obj;
	}

	@Override
	public Long leftPush(String redisKey, Object value) {		
		return this.opsForList.leftPush(this.getGlobalRedisKey(redisKey), value);
	}

	@Override
	public Long leftPushAll(String redisKey, Collection<Object> collection) {
		return this.opsForList.leftPushAll(this.getGlobalRedisKey(redisKey), collection);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public Object rightPop(String redisKey) {
		Object obj = this.opsForList.rightPop(this.getGlobalRedisKey(redisKey));
		return (obj==null || obj.equals("null"))?null:obj;
	}

	@Override
	public Long rightPush(String redisKey, Object value) {
		return this.opsForList.rightPush(this.getGlobalRedisKey(redisKey), value);
	}

	@Override
	public Long rightPushAll(String redisKey, Collection<Object> collection) {
		return this.opsForList.rightPushAll(this.getGlobalRedisKey(redisKey), collection);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
}