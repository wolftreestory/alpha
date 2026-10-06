package com.alpha.base.config;

import java.io.IOException;
import java.util.Properties;
import java.util.TreeSet;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnExpression("T(com.alpha.base.PropertiesConfig).isEnabled()")
@PropertySource(value={"classpath:application.yml","classpath:application.yaml"}, ignoreResourceNotFound=true)
public class PropertiesConfig implements EnvironmentAware {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static final String DEFAULT_APPLICATION_PROPS = "applicationProps";
	
	private Properties applicationProps;
	
	@Override
	public void setEnvironment(Environment environment) {
		Properties props = loadYamlProperties();
		
		TreeSet<String> keys = new TreeSet<>(props.stringPropertyNames());
		
		log.info(">> ----------------------------------------------------");
		for (String key : keys) {
			log.info(">> {}: {}",key,environment.getProperty(key, ""));
			props.put(key, environment.getProperty(key,""));
		}
		log.info(">> ----------------------------------------------------");
		
		this.applicationProps = props;	    
	 }

    @Bean(name=DEFAULT_APPLICATION_PROPS)
    Properties getApplicationProps() {
		 return this.applicationProps;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Properties loadYamlProperties() {
		ClassPathResource resource1 = new  ClassPathResource("application.yml");
		ClassPathResource resource2 = new  ClassPathResource("application.yaml");
		if(!resource1.exists() && !resource2.exists()) {
			throw new RuntimeException("resource(application.yml or application.yaml) is not exist.");
		}
		if(resource1.exists() && resource2.exists()) {
			throw new RuntimeException("Only one of the two resource(application.yml,application.yaml) files can be applied.");
		}

		ClassPathResource resource = null;
		if(resource1.exists()) {resource=resource1; log.info(">> extract properties from application.yml");}
		if(resource2.exists()) {resource=resource2; log.info(">> extract properties from application.yaml");}
			
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(resource); 
		factory.afterPropertiesSet();
		Properties props = factory.getObject();
		
		return props;
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class YamlPropertySourceFactory implements PropertySourceFactory {

		@Override
		public org.springframework.core.env.PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
			Properties properties = null;
			if(resource.getResource().exists()) {
		        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		        factory.setResources(resource.getResource());
		        properties = factory.getObject();
			}
	        return new PropertiesPropertySource(resource.getResource().getFilename(), properties);
		}

	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
