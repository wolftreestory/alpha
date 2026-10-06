package com.alpha.batch.domain.sample.score.config;

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
public class ScoreDataSourceConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// @Bean(name="score.hikariConfig")
	// @ConfigurationProperties(prefix="alpha.datasource.domain.score")
	// AkaHikariConfig hikariConfig() {
	// log.debug(">> board.hikariConfig");
	// return new AkaHikariConfig();
	// }

	@Bean(name = "score.hikariConfig")
	AkaHikariConfig hikariConfig(DatasourceDomainProperties domainProperties) {
		log.debug(">> score.hikariConfig");
		return domainProperties.getConfig("score");
	}

	@Bean(name = "score.dataSource")
	LazyConnectionDataSourceProxy dataSource(@Qualifier("score.hikariConfig") AkaHikariConfig hikariConfig) {
		log.debug(">> score.dataSource");
		log.debug(">> tag:{}", hikariConfig.getTag());
		return new LazyConnectionDataSourceProxy(new HikariDataSource(hikariConfig));
	}

	@Bean(name = "score.txManager")
	PlatformTransactionManager txManager(@Qualifier("score.dataSource") DataSource dataSource) {
		log.debug(">> score.txManager");
		return new DataSourceTransactionManager(dataSource);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}