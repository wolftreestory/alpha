package com.alpha.base.aspect;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alpha.base.context.PageContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@ConditionalOnWebApplication
public class PageAspect {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Autowired
	private PageContext pagingContext;
	
	@Before(value="@annotation(com.alpha.base.annotation.PageHandler)")
	public void before(JoinPoint joinPoint) throws Throwable {
		if(this.pagingContext==null) {return;}
				
		ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();	
		HttpServletRequest request = attributes.getRequest();
		List<String> parameterList = Collections.list(request.getParameterNames());
				
		this.pagingContext.set(key->parameterList.contains(key),key->request.getParameter(key));
		
		if(log.isDebugEnabled()) {
			log.debug(">> pageContext:{}",this.pagingContext.toString());
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}


