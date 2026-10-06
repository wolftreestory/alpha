package com.alpha.craft.console.controller;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alpha.base.BaseUtil;
import com.alpha.base.support.batch.SchedulerManagerPack.SchedulerManager;
import com.alpha.craft.console.Support;
import com.alpha.craft.console.service.ConsoleServicePack.AuthService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class AuthController {

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
    private SchedulerManager schedulerManager;
    
    private AuthService authService;

    private Support support;
    
    public AuthController(SchedulerManager schedulerManager, AuthService authService, Support support) {
    	this.schedulerManager = schedulerManager;
    	this.authService = authService;
    	this.support = support;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @RequestMapping(value={"/"}, method={RequestMethod.GET,RequestMethod.POST})
    public String main(HttpServletRequest req,HttpServletResponse res, Model model) throws Exception {
		log.debug(">> root");
		
		if(this.authService.isLogin()) {
			String schedulerRequestPath=this.schedulerManager.getSchedulerRequestPath();
			return "redirect:/"+schedulerRequestPath+"/main";
		}
		
		return "forward:/login";
    }

    @RequestMapping(value={"/login"}, method={RequestMethod.GET,RequestMethod.POST})
    public String login(HttpServletRequest req,HttpServletResponse res,Model model) throws Exception {
    	log.debug(">> login");
        
		if(!BaseUtil.getAlertUtil().isExistAlertMessage()) {
		    this.authService.doLogout();
		}
		
		if(BaseUtil.getLocaleUtil().getLocale()==null) {
		    BaseUtil.getLocaleUtil().setLocale(Locale.KOREAN);
		}
						
		model.addAllAttributes(support.getSystemInfo());
		model.addAttribute("authKeyList",BaseUtil.getSystemUtil().isProd()?null:authService.getAuthKeyList());
		
    	return this.support.getRoutedView("auth.login");
    }

    @RequestMapping(value={"/frame"}, method={RequestMethod.GET,RequestMethod.POST})
    public String frame(HttpServletRequest req,HttpServletResponse res) throws Exception {
    	log.debug(">> frame");
    	
    	BaseUtil.getSessionUtil().setAttribute("authKey", req.getParameter("authKey"));
    	
    	return this.support.getRoutedView("auth.frame");
    }
    
    @RequestMapping(value={"/doLink"}, method={RequestMethod.GET,RequestMethod.POST})
    public String doLink(HttpServletRequest req,HttpServletResponse res) throws Exception {
    	log.debug(">> doLink");
    	
    	String authKey=(String)req.getParameter("authKey");
    	if(!StringUtils.hasText(authKey)) {
    		Object object = BaseUtil.getSessionUtil().getAttribute("authKey");
    		if(object!=null) {authKey=object.toString();}
    	}
    	log.debug(">> authKey:{}",authKey);
    	
        if(!StringUtils.hasText(authKey)) {
			  BaseUtil.getAlertUtil().setAlertMessage(BaseUtil.getMessageUtil().getMessage("인증키가 없습니다."));
        }
        
        BaseUtil.getSessionUtil().setAttribute(AuthService.SCHEDULER_LOGIN_MODE,"innerView");
      
		//doLogin
		try {
    		Map<String,String> accessUserInfo=new HashMap<>();
    		accessUserInfo.put("authKey",authKey);
    		this.authService.doLogin(accessUserInfo);
    		
		} catch(Exception e) {
	        BaseUtil.getAlertUtil().setAlertMessage(e.getMessage()); 	
		}
		
        return "redirect:/";
    }
    
    @RequestMapping(value={"/doLogin"}, method={RequestMethod.GET,RequestMethod.POST})
    public String doLogin(HttpServletRequest req,HttpServletResponse res) throws Exception {
    	log.debug(">> doLogin");
		
        String authKey=(String)req.getParameter("authKey");
        String userId=(String)req.getParameter("userId");
        String userPw=(String)req.getParameter("userPw");
        log.debug(">> userId: {}, userPw: {}",userId,userPw);

        boolean isEnable=true;
        if(!StringUtils.hasText(authKey)) {
	        if(isEnable && !StringUtils.hasText(userId)) {isEnable=false;}
			if(isEnable && !StringUtils.hasText(userPw)) {isEnable=false;}		
			if(!isEnable) {
			    BaseUtil.getAlertUtil().setAlertMessage(BaseUtil.getMessageUtil().getMessage("auth.message.alert.001"));
			    return "redirect:/";
			}
        }
        
		//mode
		String mode=(String)req.getParameter("mode");
		if(!StringUtils.hasText(mode)) {mode="outterView";}
		BaseUtil.getSessionUtil().setAttribute(AuthService.SCHEDULER_LOGIN_MODE,mode);
		
		//doLogin
		try {
    		Map<String,String> accessUserInfo=new HashMap<>();
    		accessUserInfo.put("authKey",authKey);
    		accessUserInfo.put("userId",userId);
    		accessUserInfo.put("userPw",userPw);
    		String taskGroups = this.authService.doLogin(accessUserInfo);
    		
        	// taskGroups이 .e로 끝나면 일치하는 것 .c로 끝나면 포함하는 것
        	BaseUtil.getSessionUtil().setAttribute("searchFilterText",taskGroups+".c");  
        	
		}catch(Exception e) {
	        BaseUtil.getAlertUtil().setAlertMessage(e.getMessage()); 	
		}
		
        return "redirect:/";
    }

    @RequestMapping(value={"/doLogout"}, method={RequestMethod.GET,RequestMethod.POST})
    public String doLogout(HttpServletRequest req,HttpServletResponse res) throws Exception {
    	log.debug(">> doLogout");
		this.authService.doLogout();
		return "redirect:/";
    }
    
    @RequestMapping(value={"/doRefreshSession"}, method={RequestMethod.GET,RequestMethod.POST})
    public String doRefreshSession(HttpServletRequest req,HttpServletResponse res,ModelMap model) throws Exception {
    	log.debug(">> doRefreshSession");

		int maxInactiveInterva=BaseUtil.getSessionUtil().getHttpSession().getMaxInactiveInterval();
		model.addAttribute("maxInactiveInterval",maxInactiveInterva);	
		model.addAttribute("refreshSessionTimeout",(maxInactiveInterva-60)*1000);	
		
    	return this.support.getRoutedView("auth.refresh");
    }    

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
}
