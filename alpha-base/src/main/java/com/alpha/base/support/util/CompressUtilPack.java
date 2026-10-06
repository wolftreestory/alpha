package com.alpha.base.support.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.alpha.base.exception.SystemException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CompressUtilPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface CompressUtil {
		
		public boolean isZip(String targetPath);
								
		public File zip(String targetPath);
		public File zip(String targetPath, Boolean isRelativePath);
		public File zip(String targetPath, String zipFilePath, Boolean isRelativePath);
		
		public List<String> unZip(String zipFilePath);
		public List<String> unZip(String zipFilePath, String destPath);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static CompressUtil getCompressUtil() {
		
		return new CompressUtil() {

			@Override
			public boolean isZip(String targetPath) {	
				File target = new File(targetPath);
				if(!target.exists() || target.isDirectory()) {return false;}

				boolean result = false;
				try(ZipFile zipfile = new ZipFile(targetPath)) {
					result = true;
				}catch(IOException e) {
					result = false;
				}
				return result;
			}
			
			@Override
			public File zip(String targetPath) {
				return this.zip(targetPath, null, false);
			}

			@Override
			public File zip(String targetPath, Boolean isRelativePath) {
				return this.zip(targetPath, null, isRelativePath);
			}
			
			@Override
			public File zip(String targetPath, String zipFilePath, Boolean isRelativePath) {
				if(this.isZip(targetPath)) {throw new SystemException(targetPath+" is zip.(already compressed file)");}
				
				//target
				File target = new File(FilenameUtils.normalize(targetPath.trim()));				
				if(!target.exists()) {throw new SystemException(targetPath+" is not exist.");}
				if(target.getParent()==null) {throw new SystemException(targetPath+" is not allowed.");}
								
				if(StringUtils.isBlank(zipFilePath)) {
					zipFilePath = target.toPath().toString()+".zip";
				}
				
				//zipFile
				File zipFile = new File(FilenameUtils.normalize(zipFilePath.trim()));
				if(!zipFile.exists()) {zipFile.getParentFile().mkdirs();}
				
		    	try(ZipArchiveOutputStream zip = new ZipArchiveOutputStream(new FileOutputStream(zipFilePath))) {

		    		log.debug(">> zip: {} -> {}",target.getCanonicalPath(),zipFile.getCanonicalPath());
		    		
		    		Files.walk(target.toPath()).forEach(path -> {
		    			File entryFile = path.toFile();
		    			
		    			if(!entryFile.isDirectory()) {
		    				String entryName = entryFile.toString();    				
		    				if(isRelativePath) {
		    					entryName = entryName.substring(target.getParent().length());
		    				}
		    				log.debug(">> zip.entryName:{}",entryName);
		    				try(FileInputStream fis = new FileInputStream(entryFile)) {
		    					zip.putArchiveEntry(new ZipArchiveEntry(entryFile, entryName));
		    					IOUtils.copy(fis, zip);
		    					zip.closeArchiveEntry();
		    				}catch(IOException e) {
		    					throw new SystemException(e.getMessage());
		    				}
		    			}
		    		});
		    		zip.finish();
		    	}catch(Exception e) {
		    		throw new SystemException(e.getMessage());
		    	}

		    	return zipFile;
			
			}
			
			@Override
			public List<String> unZip(String zipFilePath) {
				return this.unZip(zipFilePath, null);
			}
			
			@Override
			public List<String> unZip(String zipFilePath, String unzipPath) {
				if(StringUtils.isBlank(zipFilePath)) {throw new SystemException(zipFilePath+" is blank.");}				
				
				//zipFile
				File zipFile = new File(FilenameUtils.normalize(zipFilePath.trim()));
				if(!zipFile.exists()) {throw new SystemException(zipFilePath+" is not exist.");}				
				
				//unzipFile
				if(StringUtils.isBlank(unzipPath)) {					
					unzipPath = zipFile.getParent();
				}
				File unzipFile = new File(FilenameUtils.normalize(unzipPath.trim()));
				if(unzipFile.exists() && !unzipFile.isDirectory()) {throw new SystemException(unzipPath+" is not directory.");}
				unzipFile.mkdirs();

				List<String> list = new ArrayList<>();
				
	            try(ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
	                	            	
	            	log.debug(">> unzip: {} -> {}",zipFile.getCanonicalPath(),unzipFile.getCanonicalPath());
	            	
	            	ZipEntry zipEntry = null;
	                while((zipEntry = zipInputStream.getNextEntry()) != null) {
	                    int length = 0;
	                    
	                    String entryFilePath = FilenameUtils.normalize(unzipPath + File.separator + zipEntry.getName());	                    
	                    log.debug(">> unzip.entryFilePath:{}",entryFilePath);
	                    new File(new File(entryFilePath).getParent()).mkdirs();	                    
	                    
	                    try(BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(entryFilePath))) {
	                        while((length = zipInputStream.read()) != -1) {
	                        	out.write(length);
	                        }
	                        zipInputStream.closeEntry();
	                    }
	                    list.add(entryFilePath);
	                }
	                
	            } catch (Exception e) {
	            	throw new SystemException(e.getMessage());
				}

	            return list;
			}
			
		};
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
