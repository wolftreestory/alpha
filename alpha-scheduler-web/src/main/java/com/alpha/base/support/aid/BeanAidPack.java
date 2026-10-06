package com.alpha.base.support.aid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BeanAidPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface BeanAid extends BeanNameAware, ApplicationContextAware{

		public ApplicationContext getApplicationContext();
		
		public void printApplicationContextInfo();
		
		public Logger getLogger(Class<?> clazz);
				
		public <T> T getBean(String beanId,Class<T> type);

		public <T> T getBean(Class<T> type);

		public Object getBean(String beanId);
		
		public String getBeanName();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static BeanAid getBeanAid() {
		
		return new BeanAid() {

			private ApplicationContext applicationContext=null;
			
			private String beanName=null;
			
			@Override
			public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
				this.applicationContext=applicationContext;
			}

			@Override
			public ApplicationContext getApplicationContext() {
				return applicationContext;
			}

			@Override
			public void setBeanName(String beanName) {
				this.beanName=beanName;
			}
			
			@Override
			public String getBeanName() {
				return this.beanName;
			}
			 
			@Override
			public <T> T getBean(String beanId, Class<T> type) {
				return getApplicationContext().getBean(beanId, type);
			}

			@Override
			public <T> T getBean(Class<T> type) {
				return getApplicationContext().getBean(type);		
			}
			
			@Override
			public Object getBean(String beanId) {
				return getApplicationContext().getBean(beanId);
			}

			@Override
			public void printApplicationContextInfo() {
				log.debug(">> applicationContext.getId: {}",applicationContext.getId());
				log.debug(">> applicationContext.getApplicationName: {}",applicationContext.getApplicationName());
				log.debug(">> applicationContext.getDisplayName: {}",applicationContext.getDisplayName());
				log.debug(">> applicationContext.BeanDefinitionCount: {}",applicationContext.getBeanDefinitionCount());
				log.debug(">> applicationContext.toString: {}",applicationContext.toString());
			}

			@Override			
			public Logger getLogger(Class<?> clazz) {
				return LoggerFactory.getLogger(clazz);
			}
		};
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}