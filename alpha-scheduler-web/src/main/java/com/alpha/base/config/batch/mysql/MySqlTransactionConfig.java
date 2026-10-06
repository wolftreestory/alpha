package com.alpha.base.config.batch.mysql;

import java.util.Collections;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@Configuration
@EnableTransactionManagement
@ConditionalOnExpression("T(com.alpha.base.BatchConfig).isEnabled()")
@ConditionalOnBean(PlatformTransactionManager.class)
@ConditionalOnProperty(name="alpha.batch.task.logStoreType", havingValue="mysql", matchIfMissing=false)
public class MySqlTransactionConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final String AOP_TX_METHOD_NAME = "*";
	private static final String AOP_TX_EXPRESSION = "execution(* com.alpha..impl.*Impl.*(..))";
	
	private final TransactionManager txManager;

	public MySqlTransactionConfig(TransactionManager transactionManager) {
		this.txManager = transactionManager;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    TransactionInterceptor txInterceptor() {
		RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
		attribute.setName(AOP_TX_METHOD_NAME);
		attribute.setRollbackRules(Collections.singletonList(new RollbackRuleAttribute(Exception.class)));
		attribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		MatchAlwaysTransactionAttributeSource attributeSource = new MatchAlwaysTransactionAttributeSource();
		attributeSource.setTransactionAttribute(attribute);
		
		return new TransactionInterceptor(this.txManager,attributeSource);
	}

    @Bean
    Advisor txAdvisor() {
		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(AOP_TX_EXPRESSION);
		return new DefaultPointcutAdvisor(pointcut, txInterceptor());
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}