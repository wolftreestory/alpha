package com.alpha.base.advice;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.alpha.base.annotation.PageHandler;
import com.alpha.base.context.PageContext;

@RestControllerAdvice
@ConditionalOnWebApplication
public class PageAdvice implements ResponseBodyAdvice<Object> {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final static String pagingKey = "SERVER-PAGING";
	
	@Autowired
	private PageContext pagingContext;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String getPagingKey() {
		return pagingKey;
	}
	
	@Override
	public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> messageConverter) {
		List<Annotation> list = Arrays.asList(methodParameter.getMethodAnnotations());		
		return list.stream().anyMatch(a -> a.annotationType().equals(PageHandler.class));
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> messageConverter, ServerHttpRequest request, ServerHttpResponse response) {
		if(this.pagingContext!=null){response.getHeaders().set(pagingKey,this.pagingContext.toJsonBase64());}
		return body;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}