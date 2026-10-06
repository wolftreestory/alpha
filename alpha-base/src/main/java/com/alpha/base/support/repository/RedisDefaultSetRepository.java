package com.alpha.base.support.repository;

import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Repository;

import com.alpha.base.support.util.RedisUtilPack.RedisOpsRepository;
import com.alpha.base.support.util.RedisUtilPack.RedisSetRepository;

//@Slf4j
@Repository(RedisOpsRepository.REPO_SET)
@ConditionalOnExpression("'${spring.redis.enabled:false}'.equals('true')")
public class RedisDefaultSetRepository implements RedisSetRepository {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private RedisTemplate<String, Object> redisTemplate;
	
	private SetOperations<String, Object> opsForSet;

	public RedisDefaultSetRepository(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.opsForSet = redisTemplate.opsForSet();
		//log.info(">> redis-opsForSet:{},{}",redisTemplate.hashCode(),this.opsForSet.hashCode());		
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
    public RedisTemplate<String, Object> getRedisTemplate(){
    	return this.redisTemplate;
    }
	
	@Override
	public RedisOperations<String, ?> getRedisOperations() {
		return this.opsForSet.getOperations();
	}

	@Override
	public Long add(String redisKey, Object... values) {
		return this.opsForSet.add(this.getGlobalRedisKey(redisKey), values);
	}

	@Override
	public Set<Object> get(String redisKey) {
		return this.opsForSet.members(this.getGlobalRedisKey(redisKey));	
	}

	@Override
	public Long size(String redisKey) {
		return this.opsForSet.size(this.getGlobalRedisKey(redisKey));
	}

	@Override
	public boolean isExist(String redisKey, Object value) {
		return this.opsForSet.isMember(this.getGlobalRedisKey(redisKey), value);	
	}
}