package com.alpha.base.config;

import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.stereotype.Component;

import com.alpha.base.support.util.SystemUtilPack.SystemUtil;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(name="spring.redis.enabled", havingValue="true", matchIfMissing=false)
@ConditionalOnExpression("T(com.alpha.base.RedisConfig).isEnabled()")
public class RedisConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final String RERDIS_TEMPLATE = "redisTemplate";
	public static final  String STRING_RERDIS_TEMPLATE = "stringRedisTemplate";
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Profile("local")
	@Configuration
	@ConditionalOnExpression("'${spring.redis.enabled:false}'.equals('true') && '${spring.redis.cluster.enabled:false}'.equals('false')")
	public static class RedisEmbeddedConfig {
		
		@Value("${spring.profiles.active:local}")
		private String springProfilesActive;
		
		@Value("${spring.redis.host:127.0.0.1}")
		private String host;
		
		@Value("${spring.redis.port:6379}")
		private int port;
	
		@Value("${spring.redis.password:}")
		private String password;

		@Value("${spring.redis.maxheap:256M}")
		private String maxheap;
		
		private redis.embedded.RedisServer redisServer;

		@PostConstruct
		public void postConstruct() throws Exception {
			this.doEmbededRedisServer();
		}
		
		@PreDestroy
		public void preDestroy() {
			Optional.ofNullable(this.redisServer).ifPresent(redis.embedded.RedisServer::stop);
		}
		
		private void doEmbededRedisServer() throws Exception {	
			boolean isEnable=true;
			if(isEnable && !this.springProfilesActive.equals("local")) {isEnable=false;}
			if(isEnable && !(this.host.equals("localhost")||this.host.equals("127.0.0.1"))) {isEnable=false;}
			if(isEnable && this.isUsePort(this.host,this.port)) {isEnable=false;log.info(">> redisServer({}:{}) exists",this.host,this.port);}
			if(!isEnable) {return;}
	
			this.redisServer = redis.embedded.RedisServer.builder().port(this.port).setting("maxheap "+this.maxheap).build();
			this.redisServer.start();
			
			log.info(">> redis server({}:{}) start: success",this.host,this.port);
		}
	
		private boolean isUsePort(String host,int port) {
			boolean isUse = false;	
			try {
				new Socket(host,port).close();
				isUse = true;
			} catch (Exception e) {
				//log.error(">> {}:{} is free",host,port);
			}
			return isUse;
		}

	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Configuration
	@ConditionalOnExpression("'${spring.redis.enabled:false}'.equals('true')")
	public static class RedisConnectionConfig {

		@Value("${spring.redis.host:127.0.0.1}")
		private String host;

		@Value("${spring.redis.port:6379}")
		private int port;

		@Value("${spring.redis.password:}")
		private String password;

		@Value("${spring.redis.cluster.nodes:}")
		private List<String> redisClusterNodes;

		@Value("${spring.redis.cluster.password:}")
		private String redisClusterPassword;

		@Value("${spring.redis.cluster.enabled:false}")
		private boolean redisClusterEnabled;

		@Value("${spring.redis.cluster.topology.refresh:60}")
		private int redisClusterTopologyRefresh;

		@Value("${spring.redis.lettuce.pool.enabled:false}")
		private boolean redisLettucePoolEnabled;

		@Value("${spring.redis.lettuce.pool.maxTotal:50}")
		private int redisLettucePoolMaxTotal;

		@Value("${spring.redis.lettuce.pool.maxIdle:10}")
		private int redisLettucePoolMaxIdle;

		@Value("${spring.redis.lettuce.pool.minIdle:0}")
		private int redisLettucePoolMinIdle;
		
		@Value("${spring.redis.lettuce.commandTimeout:60}")
		private int redisLettuceCommandTimeout;
		
		@Value("${spring.redis.lettuce.shutdownTimeout:100}")
		private int redisLettuceShutdownTimeout;

        @Bean
        RedisConnectionFactory redisConnectionFactory() {

			if(this.redisClusterEnabled) {
				log.info(">> redisConfiguration: cluster, {}",this.redisClusterNodes);				
				RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(this.redisClusterNodes);
				clusterConfig.setPassword(RedisPassword.of(this.redisClusterPassword));

				log.info(">> redisClusterTopologyRefresh: {}(seconds)",this.redisClusterTopologyRefresh);
				ClientOptions clientOptions = ClusterClientOptions.builder()
						.topologyRefreshOptions(ClusterTopologyRefreshOptions.builder()
								.enablePeriodicRefresh(Duration.ofSeconds(this.redisClusterTopologyRefresh)) //spring-default:60s, 주기적으로 connection을 갱신
								.enableAllAdaptiveRefreshTriggers() //문제가 될만한 operation-responds(MOVED_REDIRECT,ASK_REDIRECT,PERSISTENT_RECONNECTS,UNCOVERED_SLOT,UNKNOWN_NODE)이 발생한다면 즉시 connection을 갱신해
								.build())
						.build();
					
				log.info(">> redisLettucePoolEnabled: {}",this.redisLettucePoolEnabled);
				LettuceClientConfiguration clientConfig;
				if(this.redisLettucePoolEnabled) {
					clientConfig = LettucePoolingClientConfiguration.builder()
							.commandTimeout(Duration.ofSeconds(this.redisLettuceCommandTimeout)) //default:60s
							.shutdownTimeout(Duration.ofMillis(this.redisLettuceShutdownTimeout)) // default: 100ms, graceful close timeout
							.readFrom(ReadFrom.REPLICA_PREFERRED) //복제본 노드에서 읽음, 없는 경우 마스터
							.clientOptions(clientOptions)
							.poolConfig(this.createPoolConfig())
							.build();
				}else{
					clientConfig = LettuceClientConfiguration.builder()
							.commandTimeout(Duration.ofSeconds(this.redisLettuceCommandTimeout)) //default:60s
							.shutdownTimeout(Duration.ofMillis(this.redisLettuceShutdownTimeout)) // default: 100ms, graceful close timeout							
							.readFrom(ReadFrom.REPLICA_PREFERRED) //복제본 노드에서 읽음, 없는 경우 마스터
			                .clientOptions(clientOptions)
			                .build();
				}

				LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(clusterConfig,clientConfig);
				connectionFactory.afterPropertiesSet();
				return connectionFactory;
								
			}else{
				log.info(">> redisConfiguration: standalone, {}:{}",this.host,this.port);				
				RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(this.host,this.port);
				standaloneConfig.setPassword(RedisPassword.of(this.password));
				log.info(">> redisLettucePoolEnabled: {}",this.redisLettucePoolEnabled);
				LettuceClientConfiguration clientConfig;
				if(this.redisLettucePoolEnabled) {
					clientConfig = LettucePoolingClientConfiguration.builder()
							.commandTimeout(Duration.ofSeconds(this.redisLettuceCommandTimeout)) //default:60s
							.shutdownTimeout(Duration.ofMillis(this.redisLettuceShutdownTimeout)) // default: 100ms, graceful close timeout
							.poolConfig(this.createPoolConfig())
							.build();
				}else{
					clientConfig = LettuceClientConfiguration.builder()
			                .commandTimeout(Duration.ofSeconds(this.redisLettuceCommandTimeout)) //default:60s
			                .shutdownTimeout(Duration.ofMillis(this.redisLettuceShutdownTimeout)) // default: 100ms, graceful close timeout
			                .build();
				}
				
				LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(standaloneConfig,clientConfig);
				connectionFactory.afterPropertiesSet();
				return connectionFactory;
			}
		}

		@Bean(RERDIS_TEMPLATE)
		RedisTemplate<String,?> redisTemplate() {
			RedisTemplate<String,?> redisTemplate = new RedisTemplate<>();
			redisTemplate.setConnectionFactory(this.redisConnectionFactory());	
			redisTemplate.setDefaultSerializer(new Jackson2JsonRedisSerializer<Object>(Object.class));
			redisTemplate.setKeySerializer(new StringRedisSerializer());			
			redisTemplate.setHashKeySerializer(new StringRedisSerializer());
			redisTemplate.afterPropertiesSet();
			return redisTemplate;
		}

        @Bean(STRING_RERDIS_TEMPLATE)
        StringRedisTemplate stringRedisTemplate() {
			StringRedisTemplate redisTemplate = new StringRedisTemplate();
			redisTemplate.setConnectionFactory(this.redisConnectionFactory());
			redisTemplate.setDefaultSerializer(new StringRedisSerializer());
			redisTemplate.afterPropertiesSet();
			return redisTemplate;
		}

		private GenericObjectPoolConfig<StatefulConnection<?, ?>> createPoolConfig() { 
			GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
			poolConfig.setMaxTotal(this.redisLettucePoolMaxTotal);
			poolConfig.setMaxIdle(this.redisLettucePoolMaxIdle);
			poolConfig.setMinIdle(this.redisLettucePoolMinIdle);
			poolConfig.setBlockWhenExhausted(true); //사용 가능한 리소스(커넥션) 소진시 대기여부
			poolConfig.setMaxWait(Duration.ofMillis(1000));  //사용 가능한 리소스(커넥션) 없을 경우 대기시간
			return poolConfig;
		}
			
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Configuration
	@ConditionalOnExpression("'${spring.redis.enabled:false}'.equals('true') && '${spring.session.store-type:none}'.equals('redis')")
	public static class RedisSessionRepositoryConfig  {

		private final RedisIndexedSessionRepository redisSessionRepository;
		
		private final SystemUtil systemUtil;
		
		@Value("${spring.session.redis.cleanup.enabled:false}")
		private boolean springSessionRedisCleanupEnabled;

		@Value("${spring.session.redis.listenerContainer.enabled:false}")
		private boolean springSessionRedisListenerContainerEnabled;
		
		@Value("${spring.profiles.active:local}")
		private String springProfilesActive;

		@Value("${spring.session.redis.namespace:#{T(org.springframework.session.data.redis.RedisIndexedSessionRepository).DEFAULT_NAMESPACE}}")
		private String namespace;

		public RedisSessionRepositoryConfig(RedisIndexedSessionRepository sessionRepository,SystemUtil systemUtil) {
			this.redisSessionRepository = sessionRepository;
			this.systemUtil = systemUtil;
		}

		@PostConstruct
		public void postConstruct() {
			String redisNamespace=this.namespace;
			if(this.springProfilesActive.equals(SystemUtil.PROFILE_LOCAL)) {
				redisNamespace=this.namespace+":"+this.systemUtil.getServerIp();
			}
			
			this.redisSessionRepository.setRedisKeyNamespace(redisNamespace);
			log.info(">> redisSessionNamespace: {}",redisNamespace);
		}

		@Aspect
		@Component
		public class CleanupExpiredSessionsAspect {
			
			//isCleanup:true 정상적인 spring redis-session정책을 따른다. (1분주기 cleanupExpiredSessions)
			//isCleanup:false @Aspect-@Around에 의해서 cleanupExpiredSessions 기능을 수행하지 않고 null 리턴
			private boolean isCleanup = true;
			
			//isListener:true 정상적인 RedisMessageListenerContainer정책을 따른다.
			//isListener:false @Aspect-@Around에 의해서 RedisMessageListener 가동 정지
			private boolean isListener = true;

			public CleanupExpiredSessionsAspect() {
				if(springProfilesActive.equals(SystemUtil.PROFILE_LOCAL)) {return;}
				isCleanup = springSessionRedisCleanupEnabled;
				isListener = springSessionRedisListenerContainerEnabled;
			}
			
			@Around(value = "execution(void org.springframework.session.data.redis.RedisIndexedSessionRepository.cleanupExpiredSessions())")
			public Object cleanupExpiredSessions(ProceedingJoinPoint joinPoint) throws Throwable {
				//RedisIndexedSessionRepository class의 cleanupExpiredSessions()메소드에 대한 @Around하여 조건체크 후 수행
				//cleanupExpiredSessions을 하는 이유 : In Redis, a key is either lazily expired upon the next access to it, or passively by randomly reading keys in the database
				if(isCleanup) {return joinPoint.proceed();}
				log.info(">> aspect_around_skip(return null),target:org.springframework.session.data.redis.RedisIndexedSessionRepository.cleanupExpiredSessions");
				return null;	
			}
			
			@Around(value = "execution(void org.springframework.data.redis.listener.RedisMessageListenerContainer.start())")
			public Object start(ProceedingJoinPoint joinPoint) throws Throwable {
				//RedisMessageListenerContainer class의 start()메소드에 대한 @Around하여 조건체크 후 수행
				if(isListener) {return joinPoint.proceed();}
				log.info(">> aspect_around_skip(return null),target:org.springframework.data.redis.listener.RedisMessageListenerContainer.start");
				return null;
			}
			
			@Around(value = "execution(boolean org.springframework.data.redis.listener.RedisMessageListenerContainer.isRunning())")
			public Object isRunning(ProceedingJoinPoint joinPoint) throws Throwable {
				//RedisMessageListenerContainer class의 lazyListen()를 @Around하여야 하나 private 메소드라서 적용불가.
				//lazyListen() 내부로직에서 isRunning():true이면 수행하지 않도록 되어 있기에, isRunning()메소드에 대한 @Around하여 조건체크 후 수행.
				if(isListener) {return joinPoint.proceed();}
				log.info(">> aspect_around_skip(return true),target:org.springframework.data.redis.listener.RedisMessageListenerContainer.isRunning");
				return true;
			}			
			
		}
		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Configuration
	@ConditionalOnExpression("'${spring.redis.enabled:false}'.equals('true')")
	public static class RedisCacheConfig {

		private final RedisConnectionFactory connectionFactory;
				
		public RedisCacheConfig(RedisConnectionFactory connectionFactory) {
			this.connectionFactory = connectionFactory;
		}

		@Bean
		CacheManager redisCacheManager() {			
			return RedisCacheManager.RedisCacheManagerBuilder
					.fromConnectionFactory(this.connectionFactory)
					.cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()).build();
			
			//---------------------------------------------------------------------------------
			//fortify : dynamic code evaluation unsafe deserialization
			//---------------------------------------------------------------------------------
			//RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
			//		.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
			//		.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
			//return RedisCacheManager.RedisCacheManagerBuilder
			//		.fromConnectionFactory(this.connectionFactory)
			//		.cacheDefaults(cacheConfig).build();
			//---------------------------------------------------------------------------------
						
	  }
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
}

