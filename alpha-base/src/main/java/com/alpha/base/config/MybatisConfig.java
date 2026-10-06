package com.alpha.base.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnExpression("T(com.alpha.base.MybatisConfig).isEnabled()")
@ConditionalOnProperty(prefix="spring.datasource.hikari",name="jdbc-url")
@ConditionalOnBean(name=DataSourceConfig.DATA_SOURCE)
@MapperScan(basePackages="com.alpha.**.mapper", sqlSessionFactoryRef=MybatisConfig.SQL_SESSION_FACTORY)
public class MybatisConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final String SQL_SESSION_FACTORY = "default.sqlSessionFactory";
    public static final String SQL_SESSION_TEMPLATE = "default.sqlSessionTemplate";

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ApplicationContext applicationContext;
	
	public MybatisConfig(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

    @Bean(SQL_SESSION_FACTORY)
    SqlSessionFactory sqlSessionFactory(@Qualifier(DataSourceConfig.DATA_SOURCE) DataSource dataSource) throws Exception {

		String[] patterns = {
    			"classpath:mapper/**/*Mapper.xml"
    			,"classpath:mappers/**/*Mapper.xml"
    			,"classpath:sqlMap/**/*Mapper.xml"
    			,"classpath:sql/**/*Mapper.xml"
    			};

    	List<Resource> list = this.getResource(patterns);
		if(list==null || list.isEmpty()) {
			log.info(">> resources(*Mapper.xml) does not exist.");
			return null;
		}

		//log.info(">> sqlSessionFactory: {}",SQL_SESSION_FACTORY);
		SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();		
		sessionFactory.setDataSource(dataSource);
		sessionFactory.setConfigLocation(applicationContext.getResource("classpath:mybatis-config.xml"));
		sessionFactory.setMapperLocations(list.toArray(new Resource[0]));
		return sessionFactory.getObject();
    }

    @Bean(SQL_SESSION_TEMPLATE)
    SqlSessionTemplate sqlSessionTemplate(@Qualifier(MybatisConfig.SQL_SESSION_FACTORY) SqlSessionFactory sqlSessionFactory) {
		if(sqlSessionFactory==null) {
			log.info(">> sqlSessionFactory is null.");
			return null;
		}
		//log.info(">> sqlSessionTemplate: {}",SQL_SESSION_TEMPLATE);		
		return new SqlSessionTemplate(sqlSessionFactory);
	}

    
    private List<Resource> getResource(String... patterns) {
    	if(patterns==null || patterns.length==0) {return null;}

    	List<Resource> list = new ArrayList<>();
    	Arrays.stream(patterns).forEach(pattern->{
    		Resource[] resources = null;
    		try {
    			resources = new PathMatchingResourcePatternResolver().getResources(pattern);
    		} catch(Exception e){
    			//log.info(">> {}",e.getMessage());
    		} 
    		if(resources!=null) {
    			log.info(">> sqlSessionFactory.mapperLocations: {}",pattern);
    			list.addAll(Arrays.asList(resources));
    		}
    	});
    	return list;	
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
	Mapper interface class : 
		매핑 파일이나 어노테이션에 정의한 SQL에 대응하는 자바 인터페이스.
		Mapper 인터페이스의 구현 클래스를 프락시로 인스턴스화 하기 때문에(자동으로 생성) 개발자는 Mapper 인터페이스의 구현 클래스를 작성할 필요 없음.

	SQL Mapping File(XML) :
	 	SQL과 객체의 매핑 정의를 기술하는 XML 파일. SQL을 어노테이션에 지정하는 경우에는 사용하지 않는다.
	 	해당 파일을 SqlSession 객체가 참조한다.
	
	SqlSession : 
		SQL 발행이나 트랜잭션 제어용 API를 제공하는 컴포넌트. 스프링 프레임워크에서 사용하는 경우에는 마이바티스 측의 트랜잭션 제어 API는 사용하지 않음.
		SqlSession이 Mapper 파일에서 SQL을 수행하고 결과 데이터를 반환하는 역할을 한다.

	SqlSessionFactory : 
		SqlSession을 생성하기 위한 컴포넌트. [SqlSessionFactoryBuilder:빌더패턴, SqlSessionFactoryBean:스프링의 DI 컨테이너에서 Bean]
		
	SqlSessionTemplate : 
		스프링 트랜잭션 관리하에 마이바티스 표준의 SqlSession을 취급하기 위한 컴포넌트로 스레드 안전하게 구현됨.
		(SqlSession은 트랜잭션 처리를 위해 commit/rollback 메소드를 명시적으로 호출해야 함)
		SqlSession 인터페이스를 구현하고 있으며 SqlSession으로 동작(실제 처리는 마이바티스의 표준의 SqlSession에 위임)하는 것도 지원.

	MapperFactoryBean : 
		스프링 트랜잭션 관리하에 SQL을 실행하는 Mapper객체를 빈으로 생성하기 위한 컴포넌트.
		스프링의 DI 컨테이너에서 Bean으로 취급할 수 있으므로 임의의 빈에 주입해서 SQL을 실행하게 함.
	**/
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}