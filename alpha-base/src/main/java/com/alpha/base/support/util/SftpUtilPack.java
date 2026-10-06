package com.alpha.base.support.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.alpha.base.exception.SystemException;
import com.alpha.base.support.util.FileUtilPack.FileUtil;
import com.alpha.base.support.util.SftpUtilPack.SftpUtil.AuthType;
import com.alpha.base.support.util.SftpUtilPack.SftpUtil.FetchJobFunction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@Slf4j
public final class SftpUtilPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface SftpUtil {

		@FunctionalInterface
		public interface FetchJobFunction {public abstract Object execute(SFTPClient sftp, ChannelUtil util) throws Exception;}

		public enum AuthType {privatekey,password};
		public Channel build(String remoteHost, String username, String password) throws Exception;
		public Channel build(String remoteHost, String username, String password, long autoCloseMills) throws Exception;
		public Channel build(String remoteHost, String username, File privateKeyFile) throws Exception;
		public Channel build(String remoteHost, String username, File privateKeyFile, long autoCloseMills) throws Exception;
		
		public Channel build(String remoteHost, int remotePort, String username, String password) throws Exception;
		public Channel build(String remoteHost, int remotePort, String username, String password, long autoCloseMills) throws Exception;
		public Channel build(String remoteHost, int remotePort, String username, File privateKeyFile) throws Exception;
		public Channel build(String remoteHost, int remotePort, String username, File privateKeyFile, long autoCloseMills) throws Exception;
	}

	public interface ChannelUtil{
		public boolean isExist(String remotePath);
		public boolean isFile(String remotePath);
		public boolean isDirectory(String remotePath);

		public String getStorePath(String directory, String fileName);
		public String getUniqueName(String directory, String prefix, String fileName);
		public String normalizePath(String path);
		public String extractFileName(String path);
		public File mkdirs(String storePath);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class Channel {
		private static final int DEFAULT_REMOTE_PORT = 22;
		
		private SSHClient ssh;
		private SFTPClient sftp;
		
		private FileUtil fileUtil;
		private ChannelUtil util;

		private long autoCloseMills = -1;
		
		private AuthType authType;		
		private String remoteHost;
		private int remotePort;
		private String username;
		private String password;
		private File privateKeyFile;

		public Channel(String remoteHost, String username, String password, FileUtil fileUtil) {this(AuthType.password,remoteHost,DEFAULT_REMOTE_PORT,username,password,null,-1,fileUtil);}
		public Channel(String remoteHost, String username, String password, long autoCloseMills, FileUtil fileUtil) {this(AuthType.password,remoteHost,DEFAULT_REMOTE_PORT,username,password,null,autoCloseMills,fileUtil);}
		public Channel(String remoteHost, String username, File privateKeyFile, long autoCloseMills, FileUtil fileUtil) {this(AuthType.privatekey,remoteHost,DEFAULT_REMOTE_PORT,username,null,privateKeyFile,autoCloseMills,fileUtil);}			
		public Channel(String remoteHost, String username, File privateKeyFile, FileUtil fileUtil) {this(AuthType.privatekey,remoteHost,DEFAULT_REMOTE_PORT,username,null,privateKeyFile,-1,fileUtil);}

		public Channel(String remoteHost, int remotePort, String username, String password, FileUtil fileUtil) {this(AuthType.password,remoteHost,remotePort,username,password,null,-1,fileUtil);}
		public Channel(String remoteHost, int remotePort, String username, String password, long autoCloseMills, FileUtil fileUtil) {this(AuthType.password,remoteHost,remotePort,username,password,null,autoCloseMills,fileUtil);}
		public Channel(String remoteHost, int remotePort, String username, File privateKeyFile, long autoCloseMills, FileUtil fileUtil) {this(AuthType.privatekey,remoteHost,remotePort,username,null,privateKeyFile,autoCloseMills,fileUtil);}			
		public Channel(String remoteHost, int remotePort, String username, File privateKeyFile, FileUtil fileUtil) {this(AuthType.privatekey,remoteHost,remotePort,username,null,privateKeyFile,-1,fileUtil);}
		
		private Channel(AuthType authType, String remoteHost, int remotePort, String username, String password, File privateKeyFile, long autoCloseMills, FileUtil fileUtil) {
			if(StringUtils.isBlank(remoteHost)) {throw new SystemException("remoteHost is empty.");}
			if(StringUtils.isBlank(username)) {throw new SystemException("username is empty.");}
			if(authType.equals(AuthType.password) && StringUtils.isBlank(password)) {throw new SystemException("username is empty.");}
			if(authType.equals(AuthType.privatekey)) {
				if(privateKeyFile==null) {throw new SystemException("privateKeyFile is null.");}
				if(privateKeyFile.exists()==false) {throw new SystemException("privateKeyFile is not exists.");}
			}			

			this.authType = authType;
			this.remoteHost = remoteHost;
			this.remotePort = remotePort;
			this.username = username;
			this.password = password;			
			this.privateKeyFile = privateKeyFile;
			this.autoCloseMills = autoCloseMills;
			this.fileUtil = fileUtil;
		}
		
		public SFTPClient getSftp() throws IOException {
			boolean isEnable = true;
			if(isEnable && (this.ssh == null || this.sftp == null)) {isEnable=false;}
			if(isEnable && !(this.ssh.isConnected() && this.ssh.isAuthenticated())) {isEnable=false;}
			if(isEnable && !(this.sftp.getSFTPEngine().getSubsystem().isOpen())) {isEnable=false;}
			if(isEnable) {return this.sftp;}

			SSHClient ssh = null;
			if(this.authType.equals(AuthType.password)) {
				ssh = new SSHClient();
				ssh.addHostKeyVerifier(new PromiscuousVerifier());
				ssh.setRemoteCharset(Charset.forName("UTF-8"));		
				ssh.connect(this.remoteHost,this.remotePort);
				
				ssh.authPassword(this.username,this.password);
			}
			if(this.authType.equals(AuthType.privatekey)) {		
				ssh = new SSHClient();
				ssh.addHostKeyVerifier(new PromiscuousVerifier());
				ssh.setRemoteCharset(Charset.forName("UTF-8"));						
				ssh.connect(this.remoteHost,this.remotePort);
				ssh.authPublickey(this.username,ssh.loadKeys(this.privateKeyFile.getPath()));		
			}

			this.ssh = ssh;
			this.sftp = this.ssh.newSFTPClient();
			//this.sftp.getFileTransfer().setTransferListener(null);
			
			if(this.autoCloseMills > 0) {
				Timer timer = new Timer();
				timer.schedule(new TimerTask() {

					@Override
					public void run() {					
						try {
							close();
						}catch(Exception e) {
							throw new SystemException(e.getMessage());
						}
					}
				}, autoCloseMills);
			}
			
			return this.sftp;
		}
		
		public Object stage(FetchJobFunction job) throws Exception{
			Object object = job.execute(this.getSftp(),this.getChannelUtil());
			this.close();			
			return object;
		}

		public <T> T stage(FetchJobFunction job, Class<T> type) throws Exception{
			return this.stage(job, new TypeReference<T>() {
			    @Override
			    public java.lang.reflect.Type getType() {return type;}
			});
		}

		@SuppressWarnings("unchecked")
		public <T> T stage(FetchJobFunction job, TypeReference<T> typeReference) throws Exception{
			Object object = this.stage(job);
			
			if(typeReference.getType().equals((new TypeReference<FileAttributes>() {}).getType())) {return (T)object;}
			if(typeReference.getType().equals((new TypeReference<RemoteResourceInfo>() {}).getType())) {return (T)object;}
			if(typeReference.getType().equals((new TypeReference<List<RemoteResourceInfo>>() {}).getType())) {return (T)object;}
			if(typeReference.getType().equals((new TypeReference<List<FileAttributes>>() {}).getType())) {return (T)object;}

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);	
			mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);			
	    	T result = mapper.convertValue(object, typeReference);
	    	
			return result;
		}
		
		public void close() throws IOException {
			if(this.sftp!=null) {log.debug(">> sftp closed."); this.sftp.close();}
			if(this.ssh!=null) {log.debug(">> ssh closed."); this.ssh.close();}
		}

		private ChannelUtil getChannelUtil() {
			if(this.util!=null) {return this.util;}
			
			this.util = new ChannelUtil() {

				@Override
				public boolean isExist(String remotePath){
					try {
						FileAttributes attributes = sftp.statExistence(remotePath);
						if(attributes!=null) {return true;}
					}catch(Exception e) {
						//log.error(">> message:{}",e.getMessage());
					}
					return false;
				}

				@Override
				public boolean isFile(String remotePath){
					try {
						FileAttributes attributes = sftp.stat(remotePath);
						if(attributes!=null && attributes.getType().equals(FileMode.Type.REGULAR)) {return true;}
					}catch(Exception e) {
						//log.error(">> message:{}",e.getMessage());
					}
					return false;
				}

				@Override
				public boolean isDirectory(String remotePath){
					try {
						FileAttributes attributes = sftp.stat(remotePath);
						if(attributes!=null && attributes.getType().equals(FileMode.Type.DIRECTORY)) {return true;}
					}catch(Exception e) {
						//log.error(">> message:{}",e.getMessage());
					}
					return false;
				}

				@Override
				public String getStorePath(String directory, String fileName) {
					String transDate = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(System.currentTimeMillis());				
					String storePath = this.normalizePath(directory) + this.getUniqueName(directory, transDate, fileName);					
					return storePath;
				}

				@Override
				public File mkdirs(String storePath) {
		    		File file = new File(FilenameUtils.normalize(storePath));
		    		file.mkdirs();
		    		return file;
				}
				
				@Override
				public String getUniqueName(String directory, String prefix, String fileName) {
					return fileUtil.getUniqueName(directory, prefix, fileName);
				}
				
				@Override
				public String normalizePath(String path) {
					return fileUtil.normalizePath(path);
				}
			    
				@Override
				public String extractFileName(String path) {
					return fileUtil.extractFileName(path);
				}								
			};
			
			return this.getChannelUtil();
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static class ChannelContext {
    	private static Map<String,Channel> context = new HashMap<>();

    	public static String buildKey(AuthType authType, Object... keys) {
    		StringBuilder builder = new StringBuilder();    		
    		builder.append("++").append(authType);
    		for(int i=0;i<keys.length;i++) {builder.append(":").append(keys[i]);}
    		builder.append("++");
    		return builder.toString();
    	}   	
    	public static boolean isExist(String key){return context.containsKey(key);}
    	public static Channel get(String key){return isExist(key)?context.get(key):null;}    	
    	public static synchronized void add(String key,Channel channel){context.put(key, channel);}
    }
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static SftpUtil getSftpUtil(FileUtil fileUtil) {
		
		return new SftpUtil() {

			@Override
			public Channel build(String remoteHost, String username, String password) throws Exception{
				return this.build(remoteHost, Channel.DEFAULT_REMOTE_PORT, username, password);
			}
			
			@Override
			public Channel build(String remoteHost, String username, String password, long autoCloseMills) throws Exception{
				return this.build(remoteHost, Channel.DEFAULT_REMOTE_PORT, username, password, autoCloseMills);
			}
			
			@Override
			public Channel build(String remoteHost, String username, File privateKeyFile) throws Exception{
				return this.build(remoteHost, Channel.DEFAULT_REMOTE_PORT , username, privateKeyFile);
			}

			@Override
			public Channel build(String remoteHost, String username, File privateKeyFile, long autoCloseMills) throws Exception{
				return this.build(remoteHost, Channel.DEFAULT_REMOTE_PORT , username, privateKeyFile, autoCloseMills);				
			}
			
			@Override
			public Channel build(String remoteHost, int remotePort, String username, String password) throws Exception {
				return this.build(remoteHost, remotePort, username, password,-1);
			}

			@Override
			public Channel build(String remoteHost, int remotePort, String username, File privateKeyFile) throws Exception {
				return this.build(remoteHost, remotePort, username, privateKeyFile, -1);
			}
			
			@Override
			public Channel build(String remoteHost, int remotePort, String username, String password, long autoCloseMills) throws Exception {	
				if(StringUtils.isBlank(remoteHost)) {throw new SystemException("remoteHost is empty.");}
				if(StringUtils.isBlank(username)) {throw new SystemException("username is empty.");}
				if(StringUtils.isBlank(password)) {throw new SystemException("password is empty.");}

				String key = ChannelContext.buildKey(AuthType.password,remoteHost,username,password);
				if(!ChannelContext.isExist(key)) {ChannelContext.add(key,new Channel(remoteHost,remotePort,username,password,autoCloseMills,fileUtil));}		
				return ChannelContext.get(key);
			}

			@Override
			public Channel build(String remoteHost, int remotePort, String username, File privateKeyFile, long autoCloseMills) throws Exception {
				if(StringUtils.isBlank(remoteHost)) {throw new SystemException("remoteHost is empty.");}
				if(StringUtils.isBlank(username)) {throw new SystemException("username is empty.");}
				if(privateKeyFile==null) {throw new SystemException("privateKeyFile is null.");}
				
				String key = ChannelContext.buildKey(AuthType.privatekey,remoteHost,username,privateKeyFile.getPath());
				if(!ChannelContext.isExist(key)) {ChannelContext.add(key,new Channel(remoteHost,remotePort,username,privateKeyFile,autoCloseMills,fileUtil));}
				return ChannelContext.get(key);
			}

		};
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
