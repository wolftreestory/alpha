package com.alpha.base.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnExpression("T(com.alpha.base.JasyptConfig).isEnabled()")
@ConditionalOnProperty(name="jasypt.enabled", havingValue="true", matchIfMissing=false)
public class JasyptConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final String DEFAULT_PASSWORD = "alpha.jaspt.default.password.!QAZ@WSX";
	
	public static final String STRING_ENCRYPTOR = "jasyptStringEncryptor";
	
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Value("${jasypt.encryptor.password:}")
    private String password;
    
    @Bean(STRING_ENCRYPTOR)
    StringEncryptor stringEncryptor(){

        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(StringUtils.hasText(this.password)?this.password:DEFAULT_PASSWORD);
        config.setPoolSize("1");
        config.setAlgorithm("PBEWithMD5AndDES");
        config.setStringOutputType("base64");
        config.setKeyObtentionIterations("1000");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        encryptor.setConfig(config);
        return encryptor;
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}