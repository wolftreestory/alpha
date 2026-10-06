package com.alpha.base.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.alpha.base.advice.ExceptionAdvice;
import com.alpha.base.config.JwtConfig.JwtManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@ConditionalOnBean(com.alpha.base.config.JwtConfig.JwtManager.class)
@ConditionalOnExpression("T(com.alpha.base.SecurityConfig).isEnabled() && T(com.alpha.base.JwtConfig).isEnabled()")
@ConditionalOnProperty(name="alpha.security.enabled", havingValue="true", matchIfMissing=false)
@ConditionalOnWebApplication
public class SecurityConfig {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
	private enum Type {permitAll,authenticated,roles,authority};
    
	private final JwtManager jwtManager;

	private final DefaultSecurityProperties defaultSecurityProps;
	
	private final BizSecurityProperties bizSecurityProps;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Component
	@ConfigurationProperties(prefix="security.default")
	@PropertySource(value = "classpath:security-default.yml", ignoreResourceNotFound=true, factory = PropertiesConfig.YamlPropertySourceFactory.class)
	@ConditionalOnProperty(name="alpha.security.enabled", havingValue="true", matchIfMissing=false)
	private static class DefaultSecurityProperties extends AbstractSecurityProperties{
		private DefaultSecurityProperties() {log.info(">> securityConfig: security-default.yml");}		
	}

	@Component
	@ConfigurationProperties(prefix="security")
	@PropertySource(value = "classpath:security.yml", ignoreResourceNotFound=true, factory = PropertiesConfig.YamlPropertySourceFactory.class)
	@ConditionalOnProperty(name="alpha.security.enabled", havingValue="true", matchIfMissing=false)
	private static class BizSecurityProperties extends AbstractSecurityProperties{
		private BizSecurityProperties() {log.info(">> securityConfig: security.yml");}	
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unused")
	private static abstract class AbstractSecurityProperties {
		private static final String[] default_authenticated = {"/**"};
		private static final String[] default_permitAll = {"/healthcheck"};
		
	    private Map<String, List<String>> authorities;
	    private Map<String, List<String>> roles;
	    private List<String> authenticated;
	    private List<String> permitAll;

	    public Map<String, List<String>> getAuthorities() {return Optional.ofNullable(this.authorities).orElse(Collections.emptyMap());}
		public void setAuthorities(Map<String, List<String>> authorities) {this.authorities = invert(authorities);}

	    public Map<String, List<String>> getRoles() {return Optional.ofNullable(this.roles).orElse(Collections.emptyMap());}
	    public void setRoles(Map<String, List<String>> roles) {this.roles = invert(roles);}	

	    public List<String> getAuthenticated() {return Optional.ofNullable(this.authenticated).orElse(Arrays.asList(default_authenticated));}
		public void setAuthenticated(List<String> authenticated) {this.authenticated = authenticated;}
		
		public List<String> getPermitAll() {return Optional.ofNullable(this.permitAll).orElse(Arrays.asList(default_permitAll));}
		public void setPermitAll(List<String> permitAll) {this.permitAll = permitAll;}
		
		private Map<String, List<String>> invert(Map<String, List<String>> source) {
	        Map<String, List<String>> invertedMapping = new HashMap<>();
	        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
	            String authority = entry.getKey();
	            List<String> urlPattens = entry.getValue();
	            for (String urlPattern : urlPattens) {
	                List<String> authorities = invertedMapping.getOrDefault(urlPattern, new ArrayList<>());
	                authorities.add(authority);
	                invertedMapping.put(urlPattern, authorities);
	            }
	        }
	        return invertedMapping;
	    }
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Bean
	PasswordEncoder passwordEncoder() {
		// PasswordEncoder: 비밀번호를 안전하게 저장할 수 있도록 비밀번호의 단방향 암호화를 지원하는 인터페이스
		return new BCryptPasswordEncoder();
	}

    @Bean(WebMvcConfig.CORS_FILTER_NAME)
    CorsFilter corsFilter() {
    	// 스프링 시큐리티를 사용하지 않은 경우 전역적으로 CORS 설정을 하면 된다.
        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**", WebMvcConfig.getDefaultCorsConfig());        
        return new CorsFilter(configSource);
    }

	@Bean
	WebSecurityCustomizer webSecurityCustomizer() {
		return webSecurity -> webSecurity.ignoring()
				.requestMatchers(PathRequest.toStaticResources().atCommonLocations()); //정적 자원 ignoring
				//.antMatchers(defaultSecurityProps.getPermitAll()==null?new String[] {}:defaultSecurityProps.getPermitAll().toArray(String[]::new));
	}
    
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

		// CSRF 미적용
		httpSecurity.csrf(csrfCustomizer -> csrfCustomizer.disable());

        // CORS 적용
		httpSecurity.cors(corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {
				@Override
				public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
					return WebMvcConfig.getDefaultCorsConfig();
				}
        	}));

        // session 미적용
		httpSecurity.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // formLogin 미적용
		httpSecurity.formLogin(login -> login.disable());

        // httpBagic 미적용
		httpSecurity.httpBasic(basic -> basic.disable());

        // 인증필터
		httpSecurity.addFilterBefore(jwtManager.getJwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

		// permitAll (인증/권한 적용 미대상)
		httpSecurity.authorizeRequests(requests -> requests.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll());
		defaultSecurityProps.getPermitAll().forEach(pattern->authorizeRequests(Type.permitAll,httpSecurity,pattern));
		bizSecurityProps.getPermitAll().forEach(pattern->authorizeRequests(Type.permitAll,httpSecurity,pattern));

		// authenticated (인증적용)
		defaultSecurityProps.getAuthenticated().forEach(pattern->authorizeRequests(Type.authenticated,httpSecurity,pattern));
		bizSecurityProps.getAuthenticated().forEach(pattern->authorizeRequests(Type.authenticated,httpSecurity,pattern));
		if(defaultSecurityProps.getAuthenticated().isEmpty() && bizSecurityProps.getAuthenticated().isEmpty()) {
			httpSecurity.authorizeRequests(requests -> requests.anyRequest().authenticated());			
		}

        // hasAnyAuthority (권한, authority 는 접두어가 붙지 않음)
		defaultSecurityProps.getAuthorities().forEach((pattern,authorities)->authorizeRequests(Type.authority,httpSecurity,pattern,authorities));
		bizSecurityProps.getAuthorities().forEach((pattern,authorities)->authorizeRequests(Type.authority,httpSecurity,pattern,authorities));
		
		// hasAnyRole (권한, role 은 설정한 값에 접두어인 ROLE_이 붙음)
		defaultSecurityProps.getRoles().forEach((pattern,roles)->authorizeRequests(Type.roles,httpSecurity,pattern,roles));
		bizSecurityProps.getRoles().forEach((pattern,roles)->authorizeRequests(Type.roles,httpSecurity,pattern,roles));
		
        // Exception Handling
		httpSecurity.exceptionHandling(handling -> handling
        		.authenticationEntryPoint((request, response, exception) -> {
        	        log.debug(">> 인증 실패: {}", exception.getMessage());        	        
        	        writeExceptionMessage(request, response, HttpStatus.UNAUTHORIZED, exception);
        	        })        		
        		.accessDeniedHandler((request, response, exception) -> {
        	        log.debug(">> 인가 실패: {}", exception.getMessage());
        	        writeExceptionMessage(request, response, HttpStatus.FORBIDDEN, exception);
        	        })
        		);

		return httpSecurity.build();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void authorizeRequests(Type type, HttpSecurity httpSecurity, String pattern) {
    	this.authorizeRequests(type, httpSecurity, pattern, null);
    }
    
    private void authorizeRequests(Type type, HttpSecurity httpSecurity, String pattern, List<String> list) {
		try {
			if(Type.permitAll.equals(type)) {httpSecurity.authorizeRequests(requests -> requests.antMatchers(pattern).permitAll());}
			if(Type.authenticated.equals(type)) {httpSecurity.authorizeRequests(requests -> requests.antMatchers(pattern).authenticated());}
			if(Type.roles.equals(type)) {httpSecurity.authorizeRequests(requests -> requests.antMatchers(pattern).hasAnyRole(list.toArray(new String[0])));}
			if(Type.authority.equals(type)) {httpSecurity.authorizeRequests(requests -> requests.antMatchers(pattern).hasAnyAuthority(list.toArray(new String[0])));}		
		} catch (Exception e) {
			//log.error(">> {}",e.getMessage());		
			throw new RuntimeException(e.getMessage());
		}
    }
    
	private void writeExceptionMessage(HttpServletRequest request, HttpServletResponse response, HttpStatus status, Exception exception) throws IOException {
		ExceptionAdvice.printTrace(exception);

        Gson gson = new GsonBuilder().disableHtmlEscaping().create();

        Map<String, Object> map = new HashMap<>();
        map.put("code", status.value());
        map.put("status", status);
        map.put("message", exception.getMessage());

        response.setStatus(status.value());
        response.setContentType("application/json; charset=UTF-8");

        try (PrintWriter writer = response.getWriter()) {
            writer.write(gson.toJson(map));
            writer.flush();
        }		
	}
   
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * 
	DelegatingFilterProxy:
		스프링 시큐리티는 서블릿필터(Servlet-Filter)레벨에서 인증 및 인가를 처리하기 위한 프레임워크임.
		스프링 빈은 스프링 컨테이너에서 관리되는 컴포넌트이고 서블릿영역(서블릿 컨테이너)과는 분리된 개념이기 때문에 서블릿필터에서는 스프링 빈을 주입받아서 활용할 수 없다.
		서블릿 컨테이너 ↔ 스프링 컨테이너 사이에서 Bridge 역할이 필요하며, 이러한 역할을 DelegatingFilterProxy로 수행한다.
		(DelegatingFilterProxy는 스프링 컨테이너의 빈(bean)이름을 지정하여 서블릿 필터로 등록하며, 런타임시 해당 빈(bean)에 필터처리를 위임 한다.)

		1. SecurityFilterAutoConfiguration을 통해서 DelegatingFilterProxyRegistrationBean을 빈으로 등록 (springSecurityFilterChain이라는 이름을 가진 빈이 존재하는 경우)
		2. DelegatingFilterProxyRegistrationBean에서는 getFilter를 통해서 DelegatingFilterProxy 생성
		3. ServletContainerInitializerBeans에서 여러 필터들을 add (DelegatingFilterProxy도 포함)
		*. 스프링 시큐리티는 FilterChainProxy클래스를 springSecurityFilterChain이라는 이름으로 빈 등록 함

	FilterChainProxy: 
		DelegatingFilterProxy에게 요청을 위임 받고 실제 보안 처리를 한다.
		실제 보안(인증/인가)처리를 수행하는 SecurityFilterChain 빈(bean)객체를 List로 구성(List<SecurityFilterChain>)하고, 런타임시 요청에 적절한 SecurityFilterChain을 찾는다.
		설정에 따라서 SecurityFilterChain이 하나고 될 수도 있고 여러개가 될 수도 있으며 SecurityFilterChain안에 걸릴 Security Filter들을 다르게 설정할 수 있음.
		다만 한 요청의 URL패턴에 따라 SecurityFilterChain 하나를 선택하여 해당 되는 Security Filter 적용한다.
		WebSecurityConfigurerAdapter를 상속(확장)하여 SecurityFilterChain를 여러개 등록 할 수 있다. (Spring Security 5.4 이후부터 WebSecurityConfigurerAdapter가 Deprecated)
		일반적으로 단일 SecurityFilterChain를 사용하며, HttpSecurity에 대한 구성/설정 후 build 메소드를 사용하여 SecurityFilterChain 생성하여 빈(bean)으로 등록. 

	WebSecurity
		FunctionalInterface인 WebSecurityCustomizer는 단 하나의 메소드를 가진다. 이 메소드의 단일 인자인 WebSecurity에 대한 설정을 람다함수로 만든다.
		(WebSecurityConfigurerAdapter의 Configure(WebSecurity)를 재정의(Override)하여 WebSecurity설정, Spring Security 5.4 이후부터 WebSecurityConfigurerAdapter가 Deprecated)
		WebSecurity, HttpSecurity에 모두 설정을 한 경우 WebSecurity가 HttpSecurity보다 우선적으로 적용된다., HttpSecurity의 설정은 무시되어 SecurityFilterChain 거치지 않는다.
		일반적으로 HttpSecurity에서는 ignoring 을 사용하지 보안적용을 대상이 아닌 것을 명시적으로 적용한다.
		WebSecurity의 antMatchers에 패턴을 적용 후 기동시 경고문이 출력된다. (please use permitAll via HttpSecurity#authorizeHttpRequests instead.)
		이는 HttpSecurity의 authorizeHttpRequests를 사용하여 적용하라는 것으로 WebSecurityConfigurerAdapter클래스가 향 후 Deprecated 됨에 따라 권장안내임.
		
	HttpSecurity
		SecurityFilterChain 빈(bean)을 생성하는 method 인자로 주입 받아서 설정 할 수 있다.
		(WebSecurityConfigurerAdapter의 Configure(WebSecurity)를 재정의(Override)하여 WebSecurity설정, Spring Security 5.4 이후부터 WebSecurityConfigurerAdapter가 Deprecated)
		리소스(URL) 접근 권한 설정
		커스텀 로그인 페이지 지원
		인증 후 성공/실패 핸들링
		사용자 로그아웃
		CSRF 공격으로 부터 보호

	------------------------------------------------------------------------------------------------------------------------------

	CSRF: Cross-Site Request Forgery
		세션(쿠키기반) 로그인을 사용하는 서버에서 정상적으로 인증을 받은 후 악성 스크립트를 실행하게끔 하여 서버가 악의적인 요청을 처리하도록 하는 방법.
		스프링 시큐리티는 CSRF공격을 방지를 디폴트로 설정하고 있다. 사이트간 위조 방지를 목적으로 특정한 값의 토큰을 사용하여 요청을 검증하는 방식.
		
		서버에서 사용자의 요청에 Referrer 정보를 확인하는 방법이 있다. 호스트(host)와 Referrer 일치여부 체크.
		임의의 CSRF 토큰을 만들어 세션에 저장한다. 요청하는 페이지에 hidden 타입 input 태그를 이용해 토큰 값을 함께 전달, 서버는 일치여부 체크.
		
		RestAPI를 제공하는 서버는 session(쿠키)기반 인증과는 다르게 서버에 인증정보를 보관하지 않고 요청에 필요한 인증 정보를(OAuth2,JWT등)을 포함하여 요청하는 것이 일반적이다. 
		따라서 서버에 인증정보를 저장하지 않기 때문에 굳이 불필요한 CSRF protection 을 하지 않는다.
		
	CORS: Cross-Origin Resource Sharing
		브라우저는 기본적으로 응답 받은 페이지에서 다른 출처(도메인, 프로토콜, 포트)의 자원을 요청 할 수 없다. (응답을 준 서버로만 요청을 보낼 수 있다)
		다른 출처의 리소스를 사용해야 하는 경우 권한을 부여하도록 브라우저에 알려주는 체제. (추가 HTTP 헤더를 사용)
		
		다른 출처의 리소스를 요청할 때는 HTTP 프로토콜을 사용하여 요청을 보내게 되는데, 이때 브라우저는 요청 헤더에 Origin이라는 필드에 요청을 보내는 출처를 함께 담아 보낸다. 
		이 요청에 대한 응답을 할 때 응답 헤더의 Access-Control-Allow-Origin 이라는 값에 '이 리소스에 접근하는 것이 허용된 출처'를 담아 보내준다.
		그러면 브라우저는 자신이 보냈던 요청의 Origin과 서버로 부터 받은 응답의 Access-Control-Allow-Origin을 비교해본 후 이 유효한 응답인지를 결정.
		브라우저는 서버의 CORS설정 및 요청상황에 따라 요청시마다 simple-request, preflight-request, credentialed-request 를 택하여 요청한다.
		
		스프링 시큐리티를 사용하지 않은 경우 전역적으로 CORS설정.(CorsFilter를 Bean 등록 하거나, WebMvcConfigurer 구현클레스에서 addCorsMappings 메소드에서 구성)
		authorizeRequests에서 permitAll()로 지정한 요청경로는 전역적으로 설정한 CORS설정을 따름, 그 외 요청은 스프링 시큐리티의 CORS설정을 따름.
			
	formLogin: 
		스프링 시큐리티가 기본적으로 제공하는 로그인 처리/설정.(처리/설정은 임의로 지정/변경 가능)
		username과 password 를 입력하여 로그인 요청(Post Mapping)을 하면 해당 데이터가 서버로 전송, 서버는 로그인 정보를 확인하고, 해당 유저가 존재하면 세션과 인증 토큰을 생성하고 저장한다. 
		
	httpBasic:
		요청시마다 Authorization: Basic {base64(username:password)} 형식의 헤더를 포함하여 요청하여 인증을 처리.
		
	sessionManagement:
		스프링 시큐리티가 기본적으로 제공하는 세션 처리/설정.(처리/설정은 임의로 지정/변경 가능)
		SessionCreationPolicy.ALWAYS: 스프링 시큐리티가 항상 세션을 생성
		SessionCreationPolicy.IF_REQUIRED: 스프링 시큐리티가 필요시 생성(기본)
		SessionCreationPolicy.NEVER: 스프링 시큐리티가 생성하지않지만, 기존에 존재하면 사용
		SessionCreationPolicy.STATELESS: 스프링 시큐리티가 생성하지도 않고 기존 것을 사용하지도 않음 (JWT 같은토큰방식을 쓸때 사용하는 설정)
		
	exceptionHandling:
		인증/인가에서 Error 발생시 후처리를 설정해 줄 필요가 있다.
		스프링 시큐리티는 요청이 컨트롤러에 도달하기 전에 필터 체인에서 예외를 발생시킨다. @ControllerAdvice는 컨트롤러 계층에서 발생하는 예외를 처리하는데, 
		요청이 컨트롤러에 도달하기도 전에 이미 예외가 발생해서 요청이 컨트롤러에 도달하지도 못했기 때문에 @ControllerAdvice에서 처리를 할 수가 없다.
		AuthenticationEntryPoint: 인증이 되지않은 유저가 요청을 했을때 AuthenticationException 예외를 발생하며, 이에 대한 처리 절차를 구성
		AccessDeniedHandler: 서버에 요청을 할 때 액세스가 불가능 했을떄 AccessDeniedException 예외를 발생하며, 이에 대한 처리 절차를 구성
		RestAPI의 경우 적절한 JSON 메세지를 구성하여 ResponseBody에 출력한다.

	UsernamePasswordAuthenticationFilter:
		Form based Authentication 방식으로 인증을 진행할 때 아이디, 패스워드 데이터를 파싱하여 인증 요청을 위임하는 필터.
		formLogin()을 사용하면 스프링 시큐리티에서는 기본적으로 UsernamePasswordAuthenticationFilter 을 사용한다.

	JWT인증필터(JwtAuthenticationFilter):
		클라이언트 요청 시 JWT인증을 하기 위해 설치하는 커스텀 필터로 UsernamePasswordAuthenticationFilter 이전에 실행.
		인증토큰이 없을 경우 bypass하게 되며 UsernamePasswordAuthenticationFilter에서 id/pw기반 인증을 통하여 Authentication객체도 구성하지 못하여 인증관련 예외발생
		이 예외는 exceptionHandling의 AuthenticationEntryPoint에서 처리 한다.
		
	*
	**/
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////  
}
