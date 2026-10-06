package com.alpha.opus.domain.xtra.board.config;

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
@ConditionalOnProperty(prefix="alpha.domain.board.datasource",name="jdbcUrl")
public class BoardDataSourceConfig {

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean(name="board.hikariConfig")
    @ConfigurationProperties(prefix="alpha.domain.board.datasource")
    HikariConfig hikariConfig() {
    	log.debug(">> board.hikariConfig");
		return new HikariConfig();
	}
    
    @Bean(name="board.dataSource")
    @ConditionalOnBean(name="board.hikariConfig")
    LazyConnectionDataSourceProxy dataSource(@Qualifier("board.hikariConfig") HikariConfig hikariConfig) {
    	log.debug(">> board.dataSource");
		return new LazyConnectionDataSourceProxy(new HikariDataSource(hikariConfig));
	}

    @Bean(name="board.txManager")
    @ConditionalOnBean(name="board.dataSource")
    PlatformTransactionManager txManager(@Qualifier("board.dataSource") DataSource dataSource) {
    	log.debug(">> board.txManager");
		return new DataSourceTransactionManager(dataSource);
	}
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}