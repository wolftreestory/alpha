package com.alpha.base.config;

import java.util.Properties;
import java.util.Set;

import javax.servlet.MultipartConfigElement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.unit.DataSize;

import com.alpha.base.context.MaskContext;
import com.alpha.base.support.aid.AwareAidPack;
import com.alpha.base.support.aid.BeanAidPack;
import com.alpha.base.support.aid.RequestAttributesAidPack;
import com.alpha.base.support.aid.AwareAidPack.ApplicationContextAid;
import com.alpha.base.support.aid.BeanAidPack.BeanAid;
import com.alpha.base.support.aid.RequestAttributesAidPack.RequestAttributesAid;
import com.alpha.base.support.util.AlertUtilPack;
import com.alpha.base.support.util.Base64UtilPack;
import com.alpha.base.support.util.BashProcessUtilPack;
import com.alpha.base.support.util.CompressUtilPack;
import com.alpha.base.support.util.CryptoUtilPack;
import com.alpha.base.support.util.FileUtilPack;
import com.alpha.base.support.util.HttpRequestUtilPack;
import com.alpha.base.support.util.JsonUtilPack;
import com.alpha.base.support.util.LobConverterUtilPack;
import com.alpha.base.support.util.LocaleUtilPack;
import com.alpha.base.support.util.MaskUtilPack;
import com.alpha.base.support.util.MessageUtilPack;
import com.alpha.base.support.util.ObjectMapperUtilPack;
import com.alpha.base.support.util.PropertiesUtilPack;
import com.alpha.base.support.util.RestTemplateUtilPack;
import com.alpha.base.support.util.SessionUtilPack;
import com.alpha.base.support.util.SystemUtilPack;
import com.alpha.base.support.util.ThreadUtilPack;
import com.alpha.base.support.util.TimeUtilPack;
import com.alpha.base.support.util.TransactionUtilPack;
import com.alpha.base.support.util.AlertUtilPack.AlertUtil;
import com.alpha.base.support.util.Base64UtilPack.Base64Util;
import com.alpha.base.support.util.BashProcessUtilPack.BashProcessUtil;
import com.alpha.base.support.util.CompressUtilPack.CompressUtil;
import com.alpha.base.support.util.CryptoUtilPack.CryptoUtil;
import com.alpha.base.support.util.FileUtilPack.FileUtil;
import com.alpha.base.support.util.HttpRequestUtilPack.HttpRequestUtil;
import com.alpha.base.support.util.JsonUtilPack.JsonUtil;
import com.alpha.base.support.util.LobConverterUtilPack.LobConverterUtil;
import com.alpha.base.support.util.LocaleUtilPack.LocaleUtil;
import com.alpha.base.support.util.MaskUtilPack.MaskUtil;
import com.alpha.base.support.util.MessageUtilPack.MessageUtil;
import com.alpha.base.support.util.ObjectMapperUtilPack.ObjectMapperUtil;
import com.alpha.base.support.util.PropertiesUtilPack.PropertiesUtil;
import com.alpha.base.support.util.RestTemplateUtilPack.RestTemplateUtil;
import com.alpha.base.support.util.SessionUtilPack.SessionUtil;
import com.alpha.base.support.util.SystemUtilPack.SystemUtil;
import com.alpha.base.support.util.ThreadUtilPack.ThreadUtil;
import com.alpha.base.support.util.TimeUtilPack.TimeUtil;
import com.alpha.base.support.util.TransactionUtilPack.TransactionUtil;

import lombok.extern.slf4j.Slf4j;

@Configuration
public class AidBaseConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Value("${spring.application.name}")
	private String applicationName;

	@Value("${server.name:}")
	private String serverName;	
	
	@Value("${server.port:}")
	private String serverPort;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Environment environment;

	private final ReloadableResourceBundleMessageSource messageSource;

	public AidBaseConfig(Environment environment, ReloadableResourceBundleMessageSource messageSource) {
		this.environment = environment;
		this.messageSource = messageSource;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Slf4j
	public static class AidImportSelector extends AutoConfigurationImportSelector {	
		// spring-boot-starter로 시작하는 의존성을 적용시 AutoConfiguration를 수행하게 된다.
		// application.yml에 속성을 관리하고, 이에 따라 AutoConfiguration을 적용 할 수 있도록 함.
		// @SpringBootApplication가 적용된 class(Application.java)에 @Import(com.alpha.base.config.AidImportSelector.class) 를 적용
		
		@Override
		protected Set<String> getExclusions(AnnotationMetadata metadata, AnnotationAttributes attributes) {
			Set<String> exclusions = super.getExclusions(metadata, attributes);
			
			Properties props = PropertiesConfig.loadYamlProperties();

			String jdbcEnabled = props.getProperty("alpha.jdbc.enabled","true");
			//log.info(">> alpha.jdbcEnabled.enabled: {}",jdbcEnabled);
			if(jdbcEnabled.toLowerCase().equals("false")) {
				exclusions.add("org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration");
			}

			String securityEnabled = props.getProperty("alpha.security.enabled","false");
			//log.info(">> alpha.security.enabled: {}",securityEnabled);
			if(securityEnabled.toLowerCase().equals("false")) {
				exclusions.add("org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration");
			}

			String redisEnabled = props.getProperty("spring.redis.enabled","false");
			//log.info(">> spring.redis.enabled: {}",redisEnabled);	
			if(redisEnabled.toLowerCase().equals("false")) {
				exclusions.add("org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration");
				//exclusions.add("org.springframework.boot.autoconfigure.session.SessionAutoConfiguration");
			}

			if(exclusions!=null && !exclusions.isEmpty()) {
				log.info(">> exclusions:{}",exclusions);		
			}
			return exclusions;
		}
	}
	
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    @ConditionalOnExpression("'${spring.main.web-application-type:true}'.equals('none')")
    MultipartConfigElement getMultipartConfigElement() {	
    	MultipartConfigFactory factory = new MultipartConfigFactory();
    	factory.setMaxFileSize(DataSize.ofMegabytes(10));
    	return factory.createMultipartConfig();
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    ApplicationContextAid getApplicationContextAid() {
		return AwareAidPack.getApplicationContextAid();
	}

    @Bean
    BeanAid getBeanAid() {
		return BeanAidPack.getBeanAid();
	}

    
    @Bean
    RequestAttributesAid getRequestAttributesAid() {
		return RequestAttributesAidPack.getRequestAttributesAid();
	}
        
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    AlertUtil getAlertUtil() {
		return AlertUtilPack.getAlertUtil(getRequestAttributesAid(), getMessageUtil());
	}

    @Bean
    BashProcessUtil getBashProcessUtil() {
		return BashProcessUtilPack.getBashProcessUtil();
	}
    
    @Bean
    Base64Util getBase64Util() {
		return Base64UtilPack.getBase64Util();
	}

    @Bean
    CompressUtil getCompressUtil() {
    	return CompressUtilPack.getCompressUtil();
    }
    
    @Bean
    CryptoUtil getCryptoUtil() {
		return CryptoUtilPack.getCryptoUtil(CryptoUtilPack.Meta.builder().beanAid(getBeanAid()).build());
	}
    
    @Bean
    FileUtil getFileUtil() {
		return FileUtilPack.getFileUtil(getBeanAid(),getSystemUtil());
	}

    @Bean
    @ConditionalOnWebApplication
    HttpRequestUtil getHttpRequestUtil() {
		return HttpRequestUtilPack.getHttpRequestUtil(getRequestAttributesAid());
	}

    @Bean
    JsonUtil getJsonUtil() {
		return JsonUtilPack.getJsonUtil(true);
	}

    @Bean
    LobConverterUtil getLobConverterUtil() {
		return LobConverterUtilPack.getLobConverterUtil(getFileUtil());
	}

    @Bean
    LocaleUtil getLocaleUtil() {
		return LocaleUtilPack.getLocaleUtil(getRequestAttributesAid());
	}

    @Bean
    MaskUtil getMaskUtil() {
    	MaskContext context = null;
    	try{context = ApplicationContextAid.getApplicationContext().getBean(MaskContext.class);}catch(Exception e) {}
		return MaskUtilPack.getMaskUtil(context);
	}
    
    @Bean
    MessageUtil getMessageUtil() {
		return MessageUtilPack.getMessageUtil(getLocaleUtil(),this.messageSource);
	}

    @Bean
    ObjectMapperUtil getObjectMapperUtil() {
		return ObjectMapperUtilPack.getObjectMapperUtil();
	}

    @Bean
    RestTemplateUtil getRestTemplateUtil() {
		return RestTemplateUtilPack.getRestTemplateUtil();
	}

    @Bean
    @ConditionalOnWebApplication
    SessionUtil getSessionUtil() {
		return SessionUtilPack.getSessionUtil(getRequestAttributesAid());
	}

    @Bean
    SystemUtil getSystemUtil() {
		return SystemUtilPack.getSystemUtil(this.environment,this.serverName,this.serverPort,this.applicationName);
	}
    
    @Bean
    ThreadUtil getThreadUtil() {
		return ThreadUtilPack.getThreadUtil();
	}
    
    @Bean
    TimeUtil getTimeUtil() {
		return TimeUtilPack.getTimeUtil();
	}

    @Bean
    PropertiesUtil getPropertiesUtil() {
		return PropertiesUtilPack.getPropertiesUtil(getBeanAid(),getJsonUtil(),PropertiesConfig.DEFAULT_APPLICATION_PROPS);
	}

    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnBean(PlatformTransactionManager.class)
    TransactionUtil getTransactionUtil() {
    	PlatformTransactionManager txManager = null;
    	try{txManager = getBeanAid().getBean(PlatformTransactionManager.class);}catch(Exception e) {}
		return TransactionUtilPack.getTransactionUtil(txManager);
	}
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}