package com.alpha.base.support.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Blob;
import java.sql.Clob;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import com.alpha.base.exception.SystemException;
import com.alpha.base.support.aid.BeanAidPack.BeanAid;
import com.alpha.base.support.etc.Sweeper;
import com.alpha.base.support.util.SystemUtilPack.SystemUtil;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FileUtilPack{
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface FileUtil {

		@FunctionalInterface
		public interface FetchDataFunction {public abstract byte[] getData(long fetchNo) throws Exception;}
		
		@FunctionalInterface
		public interface FetchFileFunction {public abstract File getFile() throws Exception;}

		@FunctionalInterface
		public interface FetchInputStreamFunction {public abstract InputStream getInputStream() throws Exception;}

	    public static final int FILE_READ_BUFFER_SIZE = 1024*512;  //512KB
	    public static final int FILE_FLUSH_UNIT_SIZE = 1024*1024*1;  //1MB

	    public MultipartConfigElement getMultipartConfigElement();

	    public String getUniqueName(String directory, String prefix, String fileName);
	    public String normalizePath(String path); 
	    public String extractFileName(String path);
		
	    public void delete(List<String> list) throws Exception;
		public boolean delete(String storePath) throws Exception;
		
	    public MultipartFile getMultipartFile(String filePath) throws Exception;
	    public MultipartFile getMultipartFile(File file) throws Exception;	    
	    public List<MultipartFile> getMultipartFile(String... filePaths) throws Exception;
	    public List<MultipartFile> getMultipartFile(List<String> filePaths) throws Exception;	    
	    
		public StoreInfo store(MultipartFile file, String storeDirectory) throws Exception;
	    public StoreInfo store(MultipartFile file, String storeDirectory, String[] acceptedFileTypes) throws Exception;	    
	    public StoreInfo store(MultipartFile file, String storeDirectory, String[] acceptedFileTypes, long maxFileSize) throws Exception;
	    public List<StoreInfo> store(List<MultipartFile> files, String storeDirectory) throws Exception;	    
	    public List<StoreInfo> store(List<MultipartFile> files, String storeDirectory, String[] acceptedFileTypes) throws Exception;
	    public List<StoreInfo> store(List<MultipartFile> files, String storeDirectory, String[] acceptedFileTypes, long maxFileSize) throws Exception;
	    public <T extends StoreInfo> T store(MultipartFile file, String storeDirectory, String[] acceptedFileTypes, long maxFileSize, Class<T> type) throws Exception;    	
	    public <T extends StoreInfo> List<T> store(List<MultipartFile> files, String storeDirectory, String[] acceptedFileTypes, long maxFileSize, Class<T> type) throws Exception;	    
	    public <T extends StoreInfo> List<T> store(MultipartHttpServletRequest request, String storeDirectory, String[] acceptedFileTypes, long maxFileSize, Class<T> type) throws Exception;
	    
		public void pushout(String storePath, HttpServletRequest request, HttpServletResponse response) throws Exception;		
	    public void pushout(String storePath, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception;		
		public void pushout(File file, HttpServletRequest request, HttpServletResponse response) throws Exception;
		public void pushout(File file, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception;	    
		public void pushout(byte[] bytes, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception;
	    public void pushout(Clob clob, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception;
	    public void pushout(Blob blob, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception;
	    public void pushout(FetchDataFunction fetch, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception;
	    public void pushout(FetchFileFunction fetch, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception;
		public void pushout(FetchInputStreamFunction fetch, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception;
	}

    @Data
    public static class StoreInfo implements Serializable {
		private static final long serialVersionUID = 1L;
		private String attachName;
    	private long attachSize;    	
    	private String storeDirectory;
    	private String storeName;
    	private String storePath;
    	private String storeDate;
    	private Map<String,String> systemInfo;
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static FileUtil getFileUtil(BeanAid beanAid, SystemUtil systemUtil) {

		return new FileUtil() {
			
			private MultipartConfigElement multipartConfigElement = beanAid.getBean(MultipartConfigElement.class);
			
			@Override
		    public MultipartConfigElement getMultipartConfigElement() {
				return this.multipartConfigElement;
		    }

			@Override  
		    public String getUniqueName(String directory, String prefix, String fileName) {
				if(prefix==null) {prefix="";}
				
				String storeName = prefix.trim().equals("")?fileName:prefix+"_"+fileName;
				String storeDirectory = this.normalizePath(directory);

				//To prevent path manipulation, FilenameUtils.normalizes a path to a standard format
				if((new File(storeDirectory, FilenameUtils.normalize(storeName))).exists()) {				
					int index = 1;
					while(true) {
						int extComma = storeName.lastIndexOf(".");    
						String modifiedFileName = storeName.substring(0, extComma) + "(" + index + ")" + storeName.substring(extComma);
						//To prevent path manipulation, FilenameUtils.normalizes a path to a standard format
						if(!(new File(storeDirectory, FilenameUtils.normalize(modifiedFileName))).exists()) {
							storeName = modifiedFileName;
							break;
						} else {
							index = index+1;
							storeName = fileName;
						}
					}
				}
				return storeName;
			}
		
			@Override  
		    public String normalizePath(String path) {
				String fileSeparator = "";
				if(!path.substring(path.length()-1, path.length()).equals(File.separator)) {fileSeparator = File.separator;}
				return path + fileSeparator;
			}
		    
			@Override  
		    public String extractFileName(String path) {
				String name = path.substring(path.lastIndexOf("\\")+1, path.length());
				if(path.indexOf("/")>-1) {name = path.substring(path.lastIndexOf("/")+1, path.length());}
				return name;
			}
			
			@Override
		    public void delete(List<String> list) throws Exception {
		    	if(list!=null) {
		    		for(String storePath: list) {this.delete(storePath);}
		    	}
		    }

			@Override
		    public boolean delete(String storePath) throws Exception {
				boolean isDeleted = false;
				if(StringUtils.isBlank(storePath)) {return isDeleted;}
				File file = new File(FilenameUtils.normalize(storePath));
				if(!file.exists()) { file.delete(); isDeleted = true;}
				log.debug(">> isDeleted:{}, storePath:{}", isDeleted, storePath);
				return isDeleted;
			}
			
			
			@Override
		    public List<MultipartFile> getMultipartFile(String... filePaths) throws Exception {
		    	return this.getMultipartFile(Arrays.asList(filePaths));
		    }

		    @Override
		    public List<MultipartFile> getMultipartFile(List<String> filePaths) throws Exception {
		    	if(filePaths==null || filePaths.isEmpty()) {return null;}
		    	List<MultipartFile> list = new ArrayList<>();
		    	for(String filePath : filePaths) {list.add(this.getMultipartFile(filePath));}
		    	return list;
		    }
		    
		    @Override
		    public MultipartFile getMultipartFile(String filePath) throws Exception {
		    	if(StringUtils.isBlank(filePath)) {return null;}
		    	return this.getMultipartFile(new File(FilenameUtils.normalize(filePath)));
		    }
		    
		    @Override
		    public MultipartFile getMultipartFile(File file) throws Exception {
		    	if(!file.exists()) {return null;}
		    	String fieldName = "file"+String.valueOf(System.currentTimeMillis());    	
		    	FileItem fileItem = new DiskFileItem(fieldName, Files.probeContentType(file.toPath()), false, file.getName(), (int) file.length(), file.getParentFile());
		    	IOUtils.copy(new FileInputStream(file), fileItem.getOutputStream());
		    	return new CommonsMultipartFile(fileItem);
		    }

			@Override
		    public StoreInfo store(MultipartFile file, String storeDirectory) throws Exception {
		    	return this.store(file, storeDirectory, null);
		    }
		    
		    @Override
		    public StoreInfo store(MultipartFile file, String storeDirectory, String[] acceptedFileTypes) throws Exception {
		    	return this.store(file, storeDirectory, null, this.getMultipartConfigElement().getMaxFileSize());
		    }
		    
		    @Override
		    public StoreInfo store(MultipartFile file, String storeDirectory, String[] acceptedFileTypes, long maxFileSize) throws Exception {
		    	return this.store(file, storeDirectory, acceptedFileTypes, maxFileSize, StoreInfo.class);
		    }	    		    
		    
		    @Override
		    public List<StoreInfo> store(List<MultipartFile> files, String storeDirectory) throws Exception {
		    	return this.store(files, storeDirectory, null);
		    }
		    
		    @Override
		    public List<StoreInfo> store(List<MultipartFile> files, String storeDirectory, String[] acceptedFileTypes) throws Exception {
		    	return this.store(files, storeDirectory, acceptedFileTypes, this.getMultipartConfigElement().getMaxFileSize());
		    }
		    
		    @Override
		    public List<StoreInfo> store(List<MultipartFile> files, String storeDirectory, String[] acceptedFileTypes, long maxFileSize) throws Exception {
		    	return this.store(files, storeDirectory, acceptedFileTypes, maxFileSize, StoreInfo.class);
		    }
		    
			@Override
		    public <T extends StoreInfo> T store(MultipartFile file, String storeDirectory, String[] acceptedFileTypes, long maxFileSize, Class<T> type) throws Exception{
				this.checkFile(file, acceptedFileTypes, maxFileSize);
				return this.storeFile(file, storeDirectory, type);
		    }	
		    
			@Override
		    public <T extends StoreInfo> List<T> store(List<MultipartFile> files, String storeDirectory, String[] acceptedFileTypes, long maxFileSize, Class<T> type) throws Exception {
				if(CollectionUtils.isEmpty(files)) {log.debug(">> List<MultipartFile> files is empty");return null;}
				List<T> list = new ArrayList<>();
				for(MultipartFile file : files) {this.checkFile(file, acceptedFileTypes, maxFileSize);}
				for(MultipartFile file : files) {list.add(this.storeFile(file, storeDirectory, type));}
				return list;	
		    }
		    
			@Override
			public <T extends StoreInfo> List<T> store(MultipartHttpServletRequest request, String storeDirectory, String[] acceptedFileTypes, long maxFileSize, Class<T> type) throws Exception {
				if(!StoreInfo.class.isAssignableFrom(type)) {throw new SystemException(type.getCanonicalName()+") is not implemented. Class must implement com.alpha.base.support.util.vo.StoreInfo");}

				List<T> list = new ArrayList<T>();			
				Iterator<String> iterator = request.getFileNames();
				while(iterator.hasNext()) {
					String uploadFormName = (String)iterator.next();	
					T vo = this.store(request.getFile(uploadFormName), storeDirectory, acceptedFileTypes, maxFileSize, type);
					list.add(vo);
				}

				return list;
			}

			@Override
		    public void pushout(String storePath, HttpServletRequest request, HttpServletResponse response) throws Exception {
				String fileName = this.extractFileName(storePath);
				this.pushout(storePath, fileName, request, response);
			}
			
		    @Override
		    public void pushout(String storePath, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception {
				File file = StringUtils.isBlank(storePath)?null:new File(FilenameUtils.normalize(storePath));
				this.pushout(file, fileName, request, response);
			}
			
			@Override
		    public void pushout(File file, HttpServletRequest request, HttpServletResponse response) throws Exception {
				String fileName = (file != null && file.exists())?file.getName():null;
				this.pushout(file, fileName, request, response);
			}

			@Override
		    public void pushout(File file, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception {
				this.pushout(()->{return file;}, fileName, request, response);
			}	  
			
		    @Override
		    public void pushout(byte[] bytes, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception {
		        if(bytes == null) {throw new SystemException("bytes is null.");}
				if(StringUtils.isBlank(fileName)) {throw new SystemException("fileName is empty.");}

				this.buildDownloadHeader(fileName, request, response);				
		        if(bytes != null) {response.setContentLength(bytes.length);}
		        
				try(OutputStream out = response.getOutputStream()){
					try(InputStream inputStream = new ByteArrayInputStream(bytes)){
						FileCopyUtils.copy(inputStream, out);
						inputStream.close(); 
				        out.flush();
				        out.close();
					}
				}
			}
		    
		    @Override
		    public void pushout(Clob clob, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception {
		        if(clob == null) {throw new SystemException("clob is null.");}
		        if(StringUtils.isBlank(fileName)) {throw new SystemException("fileName is empty.");}

		        this.buildDownloadHeader(fileName, request, response);	

		        int contentLength = -1;
		        if(contentLength>0) {response.setContentLength(contentLength);}
		        
		        try(Reader readerStream = clob.getCharacterStream()){			        
			        char[] readBuf = new char[FILE_READ_BUFFER_SIZE];
			        int readCount = 0, writeCount = 0;
			        while((readCount = readerStream.read(readBuf))>-1) {
			            response.getOutputStream().write(new String(readBuf, 0, readCount).getBytes("UTF-8"));
			            writeCount = writeCount+readCount;
			            if(writeCount>FILE_FLUSH_UNIT_SIZE) {
			                //log.debug(">>flush: {}", writeCount);                
			                response.getOutputStream().flush();
			                writeCount = 0;
			            }
			        }
			        response.getOutputStream().flush();
			        readerStream.close();
			        response.getOutputStream().close();
		        }
		    }

		    @Override
		    public void pushout(Blob blob, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception {
		        if(blob == null) {throw new SystemException("blob is null.");}
		        if(StringUtils.isBlank(fileName)) {throw new SystemException("fileName is empty.");}

		        this.buildDownloadHeader(fileName, request, response);
		        
		        int contentLength = (int)blob.length();
		        if(contentLength>0) {response.setContentLength(contentLength);}

		        try(InputStream inputStream = blob.getBinaryStream()){
			        
			        byte[] readBuf = new byte[FILE_READ_BUFFER_SIZE];
			        int readCount = 0, writeCount = 0;
			        while((readCount = inputStream.read(readBuf))>-1) {
			            response.getOutputStream().write(readBuf, 0, readCount);
			            writeCount = writeCount+readCount;            
			            if(writeCount>FILE_FLUSH_UNIT_SIZE) {               
			                response.getOutputStream().flush();
			                writeCount = 0;
			            }
			        }
			        response.getOutputStream().flush();
			        inputStream.close();
			        response.getOutputStream().close();
		        }
		    }

		    @Override    
		    public void pushout(FetchFileFunction fetch, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception {
		    	if(fetch == null) {throw new SystemException("fetch is null.");}
		    	if(StringUtils.isBlank(fileName)) {throw new SystemException("fileName is empty.");}
		    	
		    	File file = fetch.getFile();
				if(file==null || !file.exists()) {throw new SystemException("file not found.");}

				this.buildDownloadHeader(fileName, request, response);
				
				int contentLength = (int)file.length();
		        if(contentLength>0) {response.setContentLength(contentLength);}
		        
				try(OutputStream out = response.getOutputStream()){
					try(FileInputStream fis = new FileInputStream(file)){
						FileCopyUtils.copy(fis, out);
				        fis.close(); 
				        out.flush();
				        out.close();
					}
				}
		    }
		    			
		    @Override    
		    public void pushout(FetchDataFunction fetch, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception {
		        if(fetch == null) {throw new SystemException("fetch is null.");}
		        if(StringUtils.isBlank(fileName)) {throw new SystemException("fileName is empty.");}

		        this.buildDownloadHeader(fileName, request, response);

		        try(ServletOutputStream outputStream = response.getOutputStream()){
		        
		        	int contentLength = -1;
		        	if(contentLength>0) {response.setContentLength(contentLength);}
		        
			        int fetchNo = 0, writeCount = 0;
			        while(true) {
			        	byte[] bytes = fetch.getData(fetchNo++);
						if(bytes == null || bytes.length == 0) {break;}	
						outputStream.write(bytes);            
			            writeCount = writeCount+bytes.length;	            
			            if(writeCount>FILE_FLUSH_UNIT_SIZE) {
			                //log.debug(">>flush: {}", writeCount);                
			                response.getOutputStream().flush();
			                writeCount = 0;
			            }
			        }
			        outputStream.flush();
			        outputStream.close();
		        }
		    }
		    
		    @Override  
		    public void pushout(FetchInputStreamFunction fetch, String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception{
		    	if(fetch == null) {throw new SystemException("fetch is null.");}
		    	if(StringUtils.isBlank(fileName)) {throw new SystemException("fileName is empty.");}

				this.buildDownloadHeader(fileName, request, response);
		        
				int contentLength = -1;
		        if(contentLength>0) {response.setContentLength(contentLength);}
		        
		        try(InputStream inputStream = fetch.getInputStream()){
			        byte[] readBuf = new byte[FILE_READ_BUFFER_SIZE];
			        int readCount = 0, writeCount = 0;
			        while((readCount = inputStream.read(readBuf))>-1) {
			            response.getOutputStream().write(readBuf, 0, readCount);
			            writeCount = writeCount+readCount;            
			            if(writeCount>FILE_FLUSH_UNIT_SIZE) {          
			                response.getOutputStream().flush();
			                writeCount = 0;
			            }
			        }	        
			        response.getOutputStream().flush();
			        inputStream.close();
			        response.getOutputStream().close();  
		        }
		    }
		    
		    private void buildDownloadHeader(String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception {
		        if(fileName==null || fileName.equals("")) {return;}
		        
		    	String normalizeFn = Sweeper.normalizeFilename(fileName);		        
		        String userAgent = request.getHeader("User-Agent");
		        log.debug(">> User-Agent: {}", userAgent);
		        
		        String disposition = null;
		        if(userAgent.indexOf("MSIE")>-1 || userAgent.indexOf("Trident")>-1) {
		        	disposition = "attachment;filename=" + URLEncoder.encode(normalizeFn, "UTF-8").replaceAll("\\+", Character.toString((char)32))+";";
		            log.debug(">> Content-Disposition(MSIE): {}", disposition); 
		        } else if(userAgent.indexOf("Chrome")>-1) {
		        	StringBuilder sb = new StringBuilder(); 
		        	for(int i = 0; i < normalizeFn.length(); i++) { 
		        		char c = normalizeFn.charAt(i); 
		        		sb.append(c>'~'?URLEncoder.encode(""+c, "UTF-8"):c);
		        	}
		        	disposition = "attachment;filename=\"" + sb.toString() + "\";";
		            log.debug(">> Content-Disposition(Chrome): {}", disposition);
		        } else {
		        	disposition = "attachment;filename=\"" + new String(normalizeFn.getBytes("UTF-8"), "8859_1") + "\";";
		            log.debug(">> Content-Disposition: {}", disposition);
		        }
		        
		       response.setHeader("Content-Type", "application/octet-stream");
		       response.setHeader("Content-Disposition", disposition);
		       response.setHeader("Content-Transfer-Encoding", "binary;");
		       response.setHeader("Pragma", "no-cache;");
		       response.setHeader("Expires", "-1;");
		    } 
		    
			private void checkFile(MultipartFile file, String[] acceptedFileTypes, long maxFileSize) {
				if(file == null || file.isEmpty()) {throw new SystemException("file is empty.");}

				String originalFilename = file.getOriginalFilename();
				if(StringUtils.isBlank(originalFilename)) {return;}

				//Check maxFileSize
				if(file.getSize() > maxFileSize) {
					throw new SystemException("Attach file " + originalFilename + " size("+file.getSize()+") is over max size("+maxFileSize+")");
				}
			
				//Check acceptedFileTypes
				if(acceptedFileTypes != null) {
					String fileExt = "";	
					if(originalFilename.indexOf(".")>0) {					
						fileExt = originalFilename.substring(originalFilename.lastIndexOf(".")+1, originalFilename.length());
						boolean isEnableFileType = false;
						for(int i = 0;i<acceptedFileTypes.length;i++) {
							if(acceptedFileTypes[i].toLowerCase().equals(fileExt.toLowerCase())) {isEnableFileType=true; break;}
						}
						if(!isEnableFileType) {
							throw new SystemException("Attach file " + originalFilename + " type("+fileExt+") is not allowed.");
						}
					}

					if(fileExt.equals("")) {
						throw new SystemException("Attach file " + originalFilename + " type is not exist.");					
					}
				}
			}
						
			private <T extends StoreInfo> T storeFile(MultipartFile mFile, String storeDirectory, Class<T> type) throws Exception {
				if(mFile == null || mFile.isEmpty()) {throw new SystemException("file is empty.");}
				if(type == null) {throw new SystemException("clazz is empty.");}
				if(StringUtils.isBlank(storeDirectory)) {throw new SystemException("storeDirectory is empty.");}
				
				String originalFilename = mFile.getOriginalFilename();			
				if(originalFilename==null || originalFilename.equals("")) {return null;}

				String transDate = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(System.currentTimeMillis());				
				String storeName = this.getUniqueName(storeDirectory, transDate, originalFilename);				
				String storePath = this.normalizePath(storeDirectory) + storeName;					

				//To prevent path manipulation, FilenameUtils.normalizes a path to a standard format
				storeDirectory = FilenameUtils.normalize(storeDirectory);
				File dir = new File(storeDirectory);
				if(!dir.isDirectory()) {dir.mkdirs();}
			
				//To prevent path manipulation, FilenameUtils.normalizes a path to a standard format
				storePath = FilenameUtils.normalize(storePath);
				mFile.transferTo(Paths.get(storePath));

				T storeTag = type.getDeclaredConstructor().newInstance();
				storeTag.setAttachName(originalFilename);
				storeTag.setAttachSize(mFile.getSize());
				storeTag.setStoreDirectory(storeDirectory);
				storeTag.setStoreName(storeName);
				storeTag.setStorePath(storePath);
				storeTag.setStoreDate(transDate);
				storeTag.setSystemInfo(systemUtil.getSystemInfo());
				
				return storeTag;	
			}
						
		};
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}