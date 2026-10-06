package com.alpha.base.support.interceptor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.alpha.base.BaseUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UrlBlockCheckInterceptor implements HandlerInterceptor {
	
	private static List<String> noneCheckPatternList;
	
	private static String enableYn="Y";
	
	///////////////////////////////////////////////////////////////////////////////////////////////

	public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object paramObject) throws Exception {return this.preHandleProcess(request,response);} 
	public void postHandle(HttpServletRequest request,HttpServletResponse response,Object paramObject,ModelAndView paramModelAndView) throws Exception {} 
	public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object paramObject,Exception paramException) throws Exception {} 
	
	///////////////////////////////////////////////////////////////////////////////////////////////
	
	public void setNoneCheckPatternList(List<String> arg) {
		noneCheckPatternList=arg;
	} 
	
	public void setEnableYn(String enableYnStr) {
		enableYn=enableYnStr;
	}
		
	private boolean preHandleProcess(HttpServletRequest request,HttpServletResponse response) throws Exception {	
		if(!enableYn.equals("Y")) {return true;}
		if(!BaseUtil.getHttpRequestUtil().isRequestPathCheck(request,noneCheckPatternList)) {
			log.debug(">> 차단된 URL입니다. request.getRequestURI(): {}",request.getRequestURI());
			
			String contextPath=BaseUtil.getHttpRequestUtil().getServletContext().getContextPath();
			if(!StringUtils.hasText(contextPath)) {contextPath="/";}
			response.sendRedirect(contextPath);
			return false;
		} 
		return true;
	} 
	
}