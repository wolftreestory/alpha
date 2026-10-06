package com.alpha.xtra.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.alpha.base.BaseUtil;
import com.alpha.base.support.util.HttpRequestUtilPack.ClientInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class XController {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/xtra/xxx")
    public String clientInfo(Model model, HttpServletRequest request) {
    	
    	log.debug(">> **************************************************************************************************");
		ClientInfo clientInfo = BaseUtil.getHttpRequestUtil().getClientInfo(request);
		
		Map<String,Object> map = new HashMap<>();
		map.put("clientIp",clientInfo.getIp()); //JVM 적용 -Djava.net.preferIPv4Stack=true
		map.put("userAgent",clientInfo.getUserAgent());
		map.put("deviceType",clientInfo.getDeviceType());
		map.put("osType",clientInfo.getOsType());
		map.put("browserType",clientInfo.getBrowserType());

		//여러 프록시를 거쳐 웹 서버에 도달하는 경우, 실제 요청을 보낸 클라이언트의 IP를 식별할 때 사용 (각 프록시는 이 헤더에 자신의 IP 주소를 추가 함)
		map.put("X-Forwarded-For",request.getHeader("X-Forwarded-For")); //표준		
		map.put("Proxy-Client-IP",request.getHeader("Proxy-Client-IP")); 	
		map.put("WL-Proxy-Client-IP",request.getHeader("WL-Proxy-Client-IP"));
		map.put("HTTP_CLIENT_IP",request.getHeader("HTTP_CLIENT_IP"));
		map.put("HTTP_X_FORWARDED_FOR",request.getHeader("HTTP_X_FORWARDED_FOR"));
		
		model.addAttribute("data", map);
		
        return "xxx";
    }	
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
