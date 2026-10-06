package com.alpha.base.config.batch.oracle;

import java.io.FileNotFoundException;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnExpression("T(com.alpha.base.BatchConfig).isEnabled()")
@ConditionalOnProperty(name="alpha.batch.task.logStoreType", havingValue="oracle", matchIfMissing=false)
@MapperScan(basePackages = "com.alpha.base.support.batch.mapper", sqlSessionFactoryRef = OracleMybatisConfig.SQL_SESSION_FACTORY)
public class OracleMybatisConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String SQL_SESSION_FACTORY = "task.history.oracle.sqlSessionFactory";

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    @Bean(SQL_SESSION_FACTORY)
    @ConditionalOnBean(name=OracleDataSourceConfig.DATA_SOURCE)
    SqlSessionFactory sqlSessionFactory(@Qualifier(OracleDataSourceConfig.DATA_SOURCE) DataSource dataSource) throws Exception {
    	
		Resource[] resources=null;
		try {
			resources=new PathMatchingResourcePatternResolver().getResources("classpath*:/mapper/base/**/*Mapper_oracle.xml");
		}catch(FileNotFoundException e){
			log.debug(">> [task.history.oracle] resources(*Mapper.xml) does not exist.");
			return null;
		}
		
		log.debug(">> {}",SQL_SESSION_FACTORY);

        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setConfigLocation(new PathMatchingResourcePatternResolver().getResource("classpath:/config/config-batchMyBatis.xml"));
        sessionFactory.setMapperLocations(resources);
        return sessionFactory.getObject();
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}