package com.alpha.base.support.aid;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.WebApplicationContextUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RequestAttributesAidPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public interface RequestAttributesAid {

		public ApplicationContext getApplicationContext();
		
		public void printApplicationContextInfo();
		
		public Logger getLogger(Class<?> clazz);
		
		public ServletRequestAttributes getServletRequestAttributes();
		
		public HttpServletRequest getHttpServletRequest();

		public HttpServletResponse getHttpServletResponse();

		public HttpSession getHttpSession();
		
		public ServletContext getServletContext();
		
		public WebApplicationContext getWebApplicationContext();
		
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static RequestAttributesAid getRequestAttributesAid() {
		
		return new RequestAttributesAid() {
			
			@Override
			public ServletRequestAttributes getServletRequestAttributes() {
				return (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
			}
			
			@Override
			public HttpServletRequest getHttpServletRequest() {				
				return this.getServletRequestAttributes().getRequest();
			}
			
			@Override
			public HttpServletResponse getHttpServletResponse() {				
				return this.getServletRequestAttributes().getResponse();
			}
			
			@Override
			public HttpSession getHttpSession() {
				return this.getHttpServletRequest().getSession();
			}
			
			@Override
			public ServletContext getServletContext() {
				return this.getHttpSession().getServletContext();
			}
			
			@Override
			public WebApplicationContext getWebApplicationContext() {
				return WebApplicationContextUtils.getWebApplicationContext(getServletContext());
			}
			
			@Override
			public ApplicationContext getApplicationContext() {
				return (ApplicationContext)this.getWebApplicationContext();
			}
			
			@Override
			public void printApplicationContextInfo() {
				ApplicationContext applicationContext=getApplicationContext();
				log.debug(">>applicationContext.getId: {}",applicationContext.getId());
				log.debug(">>applicationContext.getApplicationName: {}",applicationContext.getApplicationName());
				log.debug(">>applicationContext.getDisplayName: {}",applicationContext.getDisplayName());
				log.debug(">>applicationContext.BeanDefinitionCount: {}",applicationContext.getBeanDefinitionCount());
				log.debug(">>applicationContext.toString: {}",applicationContext.toString());
				log.debug(">>ServletContext: {}",getServletContext().toString());
				log.debug(">>HttpServletRequest: {}",getHttpServletRequest().toString());
				log.debug(">>HttpSession: {}",getHttpSession().toString());
			}	
			
			@Override
			public Logger getLogger(Class<?> clazz) {
				return LoggerFactory.getLogger(clazz);
			}	
		};
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
