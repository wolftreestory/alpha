package com.alpha.base.support.aid;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public final class AwareAidPack {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class ApplicationContextAid implements ApplicationContextAware {
	
		private static ApplicationContext applicationContext;
		
	    public static ApplicationContext getApplicationContext() {
	        return applicationContext;
	    }
	
		@Override
	    public void setApplicationContext(ApplicationContext context) throws BeansException {
	    	applicationContext=context;
	    }		
	}

	public static class ThreadLocalAid  {

		private static ThreadLocal<Map<String,Object>> threadLocal = null;
	
		public static synchronized Map<String,Object> getThreadLocalMap() {
			if(threadLocal==null) {threadLocal = new ThreadLocal<>();}
			if(threadLocal.get()==null) {threadLocal.set(new HashMap<>());}
			return threadLocal.get();
		}
	
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static ApplicationContextAid getApplicationContextAid() {		
		return new ApplicationContextAid();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}