package com.alpha.base.support.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.alpha.base.support.aid.BeanAidPack.BeanAid;
import com.alpha.base.support.util.JsonUtilPack.JsonUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PropertiesUtilPack  {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface PropertiesUtil {

	    public String getProperty(String key);
		public String getProperty(String propertiesBeanId,String key);

		public Properties getProperties();
		
		public Properties getProperties(File file);
		public Properties getProperties(String jsonString);
		public Properties getProperties(URL resourceUrl);

		public Properties getPropertiesThrouSystem(Properties refProps,String refStr);  
	    public Properties getPropertiesThrouSystem(String key,String value,String refStr);

		public String getPropertyParseValue(String key);
		public String getPropertyParseValue(Properties properties,String key);	
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static PropertiesUtil getPropertiesUtil(BeanAid beanAid, JsonUtil jsonUtil, String defaultProperties) {
		if(beanAid==null) {log.debug(">> param[beanAid] is null."); return null;}
		if(jsonUtil==null) {log.debug(">> param[jsonUtil] is null."); return null;}
		if(defaultProperties==null) {log.debug(">> param[defaultProperties] is null."); return null;}
		
		return new PropertiesUtil() {

		    @Override
			public String getProperty(String key) {
				return this.getProperty(defaultProperties,key);
			}
		    
		    @Override
			public String getProperty(String propertiesBeanId,String key) {
				return beanAid.getBean(propertiesBeanId,Properties.class).getProperty(key);
			}

		    @Override
			public Properties getProperties() {
				if(defaultProperties==null || defaultProperties.equals("")) {return null;}
				return beanAid.getBean(defaultProperties,Properties.class);
			}

			@Override
			public Properties getProperties(File file) {
				if(file == null) {throw new RuntimeException("file is null.");}
				if(!file.exists()) {throw new RuntimeException("file is not exist");}

				Properties prop = new Properties();
				try {
					prop.load(new FileInputStream(file));

					Enumeration<?> enumeration = prop.propertyNames();
					while(enumeration.hasMoreElements()) {
						String key = (String)enumeration.nextElement();
						String value = this.getPropertyParseValue(prop, key);
						prop.put(key, value);
					}

				} catch (Exception e) {
					throw new RuntimeException(e.getCause());
				}

				return prop;
			}
			
			@Override
			public Properties getProperties(String jsonString) {
				if(jsonString == null || jsonString.equals("")) {throw new RuntimeException("jsonString is null or blank");}

				Map<String, Object> propMap = null;
				if(jsonUtil.isJsonObject(jsonString)) {propMap=jsonUtil.getMap(jsonString);}
				if(propMap == null) {return null;}

				Properties prop = new Properties();
				Iterator<String> keys = propMap.keySet().iterator();
				while(keys.hasNext()) {
					String key = keys.next();
					String value = this.getPropertyParseValue(prop, key);
					prop.put(key, value);
				}

				return prop;
			}
			
			@Override
			public Properties getProperties(URL resourceUrl) {
				if(resourceUrl==null) {return null;}
				
				Properties properties=null;
				try {
					ClassLoader classLoader=Thread.currentThread().getContextClassLoader();
					try(InputStream is=classLoader.getResourceAsStream(resourceUrl.getPath())){
						try(BufferedInputStream buf=new BufferedInputStream(is);){
							properties=new Properties();
							properties.load(buf);
						}
					}
				}catch(Exception e) {
					log.error(">> exception skip: {}",e.getMessage());
					//e.printStackTrace();
				}
				
				return properties;
			}
			

			@Override
			public Properties getPropertiesThrouSystem(String key, String value, String refStr) {
				Properties props = new Properties();
				props.put(key, value);
				return this.getPropertiesThrouSystem(props, refStr);
			}

			@Override
			public Properties getPropertiesThrouSystem(Properties refProps, String refStr) {
				Properties mainProp = null;
				if(mainProp == null){mainProp = this.getProperties(refStr);}
				if(mainProp == null){mainProp = this.getProperties(new File(refStr));}
				if(mainProp == null){return null;}

				if(refProps != null) {
					Enumeration<?> refPropEnum = refProps.propertyNames();
					while (refPropEnum.hasMoreElements()) {
						String key = (String) refPropEnum.nextElement();
						mainProp.setProperty(key, refProps.getProperty(key));
					}
				}

				Enumeration<?> mainPropEnum = mainProp.propertyNames();
				while(mainPropEnum.hasMoreElements()) {
					String key = (String) mainPropEnum.nextElement();
					System.setProperty(key, mainProp.getProperty(key));
				}

				return mainProp;
			}

			@Override
			public String getPropertyParseValue(String key) {
				return this.getPropertyParseValue(beanAid.getBean(defaultProperties, Properties.class), key);
			}

			@Override
			public String getPropertyParseValue(Properties properties, String key) {
				if(properties == null) {return "";}
				if(key == null || key.equals("")) {return "";}

				String value = null;
				if(key.length() > 4 && key.startsWith("${") && key.endsWith("}")) {
					String propertyKey = key.substring(2, key.length() - 1);
					value = properties.getProperty(propertyKey);
					if(value == null || value.equals("")) {value = propertyKey;}
				} else {
					value = properties.getProperty(key);
					if(value == null || value.equals("")) {value = key;}
				}

				if(value.length() > 4 && value.startsWith("#{") && value.endsWith("}")) {
					String expression = value.substring(2, value.length() - 1);
					
					ExpressionParser parser = new SpelExpressionParser();
					Expression exp = parser.parseExpression(expression);
					value = exp.getValue().toString();
				}

				return value;
			}
		};
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}