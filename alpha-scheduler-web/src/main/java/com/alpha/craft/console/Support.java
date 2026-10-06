package com.alpha.craft.console;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;

import com.alpha.base.BaseUtil;
import com.alpha.base.support.batch.SchedulerManagerPack.SchedulerManager;
import com.alpha.base.support.batch.context.SchedulerContext;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Support {
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
    private SchedulerManager schedulerManager;

    public Support(SchedulerManager schedulerManager) {
    	this.schedulerManager = schedulerManager;
    }
    
    public boolean isThymeleaf() {
    	return false;
    }
    
	public String getMessage() throws JspException {
		String alertMessageKey = BaseUtil.getAlertUtil().getAlertMessageKey();
        Object object = BaseUtil.getSessionUtil().getAttribute(alertMessageKey);
        if(object==null) {return "";}
        
        String message = object.toString();        
        BaseUtil.getSessionUtil().removeAttribute(alertMessageKey);
        return message;
    }

	public String getLine(){
		String line="<div class='line'><table class='table'><tr class='tr'><td class='td1'></td></tr><tr class='tr'><td class='td2'></td></tr><tr class='tr'><td class='td1'></td></tr></table></div>";
		return line;
	}

	public String getSchedulerRequestPath(){
		String context = BaseUtil.getHttpRequestUtil().getHttpServletRequest().getContextPath();
		log.debug(">> context:{}",context);
		String schedulerRequestPath=context+"/"+schedulerManager.getSchedulerRequestPath();
		log.debug(">> schedulerRequestPath:{}",schedulerRequestPath);
		
		return schedulerRequestPath;
	}

	public Map<String,Object> getSystemInfo(){ 	
		Properties props = BaseUtil.getPropertiesUtil().getProperties();
		String systemEnvMode = props.getProperty("spring.profiles.active");
		String systemTitle = props.getProperty("alpha.batch.webConsole.title","BATCH");
		String systemTitleHtml = props.getProperty("alpha.batch.webConsole.titleHtml","console");
		if(systemEnvMode.equals("prod")) {systemEnvMode="";}
		
		String context = BaseUtil.getHttpRequestUtil().getHttpServletRequest().getContextPath();
		log.debug(">> context:{}",context);
		String schedulerRequestPath = schedulerManager.getSchedulerRequestPath();
		log.debug(">> schedulerRequestPath:{}",schedulerRequestPath);

		Map<String,Object> map = new HashMap<>();
		map.put("systemEnvMode", systemEnvMode);
		map.put("systemTitle", systemTitle);
		map.put("systemTitleHtml", systemTitleHtml);
		
		map.put("context", context);
		map.put("schedulerRequestPath", schedulerRequestPath);
		
		return map;
	}

    public  String getRoutedView(String name) {
    	return this.isThymeleaf()?"thymeleaf/"+name:name;
    }
    
    public  void setParamToSessionOnDetail(HttpServletRequest request) {
    	this.setParamToSession("activeTab",request);
    	this.setParamToSession("executionListFilterErrorYn",request);
    	this.setParamToSession("executionListFilterInterruptYn",request);
    	this.setParamToSession("executionListRownum",request);
    }
    
    public void setParamToSession(String param,HttpServletRequest request) {
        String value = request.getParameter(param);
        if(value != null && !value.equals("")) {BaseUtil.getSessionUtil().setAttribute(param,value);}
    }

    public void setParamToSession(String param,String defaultValue,HttpServletRequest request) {
        String value = request.getParameter(param);
        if(value == null || value.equals("")) {value = defaultValue;}
    	BaseUtil.getSessionUtil().setAttribute(param,value);
    }

    public String getParamAddOnSession(String param,HttpServletRequest request) {
    	String value = request.getParameter(param);
        if(value != null && !value.equals("")) {BaseUtil.getSessionUtil().setAttribute(param,value);}
        return value;
    }
        
    public String getParamLookUpSession(String param,HttpServletRequest request) {
    	String value = request.getParameter(param);
        if(value == null || value.equals("")) {
        	Object obj = BaseUtil.getSessionUtil().getAttribute(param);
        	if(obj != null) {value = obj.toString();}        	
        }        
        return value;   
    }

    public boolean isChangedParamAtSession(String param,HttpServletRequest request) {
    	Object paramSession = BaseUtil.getSessionUtil().getAttribute(param);    	
    	String paramValue = request.getParameter(param); 
        return paramValue.equals(paramSession==null?"":paramSession.toString())?false:true; 	
    }
    
    public String getExecutionPostProcessLookUpSession(String executionNo) {
        Object obj = BaseUtil.getSessionUtil().getAttribute("taskExecutionList");
        if(obj == null) {return null;}
        String value = null;
        List<Map<String,String>> list = BaseUtil.getObjectMapperUtil().getMapper().convertValue(obj, new TypeReference<List<Map<String,String>>>() {});
        for(int i = 0;i<list.size();i++) {if(list.get(i).get("executionNo").equals(executionNo)) {value = list.get(i).get("executionPostProcess");break;}}
        return value;
    }
    
    public boolean isNumericScheduleValue(String value) {
    	String _value = value;
    	if("smh".contains(_value.substring(_value.length()-1))) {_value = _value.substring(0, _value.length()-1);}
        return NumberUtils.isCreatable(_value);
    }
    
    public boolean isNumeric(String numStr) {
        try {
            Double.parseDouble(numStr);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
	}	

    public Set<String> getConfigKeySet(String configTarget) {
	   	Set<String> configKeySet = null;
	   	if(configTarget != null && !configTarget.equals("")) {
	   		configKeySet = new HashSet<>();       		
	   		String[] configKeys = configTarget.split(",");
	       	for(String key:configKeys) {configKeySet.add(key);}
	   	}
	   	return configKeySet;
    }
	
    public Map<String, Object> extractFileInfo(String dirPath) {
    	if(dirPath==null || dirPath.equals("")) {return null;}
    	
    	Map<String, Object> fileInfo = null;
        try {

            Path _dirPath = Paths.get(dirPath);
            if (!Files.exists(_dirPath) || !Files.isDirectory(_dirPath)) {
                throw new IllegalArgumentException("유효하지 않은 경로: " + _dirPath);
            }

            fileInfo = Files.walk(_dirPath)
                    .filter(file -> file.toString().endsWith(".jar"))
                    .max(Comparator.comparingLong(file -> {
                        try {
                            return Files.getLastModifiedTime(file).toMillis();
                        } catch (IOException e) {
                            return 0;
                        }
                    }))
                    .map(jarPath -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(jarPath, BasicFileAttributes.class);
                            long fileSizeKB = Files.size(jarPath) / 1024; // KB 변환

                            Map<String, Object> _fileInfo = new HashMap<>();
                            _fileInfo.put("path", jarPath);                          
                            _fileInfo.put("name", jarPath.getFileName());
                            _fileInfo.put("size", NumberFormat.getNumberInstance().format(fileSizeKB) + " (KB)");
                            _fileInfo.put("creationTime", attrs.creationTime());
                            _fileInfo.put("lastModifiedTime", attrs.lastModifiedTime());
                            _fileInfo.put("lastAccessTime", attrs.lastAccessTime());
                            return _fileInfo;
                        } catch (IOException e) {
                            throw new RuntimeException(e.getMessage());
                        }
                    })
                    .orElse(Collections.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return fileInfo;
    }

    public Map<String, Object> extractBuildInfo(String fileName){
    	if(fileName==null || fileName.equals("")) {return null;}
    	
        if (!Character.isDigit(fileName.charAt(0))) {
        	fileName = "N/A-N/A-"+fileName;
        }
        
        String[] parts = fileName.replace(".jar", "").split("-");
        if(parts.length<=5) {return null;}
        
        String buildNumber = parts[0];  // 첫 번째 요소 (빌드번호)
        String buildDate = parts[1];  // 두 번째 요소 (빌드일시)        
        String profile = parts[parts.length - 3];  // 마지막에서 3번째 요소 (프로파일)
        String version = parts[parts.length - 2] + "-" + parts[parts.length - 1];  // 마지막 두 개의 요소 (버전)

        StringBuilder projectNameBuilder = new StringBuilder();
        for (int i = 2; i < parts.length - 3; i++) {
            projectNameBuilder.append(parts[i]);
            if (i < parts.length - 4) {  // 마지막 요소가 아니면 "-" 추가
                projectNameBuilder.append("-");
            }
        }
        String projectName = projectNameBuilder.toString();

		try {
			Date date = new SimpleDateFormat("yyyyMMddHHmmss").parse(buildDate);
			buildDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
		} catch (ParseException e) {
			
		}

    	Map<String, Object> buildInfo = new HashMap<>();
        buildInfo.put("no", buildNumber);
        buildInfo.put("date", buildDate);
        buildInfo.put("project", projectName);
        buildInfo.put("profile", profile);
        buildInfo.put("version", version);

        return buildInfo;
    }    
    
    public void sleepSchedulerWait(SchedulerContext schedulerContext,int sleep,int max) {
		try {
		    for(int i=0;i<max;i++) {
		    	log.info(">> schedulerStatus:{}",schedulerContext.getSchedulerStatus());
		    	if(!schedulerContext.getSchedulerStatus().equals(SchedulerManager.SCHEDULER_STATUS_WAIT)) {break;}
		    	Thread.sleep(sleep);
		    }
		} catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage()); 
		}
    }
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
}