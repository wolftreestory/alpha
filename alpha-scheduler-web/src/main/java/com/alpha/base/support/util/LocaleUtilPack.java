package com.alpha.base.support.util;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.LocaleResolver;

import com.alpha.base.support.aid.AwareAidPack.ThreadLocalAid;
import com.alpha.base.support.aid.RequestAttributesAidPack.RequestAttributesAid;

public final class LocaleUtilPack {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface LocaleUtil {
		
		public Locale getLocale();
		public void setLocale(Locale locale);

	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static LocaleUtil getLocaleUtil(RequestAttributesAid requestAttributesAid) {
		
		return new LocaleUtil() {
			
			@Autowired(required=false)
			private LocaleResolver resolver;
			
			@Override
			public Locale getLocale() {
				if(this.resolver==null) {return Locale.getDefault();}
				Locale locale = null;
				if(requestAttributesAid.getServletRequestAttributes()!=null && this.resolver!=null) {			
					locale = this.resolver.resolveLocale(requestAttributesAid.getHttpServletRequest());
				}else{
					if(!ThreadLocalAid.getThreadLocalMap().containsKey("locale")) {ThreadLocalAid.getThreadLocalMap().put("locale",Locale.KOREAN);}			
					locale = (Locale)ThreadLocalAid.getThreadLocalMap().get("locale");
				}
				return locale;
			}

			@Override
			public void setLocale(Locale locale) {
				if(this.resolver==null) {return;}
				
				if(requestAttributesAid.getServletRequestAttributes()!=null && this.resolver!=null) {
					this.resolver.setLocale(requestAttributesAid.getHttpServletRequest(), requestAttributesAid.getHttpServletResponse(), locale);
				}else{
					ThreadLocalAid.getThreadLocalMap().put("locale",locale);
				}
			}
		};
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}