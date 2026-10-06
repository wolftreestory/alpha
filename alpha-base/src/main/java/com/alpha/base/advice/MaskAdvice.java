package com.alpha.base.advice;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.alpha.base.annotation.MaskHandler;
import com.alpha.base.context.MaskContext;

//@Slf4j
@RestControllerAdvice
@ConditionalOnWebApplication
public class MaskAdvice implements ResponseBodyAdvice<Object> {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Autowired
	private MaskContext maskContext;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> messageConverter) {
		if(!this.maskContext.isMaskSupport()) {return false;}
		List<Annotation> list = Arrays.asList(methodParameter.getMethodAnnotations());
		return list.stream().anyMatch(a -> a.annotationType().equals(MaskHandler.class));
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> messageConverter, ServerHttpRequest request, ServerHttpResponse response) {
		if(body==null || !mediaType.equals(MediaType.APPLICATION_JSON)) {return body;}		
		if(!this.maskContext.isMaskApply(request)) {return body;}
        	
		List<Annotation> list = Arrays.asList(methodParameter.getMethodAnnotations());
		Optional<Annotation> optional = list.stream().filter(a -> a.annotationType().equals(MaskHandler.class)).findFirst();		
		MaskHandler maskHandler = (MaskHandler)optional.get();

		return this.maskContext.doMask(body,maskHandler.value());
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}


