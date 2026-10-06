package com.alpha.base.support.batch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

import com.alpha.base.exception.SystemException;
import com.alpha.base.support.batch.SchedulerManagerPack.SchedulerManager;
import com.alpha.base.support.batch.TaskConfigPack.TaskConfig;
import com.alpha.base.support.batch.TaskConfigPack.TaskProperties;
import com.alpha.base.support.batch.TaskLogHandlerPack.TaskLogHandler;
import com.alpha.base.support.batch.TaskMetaAgentPack.AgentExecuteException;
import com.alpha.base.support.batch.TaskMetaAgentPack.AgentProcess;
import com.alpha.base.support.batch.TaskMetaAgentPack.AgentRequest;
import com.alpha.base.support.batch.TaskMetaAgentPack.TaskMetaAgent;
import com.alpha.base.support.batch.TaskPack.AbstractTask;
import com.alpha.base.support.batch.TaskPack.DeleteProcess;
import com.alpha.base.support.batch.TaskPack.Domain;
import com.alpha.base.support.batch.TaskPack.UpdateProcess;
import com.alpha.base.support.batch.vo.TaskExecutionVo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskMetaLoaderPack {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static enum ScheduleType {NONE,FIXED_RATE,FIXED_DELAY,CRON_EXPRESSION}
	
	@Getter @Setter @ToString
	public static class TaskMetaObject {
		private String projectName;
		private String appName;
		private String appComment;
		private String jobName;
		private String jobConfigClass;
		private String jobDescription;
		private Set<String> jobParameters;	
		private TaskMetaObject.Schedule schedule;		
		private TaskMetaObject.Environment environment;
		
		@Getter @Setter @ToString
		public static class Schedule {
			private ScheduleType scheduleType;
			private String scheduleValue;
		}

		@Getter @Setter @ToString
		public static class Environment {
			private String executionScript;
			private String packagePath;
			private String packagePathComment;			
			private String repositoryPath;
			private String repositoryPathComment;
		}
	}
		
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface TaskMetaLoader {

		public SchedulerManager getSchedulerManager();
		public void setSchedulerManager(SchedulerManager schedulerManager);
		
		public List<String> getScanPatternList();
		public void setScanPatternList(List<String> list);

		public TaskMetaAgent getTaskMetaAgent();
		public void setTaskMetaAgent(TaskMetaAgent taskMetaAgent);

		public void doCleanGarbage();
		public List<TaskMetaObject> doScanLoad();
		public List<TaskMetaObject> doScanSync(List<TaskPack.Task> taskList, Set<String> projectNameSet);

	}

    private static class GarbageRunnable implements Runnable {
    	private SchedulerManager schedulerManager;
    	private TaskMetaLoader taskMetaLoader;
    	private long interval = -1L;
    	private long max = 10;
    	public GarbageRunnable(TaskMetaLoader taskMetaLoader, SchedulerManager schedulerManager, long interval, long max) {
    		this.taskMetaLoader = taskMetaLoader;
    		this.schedulerManager = schedulerManager;
    		this.interval = interval;
    		this.max = max;
    	}		    	
        
		@Override
		public void run() {			
			try {
				for(int i=0;i<max;i++) {
					int runningTaskCount = this.schedulerManager.getSchedulerContext().getRunningTaskCount();
					log.debug(">> GarbageRunnable.runningTaskCount:{}",runningTaskCount);
					if(runningTaskCount==0) {this.taskMetaLoader.doCleanGarbage(); break;}					
					Thread.sleep(this.interval);
				}
			} catch (InterruptedException e) {
				throw new SystemException(e.getMessage());
			}
		}
    }
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static TaskMetaLoader getTaskMetaLoader(BatchUtil batchUtil, TaskLogHandler taskLogHandler) {
		
		return new TaskMetaLoader() {
			
		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
		
			class OutterTask extends AbstractTask {

				@Override
				public void task() throws Exception {
					this.getAgentProcess().execute(this);
				}
			}
			
			private static final String META_NOTE_FILE = ".metaNote";
			private static final String META_MEMORY_FILE = ".metaMemory";
			private static final String META_POLICY_FILE = ".metaPolicy";
			
			private SchedulerManager schedulerManager;			
			private List<String> scanPatternList;			
			private TaskMetaAgent taskMetaAgent;

		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////			

			@Override
			public SchedulerManager getSchedulerManager() {
				return this.schedulerManager;
			}
			
			@Override
			public void setSchedulerManager(SchedulerManager schedulerManager) {
				this.schedulerManager = schedulerManager;
			}
			
			@Override
			public List<String> getScanPatternList(){
				return this.scanPatternList;
			}
			
			@Override
			public void setScanPatternList(List<String> list) {
				this.scanPatternList = list;
			}

			@Override
			public TaskMetaAgent getTaskMetaAgent() {
				return this.taskMetaAgent;
			}

			@Override
			public void setTaskMetaAgent(TaskMetaAgent taskMetaAgent) {
				this.taskMetaAgent = taskMetaAgent;				
			}
			
			@Override
			public void doCleanGarbage() {
				long timeMills = System.currentTimeMillis();
				
		        Set<String> targetSet = new HashSet<>();
		        
		        List<TaskPack.Task> taskList = this.getSchedulerManager().getSchedulerContext().getTaskList();
		        taskList.forEach(i -> targetSet.add(i.touchTaskConfig(timeMills)));
		        
		        targetSet.forEach(target -> {
		            try (Stream<Path> files = Files.list(Paths.get(target))) {
		                files.forEach(file -> {
		                    try {
		                        FileTime fileTime = Files.getLastModifiedTime(file);
		                        if(fileTime.toMillis() != timeMills) {Files.delete(file);}
		                    } catch (IOException e) {
		                        throw new SystemException(e.getMessage());
		                    }
		                });
		            } catch (IOException e) {
                        throw new SystemException(e.getMessage());
		            }
		        });
			}
							
			@Override
			public List<TaskMetaObject> doScanLoad() {
				List<TaskMetaObject> workList = Optional.ofNullable(this.getLoadList()).orElse(Collections.emptyList()).stream()
						.collect(Collectors.toList());
				this.syncTask(this.getSchedulerManager().getSchedulerContext().getTaskList(),workList);
				return workList;
		    }

			@Override
			public List<TaskMetaObject> doScanSync(List<TaskPack.Task> taskList, Set<String> projectNameSet) {
				List<TaskMetaObject> workList = Optional.ofNullable(this.getLoadList()).orElse(Collections.emptyList()).stream()
						.filter(t -> (projectNameSet==null || projectNameSet.isEmpty())?true:projectNameSet.contains(t.getProjectName()))
					    .collect(Collectors.toList());
				this.syncTask(taskList,workList);
				return workList;
			}

		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	

			private void syncTask(List<TaskPack.Task> taskList, List<TaskMetaObject> workList) {
				if(workList==null) {log.info(">> List<TaskMetaObject> workList is null."); return;}
				// 삭제처리를 위해서는 workList empty를 허용해야 함.

				List<TaskMetaObject> addList = new ArrayList<TaskMetaObject>();
				
				// 추가대상선정, 즉시수정, 수행후수정
				workList.forEach(taskMetaObject -> {
					String appName = taskMetaObject.getAppName();
					
					boolean isEnable = true;
					if(isEnable && StringUtils.isEmpty(taskMetaObject.getProjectName())) {isEnable=false;}
					if(isEnable && !appName.startsWith(taskMetaObject.getProjectName())) {isEnable=false;}
					if(isEnable && !appName.endsWith(taskMetaObject.getJobName())) {isEnable=false;}
					if(!isEnable) {return;}

					TaskPack.Task targetTask = this.getSchedulerManager().getSchedulerContext().getTaskByName(appName);
					
					// 추가대상 선정 : 작업대상에 있지만, 현재 schedulerContext에 없음
					if(targetTask==null) {
						addList.add(taskMetaObject);
					}
					
					// 즉시수정
					if(targetTask!=null && targetTask.isReady()) {		
						targetTask.setTaskMetaObject(taskMetaObject);
						this.getUpdateProcess().execute(targetTask);
					}
					
					// 수행후수정 : 수행중인 task 는 수행이 끝나고 task.executeTask()의 finally 블럭에서 updateProcess를 실행			
					if(targetTask!=null && targetTask.isRunning()) {
						targetTask.setTaskMetaObject(taskMetaObject);
						targetTask.setUpdateProcess(this.getUpdateProcess());
					}
				});
				
				// 삭제대상
				List<TaskPack.Task> deleteList = taskList.stream()
						.filter(t -> t.getDomain() == Domain.outter)
						.filter(t -> t.getDeleteYn().equals("N"))
						.filter(t -> workList.stream().noneMatch(w -> w.getAppName().equals(t.getName())))
						.collect(Collectors.toList());
				
				// 삭제대상, 수행후삭제
				deleteList.forEach(targetTask -> {
					// 즉시삭제
					if(targetTask.isReady()) {
						this.getDeleteProcess().execute(targetTask);
					}
					
					// 수행후삭제 : 수행중인 task 는 수행이 끝나고 task.executeTask()의 finally 블럭에서 deleteProcess를 실행			
					if(targetTask.isRunning()) {
						targetTask.setDeleteProcess(this.getDeleteProcess());
						}
				});

				// 신규건추가
		        if(!addList.isEmpty()) {
		        	addList.forEach(i->this.addTask(i));
		        }

		        // 비동기청소 (10초간격 체크후 수행, max:10)
	            Thread thread = new Thread(new GarbageRunnable(this,this.getSchedulerManager(),10000,10));
	            thread.start();
			}
					
			private void addTask(TaskMetaObject taskMetaObject) {
				log.info("");
				log.info(">> taskMetaObject: {}",taskMetaObject);
				
			    TaskProperties taskProperties = new TaskProperties();
			    
			    Set<String> jobParameters = taskMetaObject.getJobParameters();				    
				jobParameters.forEach(name->{
					String initValue = name.equals("params")?"*":"";
					taskProperties.setProperty(name,initValue,"jobParameters["+name+"]");
				});
				log.debug(">> taskMetaObject.properties: {}, {}",taskProperties.getBizProps(),taskProperties.getBizPropsDesc());
				
				TaskMetaObject.Schedule schedule = taskMetaObject.getSchedule();
				log.debug(">> taskMetaObject.schedule: {}",schedule.toString());
				
				TaskMetaObject.Environment environment = taskMetaObject.getEnvironment();
				log.debug(">> taskMetaObject.environment: {}",environment.toString());
				
			    TaskConfig config = TaskConfigPack.getTaskConfig(this.getSchedulerManager().getDefaultTaskConfig());
			    if(!taskProperties.isEmpty()) {
				    config.setBizProps(taskProperties.getBizProps());
				    config.setBizPropsDesc(taskProperties.getBizPropsDesc());
			    }

			    String enableYn = "Y";
			    if(enableYn.equals("Y") && batchUtil.getSystemUtil().isLocal()) {enableYn = "N";}
			    if(enableYn.equals("Y") && schedule.getScheduleType() == ScheduleType.NONE) {enableYn = "N";}
			    if(enableYn.equals("Y") && StringUtils.isEmpty(schedule.getScheduleValue())) {enableYn = "N";}

			    OutterTask task = new OutterTask();				    
				task.setDomain(Domain.outter);
				task.setGroup(taskMetaObject.getProjectName());
				task.setName(taskMetaObject.getAppName());
				task.setDescription(taskMetaObject.getJobDescription());					
				task.setEnableYn(enableYn);					
				task.setCronExpression(schedule.getScheduleType()==ScheduleType.CRON_EXPRESSION?schedule.getScheduleValue():null);
				task.setFixedDelay(schedule.getScheduleType()==ScheduleType.FIXED_DELAY?schedule.getScheduleValue():null);
				task.setFixedRate(schedule.getScheduleType()==ScheduleType.FIXED_RATE?schedule.getScheduleValue():null);
				task.setTaskConfig(config);				
			    task.setTaskMetaObject(taskMetaObject);
			    task.setTaskMetaAgent(this.taskMetaAgent);
			    task.setAgentProcess(this.getExecuteProcess());

			    this.getSchedulerManager().getSchedulerContext().getTaskList().removeIf(i->i.getName().equals(task.getName()));    
			    this.getSchedulerManager().addTask(task);
			}
						
		    private Set<String> noteFileToSet(String filePath) {
		    	File noteFile = new File(filePath);
		        
		        Set<String> set = new HashSet<>();
		        if(noteFile.exists()) {
			        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			            String line;
			            while ((line = reader.readLine()) != null) {set.add(line.trim());}
			            noteFile.delete();
			        } catch (IOException e) {	
			            throw new RuntimeException("파일 읽기 오류: " + e.getMessage(), e);
			        }
		        }
		        
		        this.deleteFilePattern(filePath, "^" + META_NOTE_FILE + ".*$");
		        return set;
		    }

		    private List<TaskMetaObject> getLoadList() {
		    	List<String> scanPatternList = this.getScanPatternList();
		    	if(scanPatternList==null || scanPatternList.isEmpty()) {return null;}
		    	scanPatternList.forEach(i->log.debug(">> scanPattern: {}",i));
		    	
				List<String> metaFileList = new ArrayList<>();
		    	scanPatternList.forEach(pattern->metaFileList.addAll(this.searchFiles(pattern)));
		    	metaFileList.forEach(i->log.debug(">> metaFile: {}",i));
		    	
				List<TaskMetaObject> loadList = new ArrayList<>();
				if(metaFileList==null || metaFileList.isEmpty()) {return null;}
				metaFileList.forEach(metaFile->{
					List<TaskMetaObject> list = this.readJsonFile(metaFile);
					list.forEach(t->{
						if(StringUtils.isEmpty(t.getProjectName())) {
							t.setProjectName(this.extractProjectName(scanPatternList, metaFile));
						}
					});
					loadList.addAll(list);	
				});
				
				final String activeProfile = this.getActiveProfile();
				
				loadList.forEach(t->{
					t.setAppComment(t.getEnvironment().getPackagePath()+"/"+t.getAppName()+"-[deployInfo].jar");
					t.getEnvironment().setPackagePathComment("[배치잡애플리케이션이름]-[패키지구성일시]-[빌드번호]-[빌드일시]-[프로젝트]-"+activeProfile+"-[버전].jar");
					t.getEnvironment().setRepositoryPathComment("[빌드번호]-[빌드일시]-[프로젝트]-"+activeProfile+"-[버전].jar");
				});
					   
				if(batchUtil.getSystemUtil().isLocal() && System.getProperty("os.name").toLowerCase().contains("windows")) {
					loadList.forEach(t->{
		            	String targetPath = scanPatternList.stream()
		            		    .map(s -> s.contains("**") ? s.replace("**", t.getProjectName()) : s)
		            		    .map(s -> s.contains("/target") ? s.substring(0, s.indexOf("/target") + "/target".length()) : s)
		            		    .distinct().findFirst().orElse(null);	
		            	
		            	if(targetPath!=null) {	
		            		String jarPath=this.getJarPath(targetPath);
		            		
		            		StringBuilder executionScript = new StringBuilder();
		            		executionScript.append("java");
		            		executionScript.append(" -jar ").append(jarPath);
		            		executionScript.append(" --spring.batch.job.names=").append(t.getJobName());
		            		executionScript.append(" --spring.profiles.active=").append(activeProfile);
		            		
		            		t.setAppComment(jarPath);
		            		t.getEnvironment().setPackagePath(targetPath);
							t.getEnvironment().setPackagePathComment("[프로젝트]-"+activeProfile+"-[버전].jar");
		            		t.getEnvironment().setRepositoryPath(targetPath);
							t.getEnvironment().setRepositoryPathComment("[프로젝트]-"+activeProfile+"-[버전].jar");
		            		t.getEnvironment().setExecutionScript(executionScript.toString());
		            	}
					});
				}
								
				if(loadList==null || loadList.isEmpty()) {return null;}
				loadList.forEach(i->log.debug(">> load: {}",i.getAppName()));
				
				return loadList;
		    }
		    
		    private String getActiveProfile() {
				if(batchUtil.getSystemUtil().isProd()) {return "prod";}
				else if(batchUtil.getSystemUtil().isStg()) {return "stg";}
				else if(batchUtil.getSystemUtil().isDev()) {return "dev";}
				else {return "local";}
		    }

		    private String extractProjectName(List<String> scanPatternList,String realPath) {	            
	            AntPathMatcher matcher = new AntPathMatcher();
	            String _realPath = realPath.replace("\\", "/");		            
		    	String projectName="";
		        for (String pattern : scanPatternList) {
		            if (matcher.match(pattern, _realPath)) {
		                String uriPattern = pattern.replace("**", "{wildcard}");
		                Map<String, String> variables = matcher.extractUriTemplateVariables(uriPattern, _realPath);
		                projectName = variables.get("wildcard");
		                break;
		            }
		        }
		        //log.debug(">> projectName:{}",projectName);
		        return projectName;
		    }
		    
		    private String getJarPath(String path) {
				try {
			    	return Files.walk(Paths.get(path))
							    .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".jar"))
							    .distinct().map(Path::toString).findFirst().orElse("");			    	
				} catch (IOException e) {
					throw new RuntimeException(e.getMessage());
				}		    	
		    }
		    
		    private List<TaskMetaObject> readJsonFile(String filePath) {
		        ObjectMapper objectMapper = new ObjectMapper();
		        try {
		            return objectMapper.readValue(new File(filePath), new TypeReference<List<TaskMetaObject>>(){});
		        } catch (IOException e) {
		            throw new RuntimeException(e.getMessage());
		        }
		    }
		    
		    private List<String> searchFiles(String pattern) {
		    	log.debug(">> pattern: {}",pattern);
		        List<String> foundPaths = new ArrayList<>();

		        String[] parts = pattern.split("\\*\\*", 2);        
		        if (parts.length != 2) {
		            Path filePath = Paths.get(pattern);
		            if (Files.exists(filePath)) {
		                foundPaths.add(filePath.toString());
		                return foundPaths;
		            }
		        }

		        String startDir = parts[0];
		        String globPattern = (parts.length > 1) ? "**/" + parts[1] : "";
		        globPattern="glob:" + globPattern.replace("//", "/");
		        
		    	//log.debug(">> startDir:{}",startDir);
		    	//log.debug(">> globPattern:{}",globPattern);

		        Path startPath = Paths.get(startDir);
		        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(globPattern);
		        
		        try {
		            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
		                @Override
		                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {	                    	
		                    if (matcher.matches(file)) {	  
		                    	log.debug(">> match: {}",file);
		                    	foundPaths.add(file.toString());
		                    }
		                    return FileVisitResult.CONTINUE;
		                }
		            });
		        } catch (IOException e) {
		        	log.warn("파일 탐색 중 오류 발생 (skip): " + e.getMessage());
		            //throw new RuntimeException(e);
		        }

		        return foundPaths;
		    }
		
		    private void writeFile(String filePath, String data) {
		        try (FileWriter writer = new FileWriter(filePath)) {
		            writer.write(data);
		        } catch (IOException e) {
		        	throw new RuntimeException("파일 저장 오류: " + e.getMessage(), e);
		        }
		    };	
		    
		    private void deleteFilePattern(String filePath, String fileNamePattern) {
		        Path dir = Paths.get(filePath).getParent();
		        //Pattern pattern = Pattern.compile("^" + META_MEMORY_FILE + ".*$");
		        Pattern pattern = Pattern.compile(fileNamePattern);
		        try {
		            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
		                @Override
		                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		                    Matcher matcher = pattern.matcher(file.getFileName().toString());
		                    if (matcher.matches()) {
		                    	Files.delete(file);
		                    }
		                    return FileVisitResult.CONTINUE;
		                }
		            });
		        } catch (IOException e) {
		        	throw new RuntimeException("파일 정리 오류: " + e.getMessage(), e);
		        }
		    };
		    
			private UpdateProcess getUpdateProcess() {
				return (TaskPack.Task task) -> {
					TaskMetaObject taskMetaObject = task.getTaskMetaObject();
					if(taskMetaObject == null) {return;}
	
					log.info(">> update: {}",task.getName());
					
					TaskMetaObject.Schedule schedule = taskMetaObject.getSchedule();
	
					String asisEnableYn = task.getEnableYn();						
					String enableYn = "Y";
				    if(enableYn.equals("Y") && batchUtil.getSystemUtil().isLocal()) {enableYn = "N";}
				    if(enableYn.equals("Y") && schedule.getScheduleType() == ScheduleType.NONE) {enableYn = "N";}
				    if(enableYn.equals("Y") && StringUtils.isEmpty(schedule.getScheduleValue())) {enableYn = "N";}
				    if(enableYn.equals("N")) {asisEnableYn="N";}
				    
					task.setEnableYn("N");
				    task.setDescription(taskMetaObject.getJobDescription());					    
				    task.setCronExpression(schedule.getScheduleType()==ScheduleType.CRON_EXPRESSION?schedule.getScheduleValue():null);
				    task.setFixedDelay(schedule.getScheduleType()==ScheduleType.FIXED_DELAY?schedule.getScheduleValue():null);
				    task.setFixedRate(schedule.getScheduleType()==ScheduleType.FIXED_RATE?schedule.getScheduleValue():null);
				    task.setUpdateDate((new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date(System.currentTimeMillis()))).toString());						
				    task.setEnableYn(asisEnableYn);
				    
				    task.getTaskConfig().saveConfigData(task.getName());				    
				};
			};
			
			private DeleteProcess getDeleteProcess() {
				return (TaskPack.Task task) -> {				
					log.info(">> delete: {}",task.getName());
	
					String taskName = task.getName();
					
		            // 삭제1 : configFile
		            task.getTaskConfig().deleteConfigData(taskName);
	
		            // 삭제2 : logFile,contextFile
		            TaskExecutionVo vo = new TaskExecutionVo();
		            vo.setExecutionName(taskName);
		            vo.setDeleteAllYn("Y"); 
		            taskLogHandler.getTaskLogContext().clearTaskLog(vo);
	
		            // 삭제3 : task
		            this.getSchedulerManager().getSchedulerContext().getTaskList().removeIf(t -> t.getName().equals(taskName));        
				};
			};
			
			private AgentProcess getExecuteProcess() {
				return (TaskPack.Task task) -> {
					
					log.info(">> [start] externalTaskProcess");
					task.setAgentRequest(new AgentRequest());
					
			        Map<String, Object> _jobParameters = null;
			        if(task.getTaskConfig().getBizProps()!=null && !task.getTaskConfig().getBizProps().isEmpty()) {
			        	_jobParameters = new HashMap<>();
				        for (String key : task.getTaskConfig().getBizProps().stringPropertyNames()) {
				        	String value = task.getTaskConfig().getBizProps().getProperty(key);
				        	if(value!=null && !value.equals("")) {_jobParameters.put(key,value);}
				        }
			        }
			        
			    	String executeScript = task.getTaskMetaObject().getEnvironment().getExecutionScript();		    	
					log.info(">> executeScript:{}",executeScript);

					String executeNotePath = task.getTaskMetaObject().getEnvironment().getPackagePath()+File.separator+META_NOTE_FILE+task.getExecutionNo();
					log.info(">> executeNotePath:{}",executeScript);

					String executeMemoryPath = task.getTaskMetaObject().getEnvironment().getPackagePath()+File.separator+META_MEMORY_FILE+task.getExecutionNo();
					log.info(">> executeMemoryPath:{}",executeMemoryPath);
					
					String executePolicyPath = task.getTaskMetaObject().getEnvironment().getPackagePath()+File.separator+META_POLICY_FILE+task.getExecutionNo();
					log.info(">> executePolicyPath:{}",executePolicyPath);
					
					// 실행메모리정보 파일생성 (bash 스크립트에서 참조함)
					if(task.getTaskConfig().getJvmMemoryEnableYn().equals("Y") && !StringUtils.isEmpty(task.getTaskConfig().getJvmMemory())) {						
						this.writeFile(executeMemoryPath,task.getTaskConfig().getJvmMemory());						
					}
					
					// 실행요청정보
					AgentRequest executeRequest = task.getAgentRequest();
			    	executeRequest.setAppName(task.getTaskMetaObject().getAppName());
			    	executeRequest.setExecuteNo(task.getExecutionNo());
			    	executeRequest.setExecuteSyncYn("Y");
			    	executeRequest.setExecuteNotePath(executeNotePath);			    	
			    	executeRequest.setJobParameters(_jobParameters);			    	
			    	if(task.getTaskConfig().getJobParametersApiUrlEnableYn().equals("Y")) {
				    	executeRequest.setJobParametersApiUrl(task.getTaskConfig().getJobParametersApiUrl());
				    	executeRequest.setJobParametersApiUrlOverlapYn(task.getTaskConfig().getJobParametersApiUrlOverlapYn());
			    	}
			    	if(task.getTaskConfig().getCallbackApiUrlEnableYn().equals("Y")) {
			    		executeRequest.setCallbackApiUrl(task.getTaskConfig().getCallbackApiUrl());
			    	}
					log.info(">> executeRequest:{}",executeRequest);
					
					// 실행
					task.setAgentResponse(task.getTaskMetaAgent().execute(executeRequest,executeScript));			

					// 실행로그파일정보 추출 (bash 스크립트 > 자바로 전달된executeNotePath에 logback에서 생성하는 로그파일경로 저장)
					Set<String> noteSet = this.noteFileToSet(executeNotePath);
					Set<String> logFileSet = noteSet.stream().filter(s -> s.startsWith("logfile:")).map(s -> s.substring("logfile:".length())).collect(Collectors.toSet());
					
					// 실행메모리파일 삭제
					this.deleteFilePattern(executeMemoryPath, "^" + META_MEMORY_FILE + ".*$");
					
					task.getAgentResponse().setProcessExecuteNote(noteSet.toString());
					task.getAgentResponse().setProcessExecuteLogFile(logFileSet);
			
					// 오류처리
					if(task.getAgentResponse().getExitValue().equals("0")==false) {
						String line="────────────────────────────────────────────────────────────────────────────";
						String errorMsg = task.getAgentResponse().getError().trim();
						StringBuilder builder = new StringBuilder();
						builder.append("metaTaskAgent(agent<bash<"+task.getTaskMetaObject().getAppName()+") error 전달").append("\n");
						builder.append(line).append("\n");
						builder.append("[error] package: "+task.getTaskMetaObject().getAppName()+", class: "+task.getTaskMetaObject().getJobConfigClass()).append("\n");
						if(StringUtils.isNotEmpty(errorMsg)) {
							builder.append(line).append("\n");
							builder.append(errorMsg).append("\n");
							builder.append(line);
						}
						throw new AgentExecuteException(builder.toString());
					}

					log.info(">> [end] externalTaskProcess");				
			    };
			};
		    
		};
	    
	    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////			
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}