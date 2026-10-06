package com.alpha.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.alpha.batch.config.BatchDefaultConfig.DefaultAttributes;
import com.alpha.batch.config.BatchDefaultConfig.DefaultBatchJobProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class BatchProperties {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Component
	@ConfigurationProperties(prefix = "alpha.batch")
	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class BatchJobProperties extends DefaultBatchJobProperties {
		private String fileBase;
		private Sample1JobProperties sample1Job;
		private Sample2JobProperties sample2Job;

		@Data
		@EqualsAndHashCode(callSuper = true)
		public static class Sample1JobProperties extends DefaultAttributes {
			private String workApi;
			private String uploadApi;
		}

		@Data
		@EqualsAndHashCode(callSuper = true)
		public static class Sample2JobProperties extends DefaultAttributes {
			private String downloadApi;
			private String downloadApiParam;
			private String remoteHost;
			private String remotePath;
			private String username;
			private String keyPath;
		}

	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}