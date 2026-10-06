package com.alpha.base.support.util;

import java.util.Locale;

import org.springframework.context.MessageSource;

import com.alpha.base.support.util.LocaleUtilPack.LocaleUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MessageUtilPack {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface MessageUtil {

		public static final String messageSourceName="messageSource";

		public String getMessage(String messageCode);		
		public String getMessage(String messageCode,String arg1);
		public String getMessage(String messageCode,String arg1,String arg2);
		public String getMessage(String messageCode,String arg1,String arg2,String arg3);
		public String getMessage(String messageCode,Object[] paramArrayOfObject);
		public String getMessage(String messageCode,Object[] paramArrayOfObject, Locale paramLocale);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static MessageUtil getMessageUtil(LocaleUtil localeUtil, MessageSource messageSource) {
		if(localeUtil==null) {log.debug(">> param[localeUtil] is null."); return null;}
		if(messageSource==null) {log.debug(">> param[messageSource] is null."); return null;}
		
		return new MessageUtil() {

			@Override
			public String getMessage(String messageCode) {
				return this.getMessage(messageCode,"");
			}

			@Override
			public String getMessage(String messageCode,String arg1) {
				return this.getMessage(messageCode,arg1,null);
			}

			@Override
			public String getMessage(String messageCode,String arg1,String arg2) {
				return this.getMessage(messageCode,arg1,arg2,null);
			}

			@Override
			public String getMessage(String messageCode,String arg1,String arg2,String arg3) {
				return this.getMessage(messageCode,new String[] {arg1,arg2,arg3});
			}

			@Override
			public String getMessage(String messageCode,Object[] paramArrayOfObject) {
				return this.getMessage(messageCode, paramArrayOfObject, localeUtil.getLocale());
			}
				
			@Override
			public String getMessage(String messageCode,Object[] paramArrayOfObject, Locale paramLocale) {
				return messageSource.getMessage(messageCode, paramArrayOfObject, paramLocale);
			}
		};	
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}