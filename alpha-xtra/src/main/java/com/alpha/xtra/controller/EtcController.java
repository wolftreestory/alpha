package com.alpha.xtra.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alpha.Application.DefaultBinder;
import com.alpha.base.BaseUtil;
import com.alpha.base.support.util.HttpRequestUtilPack.ClientInfo;
import com.alpha.base.support.util.HttpRequestUtilPack.RefererInfo;
import com.alpha.base.support.util.ThreadUtilPack.ThreadJob;
import com.alpha.xtra.service.BoardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path="/xtra/etc/v1")
public class EtcController extends DefaultBinder {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Autowired
	private BoardService boardService;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/dummy")
    @Operation(summary = "날짜 출력", description = "logger 테스트용") 
    public Map<String,Object> dummy() {
		log.debug(">> dummy");
		log.info(">> dummy");
		log.warn(">> dummy");
		log.error(">> dummy");

		Map<String,Object> map = new HashMap<>();
		map.put("currentTimeMillis", System.currentTimeMillis());
		map.put("dayOfWeek", BaseUtil.getTimeUtil().getDayOfWeek());

        return map;
    }
    
    @GetMapping("/clientInfo")
    @Operation(summary = "접속자 정보", description = "IPv4 : JVM 옵션 -Djava.net.preferIPv4Stack=true")   
    public Map<String,Object> clientInfo(HttpServletRequest request) {

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

        return map;
    }	

    @GetMapping("/jasyptEncrypt")
    @Operation(summary = "jasypt 프로퍼티 암호화", description = "jasypt 프로퍼티 암호화") 
    public String jasyptEncrypt(
    		@Parameter(description = "message") @RequestParam(required = true) final String message
    		){
    	//StringEncryptor stringEncryptor = BaseUtil.getBean("jasyptStringEncryptor",StringEncryptor.class);
    	StringEncryptor stringEncryptor = BaseUtil.getCryptoUtil().getStringEncryptor();
    	
    	return stringEncryptor.encrypt(message);
    }
    
    @GetMapping("/jasyptDecrypt")
    @Operation(summary = "jasypt 프로퍼티 복호화", description = "jasypt 프로퍼티 복호화") 
    public String jasyptDecrypt(
    		@Parameter(description = "message") @RequestParam(required = true) final String message
    		){

    	//StringEncryptor stringEncryptor = BaseUtil.getBean("jasyptStringEncryptor",StringEncryptor.class);
    	StringEncryptor stringEncryptor = BaseUtil.getCryptoUtil().getStringEncryptor();
    	
    	return stringEncryptor.decrypt(message);
    }
    
    @GetMapping("/systemUsage")
    @Operation(summary = "시스템 사용량", description = "BaseUtil.getSystemUtil().getSystemUsage()") 
    public List<Map<String,String>> monitoring(
    		@Parameter(description = "count") @RequestParam(required = false, defaultValue = "10") final int count,
    		@Parameter(description = "sleep") @RequestParam(required = false, defaultValue = "50") final int sleep
    		){
    	
        List<Map<String,String>> list = new ArrayList<>();
        for(int i=0;i<count;i++){
        	Map<String,String> systemUsage = BaseUtil.getSystemUtil().getSystemUsage();
            list.add(systemUsage);
            this.threadSleepRandom(sleep,0);
            log.debug(">> systemUsage:{}",systemUsage);
        }

        return list;
    }

    @GetMapping("/multiThread")
    @Operation(summary = "쓰레드 동시처리", description = "순차처리-수행시간 > 동시처리-수행시간")
    public Map<String,Object> concurrent(
    		@Parameter(description = "count") @RequestParam(required = false, defaultValue="10") final int count,
    		@Parameter(description = "sleep") @RequestParam(required = false, defaultValue="100") final int sleep,
    		@Parameter(description = "isParallel") @RequestParam(required=true, defaultValue="false") final boolean isParallel) throws Exception{
    	
    	log.debug(">> START : {}",BaseUtil.getSystemUtil().getSystemUsage());

    	Map<String,Object> result = null;
    	
    	long time = System.nanoTime();
    	
    	if(isParallel) {
    		List<ThreadJob> list = new ArrayList<>();
    		for(int i=0;i<count;i++) {
    			String id = "job"+i;
    			int no = i<10?i:count%10;		
    			list.add(new ThreadJob(id,()->{return this.getBoardInfo(no,sleep);}));
    		}
    		result = BaseUtil.getThreadUtil().execute(list);
    	} else {
    		result = new HashMap<>();
    		for(int i=0;i<count;i++) {
    			final String id = "job"+i;
    			final int no = i<10?i:count%10;  		    			
    			result.put(id, this.getBoardInfo(no,sleep));
    		}
    	}
    	
    	long executeTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-time);
    	result.put("executeTime(ms)", executeTime);

    	log.debug(">> END : {}ms, {}",executeTime,BaseUtil.getSystemUtil().getSystemUsage());
    	
        return result;
    }

    @GetMapping("/refererInfo")
    @Operation(summary = "요청 출처정보", description = "referer : 요청헤더에 담겨있는 이전페이지의 uri정보")
    public Map<String,Object> refererInfo(HttpServletRequest request) {

		RefererInfo referer = BaseUtil.getHttpRequestUtil().getRefererInfo(request);		
		log.debug(">> referer.getRawData():{}",referer.getRawData());
		log.debug(">> referer.getProtocol():{}",referer.getProtocol());
		log.debug(">> referer.getDomain():{}",referer.getDomain());
		log.debug(">> referer.getPort():{}",referer.getPort());
		log.debug(">> referer.getPath():{}",referer.getPath());
		log.debug(">> referer.getQuery():{}",referer.getQuery());
		
		Map<String,Object> map = new HashMap<>();
		map.put("referer", referer);
		
        return map;
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Object getBoardInfo(int no,int sleep){
    	this.threadSleepRandom(sleep,0);    	
    	return this.boardService.getBoardInfo(no);
    }

    private void threadSleepRandom(long base,long scale) {
    	if(base<=0) {return;}
		try {
			long millis = base+(long)(Math.random()*scale);
			log.debug(">> sleep:{}",millis);
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
