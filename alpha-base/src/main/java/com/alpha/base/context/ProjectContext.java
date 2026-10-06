package com.alpha.base.context;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix="project")
@Data
public class ProjectContext {
	private Meta meta;
	private Build build;

	private String groupId;
	private String artifact;
	private String version;
	private String name;
	private String description;
  	  	  
	@Data
	public static class Meta{
		private String group;
		private String service;
		private String jobId;
		private String basePathPrefix;
		//private String logfile;
	}
	
	@Data
	public static class Build {
		private String finalName;
		private String revision;
		private String branch;
		private String timestamp;
	}
}

