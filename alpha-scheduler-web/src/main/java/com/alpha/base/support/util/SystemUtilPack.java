package com.alpha.base.support.util;

import java.lang.management.ManagementFactory;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SystemUtilPack {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface SystemUtil {

		public final static String PROFILE_LOCAL="local";
		public final static String PROFILE_DEV="dev";
		public final static String PROFILE_QA="qa";
		public final static String PROFILE_PROD="prod";	

		public Environment getEnvironment();
		public Map<String,String> getSystemInfo();
		public Map<String,String> getSystemUsage();
		
		public String getApplicationName();
		public String getServerName();			
		public String getServerIp();
		public String getServerPort();
		public String getServerIpPort();
		public String getServerInstanceCode();
		public String getServerInstanceCode(String serverIpPort);
		public String getServerInstanceCode(String serverIp,String serverPort);
		public String getHeartBeat();
		public String[] getActiveProfiles();
		public boolean isLocal();
		public boolean isDev();
		public boolean isStg();
		public boolean isProd();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static SystemUtil getSystemUtil(Environment environment, String serverName, String serverPort, String applicationName) {
		if(environment==null) {log.debug(">> param[environment] is null."); return null;}
		if(serverPort==null) {log.debug(">> param[serverPort] is null."); return null;}

		return new SystemUtil() {

			private Map<String,String> systemInfo=null;
			
			@Override
			public Map<String,String> getSystemInfo(){
				if(this.systemInfo==null) {
					this.systemInfo = new HashMap<>();
					systemInfo.put("serverName", this.getServerName());
					systemInfo.put("serverIp", this.getServerIp());
					systemInfo.put("serverPort", this.getServerPort());
					systemInfo.put("serverInstanceCode", this.getServerInstanceCode());
				}
				return this.systemInfo;				
			}

			@SuppressWarnings("restriction")
			@Override
			public Map<String,String> getSystemUsage() {
				com.sun.management.OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);        
		        Map<String,String> map = new HashMap<>();
		        map.put("1.cpu",String.format("%.2f",osBean.getSystemCpuLoad()*100));
		        map.put("2.memory.free",String.format("%.2f", (double)osBean.getFreePhysicalMemorySize()/1024/1024/1024));
		        map.put("3.memory.total",String.format("%.2f", (double)osBean.getTotalPhysicalMemorySize()/1024/1024/1024));
		        return map;
		    }
			
			public String getApplicationName() {
				return applicationName;
			}
			
			@Override
			public Environment getEnvironment() {
				return environment;
			}

			@Override
			public String getServerName() {
				if(serverName==null || serverName.equals("")) {
					try {return InetAddress.getLocalHost().getHostName();}
					catch (UnknownHostException e) {
						log.error(">> exception skip: {}",e.getMessage());
						//e.printStackTrace();
					}
				}
				return serverName;
			}

			@Override
			public String getServerIp() {
				String hostAddress=null;
				
				if(!StringUtils.isEmpty(this.getServerPort())) {
					try(DatagramSocket socket = new DatagramSocket()) {
						socket.connect(InetAddress.getByName(InetAddress.getLocalHost().getHostName()), Integer.parseInt(this.getServerPort()));
						hostAddress = socket.getLocalAddress().getHostAddress();		
						//log.debug("socket.getLocalAddress().getHostAddress():{}",hostAddress);
					} catch (Exception e) {
						log.error(">> exception skip: {}",e.getMessage());
						//e.printStackTrace();
					}
				}
				
				if(hostAddress==null || hostAddress.equals("")) {hostAddress="127.0.0.1";}
				return hostAddress;
			}
			
			@Override
			public String getServerPort() {
				return serverPort;
			}
					
			@Override
			public String getServerIpPort() {
				return this.getServerIp()+":"+this.getServerPort();
			}
			
			@Override
			public String getServerInstanceCode() {
				return this.getServerInstanceCode(this.getServerIp(),serverPort);
			}
			
			@Override
			public String getServerInstanceCode(String serverIpPort) {
				if(serverIpPort==null || serverIpPort.equals("")) {return null;}
				if(serverIpPort.indexOf(":")==-1) {return this.getServerInstanceCode(serverIpPort,"");}
				
				String[] array=serverIpPort.split("\\:");
				if(array.length==2) {return this.getServerInstanceCode(array[0],array[1]);}			

				return null;
			}
			
			@Override
			public String getServerInstanceCode(String serverIp,String serverPort) {
				if(serverIp==null || serverIp.equals("")) {return null;}
				
				String[] array=serverIp.split("\\.");
				String hexCode="";
				for(int i=0;i<array.length;i++) {
					hexCode += Integer.toHexString(Integer.parseInt(array[i]));
				}
				hexCode+=serverPort;
				return hexCode;
			}

			@Override
			public String getHeartBeat() {
				return this.getServerInstanceCode()+".heartBeat:"+System.currentTimeMillis();
			}
		    
			@Override
			public String[] getActiveProfiles() {
				return environment.getActiveProfiles();
			}

			@Override
			public boolean isLocal() {
				return this.isProfile(PROFILE_LOCAL);				
			}

			@Override
			public boolean isDev() {
				return this.isProfile(PROFILE_DEV);
			}

			@Override
			public boolean isStg() {
				return this.isProfile(PROFILE_QA);								
			}

			@Override
			public boolean isProd() {
				return this.isProfile(PROFILE_PROD);				
			}
			
			private boolean isProfile(String target) {
				String[] profiles = this.getActiveProfiles();
				if(profiles==null || profiles.length==0) {return false;}
				for(String profile:profiles) {
					if(profile.equals(target)) {return true;}
				}
				return false;
			}
		};
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}