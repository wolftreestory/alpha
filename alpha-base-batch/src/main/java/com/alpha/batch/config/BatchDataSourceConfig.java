package com.alpha.batch.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class BatchDataSourceConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean(name="batch.hikariConfig")
    @ConfigurationProperties(prefix="alpha.datasource.batch")
    HikariConfig hikariConfig() {
    	log.debug(">> batch.hikariConfig");
		return new HikariConfig();
	}

    @Bean(name="batch.dataSource")
    @Primary @BatchDataSource
    LazyConnectionDataSourceProxy dataSource(@Qualifier("batch.hikariConfig") HikariConfig hikariConfig) {
    	log.debug(">> batch.dataSource");  	
		return new LazyConnectionDataSourceProxy(new HikariDataSource(hikariConfig));
	}

    @Bean(name="batch.txManager")
    @Primary @BatchDataSource
    PlatformTransactionManager txManager(@Qualifier("batch.dataSource") DataSource dataSource) {
    	log.debug(">> batch.txManager");
		return new DataSourceTransactionManager(dataSource);
	}
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
}