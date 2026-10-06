package com.alpha.opus.domain.xtra.score.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariConfig;

import lombok.Getter;
import lombok.Setter;

@Component
@Profile({"local","dev"})
@ConfigurationProperties(prefix="alpha.domain.score")
@Getter @Setter
public class ScoreProperties {

	private String description;
	private DomainHikariConfig datasource;
	
	@Getter @Setter
	public static class DomainHikariConfig extends HikariConfig {}
	
}