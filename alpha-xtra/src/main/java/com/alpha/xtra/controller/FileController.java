package com.alpha.xtra.controller;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.alpha.Application.DefaultBinder;
import com.alpha.base.BaseUtil;
import com.alpha.base.support.util.FileUtilPack.StoreInfo;
import com.alpha.base.support.util.SftpUtilPack.Channel;
import com.fasterxml.jackson.core.type.TypeReference;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.sftp.RemoteResourceInfo;

@Slf4j
@RestController
@RequestMapping(path="/xtra/file/v1")
public class FileController extends DefaultBinder {

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static class DefaultValue {
		//private static boolean isProd = BaseUtil.getSystemUtil().isProd();
		public static String getDefaultRemoteHost() {return BaseUtil.getSystemUtil().getServerIp();}
		public static int getDefaultRemotePort() {return 22;}
	}

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String SPEL_REMOTEHOST = "#{T(com.alpha.xtra.controller.FileController$DefaultValue).getDefaultRemoteHost()}";
	private final String SPEL_REMOTEPORT = "#{T(com.alpha.xtra.controller.FileController$DefaultValue).getDefaultRemotePort()}";
	
	public enum SftpTestType {GET,PUT};
	public enum SftpAuthType {KEY,PASSWORD};

	@Value("${spring.profiles.active:local}")
	private String springProfilesActive;
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
	@PostMapping(value="/attach/fileUpload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "파일 업로드", description = "consumes=MediaType.MULTIPART_FORM_DATA_VALUE")   
	public List<?> attachFileUpload(
			@Parameter(description = "첨부파일") @RequestPart(required = false) List<MultipartFile> mFiles) throws Exception {

		String storeDirectory = BaseUtil.getSystemUtil().isLocal()?"/azure.data/xtra":"/app/data/xtra";
		
		List<StoreInfo> list = BaseUtil.getFileUtil().store(mFiles, storeDirectory);

		return list;
	}
	
    @GetMapping("/attach/fileDownload")
	@Operation(summary = "파일 다운로드", description = "서버파일경로: 파일 업로드시 서버에 생성된 파일 경로를 지정, 파일이름: 응답헤더 Content-Disposition: attachment;filename=[파일이름]")   
    public void attachFileDownload(
    		@Parameter(description = "서버파일경로") @RequestParam(required = true) final String serverFilePath
    		,@Parameter(description = "파일이름") @RequestParam(required = false, defaultValue = "파일") final String fileName
    		,HttpServletRequest request,HttpServletResponse response) throws Exception {
    	
    	BaseUtil.getFileUtil().pushout(serverFilePath, fileName, request, response);
    }
	
    @Hidden
    @GetMapping("/attach/fileDownload/recent")
	@Operation(summary = "최신파일 다운로드", description = "서버저장경로: 파일이 존재하는 경로: 응답헤더 Content-Disposition: attachment;filename=[파일이름]")   
    public void attachFileDownloadRecent(
    		@Parameter(description = "서버저장경로") @RequestParam(required = true) final String serverStoreDirPath
    		,@Parameter(description = "파일이름") @RequestParam(required = false, defaultValue = "") final String fileName
    		,HttpServletRequest request,HttpServletResponse response) throws Exception {
    	
    	File directory = new File(serverStoreDirPath);

    	List<File> files = Arrays.stream(directory.listFiles(File::isFile))
    	.filter(f->f.getName().indexOf(fileName)>-1)
    	.sorted(Comparator.comparingLong(File::lastModified).reversed())
    	.collect(Collectors.toList());
    	
    	if(files!=null && !files.isEmpty()) {
        	BaseUtil.getFileUtil().pushout(files.get(0), fileName, request, response);
    	}
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/compress/zip")
	@Operation(summary = "압축", description = "대상경로: 압축을 수행할 경로(파일,폴더), 상대경로여부: 대상경로를 기준으로 압축")  
    public void zip(
    		@Parameter(description = "대상경로") @RequestParam(required = true, defaultValue = "/azure.data/data") final String targetPath
    		,@Parameter(description = "압축파일경로") @RequestParam(required = false, defaultValue = "/azure.data/data.zip") final String zipFilePath
    		,@Parameter(description = "상대경로여부") @RequestParam(required = true, defaultValue = "true") final Boolean isRelativePath
    		,HttpServletRequest request,HttpServletResponse response) throws Exception {

    		File zipFile = BaseUtil.getCompressUtil().zip(targetPath, zipFilePath, isRelativePath);
    		
    		BaseUtil.getFileUtil().pushout(zipFile, request, response);
    }

    @GetMapping("/compress/unzip")
	@Operation(summary = "압축풀기", description = "대상경로: 압축을 수행할 경로(파일,폴더), 상대경로여부: 대상경로를 기준으로 압축")  
    public List<?> unzip(
    		@Parameter(description = "압축파일경로") @RequestParam(required = true, defaultValue = "/azure.data/data.zip") final String zipFilePath
    		,@Parameter(description = "압축파일경로") @RequestParam(required = false, defaultValue = "/azure.data/data") final String unzipPath
    		,HttpServletRequest request,HttpServletResponse response) throws Exception {

    		List<String> list = BaseUtil.getCompressUtil().unZip(zipFilePath, unzipPath);

    		return list;
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    @Hidden    
	@PostMapping(value="/sftp/fileUpload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "SFTP 파일 업로드", description = "첨부파일을 서버에 저장 후 원격지서버로 SFTP 파일전송(put), 원격지서버 목록(ls) 정보 출력 ")   
	public Object sftpFileUpload(
			@Parameter(description = "첨부파일") @RequestPart(required = false) List<MultipartFile> mFiles
    		,@Parameter(description = "원격서버 ip") @RequestParam(required = true, defaultValue = SPEL_REMOTEHOST) final String remoteHost
    		,@Parameter(description = "원격서버 port") @RequestParam(required = true, defaultValue = SPEL_REMOTEPORT) final int remotePort
    		,@Parameter(description = "원격서버 디렉토리") @RequestParam(required = true, defaultValue = "/app/data") final String remoteDirPath
    		,@Parameter(description = "원격서버 인증구분") @RequestParam(required = true, defaultValue = "KEY") SftpAuthType sftpAuthType
    		,@Parameter(description = "원격서버 접속id") @RequestParam(required = true, defaultValue = "blue") final String username
    		,@Parameter(description = "원격서버 접속pw") @RequestParam(required = false, defaultValue = "password") final String password
    		,@Parameter(description = "원격서버 접속key(rsa)") @RequestParam(required = true, defaultValue = "/azure.data/key/id_rsa") final String keyPath) throws Exception {

		List<StoreInfo> list = BaseUtil.getFileUtil().store(mFiles, "/azure.data/xtra");
		if(list==null || list.isEmpty()) {return null;}

    	Channel channel = null;
    	if(sftpAuthType.equals(SftpAuthType.KEY)) {
        	assertTrue(StringUtils.isNotBlank(username));
        	assertTrue(StringUtils.isNotBlank(keyPath));      		
    		channel = BaseUtil.getSftpUtil().build(remoteHost,remotePort,username,new File(FilenameUtils.normalize(keyPath)));
    	}
    	if(sftpAuthType.equals(SftpAuthType.PASSWORD)) {
        	assertTrue(StringUtils.isNotBlank(username));
        	assertTrue(StringUtils.isNotBlank(password));      		
    		channel = BaseUtil.getSftpUtil().build(remoteHost,remotePort,username,password);
    	}

    	Object result = channel.stage((sftp,util) -> {
    		String separator="/";
			if(remoteDirPath.substring(remoteDirPath.length()-1).equals("/")) {
				separator="";
			}
    		for(StoreInfo storeInfo : list){
    			sftp.put(storeInfo.getStorePath(), remoteDirPath + separator + storeInfo.getAttachName());
    		}
    		return sftp.ls(remoteDirPath);
    	});

    	return result;
	}
    
    @Hidden	
    @GetMapping("/sftp/fileDownload")
	@Operation(summary = "SFTP 파일 다운로드", description = "서버에서 원격지서버로 SFTP 다운로드(get) 후 다운로드 응답처리")   
    public void sftpFileDownload(
    		@Parameter(description = "원격서버 ip") @RequestParam(required = true, defaultValue = SPEL_REMOTEHOST) final String remoteHost
    		,@Parameter(description = "원격서버 port") @RequestParam(required = true, defaultValue = SPEL_REMOTEPORT) final int remotePort    		
    		,@Parameter(description = "원격서버 경로") @RequestParam(required = true, defaultValue = "/app/data") final String remotePath
    		,@Parameter(description = "원격서버 인증구분") @RequestParam(required = true, defaultValue = "KEY") SftpAuthType sftpAuthType
    		,@Parameter(description = "원격서버 접속id") @RequestParam(required = true, defaultValue = "blue") final String username
    		,@Parameter(description = "원격서버 접속pw") @RequestParam(required = false, defaultValue = "password") final String password  		
    		,@Parameter(description = "원격서버 접속key(rsa)경로") @RequestParam(required = true, defaultValue = "/azure.data/key/id_rsa") final String keyPath
    		,@Parameter(description = "파일저장 경로") @RequestParam(required = true, defaultValue = "/azure.data/download") final String storePath    		
    		,HttpServletRequest request,HttpServletResponse response) throws Exception {
    	
    	Channel channel = null;
    	if(sftpAuthType.equals(SftpAuthType.KEY)) {
        	assertTrue(StringUtils.isNotBlank(username));
        	assertTrue(StringUtils.isNotBlank(keyPath));    		
    		channel = BaseUtil.getSftpUtil().build(remoteHost,remotePort,username,new File(FilenameUtils.normalize(keyPath)));
    	}
    	if(sftpAuthType.equals(SftpAuthType.PASSWORD)) {
        	assertTrue(StringUtils.isNotBlank(username));
        	assertTrue(StringUtils.isNotBlank(password));    		
    		channel = BaseUtil.getSftpUtil().build(remoteHost,username,password);
    	}
    	
    	String filePath = channel.stage((sftp,util)->{
    		if(!util.isFile(remotePath)) {return null;}
			String remoteFileName = util.extractFileName(remotePath);
			String storeFilePath = util.getStorePath(storePath, remoteFileName);			
			sftp.get(remotePath, storeFilePath);			
    		return storeFilePath;    		
    	},String.class);
    	
    	if(StringUtils.isBlank(filePath)) {return;}
    	
    	BaseUtil.getFileUtil().pushout(filePath, request, response);
    }
    
    @Hidden
    @GetMapping("/sftp/getput")
	@Operation(summary = "SFTP GET/PUT", description = "원격지서버로 SFTP GET/PUT")   
    public void sftpGet(
    		@Parameter(description="mode") @RequestParam(required = true, defaultValue = "GET") SftpTestType sftpTestType
    		,@Parameter(description = "원격서버 ip") @RequestParam(required = true, defaultValue = SPEL_REMOTEHOST) final String remoteHost
    		,@Parameter(description = "원격서버 port") @RequestParam(required = true, defaultValue = SPEL_REMOTEPORT) final int remotePort
    		,@Parameter(description = "원격서버 경로") @RequestParam(required = true, defaultValue = "/app/data") final String remotePath
    		,@Parameter(description = "원격서버 인증구분") @RequestParam(required = true, defaultValue = "KEY") SftpAuthType sftpAuthType
    		,@Parameter(description = "원격서버 접속id") @RequestParam(required = true, defaultValue = "blue") final String username
    		,@Parameter(description = "원격서버 접속pw") @RequestParam(required = false, defaultValue = "password") final String password    		
    		,@Parameter(description = "원격서버 접속key(rsa)경로") @RequestParam(required = true, defaultValue = "/azure.data/key/id_rsa") final String keyPath
    		,@Parameter(description = "파일저장 경로") @RequestParam(required = true, defaultValue = "/azure.data/download") final String storePath    		
    		,HttpServletRequest request,HttpServletResponse response) throws Exception {
    	    	
    	Channel channel = null;
    	if(sftpAuthType.equals(SftpAuthType.KEY)) {
        	assertTrue(StringUtils.isNotBlank(username));
        	assertTrue(StringUtils.isNotBlank(keyPath));
    		channel = BaseUtil.getSftpUtil().build(remoteHost,remotePort,username,new File(FilenameUtils.normalize(keyPath)));
    	}
    	if(sftpAuthType.equals(SftpAuthType.PASSWORD)) {
        	assertTrue(StringUtils.isNotBlank(username));
        	assertTrue(StringUtils.isNotBlank(password));
    		channel = BaseUtil.getSftpUtil().build(remoteHost,remotePort,username,password);
    	}
    	
    	if(sftpTestType.equals(SftpTestType.GET)) {
	    	File storeDir = channel.stage((sftp,util)->{    
	    		File _storeDir = util.mkdirs(storePath);	    		
	    		sftp.get(remotePath, storePath);	    		
	    		return _storeDir;
	    	},File.class);
	    	
	    	this.printDir(storeDir);
    	}
    	if(sftpTestType.equals(SftpTestType.PUT)) {
    		List<RemoteResourceInfo> list = channel.stage((sftp,util)->{    

	    		sftp.put(storePath, remotePath);	    		
	    		List<RemoteResourceInfo> remoteList = sftp.ls(remotePath);	
	    		return remoteList;	    		
	    	},new TypeReference<List<RemoteResourceInfo>>() {});	
    		
    		list.forEach(i->log.debug(">> {}",i.toString()));
    	}
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void printDir(File dir) {
    	for(File file : dir.listFiles()) {
    		if(file.isDirectory()) {printDir(file);}
    		log.debug(">> {}",file.getPath());    		
    	}
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
