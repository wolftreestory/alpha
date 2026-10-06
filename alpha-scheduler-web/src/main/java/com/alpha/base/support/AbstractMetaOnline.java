package com.alpha.base.support;


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

public abstract class AbstractMetaOnline extends AbstractMeta {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final AlertUtil getAlertUtil() {return MetaUtil.getAlertUtil();}
	public static final BashProcessUtil getBashProcessUtil() {return MetaUtil.getBashProcessUtil();}    
	public static final Base64Util getBase64Util() {return MetaUtil.getBase64Util();}
	public static final CompressUtil getCompressUtil() {return MetaUtil.getCompressUtil();} 
	public static final CryptoUtil getCryptoUtil() {return MetaUtil.getCryptoUtil();} 
	public static final FileUtil getFileUtil() { return MetaUtil.getFileUtil();} 
	public static final HttpRequestUtil getHttpRequestUtil() {return MetaUtil.getHttpRequestUtil();}
	public static final JsonUtil getJsonUtil() {return MetaUtil.getJsonUtil();}
	public static final LobConverterUtil getLobConvertUtil() {return MetaUtil.getLobConvertUtil();}
	public static final LocaleUtil getLocaleUtil() {return MetaUtil.getLocaleUtil();} 
	public static final MaskUtil getMaskUtil() {return MetaUtil.getMaskUtil();} 	
	public static final MessageUtil getMessageUtil() {return MetaUtil.getMessageUtil();} 
	public static final ObjectMapperUtil getObjectMapperUtil() {return MetaUtil.getObjectMapperUtil();}
	public static final PropertiesUtil getPropertiesUtil() {return MetaUtil.getPropertiesUtil();}
	public static final RestTemplateUtil getRestTemplateUtil() {return MetaUtil.getRestTemplateUtil();}
	public static final SessionUtil getSessionUtil() {return MetaUtil.getSessionUtil();} 	
	public static final SystemUtil getSystemUtil() {return MetaUtil.getSystemUtil();}
	public static final ThreadUtil getThreadUtil() {return MetaUtil.getThreadUtil();}
	public static final TimeUtil getTimeUtil() {return MetaUtil.getTimeUtil();} 
    public static final TransactionUtil getTransactionUtil() {return MetaUtil.getTransactionUtil();}	
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}