package com.alpha.opus.domain.xtra.score.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@Profile({"local","dev"})
@ConditionalOnProperty(prefix="alpha.domain.score.datasource",name="jdbcUrl")
public class ScoreDataSourceConfig {
	
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean(name="score.hikariConfig")
    @ConfigurationProperties(prefix="alpha.domain.score.datasource")
    HikariConfig hikariConfig() {
    	log.debug(">> score.hikariConfig");
		return new HikariConfig();
	}

    @Bean(name="score.dataSource")
    @ConditionalOnBean(name="score.hikariConfig")
    LazyConnectionDataSourceProxy dataSource(@Qualifier("score.hikariConfig") HikariConfig hikariConfig) {
    	log.debug(">> score.dataSource");   	
		return new LazyConnectionDataSourceProxy(new HikariDataSource(hikariConfig));
	}

    @Bean(name="score.txManager")
    @ConditionalOnBean(name="score.dataSource")
    PlatformTransactionManager txManager(@Qualifier("score.dataSource") DataSource dataSource) {
    	log.debug(">> score.txManager");
		return new DataSourceTransactionManager(dataSource);
	}
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}