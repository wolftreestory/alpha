package com.alpha.base.context;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix="project")
@Getter @Setter
public class ProjectContext {
	private Meta meta;
	private Build build;

	private String groupId;
	private String artifact;
	private String version;
	private String name;
	private String description;
  	  	  
	@Getter @Setter
	public static class Meta{
		private String group;
		private String service;
		private String jobId;
		private String basePathPrefix;
	}
	
	@Getter @Setter
	public static class Build {
		private String finalName;
		private String revision;
		private String branch;
		private String timestamp;
	}
}

