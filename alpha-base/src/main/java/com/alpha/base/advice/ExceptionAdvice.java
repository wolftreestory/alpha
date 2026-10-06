package com.alpha.base.advice;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.alpha.base.exception.BusinessException;
import com.alpha.base.exception.SystemException;
import com.alpha.base.support.AbstractMeta;
import com.alpha.base.support.util.SystemUtilPack.SystemUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvice extends AbstractMeta {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final static String DEFAULT_MESSAGE = "시스템 이용에 불편을 드려서 죄송합니다.";
	
	@Value("${spring.application.name}")
	private String applicationName;

	@Value("${spring.profiles.active:local}")
	private String springProfilesActive;
	
	@Value("${project.name:}")
	private String projectName;	
	
	@FunctionalInterface
	public interface ExceptionMessageFunction {public abstract String getMessage();}

	public static String message(String message) {return makeMessage(message,null);}
	public static String message(String message, String arg1) {return makeMessage(message,new Object[] {arg1});}
	public static String message(String message, Object[] args) {return makeMessage(message,args);}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String getTrace(Throwable e) {
		try {
			try(ByteArrayOutputStream out = new ByteArrayOutputStream()){
				PrintStream stream = new PrintStream(out);
				e.printStackTrace(stream);
				return out.toString();
			}
		}catch(Exception ex) {
			return ex.getMessage();
		}
	}

	public static Duration getExceptionTraceDuration() {
		return Duration.ofMinutes(10);
	}
	
	public static void printTrace(Exception ex) {
		String exceptionName = ex.getClass().getName();
		
		StringBuilder traceId = new StringBuilder();
		traceId.append(new SimpleDateFormat("yyyyMMddHHmmssSSS").format(System.currentTimeMillis())).append(":"); //When
		traceId.append(MetaUtil.getSystemUtil().getServerInstanceCode()).append(":"); //Where[base64(IP)+port]
		traceId.append(MetaUtil.getSystemUtil().getApplicationName()).append(":"); //Application
		traceId.append(exceptionName.substring(exceptionName.lastIndexOf(".")+1)); //Exception
				
		log.error("\ntraceId:{}\n{}",traceId.toString(),getTrace(ex));
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static String makeMessage(String message, Object[] args) {		
		String realMessage=MetaUtil.getMessageUtil().getMessage(message==null?"":message,args);
		
		boolean isCode=true;
		if(isCode && message==null) {isCode=false;}
		if(isCode && message.equals(realMessage)) {isCode=false;}
		
		Map<String,Object> map = new HashMap<>();
		map.put("messageCode",message);
		map.put("message",realMessage);
		
		return MetaUtil.getJsonUtil().toJson(map);
	}

	private ResponseEntity<Object> getResponseEntity(HttpStatus status,Map<String,Object> body){
		body.put("code", status.value());
		body.put("status", status);	
		return ResponseEntity.status(status).body(body);
	}
	
	private ResponseEntity<Object> getResponseEntity(HttpStatus status, String message){
		Map<String,Object> body = new HashMap<>();
		body.put("code", status.value());
		body.put("status", status);
		body.put("message", message);		
		return getResponseEntity(status,body);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@ExceptionHandler({BusinessException.class})
	protected ResponseEntity<Object> businessExceptionHandler(BusinessException e, WebRequest request){
		printTrace(e);
		return this.getResponseEntity(e.getHttpStatus(),this.parseMessage(e,request));
	}
	
	@ExceptionHandler({SystemException.class})
	protected ResponseEntity<Object> systemExceptionHandler(SystemException e, WebRequest request){
		printTrace(e);
		return this.getResponseEntity(e.getHttpStatus(),this.parseMessage(e,request));
	}
	
	// 400 Bad Request(Field Bind Argument 예외)
	@ExceptionHandler({BindException.class})
	public ResponseEntity<Object> exceptionHandler(BindException e) {
		printTrace(e);
		return this.getResponseEntity(HttpStatus.BAD_REQUEST,e.getMessage());
	}

	// 400 Bad Request(Field Argument 예외)	
	@ExceptionHandler({MethodArgumentNotValidException.class})
	public ResponseEntity<Object> exceptionHandler(MethodArgumentNotValidException e) {
		printTrace(e);
		StringBuilder builder = new StringBuilder();
		for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
			builder.append(fieldError.getField()).append(":").append(fieldError.getDefaultMessage());
			builder.append("(").append(fieldError.getRejectedValue()).append(")");
		}
		return this.getResponseEntity(HttpStatus.BAD_REQUEST,builder.toString());
	}
	
	// 401 Unauthorized
	@ExceptionHandler({AccessDeniedException.class})
	public ResponseEntity<Object> exceptionHandler(AccessDeniedException e) {
		printTrace(e);
		return this.getResponseEntity(HttpStatus.UNAUTHORIZED,e.getMessage());		
	}

	// 404 Not Found
	@ExceptionHandler({NoHandlerFoundException.class})
	public ResponseEntity<Object> exceptionHandler(NoHandlerFoundException e) {
		printTrace(e);		
		return this.getResponseEntity(HttpStatus.NOT_FOUND,e.getMessage());		
	}

	// 400 Bad Request
	@ExceptionHandler({RuntimeException.class})
	public ResponseEntity<Object> exceptionHandler(RuntimeException e) {
		printTrace(e);		
		return this.getResponseEntity(HttpStatus.BAD_REQUEST,e.getMessage());		
	}

	// 500 Internal Server Error
	@ExceptionHandler({Exception.class})
	public ResponseEntity<Object> exceptionHandler(Exception e) {
		printTrace(e);
		return this.getResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR,e.getMessage());		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Map<String,Object> parseMessage(Throwable ex, WebRequest request){
		String message = ex.getMessage();
		String exceptionName = ex.getClass().getName();
		
		Map<String,Object> map = new HashMap<>();
		map.put("exception",exceptionName);
		map.put("propagation",this.projectName);
		map.put("date",new Date());
		map.put("messageCode","");
		map.put("message",message);
		
		StringBuilder traceId = new StringBuilder();
		traceId.append(new SimpleDateFormat("yyyyMMddHHmmssSSS").format(System.currentTimeMillis())).append(":"); //when
		traceId.append(MetaUtil.getSystemUtil().getServerInstanceCode()).append(":"); //where[base64(ip)+port]
		traceId.append(this.applicationName).append(":"); //application
		traceId.append(exceptionName.substring(exceptionName.lastIndexOf(".")+1)); //exption
				
		log.error("\ntraceId:{}\n{}",traceId.toString(),getTrace(ex));

		//empty 인경우 디폴트메세지 처리
		//com.alpha 패키지 하부의 Exception 이 아닌 runtimeException이 throw 된 경우는 개발자가 의도적으로 메세지 처리를 하지 않은 경우로 디폴트메세지를 사용한다.
		//com.alpha 패키지 하부의 Exception 을 사용한 경우는 메세지가 구성된 Exception 으로 메세지를 추출하여 사용한다.
		boolean isDefault=false;
		if(!isDefault && StringUtils.isBlank(message)) {isDefault=true;}
		if(!isDefault && ex.getClass().getCanonicalName().indexOf("com.alpha")==-1) {isDefault=true;}
		if(isDefault) {
				log.debug(">> Change Default Message:[{}]->[{}]",message,DEFAULT_MESSAGE);
				map.put("message",DEFAULT_MESSAGE);
		}

		//오류가 타 시스템으로 부터 전파경우 메세지 처리
		if(MetaUtil.getJsonUtil().isJsonObject(map.get("message"))) {
			try {
				Map<String,Object> refMap = MetaUtil.getObjectMapperUtil().readValue(String.valueOf(map.get("message")), new TypeReference<Map<String,Object>>(){});
				
				if(refMap.containsKey("propagation")) {map.put("propagation",refMap.get("propagation")+">"+map.get("propagation"));}
				if(refMap.containsKey("messageCode")) {map.put("messageCode",refMap.get("messageCode"));}
				if(refMap.containsKey("message")) {map.put("message",refMap.get("message"));}
				
			}catch(Exception e){
				log.debug(">> Change Default Message:[{}]->[{}]",map.get("message"),DEFAULT_MESSAGE);
				map.put("message",DEFAULT_MESSAGE);
				log.debug(">> 에러메세지 처리시 오류 발생, 처리대상 메세지:{}",map.get("message"));
				log.debug(">> 디폴드 메세지를 사용함 :{}",DEFAULT_MESSAGE);
			}			
		}

		//운영환경이 아닌 경우, 상세 에러메세지를 조회할 수 있는 traceId를 붙인다
		if(!this.springProfilesActive.equals(SystemUtil.PROFILE_PROD)) {
			StringBuilder builder = new StringBuilder();
			builder.append(map.get("message"));
			builder.append("(traceId:").append(traceId).append(")");
			map.put("message",builder.toString());
		}

		return map;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
