package com.alpha.base.support.interceptor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.alpha.base.BaseUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoginCheckInterceptor implements HandlerInterceptor {

	private static List<String> noneCheckPatternList;
	
	///////////////////////////////////////////////////////////////////////////////////////////////

	public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object paramObject) throws Exception {return this.preHandleProcess(request,response);} 
	public void postHandle(HttpServletRequest request,HttpServletResponse response,Object paramObject,ModelAndView paramModelAndView) throws Exception {} 
	public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object paramObject,Exception paramException) throws Exception {} 
	
	///////////////////////////////////////////////////////////////////////////////////////////////
	
	public void setNoneCheckPatternList(List<String> arg) {
		noneCheckPatternList=arg;
	} 
	
	private boolean preHandleProcess(HttpServletRequest request,HttpServletResponse response) throws Exception {	
		if(BaseUtil.getHttpRequestUtil().isRequestPathCheck(request,noneCheckPatternList)) {return true;}
		
		if(!BaseUtil.getBean(LoginCheck.LOGIN_CHECK_BEAN_NAME,LoginCheck.class).isLogin()) {
			log.debug(">> 인증이 필요합니다. request.getRequestURI(): {}",request.getRequestURI());			
			String contextPath=BaseUtil.getHttpRequestUtil().getServletContext().getContextPath();			
			response.sendRedirect(contextPath);			
			return false;
		}
		return true;
	} 
	
	///////////////////////////////////////////////////////////////////////////////////////////////

	public static interface LoginCheck {
		public static final String LOGIN_CHECK_BEAN_NAME  = "defaultLoginCheckService";
		public boolean isLogin();
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////

} 