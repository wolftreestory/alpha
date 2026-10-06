package com.alpha.base.context;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;

import com.alpha.base.support.AbstractMeta;
import com.alpha.base.support.aid.BeanAidPack;
import com.alpha.base.support.aid.RequestAttributesAidPack.RequestAttributesAid;
import com.alpha.base.support.util.JsonUtilPack;
import com.alpha.base.support.util.PropertiesUtilPack;
import com.alpha.base.support.util.JsonUtilPack.JsonUtil;
import com.alpha.base.support.util.PropertiesUtilPack.PropertiesUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnExpression("T(com.alpha.base.MaskContext).isEnabled()")
@ConditionalOnWebApplication
public class MaskContext extends AbstractMeta {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private interface BaseToken {	
		public String getName();
		public String getValue();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Component
	@ConfigurationProperties(prefix="alpha.mask")
	public static class MaskProperties{		
		private final String defaultProperties="classpath:mask/mask-default";
		
		private boolean enabled = false;
		private String ruleBase = "maskRule";
		private long maskTokenExpireTime = 30L; //단위:초
		private String spelBase = "T(com.alpha.base.BaseUtil).getMaskUtil()";		
		private List<String> properties;
		
		@PostConstruct
		private void postConstruct() {
			if(this.properties==null) {this.properties = new ArrayList<>();}
			if(!this.properties.contains(this.defaultProperties)) {this.properties.add(this.defaultProperties);}
		}
		
		public String getDefaultProperties() {return this.defaultProperties;}
		
		public boolean isEnabled() {return enabled;}
		public void setEnabled(boolean enabled) {this.enabled = enabled;}
		
		public String getRuleBase() {return ruleBase;}
		public void setRuleBase(String ruleBase) {this.ruleBase = ruleBase;}
		
		public long getMaskTokenExpireTime() {return maskTokenExpireTime;}
		public void setMaskTokenExpireTime(long maskTokenExpireTime) {this.maskTokenExpireTime = maskTokenExpireTime;}
		
		public String getSpelBase() {return spelBase;}
		public void setSpelBase(String spelBase) {this.spelBase = spelBase;}
		
		public List<String> getProperties() {return properties;}
		public void setProperties(List<String> properties) {this.properties = properties;}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Autowired
	private RequestAttributesAid requestAttributesAid;

	@Autowired
	private MaskProperties props;

	private SpelExpressionParser spelParser;
	private Map<String,Map<String,String>> maskRule;

	private String[] defaultMaskRuleId;
	
	private JsonUtil jsonUtil;
	private PropertiesUtil propertiesUtil;

	private final String maskTokenMeta="*alpha123456789*";
	private final String maskTokenName="_maskToken";
	private final String maskApplyAttribute="_maskApplyAttribute";
	private final TypeReference<List<Object>> listTypeReference = new TypeReference<List<Object>>(){};
	private final TypeReference<Map<String,Object>> mapTypeReference = new TypeReference<Map<String,Object>>(){};

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@PostConstruct
	private void postConstruct() throws Exception {
		if(!this.isMaskSupport()) {return;}
			
		this.jsonUtil = JsonUtilPack.getJsonUtil(false);
		this.propertiesUtil= PropertiesUtilPack.getPropertiesUtil(BeanAidPack.getBeanAid(), this.jsonUtil, "");

		this.maskRule = new ConcurrentHashMap<>();
		this.spelParser = new SpelExpressionParser();
		
		List<String> list = this.props.getProperties();
		if(list==null) {return;}
		
		for(String resource:list) {
			log.debug(">> maskProperties:{}",resource);
			
			Properties properties = this.getProperties(resource);
			if(properties==null || properties.isEmpty()) {continue;}
			
			String key,value;
			
			Iterator<String> keys = properties.stringPropertyNames().iterator();
			while(keys.hasNext()) {
		      key = keys.next(); 
		      value = properties.getProperty(key);
		      log.debug(">> {}:{}",key,value);
		      this.maskRule.put(key,this.buildMaskRule(this.props.getRuleBase(), value));
		    }

			if(resource.equals(this.props.getDefaultProperties())) {
				this.defaultMaskRuleId = properties.stringPropertyNames().stream().sorted().toArray(String[]::new);
				//log.debug(">> defaultMaskRuleId:{}",Arrays.toString(this.defaultMaskRuleId));
			}
		}
		
	    log.debug(">> maskRule: {}",maskRule.keySet());
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean isMaskSupport() {
		return this.props.isEnabled();
	}
	
	public void setMaskApply(boolean isApply) {
		if(this.isMaskSupport()) {
			
			this.requestAttributesAid.getHttpServletRequest().setAttribute(this.maskApplyAttribute,isApply);
		}
	}
	
	//용도: @MaskHandler없이 maskUtil을 사용한 경우 HttpServletRequest header에서 maskToken확인처리
	public boolean isMaskApply() {
		if(!this.isMaskSupport()) {return false;}
		
		HttpServletRequest request = this.requestAttributesAid.getHttpServletRequest();
		Object attribute = request.getAttribute(this.maskApplyAttribute);
		if(attribute!=null) {return (boolean)attribute;}

		return this.isMaskApply(request.getHeader(this.maskTokenName));
	}

	//용도: @MaskHandler에 대한 처리시 ServerHttpRequest header에서 maskToken확인처리 (for MaskAdvice)
	public boolean isMaskApply(ServerHttpRequest request) {
		if(!this.isMaskSupport()) {return false;}
		
		return this.isMaskApply(request.getHeaders().getFirst(this.maskTokenName));
	}
	
	public BaseToken createMaskToken() {

		String uuid = UUID.randomUUID().toString().replace("-", "");
		String maskTokenMeta = uuid+":"+MetaUtil.getCryptoUtil().getAES256().encryption(this.maskTokenMeta,uuid);
		String maskTokenKey = maskTokenMeta+"@"+MetaUtil.getSystemUtil().getServerInstanceCode();
		//log.debug(">> maskTokenMeta:{}",maskTokenMeta);

		BaseToken token = new BaseToken() {
			@Override public String getName() {return maskTokenName;}
			@Override public String getValue() {return maskTokenKey;}			
		};
		
		return token;
	}

	public Object doMask(Object body, String maskRuleId[]) {
        //log.debug(">> bodyType:{}",body.getClass().getTypeName());
        //log.debug(">> maskRuleId:{}",Arrays.toString(maskRuleId));

		Object data = null;
		if(data==null) {data = this.jsonUtil.getList(body);}
		if(data==null) {data = this.jsonUtil.getMap(body);}
		if(data==null) {data = body;}
		
		Map<String,String> maskRule = this.getMaskRule(maskRuleId);

		if(maskRule==null || maskRule.isEmpty()) {return body;}
		
		return this.doMask(data, maskRule, this.props.getRuleBase());
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean isMaskApply(String maskTokenKey) {

		if(maskTokenKey==null || maskTokenKey.equals("")) {return true;}

		//[check] maskTokenMeta
		if(maskTokenKey.indexOf("@")>-1 && maskTokenKey.indexOf(":") < maskTokenKey.indexOf("@")) {
			String maskTokenMeta = maskTokenKey.substring(0,maskTokenKey.indexOf("@"));
			log.debug(">> maskTokenMeta:{}",maskTokenMeta);
			
			String uuid = maskTokenMeta.substring(0,maskTokenMeta.indexOf(":"));
			String data= maskTokenMeta.substring(maskTokenMeta.indexOf(":"));
			if(this.maskTokenMeta.equals(MetaUtil.getCryptoUtil().getAES256().decryption(data, uuid))) {
				this.setMaskApply(false);
				return false;
			}
		}

		this.setMaskApply(true);		
		return true;
	}

	private Object doMask(Object data, Map<String,String> maskRuleMap, String maskRuleKey) {
		
		if(data instanceof List) {
			//log.debug(">> List");
			List<Object> list = MetaUtil.getObjectMapperUtil().convert(data,this.listTypeReference);
			for(int i=0;i<list.size();i++) {list.set(i, this.doMask(list.get(i), maskRuleMap, maskRuleKey+"[*]"));}
			return list;
		}
		
		if(data instanceof Map) {
			//log.debug(">> Map");
			Map<String,Object> map = MetaUtil.getObjectMapperUtil().convert(data,this.mapTypeReference);	
			for(String key:map.keySet()) {map.put(key, this.doMask(map.get(key), maskRuleMap, maskRuleKey+"."+key));}
			return map;
		}

		if(maskRuleMap.containsKey(maskRuleKey)) {
			String maskSpel = maskRuleMap.get(maskRuleKey);
			Boolean isEnable = true;
			if(isEnable && maskSpel==null) {isEnable=false;}
			if(isEnable && maskSpel.equals("")) {isEnable=false;}
			if(isEnable && maskSpel.indexOf("@data")==-1) {isEnable=false;}			
			if(isEnable) {
				if(isEnable && maskSpel.startsWith("T")) {isEnable=false;}
				if(isEnable && maskSpel.indexOf(this.props.getSpelBase())!=-1) {isEnable=false;}
				if(isEnable) {maskSpel=this.props.getSpelBase()+"."+maskSpel;}	
				
				maskSpel = maskSpel.replace("@data",String.valueOf(data));
				data = this.spelParser.parseExpression(maskSpel).getValue(String.class);
				//log.debug(">> {}:{}",maskRuleKey,data);
			}
		} 

		return data;
	}	

	private Properties getProperties(String resource) throws Exception {
		
		if(!resource.endsWith(".properties")) {resource=resource+".properties";}
		
		Properties properties = this.propertiesUtil.getProperties(new URL(resource));
		if(properties==null || properties.isEmpty()) {return null;}
		
		Set<String> keySet = properties.stringPropertyNames();
		
		String key1,key2;
		String value;

		Iterator<String> iterator1 = keySet.iterator();
		while(iterator1.hasNext()) {
			key1 = iterator1.next();   
			value = properties.getProperty(key1);
			if(!value.matches(".*[$]\\{.*\\}.*")) {continue;}
			
			Iterator<String> iterator2 = keySet.iterator();
			while(iterator2.hasNext()) {
				key2 = iterator2.next();
				if(key2.equals(key1)) {continue;}
				value = value.replace("${"+key2+"}",properties.getProperty(key2));				
				properties.replace(key1, value);				
				if(!value.matches(".*[$]\\{.*\\}.*")) {break;}
			}
	    }
	    
	    return properties;
	}
	
	private Map<String,String> buildMaskRule(String ruleBase, Object value){
		Map<String,String> map = this.jsonUtil.getSerialMap(ruleBase, value);

		String keys[] = map.keySet().stream().sorted().toArray(String[]::new);
		for(String key:keys) {
			if(key.indexOf("[0]")==-1){continue;}
			map.put(key.replace("[0]","[*]"), map.get(key));
			map.remove(key);
		}
	    return map;
	}
	
	private Map<String,String> getMaskRule(String maskRuleId[]){

		if(maskRuleId==null || Arrays.toString(maskRuleId).equals("[]")) {
			maskRuleId=this.defaultMaskRuleId;
		}
		
		String key = maskRuleId.length>1?Arrays.toString(maskRuleId):maskRuleId[0];		
		log.debug(">> maskRuleId:{}",key);
		
		if(!this.maskRule.containsKey(key)) {
			Map<String,String> map = new HashMap<>();			
			Arrays.stream(maskRuleId)
			.filter(s->this.maskRule.get(s)==null?false:true)
			.forEach(s -> map.putAll(this.maskRule.get(s)));
			if(!map.isEmpty()) {this.maskRule.put(key, map);}
		}

		return this.maskRule.get(key);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}