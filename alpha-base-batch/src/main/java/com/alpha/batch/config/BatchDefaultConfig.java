package com.alpha.batch.config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alpha.base.exception.SystemException;
import com.zaxxer.hikari.HikariConfig;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class BatchDefaultConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Component
	@ConfigurationProperties(prefix="spring.batch.job")
	@Data
	public static class DomainScopeResolver {
		
		@Autowired
		private BatchJobProperties batchJobProperties;
		
		private String names;
		
		private String domainScope;
		
		private List<String> getList() {
			if(domainScope == null || domainScope.isEmpty()) {
				if(names == null || names.isEmpty()) {return null;}								
				List<String> list = Arrays.asList(names.indexOf(",")>-1?names.split(","):new String[] {names});
				for(String name:list) {domainScope=domainScope+batchJobProperties.getDefaultDomainScope(name)+",";}				
				return this.getList();
			}
			
			List<String> list = Arrays.asList(domainScope.indexOf(",")>-1?domainScope.split(","):new String[] {domainScope});
			list.replaceAll(String::trim);
			list.removeIf(String::isEmpty); //빈값제거
			list = list.stream().distinct().collect(Collectors.toList()); //중복제거
			
			return list;
			
		}

		public String getDomainKey(String name) {
			String targetKey = null;			
			for(String key : this.getList()) {
				targetKey=key.trim();
				if(key.trim().equals(name.trim())){break;} 
			}
			return targetKey;
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Component
	@ConfigurationProperties(prefix="alpha.datasource")
	@Data
	public static class DatasourceDomainProperties{
		
		@Autowired
		private DomainScopeResolver resolver;
		
		Map<String, AkaHikariConfig> domain;


		public AkaHikariConfig getConfig(String name) {

			String targetKey=resolver.getDomainKey(name);
			log.debug(">> targetKey: {}",targetKey);

			if(!StringUtils.hasText(targetKey)) {
				throw new SystemException("targetKey is null or empty.");
			}
			if(!domain.containsKey(targetKey)) {
				throw new SystemException("Does not exist in domain(Map<String, AkaHikariConfig>) for key(" + name +")");
			}
			
			return domain.get(targetKey);	
		}
		
		@Data
		@EqualsAndHashCode(callSuper=true)
		public static class AkaHikariConfig extends HikariConfig {
			private String tag;
		}		
	}

	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static interface BatchJobProperties {

		default public String getDefaultDomainScope(String jobName) {
			String value = null;
			try {
				String methodName = "get"+jobName.substring(0, 1).toUpperCase() + jobName.substring(1);
				Method method = this.getClass().getDeclaredMethod(methodName);
				DefaultAttributes attributes = (DefaultAttributes)method.invoke(this);
				value = attributes.getDefaultDomainScope();
			} catch (Exception e) {
				log.info(">> Cannot find defaultDatasourceDomain for jobName({})",jobName);
				return null;
			}
			return value;
		}
	}

	@Data
	public static class DefaultAttributes {
		private String defaultDomainScope;
	}


	@Data
	public static class DefaultBatchJobProperties implements BatchJobProperties {
		//no method
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}