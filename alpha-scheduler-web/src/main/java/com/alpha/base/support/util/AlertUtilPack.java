package com.alpha.base.support.util;

import javax.servlet.http.HttpServletRequest;

import com.alpha.base.exception.BusinessException;
import com.alpha.base.exception.SystemException;
import com.alpha.base.support.aid.RequestAttributesAidPack.RequestAttributesAid;
import com.alpha.base.support.util.MessageUtilPack.MessageUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AlertUtilPack {
		
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public interface AlertTaglib {
		public String getJavaScriptAlertContent(Object message);
	}

	public interface AlertUtil {
		
		public static final String DEFAULT_ALERT_MESSAGE_KEY="DEFAULT_ALERT_MESSAGE_KEY";
		
	    public boolean isExistAlertMessage();	
		public String getAlertMessageKey();	
		public void setAlertMessage(String messageStr) throws Exception;
		
		public void setAlertMessageByCode(String messageCode) throws Exception;
		public void setAlertMessageByCode(String messageCode,String arg1) throws Exception;
		public void setAlertMessageByCode(String messageCode,String arg1,String arg2) throws Exception;
		public void setAlertMessageByCode(String messageCode,String arg1,String arg2,String arg3) throws Exception;
		
		public boolean setAlertMessageByException(Exception e) throws Exception;
		public boolean setAlertMessageByException(Exception e, String messageStr) throws Exception;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static AlertUtil getAlertUtil(RequestAttributesAid requestAttributesAid, MessageUtil messageUtil) {
	
		return new AlertUtil() {
			
			///////////////////////////////////////////////////////////////////////////////////////////////

			private void setAlertMessage(String Key, String message) throws Exception {
				this.setAlertMessage(requestAttributesAid.getHttpServletRequest(),Key,message);
			}
			
			private void setAlertMessage(HttpServletRequest request, String Key, String message) throws Exception {
				request.getSession().setAttribute(Key,message);
			}

			private boolean isExistAlertMessage(String Key) {
			    return requestAttributesAid.getHttpServletRequest().getSession().getAttribute(Key)==null?false:true;
			}

			///////////////////////////////////////////////////////////////////////////////////////////////

			@Override
			public String getAlertMessageKey() {
				return DEFAULT_ALERT_MESSAGE_KEY;
			}
			
		    @Override
		    public boolean isExistAlertMessage() {
		        return this.isExistAlertMessage(this.getAlertMessageKey());
		    }
		    
			@Override
			public void setAlertMessage(String messageStr) throws Exception {
				this.setAlertMessage(this.getAlertMessageKey(),messageStr);
			} 

			@Override
			public void setAlertMessageByCode(String messageCode) throws Exception {
				this.setAlertMessage(messageUtil.getMessage(messageCode));
			}

			@Override
			public void setAlertMessageByCode(String messageCode,String arg1) throws Exception {
				this.setAlertMessage(messageUtil.getMessage(messageCode, arg1));
			}
			
			@Override
			public void setAlertMessageByCode(String messageCode,String arg1,String arg2) throws Exception {
				this.setAlertMessage(messageUtil.getMessage(messageCode, arg1, arg2));
			}
			
			@Override
			public void setAlertMessageByCode(String messageCode,String arg1,String arg2,String arg3) throws Exception {
				this.setAlertMessage(messageUtil.getMessage(messageCode, arg1, arg2, arg3));		
			}

			@Override
			public boolean setAlertMessageByException(Exception e) throws Exception {
				return this.setAlertMessageByException(e,null);
			}
			
			@Override
			public boolean setAlertMessageByException(Exception e, String message) throws Exception {		
				log.debug(">>Exception Type: {}",e.getClass().toString());
				
				boolean isMessageException=false;
				if(e instanceof BusinessException) {isMessageException=true;}
				else if(e instanceof SystemException) {isMessageException=true;}

				boolean returnFlag=false;
				if(isMessageException) {
					String alertMessage="";
					if(e.getMessage()!=null && !e.getMessage().equals("")) {alertMessage=e.getMessage();}
					if(message!=null && !message.equals("")) {alertMessage=message;}		
					if(alertMessage.equals("")) {alertMessage=messageUtil.getMessage("W0000002");}

					if(!alertMessage.equals("")) {
						this.setAlertMessage(alertMessage);
						returnFlag=true;
					}
				}
				log.debug(">>returnFlag: {}",returnFlag);
				return returnFlag;
			}
		};
		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}