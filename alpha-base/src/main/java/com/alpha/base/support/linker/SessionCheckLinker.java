package com.alpha.base.support.linker;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.alpha.base.support.AbstractMeta;

import io.swagger.v3.oas.annotations.Hidden;

public class SessionCheckLinker extends AbstractMeta {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Hidden
	@RestController
	@ConditionalOnWebApplication
	public static class SessionCheckController {

		@Value("${alpha.healthcheck.systemPrefix:alpha}")
		private String systemPrefix;
		
		@Value("${spring.application.name}")
		private String applicationName;
		
		@GetMapping(path="/{applicationCode}/sessioncheck")
		public Map<String,Object> sessionCheck(@PathVariable final String applicationCode) throws Exception {	
			if (!this.applicationName.equals(systemPrefix + "-" + applicationCode)) {
				throw new RuntimeException("applicationCode is wrong.");
			}
			
			HttpSession session = MetaUtil.getSessionUtil().getHttpSession();
			
	        Map<String,Object> map = new HashMap<>();
	        map.put("sessionId", session.getId());
	        map.put("time", session.getAttribute("time"));
	        map.put("interval", session.getMaxInactiveInterval());
	        
	        return map;
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}
