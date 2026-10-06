package com.alpha.base.support;

import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import com.alpha.base.support.aid.AwareAidPack.ApplicationContextAid;
import com.alpha.base.support.aid.AwareAidPack.ThreadLocalAid;
import com.alpha.base.support.util.AlertUtilPack.AlertUtil;
import com.alpha.base.support.util.Base64UtilPack.Base64Util;
import com.alpha.base.support.util.BashProcessUtilPack.BashProcessUtil;
import com.alpha.base.support.util.CompressUtilPack.CompressUtil;
import com.alpha.base.support.util.CryptoUtilPack.CryptoUtil;
import com.alpha.base.support.util.FileUtilPack.FileUtil;
import com.alpha.base.support.util.HttpRequestUtilPack.HttpRequestUtil;
import com.alpha.base.support.util.JsonUtilPack.JsonUtil;
import com.alpha.base.support.util.LobConverterUtilPack.LobConverterUtil;
import com.alpha.base.support.util.LocaleUtilPack.LocaleUtil;
import com.alpha.base.support.util.MaskUtilPack.MaskUtil;
import com.alpha.base.support.util.MessageUtilPack.MessageUtil;
import com.alpha.base.support.util.ObjectMapperUtilPack.ObjectMapperUtil;
import com.alpha.base.support.util.PropertiesUtilPack.PropertiesUtil;
import com.alpha.base.support.util.RestTemplateUtilPack.RestTemplateUtil;
import com.alpha.base.support.util.SessionUtilPack.SessionUtil;
import com.alpha.base.support.util.SystemUtilPack.SystemUtil;
import com.alpha.base.support.util.ThreadUtilPack.ThreadUtil;
import com.alpha.base.support.util.TimeUtilPack.TimeUtil;
import com.alpha.base.support.util.TransactionUtilPack.TransactionUtil;

public abstract class AbstractMeta {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected final class MetaUtil extends AbstractMetaUtil {};

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final ApplicationContext getApplicationContext() {return MetaUtil.getApplicationContext();}	
	public static final Map<String,Object> getThreadLocalMap() {return MetaUtil.getThreadLocalMap();}	
	public static final <T> T getBean(Class<T> type) {return MetaUtil.getBean(type);}	
	public static final <T> T getBean(String beanId, Class<T> type) {return MetaUtil.getBean(beanId, type);}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private abstract static class AbstractMetaUtil {
		public static ApplicationContext getApplicationContext() {return ApplicationContextAid.getApplicationContext();}
		public static Map<String,Object> getThreadLocalMap() {return ThreadLocalAid.getThreadLocalMap();}
		public static <T> T getBean(Class<T> type) {return getBean(null,type);}
		public static <T> T getBean(String beanId, Class<T> type) {return StringUtils.hasText(beanId)?getApplicationContext().getBean(beanId,type):getApplicationContext().getBean(type);}

		// com.alpha.base.config.AidBaseConfig에 정의된 bean에 대한 method 등록		
		public static final AlertUtil getAlertUtil() {return getBean(AlertUtil.class);}
		public static final BashProcessUtil getBashProcessUtil() {return getBean(BashProcessUtil.class);}		
		public static final Base64Util getBase64Util() {return getBean(Base64Util.class);}
		public static final CompressUtil getCompressUtil() {return getBean(CompressUtil.class);}
		public static final CryptoUtil getCryptoUtil() {return getBean(CryptoUtil.class);}
		public static final FileUtil getFileUtil() { return getBean(FileUtil.class);}
		public static final HttpRequestUtil getHttpRequestUtil() {return getBean(HttpRequestUtil.class);}
		public static final JsonUtil getJsonUtil() {return getBean(JsonUtil.class);}
		public static final LobConverterUtil getLobConvertUtil() {return getBean(LobConverterUtil.class);}
		public static final LocaleUtil getLocaleUtil() {return getBean(LocaleUtil.class);}
		public static final MaskUtil getMaskUtil() {return getBean(MaskUtil.class);}		
		public static final MessageUtil getMessageUtil() {return getBean(MessageUtil.class);}
		public static final ObjectMapperUtil getObjectMapperUtil() {return getBean(ObjectMapperUtil.class);}
		public static final PropertiesUtil getPropertiesUtil() {return getBean(PropertiesUtil.class);}
		public static final RestTemplateUtil getRestTemplateUtil() {return getBean(RestTemplateUtil.class);}
		public static final SessionUtil getSessionUtil() {return getBean(SessionUtil.class);}
		public static final SystemUtil getSystemUtil() {return getBean(SystemUtil.class);}
		public static final ThreadUtil getThreadUtil() {return getBean(ThreadUtil.class);}
		public static final TimeUtil getTimeUtil() {return getBean(TimeUtil.class);}
	    public static final TransactionUtil getTransactionUtil() {return getBean(TransactionUtil.class);}
	} 

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}