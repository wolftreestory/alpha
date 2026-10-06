package com.alpha.base.support.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Clob;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.alpha.base.support.util.FileUtilPack.FileUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class LobConverterUtilPack {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface LobConverterUtil {

		public String doClobToString(Object object);
		public String doClobToString(Clob clob);
	    public void doClobToFileRender(Clob clob,String attachFileName,HttpServletRequest request,HttpServletResponse response) throws Exception;

	    public byte[] doBlobToBytes(Object object);
		public byte[] doBlobToBytes(Blob blob);
	    public void doBlobToFileRender(Blob blob,String attachFileName,HttpServletRequest request,HttpServletResponse response) throws Exception;

	    public Map<String,String> doGetMethodToMap(Object object);		
		public Object getSpelParsingValue(Object rootObj,String spel);	
		public Object getExtractValue(Object object,String refString);	
	}	
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static LobConverterUtil getLobConverterUtil(FileUtil fileUtil) {
		if(fileUtil==null) {log.debug(">> param[fileUtil] is null."); return null;}
		
		return new LobConverterUtil() {

			@Override
			public String doClobToString(Object object) {
				Clob clob=null;
				if(object instanceof Clob) {clob=(Clob)object;}
				if(clob==null) {return "";}		
				return this.doClobToString(clob);
			}

			@Override
			public String doClobToString(Clob clob) {
				if(clob==null) {return "";}
				
				StringBuilder outBuf=new StringBuilder();
				try {		
					String readLine="";
					try(BufferedReader bufReader=new BufferedReader(clob.getCharacterStream())){					
						while((readLine=bufReader.readLine())!=null) {outBuf.append(readLine).append("\n");}
					}
				}catch(Exception e) {
					log.error(">> exception skip: {}",e.getMessage());
					//e.printStackTrace();
				}

				return outBuf.toString();
			}

			@Override
		    public void doClobToFileRender(Clob clob,String attachFileName,HttpServletRequest request,HttpServletResponse response) throws Exception {
			    fileUtil.pushout(clob, attachFileName, request, response);
		    }

			@Override	
			public byte[] doBlobToBytes(Object object) {
				Blob blob=null;
				if(object instanceof Blob) {blob=(Blob)object;}
				if(blob==null) {return null;}
				return this.doBlobToBytes(blob);
			}
			
			@Override	
			public byte[] doBlobToBytes(Blob blob) {
				if(blob==null) {return null;}
				
				byte[] bytes=null;
				try {
					try(BufferedInputStream bufInputStream=new BufferedInputStream(blob.getBinaryStream())){
				        int blobSize=(int)blob.length();
				        bytes=new byte[blobSize];    
				        bufInputStream.read(bytes,0,blobSize);
					}
				}catch(Exception e) {
					log.error(">> exception skip: {}",e.getMessage());
					//e.printStackTrace();
				}
				return bytes;
			}

		   @Override
		    public void doBlobToFileRender(Blob blob,String attachFileName,HttpServletRequest request,HttpServletResponse response) throws Exception {
		       fileUtil.pushout(blob, attachFileName, request, response);
		    }

			@Override
			public Map<String,String> doGetMethodToMap(Object object) {
				if(object==null) {return null;}

		        Map<String,String> map=new HashMap<>();

		        Method[] methods=object.getClass().getDeclaredMethods();
		        for(Method method:methods) {
		        	if(!method.getName().startsWith("get")) {continue;}

		        	String value=null;  
		            try {
		            	Object methodInvokeOject=object.getClass().getDeclaredMethod(method.getName()).invoke(object);
		        		if(value==null && method.getReturnType().isPrimitive()) {value=String.valueOf(methodInvokeOject);}
		        		if(value==null && method.getReturnType().toString().indexOf("java.lang.String")!=-1) {value=(methodInvokeOject==null?"":methodInvokeOject.toString());}
		        		if(value==null && methodInvokeOject!=null) {
		            		Map<String,String> mapSub=this.doGetMethodToMap(methodInvokeOject);
		            		if(mapSub!=null) {value=mapSub.toString();}
		        		}
		            }catch(Exception e) {
		            	log.error(">> exception skip: {}",e.getMessage());
		            }
		            map.put(method.getName(),value);
		        }

		        return map;
			}

			@Override
			public Object getSpelParsingValue(Object rootObj,String spel) {
				ExpressionParser parser=new SpelExpressionParser();
				Expression exp=parser.parseExpression(spel);
				EvaluationContext context=new StandardEvaluationContext(rootObj);		
				return exp.getValue(context);
			}
			
			@Override	
			public Object getExtractValue(Object object,String refString) {
				if(object==null) {return null;}
				if(refString==null || refString.equals("")) {return null;}
				if(refString.indexOf(".")==-1) {return this.getSpelParsingValue(object,refString);}
				Object objectChild=this.getSpelParsingValue(object,refString.substring(0,refString.indexOf(".")));
				return this.getExtractValue(objectChild,refString.substring(refString.indexOf(".")+1,refString.length()));
			}
			
		};
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}