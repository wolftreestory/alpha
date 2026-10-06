package com.alpha.base.support.util;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openjdk.jol.info.GraphLayout;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

@Slf4j
public final class SystemUtilPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/*
	 * SystemUtilPack은 애플리케이션·서버 식별 정보와 시스템 사용량을 조회한다.
	 *
	 * ◼ 주요 기능:
	 * - Spring Environment와 서버 설정을 이용한 배포 정보 구성
	 * - CPU·메모리 등 시스템 사용량 조회
	 * - 프로파일 및 서버 인스턴스 식별 정보 제공
	 *
	 * ◼ 주의사항:
	 * - CPU 부하 등 사용량 값은 측정 시점과 OSHI 지원 환경에 따라 달라질 수 있다.
	 * - 서버 식별·네트워크 주소는 실행 환경 설정에 의존한다.
	 */
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public interface SystemUtil {
		
		public final static String PROFILE_LOCAL="local";
		public final static String PROFILE_DEV="dev";
		public final static String PROFILE_QA="qa";
		public final static String PROFILE_PROD="prod";
		
		public Environment getEnvironment();
		public Map<String, String> getSystemInfo();
		public Map<String, String> getSystemUsage();
		public void printGraphLayout(Object obj);
		
		public String getApplicationName();
		public String getServerName();
		public String getServerIp();
		public String getServerPort();
		public String getServerIpPort();
		public String getServerInstanceCode();
		public String getServerInstanceCode(String serverIpPort);
		public String getServerInstanceCode(String serverIp, String serverPort);
		public String getHeartBeat();
		public String[] getActiveProfiles();
		public boolean isLocal();
		public boolean isDev();
		public boolean isStg();
		public boolean isProd();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static SystemUtil getSystemUtil(Environment environment, String serverName, String serverPort, String applicationName) {
		if (environment == null) {log.debug(">> param[environment] is null."); return null;}
		if (serverPort == null) {log.debug(">> param[serverPort] is null."); return null;}
		
		return new SystemUtil() {
			
			private Map<String, String> systemInfo=null;
			
			@Override
			public Map<String, String> getSystemInfo() {
				if (this.systemInfo == null) {
					this.systemInfo = new HashMap<>();
					systemInfo.put("serverName", this.getServerName());
					systemInfo.put("serverIp", this.getServerIp());
					systemInfo.put("serverPort", this.getServerPort());
					systemInfo.put("serverInstanceCode", this.getServerInstanceCode());
				}
				return this.systemInfo;
			}
			
			@Override
			public Map<String, String> getSystemUsage() {
			    SystemInfo si = new SystemInfo();
			    HardwareAbstractionLayer hal = si.getHardware();
			    CentralProcessor cpu = hal.getProcessor();

			    Map<String, String> map = new HashMap<>();

			    // CPU load (ticks 기반, 더 정확)
			    long[] prevTicks = cpu.getSystemCpuLoadTicks();
			    try {
			        Thread.sleep(1000); // 1초 간격으로 측정
			    } catch (InterruptedException e) {
			        Thread.currentThread().interrupt();
			    }
			    double cpuLoad = cpu.getSystemCpuLoadBetweenTicks(prevTicks) * 100;

			    // Memory
			    long freeMem = hal.getMemory().getAvailable();
			    long totalMem = hal.getMemory().getTotal();

			    map.put("1.cpu", String.format("%.2f", cpuLoad));
			    map.put("2.memory.free", String.format("%.2f", freeMem / 1024.0 / 1024.0 / 1024.0));
			    map.put("3.memory.total", String.format("%.2f", totalMem / 1024.0 / 1024.0 / 1024.0));

			    return map;
			}

			@Override
			public void printGraphLayout(Object obj) {
				System.out.println();
				System.out.println("object : " + obj.getClass().getName() + "(" + System.identityHashCode(obj) + ")");
				
				GraphLayout layout = GraphLayout.parseInstance(obj);
				String footprint = layout.toFootprint();
				String[] lines = footprint.split("\n");
				
				int maxDescLen = 40;
				List<String[]> rows = new ArrayList<>();
				long totalSum = 0;
				
				// DESCRIPTION 최대 길이 계산 + SUM 합산
				for (String line : lines) {
					line = line.trim();
					if (line.matches("^\\d + .*")) {
						String[] parts = line.split("\\s + ");
						if (parts.length >= 4) {
							
							// JVM 배열 타입을 사람이 읽기 쉬운 형태로 변환
							parts[3] = convertJvmArrayType(parts[3]);
							
							rows.add(parts);
							maxDescLen = Math.max(maxDescLen, parts[3].length());
							totalSum += Long.parseLong(parts[2]);
						}
					}
				}
				
				String border = " + ---------- + ---------- + ---------- + " + this.repeat("-", maxDescLen + 2) + " + ";
				
				System.out.println(border);
				System.out.printf("|%-10s|%-10s|%-10s| %-" + maxDescLen + "s |%n", "COUNT", "AVG", "SUM", "DESCRIPTION");
				System.out.println(border);
				
				for (String[] parts : rows) {
					System.out.printf("|%-10s|%-10s|%-10s| %-" + maxDescLen + "s |%n", parts[0], parts[1], parts[2], parts[3]);
				}
				
				System.out.println(border);
				System.out.printf("|%-10s|%-10s|%-10d| %-" + maxDescLen + "s |%n", "TOTAL", "", totalSum, "");
				System.out.println(border);
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
				if (serverName == null || serverName.equals("")) {
					try {return InetAddress.getLocalHost().getHostName();}
					catch (UnknownHostException e) {
						log.error(">> exception skip: {}", e.getMessage());
						//e.printStackTrace();
					}
				}
				return serverName;
			}
			
			@Override
			public String getServerIp() {
				String hostAddress=null;
				
				if (!StringUtils.isEmpty(this.getServerPort())) {
					try (DatagramSocket socket = new DatagramSocket()) {
						socket.connect(InetAddress.getByName(InetAddress.getLocalHost().getHostName()), Integer.parseInt(this.getServerPort()));
						hostAddress = socket.getLocalAddress().getHostAddress();
						//log.debug("socket.getLocalAddress().getHostAddress():{}", hostAddress);
					} catch (Exception e) {
						log.error(">> exception skip: {}", e.getMessage());
						//e.printStackTrace();
					}
				}
				
				if (hostAddress == null || hostAddress.equals("")) {hostAddress="127.0.0.1";}
				return hostAddress;
			}
			
			@Override
			public String getServerPort() {
				return serverPort;
			}
			
			@Override
			public String getServerIpPort() {
				return this.getServerIp() + ":" + this.getServerPort();
			}
			
			@Override
			public String getServerInstanceCode() {
				return this.getServerInstanceCode(this.getServerIp(), serverPort);
			}
			
			@Override
			public String getServerInstanceCode(String serverIpPort) {
				if (serverIpPort == null || serverIpPort.equals("")) {return null;}
				if (serverIpPort.indexOf(":") == -1) {return this.getServerInstanceCode(serverIpPort, "");}
				
				String[] array=serverIpPort.split("\\:");
				if (array.length == 2) {return this.getServerInstanceCode(array[0], array[1]);}
				
				return null;
			}
			
			@Override
			public String getServerInstanceCode(String serverIp, String serverPort) {
				if (serverIp == null || serverIp.equals("")) {return null;}
				
				String[] array=serverIp.split("\\.");
				String hexCode="";
				for (int i = 0 ; i < array.length ; i++ ) {
					hexCode += Integer.toHexString(Integer.parseInt(array[i]));
				}
				hexCode+=serverPort;
				return hexCode;
			}
			
			@Override
			public String getHeartBeat() {
				return this.getServerInstanceCode() + ".heartBeat:" + System.currentTimeMillis();
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
				if (profiles == null || profiles.length == 0) {return false;}
				for (String profile:profiles) {
					if (profile.equals(target)) {return true;}
				}
				return false;
			}
			
			private String repeat(String s, int count) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < count; i++ ) sb.append(s);
				return sb.toString();
			}
			
			private String convertJvmArrayType(String desc) {
				if (desc.equals("[Z")) return "boolean[]";
				if (desc.equals("[B")) return "byte[]";
				if (desc.equals("[C")) return "char[]";
				if (desc.equals("[S")) return "short[]";
				if (desc.equals("[I")) return "int[]";
				if (desc.equals("[J")) return "long[]";
				if (desc.equals("[F")) return "float[]";
				if (desc.equals("[D")) return "double[]";
				
				if (desc.startsWith("[L") && desc.endsWith(";")) {
					String className = desc.substring(2, desc.length() - 1);
					return className + "[]";
				}
				
				return desc;
			}			
		};
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}