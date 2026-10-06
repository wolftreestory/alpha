package com.alpha.base.support.util;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.alpha.base.exception.SystemException;
import com.alpha.base.support.util.RestTemplateUtilPack.RestTemplateUtil.RestAttachType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RestTemplateUtilPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface RestTemplateUtil extends RestOperations {
		
		@FunctionalInterface
		public interface FetchBodyFunction {public abstract MultiValueMap<String, Object> getBody() throws Exception;}

		public enum RestAttachType {buffer,stream};
				
	    public static final int DEFAULT_MAX_CONN_TOTAL = 50;
	    public static final int DEFAULT_MAX_CONN_ROUTE = 50;
	    public static final int DEFAULT_CONNECTION_TIMEOUT = 2*1000; //ms
	    public static final int DEFAULT_READ_TIMEOUT = 5*1000; //ms 

	    public RestTemplateUtil build(RestTemplateHttpClientConfig config);
	    
	    public void setRestTemplateUtilFactory(RestTemplateUtilFactory factory);
	    
	    public <T> T attachBuffer(URI remoteURI, String fileParamName, MultipartFile files, TypeReference<T> typeReference) throws Exception;
	    public <T> T attachBuffer(URI remoteURI, String fileParamName, List<MultipartFile> files, TypeReference<T> typeReference) throws Exception;
	    
	    public <T> T attachStream(URI remoteURI, String fileParamName, MultipartFile files, TypeReference<T> typeReference) throws Exception;
	    public <T> T attachStream(URI remoteURI, String fileParamName, List<MultipartFile> files, TypeReference<T> typeReference) throws Exception;

	    public <T> T attach(RestAttachType restAttachType, URI remoteURI, FetchBodyFunction fetchBody, TypeReference<T> typeReference) throws Exception;
	}

	public interface RestTemplateUtilFactory extends FactoryBean<RestTemplateUtil> {
		public RestTemplateUtil getInstance();
		public RestTemplateUtil getObject(RestTemplateHttpClientConfig config) throws Exception;
	}
	
	@Data @Builder @ToString
	public static class RestTemplateHttpClientConfig {	
		@Builder.Default RestAttachType restAttachType = RestAttachType.buffer;
		@Builder.Default private int maxConnTotal = RestTemplateUtil.DEFAULT_MAX_CONN_TOTAL;
		@Builder.Default private int maxConnRoute = RestTemplateUtil.DEFAULT_MAX_CONN_ROUTE;
		@Builder.Default private int connTimeout = RestTemplateUtil.DEFAULT_CONNECTION_TIMEOUT;
		@Builder.Default private int readTimeout = RestTemplateUtil.DEFAULT_READ_TIMEOUT;	
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static class RestTemplateUtilContext {
    	private static Map<String,RestTemplateUtil> context = new HashMap<>();

    	public static String buildKey(String bufferYn, int... keys) {
    		StringBuilder builder = new StringBuilder();    		
    		builder.append("++").append(bufferYn);
    		for(int i=0;i<keys.length;i++) {builder.append(":").append(keys[i]);}
    		builder.append("++");
    		return builder.toString();
    	}    	
    	public static boolean isExist(String key){return context.containsKey(key);}
    	public static RestTemplateUtil get(String key){return isExist(key)?context.get(key):null;}    	
    	public static synchronized void add(String key,RestTemplateUtil restTemplateUtil){context.put(key, restTemplateUtil);}
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static RestTemplateUtil getRestTemplateUtil() {
		
		return new RestTemplateUtilFactory() {

			@Override
			public RestTemplateUtil getInstance() {
				try {return this.getObject();} catch (Exception e) {throw new SystemException(e.getMessage());}
			}
			
			@Override
			public RestTemplateUtil getObject() throws Exception {
				return this.getObject(RestAttachType.buffer,RestTemplateUtil.DEFAULT_MAX_CONN_TOTAL,RestTemplateUtil.DEFAULT_MAX_CONN_ROUTE,RestTemplateUtil.DEFAULT_CONNECTION_TIMEOUT,RestTemplateUtil.DEFAULT_READ_TIMEOUT);
			}

			@Override
			public RestTemplateUtil getObject(RestTemplateHttpClientConfig config) throws Exception {
				return this.getObject(config.getRestAttachType(),config.getMaxConnTotal(),config.getMaxConnRoute(),config.getConnTimeout(),config.getReadTimeout());
			}

			@Override
			public Class<?> getObjectType() {
				return RestTemplateUtil.class;
			}

			private RestTemplateUtil getObject(RestAttachType restAttachType,int maxConnTotal, int maxConnRoute, int connectTimeout, int readTimeout) throws Exception {

				/*******************************************************************************/
				class _RestTemplateUtil extends RestTemplate implements RestTemplateUtil {
					
					private RestTemplateUtilFactory factory;
					
					private _RestTemplateUtil(ClientHttpRequestFactory requestFactory) {
						super(requestFactory);
					}

					@Override
				    public RestTemplateUtil build(RestTemplateHttpClientConfig config) {
						try {
							return this.factory.getObject(config);
						} catch (Exception e) {
							throw new SystemException(e.getMessage());
						}
				    }
					
					@Override
				    public void setRestTemplateUtilFactory(RestTemplateUtilFactory factory) {
						this.factory = factory;
				    }
				    
					@Override
					public <T> T attachBuffer(URI remoteURI, String fileParamName, MultipartFile file, TypeReference<T> typeReference) throws Exception{
						return this.attach(RestAttachType.buffer, remoteURI, ()->{return this.makeBody(fileParamName, file, RestAttachType.buffer);}, typeReference);				
					}

					@Override
				    public <T> T attachBuffer(URI remoteURI, String fileParamName, List<MultipartFile> files, TypeReference<T> typeReference) throws Exception{
						return this.attach(RestAttachType.buffer, remoteURI, ()->{return this.makeBody(fileParamName, files, RestAttachType.buffer);}, typeReference);
					}
					
					@Override
				    public <T> T attachStream(URI remoteURI, String fileParamName, MultipartFile file, TypeReference<T> typeReference) throws Exception{
						return this.attach(RestAttachType.stream, remoteURI, ()->{return this.makeBody(fileParamName, file, RestAttachType.stream);}, typeReference);
					}

					@Override
				    public <T> T attachStream(URI remoteURI, String fileParamName, List<MultipartFile> files, TypeReference<T> typeReference) throws Exception{
						return this.attach(RestAttachType.stream, remoteURI, ()->{return this.makeBody(fileParamName, files, RestAttachType.stream);}, typeReference);
					}
				    
					@Override
				    public <T> T attach(RestAttachType restAttachType, URI remoteURI, FetchBodyFunction fetchBody, TypeReference<T> typeReference) throws Exception {				

						HttpHeaders headers = new HttpHeaders();
				    	headers.setContentType(MediaType.MULTIPART_FORM_DATA);
				    	
				    	HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(fetchBody.getBody(), headers);
				    	
				    	RestTemplateUtil restTemplateUtil = this.factory.getObject(RestTemplateHttpClientConfig.builder().restAttachType(restAttachType).build());
				    	
				    	ResponseEntity<Object> response = restTemplateUtil.postForEntity(remoteURI, requestEntity, Object.class);	    	
				    	//HttpHeaders resHeader = response.getHeaders();
				    	//HttpStatus resStatus = response.getStatusCode();    	
				    	Object resBody = response.getBody();
				    	
						ObjectMapper mapper = new ObjectMapper();
						mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);	
						mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);			
				    	T result = mapper.convertValue(resBody, typeReference);
				    	
				    	return result;
					}
					
					private MultiValueMap<String, Object> makeBody(String fileParamName, MultipartFile file, RestAttachType restAttachType) {
						List<MultipartFile> files = new ArrayList<>();
						files.add(file);
						return this.makeBody(fileParamName, files, restAttachType);
					}
					
					private MultiValueMap<String, Object> makeBody(String fileParamName, List<MultipartFile> files, RestAttachType restAttachType) {
				    	MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
				    	for(MultipartFile file : files) {
				    		if(file==null) {continue;}
				    		if(restAttachType.equals(RestAttachType.buffer)) {
					    		try {
									body.add(fileParamName, new ByteArrayResource(file.getBytes()){public String getFilename(){return file.getOriginalFilename();}});
								} catch (IOException e) {	
									throw new SystemException(e.getMessage());
								}
				    		}
				    		if(restAttachType.equals(RestAttachType.stream)) {
				    			body.add(fileParamName, file.getResource());
				    		}
				    	}
				    	return body;
					}		
				}				
				/*******************************************************************************/
				
				String key = RestTemplateUtilContext.buildKey(restAttachType.toString(), maxConnTotal, maxConnRoute, connectTimeout, readTimeout);
				//log.debug(">> RestTemplateUtilContext.key:{}",key);
				if(RestTemplateUtilContext.isExist(key)) {
					return RestTemplateUtilContext.get(key);
				}
				
				boolean isBuffer = true; //default
				if(restAttachType.equals(RestAttachType.buffer)) {isBuffer=true;}
				if(restAttachType.equals(RestAttachType.stream)) {isBuffer=false;}
				
				//HttpClient를 이용해서 connection pool을 사용 (maxConnTotal:최대 커넥션 개수, maxConnRoute: IP:PORT 쌍에 대한 커넥션 수)
				HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
				factory.setHttpClient(HttpClientBuilder.create().setMaxConnTotal(maxConnTotal).setMaxConnPerRoute(maxConnRoute).build());
				factory.setConnectTimeout(connectTimeout);
				factory.setReadTimeout(readTimeout);
				factory.setBufferRequestBody(isBuffer); // true: buffer(in-memory), false: streaming 				
			
				RestTemplate restTemplate = null;
				if(isBuffer) {
					//인터셉터에서 Body Stream을 읽어 소비가 되면 실제 비즈니스 로직에서는 Body가 없어짐
					//이를 해결하기 위해 requestFactory에 BufferingClientHttpRequestFactory를 사용
					//ClientHttpRequestInterceptor는 ClientHttpRequest가 BufferRequestBody = true인 경우 만 적용 됨
					restTemplate = new _RestTemplateUtil(new BufferingClientHttpRequestFactory(factory));			
					restTemplate.setInterceptors(this.getClientHttpRequestInterceptors());
				}else {
					restTemplate = new _RestTemplateUtil(factory);
				}
				restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
				
				RestTemplateUtil restTemplateUtil = (RestTemplateUtil)restTemplate;
				restTemplateUtil.setRestTemplateUtilFactory(this);
				RestTemplateUtilContext.add(key, restTemplateUtil);
				
				return restTemplateUtil;
			}
			
			private List<ClientHttpRequestInterceptor> getClientHttpRequestInterceptors() {
				List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
				interceptors.add(new ClientHttpRequestInterceptor() {

					@Override
					public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

						long start=System.currentTimeMillis();
						String executionNo="executionNo-"+String.valueOf(start);
						MediaType contentType = request.getHeaders().getContentType();
						
						log.debug(">> [{}], request.uri:{}",executionNo,request.getURI());
						log.debug(">> [{}], request.method:{}",executionNo,request.getMethod());
						log.debug(">> [{}], request.headers:{}",executionNo,request.getHeaders());

						if(contentType!=null && contentType.includes(MediaType.MULTIPART_FORM_DATA)) {								
							String bodyStr = new String(body,"UTF-8");
							String lines[] = bodyStr.split("\n");
							String boundary = lines[0].trim();
							
							StringBuilder builder = new StringBuilder();							
							Arrays.asList(lines).stream().forEach(line->{
								if(line.startsWith(boundary)) {builder.append(line).append("\n");}
								else if(line.startsWith("Content-Disposition")) {builder.append(line).append("\n");}
								else if(line.startsWith("Content-Type")) {builder.append(line).append("\n");}
								else if(line.startsWith("Content-Length")) {builder.append(line).append("\n**첨부파일(로그생략)**\n");}
							});
							log.debug(">> [{}], request.body:\n{}",executionNo,builder.toString());
						}else {
							log.debug(">> [{}], request.body:{}",executionNo,new String(body,"UTF-8"));		
						}
						
						ClientHttpResponse response = execution.execute(request, body);
						ContentDisposition contentDisposition = response.getHeaders().getContentDisposition();
						String fileName = contentDisposition==null?null:contentDisposition.getFilename();
						
						log.debug(">> [{}], response.statusCode:{}",executionNo,response.getStatusCode());
						log.debug(">> [{}], response.statusText:{}",executionNo,response.getStatusText());
						log.debug(">> [{}], response.headers:{}",executionNo,response.getHeaders());
						
						if(fileName!=null && !fileName.equals("")) {
							log.debug(">> [{}], response.body:{}",executionNo,"**첨부파일(로그생략)**");
						}else {
							log.debug(">> [{}], response.body:{}",executionNo,StreamUtils.copyToString(response.getBody(),Charset.defaultCharset()));							
						}
						log.debug(">> [{}], execution.time(ms):{}",executionNo,System.currentTimeMillis()-start);

						return response;
					}
					
				});
				return interceptors;
			}
			
		}.getInstance();
	}	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
