package com.alpha.base.support.util;

import java.util.Enumeration;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.springframework.web.context.request.RequestContextHolder;

import com.alpha.base.support.aid.RequestAttributesAidPack.RequestAttributesAid;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SessionUtilPack  {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface SessionUtil {
	
		public boolean isEnableHttpSession();		
		public HttpSession getHttpSession();
	    public String getHttpSessionId();

	    public Object getAttribute(String name);	 
	    public void setAttribute(String name, Object object);
	    public void removeAttribute(String name);
	    
	    public void doInvalidate();
	    public void printSession(Logger logger);

	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static SessionUtil getSessionUtil(RequestAttributesAid requestAttributesAid) {
		if(requestAttributesAid==null) {log.debug(">> param[requestAttributesAid] is null."); return null;}

		return new SessionUtil() {

			@Override
			public boolean isEnableHttpSession() {
				return RequestContextHolder.getRequestAttributes()==null?false:true;
			}

			@Override
			public HttpSession getHttpSession() {
				return requestAttributesAid.getHttpServletRequest().getSession();
			}
			
			@Override
			public String getHttpSessionId() {
				return this.getHttpSession().getId();
		    }

			@Override
			public Object getAttribute(String name) {
				return this.getHttpSession().getAttribute(name);
		    }

			@Override
			public void setAttribute(String name, Object object) {
				this.getHttpSession().setAttribute(name, object);
		    }

			@Override
			public void removeAttribute(String name) {
				this.getHttpSession().removeAttribute(name);
		    }

			@Override
			public void doInvalidate() {
				this.getHttpSession().invalidate();
			}

			@Override
			public void printSession(Logger logger) {
				
			     String key;
			     Object value;
			     HttpSession session=this.getHttpSession();

		         logger.debug(">> Start Print Session");
		         requestAttributesAid.printApplicationContextInfo();
			     for (Enumeration<?> e=session.getAttributeNames() ; e.hasMoreElements() ;) {
			         key=(String)e.nextElement();
			         value=session.getAttribute(key);
			         logger.debug(">> key= {}, value= {}",key,value);
			     } 
		         logger.debug(">> End Print Session");
			}
			
		};
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}