package com.alpha.base.support.util;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.json.JsonSanitizer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class JsonUtilPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface JsonUtil {

		public static final String JSON_ROOT_KEY="json";

		public Gson getGson();
		public String toJson(Object obj);
		public <T> T fromJson(String json, Class<T> type);
		public <T> T fromJson(String json, Type typeOfT);

		public boolean isJsonObject(Object object);
		public JSONObject getJsonObject(Map<String,Object> map);
		public JSONObject getJsonObject(String jsonString);

		public boolean isJsonArray(Object object);
		public JSONArray getJsonArray(List<Map<String, Object>> list);
		public JSONArray getJsonArray(String jsonString);

		public String getJsonString(Map<String,Object> map);
		public String getJsonString(List<Map<String, Object>> list);

		public Map<String,Object> getMap(Object obj);
		public Map<String,Object> getMap(JSONObject jsonObject);
	    public Map<String,Object> getMap(String jsonString);

	    public List<Map<String,Object>> getList(Object obj);
	    public List<Map<String,Object>> getList(JSONArray jsonArray);
	    public List<Map<String,Object>> getList(String jsonString);	

		public Map<String,String> getSerialMap(Object object);
		public Map<String,String> getSerialMap(String jsonRootKey,Object object);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static JsonUtil getJsonUtil() {
		return getJsonUtil(false);
	}
	
	public static JsonUtil getJsonUtil(boolean isLog) {
		
		return new JsonUtil() {
			
			@Override
			public Gson getGson() {
				return new GsonBuilder().disableHtmlEscaping().setLenient().create();
			}

			@Override
			public String toJson(Object obj) {
				return this.getGson().toJson(obj);
			}
			
			@Override
			public <T> T fromJson(String json, Class<T> type) {
				return this.getGson().fromJson(JsonSanitizer.sanitize(json), type);
			}
			
			@Override
			public <T> T fromJson(String json, Type typeOfT) {
				return this.getGson().fromJson(json, typeOfT);
			}

			@Override
			public boolean isJsonObject(Object object) {
				boolean isEnable=false;
				try {
					if(!isEnable && object instanceof org.json.simple.JSONObject) {isEnable=true;}
					if(!isEnable && object instanceof String) {isEnable=this.isJsonObject(new JSONParser().parse(String.valueOf(object)));}
				}catch (Exception e) {
					//if(isLog) {log.error("Not parsable: {}",object);}
				}
				return isEnable;
			}

			@Override
			public boolean isJsonArray(Object object) {
				boolean isEnable=false;
				try {
					if(!isEnable && object instanceof org.json.simple.JSONArray) {isEnable=true;}
					if(!isEnable && object instanceof String) {isEnable=this.isJsonArray(new JSONParser().parse(String.valueOf(object)));}
				}catch (Exception e) {
					//if(isLog) {log.error("Not parsable: {}",object);}
				}
				return isEnable;
			}

			@Override
			public JSONObject getJsonObject(Map<String,Object> map) {
				return new JSONObject(map);		
			}

			@Override
			public JSONObject getJsonObject(String jsonString) {
				if(!this.isJsonObject(jsonString)) {return null;}
				JSONObject jsonObject=null;
				try {
					JSONParser parser=new JSONParser();
					jsonObject=(JSONObject)parser.parse(jsonString);
				} catch (ParseException e) {
					if(isLog) {log.error("Not parsable: {}",jsonString);}
				}
				return jsonObject;
			}

			@SuppressWarnings("unchecked")
			@Override
			public JSONArray getJsonArray(List<Map<String, Object>> list) {
				JSONArray jsonArray=new JSONArray();
				for(Map<String, Object> map : list ) {
					jsonArray.add(this.getJsonObject(map));
				}
				return jsonArray;
			}
			
			@Override	
			public JSONArray getJsonArray(String jsonString) {
				if(!this.isJsonArray(jsonString)) {return null;}
				JSONArray jsonArray=null;
				try {
					JSONParser parser=new JSONParser();
					jsonArray=(JSONArray)parser.parse(jsonString);
				} catch (ParseException e) {
					if(isLog) {log.error("Not parsable: {}",jsonString);}
				}
				return jsonArray;
			}

			@Override
			public String getJsonString(Map<String, Object> map) {
				return this.getJsonObject(map).toJSONString();
			}

			@Override
			public String getJsonString(List<Map<String, Object>> list) {
				return this.getJsonArray(list).toJSONString();
			}

			@Override
			public Map<String,Object> getMap(Object obj) {
				return this.getMap(this.toJson(obj));
			}
			
			@Override
			public Map<String, Object> getMap(JSONObject jsonObject) {
				Map<String,Object> map=null;
				try {
					//To prevent server-side JSON injections, sanitize all data before serializing it to JSON
					String wellFormedJson = JsonSanitizer.sanitize(jsonObject.toJSONString());
					Type type = new TypeToken<Map<String,Object>>() {}.getType();
					map=this.getGson().fromJson(wellFormedJson,type);
				} catch (Exception e) {
					if(isLog) {log.error("Not convertable: {}",jsonObject.toJSONString());}
				}
		        return map;
			}
			
		    @Override
		    public Map<String,Object> getMap(String jsonString) {
		        if(jsonString==null || jsonString.equals("")) {return null;}
		        String modifiedJsonString=jsonString;

		        //if(isLog) {log.debug("try1: {}",modifiedJsonString);}
		        if(this.isJsonObject(modifiedJsonString)) {
		            return this.getMap(this.getJsonObject(modifiedJsonString));            
		        }
		        
		        modifiedJsonString=modifiedJsonString.replaceAll("'","\"");
		        //if(isLog) {log.debug("try2: {}",modifiedJsonString);}
		        if(this.isJsonObject(modifiedJsonString)) {
		            return this.getMap(this.getJsonObject(modifiedJsonString));  
		        }

		        modifiedJsonString=modifiedJsonString.replaceAll("\"",""); 
		        //modifiedJsonString=modifiedJsonString.replaceAll("\\ {","\\ {\"");
		        modifiedJsonString=modifiedJsonString.replaceAll(":","\":\"");
		        modifiedJsonString=modifiedJsonString.replaceAll(",","\",\"");
		        modifiedJsonString=modifiedJsonString.replaceAll("\\}","\"\\}");
		        
		        //if(isLog) {log.debug("try3: {}",modifiedJsonString);}
		        if(this.isJsonObject(modifiedJsonString)) {
		            return this.getMap(this.getJsonObject(modifiedJsonString));  
		        }

		        return null;
		    }
		    
			@Override
		    public List<Map<String,Object>> getList(Object obj){
		    	return this.getList(this.toJson(obj));
		    }
		    
			@Override
			public List<Map<String, Object>> getList(JSONArray jsonArray) {
				List<Map<String, Object>> list=new ArrayList<>();
				if(jsonArray!=null) {
					for(int i=0;i<jsonArray.size();i++) {
						Map<String, Object> map=this.getMap((JSONObject)jsonArray.get(i));
						list.add(map);
					}
				}
				return list;
			}	
			
			@Override
			public List<Map<String,Object>> getList(String jsonString) {
				List<Map<String,Object>> list=null;
				try {
					TypeToken<List<Map<String,Object>>> tokenType=new TypeToken<List<Map<String,Object>>>() {};			
					list=this.getGson().fromJson(jsonString,tokenType.getType());
				} catch (Exception e) {
					if(isLog) {log.error("Not convertable: {}",jsonString);}
				}
		        return list;
			}
			
	
			@Override
			public Map<String,String> getSerialMap(Object object) {	
				return this.getSerialMap(JSON_ROOT_KEY,object);
			}
			
			@Override
			public Map<String,String> getSerialMap(String jsonRootKey,Object object) {
				Object parseObj = object;
				if(object instanceof String) {
					try {
						parseObj = new JSONParser().parse((String)object);
					} catch (ParseException e) {
						if(isLog) {log.error("Not parsable: {}",object);}
					}
				}
				return this.makeSerialMap(jsonRootKey,parseObj);
			}
			
			private Map<String,String> makeSerialMap(String baseKey,Object object) {
				Map<String,String> serialMap=new HashMap<>();
				
				if(this.isJsonObject(object)) {
					//log.debug(">> isJsonObject");
					JSONObject jsonObject=(JSONObject)object;
					Iterator<?> keys=jsonObject.keySet().iterator();
					while(keys.hasNext()) {
						String key=(String)keys.next();
						
						String mainKey=baseKey+"."+key;
						Object value=jsonObject.get(key);
						if(value instanceof String) {
							//log.debug(">>>> Object String {}:{}",mainKey,(String)value);
							serialMap.put(mainKey,(String)value);
						} else {
							//log.debug(">>>> Object Object {}:{}",mainKey,(value==null?"":value.toString()));
							serialMap.put(mainKey,(value==null?"":value.toString()));
							serialMap.putAll(this.makeSerialMap(mainKey,value));
						}
					}
				}else if(this.isJsonArray(object)) {
					//log.debug(">> isJsonArray");
					JSONArray jsonArray=(JSONArray)object;
					for(int i=0;i<jsonArray.size();i++) {
						Object value=jsonArray.get(i);
						String mainKey=baseKey+"["+String.valueOf(i)+"]";
						if(value instanceof String) {
							//log.debug(">>>> Array String {}:{}",mainKey,(String)value);
							serialMap.put(mainKey,(String)value);
						} else {
							//log.debug(">>>> Array Object {}:{}",mainKey,value);
							serialMap.put(mainKey,value.toString());
							serialMap.putAll(this.makeSerialMap(mainKey,value));
						}
					}	
				}else{
					//log.debug(">> isString");
					//log.debug(">>>> String String {}:{}",baseKey,(object==null?"":object.toString()));
					serialMap.put(baseKey,(object==null?"":object.toString()));
				}
				
				return serialMap;
			}
			
		};
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}