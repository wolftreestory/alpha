package com.alpha.base.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnExpression("T(com.alpha.base.DataSourceConfig).isEnabled()")
@ConditionalOnProperty(prefix="spring.datasource.hikari",name="jdbc-url")
public class DataSourceConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String HIKARI_CONFIG = "default.hikariConfig";
    public static final String DATA_SOURCE = "default.dataSource";
    public static final String TRANSACTION_MANAGER = "default.transactionManager";

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
    @Bean(HIKARI_CONFIG)
    @ConfigurationProperties(prefix="spring.datasource.hikari")
    HikariConfig hikariConfig() {
    	log.info(">> hikariConfig: {}",HIKARI_CONFIG);    	
		return new HikariConfig();
	}

    @Bean(DATA_SOURCE)
    @ConditionalOnBean(name=HIKARI_CONFIG)
    LazyConnectionDataSourceProxy dataSource(@Qualifier(HIKARI_CONFIG) HikariConfig hikariConfig) {
    	log.info(">> dataSource: {} (maximumPoolSize:{})",DATA_SOURCE,hikariConfig.getMaximumPoolSize());    	
    	//스프링의 경우 트랜잭션 시작 시, DataSource를 사용하건 안하건 커넥션을 확보하며, 그로 인해 불필요한 리소스가 발생됨.
    	//이를 줄이기 위해 LazyConnectionDataSourceProxy로 wrapping 할 경우 실제 커넥션이 필요한 경우에만 datasource 에서 connection 을 반환
		DataSource dataSource = new HikariDataSource(hikariConfig);
		return new LazyConnectionDataSourceProxy(dataSource);
	}

    @Bean(TRANSACTION_MANAGER)
    @ConditionalOnBean(name=DATA_SOURCE)
    PlatformTransactionManager txManager(@Qualifier(DATA_SOURCE) DataSource dataSource) {
    	log.info(">> txManager: {}",TRANSACTION_MANAGER);
		DataSourceTransactionManager txManager = new DataSourceTransactionManager(dataSource);
		txManager.setNestedTransactionAllowed(true);
		txManager.setGlobalRollbackOnParticipationFailure(false);
		return txManager;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////    
}