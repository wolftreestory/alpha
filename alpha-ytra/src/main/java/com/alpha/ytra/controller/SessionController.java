package com.alpha.ytra.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alpha.base.BaseUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path="/ytra/session/v1")
public class SessionController {

    @GetMapping("/setSession")
    public Map<String,Object> setSession() {
		log.debug(">> setSession");
		
		HttpSession session = BaseUtil.getSessionUtil().getHttpSession();
		session.setAttribute("time",System.currentTimeMillis());
		
		Map<String,Object> map = new HashMap<>();
		map.put("sessionId", session.getId());
		map.put("time", session.getAttribute("time"));
		map.put("interval", session.getMaxInactiveInterval());
        
        return map;
    }
    
    @GetMapping("/getSession")
    public Map<String,Object> getSession() {
		log.debug(">> getSession");

		HttpSession session = BaseUtil.getSessionUtil().getHttpSession();

		Map<String,Object> map = new HashMap<>();
		map.put("sessionId", session.getId());
		map.put("time", session.getAttribute("time"));
		map.put("interval", session.getMaxInactiveInterval());
		
        return map;
    }

}
