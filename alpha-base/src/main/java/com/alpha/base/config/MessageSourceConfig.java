package com.alpha.base.config;

import java.util.HashSet;
import java.util.Set;

import javax.validation.ValidatorFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnExpression("T(com.alpha.base.MessageSourceConfig).isEnabled()")
public class MessageSourceConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Component
	@ConfigurationProperties(prefix="alpha.message-source")
	@Data
	private static class MessageSourceProperties{
		private final String defaultMessageSource="classpath:message/message-default";
		private Set<String> basenames;
		public Set<String> getBasenames() {return basenames;}
		public String getDefaultMessageSource() {return defaultMessageSource;}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Autowired
	private MessageSourceProperties props;
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    ReloadableResourceBundleMessageSource messageSource() {
		ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
		source.setBasenames(this.getMessageSourceBasenames());
        source.setDefaultEncoding("UTF-8");
        source.setCacheSeconds(60);
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }

    @Bean
    ValidatorFactory validatorFactory() {
        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.setValidationMessageSource(messageSource());
        return validatorFactory;
    }
    
	private String[] getMessageSourceBasenames() {
		Set<String> set = this.props.getBasenames();
		if(set==null) {set = new HashSet<>();}
		set.add(props.getDefaultMessageSource());
		set.forEach(p->log.info(">> messageSource.basename: {}",p));
		return set.toArray(new String[0]);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
}
