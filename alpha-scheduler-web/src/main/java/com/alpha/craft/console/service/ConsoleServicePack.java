package com.alpha.craft.console.service;

import java.util.List;
import java.util.Map;

import com.alpha.base.support.batch.SchedulerManagerPack.SchedulerManager;
import com.alpha.base.support.interceptor.LoginCheckInterceptor.LoginCheck;

public class ConsoleServicePack {
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static interface AuthService extends LoginCheck {

		public static final String SCHEDULER_LOGIN_USER_INFO = "SCHEDULER_LOGIN_USER_INFO";
		
		public static final String SCHEDULER_LOGIN_MODE = "SCHEDULER_LOGIN_MODE";

		public List<Map<String,Object>> getAuthKeyList();

		public Map<String,Object> getUserInfo();
		
		public String doLogin(Map<String,String> accessUserInfo);
		 
		public void doLogout() throws Exception;
	}

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static interface MainService {
		
		public SchedulerManager getSchedulerManager();
		 
		public boolean isEnableRequest(String requestPath);
	}

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}