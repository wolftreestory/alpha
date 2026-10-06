package com.alpha.base.config;

import java.util.Collections;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableTransactionManagement
@ConditionalOnBean(PlatformTransactionManager.class)
@ConditionalOnExpression("T(com.alpha.base.TransactionConfig).isEnabled()")
public class TransactionConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final String AOP_TX_NAME = "alpha";
	private static final String AOP_TX_EXPRESSION = "execution(* com.alpha..service..*Impl.*(..))";
	
	private final TransactionManager txManager;

	public TransactionConfig(@Qualifier(DataSourceConfig.TRANSACTION_MANAGER) TransactionManager transactionManager) {
		this.txManager = transactionManager;
	}

    @Bean("default.txInterceptor")
    TransactionInterceptor txInterceptor() {
		RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
		attribute.setName(AOP_TX_NAME);
		attribute.setRollbackRules(Collections.singletonList(new RollbackRuleAttribute(Exception.class)));
		attribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

    	log.info(">> txAttribute.name: {}",attribute.getName());
    	log.info(">> txAttribute.rollbackRules: {}",attribute.getRollbackRules());
    	log.info(">> txAttribute.propagationBehavior: {}",attribute.getPropagationBehavior());

		MatchAlwaysTransactionAttributeSource attributeSource = new MatchAlwaysTransactionAttributeSource();
		attributeSource.setTransactionAttribute(attribute);
		
		return new TransactionInterceptor(this.txManager,attributeSource);
	}

    @Bean("default.txAdvisor")
    Advisor txAdvisor(@Qualifier("default.txInterceptor") TransactionInterceptor txInterceptor) {
		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(AOP_TX_EXPRESSION);
    	log.info(">> txPointcut.expression: {}",pointcut.getExpression());
		return new DefaultPointcutAdvisor(pointcut, txInterceptor);
	}

    @Aspect
    @Component
    @ConditionalOnProperty(name="alpha.transaction.log.enabled", havingValue="true", matchIfMissing=true)
    public class TransactionLoggingAspect {
    	
        @Before(AOP_TX_EXPRESSION)
        public void beforeTransaction(JoinPoint joinPoint) {
            log.info(">> Transaction[{}] Starting: {}",this.getTxName(), joinPoint.getSignature());
        }

        @AfterReturning(AOP_TX_EXPRESSION)
        public void afterCommit(JoinPoint joinPoint) {
            log.info(">> Transaction[{}] Committed: {}",this.getTxName(), joinPoint.getSignature());
        }

        @AfterThrowing(value = AOP_TX_EXPRESSION, throwing = "ex")
        public void afterRollback(JoinPoint joinPoint, Throwable ex) {
            log.error(">> Transactio[{}] Rolled Back: {} due to {}",this.getTxName(), joinPoint.getSignature(), ex.getMessage());
        }

        @After(AOP_TX_EXPRESSION)
        public void afterTransaction(JoinPoint joinPoint) {
            log.info(">> Transaction[{}] Ended: {}",this.getTxName(), joinPoint.getSignature());
        }
        
    	private String getTxName() {
        	String name = TransactionSynchronizationManager.getCurrentTransactionName();
    		return name != null ? name : "-";
    	}
        
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}