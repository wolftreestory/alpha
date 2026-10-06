package com.alpha.base.aspect;

import java.util.Locale;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.alpha.base.context.PageContext;
import com.alpha.base.support.AbstractMeta;

@Aspect
@Component
@ConditionalOnExpression("T(com.alpha.base.KafkaConfig).isEnabled()")
@ConditionalOnProperty(name="spring.kafka.enabled", havingValue="true", matchIfMissing=false)
public class KafkaListenerAspect extends AbstractMeta {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Around(value="@annotation(com.alpha.base.annotation.KafkaListener)")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

		MetaUtil.getThreadLocalMap().put("locale",Locale.KOREAN);
		MetaUtil.getThreadLocalMap().put("pageContext",new PageContext());
		
		try {
			return joinPoint.proceed();			
		}catch(Exception e) {		
			throw e;
		}finally {		
			MetaUtil.getThreadLocalMap().remove("locale");		
			MetaUtil.getThreadLocalMap().remove("pageContext");
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}