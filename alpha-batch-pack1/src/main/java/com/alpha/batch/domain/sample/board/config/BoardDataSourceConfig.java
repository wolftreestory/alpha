package com.alpha.batch.domain.sample.board.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import com.alpha.batch.config.BatchDefaultConfig.DatasourceDomainProperties;
import com.alpha.batch.config.BatchDefaultConfig.DatasourceDomainProperties.AkaHikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnExpression("#{'${spring.batch.job.names}'.contains('sample2Job')}")
public class BoardDataSourceConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// @Bean(name="board.hikariConfig")
	// @ConfigurationProperties(prefix="alpha.datasource.domain.board")
	// AkaHikariConfig hikariConfig() {
	// log.debug(">> board.hikariConfig");
	// return new AkaHikariConfig();
	// }

	@Bean(name = "board.hikariConfig")
	AkaHikariConfig hikariConfig(DatasourceDomainProperties domainProperties) {
		log.debug(">> board.hikariConfig");
		return domainProperties.getConfig("board");
	}

	@Bean(name = "board.dataSource")
	LazyConnectionDataSourceProxy dataSource(@Qualifier("board.hikariConfig") AkaHikariConfig hikariConfig) {
		log.debug(">> board.dataSource");
		log.debug(">> tag:{}", hikariConfig.getTag());
		return new LazyConnectionDataSourceProxy(new HikariDataSource(hikariConfig));
	}

	@Bean(name = "board.txManager")
	PlatformTransactionManager txManager(@Qualifier("board.dataSource") DataSource dataSource) {
		log.debug(">> board.txManager");
		return new DataSourceTransactionManager(dataSource);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}