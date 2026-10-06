package com.alpha.xtra.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alpha.Application.DefaultBinder;
import com.alpha.xtra.service.RedisStoreService;

import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping(path="/xtra/redis/store/v1")
@ConditionalOnProperty(name="spring.redis.enabled", havingValue="true", matchIfMissing=false)
public class RedisStoreController extends DefaultBinder {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final RedisStoreService redisStoreService;

	public RedisStoreController(RedisStoreService redisStoreService) {
		this.redisStoreService = redisStoreService;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @PutMapping("/data")
    public Map<String,Object> setRedisData(
    		@Parameter(description="message") @RequestParam(required=true, defaultValue="강원랜드 차세대ERP시스템") final String message,
    		@Parameter(description="expireTime(s)") @RequestParam(required=true, defaultValue="60") final long expireTime,
    		@Parameter(description="exceptionYn") @RequestParam(required=true, defaultValue="N") final String exceptionYn
    		) {

    	this.redisStoreService.set(message,expireTime,exceptionYn);
		
    	 Map<String,Object> map = new HashMap<>();
    	 map.put("message",message);
    	 
        return map;
    }

    @GetMapping("/data")
    public Map<String,Object> getRedisData() {
        return this.redisStoreService.get();
    }
    
    @GetMapping("/increment")
    public Map<String,Object> getIncrement() {
        return this.redisStoreService.increment();
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}


