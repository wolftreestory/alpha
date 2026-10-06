package com.alpha.base.support.exception.service.impl;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import com.alpha.base.BaseUtil;
import com.alpha.base.support.exception.service.SimpleMappingExceptionResolverService;
import com.alpha.base.support.exception.service.StoreExceptionHandlerService;
import com.alpha.base.support.exception.service.StoreExceptionSqlService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractSimpleMappingExceptionResolverServiceImpl extends SimpleMappingExceptionResolver implements SimpleMappingExceptionResolverService,InitializingBean {

	@Autowired
	private ApplicationContext applicationContext;
		
	private String intervalContextName;
	private StoreExceptionHandlerService storeExceptionHandler=null;
	
	private Class<? extends StoreExceptionSqlService> storeExceptionSqlClass;
	private StoreExceptionSqlService storeExceptionSql;
	private String storeExceptionSqlEnableYn="Y";
	
	public void setIntervalContextName(String intervalContextName) {this.intervalContextName=intervalContextName;}	
	public void setStoreExceptionHandler(StoreExceptionHandlerService handler) {this.storeExceptionHandler=handler;}	
	public void setStoreExceptionSqlClass(Class<? extends StoreExceptionSqlService> cls) {storeExceptionSqlClass = cls;}
	public void setStoreExceptionSqlEnableYn(String yn) {this.storeExceptionSqlEnableYn=yn;}
	
	///////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void afterPropertiesSet() throws Exception {
		if(this.storeExceptionSqlClass!=null) {
			this.storeExceptionSql = this.applicationContext.getBean(this.storeExceptionSqlClass);
			log.debug(">> storeExceptionMeta:{}",this.storeExceptionSql.getClass());
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
		this.print(exception);

		if(this.storeExceptionSql!=null && this.storeExceptionSqlEnableYn.equals("Y")) {
			request.setAttribute("exceptionOccuredSql", this.storeExceptionSql.getExceptionOccuredSql());
		}

		if(this.storeExceptionHandler!=null) {
			this.storeExceptionHandler.store(request, response, exception);
		}
		
		this.resetIntervalCheck(request);
				
		return super.resolveException(request, response, handler, exception);
	}

	@Override
	public void print(Exception exception) {
	    log.error(">>ERROR",exception);	
	}
		
	@SuppressWarnings("unchecked")
	private void resetIntervalCheck(HttpServletRequest request) {
		if(this.intervalContextName==null || this.intervalContextName.equals("")) {return;}
		if(request.getSession().getAttribute(this.intervalContextName)!=null) {
			Map<String,Long> requestStampMap = (Map<String,Long>)request.getSession().getAttribute(this.intervalContextName);
			requestStampMap.remove(BaseUtil.getHttpRequestUtil().getRequestPageURI(request));
		}
	}	
}
	