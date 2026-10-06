package com.alpha.base.support.util;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.alpha.base.config.RedisConfig;
import com.alpha.base.support.aid.BeanAidPack.BeanAid;
import com.alpha.base.support.etc.RedisSequence;
import com.alpha.base.support.etc.RedisSequence.SequenceType;
import com.alpha.base.support.util.PropertiesUtilPack.PropertiesUtil;
import com.alpha.base.support.util.SystemUtilPack.SystemUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RedisUtilPack {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface RedisOpsRepository {
		public static String REPO_HASH = "redisDefaultHashRepository";
		public static String REPO_LIST = "redisDefaultListRepository";
		public static String REPO_SET = "redisDefaultSetRepository";
		public static String REPO_OBJECT = "redisDefaultObjectRepository";
		
		public static String NAMESPACE_REPOSITORY_HASH = "alpha:GLOBAL:REPOSITORY:HASH";
		public static String NAMESPACE_REPOSITORY_LIST = "alpha:GLOBAL:REPOSITORY:LIST";
		public static String NAMESPACE_REPOSITORY_SET = "alpha:GLOBAL:REPOSITORY:SET";
		public static String NAMESPACE_REPOSITORY_OBJECT = "alpha:GLOBAL:REPOSITORY:OBJECT";
		
		default public boolean delete(String redisKey) {return this.getRedisOperations().delete(this.getGlobalRedisKey(redisKey));}
		default public boolean isExists(String redisKey) {return this.getRedisOperations().hasKey(this.getGlobalRedisKey(redisKey));}
	    default public boolean setExpireTime(String redisKey, long expirationTime) {return this.getRedisOperations().expire(this.getGlobalRedisKey(redisKey), expirationTime, TimeUnit.SECONDS);}
	    default public long getExpireTime(String redisKey){return this.getRedisOperations().getExpire(this.getGlobalRedisKey(redisKey), TimeUnit.SECONDS);}
	    
	    public RedisTemplate<String, Object> getRedisTemplate();
	    public RedisOperations<String, ?> getRedisOperations();
		public String getGlobalRedisKey(String key);
	}
	
	public interface RedisHashRepository extends RedisOpsRepository {
		default public String getGlobalRedisKey(String redisKey) {return NAMESPACE_REPOSITORY_HASH + ":" + redisKey;}
		public Map<String,Object> get(String redisKey);
		public Object get(String redisKey, String valueKey);
		public void put(String redisKey, String valueKey, Object value);
		public Long delete(String redisKey, String valueKey);
	}

	public interface RedisListRepository extends RedisOpsRepository {
		default public String getGlobalRedisKey(String redisKey) {return NAMESPACE_REPOSITORY_LIST + ":" + redisKey;}
		public Long size(String redisKey);
		public void set(String redisKey, long index, Object value);
		public void trim(String redisKey, long start, long end);
		public Long remove(String redisKey, long index, Object value);
		public List<Object> range(String redisKey, long start, long end);
		public List<Object> get(String redisKey);
		
		public Object leftPop(String redisKey);
		public Long leftPush(String redisKey, Object value);
		public Long leftPushAll(String redisKey, Collection<Object> collection);
		
		public Object rightPop(String redisKey);
		public Long rightPush(String redisKey, Object value);
		public Long rightPushAll(String redisKey, Collection<Object> collection);
	}

	public interface RedisSetRepository extends RedisOpsRepository {
		default public String getGlobalRedisKey(String redisKey) {return NAMESPACE_REPOSITORY_SET + ":" + redisKey;}
		public Long add(String redisKey, Object...values);
		public Set<Object> get(String redisKey);		
		public Long size(String redisKey);		
		public boolean isExist(String redisKey, Object value);		
	}

	public interface RedisObjectRepository extends RedisOpsRepository {
		default public String getGlobalRedisKey(String redisKey) {return NAMESPACE_REPOSITORY_OBJECT + ":" + redisKey;}
		public void put(String redisKey, Object value);
		public void put(String redisKey, Object value, Duration duration);
		public Object get(String redisKey);
		public <T> T get(String redisKey, Class<T> clazz);
		public <T> T get(String redisKey, TypeReference<T> typeReference);	    
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface RedisUtil {
		
		@FunctionalInterface
		public interface RedisMultiBlockFunction<T> {public abstract T execute(RedisOperations<String, Object> operations);}

		@FunctionalInterface
		public interface RedisPipeBlockFunction {public abstract Object execute(RedisConnection connection, RedisSerializer<String> keySerializer, RedisSerializer<Object> valueSerializer);}

		public boolean isEnable();
		public boolean isCluster();
		public <T> T executeMulti(RedisMultiBlockFunction<T> lambda);
		public <T> T executeMulti(RedisTemplate<?,?> redisTemplate, RedisMultiBlockFunction<T> lambda);

		public List<Object> executePipe(RedisPipeBlockFunction lambda);
		public List<Object> executePipe(RedisTemplate<?,?> redisTemplate, RedisPipeBlockFunction lambda);

		public RedisTemplate<?,?> getRedisTemplate();
		public StringRedisTemplate getStringRedisTemplate();
		
		public RedisHashRepository getRedisHashRepository();
		public RedisListRepository getRedisListRepository();
		public RedisSetRepository getRedisSetRepository();
		public RedisObjectRepository getRedisObjectRepository();
		
		public String getSequence(SequenceType sequenceType);		
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Data @Builder
	public static class Meta {
		private PropertiesUtil propertiesUtil;
		private SystemUtil systemUtil;
		private BeanAid beanAid;
	}
	
	public static RedisUtil getRedisUtil(Meta meta) {

		return new RedisUtil() {

			@Override
			public boolean isEnable() {
				String value = meta.getPropertiesUtil().getProperty("spring.redis.enabled");
				if(value==null || value.equals("")) {return false;}
				return Boolean.valueOf(value);
			}

			@Override
			public boolean isCluster() {
				String value = meta.getPropertiesUtil().getProperty("spring.redis.cluster.enabled");
				if(value==null || value.equals("")) {return false;}
				return Boolean.valueOf(value);				
			}
			
			@Override
			public <T> T executeMulti(RedisMultiBlockFunction<T> lambda) {
				return this.executeMulti(this.getRedisTemplate(),lambda);
			}

			@Override
			public <T> T executeMulti(RedisTemplate<?,?> redisTemplate, RedisMultiBlockFunction<T> lambda) {
				if(redisTemplate==null) {return null;}
				redisTemplate.setEnableTransactionSupport(false);
				
				return redisTemplate.execute(new SessionCallback<T>() {
					@Override @SuppressWarnings("unchecked")
					public <K, V> T execute(RedisOperations<K, V> operations) throws DataAccessException {
						return lambda.execute((RedisOperations<String, Object>)operations);
					}
				});
			}
			
			@Override
			public List<Object> executePipe(RedisPipeBlockFunction lambda) {
				return this.executePipe(this.getRedisTemplate(),lambda);
			}
			
			@Override
			public List<Object> executePipe(RedisTemplate<?,?> redisTemplate, RedisPipeBlockFunction lambda) {
				if(redisTemplate==null) {return null;}
				redisTemplate.setEnableTransactionSupport(false);
				
				@SuppressWarnings("unchecked")
				RedisSerializer<String> keySerializer = (RedisSerializer<String>)redisTemplate.getKeySerializer();
				
				@SuppressWarnings("unchecked")
				RedisSerializer<Object> valueSerializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();
				
				return redisTemplate.executePipelined(new RedisCallback<Object>() {
					@Override
					public Object doInRedis(RedisConnection connection) throws DataAccessException {
						return lambda.execute(connection,keySerializer,valueSerializer);
					}
				});
			}
			
			@Override
			public RedisTemplate<?,?> getRedisTemplate() {return meta.getBeanAid().getBean(RedisConfig.RERDIS_TEMPLATE, RedisTemplate.class);}
			
			@Override
			public StringRedisTemplate getStringRedisTemplate() {return meta.getBeanAid().getBean(RedisConfig.STRING_RERDIS_TEMPLATE, StringRedisTemplate.class);}
			
			@Override
			public RedisHashRepository getRedisHashRepository() {return meta.getBeanAid().getBean(RedisOpsRepository.REPO_HASH, RedisHashRepository.class);}

			@Override
			public RedisListRepository getRedisListRepository() {return meta.getBeanAid().getBean(RedisOpsRepository.REPO_LIST, RedisListRepository.class);}

			@Override
			public RedisSetRepository getRedisSetRepository() {return meta.getBeanAid().getBean(RedisOpsRepository.REPO_SET, RedisSetRepository.class);}

			@Override
			public RedisObjectRepository getRedisObjectRepository() {return meta.getBeanAid().getBean(RedisOpsRepository.REPO_OBJECT, RedisObjectRepository.class);}
   			
			@Override
			public String getSequence(SequenceType sequenceType) {
				if(sequenceType==null) {return null;}
				
				String sequence = RedisSequence.getSequence("alpha:GLOBAL:SEQUENCE:"+SequenceType.elecSignKeyNo, "2024060101", "7.3");
				//"7.3".split(\\.)[0]:baseDate부터 분을 표기(baseDate부터 19년)
				//"7.3".split(\\.)[1]:increment(0~999) => 1분에 1000개
				
				if(meta.getPropertiesUtil().getProperty("spring.profiles.active").equals(SystemUtil.PROFILE_LOCAL)) {
					String discrimer = meta.getSystemUtil().getServerIp().split("\\.")[3];
					discrimer = "9"+(discrimer.length()==2?"0":"")+discrimer;					
					sequence = discrimer+sequence.substring(discrimer.length()+1);
				}
				
				log.debug(">> sequence:{}",sequence);
				
				return sequence;
			}
		};
			
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}