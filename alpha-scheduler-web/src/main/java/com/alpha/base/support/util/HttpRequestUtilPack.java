package com.alpha.base.support.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.context.ApplicationContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.alpha.base.support.aid.RequestAttributesAidPack.RequestAttributesAid;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class HttpRequestUtilPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public enum DeviceType {mobile,mobileCS,pc,jvm};
	public enum OsType {windows,linux,macintosh,ios,android,java,etc};
	public enum BrowserType {ie,edge,chrome,kakaotalk,opera,firefox,safari,feign,etc};

	public static class ClientInfo {
		private String userAgent;
		
		private String ip = null;

		private DeviceType deviceType;
		private OsType osType;
		private BrowserType browserType;
		
		private boolean isMobileCS;
		private boolean isFeignRequest;
		
		public ClientInfo() {};

		public ClientInfo(String ip, String userAgent, DeviceType deviceType, OsType osType, BrowserType browserType, boolean isMobileCS,boolean isFeignRequest) {
			this.ip = ip;
			this.userAgent = userAgent;
			this.deviceType = deviceType;
			this.osType = osType;
			this.browserType = browserType;
			this.isMobileCS = isMobileCS;
			this.isFeignRequest = isFeignRequest;
		}

		public String getIp() {return ip;}
		public String getUserAgent() {return userAgent;}
		public DeviceType getDeviceType() {return deviceType;}
		public OsType getOsType() {return osType;}
		public BrowserType getBrowserType() {return browserType;}	
		public boolean isMobileCS() {return isMobileCS;}
		public boolean isFeignRequest() {return isFeignRequest;}

		@Override
		public String toString() {
			return "ClientInfo [userAgent=" + userAgent + ", ip=" + ip + ", deviceType=" + deviceType + ", osType="
					+ osType + ", browserType=" + browserType + ", isMobileCS=" + isMobileCS + ", isFeignRequest="
					+ isFeignRequest + "]";
		}
	}

	public static class RefererInfo {
		private String rawData;
		private String protocol;
		private String domain;
		private int port;
		private String path;
		private String query;
    	
		public RefererInfo(String rawData, String protocol, String domain, int port, String path, String query) {
			this.rawData = rawData;
			this.protocol = protocol;
			this.domain = domain;			
			this.port = port;
			this.path = path;
			this.query = query;
		}
		
		public String getRawData() {return rawData;}
		public String getProtocol() {return protocol;}
		public String getDomain() {return domain;}
		public int getPort() {return port;}
		public String getPath() {return path;}
		public String getQuery() {return query;}

		@Override
		public String toString() {
			return "Referer [rawData=" + rawData + ", protocol=" + protocol + ", domain=" + domain + ", port=" + port
					+ ", path=" + path + ", query=" + query + "]";
		}

    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface HttpRequestUtil {
		
		public HttpServletRequest getHttpServletRequest();
		
		public ServletContext getServletContext();
			
		public String getForwardView(String view);
		public String getRedirectView(String view);

		public String getParamThrouSession(String param,HttpServletRequest request);
		public List<String> getRequestMappingUrl(ApplicationContext cxt);	
		public boolean isRequestPathCheck(HttpServletRequest request,List<String> urlPattrenList);
		public String getCookieValue(HttpServletRequest request,String cookieName);
		
		public String getRequestPageURI(HttpServletRequest request);			
		public String getDigestPageURI(HttpServletRequest request,String pageURI);

		public String getRemoteAddress();
		public String getRemoteAddress(HttpServletRequest request);
		
		public RefererInfo getRefererInfo(HttpServletRequest request);
		public ClientInfo getClientInfo(HttpServletRequest request);
		
		public OsType getOsType(HttpServletRequest request);
		public DeviceType getDeviceType(HttpServletRequest request);
		public BrowserType getBrowserType(HttpServletRequest request);
		
		//public Device getCurrentDevice(HttpServletRequest request);			
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static HttpRequestUtil getHttpRequestUtil(RequestAttributesAid requestAttributesAid) {
		if(requestAttributesAid==null) {log.debug(">> param[requestAttributesAid] is null."); return null;}
		
		return new HttpRequestUtil() {

			@Override
			public HttpServletRequest getHttpServletRequest() {
				return requestAttributesAid.getHttpServletRequest();			
			}
			
			@Override
			public ServletContext getServletContext() {
				return requestAttributesAid.getServletContext();
			}

			@Override	
			public String getForwardView(String view) {
				return "forward:"+view;
			}
			
			@Override		
			public String getRedirectView(String view) {
				return "redirect:"+view;
			}

			@Override
			public String getParamThrouSession(String param, HttpServletRequest request) {
		        String value=request.getParameter(param);
		        HttpSession session = requestAttributesAid.getHttpServletRequest().getSession();
		        if(value==null || value.equals("")) {value=(session.getAttribute(param)==null?"":session.getAttribute(param).toString());}        
		        if(value!=null && !value.equals("")) {session.setAttribute(param,value);}        
		        return value;   
			}

			@Override
			public List<String> getRequestMappingUrl(ApplicationContext cxt) {
				RequestMappingHandlerMapping mapping=cxt.getBean(RequestMappingHandlerMapping.class);		
				Map<RequestMappingInfo,HandlerMethod> map = mapping.getHandlerMethods();	
				
				List<String> list = new ArrayList<>();		
				for(RequestMappingInfo key : map.keySet()) {
					list.addAll(key.getPatternsCondition().getPatterns());
				}	
				return list;
			}

			@Override
			public boolean isRequestPathCheck(HttpServletRequest request, List<String> urlPattrenList) {
				if(urlPattrenList==null) {return false;}
				
				AntPathMatcher antPathMatcher=new AntPathMatcher();
				String context=(request.getContextPath().equals("/")?request.getContextPath():request.getContextPath()+"/");
				String checkPath=request.getRequestURI();
				if(checkPath.lastIndexOf(";")!=-1) {checkPath=checkPath.substring(0, checkPath.indexOf(";"));}
				for(int i=0;i<urlPattrenList.size();i++) {
					if(urlPattrenList.get(i)!=null && !urlPattrenList.get(i).equals("")) {
						String pattern=context+(urlPattrenList.get(i).equals("/")?"":urlPattrenList.get(i));
						if(antPathMatcher.match(pattern,checkPath)) {return true;}
					}
				}
				return false;
			}

			@Override
			public String getCookieValue(HttpServletRequest request, String cookieName) {
				String cookieValue="";
				Cookie[] cookies=request.getCookies();
				if(cookies != null && cookies.length>0) {
				    for(int i=0;i<cookies.length;i++) {
				    	if(cookies[i].getName().equals(cookieName)) {
				    		cookieValue=cookies[i].getValue();
				    		break;
				    	}
				    }
				}
				return cookieValue;
			}

			@Override
			public String getRequestPageURI(HttpServletRequest request) {
				return this.getDigestPageURI(request,request.getRequestURI());
			}

			@Override
			public String getDigestPageURI(HttpServletRequest request,String pageURI) {
				if(pageURI==null || pageURI.equals("")) {return pageURI;}
				
				String requestUri=pageURI;
				if(requestUri.startsWith(request.getContextPath())) {
					requestUri=requestUri.substring(request.getContextPath().length(),requestUri.length());
				}
				return requestUri;
			}

			@Override
			public String getRemoteAddress() {
				return this.getRemoteAddress(requestAttributesAid.getHttpServletRequest());
			}

			@Override
			public String getRemoteAddress(HttpServletRequest request) {
		        String ip = request.getHeader("X-Forwarded-For");
		        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {ip = request.getHeader("Proxy-Client-IP");}  
		        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {ip = request.getHeader("WL-Proxy-Client-IP");}  
		        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {ip = request.getHeader("HTTP_CLIENT_IP");}  
		        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {ip = request.getHeader("HTTP_X_FORWARDED_FOR");}  
		        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {ip = request.getRemoteAddr();}		        
		        if(ip.indexOf(",")>-1) {ip=ip.split(",")[0];}
		        
				return ip;
			}
			
			@Override			
			public RefererInfo getRefererInfo(HttpServletRequest request) {
				String rawData = request.getHeader("referer");
				if(rawData==null || rawData.equals("")) {return null;}

				String protocol = rawData.substring(0,rawData.indexOf("://"));

				String temp = rawData.substring(rawData.indexOf("://")+3);
				temp = temp.indexOf("/")>-1?temp.substring(0,temp.indexOf("/")):temp;
				String domain=temp.indexOf(":")>-1?domain=temp.substring(0,temp.indexOf(":")):temp;

				int port=temp.indexOf(":")>-1?port=Integer.parseInt(temp.substring(temp.indexOf(":")+1)):(protocol.equals("https")?443:80);

				String path = rawData.substring(rawData.indexOf("://")+3);
				path = path.indexOf("/")>-1?path.substring(path.indexOf("/")):"/";
				if(path!=null && !path.equals("") && path.indexOf("?")>-1) {path=path.substring(0,path.indexOf("?"));}		

				String query = rawData.indexOf("?")>-1?rawData.substring(rawData.indexOf("?")+1):null;

				return  new RefererInfo(rawData,protocol,domain,port,path,query);	
			}
			
			@Override			
			public ClientInfo getClientInfo(HttpServletRequest request) {
				String userAgent = request.getHeader("User-Agent");
				log.debug(">> userAgent:{}",userAgent);

				DeviceType  device = null;
				if (userAgent == null) {device=null;}
				else if(userAgent.indexOf("Mobile")>-1 && userAgent.indexOf("MobileCS")!=-1) {device=DeviceType.mobileCS;}
				else if(userAgent.indexOf("Mobile")>-1 && userAgent.indexOf("MobileCS")==-1) {device=DeviceType.mobile;}				
				else if(userAgent.indexOf("Java")>-1){device = DeviceType.jvm;} //feign 서버간호출
				else {device = DeviceType.pc;}		

				OsType osType = null;				
				if (userAgent == null) {userAgent=null;}
				else if(userAgent.indexOf("Windows")>-1) {osType = OsType.windows;}
				else if(userAgent.indexOf("Linux")>-1) {osType = OsType.linux;}
				else if(userAgent.indexOf("Macintosh")>-1) {osType = OsType.macintosh;}
				else if(userAgent.indexOf("iPhone")>-1) {osType = OsType.ios;}
				else if(userAgent.indexOf("iPad")>-1) {osType = OsType.ios;}
				else if(userAgent.indexOf("Java")>-1) {osType = OsType.java;} //feign 서버간호출
				else {osType = OsType.etc;}

				BrowserType browser = null;
				if (userAgent == null) {browser=null;}
				else if(userAgent.indexOf("Trident")>-1) {browser=BrowserType.ie;}
				else if(userAgent.indexOf("Edge")>-1) {browser=BrowserType.edge;}
				else if(userAgent.indexOf("Firefox")>-1) {browser=BrowserType.firefox;}
				else if(userAgent.indexOf("KAKAOTALK")>-1) {browser=BrowserType.kakaotalk;}
				else if(userAgent.indexOf("Chrome")>-1) {
					if(userAgent.indexOf("Opera")>-1 || userAgent.indexOf("OPR")>-1) {browser=BrowserType.opera;}					
					else {browser=BrowserType.chrome;}
				}
				else if(userAgent.indexOf("Safari")>-1 && userAgent.indexOf("Chrome")==-1) {browser=BrowserType.safari;}

				boolean isMobileCS = userAgent.indexOf("MobileCS")>-1?true:false;

				ClientInfo clientInfo = new ClientInfo(this.getRemoteAddress(request),userAgent,device,osType,browser,isMobileCS,false);
				log.debug(">> clientInfo:{}",clientInfo.toString());
				
				return clientInfo;
			}

			@Override			
			public OsType getOsType(HttpServletRequest request) {
				return this.getClientInfo(request).getOsType();	
			}
			
			@Override			
			public BrowserType getBrowserType(HttpServletRequest request) {
				return this.getClientInfo(request).getBrowserType();
			}
			
			@Override			
			public DeviceType getDeviceType(HttpServletRequest request) {
				return this.getClientInfo(request).getDeviceType();			
			}

			//@Override
			//public Device getCurrentDevice(HttpServletRequest request) {
			//	return DeviceUtils.getCurrentDevice(request);
			//}
			
		};
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}