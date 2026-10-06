package com.alpha.ytra.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.alpha.base.BaseUtil;
import com.alpha.base.support.util.RestTemplateUtilPack.RestTemplateHttpClientConfig;
import com.alpha.base.support.util.RestTemplateUtilPack.RestTemplateUtil.RestAttachType;
import com.fasterxml.jackson.core.type.TypeReference;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path="/ytra/file/v1")
public class FileController {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private enum RemoteFileDownloadMode {file2OutputStream,inputstream2OutputStream};
	
	@Value("${spring.servlet.multipart.location}")
	private String tempLocation;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@PostMapping(value="/remoteFileUpload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "원격 파일 업로드", description = "업로드한 파일을 apiUri로 지정한 곳으로 파일업로드")   
	public List<?> fileUpload(
			@Parameter(description = "apiUri") @RequestParam(required = true, defaultValue = "http://localhost:9970/xtra/etc/v1/fileUpload") final String apiUri
			,@Parameter(description="전송방식") @RequestParam(required = true, defaultValue = "buffer") RestAttachType restAttachType
			,@Parameter(description = "첨부파일") @RequestPart(required = false) List<MultipartFile> fileList) throws Exception {
		
	
		if(fileList==null || fileList.isEmpty()) {fileList = new ArrayList<>();}
		fileList.add(BaseUtil.getFileUtil().getMultipartFile("C:\\azure.data\\data1.jar"));
		fileList.add(BaseUtil.getFileUtil().getMultipartFile("C:\\azure.data\\data2.jar"));

		List<?> list = null;
		
		if(restAttachType.equals(RestAttachType.buffer)) {
			list = BaseUtil.getRestTemplateUtil().attachBuffer(new URI(apiUri), "files", fileList, new TypeReference<List<?>>(){});
		}
		if(restAttachType.equals(RestAttachType.stream)) {
			list = BaseUtil.getRestTemplateUtil().attachStream(new URI(apiUri), "files", fileList, new TypeReference<List<?>>(){});
		}

		return list;
	}
    
    @GetMapping("/remoteFileDownload")
	@Operation(summary = "원격 파일 다운로드", description = "apiUri에서 count에 지정한 숫자만큼 데이타 증폭(반복)된 파일을 다운로드")   
    public void string2FileDownload(
    		@Parameter(description = "apiUri") @RequestParam(required = true, defaultValue = "http://localhost:9970/xtra/boards/v1/json2FileDownload") final String apiUri
    		,@Parameter(description = "readTimeout(ms)") @RequestParam(required = true, defaultValue = "10 * 60 * 1000" ) final String readTimeoutSpel
    		,@Parameter(description = "파일이름") @RequestParam(required = false, defaultValue = "파일") final String fileName
    		,@Parameter(description = "데이타반복") @RequestParam(required = true, defaultValue = "1") final int count
    		,@Parameter(description = "처리방식") @RequestParam(required = true, defaultValue = "file2OutputStream") RemoteFileDownloadMode mode
    		,HttpServletRequest request,HttpServletResponse response) throws Exception {

		URI remoteUri = UriComponentsBuilder.fromUriString(apiUri)
				.queryParam("fileName","json2FileDownload")
				.queryParam("count", count)
				.encode().build().toUri();
		
		Integer readTimeout = this.parseReadTimeout(readTimeoutSpel);

		log.debug(">> start:systemUsage:{}",BaseUtil.getSystemUtil().getSystemUsage().toString());

		if(mode.equals(RemoteFileDownloadMode.file2OutputStream)) {
			File file = BaseUtil.getRestTemplateUtil().build(RestTemplateHttpClientConfig.builder().readTimeout(readTimeout).build())
					.execute(remoteUri, HttpMethod.GET
						,requestCallback -> {
							requestCallback.getHeaders().set("request-system", BaseUtil.getSystemUtil().getSystemInfo().toString());
						}
						,responseExtractor -> {
						    File f = File.createTempFile("temp", ".download", new File(FilenameUtils.normalize(this.tempLocation)));
						    f.deleteOnExit(); //JVM 이 종료 될 때 자동으로 지정된 파일을 삭제
						    StreamUtils.copy(responseExtractor.getBody(), new FileOutputStream(f));
						    return f;
						}
					);
			log.debug(">> step1:systemUsage:{}",BaseUtil.getSystemUtil().getSystemUsage().toString());
	    	BaseUtil.getFileUtil().pushout(file, fileName, request, response);
		}

		if(mode.equals(RemoteFileDownloadMode.inputstream2OutputStream)) {
			InputStream inputStream = BaseUtil.getRestTemplateUtil().build(RestTemplateHttpClientConfig.builder().readTimeout(readTimeout).build())
					.execute(remoteUri, HttpMethod.GET
	    				,requestCallback->{
	    					requestCallback.getHeaders().set("request-system", BaseUtil.getSystemUtil().getSystemInfo().toString());
	    				}
	    				,responseExtractor->{
	    					return responseExtractor.getBody();
	    				}
		    		);
			log.debug(">> step1:systemUsage:{}",BaseUtil.getSystemUtil().getSystemUsage().toString());
			BaseUtil.getFileUtil().pushout(()->{return inputStream;}, fileName, request, response);
						
			/*
			 * 축약코드			 
			BaseUtil.getFileUtil().pushout(()->{
				return BaseUtil.getRestTemplateUtil()
						.build(RestTemplateUtilConfig.builder().readTimeout(readTimeout).build())
						.execute(remoteUri, HttpMethod.GET
								,requestCallback->{requestCallback.getHeaders().set("request-system", BaseUtil.getSystemUtil().getSystemInfo().toString());}
			    				,responseExtractor->{return responseExtractor.getBody();}
			    				);
				}, fileName, request, response);
			*/
		}

		log.debug(">> end:systemUsage:{}",BaseUtil.getSystemUtil().getSystemUsage().toString());
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Integer parseReadTimeout(String spel) {
		ExpressionParser parser = new SpelExpressionParser();
		Integer readTimeout = parser.parseExpression(spel).getValue(Integer.class);
		return readTimeout;
    }
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
