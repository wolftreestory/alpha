package com.alpha.base.support.exception.service.impl;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.alpha.base.BaseUtil;
import com.alpha.base.support.exception.service.StoreExceptionHandlerService;
import com.alpha.base.support.exception.service.StoreExceptionMetaService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractStoreExceptionHandlerServiceImpl implements StoreExceptionHandlerService, InitializingBean  {

	@Autowired
	private ApplicationContext applicationContext;
	
	private String enableYn="Y";	
	private String executorBeanName=null;
	
	private long storeAsyncDelay = 100; //millisecond
	private String storeAsyncYn = "Y";
	private String[] sessionTraceKeys;	
	private String[] sessionTraceMdcKeys;
	
	private Class<? extends StoreExceptionMetaService> storeExceptionMetaClass;
	private StoreExceptionMetaService storeExceptionMeta;

	private ThreadPoolTaskExecutor executor=null;

	public void setEnableYn(String enableYn) {this.enableYn=enableYn;}
	public void setExecutorBeanName(String executorBeanName) {this.executorBeanName=executorBeanName;}	
	
	public void setStoreAsyncDelay(long storeAsyncDelay) {this.storeAsyncDelay = storeAsyncDelay;}
	public void setStoreAsyncYn(String storeAsyncYn) {this.storeAsyncYn = storeAsyncYn;}
	public void setSessionTraceKeys(String[] traceKeys) {sessionTraceKeys = traceKeys;}	
	public void setSessionTraceMdcKeys(String[] kyes) {sessionTraceMdcKeys = kyes;}
	
	public void setStoreExceptionMetaClass(Class<? extends StoreExceptionMetaService> cls) {storeExceptionMetaClass = cls;}
	
	///////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public void afterPropertiesSet() throws Exception {

		if(this.executor==null) {
			this.executor = this.applicationContext.getBean(this.executorBeanName,ThreadPoolTaskExecutor.class);
			if(this.executor==null) {log.debug(">> executor is null");return;}
		}

		if(this.storeExceptionMetaClass!=null) {
			this.storeExceptionMeta = this.applicationContext.getBean(this.storeExceptionMetaClass);
			log.debug(">> storeExceptionMeta:{}",this.storeExceptionMeta.getClass());
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void store(HttpServletRequest request, HttpServletResponse response,Exception exception) {
		if(!this.enableYn.equals("Y")) {return;}
		
		if(this.storeExceptionMeta==null) {log.debug(">> storeExceptionMeta is null");return;}
		
		//sessionMap
		Map<String,Object> metaMap = new HashMap<>();
		metaMap.put("rqstIpAddr",BaseUtil.getHttpRequestUtil().getRemoteAddress(request)); /*요청IP주소*/
		metaMap.put("prcsSrvrNm",BaseUtil.getSystemUtil().getServerName()); /*처리서버명*/
		metaMap.put("exceptionOccuredSql",request.getAttribute("exceptionOccuredSql")); /*오류SQL*/
		metaMap.put("mdcInfo",this.getMdcInfo(request)); /*오류SQL*/

		if(sessionTraceKeys!=null) {
			for(String traceKey : sessionTraceKeys){	
				metaMap.put(traceKey, request.getSession().getAttribute(traceKey));
			}
		}

		if(this.storeAsyncYn.equals("Y")) {
			this.executor.execute(new storeExceptionMetaRunnable(this.storeExceptionMeta,exception,this.storeAsyncDelay,metaMap));
		} else {
			this.storeExceptionMeta.doStore(exception,metaMap);
	    }
	}
	
	class storeExceptionMetaRunnable implements Runnable {
		private StoreExceptionMetaService storeExceptionMeta;
		private Exception exception;
		private Map<String,Object> metaMap;
		private long delay=-1;
		
		public storeExceptionMetaRunnable(StoreExceptionMetaService storeExceptionMeta,Exception exception,long delay,Map<String,Object> metaMap) {
			this.storeExceptionMeta = storeExceptionMeta;
			this.exception = exception;
			this.metaMap = metaMap;
			this.delay = delay;
		}
	
		@Override
		public void run() {
			if(this.delay>0) {
				try {Thread.sleep(this.delay);} 
				catch (InterruptedException e) {log.debug(e.toString());} 
			}
			
			@SuppressWarnings("unchecked")
			Map<String,String> mdcInfo = (Map<String,String>)this.metaMap.get("mdcInfo");
			if(mdcInfo!=null) {
				for(String checkKey : mdcInfo.keySet()){
					MDC.put(checkKey,mdcInfo.get(checkKey));
				}
			}
			
			this.storeExceptionMeta.doStore(this.exception,this.metaMap);
			return;
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////

	private Map<String,String> getMdcInfo(HttpServletRequest request){
		Map<String,String> map=null;
		if(this.sessionTraceMdcKeys!=null && this.sessionTraceMdcKeys.length!=0) {
			map=new HashMap<>();
			for(String checkKey : this.sessionTraceMdcKeys){
				String checkValue=(String)request.getSession().getAttribute(checkKey);
				if(checkValue!=null) {map.put(checkKey,checkValue);}
			}
		}
		return map;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////
	
}
	