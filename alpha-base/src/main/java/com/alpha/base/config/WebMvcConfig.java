package com.alpha.base.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnExpression("T(com.alpha.base.WebMvcConfig).isEnabled()")
@ConditionalOnWebApplication
public class WebMvcConfig implements WebMvcConfigurer {

	public static final String CORS_FILTER_NAME = "default.corsFilter";
	
	public enum LocaleResolverType {AcceptHeader,Cookie,Sesssion};
	
	private LocaleResolverType localeResolverType = LocaleResolverType.AcceptHeader;
	
	@Value("${project.name:}")
	private String projectName;	

	@Value("${interceptor.enabled:true}")
	private boolean isEnable_interceptor;

	@Value("${interceptor.locale.enabled:true}")
	private boolean isEnable_localeInterceptor;

	@Autowired(required=false) @Qualifier(CORS_FILTER_NAME)
	private CorsFilter corsFilter = null;
	
	//@Autowired(required=false)
	//private ObjectMapper objectMapper;
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static CorsConfiguration getDefaultCorsConfig() {
        // allowedOrigins("*")와 allowCredentials(true)를 동시에 사용할 수 없음
		// allowCredentials(true) 설정시 allowedOrigins("*") -> allowedOriginPatterns("*") 를 사용해야 함.

		CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Collections.singletonList("*")); // Access-Control-Allow-Origin : 요청을 보내는 페이지의 출처 (*, 도메인)
        config.setAllowedMethods(Collections.singletonList("*")); // Access-Control-Allow-Methods : 요청을 허용하는 메소드 (Default : GET, POST, HEAD)
        config.setAllowedHeaders(Collections.singletonList("*")); // Access-Control-Allow-Headers : 요청을 허용하는 헤더
        config.setAllowCredentials(true); // Access-Control-Allow-Credentials : 쿠키를 요청에 포함, 쿠키 인증 요청 허용 (요청시 withCredentials:true)
        config.setMaxAge(3600L); // Access-Control-Max-Age : 클라이언트에서 preFlight의 요청 결과를 저장할 시간(초) 지정
        config.setExposedHeaders(Collections.singletonList("*")); // JavaScript에서 참조하기 위한 추가, CORS의 경우 기본적으로 화면(JavaScript)에서 response header 값을 읽지 못함.

        return config;
    }
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override	
	public void addCorsMappings(CorsRegistry registry) {
		//corsFilter 가 존재할 경우 CorsRegistry를 구성하지 않음
		if(corsFilter!=null) {return;}
		
		CorsConfiguration config = getDefaultCorsConfig();
		registry.addMapping("/**")		
			.allowedOriginPatterns(config.getAllowedOriginPatterns().toArray(new String[0]))
			.allowedHeaders(config.getAllowedHeaders().toArray(new String[0]))
			.allowedMethods(config.getAllowedMethods().toArray(new String[0]))
			.allowCredentials(config.getAllowCredentials())
			.maxAge(config.getMaxAge())
			.exposedHeaders(config.getExposedHeaders().toArray(new String[0]));
	}
	
    @Override
    public void addInterceptors(InterceptorRegistry registry) {    	
    	if(!isEnable_interceptor) {return;}

    	int order=0;
        if(isEnable_localeInterceptor) {registry.addInterceptor(localeChangeInterceptor()).order(++order);}
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//@Bean
	//MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
	//	// Bean 등록시 스프링 컨텍스트의 Converter 리스트에 이를 자동으로 추가
	//
	//	ObjectMapper mapper = objectMapper==null?null:objectMapper.copy();		
	//	if(objectMapper==null) {
	//		mapper = new Jackson2ObjectMapperBuilder()
	//				.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
	//				.modules(new JavaTimeModule())
	//				.timeZone(Calendar.getInstance().getTimeZone())
	//				.build();
	//	}
	//	
	//	mapper.getFactory().setCharacterEscapes(new CharacterEscapes() {
	//		private static final long serialVersionUID = 1L;
	//
	//		private int[] asciiEscapes;
	//		
	//		@Override
	//		public int[] getEscapeCodesForAscii() {
	//			if(asciiEscapes==null || asciiEscapes.length==0) {
	//                asciiEscapes = CharacterEscapes.standardAsciiEscapesForJSON();
	//                asciiEscapes['<'] = CharacterEscapes.ESCAPE_CUSTOM;
	//                asciiEscapes['>'] = CharacterEscapes.ESCAPE_CUSTOM;
	//                asciiEscapes['&'] = CharacterEscapes.ESCAPE_CUSTOM;
	//                asciiEscapes['\"'] = CharacterEscapes.ESCAPE_CUSTOM;
	//                asciiEscapes['('] = CharacterEscapes.ESCAPE_CUSTOM;
	//                asciiEscapes[')'] = CharacterEscapes.ESCAPE_CUSTOM;
	//                asciiEscapes['#'] = CharacterEscapes.ESCAPE_CUSTOM;
	//                asciiEscapes['\''] = CharacterEscapes.ESCAPE_CUSTOM;
	//			}
	//			return asciiEscapes;
	//		}
	//
	//		@Override
	//		public SerializableString getEscapeSequence(int ch) {
	//			return new SerializedString(StrialphacapeUtils.escapeHtml4(Character.toString(ch)));
	//		}			
	//	});
	//	
	//	MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
	//	log.info(">> converter: {}",converter.getClass().getSimpleName());
	//	
	//    return converter;
	//}

    
    @Bean
    ErrorAttributes errorAttributes() {
    	
		return new DefaultErrorAttributes() {
			@Override
			public Map<String,Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
				Integer status = (Integer) webRequest.getAttribute("javax.servlet.error.status_code", RequestAttributes.SCOPE_REQUEST);				
				if(status!=HttpStatus.NOT_FOUND.value()) {return super.getErrorAttributes(webRequest, options);}
				
				Map<String, Object> errorAttributes = new HashMap<>();				
				errorAttributes.put("httpStatus", HttpStatus.NOT_FOUND.value());
				errorAttributes.put("code", "404");
				errorAttributes.put("status", "404");
				errorAttributes.put("message", "Not Found");
				//errorAttributes.put("messageCode", "404");
				//errorAttributes.put("exception", "");
				//errorAttributes.put("propagation", projectName);
	
				return errorAttributes;
			}
		};
	}

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    @ConditionalOnExpression("'${interceptor.enabled:true}'.equals('true') && '${interceptor.locale.enabled:true}'.equals('true')")
    HandlerInterceptor localeChangeInterceptor() {
    	LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
    	log.info(">> interceptor-localeChangeInterceptor [paramName:{}]",interceptor.getParamName());
        return interceptor;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Bean
    LocaleResolver localeResolver() {
    	LocaleResolver localeResolver = null;
    	Locale defaultLocale = Locale.KOREA;
    	if(localeResolverType.equals(LocaleResolverType.AcceptHeader)) {
        	// 헤더로부터 Locale 정보를 구한다. setLocale() 메서드를 지원 하지 않는다.(웹브라우저의 locale 변경)
    		// localeChangeInterceptor를 통한 local 변경 불가(오류발생)
        	AcceptHeaderLocaleResolver _localeResolver = new AcceptHeaderLocaleResolver();
        	_localeResolver.setDefaultLocale(defaultLocale);   		
        	localeResolver=_localeResolver;
    	}
    	if(localeResolverType.equals(LocaleResolverType.Cookie)) {
    		// 쿠키를 이용해서 Locale 정보를 구한다. setLocale() 메서드는 쿠키에 Locale 정보를 저장
    		// localeChangeInterceptor를 통한 local 변경 가능
    		CookieLocaleResolver _localeResolver = new CookieLocaleResolver();
        	_localeResolver.setDefaultLocale(defaultLocale);
        	localeResolver=_localeResolver;
    	}    	
    	if(localeResolverType.equals(LocaleResolverType.Sesssion)) {
    		// 세션으로부터 Locale 정보를 구한다. setLocale() 메서드는 세션에 Locale 정보를 저장
    		// localeChangeInterceptor를 통한 local 변경 가능
        	SessionLocaleResolver _localeResolver = new SessionLocaleResolver();
        	_localeResolver.setDefaultLocale(defaultLocale);
        	localeResolver=_localeResolver;
    	}    	
    	log.info(">> locale.resolver: {}",localeResolver.getClass().getSimpleName());
    	log.info(">> locale.default: {}",defaultLocale);
    	return localeResolver;
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //세션미사용주석처리
	//@Bean
	//public HttpSessionListener getHttpSessionListener() {
	//    return new HttpSessionListener() {
	//    	
	//        @Override
	//        public void sessionCreated(HttpSessionEvent se) {
	//        	HttpSession s = se.getSession();
	//    		log.debug(">> 세션.생성[id:{},creationTime:{},maxInactiveInterval:{}]",s.getId(),s.getCreationTime());
	//        }
	//
	//        @Override
	//        public void sessionDestroyed(HttpSessionEvent se) {
	//        	HttpSession s = se.getSession();
	//    		log.debug(">> 세션.종료[id:{},creationTime:{},maxInactiveInterval:{}]",s.getId(),s.getCreationTime());
	//        }
	//    };
	//}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//addCorsMappings 메소드 적용으로 주석처리
	//	@Bean
	//	public FilterRegistrationBean<Filter> filterRegistrationBean() { 
	//		CorsConfiguration config = new CorsConfiguration();
	//		config.setMaxAge(1800L); // Access-Control-Max-Age : 클라이언트에서 preFlight의 요청 결과를 저장할 시간(초) 지정 (default:1800L)
	//		config.addAllowedOrigin("*"); // Access-Control-Allow-Orgin : 요청을 보내는 페이지의 출처 (*, 도메인)
	//		config.addAllowedHeader("*"); // Access-Control-Allow-Headers : 요청을 허용하는 헤더
	//		config.addAllowedMethod("*"); // Access-Control-Allow-Methods : 요청을 허용하는 메소드 (Default : GET, POST, HEAD)
	//		config.setAllowCredentials(true); // Access-Control-Allow-Credentials : 쿠키를 요청에 포함 (요청시 withCredentials:true)
	//		config.addExposedHeader(PageAdvice.getPagingKey()); // JavaScript에서 참조하기 위한 추가, CORS의 경우 기본적으로 화면(JavaScript)에서 response header 값을 읽지 못함.
	//		
	//		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	//		source.registerCorsConfiguration("/**", config);
	//		
	//		Filter filter = new BaseCorsFilter(source);
	//		
	//		FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(filter);
	//		bean.setOrder(0);
	//		
	//		return bean;
	//	} 
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}