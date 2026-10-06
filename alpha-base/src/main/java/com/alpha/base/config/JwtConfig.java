package com.alpha.base.config;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.alpha.base.support.AbstractMeta;
import com.fasterxml.jackson.core.type.TypeReference;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnExpression("T(com.alpha.base.SecurityConfig).isEnabled() && T(com.alpha.base.JwtConfig).isEnabled()")
@ConditionalOnProperty(name="alpha.security.enabled", havingValue="true", matchIfMissing=false)
@ConditionalOnWebApplication
public class JwtConfig extends AbstractMeta {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final String INTERFACE_ACCESS_ID = "ifu"; // interface user fix
	private static final String INTERFACE_ACCESS_ROLE = "ITF"; // interface user
    private static final long INTERFACE_ACCESS_VALID_TIME = 60 * 60 * 24 * 30 * 100 * 1000L; // 100년 무제한
    
    private static final long JWT_MAX_LENGTH = 8 * 1024; //8KB

	@Value("${spring.profiles.active:local}")
	private static String springProfilesActive;
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Component
	@ConfigurationProperties(prefix="alpha.security.jwt.token")
	@Data
	public static class JwtTokenProperties {
		private static enum Type {Authorization,RefreshToken};
	    private static final String DEFAULT_JWT_SCHEME = "Bearer";
		private static final String DEFAULT_SECRET_SEED = "alpha.jwt.secret.seed.π(3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679)";
		private static final long MINUTE = 60 * 1000L; // minute
		private static final long DAY = 60 * 60 * 24 * 1000L; //day

		private TokenSpec access;
		private TokenSpec refresh;
		private String prefix;

		@Data
		public class TokenSpec {
			private String header;
			private String secretKey;
			private Key key;
			private long validTime;
		}
		
		@PostConstruct
		private void PostConstruct() {
			StringBuilder secretBase = new StringBuilder();
			secretBase.append(DEFAULT_SECRET_SEED).append(".").append(springProfilesActive).append(".");
			
			if(this.access==null) {	
				String secretKey = secretBase.append(Type.Authorization).toString();				
				TokenSpec spec = new TokenSpec();
				spec.setHeader(Type.Authorization.toString());
				spec.setSecretKey(Encoders.BASE64.encode(secretKey.getBytes()));
				spec.setValidTime(240 * MINUTE);
				this.setAccess(spec);
			}
			
			if(this.refresh==null) {	
				String secretKey = secretBase.append(Type.RefreshToken).toString();				
				TokenSpec spec = new TokenSpec();
				spec.setHeader(Type.RefreshToken.toString());
				spec.setSecretKey(Encoders.BASE64.encode(secretKey.getBytes()));
				spec.setValidTime(1 * DAY);
				this.setRefresh(spec);
			}
				
			if(!StringUtils.hasText(this.prefix)) {	
				this.setPrefix(DEFAULT_JWT_SCHEME);
			}

			boolean isEnable = true; String errorMsg="";
			if(isEnable && !StringUtils.hasText(access.getHeader())) {isEnable=false; errorMsg="access.header is null or empty";}
			if(isEnable && !StringUtils.hasText(access.getSecretKey())) {isEnable=false; errorMsg="access.secretKey is null or empty";}
			if(isEnable && this.access.getValidTime()<MINUTE) {isEnable=false; errorMsg="access.validTime is wrong.";}
			if(isEnable && !StringUtils.hasText(refresh.getHeader())) {isEnable=false; errorMsg="refresh.header is null or empty";}
			if(isEnable && !StringUtils.hasText(refresh.getSecretKey())) {isEnable=false; errorMsg="refresh.secretKey is null or empty";}
			if(isEnable && this.refresh.getValidTime()<DAY) {isEnable=false; errorMsg="refresh.validTime is wrong.";}			
			if(!isEnable) {throw new RuntimeException(errorMsg);}
			
			this.access.setKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.access.getSecretKey())));
			this.refresh.setKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.refresh.getSecretKey())));
		};
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor	
	private static class UserDetailsMeta implements UserDetails {
		private static final long serialVersionUID = 1L;

		private Map<String,Object> meta;
		private String userName;
		private String role;		
		
		@Override
		public Collection<? extends GrantedAuthority> getAuthorities() {
	        String authorityPreFix = "ROLE_";
	        ArrayList<GrantedAuthority> list = new ArrayList<>();
	        Arrays.asList(role.split(",")).forEach(s -> list.add(new SimpleGrantedAuthority(authorityPreFix + s)));
	        return list;
		}

		@Override
		public String getUsername() {return this.userName;}

		@Override
		public String getPassword() {return "";}

		@Override
		public boolean isAccountNonExpired() {return true;}

		@Override
		public boolean isAccountNonLocked() {return true;}

		@Override
		public boolean isCredentialsNonExpired() {return true;}

		@Override
		public boolean isEnabled() {return true;}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static interface JwtManager {
		
		public JwtTokenProperties getJwtTokenProperties();
		
	    public Filter getJwtAuthenticationFilter();
	    
	    public String createAccessToken(String subject, List<String> roles);
	    public String createAccessToken(String subject, Map<String,Object> meta, List<String> roles);
	    public String createInterfaceAccessToken();
	    public String createRefreshToken(String value);
	    
	    public <T> T getUserDetails(Class<T> clazz);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
    //@Value("${server.meta.textbook.systemType:}")
    //private String systemType;

    //@Value("${alpha.endpoint.userDetailApi:}")
    //private String endpoint_userDetailApi;
    
	@Bean
	JwtManager getJwtManager(JwtTokenProperties props){
		//log.debug(">> JwtTokenProperties:{}",props.toString());
		
		return new JwtManager() {
			
		    @Override
			public JwtTokenProperties getJwtTokenProperties() {
				return props;
			}
			
		    @Override
		    public Filter getJwtAuthenticationFilter() {
		    	
		    	return new OncePerRequestFilter() {
		    		@Override
		    		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		 
		    	        String prefix = props.getPrefix()+" "; 
		    			String header = request.getHeader(props.getAccess().getHeader().toString());
		    	    	String token = (StringUtils.hasText(header) && header.startsWith(prefix))?header.substring(prefix.length()):null;
		    	        //log.debug(">> token: {}",token);
		    	        
		    	        if(StringUtils.hasText(token)) {
		    		        Key key = props.getAccess().getKey();
		    		        JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
		    		        
		    		        Jws<Claims> claims = jwtParser.parseClaimsJws(token);
		    		        log.debug(">> claims: {}",claims);

		    		        UserDetails userDetails = getUserDetails(claims);	    		        
		    		        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());	            
		    	            SecurityContextHolder.getContext().setAuthentication(authentication);           
		    	            
		    	            log.debug(">> authentication: {}",SecurityContextHolder.getContext().getAuthentication());
		    	        }
		    	        
		    	        filterChain.doFilter(request, response);
		    		} 	    		
		    	};
		    }
		    
		    @Override
		    public String createAccessToken(String subject, List<String> roles) {
		    	return this.createAccessToken(subject, null, roles);
		    }

		    @Override
		    public String createAccessToken(String subject, Map<String,Object> meta, List<String> roles) {
		    	Claims claims = Jwts.claims().setSubject(subject);
		        claims.put("roles", roles);

		    	if(meta!=null) {
			    	String json = MetaUtil.getJsonUtil().toJson(meta);
			    	claims.put("meta",MetaUtil.getCryptoUtil().getAES256().encryption(json));
		    	}
		        		        
		        String token = this.createToken(claims,new Date(),props.getAccess().getKey(),props.getAccess().getValidTime());
		        log.debug(">> accessToken: {}",token);
		        
		        return token;	        
		    }
		    	
		    @Override
		    public String createInterfaceAccessToken() {
		        Claims claims = Jwts.claims().setSubject(INTERFACE_ACCESS_ID);
		        claims.put("roles", INTERFACE_ACCESS_ROLE);
		        String token = this.createToken(claims,new Date(),props.getAccess().getKey(),INTERFACE_ACCESS_VALID_TIME);
		        log.debug(">> interfaceAccessToken: {}",token);
		        
		        return token;
		    }
		    
		    @Override
		    public String createRefreshToken(String value) {    	
		        Claims claims = Jwts.claims();
		        claims.put("value", value);		
		        String token = this.createToken(claims,new Date(),props.getAccess().getKey(),props.getRefresh().getValidTime());
		        log.debug(">> refreshToken: {}",token);
		        return token;
		    }

		    @Override
		    public <T> T getUserDetails(Class<T> clazz) {
		    	//SecurityContextHolder 유저정보
		    	Object object = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		    	if(object==null) {return null;}
		    	UserDetailsMeta userDetailsMeta = (UserDetailsMeta)object;	    	
		    	return MetaUtil.getObjectMapperUtil().convert(userDetailsMeta.getMeta(), clazz); 	
		    }

			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		    private String createToken(Claims claims, Date date, Key tokenKey, long validTime) {
		        String token = Jwts.builder().setClaims(claims)
		                .setIssuedAt(date).setExpiration(new Date(date.getTime() + validTime))
		                .signWith(tokenKey, SignatureAlgorithm.HS512).compact();
		        if(token.getBytes().length > JWT_MAX_LENGTH) {
		        	throw new RuntimeException("token too long : "+token);
		        }
		        return token;
		    }
		    
		    private UserDetails getUserDetails(Jws<Claims> claims) {
		    	String subject = claims.getBody().getSubject();
		    	log.debug(">> claims.body.subject: {}",subject);
		    	
		    	if (!StringUtils.hasText(subject)) {
		    		throw new RuntimeException("claims.body.subject is null or blank.");
		    	}    
		    	
		    	UserDetails userDetails = null;
		    	
		        // case1
		        if (userDetails==null && subject.equals(INTERFACE_ACCESS_ID)) {
		        	log.debug(">> userDetails구성.인터페이스");	        	
		        	userDetails = UserDetailsMeta.builder().userName(subject).role(INTERFACE_ACCESS_ROLE).build();
		        }
		
		        // case2
		        if(userDetails==null && claims.getBody().containsKey("meta")) {
		        	log.debug(">> userDetails구성.meta추출");
		        	String claimsBodyMeta = claims.getBody().get("meta", String.class);
	        		//log.debug(">> claimsBodyMeta: {}",claimsBodyMeta);
		        	try {
			        	if(StringUtils.hasText(claimsBodyMeta)) {
			        		String data = MetaUtil.getCryptoUtil().getAES256().decryption(claimsBodyMeta);
			        		//log.debug(">> data: {}",data);
				        	Map<String,Object> meta = MetaUtil.getObjectMapperUtil().readValue(data, new TypeReference<Map<String,Object>>(){});
			        		//log.debug(">> meta: {}",meta);			        	
				        	userDetails = UserDetailsMeta.builder().meta(meta).userName(subject).role(meta.get("role").toString()).build();
			        	}
		        	}catch(Exception e) {
		        		log.error(">> userDetails구성.meta추출: {}",e.getMessage());
		        	}       	
		        }
		        
		        /*
		        // case3
		        if (userDetails==null && systemType.equals("alpha-api-lm")) {
		        	log.debug(">> userDetails구성 DB조회");
		        	if(commonDao==null) {return null;}
		        	Object data = commonDao.select("api.at.user.selectUserDetail", subject);
		        	Map<String,Object> meta = MetaUtil.getObjectMapperUtil().convert(data, new TypeReference<Map<String,Object>>(){});	        	
		        	userDetails = UserDetailsMeta.builder().meta(meta).userName(subject).role(meta.get("role").toString()).build();
		        }
		        
		        // case4
		        if (userDetails==null && StringUtils.hasText(endpoint_userDetailApi)) {
			        log.debug(">> userDetails구성.api호출");
			        
			        HttpHeaders httpHeaders = new HttpHeaders();
			        httpHeaders.add("Content-Type", "application/json");
			
			        Map<String,Object> request = new HashMap<>();
			        request.put("usrId", subject);
			        request.put("userName", subject);
			        request.put("subject", subject);
			        
			        User.ResponseDto response = webFluxUtil.post(endpoint_userDetailApi,httpHeaders,request,User.ResponseDto.class);
			        //log.debug(">> response:{}",response);
			        if (response == null) {throw new RuntimeException("userResponseDto is null");}
			        if (StringUtils.hasText(response.getMessage())) {throw new RuntimeException(response.getMessage());}
			        
			        Map<String,Object> meta = MetaUtil.getObjectMapperUtil().convert(response.getData(), new TypeReference<Map<String,Object>>(){});	  
			        userDetails = UserDetailsMeta.builder().meta(meta).userName(subject).role(meta.get("role").toString()).build();		        
		        }
		        */
		        
		        if(userDetails == null) {
		        	throw new RuntimeException("Failure to create/configure user details.");
		        }
		        
		        return userDetails;
		    }
		};
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    
}