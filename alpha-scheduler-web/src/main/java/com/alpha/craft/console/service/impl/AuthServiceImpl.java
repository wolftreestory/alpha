package com.alpha.craft.console.service.impl;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.alpha.base.BaseUtil;
import com.alpha.base.config.batch.BatchProperties;
import com.alpha.base.exception.SystemException;
import com.alpha.craft.console.service.ConsoleServicePack.AuthService;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service(AuthService.LOGIN_CHECK_BEAN_NAME)
public class AuthServiceImpl implements AuthService {

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private List<Map<String,Object>> accountMapList;
	private List<String> accountStringList;

    public AuthServiceImpl(BatchProperties batchProperties) {
    	this.accountStringList = batchProperties.getWebConsole().getAccountList();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public boolean isLogin() {
		Object loginUserInfo=BaseUtil.getSessionUtil().getAttribute(AuthService.SCHEDULER_LOGIN_USER_INFO);
		if(loginUserInfo==null) {return false;}		
		return true;
	}

	@Override
	public Map<String,Object> getUserInfo() {
		Object loginUserInfo=BaseUtil.getSessionUtil().getAttribute(AuthService.SCHEDULER_LOGIN_USER_INFO);
		if(loginUserInfo==null) {return null;}		
		//return (Map<String,Object>)loginUserInfo;
		return BaseUtil.getObjectMapperUtil().getMapper().convertValue(loginUserInfo, new TypeReference<Map<String,Object>>(){});
	}
	
	@Override
	public String doLogin(Map<String,String> accessUserInfo) {
		if(accessUserInfo==null) {throw new SystemException("인증정보가 없습니다.");}
		
		Map<String,Object> accessauthKeyMap = this.extractAuthKey(accessUserInfo.get("authKey"));
		
		String accessUserId="";
		String accessUserPw="";
		if (accessauthKeyMap!=null) {
			accessUserId=accessauthKeyMap.get("userId").toString();
			accessUserPw=accessauthKeyMap.get("userPw").toString();		
		} else {
			accessUserId=accessUserInfo.get("userId");
			if(accessUserId==null) {accessUserId="";}

			accessUserPw=accessUserInfo.get("userPw");			
			if(accessUserPw==null) {accessUserPw="";}
		}		

		if(this.accountMapList==null || accountMapList.isEmpty()) {
			this.accountMapList=this.extractAccountList(accountStringList);
		}
		
		boolean isEnable=false;
		Map<String,Object> accountMap=null;
		for(int i=0;i<this.accountMapList.size();i++) {
			accountMap=this.accountMapList.get(i);
			String userId=(accountMap.get("userId")==null?"":accountMap.get("userId").toString());
			String userPw=(accountMap.get("userPw")==null?"":accountMap.get("userPw").toString());
			if(userId.equals(accessUserId) && userPw.equals(accessUserPw)) {isEnable=true;break;}
		}
		if(!isEnable) {throw new SystemException("인증정보가 올바르지 않습니다.");}

		String taskGroups = accountMap.get("groups").toString();
        if(StringUtils.hasText(taskGroups) && taskGroups.equals("*")) {taskGroups="";}      	
		
		BaseUtil.getSessionUtil().setAttribute(AuthService.SCHEDULER_LOGIN_USER_INFO,accountMap);
		log.debug(">> accountMap: {}",accountMap);	

		return taskGroups;
	}

	@Override
	public void doLogout() throws Exception {
		BaseUtil.getSessionUtil().getHttpSession().invalidate();
	    return;
	}    

	@Override
	public List<Map<String,Object>> getAuthKeyList() {
		if(this.accountMapList==null) {
			this.accountMapList=this.extractAccountList(accountStringList);
		}
		this.accountMapList.forEach(map -> {
			map.remove("authKey");
			map.put("authKey", this.creatAuthKey(map));
		});
		return this.accountMapList;
	}
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private List<Map<String,Object>> extractAccountList(List<String> list) {
		if(list==null || list.isEmpty()) {return null;}

		List<Map<String,Object>> authKeyList = new ArrayList<>();
		try {
			for(String item : list) {
				authKeyList.add(BaseUtil.getObjectMapperUtil().readValue(item, new TypeReference<Map<String,Object>>() {}));			
			}
		}catch(Exception e) {
			throw new SystemException(e.getMessage());
		}
		return authKeyList;
	}
	
	private Map<String,Object> extractAuthKey(String accessauthKey) {
		if(!StringUtils.hasText(accessauthKey)) {return null;}

		Map<String,Object> accessauthKeyMap = null;
		try {
			String temp=accessauthKey;
			if(this.isURLEncoded(temp)) {
				temp = URLDecoder.decode(temp,"UTF-8");
			}			
			temp=BaseUtil.getCryptoUtil().getAES256().decryption(temp);
			temp = BaseUtil.getBase64Util().getDecodeBase64String(temp);
			accessauthKeyMap = BaseUtil.getObjectMapperUtil().readValue(temp, new TypeReference<Map<String,Object>>() {});	
		}catch(Exception e) {
			throw new SystemException(e.getMessage());
		}
		return accessauthKeyMap;
	}

	private String creatAuthKey(Map<String,Object> accessauthKeyMap) {		
		if(accessauthKeyMap==null) {return null;}
		String accessauthKey = null;
		try {
			Map<String,Object> map = new HashMap<>();
			map.put("userId", accessauthKeyMap.get("userId"));
			map.put("userPw", accessauthKeyMap.get("userPw"));
 			accessauthKey = BaseUtil.getJsonUtil().toJson(map);
			accessauthKey = BaseUtil.getBase64Util().getEncodeBase64String(accessauthKey);
			accessauthKey = BaseUtil.getCryptoUtil().getAES256().encryption(accessauthKey);
			accessauthKey = URLEncoder.encode(accessauthKey,"UTF-8");
		}catch(Exception e) {
			throw new SystemException(e.getMessage());
		}
		return accessauthKey;
	}
	
	
	private boolean isURLEncoded(String value) throws Exception {
        String decodedUrl = URLDecoder.decode(value, "UTF-8");
        String encodedUrl = URLEncoder.encode(decodedUrl, "UTF-8");        
        return value.equals(encodedUrl)?true:false;
	}
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}