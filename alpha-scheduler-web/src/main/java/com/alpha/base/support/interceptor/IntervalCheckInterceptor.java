package com.alpha.base.support.interceptor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.CookieGenerator;

import com.alpha.base.BaseUtil;
import com.alpha.base.exception.SystemException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntervalCheckInterceptor implements HandlerInterceptor {

	private static final String SPLIT_STRING="#intervalCheckTime=";
	private static final String INTERVAL_INFO="intervalInfo";
	private static final String TOKEN_INFO="intervalTokenInfo";
	
	private static List<String> noneCheckPatternList;
	private static long interval=500;
	private static String enableYn="Y";
	
	///////////////////////////////////////////////////////////////////////////////////////////////

	public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object paramObject) throws Exception {return this.preHandleProcess(request,response);} 
	public void postHandle(HttpServletRequest request,HttpServletResponse response,Object paramObject,ModelAndView paramModelAndView) throws Exception {this.postHandleProcess(request,response);} 
	public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object paramObject,Exception paramException) throws Exception {} 
	
	///////////////////////////////////////////////////////////////////////////////////////////////

	public void setEnableYn(String enableYnStr) {
		enableYn=enableYnStr;
	}
		
	public void setInterval(long ms) {
		interval=ms;
	}
	
	public void setNoneCheckPatternList(List<String> arg) {
		noneCheckPatternList=arg;
	} 
	
	private boolean preHandleProcess(HttpServletRequest request,HttpServletResponse response) throws Exception {	
		if(!enableYn.equals("Y")) {return true;}
		if(BaseUtil.getHttpRequestUtil().isRequestPathCheck(request,noneCheckPatternList)) {return true;} 
		
		String serverInvervalInfo=(String)BaseUtil.getSessionUtil().getAttribute(INTERVAL_INFO);	
		if(serverInvervalInfo==null) {serverInvervalInfo="";}
		//log.debug(">>serverInvervalInfo: {}",serverInvervalInfo);	
		
		String serverToken=(String)BaseUtil.getSessionUtil().getAttribute(TOKEN_INFO);
		if(serverToken==null) {serverToken="";}
		//log.debug(">>serverToken: {}",serverToken);	
		
		String requestPageURI=BaseUtil.getHttpRequestUtil().getRequestPageURI(request);

		if(serverInvervalInfo.indexOf(requestPageURI)>-1) {
			
			long currentInterval=System.currentTimeMillis() - Long.parseLong(serverInvervalInfo.split(SPLIT_STRING)[1]);
			String currentToken=BaseUtil.getHttpRequestUtil().getCookieValue(request,TOKEN_INFO);
			log.debug(">> requestPageURI: {},currentInterval: {},currentToken: {}",requestPageURI,currentInterval,currentToken);	

			if(currentInterval < interval && !serverToken.equals(currentToken)) {
				throw new SystemException("중복요청을 허용하지 않습니다.");
			}				
		}
		
		BaseUtil.getSessionUtil().setAttribute(INTERVAL_INFO,requestPageURI+SPLIT_STRING+System.currentTimeMillis());
		BaseUtil.getSessionUtil().setAttribute(TOKEN_INFO,"");
		return true;
	} 

		
	private void postHandleProcess(HttpServletRequest request,HttpServletResponse response) throws Exception {	
		if(!enableYn.equals("Y")) {return;}
		if(BaseUtil.getHttpRequestUtil().isRequestPathCheck(request,noneCheckPatternList)) {return;} 
		
		String responseTime=String.valueOf(System.currentTimeMillis());
		
		//세션에 저장
		BaseUtil.getSessionUtil().setAttribute(TOKEN_INFO,responseTime);		
		//log.debug(">>responseTime: {}",responseTime);	
		
		//쿠키에 저장
		CookieGenerator generator=new CookieGenerator();
		generator.setCookieName(TOKEN_INFO);
		generator.addCookie(response,responseTime);
	}
}