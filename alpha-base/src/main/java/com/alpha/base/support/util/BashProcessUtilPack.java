package com.alpha.base.support.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.alpha.base.exception.SystemException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BashProcessUtilPack {
		
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// 값 변경시 bash 스크립트의 해당부분도 변경해야 함
	public static final String SYNC_KEY_NAME = "agentSyncYn";

	// 값 변경시 bash 스크립트의 해당부분도 변경해야 함
	private static final String TRACE_KEY_NAME = "processExecuteNo";

	// 값 변경시 bash 스크립트의 해당부분도 변경해야 함
	private static final String NOTE_KEY_NAME = "processExecuteNotePath";

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Data
	public static class BashProcessMeta implements Serializable{
		private static final long serialVersionUID = 1L;
		private String directory;	
		private String message;
	}
	
	@Data
	public static class BashProcessRequest implements Serializable {
		private static final long serialVersionUID = 1L;
		private String script;		
		private long processExecuteNo;
		private String processExecuteSyncYn;
		private String processExecuteNotePath;
		private Map<String,String> environments;
		private List<String> parameters;
	}

	@Data
	public static class BashProcessRequestOption implements Serializable {
		private static final long serialVersionUID = 1L;
		private long metaExecuteNo;
		private String metaExecuteSyncYn = "N"; //default: N
		private String metaExecuteNotePath;
	}
	
	@Data
	public static class BashProcessResponse implements Serializable {
		private static final long serialVersionUID = 1L;
		private long processExecuteNo;
		private Date processExecuteDate;
		private String console;
		private String error;
		private String exitValue;
	}

	@Data
	public static class BashProcessStatus implements Serializable {
		private static final long serialVersionUID = 1L;
    	private String uid;
    	private String pid;
    	private String ppid;
    	private String cpu;
    	private String stime;
    	private String tty;
    	private String time;
    	private String cmd;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface BashProcessUtil {

		@FunctionalInterface
		public interface BashProcessMetaFunction {public abstract BashProcessMeta getMeta();}

		public String command(String command);

		public BashProcessResponse execute(String script, BashProcessMetaFunction metaFunc);
		public BashProcessResponse execute(String script, BashProcessRequestOption option, BashProcessMetaFunction metaFunc);

		public BashProcessResponse execute(String script, List<String> parameters, BashProcessMetaFunction metaFunc);
		public BashProcessResponse execute(String script, List<String> parameters, BashProcessRequestOption option, BashProcessMetaFunction metaFunc);
		
		public BashProcessResponse execute(BashProcessRequest bashRequest, BashProcessMetaFunction metaFunc);
		public BashProcessResponse execute(BashProcessRequest bashRequest, BashProcessRequestOption option, BashProcessMetaFunction metaFunc);
		
		public int stop(long processExecuteNo);
		public int stop(String pid);
		
		public BashProcessStatus status(long processExecuteNo);
		public List<BashProcessStatus> status(String... greps);
		public List<BashProcessStatus> status(Set<String> grepSet);
		public List<BashProcessStatus> status(String grepInfo);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static String getEnvShell(String osName) {
		//log.info(">> osName: {}",osName);
		if (!osName.toLowerCase().contains("linux")) {return null;}
		
		StringBuilder console = new StringBuilder();
		try {
			ProcessBuilder processBuilder=new ProcessBuilder("sh","-c","env | grep SHELL");
			Process process=processBuilder.start();
            			
			try(BufferedReader consoleReader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
				
				String line;
	            while((line=consoleReader.readLine()) != null) {
	            	console.append(line);
	            }
			}

		} catch (IOException e) {
			throw new SystemException(e.getMessage());
		}
		
		log.debug(">> envShell :{}",console);
		return console.toString();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static BashProcessUtil getBashProcessUtil() {
		
		String osName = System.getProperty("os.name"); 
		String envShell = getEnvShell(osName); 
		
		return new BashProcessUtil() {
						
			private void checkSystem() {
		    	if (!osName.toLowerCase().contains("linux")) {throw new SystemException("해당 기능은 linux 시스템에서만 지원합니다.");}
		    	if (!envShell.toLowerCase().contains("bash")) {throw new SystemException("해당 기능은 bash 시스템에서만 지원합니다.");}
			}
			
			@Override
			public String command(String command) {
				if (StringUtils.isEmpty(command)) {return null;}
				BashProcessRequest bashRequest=new BashProcessRequest();
				bashRequest.setScript(command);
				BashProcessResponse response = this._execute(bashRequest, "command", null, false);
		    	if (response == null ) {return null;}
		    	
		    	if (!StringUtils.isEmpty(response.getError())) {
		    		throw new SystemException(response.getError());
		    	}
		    	
		    	return response.getConsole();				
			}

			@Override
			public BashProcessResponse execute(String script, BashProcessMetaFunction metaFunc) {
				return this.execute(script, null, null, metaFunc);
			}

			@Override
			public BashProcessResponse execute(String script, List<String> parameters, BashProcessMetaFunction metaFunc) {
				return this.execute(script, parameters, null, metaFunc);
			}
			
			@Override
			public BashProcessResponse execute(String script, BashProcessRequestOption option, BashProcessMetaFunction metaFunc) {
				return this.execute(script, null, option, metaFunc);
			}
			
			@Override
			public BashProcessResponse execute(String script, List<String> parameters, BashProcessRequestOption option, BashProcessMetaFunction metaFunc) {
				BashProcessRequest bashRequest=new BashProcessRequest();
				bashRequest.setScript(script);				
				bashRequest.setParameters(parameters);				
				return this.execute(bashRequest, option, metaFunc);
			}

			@Override
			public BashProcessResponse execute(BashProcessRequest bashRequest, BashProcessRequestOption option, BashProcessMetaFunction metaFunc) {
				bashRequest.setProcessExecuteNo(option==null?-1L:option.getMetaExecuteNo());			
				bashRequest.setProcessExecuteSyncYn(option==null?"N":option.getMetaExecuteSyncYn());
				
				return this.execute(bashRequest, metaFunc);
			}
			
			@Override
			public BashProcessResponse execute(BashProcessRequest bashRequest, BashProcessMetaFunction metaFunc) {
				return this._execute(bashRequest,"script",metaFunc,true);
			}
			
			@Override
			public int stop(long processExecuteNo) {
				BashProcessStatus status=this.status(processExecuteNo);
				if (status == null) {return -1;}
				return this.stop(status.getPid());
			}
			
			@Override
			public int stop(String pid) {
				if (StringUtils.isEmpty(pid)) {return -1;}
				
				BashProcessRequest bashRequest=new BashProcessRequest();
				bashRequest.setScript("kill -9 "+pid);
				
				BashProcessResponse response = this._execute(bashRequest,"command",null,true);
				return Integer.parseInt(response.getExitValue());
			}

			@Override
			public BashProcessStatus status(long processExecuteNo) {
				if (processExecuteNo < 0) {return null;}
				
				Set<String> grepSet = new HashSet<>();
				grepSet.add(TRACE_KEY_NAME+":"+processExecuteNo);
				
				List<BashProcessStatus> list = this.status(grepSet);
				return (list != null && list.size()==1)?list.get(0):null;
			}

			@Override
			public List<BashProcessStatus> status(String... greps) {
				return this.status(new HashSet<String>(Arrays.asList(greps)));
			}
			
			@Override
			public List<BashProcessStatus> status(Set<String> grepSet) {
				if (grepSet == null || grepSet.isEmpty()) {return null;}
	
				String grepInfo = "ps -ef | grep -v grep";
				for(String item : grepSet) {
					grepInfo += " | grep "+item.trim();
				}
				return this.status(grepInfo);
			}
			
			@Override
			public List<BashProcessStatus> status(String grepInfo) {
				this.checkSystem();
				
				if (StringUtils.isEmpty(grepInfo)) {return null;}
				
				BashProcessRequest request = new BashProcessRequest();
				request.setScript(grepInfo);
				
				BashProcessResponse response = this._execute(request,"command",null,true);
				
		    	if (!StringUtils.isEmpty(response.getError())) {
		    		throw new SystemException(response.getError());
		    	}
		    	
		    	if (StringUtils.isEmpty(response.getConsole())) {
		    		return null;
		    	}

		    	String uid = response.getConsole().split(" ")[0];

		    	List<BashProcessStatus> processStatusList = new ArrayList<>();		    	
				Arrays.asList(response.getConsole().split(uid)).stream()
						.filter(StringUtils::isNotEmpty)
						.collect(Collectors.toList())
						.forEach(data -> processStatusList.add(this.getBashProcessStatus(data,uid)));

		    	return processStatusList;
			}

		    private BashProcessStatus getBashProcessStatus(String data, String rowSpliter) {

				String[] segments = null;
				if (data.indexOf(" ") != -1) {
					data=data.replace(" ","@");
					while(data.indexOf("@@") > -1) {data = data.replace("@@","@");}		
					if (data.indexOf("@") != -1) {segments = data.split("@");}
				}

				if (segments == null) {return null;}

				BashProcessStatus processStatus = new BashProcessStatus();
				processStatus.setUid(rowSpliter);
				if (segments.length > 0) {processStatus.setPid(segments[1]);}
				if (segments.length > 1) {processStatus.setPpid(segments[2]);}
				if (segments.length > 2) {processStatus.setCpu(segments[3]);}
				if (segments.length > 3) {processStatus.setStime(segments[4]);}
				if (segments.length > 4) {processStatus.setTty(segments[5]);}
				if (segments.length > 5) {processStatus.setTime(segments[6]);}
				if (segments.length > 7) {
					String cmd="";
					for(int i=7 ; i < segments.length ; i++) {
						cmd += segments[i] + " ";
					}
					processStatus.setCmd(cmd.trim());
				}
	
		    	return processStatus;
		    }
		    
			private BashProcessResponse _execute(BashProcessRequest bashRequest, String mode, BashProcessMetaFunction metaFunc, boolean isLog) {
				this.checkSystem();
				
				if(isLog) {
					log.debug("");
					log.debug(">> [start] bash process execute");
				}
				
				long startTime = System.nanoTime();
				
				if (bashRequest == null) {
					log.warn(">> bashRequest is null");
					return null;
				}
				if (StringUtils.isEmpty(bashRequest.getScript())) {
					log.warn(">> bashRequest.getScript() is null");
					return null;
				}

				long processExecuteNo = startTime;
				if(bashRequest.getProcessExecuteNo()>-1L) {processExecuteNo=bashRequest.getProcessExecuteNo();}

				String processExecuteSyncYn = bashRequest.getProcessExecuteSyncYn();
				String processExecuteNotePath = bashRequest.getProcessExecuteNotePath();
						    	
				String bashScript=bashRequest.getScript();
				if (mode.equals("script")) {
			    	if (bashRequest.getParameters() != null && !bashRequest.getParameters().isEmpty()) {
			    		for(String param : bashRequest.getParameters()) {
			    			bashScript += " \""+param+"\"";
			    		}
			    	}
			    	bashScript+= " \"--" + TRACE_KEY_NAME + ":" + processExecuteNo +"\"";
			    	bashScript+= " \"--" + SYNC_KEY_NAME + ":" + processExecuteSyncYn +"\"";		
			    	bashScript+= " \"--" + NOTE_KEY_NAME + ":" + processExecuteNotePath +"\"";		

		    		bashScript = bashScript.trim();
		    		if(isLog) {log.debug(">> bashScript: {}",bashScript);}
				} else {
					if(isLog) {log.debug(">> bashCommand: {}",bashScript);}		
				}
		    	
		    	List<String> command=new ArrayList<>();
		    	command.add("bash");
		    	command.add("-c");
		    	command.add(bashScript);
		    	
		    	ProcessBuilder processBuilder = new ProcessBuilder(command);    	
		    	Map<String, String> environment = processBuilder.environment();
		    	environment.put("TERM", "xterm");
		    	environment.put("COLOR_ENABLE", "false");
		    	if (mode.equals("script")) {
			    	if (bashRequest.getEnvironments() != null) {
			    		bashRequest.getEnvironments().forEach((k,v) -> environment.put(k,v));
			    	}
		    	}
		    	int exitValue = -1;
		    	
		        Date processExecuteDate = null;
		        Process process = null;
	            StringBuilder console = new StringBuilder();
		        StringBuilder error = new StringBuilder();
		       
	        	boolean isEnable = true;
	        	if(isEnable && metaFunc==null) {isEnable=false;}
	        	if(isEnable && metaFunc.getMeta()==null) {isEnable=false;}
	        	if(isEnable) {
	        		BashProcessMeta meta = metaFunc.getMeta();
	        		if(isLog) {log.debug(">> meta: {}",meta);}
	        		if(isEnable && StringUtils.isEmpty(meta.getDirectory())) {isEnable=false;}
	        		if(isEnable && StringUtils.isEmpty(meta.getMessage())) {isEnable=false;}	        		
		        	if(isEnable){
		        		String metaFile = meta.getDirectory()+"/"+ processExecuteNo;			        	
		        		if(isLog) {log.debug(">> metaFile: {}",metaFile);}
		        		File file = new File(FilenameUtils.normalize(meta.getDirectory()));
		        		if(!file.exists()) {file.mkdirs();}
		        		if(!file.isDirectory()) {
		        			throw new SystemException(meta.getDirectory() + " is not directory.");
		        		}
		        		try(FileWriter fileWriter = new FileWriter(FilenameUtils.normalize(metaFile))){							
			        		fileWriter.write(meta.getMessage());
						} catch (IOException e) {
							throw new SystemException(e.getMessage());
						}
		        	}	        		
	        	}

		        StringBuilder consoleStackTraceBuilder = new StringBuilder(); // 스택 트레이스 저장 변수
		        StringBuilder errorStackTraceBuilder = new StringBuilder(); // 스택 트레이스 저장 변수
		        
		        try {

		        	// start
		        	processExecuteDate = new Date();
		            process = processBuilder.start();

			        boolean isExceptionBlock = false;
			        
			        String line;

		            // read console
				    try(BufferedReader consoleReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {          
			            while ((line = consoleReader.readLine()) != null) {
			            	console.append(line).append("\n");
			            	if (mode.equals("script")) {log.debug(">> console: {}",line);}
			            	
			            	// 예외 시작 부분 감지 -> 저장 -> 빈 줄을 만나면 예외 블록 종료
			                if (line.matches(".*Exception.*") || line.matches(".*Error.*") || line.matches(".*Caused by.*")) {isExceptionBlock = true;}
			                if (isExceptionBlock) {consoleStackTraceBuilder.append(line).append("\n");}
			                if (isExceptionBlock && line.trim().isEmpty()) {isExceptionBlock = false;}
			            }
				     }

			        // read error
				    try(BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
			            while ((line = errorReader.readLine()) != null) {
			            	error.append(line).append("\n");
			            	if (mode.equals("script")) {log.debug(">> error: {}",line);}
			            	
			            	// 예외 시작 부분 감지 -> 저장 -> 빈 줄을 만나면 예외 블록 종료
			                if (line.matches(".*Exception.*") || line.matches(".*Error.*") || line.matches(".*Caused by.*")) {isExceptionBlock = true;}
			                if (isExceptionBlock) {errorStackTraceBuilder.append(line).append("\n");}
			                if (isExceptionBlock && line.trim().isEmpty()) {isExceptionBlock = false;}			            	
			            }
		        	}
		            
		            exitValue = process.waitFor();
		            if (mode.equals("script")) {log.debug(">> exitValue: {}",exitValue);}
		            
		            if (exitValue != 0 && error.length() > 0) {
		            	error.append("Bash Script exitValue(").append(exitValue).append(") is not success.");
		            }
		            
				} catch (Exception e) {
					throw new SystemException(e.getMessage());
				}

		        StringBuilder errorBuilder = new StringBuilder();
		        if(exitValue!=0) {
			        if(!StringUtils.isEmpty(error)){errorBuilder.append(error.toString()).append("\n");}
			        if(consoleStackTraceBuilder.length()>0) {errorBuilder.append(consoleStackTraceBuilder.toString()).append("\n");}
			        if(errorStackTraceBuilder.length()>0) {errorBuilder.append(errorStackTraceBuilder.toString()).append("\n");}
		        }

		        BashProcessResponse bashResponse = new BashProcessResponse();
		        bashResponse.setProcessExecuteNo(processExecuteNo);
		        bashResponse.setProcessExecuteDate(processExecuteDate);
		        bashResponse.setConsole(console.toString());
		        bashResponse.setError(errorBuilder.toString());
		        bashResponse.setExitValue(String.valueOf(exitValue));
		        
		        long executeTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-startTime);
		        
		        if(isLog) {
		        	log.debug(">> [end] bash process execute,{}ms",executeTime);
		        }
				
		    	return bashResponse;		        
			}		    
		    
		};

	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}