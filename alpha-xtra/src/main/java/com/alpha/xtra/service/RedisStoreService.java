package com.alpha.xtra.service;

import java.io.Serializable;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public interface RedisStoreService {
	
    public void set(String name,long expireTime,String exceptionYn);
    
    public Map<String,Object> get();
    
    public Map<String,Object> increment();
    
	@Getter
	@Setter
	@ToString
	public static class DataVo implements Serializable {
		private static final long serialVersionUID = 1L;
		private String message;
	}    
	
}
