package com.alpha.base.support.etc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.AntPathMatcher;

//@Slf4j
public final class PathCheck {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean isRequestPathCheck(HttpServletRequest request, Set<String> set) {
		return isPathCheck(request,set==null?null:new ArrayList<>(set),null);
	}

	public static boolean isRequestPathCheck(HttpServletRequest request, Set<String> set, Map<String,String> map) {
		return isPathCheck(request,set==null?null:new ArrayList<>(set),map);
	}

	public static boolean isRequestPathCheck(HttpServletRequest request, List<String> list) {
		return isPathCheck(request,list,null);
	}

	public static boolean isRequestPathCheck(HttpServletRequest request, List<String> list, Map<String,String> map) {
		return isPathCheck(request,list,map);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static boolean isPathCheck(HttpServletRequest request, List<String> list, Map<String,String> map) {
		if(list==null){return false;}

		AntPathMatcher antPathMatcher = new AntPathMatcher();

		String context=(request.getContextPath().equals("/")?request.getContextPath():request.getContextPath()+"/");
		
		String requestMethod = request.getMethod();
		String requestURI = request.getRequestURI();		
		if(requestURI.lastIndexOf(";")!=-1){ 
			requestURI=requestURI.substring(0,requestURI.indexOf(";"));
		}

		String matchPattern,matchMethod;
		for(String pattern:list){			
			if(pattern==null || pattern.equals("")){continue;}
			
			matchPattern = (pattern.equals("/")?"":pattern);
			matchPattern = context+(matchPattern.startsWith("/")?matchPattern.substring(1):matchPattern);	
			
			if(antPathMatcher.match(matchPattern,requestURI)){
				if(map==null || map.isEmpty()) {return true;}
			
				matchMethod = map.get(matchPattern);				
				boolean isEnable=false;
				if(!isEnable && matchMethod==null) {isEnable=true;}
				if(!isEnable && matchMethod.equals("")) {isEnable=true;}
				if(!isEnable && matchMethod.indexOf(requestMethod)>-1) {isEnable=true;}
				if(isEnable) {
					//log.debug(">> match[pattern:{},requestURI:{},method:{}]",matchPattern,requestURI,requestMethod);			
					return true;
				}
			}
		}
		
		return false;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}