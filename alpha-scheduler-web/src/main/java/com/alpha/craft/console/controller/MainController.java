package com.alpha.craft.console.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alpha.base.BaseUtil;
import com.alpha.base.advice.ExceptionAdvice;
import com.alpha.base.config.batch.BatchConfig;
import com.alpha.base.config.batch.BatchProperties;
import com.alpha.base.support.batch.TaskPack;
import com.alpha.base.support.batch.TaskConfigPack.TaskConfig;
import com.alpha.base.support.batch.TaskLogContextPack.TaskLogContext;
import com.alpha.base.support.batch.TaskLogHandlerPack.TaskLogHandler;
import com.alpha.base.support.batch.TaskMetaLoaderPack.TaskMetaLoader;
import com.alpha.base.support.batch.TaskMetaLoaderPack.TaskMetaObject;
import com.alpha.base.support.batch.TaskPack.Domain;
import com.alpha.base.support.batch.TaskPack.Task;
import com.alpha.base.support.batch.context.SchedulerContext;
import com.alpha.base.support.batch.vo.TaskExecutionVo;
import com.alpha.craft.console.Support;
import com.alpha.craft.console.service.ConsoleServicePack.AuthService;
import com.alpha.craft.console.service.ConsoleServicePack.MainService;
import com.alpha.craft.console.service.impl.AuthServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class MainController {

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
	private static String DEFAULT_TASK_LIST_PAGE_SIZE = "10";	
	private static String DEFAULT_EXECUTION_LIST_ROWNUM = "5";
    
    private AuthService authService;    
    private MainService mainService;
    private TaskLogHandler taskLogHandler;  
    private Support support;
    
	private String taskListPageSize = DEFAULT_TASK_LIST_PAGE_SIZE;
	private String confirmCheckValueYn;
	private String logHistoryShrinkYn;	
	private String supportTaskConfigInitYn;	
	private String supportOutterTaskChangeApplyYn;
	private String supportOutterTaskParamConfigYn;
	
    public MainController(BatchProperties batchProperties, TaskLogHandler taskLogHandler, AuthService authService, MainService mainService, Support support, AuthServiceImpl defaultLoginCheckService, ExceptionAdvice exceptionAdvice) {
    	this.taskLogHandler = taskLogHandler;
    	this.mainService = mainService;
    	this.authService = authService;
    	this.support = support;
    	
    	this.taskListPageSize = batchProperties.getWebConsole().getPageSize();
    	this.confirmCheckValueYn = batchProperties.getWebConsole().getConfirmCheckValueYn();
    	this.logHistoryShrinkYn = batchProperties.getTask().getLogPolicy().getLogHistoryShrinkYn(); 	
    	this.supportTaskConfigInitYn = batchProperties.getTask().getSupportTaskConfigInitYn();   	
    	this.supportOutterTaskParamConfigYn = batchProperties.getTask().getSupportOutterTaskParamConfigYn();
    	this.supportOutterTaskChangeApplyYn = batchProperties.getTask().getSupportOutterTaskChangeApplyYn();
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @RequestMapping(value = {"/{schedulerRequestPath}/main"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String schedulerMain(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}                
       
        log.debug(">> main:scheduler.list");
        
        String authKeyListYn = request.getParameter("authKeyListYn");
        if(!StringUtils.hasText(authKeyListYn)) {authKeyListYn = "N";}

        String taskListPageNo = this.support.getParamLookUpSession("taskListPageNo",request);  
        if(!StringUtils.hasText(taskListPageNo)) {taskListPageNo = "1";} //default
        this.support.setParamToSession("taskListPageNo", taskListPageNo, request);

        String taskListPageSize = this.support.getParamLookUpSession("taskListPageSize",request); 
        if(!StringUtils.hasText(taskListPageSize)) {taskListPageSize =  this.taskListPageSize; taskListPageNo = "1";} //default
        this.support.setParamToSession("taskListPageSize", taskListPageSize, request);

        String searchFilterArea = this.support.getParamLookUpSession("searchFilterArea",request);
        if(!StringUtils.hasText(searchFilterArea)) {searchFilterArea = "group";} //default
        this.support.setParamToSession("searchFilterArea", searchFilterArea, request);

        String searchFilterText = this.support.getParamLookUpSession("searchFilterText",request);
        String searchFilterType = this.support.getParamLookUpSession("searchFilterType",request); 
        if(StringUtils.hasText(searchFilterText) && searchFilterText.equals("*")) {searchFilterText = ""; searchFilterType="contains";}
        if(StringUtils.hasText(searchFilterText) && searchFilterText.endsWith(".e")) {searchFilterText = searchFilterText.substring(0, searchFilterText.length() - 2); searchFilterType="equals";}
        if(StringUtils.hasText(searchFilterText) && searchFilterText.endsWith(".c")) {searchFilterText = searchFilterText.substring(0, searchFilterText.length() - 2); searchFilterType="contains";}        
        this.support.setParamToSession("searchFilterText", searchFilterText, request);
        this.support.setParamToSession("searchFilterType", searchFilterType, request);

        String hiddenFilterYn = this.support.getParamLookUpSession("hiddenFilterYn",request);  
        if(!StringUtils.hasText(hiddenFilterYn)) {hiddenFilterYn = "Y";}
        this.support.setParamToSession("hiddenFilterYn", hiddenFilterYn, request);
        
        String viewPage = ((request.getParameter("meta") != null && request.getParameter("meta").equals("status"))?"console.meta":"console.main");
        viewPage = this.support.getRoutedView(viewPage);
        log.debug(">> viewPage:{}",viewPage);
        
        SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
        log.debug(">> schedulerContext.getSchedulerStatus():{}",schedulerContext.getSchedulerStatus()); 
        
        int taskCount = (schedulerContext.getTaskList() == null?0:schedulerContext.getTaskList().size());             
        String taskAnchor = (BaseUtil.getSessionUtil().getAttribute("taskAnchor") == null?"":(String)BaseUtil.getSessionUtil().getAttribute("taskAnchor"));
        
        String asyncChange = (BaseUtil.getSessionUtil().getAttribute("asyncChange") == null?"":(String)BaseUtil.getSessionUtil().getAttribute("asyncChange"));

        boolean isAutoReflash = false;
        if(!isAutoReflash && schedulerContext.isExistRunningTask()) {isAutoReflash = true;}
        if(!isAutoReflash && (asyncChange.equals("start") && !schedulerContext.isSchedulerStart())) {isAutoReflash = true;}
        if(!isAutoReflash && (asyncChange.equals("stop") && !schedulerContext.isSchedulerStop())) {isAutoReflash = true;}
        if(!isAutoReflash) {BaseUtil.getSessionUtil().removeAttribute("asyncChange");}
        log.debug(">> isAutoReflash: {}",isAutoReflash);
        
        String schedulerChangeStamp = (isAutoReflash?schedulerContext.getSchedulerChangeStamp():schedulerContext.getNewSchedulerChangeStamp());

        Map<String,Object> userInfo = this.authService.getUserInfo();
        if(userInfo==null) {return "redirect:/";}
        String adminYn=userInfo.get("userId").toString().equals("admin")?"Y":"N";
        
        Map<String,String> pageInfo = new HashMap<>();

        List<TaskPack.Task> taskList = schedulerContext.getTaskList(searchFilterArea,searchFilterText,searchFilterType,hiddenFilterYn);
        int taskListSize = taskList.size();
        
        if(taskList!=null && !taskList.isEmpty()) {
    		int listSize = taskList.size();
            int pageNo = Integer.parseInt(taskListPageNo);
            int pageSize = Integer.parseInt(taskListPageSize);
            int fromIndex = Math.max(0, (pageNo - 1) * pageSize);
            int toIndex = Math.min(fromIndex + pageSize, listSize);
            if(fromIndex>toIndex) {taskListPageNo="1";}

		    pageInfo.put("taskListPageNo", taskListPageNo);
		    pageInfo.put("taskListPageSize", taskListPageSize);
    		taskList = schedulerContext.getSubList(taskList, pageInfo);
		}

        Map<String,Object> schedulerInfo = new HashMap<>();
        schedulerInfo.put("serverIpPort", BaseUtil.getSystemUtil().getServerIpPort());
        schedulerInfo.put("serverInstanceCode", BaseUtil.getSystemUtil().getServerInstanceCode());
        schedulerInfo.put("schedulerContextStartTime", schedulerContext.getSchedulerContextStartTime());
        schedulerInfo.put("schedulerStatus", schedulerContext.getSchedulerStatus());
        schedulerInfo.put("schedulerStartTime", schedulerContext.getSchedulerStartTime());
        schedulerInfo.put("schedulerStopTime", schedulerContext.getSchedulerStopTime());
        schedulerInfo.put("userInfo", userInfo);

        model.addAllAttributes(support.getSystemInfo());
        
        model.addAttribute("metaType",request.getParameter("meta"));        
        model.addAttribute("loginMode",BaseUtil.getSessionUtil().getAttribute(AuthService.SCHEDULER_LOGIN_MODE));
        model.addAttribute("adminYn",adminYn);           
        
        model.addAttribute("actionPage","main");
        model.addAttribute("autoReflashYn",(isAutoReflash?"Y":"N"));
        
        model.addAttribute("schedulerInfo",BaseUtil.getJsonUtil().toJson(schedulerInfo));
        model.addAttribute("schedulerStatus",schedulerContext.getSchedulerStatus());
        model.addAttribute("schedulerChangeStamp",schedulerChangeStamp);        
        model.addAttribute("searchFilterArea",searchFilterArea);
        model.addAttribute("searchFilterText",searchFilterText);           

        model.addAttribute("taskList",taskList);
        model.addAttribute("taskListSize",taskListSize);
        model.addAttribute("taskListPageSize",taskListPageSize);
        model.addAttribute("taskListPageNo",taskListPageNo);
        model.addAttribute("taskListPageInfo",pageInfo.containsKey("pageInfo")?pageInfo.get("pageInfo"):"");
        model.addAttribute("taskListPageFirstYn",pageInfo.containsKey("firstYn")?pageInfo.get("firstYn"):"Y");
        model.addAttribute("taskListPageLastYn",pageInfo.containsKey("lastYn")?pageInfo.get("lastYn"):"N");
        
        model.addAttribute("isExistHidden", schedulerContext.isExistHidden());
        model.addAttribute("hiddenFilterYn",hiddenFilterYn);
        model.addAttribute("confirmCheckValueYn",this.confirmCheckValueYn);
        model.addAttribute("logHistoryShrinkYn",this.logHistoryShrinkYn);
        model.addAttribute("supportTaskConfigInitYn",this.supportTaskConfigInitYn);
        model.addAttribute("supportOutterTaskChangeApplyYn",this.supportOutterTaskChangeApplyYn);
        
        model.addAttribute("taskCount",taskCount);
        model.addAttribute("taskAnchor",taskAnchor);

    	model.addAttribute("authKeyList",authKeyListYn.equals("Y")?authService.getAuthKeyList():null);

        BaseUtil.getSessionUtil().removeAttribute("taskAnchor");

        return viewPage;
    }
    
    @RequestMapping(value = {"/{schedulerRequestPath}/start"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String schedulerStart(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

        log.debug(">> main:scheduler.start [schedulerChangeStamp:{}]",request.getParameter("schedulerChangeStamp"));
        String schedulerChangeStamp = request.getParameter("schedulerChangeStamp");

        SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
        log.debug(">> schedulerContext.getSchedulerStatus():{}",schedulerContext.getSchedulerStatus());  
        
        String message = "";
        boolean isEnable = true;
        if(isEnable && schedulerChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        if(isEnable && !schedulerContext.isEnableSchedulerChangeStamp(schedulerChangeStamp)) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
        if(isEnable && !schedulerContext.isSchedulerStop()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.011");}        
        if(isEnable && !schedulerContext.isExistEnableTask()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.012");} 
        if(isEnable && schedulerContext.isExistRunningTask()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.013");}      
        if(!isEnable) {
            BaseUtil.getAlertUtil().setAlertMessage(message); 
            return "redirect:main";
        }

        //this.support.sleepSchedulerWait(schedulerContext,100,100);
        
        BaseUtil.getSessionUtil().setAttribute("asyncChange","start");        
        schedulerContext.clearChangeStamp();        
        this.mainService.getSchedulerManager().asyncStartScheduler();
        
        return "redirect:main";
    }
    
    @RequestMapping(value = {"/{schedulerRequestPath}/stop"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String schedulerStop(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
        
        log.debug(">> main:scheduler.stop [schedulerChangeStamp:{}]",request.getParameter("schedulerChangeStamp"));     
        String schedulerChangeStamp = request.getParameter("schedulerChangeStamp");

        SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();   
        
        log.debug(">> schedulerContext.getSchedulerStatus():{}",schedulerContext.getSchedulerStatus());     
        
        String message = "";
        boolean isEnable = true;
        if(isEnable && schedulerChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        if(isEnable && !schedulerContext.isEnableSchedulerChangeStamp(schedulerChangeStamp)) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
        if(isEnable && !schedulerContext.isSchedulerStart()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.021");}        
        if(!isEnable) {
            BaseUtil.getAlertUtil().setAlertMessage(message); 
            return "redirect:main";
        }
        
        //this.support.sleepSchedulerWait(schedulerContext,100,100);
        
        BaseUtil.getSessionUtil().setAttribute("asyncChange","stop");
        schedulerContext.clearChangeStamp();        
        
        if(schedulerContext.isExistRunningTask()) {
        	BaseUtil.getAlertUtil().setAlertMessage(BaseUtil.getMessageUtil().getMessage("main.message.alert.022"));
        }
        
        this.mainService.getSchedulerManager().asyncStopScheduler();
        
        return "redirect:main";
    }   

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @RequestMapping(value = {"/{schedulerRequestPath}/support/enableAll"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String supportEnableAll(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

        log.debug(">> main:support.enable.all [schedulerChangeStamp:{}]",request.getParameter("schedulerChangeStamp"));
        String schedulerChangeStamp = request.getParameter("schedulerChangeStamp");
        
        SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
        String enableYn = request.getParameter("enableYn");
        
        String message = "";
        boolean isEnable = true;
        if(isEnable && schedulerChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        if(isEnable && !schedulerContext.isEnableSchedulerChangeStamp(schedulerChangeStamp)) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
        if(isEnable && enableYn==null || enableYn.equals("")) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        if(!isEnable) {
            BaseUtil.getAlertUtil().setAlertMessage(message); 
            return "redirect:../main";
        }
        
        String searchFilterArea = this.support.getParamLookUpSession("searchFilterArea",request);
        if(!StringUtils.hasText(searchFilterArea)) {searchFilterArea = "group";} //default
        //log.debug(">> searchFilterArea:{}",searchFilterArea);

        String searchFilterText = this.support.getParamLookUpSession("searchFilterText",request);
        String searchFilterType = this.support.getParamLookUpSession("searchFilterType",request);
        if(StringUtils.hasText(searchFilterText) && searchFilterText.equals("*")) {searchFilterText = ""; searchFilterType="contains";}
        if(StringUtils.hasText(searchFilterText) && searchFilterText.endsWith(".e")) {searchFilterText = searchFilterText.substring(0, searchFilterText.length() - 2); searchFilterType="equals";}
        if(StringUtils.hasText(searchFilterText) && searchFilterText.endsWith(".c")) {searchFilterText = searchFilterText.substring(0, searchFilterText.length() - 2); searchFilterType="contains";}  
        //log.debug(">> searchFilterText:{}",searchFilterText);
        //log.debug(">> searchFilterType:{}",searchFilterType);

        String hiddenFilterYn = this.support.getParamLookUpSession("hiddenFilterYn",request);  
        if(!StringUtils.hasText(hiddenFilterYn)) {hiddenFilterYn = "Y";}
        //log.debug(">> hiddenFilterYn:{}",hiddenFilterYn);
        
        int workCount=0;
        int noneCount=0;
        StringBuilder builder = new StringBuilder();
        List<TaskPack.Task> taskList = schedulerContext.getTaskList(searchFilterArea,searchFilterText,searchFilterType,hiddenFilterYn);        
        for(TaskPack.Task task : taskList) {
        	if(task.getTriggerType()==null) {noneCount++;continue;}
        	if(task.getEnableYn().equals(enableYn)) {continue;}
    		this.mainService.getSchedulerManager().setEnable(task.getTaskId(),enableYn);
    		builder.append(" - ").append(task.getName()).append("\\n");
    		workCount++;
        }
        //log.debug(">> workCount:{},noneCount:{}",workCount,noneCount);
        
        if(enableYn.equals("Y")) {message="타스크 활성 (대상:"+workCount+", 미대상:"+noneCount+")\\n"+builder.toString();}
        else {message="타스크 비활성 (대상:"+workCount+", 미대상:"+noneCount+")\\n"+builder.toString();}        
        //log.debug(">> message:{}",message);
        
        BaseUtil.getAlertUtil().setAlertMessage(message); 
        
        return "redirect:../main";
    }
    
    @RequestMapping(value = {"/{schedulerRequestPath}/support/clearHistory"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String supportClearHistory(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
        
        String actionPage = ((request.getParameter("actionPage") == null)?"":request.getParameter("actionPage"));
        
        if(actionPage.equals("main")) {          
            log.debug(">> main:support.clearHistory [schedulerChangeStamp:{}]",request.getParameter("schedulerChangeStamp"));

        	SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
	        
	        String message = "";
	        boolean isEnable = true;
	        if(isEnable && (this.logHistoryShrinkYn == null || this.logHistoryShrinkYn.equals("N"))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.031");}
	        if(isEnable && schedulerContext.isSchedulerStart()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.032"); }
	        if(isEnable && schedulerContext.isExistRunningTask()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.033");}
	        if(!isEnable) {
	            BaseUtil.getAlertUtil().setAlertMessage(message); 
	            return "redirect:../main";
	        }
	        
	        //모든 수행카운트 초기화
	        schedulerContext.getTaskList().forEach(item -> item.resetCount());
	        
	        //모든 수행컨텍스트 삭제
	        TaskExecutionVo vo = new TaskExecutionVo();
	        vo.setDeleteAllYn("Y");
	        this.taskLogHandler.getTaskLogContext().clearTaskLog(vo);
	        
	        BaseUtil.getAlertUtil().setAlertMessage("모든 타스크의 수행로그를 삭제하였습니다."); 
	        
	        return "redirect:../main";
        }
        
        if(actionPage.equals("detail")) {
        	
            log.debug(">> detail:support.clearHistory [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
            
            String taskChangeStamp = request.getParameter("taskChangeStamp");
            String executionNo = request.getParameter("executionNo");    
            String deleteAllYn = request.getParameter("deleteAllYn");             
            String taskId = this.support.getParamAddOnSession("taskId",request);

            this.support.setParamToSessionOnDetail(request);

            SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
            
            String message = "";
            boolean isEnable = true;
            if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
            if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
            if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
            if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.033");}  
            if(!isEnable) {
                BaseUtil.getAlertUtil().setAlertMessage(message); 
                return "redirect:../detail";
            }
            
            //선택한 수행카운트 초기화
            TaskPack.Task task = schedulerContext.getTask(taskId);
            if(deleteAllYn.equals("Y")) {task.resetCount();}
            
            //선택한 수행컨텍스트 삭제
            TaskExecutionVo vo = new TaskExecutionVo();
            vo.setExecutionNo(executionNo);
            vo.setExecutionName(task.getName());
            vo.setDeleteAllYn(deleteAllYn);
            this.taskLogHandler.getTaskLogContext().clearTaskLog(vo);
                        
            return "redirect:../detail";
        }
        
        return "redirect:../main";        
        
    } 

    @RequestMapping(value = {"/{schedulerRequestPath}/support/initConfig"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String supportInitConfig(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

        log.debug(">> main:support.initConfig [schedulerChangeStamp:{}]",request.getParameter("schedulerChangeStamp"));
        
        SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
        
        String message = "";
        boolean isEnable = true;
        if(isEnable && (this.supportTaskConfigInitYn == null || this.supportTaskConfigInitYn.equals("N"))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.061");}
        if(isEnable && schedulerContext.isSchedulerStart()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.062");}
        if(isEnable && schedulerContext.isExistRunningTask()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.063");}
        if(!isEnable) {
            BaseUtil.getAlertUtil().setAlertMessage(message); 
            return "redirect:../main";
        }

        schedulerContext.getTaskList().forEach(item -> item.initTaskConfig());                

        BaseUtil.getAlertUtil().setAlertMessage("모든 타스크의 설정이 재구성 되었습니다."); 
        
        return "redirect:../main";
    } 
            
    @RequestMapping(value = {"/{schedulerRequestPath}/support/scanDeploy"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String supportScanDeploy(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

        log.debug(">> main:support.scanMeta [schedulerChangeStamp:{}]",request.getParameter("schedulerChangeStamp"));
        
        SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
        
        String message = "";
        boolean isEnable = true;
        if(isEnable && (this.supportOutterTaskChangeApplyYn == null || this.supportOutterTaskChangeApplyYn.equals("N"))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.71");}
        if(isEnable && schedulerContext.isSchedulerStart()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.062");}
        if(isEnable && schedulerContext.isExistRunningTask()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.063");}        
        if(!isEnable) {
            BaseUtil.getAlertUtil().setAlertMessage(message); 
            return "redirect:main";
        }
        
        String searchFilterArea = this.support.getParamLookUpSession("searchFilterArea",request);  
        log.debug(">> searchFilterArea:{}",searchFilterArea);

        String searchFilterText = this.support.getParamLookUpSession("searchFilterText",request);  
        log.debug(">> searchFilterText:{}",searchFilterText);
                
        List<TaskPack.Task> taskList = schedulerContext.getTaskList(searchFilterArea,searchFilterText,"contains","N");

        Set<String> groupSet = null;
        if(StringUtils.hasText(searchFilterText)) { // 현재 선택된 taskGroup 추출
        	groupSet = taskList.stream().filter(t->t.getDomain() == Domain.outter).map(TaskPack.Task::getGroup).collect(Collectors.toSet());
        }    
        log.debug(">> groupSet:{}",groupSet);

        
        TaskMetaLoader taskMetaLoader = this.mainService.getSchedulerManager().getTaskMetaLoader();
        List<TaskMetaObject> workList = taskMetaLoader.doScanSync(taskList, groupSet);
        
        int workCount=0;
        StringBuilder builder = new StringBuilder();
        for(TaskMetaObject object : workList) {
    		builder.append(" - ").append(object.getAppName()).append("\\n");
    		workCount++;
        }

        BaseUtil.getAlertUtil().setAlertMessage("배포된 배치프로젝트로 업데이트 되었습니다. (대상:"+workCount+")\\n"+builder.toString()); 
        
        return "redirect:../main";
    } 
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    @RequestMapping(value = {"/{schedulerRequestPath}/enable"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String taskEnable(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
        
        String actionPage = ((request.getParameter("actionPage") == null)?"":request.getParameter("actionPage"));
        log.debug(">> actionPage:{}",actionPage);
        
        if(actionPage.equals("main")) {
            log.debug(">> main:task.enable [schedulerChangeStamp:{}]",request.getParameter("schedulerChangeStamp"));

            String schedulerChangeStamp = request.getParameter("schedulerChangeStamp");
            String taskId = request.getParameter("taskId");
            String enableYn = request.getParameter("enableYn");

            SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
            Task task = schedulerContext.getTask(taskId);
            
            String message = "";
            boolean isEnable = true;
            if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}    
            if(isEnable && (enableYn == null || enableYn.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");} 
            if(isEnable && schedulerChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
            if(isEnable && !schedulerContext.isEnableSchedulerChangeStamp(schedulerChangeStamp)) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
            if(isEnable && task.isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}            
            if(isEnable && task.isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
            if(isEnable && task.getTriggerType()==null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.045");}            
            if(isEnable && task.getTaskConfig().getScheduleTermEnableYn().equals("Y") && !task.getTaskConfig().isScheduleTermAlive()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.046");}            
            if(!isEnable) {
                BaseUtil.getSessionUtil().setAttribute("taskAnchor",taskId);
                BaseUtil.getAlertUtil().setAlertMessage(message); 
                return "redirect:main";
            }
            BaseUtil.getSessionUtil().setAttribute("taskAnchor",taskId);  
            schedulerContext.getTask(taskId).clearTaskChangeStamp();
            this.mainService.getSchedulerManager().setEnable(taskId,enableYn);
            
            return "redirect:main";
        }
        
        if(actionPage.equals("detail")) {
            log.debug(">> detail:task.enable [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
            
            String taskChangeStamp  = request.getParameter("taskChangeStamp");        
            String taskId = this.support.getParamAddOnSession("taskId",request);
            String enableYn = request.getParameter("enableYn");       

            this.support.setParamToSessionOnDetail(request);

            SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
            Task task = schedulerContext.getTask(taskId);
            
            String message = "";
            boolean isEnable = true;
            if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}    
            if(isEnable && (enableYn == null || enableYn.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");} 
            if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
            if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
            if(isEnable && task.isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}            
            if(isEnable && task.isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
            if(isEnable && task.getTriggerType()==null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.045");}            
            if(isEnable && task.getTaskConfig().getScheduleTermEnableYn().equals("Y") && !task.getTaskConfig().isScheduleTermAlive()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.046");}            
            if(!isEnable) {      
                BaseUtil.getAlertUtil().setAlertMessage(message); 
                return "redirect:detail";
            }      
            this.mainService.getSchedulerManager().setEnable(taskId,enableYn);
            schedulerContext.clearschedulerChangeStamp();
            return "redirect:detail";
        }
        
        return "redirect:main";
    }   
    
    @RequestMapping(value = {"/{schedulerRequestPath}/execute"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String taskExecute(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
        
        String actionPage = ((request.getParameter("actionPage") == null)?"":request.getParameter("actionPage"));
        
        if(actionPage.equals("main")) {        
            log.debug(">> main:task.execute [schedulerChangeStamp:{}]",request.getParameter("schedulerChangeStamp"));
            String schedulerChangeStamp = request.getParameter("schedulerChangeStamp");    
            String taskId = request.getParameter("taskId");
    
            SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
            
            String message = "";
            boolean isEnable = true;
            if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
            if(isEnable && schedulerChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
            if(isEnable && !schedulerContext.isEnableSchedulerChangeStamp(schedulerChangeStamp)) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
            if(isEnable && schedulerContext.getTask(taskId).isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}
            if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.043");}
            if(!isEnable) {
                BaseUtil.getSessionUtil().setAttribute("taskAnchor",taskId);        
                BaseUtil.getAlertUtil().setAlertMessage(message); 
                return "redirect:main";
            }
            BaseUtil.getSessionUtil().setAttribute("taskAnchor",taskId);          
            schedulerContext.getTask(taskId).clearTaskChangeStamp();
            this.mainService.getSchedulerManager().doExecute(taskId);    
            
            return "redirect:main";
        }
        
        if(actionPage.equals("detail")) {
            log.debug(">> detail:task.execute [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
            String taskChangeStamp  = request.getParameter("taskChangeStamp");        
            String taskId = this.support.getParamAddOnSession("taskId",request);

            this.support.setParamToSessionOnDetail(request);

            SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
            
            String message = "";
            boolean isEnable = true;
            if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
            if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
            if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
            if(isEnable && schedulerContext.getTask(taskId).isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}
            if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.043");}
            if(!isEnable) {     
                BaseUtil.getAlertUtil().setAlertMessage(message); 
                return "redirect:detail";
            }
            schedulerContext.clearschedulerChangeStamp();
            this.mainService.getSchedulerManager().doExecute(taskId);
            
            return "redirect:detail";
        }
        
        return "redirect:main";
    }   

    @RequestMapping(value = {"/{schedulerRequestPath}/interrupt"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String taskInterrupt(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
        
        String actionPage = ((request.getParameter("actionPage") == null)?"":request.getParameter("actionPage"));
        
        if(actionPage.equals("main")) {        
            log.debug(">> main:task.interrupt [schedulerChangeStamp:{}]",request.getParameter("schedulerChangeStamp"));
            String schedulerChangeStamp = request.getParameter("schedulerChangeStamp");    
            String taskId = request.getParameter("taskId");
    
            SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
            
            String message = "";
            boolean isEnable = true;
            if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
            if(isEnable && schedulerChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
            if(isEnable && !schedulerContext.isEnableSchedulerChangeStamp(schedulerChangeStamp)) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
            if(isEnable && schedulerContext.getTask(taskId).isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}           
            if(isEnable && schedulerContext.getTask(taskId).isReady()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.044");}
            if(!isEnable) {
                BaseUtil.getSessionUtil().setAttribute("taskAnchor",taskId);        
                BaseUtil.getAlertUtil().setAlertMessage(message); 
                return "redirect:main";
            }
            BaseUtil.getSessionUtil().setAttribute("taskAnchor",taskId);   
            schedulerContext.getTask(taskId).clearTaskChangeStamp();
            this.mainService.getSchedulerManager().doInterrupt(taskId);    
            
            return "redirect:main";
        }
        if(actionPage.equals("detail")) {
            log.debug(">> detail:task.interrupt [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
            String taskChangeStamp  = request.getParameter("taskChangeStamp");        
            String taskId = this.support.getParamAddOnSession("taskId",request);

            this.support.setParamToSessionOnDetail(request);

            SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
            
            String message = "";
            boolean isEnable = true;
            if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
            if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
            if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
            if(isEnable && schedulerContext.getTask(taskId).isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}
            if(isEnable && schedulerContext.getTask(taskId).isReady()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.044");}
            if(!isEnable) {     
                BaseUtil.getAlertUtil().setAlertMessage(message); 
                return "redirect:detail";
            }
            schedulerContext.clearschedulerChangeStamp();
            this.mainService.getSchedulerManager().doInterrupt(taskId);
            
            return "redirect:detail";
        }
        
        return "redirect:main"; 
    }
        
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @RequestMapping(value = {"/{schedulerRequestPath}/detail"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String taskDetail(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
        
        log.debug(">> detail:task");

        Map<String,Object> userInfo = this.authService.getUserInfo();
        if(userInfo==null) {return "redirect:/";}
        String adminYn=userInfo.get("userId").toString().equals("admin")?"Y":"N";
        
        String viewPage = ((request.getParameter("meta") != null && request.getParameter("meta").equals("status"))?"console.meta":"console.detail");
        viewPage = this.support.getRoutedView(viewPage);
        log.debug(">> viewPage:{}",viewPage);
        
        String taskId = request.getParameter("taskId");
        if(taskId == null || taskId.equals("")) {taskId = (BaseUtil.getSessionUtil().getAttribute("taskId") == null?"":BaseUtil.getSessionUtil().getAttribute("taskId").toString());}
        if(taskId == null || taskId.equals("")) {return "redirect:main";}

        //String taskName = request.getParameter("taskName");
        //if(taskName == null || taskName.equals("")) {taskName = (BaseUtil.getSessionUtil().getAttribute("taskName") == null?"":BaseUtil.getSessionUtil().getAttribute("taskName").toString());}
        //if(taskName == null || taskName.equals("")) {return "redirect:main";}

        String activeTab = request.getParameter("activeTab");
        if(activeTab == null || activeTab.equals("")) {activeTab = (BaseUtil.getSessionUtil().getAttribute("activeTab") == null?"":BaseUtil.getSessionUtil().getAttribute("activeTab").toString());}
        if(activeTab == null || activeTab.equals("")) {activeTab = "";}

        String detailViewCheckYn = this.support.getParamLookUpSession("detailViewCheckYn",request);  
        if(detailViewCheckYn == null || detailViewCheckYn.equals("")) {detailViewCheckYn = "N";} //default

        String paramExtensionConfigCheckYn = this.support.getParamLookUpSession("paramExtensionConfigCheckYn",request);  
        if(paramExtensionConfigCheckYn == null || paramExtensionConfigCheckYn.equals("")) {paramExtensionConfigCheckYn = "N";} //default
        
        String executionEnvViewCheckYn = this.support.getParamLookUpSession("executionEnvViewCheckYn",request);  
        if(executionEnvViewCheckYn == null || executionEnvViewCheckYn.equals("")) {executionEnvViewCheckYn = "N";} //default

        String executionListFilterErrorYn = this.support.getParamLookUpSession("executionListFilterErrorYn",request);
        if(executionListFilterErrorYn == null || executionListFilterErrorYn.equals("")) {executionListFilterErrorYn = "";} //default:모두
         
        String executionListFilterInterruptYn = this.support.getParamLookUpSession("executionListFilterInterruptYn",request);
        if(executionListFilterInterruptYn == null || executionListFilterInterruptYn.equals("")) {executionListFilterInterruptYn = "";} //default:모두

        String executionListRownum = this.support.getParamLookUpSession("executionListRownum",request);
        if(executionListRownum == null || executionListRownum.equals("") || !this.support.isNumeric(executionListRownum)) {executionListRownum = DEFAULT_EXECUTION_LIST_ROWNUM;} //default
        //this.setParamToSession("executionListRownum", request);
        
        SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
        TaskPack.Task task = schedulerContext.getTask(taskId);
        
        TaskExecutionVo vo = new TaskExecutionVo();
        vo.setExecutionName(task.getName());
        vo.setRownum(Long.parseLong(executionListRownum));   
    	vo.setExceptionYn(executionListFilterErrorYn.equals("Y")||executionListFilterInterruptYn.equals("Y")?"Y":"");
    	vo.setErrorYn(executionListFilterErrorYn.equals("Y")?"Y":"");
    	vo.setInterruptYn(executionListFilterInterruptYn.equals("Y")?"Y":"");

        List<Map<String,Object>> taskExecutionList = this.taskLogHandler.getTaskLogContext().getTaskExecutionList(vo);
        BaseUtil.getSessionUtil().setAttribute("taskExecutionList", taskExecutionList);
        Map<String,Object> autoReflashTaskExecutionInfo = (taskExecutionList != null && taskExecutionList.size()>0?taskExecutionList.get(0):null);
        
        boolean isNowExecutionPostProcessEnd = this.taskLogHandler.isNowExecutionPostProcessEnd(task,taskExecutionList);
        boolean isOldExecutionPostProcessEnd = this.taskLogHandler.isOldExecutionPostProcessEnd(task,taskExecutionList);
        log.debug(">> isNowExecutionPostProcessEnd:{},isOldExecutionPostProcessEnd:{}",isNowExecutionPostProcessEnd,isOldExecutionPostProcessEnd);
        
        if(paramExtensionConfigCheckYn.equals("N")) {
	        if(task.getTaskConfig().getJobParametersApiUrlEnableYn()!=null && task.getTaskConfig().getJobParametersApiUrlEnableYn().equals("Y")) {paramExtensionConfigCheckYn="Y";}
	        if(task.getTaskConfig().getCallbackApiUrlEnableYn()!=null && task.getTaskConfig().getCallbackApiUrlEnableYn().equals("Y")) {paramExtensionConfigCheckYn="Y";}
        }
        
        String autoReflashStep = "";
        boolean isAutoReflash = false;
        if(!isAutoReflash && task.isRunning()) {isAutoReflash = true;autoReflashStep = "run";}
        if(!isAutoReflash && isNowExecutionPostProcessEnd == false) {isAutoReflash = true;autoReflashStep = "log";}     
        if(!isAutoReflash && isOldExecutionPostProcessEnd == false) {isAutoReflash = true;autoReflashStep = "lazy";}        
        if(!isAutoReflash) {
            BaseUtil.getSessionUtil().removeAttribute("taskId");
            BaseUtil.getSessionUtil().removeAttribute("activeTab");
            BaseUtil.getSessionUtil().removeAttribute("detailViewCheckYn");
            BaseUtil.getSessionUtil().removeAttribute("paramExtensionConfigCheckYn");
            BaseUtil.getSessionUtil().removeAttribute("executionEnvViewCheckYn");
            BaseUtil.getSessionUtil().removeAttribute("executionListRownum");
            BaseUtil.getSessionUtil().removeAttribute("executionListFilterErrorYn");
            BaseUtil.getSessionUtil().removeAttribute("executionListFilterInterruptYn");
        } else {
            BaseUtil.getSessionUtil().setAttribute("taskId",taskId);
            BaseUtil.getSessionUtil().setAttribute("activeTab",activeTab);
            BaseUtil.getSessionUtil().setAttribute("detailViewCheckYn",detailViewCheckYn);
            BaseUtil.getSessionUtil().setAttribute("paramExtensionConfigCheckYn",paramExtensionConfigCheckYn);
            BaseUtil.getSessionUtil().setAttribute("executionEnvViewCheckYn",executionEnvViewCheckYn);
            BaseUtil.getSessionUtil().setAttribute("executionListRownum",executionListRownum);
            BaseUtil.getSessionUtil().setAttribute("executionListFilterErrorYn",executionListFilterErrorYn);
            BaseUtil.getSessionUtil().setAttribute("executionListFilterInterruptYn",executionListFilterInterruptYn);
        }
        
        Map<String,String> defaultConstant = new HashMap<>();
        defaultConstant.put("logExtractMode", TaskLogContext.LOG_EXTRACT_MODE_BLOCK);
        defaultConstant.put("logBlockSize", String.valueOf(TaskLogContext.LOG_PRINT_BLOCK_SIZE));
        defaultConstant.put("logChunkSize", String.valueOf(TaskLogContext.LOG_PRINT_CHUNK_SIZE));
        defaultConstant.put("logLineSize", String.valueOf(TaskLogContext.LOG_PRINT_LINE_SIZE));
                
        Map<String,Object> schedulerInfo = new HashMap<>();
        schedulerInfo.put("serverIpPort", BaseUtil.getSystemUtil().getServerIpPort());
        schedulerInfo.put("serverInstanceCode", BaseUtil.getSystemUtil().getServerInstanceCode());
        schedulerInfo.put("schedulerContextStartTime", schedulerContext.getSchedulerContextStartTime());
        schedulerInfo.put("schedulerStatus", schedulerContext.getSchedulerStatus());
        schedulerInfo.put("schedulerStartTime", schedulerContext.getSchedulerStartTime());
        schedulerInfo.put("schedulerStopTime", schedulerContext.getSchedulerStopTime());        
        schedulerInfo.put("userInfo", this.authService.getUserInfo());
        schedulerInfo.put("defaultConstant", defaultConstant);
        
        model.addAllAttributes(support.getSystemInfo());

        model.addAttribute("metaType",request.getParameter("meta"));
        model.addAttribute("loginMode",BaseUtil.getSessionUtil().getAttribute(AuthService.SCHEDULER_LOGIN_MODE)); 
        model.addAttribute("adminYn",adminYn);
        
        model.addAttribute("actionPage","detail");
        model.addAttribute("activeTab",activeTab);
        model.addAttribute("detailViewCheckYn",detailViewCheckYn);
        model.addAttribute("executionEnvViewCheckYn",executionEnvViewCheckYn);
        model.addAttribute("executionListRownum",executionListRownum);        
        model.addAttribute("executionListFilterErrorYn",executionListFilterErrorYn);
        model.addAttribute("executionListFilterInterruptYn",executionListFilterInterruptYn);
                
        model.addAttribute("task",task);     
        model.addAttribute("schedulerStatus",schedulerContext.getSchedulerStatus());
        model.addAttribute("taskChangeStamp",task.getNewChangeStamp());
        model.addAttribute("taskExecutionList",taskExecutionList);

        model.addAttribute("schedulerInfo",BaseUtil.getJsonUtil().toJson(schedulerInfo));
        
        model.addAttribute("configEnableProps",this.taskLogHandler.getConfigEnableProps());
        model.addAttribute("globalLogPolicyProps",this.taskLogHandler.getGlobalLogPolicyProps());

        model.addAttribute("logHistoryShrinkYn",this.logHistoryShrinkYn);
        model.addAttribute("paramExtensionConfigCheckYn",paramExtensionConfigCheckYn);
        model.addAttribute("supportOutterTaskParamConfigYn",supportOutterTaskParamConfigYn);

        model.addAttribute("autoReflashYn",(isAutoReflash?"Y":"N"));
        model.addAttribute("autoReflashStep",autoReflashStep);
        model.addAttribute("autoReflashTaskExecutionInfo",(isAutoReflash?autoReflashTaskExecutionInfo:null));

        model.addAttribute("loggedExecutionNo",isNowExecutionPostProcessEnd?task.getExecutionNo():null); 
        model.addAttribute("lazyPostProcessEndYn",isOldExecutionPostProcessEnd?"Y":"N"); 

        return viewPage;
    }   

    @RequestMapping(value = {"/{schedulerRequestPath}/config/schedule"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskSchedule(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
        
        log.debug(">> detail:task.schedule [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
        
        String taskChangeStamp  = request.getParameter("taskChangeStamp");    
        String taskId = this.support.getParamAddOnSession("taskId",request);
        String activeTab = this.support.getParamAddOnSession("activeTab",request);

        String scheduleType = request.getParameter("scheduleType");    
        String scheduleValue = request.getParameter("scheduleValue");
		log.debug(">> scheduleType:{}",scheduleType);
		log.debug(">> scheduleValue:{}",scheduleValue);
		
        this.support.setParamToSessionOnDetail(request);
        
        SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();

        String message = "";
        boolean isEnable = true;
        if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
        if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
        if(isEnable && schedulerContext.isSchedulerStart()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.051");}
        if(isEnable && (scheduleType.equals("cronExpression") && !CronExpression.isValidExpression(scheduleValue))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.052",scheduleValue);}
        if(isEnable && (scheduleType.equals("fixedDelay") && !this.support.isNumericScheduleValue(scheduleValue))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.053",scheduleValue);}
        if(isEnable && (scheduleType.equals("fixedRate") && !this.support.isNumericScheduleValue(scheduleValue))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.054",scheduleValue);}
        if(!isEnable) {
            BaseUtil.getAlertUtil().setAlertMessage(message); 
            return "redirect:../detail"; 
        }

        TaskPack.Task task = schedulerContext.getTask(taskId);
        task.setCronExpression(null);
        task.setFixedDelay(null);
        task.setFixedRate(null);
        if(scheduleType.equals("cronExpression")) {task.setCronExpression(scheduleValue);}
        if(scheduleType.equals("fixedDelay")) {task.setFixedDelay(scheduleValue);}
        if(scheduleType.equals("fixedRate")) {task.setFixedRate(scheduleValue);}
        
        TaskConfig taskConfig = task.getTaskConfig(); 
        taskConfig.setScheduleType(scheduleType);
        taskConfig.setScheduleValue(scheduleValue);
        
        if(task.getTaskConfig().getSaveImmediatelyYn().equals("Y")) {
     	   task.saveTaskConfig(this.support.getConfigKeySet("scheduleType,scheduleValue")); //설정정보를 설정파일에 저장
        }
        
        return "redirect:../detail"; 
    }    

    @RequestMapping(value = {"/{schedulerRequestPath}/resetCount"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskResetCount(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
       if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

       log.debug(">> detail:task.resetCount [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
       String taskChangeStamp  = request.getParameter("taskChangeStamp");    
       String taskId = this.support.getParamAddOnSession("taskId",request);
       String activeTab = this.support.getParamAddOnSession("activeTab",request);
       
       this.support.setParamToSessionOnDetail(request);
       
       SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();

       String message = "";
       boolean isEnable = true;
       if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
       if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
       if(!isEnable) {
           BaseUtil.getAlertUtil().setAlertMessage(message); 
           return "redirect:detail"; 
       }
       
       TaskPack.Task task = schedulerContext.getTask(taskId);
       task.resetCount();
       
       return "redirect:detail"; 
	}
    
    @RequestMapping(value = {"/{schedulerRequestPath}/config/init"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskConfigInit(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
       if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

       log.debug(">> detail:task.config.init [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
       
       String taskChangeStamp  = request.getParameter("taskChangeStamp");    
       String taskId = this.support.getParamAddOnSession("taskId",request);
       String activeTab = this.support.getParamAddOnSession("activeTab",request);

       this.support.setParamToSessionOnDetail(request);
       
       SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();

       String message = "";
       boolean isEnable = true;
       if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
       if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
       if(!isEnable) {
           BaseUtil.getAlertUtil().setAlertMessage(message); 
           return "redirect:../detail"; 
       }
       
       // configFile 재구성
       TaskPack.Task task = schedulerContext.getTask(taskId);
       task.initTaskConfig();
       
       return "redirect:../detail"; 
	}
        
    @RequestMapping(value = {"/{schedulerRequestPath}/config/load"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskConfigLoad(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
       if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

       log.debug(">> detail:task.config.load [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
       
       String taskChangeStamp  = request.getParameter("taskChangeStamp");    
       String taskId = this.support.getParamAddOnSession("taskId",request);
       String activeTab = this.support.getParamAddOnSession("activeTab",request);
       String configTarget = this.support.getParamAddOnSession("configTarget",request);
       
       this.support.setParamToSessionOnDetail(request);
       
       SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();

       String message = "";
       boolean isEnable = true;
       if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (configTarget == null || configTarget.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
       if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
       if(!isEnable) {
           BaseUtil.getAlertUtil().setAlertMessage(message); 
           return "redirect:../detail"; 
       }
       
       // configFile에서 configTarget(콤마로 분리한 설정이름)에 해당하는 값을 읽어 configData에 적용
       TaskPack.Task task = schedulerContext.getTask(taskId);
       task.loadTaskConfig(this.support.getConfigKeySet(configTarget));
       
       return "redirect:../detail"; 
	}

    @RequestMapping(value = {"/{schedulerRequestPath}/config/save"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskConfigSave(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
       if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

       log.debug(">> detail:task.config.save [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
       
       String taskChangeStamp  = request.getParameter("taskChangeStamp");    
       String taskId = this.support.getParamAddOnSession("taskId",request);
       String activeTab = this.support.getParamAddOnSession("activeTab",request);
       String configTarget = this.support.getParamAddOnSession("configTarget",request);
       
       this.support.setParamToSessionOnDetail(request);

       SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();

       String message = "";
       boolean isEnable = true;
       if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (configTarget == null || configTarget.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}
       if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
       if(!isEnable) {
           BaseUtil.getAlertUtil().setAlertMessage(message); 
           return "redirect:../detail"; 
       }

       // configData객체로 부터 configTarget(콤마로 분리한 설정이름)에 해당하는 값을 configFile에 적용
       TaskPack.Task task = schedulerContext.getTask(taskId);
       task.saveTaskConfig(this.support.getConfigKeySet(configTarget));
       
       return "redirect:../detail"; 
    }

    @RequestMapping(value = {"/{schedulerRequestPath}/config/bizProps"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskConfigBizProps(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
       if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
       
       log.debug(">> detail:task.config.bizProps [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
       
       String taskChangeStamp  = request.getParameter("taskChangeStamp"); 
       String taskId = this.support.getParamAddOnSession("taskId",request);
       String activeTab = this.support.getParamAddOnSession("activeTab",request);
       String propertyKey = request.getParameter("propertyKey");
       String propertyValue = request.getParameter("propertyValue");

       this.support.setParamToSessionOnDetail(request);
       
       SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
       TaskPack.Task task = schedulerContext.getTask(taskId);
       
       String message = "";
       boolean isEnable = true;
       if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (propertyKey == null || propertyKey.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (propertyValue == null || propertyValue.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");} 
       if(isEnable && schedulerContext.getTask(taskId).isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}       
       if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
       if(!isEnable) {
           BaseUtil.getAlertUtil().setAlertMessage(message); 
           return "redirect:../detail"; 
       }
       
       // configData객체 bizProps에 적용
       task.getTaskConfig().updateBizProps(propertyKey,propertyValue); 

       // configData객체 bizProps를 읽어 configFile에 저장
       if(task.getTaskConfig().getSaveImmediatelyYn().equals("Y")) {
     	   task.saveTaskConfig(this.support.getConfigKeySet("bizProps")); //설정정보를 설정파일에 저장
        }
       
       return "redirect:../detail";
    }

    @RequestMapping(value = {"/{schedulerRequestPath}/config/logEnable"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskConfigLogEnable(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
       if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

       log.debug(">> detail:task.config.logEnable [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));

       String taskChangeStamp  = request.getParameter("taskChangeStamp");          
       String taskId = this.support.getParamAddOnSession("taskId",request);
       String activeTab = this.support.getParamAddOnSession("activeTab",request);
       String logEnableCheck = request.getParameter("logEnableCheck");

       this.support.setParamToSessionOnDetail(request);
       
       SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
       TaskPack.Task task = schedulerContext.getTask(taskId);

       String message = "";
       boolean isEnable = true;
       if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}   
       if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}      
       if(isEnable && (logEnableCheck == null || logEnableCheck.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");} 
       if(isEnable && schedulerContext.getTask(taskId).isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}       
       if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
       if(!isEnable) {
           BaseUtil.getAlertUtil().setAlertMessage(message); 
           return "redirect:../detail"; 
       }
 
       task.getTaskConfig().setLogEnableYn(logEnableCheck.equals("true")?"Y":"N");
       if(task.getTaskConfig().getSaveImmediatelyYn().equals("Y")) {
    	   task.saveTaskConfig(this.support.getConfigKeySet("logEnableYn")); //설정정보를 설정파일에 저장
       }
       
       return "redirect:../detail"; 
    }

    @RequestMapping(value = {"/{schedulerRequestPath}/config/logFileLevel"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskConfigLogFileLevel(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
		if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
		
	       log.debug(">> detail:task.config.logFileLevel [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));

	       String taskChangeStamp  = request.getParameter("taskChangeStamp");         
	       String taskId = this.support.getParamAddOnSession("taskId",request);
	       String activeTab = this.support.getParamAddOnSession("activeTab",request);
	       
	       String logType = request.getParameter("logType");
	       String logLevel = request.getParameter("logLevel");
	       String logFilePathPattern = request.getParameter("logFilePathPattern");
	       
	       this.support.setParamToSessionOnDetail(request);
	       
	       SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
	       TaskPack.Task task = schedulerContext.getTask(taskId);
	       TaskConfig taskConfig = task.getTaskConfig();  

	       String message = "";
	       boolean isEnable = true;
	       if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
	       if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
	       if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}  
	       if(isEnable && (logType == null || logType.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}  
	       if(isEnable && (logFilePathPattern == null || logFilePathPattern.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
	       if(isEnable && (logLevel == null || logLevel.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");} 
	       if(isEnable) { //배타적인체크(실행로그파일패턴과 오류로그파일패턴은 반드시 달라야함)
	    	   String exclusiveCheckValue = null;
	    	   if(logType.equals("executionLog")) {exclusiveCheckValue = taskConfig.getExceptionLogFilePathPattern();}
	    	   if(logType.equals("exceptionLog")) {exclusiveCheckValue = taskConfig.getExecutionLogFilePathPattern();}
	    	   if(exclusiveCheckValue != null && exclusiveCheckValue.toLowerCase() == logFilePathPattern.toLowerCase()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
	       }
	       if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");} 
	       if(isEnable && schedulerContext.getTask(taskId).isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}       
	       if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
	       if(!isEnable) {
	           BaseUtil.getAlertUtil().setAlertMessage(message); 
	           return "redirect:../detail"; 
	       }
	       
	       if(logType.equals("executionLog")) {
	    	   taskConfig.setExecutionLogFilePathPattern(logFilePathPattern);
	    	   taskConfig.setExecutionLogLevel(logLevel);
	    	   if(task.getTaskConfig().getSaveImmediatelyYn().equals("Y")) {
	    		   task.saveTaskConfig(this.support.getConfigKeySet("executionLogLevel,executionLogFilePathPattern")); //설정정보를 설정파일에 저장
	    	   }
	       }
	       if(logType.equals("exceptionLog")) {
	    	   taskConfig.setExceptionLogFilePathPattern(logFilePathPattern);
	    	   taskConfig.setExceptionLogLevel(logLevel);
	    	   if(task.getTaskConfig().getSaveImmediatelyYn().equals("Y")) {
	    		   task.saveTaskConfig(this.support.getConfigKeySet("exceptionLogLevel,exceptionLogFilePathPattern")); //설정정보를 설정파일에 저장
	    	   }
	       }
	       
	       return "redirect:../detail"; 
	}
	
    @RequestMapping(value = {"/{schedulerRequestPath}/config/logExceptEnable"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskConfigLogExceptEnable(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
       if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

       log.debug(">> detail:task.config.logExceptEnable [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
       
       String taskChangeStamp  = request.getParameter("taskChangeStamp");         
       String taskId = this.support.getParamAddOnSession("taskId",request);
       String activeTab = this.support.getParamAddOnSession("activeTab",request);
       String logExceptEnableYn = request.getParameter("logExceptEnableYn");

       this.support.setParamToSessionOnDetail(request);
       
       SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
       TaskPack.Task task = schedulerContext.getTask(taskId);
       
       String message = "";
       boolean isEnable = true;
       if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}  
       if(isEnable && (logExceptEnableYn == null || logExceptEnableYn.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}  
       if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");} 
       if(isEnable && schedulerContext.getTask(taskId).isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}       
       if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
       if(!isEnable) {
           BaseUtil.getAlertUtil().setAlertMessage(message); 
           return "redirect:../detail"; 
       }

       task.getTaskConfig().setLogExceptEnableYn(logExceptEnableYn);
       if(task.getTaskConfig().getSaveImmediatelyYn().equals("Y")) {
    	   task.saveTaskConfig(this.support.getConfigKeySet("logExceptEnableYn")); //설정정보를 설정파일에 저장
       }
       
       return "redirect:../detail"; 
    }
   
    @RequestMapping(value = {"/{schedulerRequestPath}/config/logExceptPattern"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskConfigLogExceptPattern(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
       if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
              
       log.debug(">> detail:task.config.logExceptPattern [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
       
       String taskChangeStamp  = request.getParameter("taskChangeStamp");   
       String taskId = this.support.getParamAddOnSession("taskId",request);
       String activeTab = this.support.getParamAddOnSession("activeTab",request);
       String logExceptLoggerPattern = request.getParameter("logExceptLoggerPattern");
       String logExceptCommandType = request.getParameter("logExceptCommandType");

       this.support.setParamToSessionOnDetail(request);
       
       SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
       TaskPack.Task task = schedulerContext.getTask(taskId);
       
       String message = "";
       boolean isEnable = true;
       if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false;}
       if(isEnable && (logExceptCommandType == null || logExceptCommandType.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (logExceptLoggerPattern == null || logExceptLoggerPattern.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (!logExceptCommandType.equals("add") && !isEnable && logExceptCommandType.equals("remove"))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");} 
       if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");} 
       if(isEnable && schedulerContext.getTask(taskId).isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}       
       if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
       if(!isEnable) {
           BaseUtil.getAlertUtil().setAlertMessage(message); 
           return "redirect:../detail"; 
       }

       if(logExceptCommandType.equals("add")) {task.getTaskConfig().addLogExceptPattern(logExceptLoggerPattern);}
       if(logExceptCommandType.equals("remove")) {task.getTaskConfig().removeLogExceptPattern(logExceptLoggerPattern);}
       if(task.getTaskConfig().getSaveImmediatelyYn().equals("Y")) {
    	   task.saveTaskConfig(this.support.getConfigKeySet("logExceptPatternList")); //설정정보를 설정파일에 저장
       }
       return "redirect:../detail";  
	}

    @RequestMapping(value = {"/{schedulerRequestPath}/config/logPolicyProps"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskConfigLogPolicyProps(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
       if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

       log.debug(">> detail:task.config.logPolicyProps [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));

       String taskChangeStamp  = request.getParameter("taskChangeStamp");          
       String taskId = this.support.getParamAddOnSession("taskId",request);
       String activeTab = this.support.getParamAddOnSession("activeTab",request);
       String propertyKey = request.getParameter("propertyKey");
       String propertyValue = request.getParameter("propertyValue");

       this.support.setParamToSessionOnDetail(request);
       
       SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
       TaskPack.Task task = schedulerContext.getTask(taskId);
       
       String message = "";
       boolean isEnable = true;
       if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}   
       if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}      
       if(isEnable && (propertyKey == null || propertyKey.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (propertyValue == null || propertyValue.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}   
       if(isEnable && (propertyKey.equals("logHistoryShrinkBaseDays") && !this.support.isNumeric(propertyValue))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (propertyKey.equals("logHistoryShrinkBaseRows") && !this.support.isNumeric(propertyValue))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}              
       if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");} 
       if(isEnable && schedulerContext.getTask(taskId).isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}       
       if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
       if(!isEnable) {
           BaseUtil.getAlertUtil().setAlertMessage(message); 
           return "redirect:../detail"; 
       }
       
       if(isEnable && propertyKey.equals("logHistoryShrinkBaseDays") && Long.parseLong(propertyValue)<0) {isEnable = false;}
       if(isEnable && propertyKey.equals("logHistoryShrinkBaseRows") && Long.parseLong(propertyValue)<0) {isEnable = false;}
       if(!isEnable) {isEnable = true;propertyValue = "";}
       
       Properties configEnableProps = this.taskLogHandler.getConfigEnableProps();
       if(configEnableProps.containsKey(propertyKey)) {
    	   String configEnable = configEnableProps.getProperty(propertyKey);
    	   if(configEnable != null && configEnable.equals("N")) {
    		   propertyValue = this.taskLogHandler.getGlobalLogPolicyProps().getProperty(propertyKey);
    	   }
       }
       
       TaskConfig taskConfig = task.getTaskConfig();  
       taskConfig.updateLogPolicyProps(propertyKey,propertyValue);
       
       if(propertyKey.equals("logStampPrintYn") && propertyValue.equals(TaskLogContext.LOG_STAMP_PRINT_Y)) {
           if(taskConfig.getLogPolicyProps().getProperty("logStampStartPrintYn") == null) {taskConfig.updateLogPolicyProps("logStampStartPrintYn",this.taskLogHandler.getGlobalLogPolicyProps().getProperty("logStampStartPrintYn"));}
           if(taskConfig.getLogPolicyProps().getProperty("logStampExceptionPrintYn") == null) {taskConfig.updateLogPolicyProps("logStampExceptionPrintYn",this.taskLogHandler.getGlobalLogPolicyProps().getProperty("logStampExceptionPrintYn"));}
           if(taskConfig.getLogPolicyProps().getProperty("logStampAroundPrintYn") == null) {taskConfig.updateLogPolicyProps("logStampAroundPrintYn",this.taskLogHandler.getGlobalLogPolicyProps().getProperty("logStampAroundPrintYn"));}
           if(taskConfig.getLogPolicyProps().getProperty("logStampEndPrintYn") == null) {taskConfig.updateLogPolicyProps("logStampEndPrintYn",this.taskLogHandler.getGlobalLogPolicyProps().getProperty("logStampEndPrintYn"));}
       }
       
       if(propertyKey.equals("logHistoryShrinkYn") && propertyValue.equals(TaskLogContext.LOG_HISTORY_SHRINK_Y)) {
           if(taskConfig.getLogPolicyProps().getProperty("logHistoryShrinkBaseDays") == null) {taskConfig.updateLogPolicyProps("logHistoryShrinkBaseDays",this.taskLogHandler.getGlobalLogPolicyProps().getProperty("logHistoryShrinkBaseDays"));}
           if(taskConfig.getLogPolicyProps().getProperty("logHistoryShrinkBaseRows") == null) {taskConfig.updateLogPolicyProps("logHistoryShrinkBaseRows",this.taskLogHandler.getGlobalLogPolicyProps().getProperty("logHistoryShrinkBaseRows"));}
       }
       
       if(task.getTaskConfig().getSaveImmediatelyYn().equals("Y")) {
    	   task.saveTaskConfig(this.support.getConfigKeySet("logPolicyProps")); //설정정보를 설정파일에 저장
       }
       
       log.debug("LogPolicyProps: {}",taskConfig.getLogPolicyProps());
       
       return "redirect:../detail"; 
	}

    @RequestMapping(value = {"/{schedulerRequestPath}/config/outterOption"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskConfigOutterOption(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

        log.debug(">> detail:task.config.outterOption [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
        
        String taskChangeStamp  = request.getParameter("taskChangeStamp"); 
        String taskId = this.support.getParamAddOnSession("taskId",request);
        String activeTab = this.support.getParamAddOnSession("activeTab",request);
        
        String outterOptionType = request.getParameter("outterOptionType");

        String jobParametersApiUrlEnableYn = request.getParameter("jobParametersApiUrlEnableYn");
        String jobParametersApiUrlOverlapYn = request.getParameter("jobParametersApiUrlOverlapYn");
        String jobParametersApiUrl = request.getParameter("jobParametersApiUrl");
        
        String callbackApiUrlEnableYn = request.getParameter("callbackApiUrlEnableYn");
        String callbackApiUrl = request.getParameter("callbackApiUrl");
        
        String jvmMemoryEnableYn = request.getParameter("jvmMemoryEnableYn");
        String jvmMemory = request.getParameter("jvmMemory");
        
        SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
        TaskPack.Task task = schedulerContext.getTask(taskId);
        
        String message = "";
        boolean isEnable = true;
        if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}        
        if(outterOptionType.equals("optionType1")) {
        	if(isEnable && (jobParametersApiUrlEnableYn == null || jobParametersApiUrlEnableYn.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        	if(isEnable && (jobParametersApiUrlOverlapYn == null || jobParametersApiUrlOverlapYn.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        	if(isEnable && (jobParametersApiUrlEnableYn.equals("Y") || jobParametersApiUrlOverlapYn.equals("Y")) && (jobParametersApiUrl == null || jobParametersApiUrl.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        }
        if(outterOptionType.equals("optionType2")) {
        	if(isEnable && (callbackApiUrlEnableYn == null || callbackApiUrlEnableYn.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        	if(isEnable && (callbackApiUrlEnableYn.equals("Y")) && (callbackApiUrl == null || callbackApiUrl.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        }
        if(outterOptionType.equals("optionType3")) {
        	if(isEnable && (jvmMemoryEnableYn == null || jvmMemoryEnableYn.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}        	
        	if(isEnable && (jvmMemoryEnableYn.equals("Y")) && jvmMemoryEnableYn.equals("Y") && (jvmMemory == null || jvmMemory.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
        }
        if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");} 
        if(isEnable && schedulerContext.getTask(taskId).isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}       
        if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
        if(!isEnable) {
            BaseUtil.getAlertUtil().setAlertMessage(message); 
            return "redirect:../detail"; 
        }

        if(outterOptionType.equals("optionType1")) {
        	task.getTaskConfig().setJobParametersApiUrlEnableYn(jobParametersApiUrlEnableYn);
        	task.getTaskConfig().setJobParametersApiUrlOverlapYn(jobParametersApiUrlOverlapYn);
        	task.getTaskConfig().setJobParametersApiUrl(jobParametersApiUrl);
        }
        if(outterOptionType.equals("optionType2")) {
        	task.getTaskConfig().setCallbackApiUrlEnableYn(callbackApiUrlEnableYn);
        	task.getTaskConfig().setCallbackApiUrl(callbackApiUrl);
        }
        if(outterOptionType.equals("optionType3")) {
        	task.getTaskConfig().setJvmMemoryEnableYn(jvmMemoryEnableYn);
        	task.getTaskConfig().setJvmMemory(jvmMemory);
        }

        if(task.getTaskConfig().getSaveImmediatelyYn().equals("Y")) {        	
      	   String configTarget="";
           if(outterOptionType.equals("optionType1")) {configTarget="jobParametersApiUrl,jobParametersApiUrlOverlapYn,jobParametersApiUrlEnableYn";}
           if(outterOptionType.equals("optionType2")) {configTarget="callbackApiUrl,callbackApiUrlEnableYn";}
           if(outterOptionType.equals("optionType3")) {configTarget="jvmMemory,jvmMemoryEnableYn";}           
      	   task.saveTaskConfig(this.support.getConfigKeySet(configTarget)); //설정정보를 설정파일에 저장      
        }
        
        BaseUtil.getSessionUtil().setAttribute("paramExtensionConfigCheckYn","Y");
        
        return "redirect:../detail";
    }

    @RequestMapping(value = {"/{schedulerRequestPath}/config/scheduleTermEnable"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskConfigScheduleTermEnable(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
       if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

       log.debug(">> detail:task.config.scheduleTermEnable [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
       
       String taskChangeStamp  = request.getParameter("taskChangeStamp");         
       String taskId = this.support.getParamAddOnSession("taskId",request);
       String activeTab = this.support.getParamAddOnSession("activeTab",request);
       String scheduleTermEnableYn = request.getParameter("scheduleTermEnableYn");

       this.support.setParamToSessionOnDetail(request);
       
       SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
       TaskPack.Task task = schedulerContext.getTask(taskId);
       
       String message = "";
       boolean isEnable = true;
       if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}  
       if(isEnable && (scheduleTermEnableYn == null || scheduleTermEnableYn.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}  
       if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");} 
       if(isEnable && schedulerContext.getTask(taskId).isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}       
       if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
       if(!isEnable) {
           BaseUtil.getAlertUtil().setAlertMessage(message); 
           return "redirect:../detail"; 
       }

       task.getTaskConfig().setScheduleTermEnableYn(scheduleTermEnableYn);
       if(task.getTaskConfig().getSaveImmediatelyYn().equals("Y")) {
    	   task.saveTaskConfig(this.support.getConfigKeySet("scheduleTermEnableYn")); //설정정보를 설정파일에 저장
       }
       
       return "redirect:../detail"; 
    }

    @RequestMapping(value = {"/{schedulerRequestPath}/config/scheduleTerm"}, method = {RequestMethod.GET,RequestMethod.POST})
	public String taskConfigScheduleTerm(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
       if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}

       log.debug(">> detail:task.config.scheduleTerm [taskChangeStamp:{}]",request.getParameter("taskChangeStamp"));
       
       String taskChangeStamp  = request.getParameter("taskChangeStamp");   
       String taskId = this.support.getParamAddOnSession("taskId",request);
       String activeTab = this.support.getParamAddOnSession("activeTab",request);
       String scheduleTermCommandType = request.getParameter("scheduleTermCommandType");
       String scheduleTermValue = request.getParameter("scheduleTermValue");
       
       String scheduleTermStart = scheduleTermValue.indexOf("|")>-1?scheduleTermValue.split("\\|")[0]:null;
       String scheduleTermEnd = scheduleTermValue.indexOf("|")>-1?scheduleTermValue.split("\\|")[1]:null;
       
       this.support.setParamToSessionOnDetail(request);
       
       SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
       TaskPack.Task task = schedulerContext.getTask(taskId);
       
       String message = "";
       boolean isEnable = true;
       if(isEnable && taskChangeStamp == null) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (taskId == null || taskId.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (activeTab == null || activeTab.equals(""))) {isEnable = false;}
       if(isEnable && (scheduleTermCommandType == null || scheduleTermCommandType.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (!scheduleTermCommandType.equals("add") && !isEnable && scheduleTermCommandType.equals("remove"))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");} 
       if(isEnable && (scheduleTermStart == null || scheduleTermStart.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && (scheduleTermEnd == null || scheduleTermEnd.equals(""))) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.001");}
       if(isEnable && BaseUtil.getTimeUtil().getDifference(scheduleTermStart,"yyyy-MM-dd",scheduleTermEnd,"yyyy-MM-dd") < 0) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.055");}       
       if(isEnable && !taskChangeStamp.equals(schedulerContext.getTask(taskId).getTaskChangeStamp())) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.002");}        
       if(isEnable && schedulerContext.getTask(taskId).isBlock()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.041");}       
       if(isEnable && schedulerContext.getTask(taskId).isRunning()) {isEnable = false; message = BaseUtil.getMessageUtil().getMessage("main.message.alert.042");}
       if(!isEnable) {
           BaseUtil.getAlertUtil().setAlertMessage(message); 
           return "redirect:../detail"; 
       }
       
       if(scheduleTermCommandType.equals("add")) {task.getTaskConfig().addScheduleTerm(scheduleTermStart, scheduleTermEnd);}
       if(scheduleTermCommandType.equals("remove")) {task.getTaskConfig().removeScheduleTerm(scheduleTermStart, scheduleTermEnd);}
       if(task.getTaskConfig().getSaveImmediatelyYn().equals("Y")) {
    	   task.saveTaskConfig(this.support.getConfigKeySet("scheduleTermList,scheduleTermDesc")); //설정정보를 설정파일에 저장
       }
       return "redirect:../detail";  
	}

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @RequestMapping(value = {"/{schedulerRequestPath}/meta/envInfo"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String metaEnvInfo(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
    
        log.debug(">> meta.envInfo");
        
        String executionNo = request.getParameter("executionNo");
        if(executionNo == null || executionNo.equals("")) {return null;}
        log.debug("executionNo: {}",executionNo);
        
        model.addAttribute("metaType","envInfo");
        model.addAttribute("executionNo",executionNo);
        
        return "console.meta";
    }   
	
    @RequestMapping(value = {"/{schedulerRequestPath}/meta/logInfo"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String metaLogInfo(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
    
        log.debug(">> meta.logInfo");
        
        String executionNo = request.getParameter("executionNo");
        if(executionNo == null || executionNo.equals("")) {return null;}
        log.debug("executionNo: {}",executionNo);
        
        String executionPostProcessInfo = this.support.getExecutionPostProcessLookUpSession(executionNo);
        if(executionPostProcessInfo == null || executionPostProcessInfo.equals("")) {return null;}
        //log.debug("executionPostProcessInfo: {}",executionPostProcessInfo);

        String logType = request.getParameter("logType");
        if(logType == null || logType.equals("")) {return null;}
        //log.debug("logType: {}",logType);

        boolean isEnable = false;
        if(!isEnable && logType.startsWith("executionLog")) {isEnable = true;}
        if(!isEnable && logType.startsWith("exceptionLog")) {isEnable = true;}
        if(!isEnable && logType.startsWith("metaLog")) {isEnable = true;}
        if(!isEnable) {return null;}

        String logSeq = request.getParameter("logSeq");
        if(logSeq == null || logSeq.equals("")) {return null;}

        String logExtractMode = request.getParameter("logExtractMode");
        if(logExtractMode == null || logExtractMode.equals("")) {logExtractMode = TaskLogContext.LOG_EXTRACT_MODE_BLOCK;}

        String logExtractSize = request.getParameter("logExtractSize");
        if(logExtractSize == null || logExtractSize.equals("")) {logExtractSize = "-1";}
        log.debug("logType: {}, logExtractMode: {}, logExtractSize: {}, logSeq: {}", logType, logExtractMode, logExtractSize, logSeq);
                
        TaskExecutionVo vo = new TaskExecutionVo(executionNo);
        vo.setExecutionPostProcess(executionPostProcessInfo);
        vo.setLogType(logType);
        vo.setLogSeq(Integer.parseInt(logSeq));
        vo.setLogExtractMode(logExtractMode);
        vo.setLogExtractSize(Integer.parseInt(logExtractSize));
        Map<String,Object> logInfo = this.taskLogHandler.getTaskLogContext().extractTaskLog(vo);

        model.addAttribute("metaType","logInfo");
        model.addAttribute("executionNo",executionNo);
        model.addAttribute("logType",logType);
        model.addAttribute("logSeq",logSeq);
        model.addAttribute("logInfo",logInfo);
        model.addAttribute("logPrintEnableYn",(logInfo != null && logInfo.get("logData") != null)?"Y":"N");
        model.addAttribute("logPrintBlockSize",String.valueOf(TaskLogContext.LOG_PRINT_BLOCK_SIZE));

        return "console.meta";
    }   

    @RequestMapping(value = {"/{schedulerRequestPath}/meta/logInfoDownload"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String metaLogInfoDownload(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
        
        log.debug(">> meta.logInfoDownload");

        String executionNo = request.getParameter("executionNo");
        if(executionNo == null || executionNo.equals("")) {return null;}

        String logType = request.getParameter("logType");
        if(logType == null || logType.equals("")) {return null;}
        
        boolean isEnable = false;
        if(!isEnable && logType.startsWith("executionLog")) {isEnable = true;}
        if(!isEnable && logType.startsWith("exceptionLog")) {isEnable = true;}
        if(!isEnable && logType.startsWith("metaLog")) {isEnable = true;}
        if(!isEnable) {return null;}
        
        request.setAttribute("executionNo",executionNo);
        request.setAttribute("logType",logType);

        return BatchConfig.TASK_LOG_FILE_DOWNLOAD;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    @RequestMapping(value = {"/{schedulerRequestPath}/guide"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String guide(@PathVariable String schedulerRequestPath,HttpServletRequest request,HttpServletResponse response,ModelMap model) throws Exception {
        if(this.mainService.isEnableRequest(schedulerRequestPath) == false) {return null;}
    
        log.debug(">> guide");
        
        String type = request.getParameter("type");
        if(!StringUtils.hasText(type)) {return null;}
         
        String mode = request.getParameter("mode");
        if(!StringUtils.hasText(mode)) {mode="default";}
        
        model.addAttribute("type",type);      
        model.addAttribute("mode",mode);      

        if(mode.equals("timetable")) {
            String taskId = request.getParameter("taskId");
            if(!StringUtils.hasText(taskId)) {return null;}

            int pageNo = 1; //default
            String _pageNo = request.getParameter("pageNo");
            if(StringUtils.hasText(_pageNo)) {pageNo=Integer.parseInt(_pageNo);}

            int pageSize = 10; //default
            String _pageSize = request.getParameter("pageSize");
            if(StringUtils.hasText(_pageSize)) {pageSize=Integer.parseInt(_pageSize);}

            SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
            TaskPack.Task task = schedulerContext.getTask(taskId);

            List<Map<String,String>> timeList = task.getCronExpressionTimeList(pageNo,pageSize);
            
            model.addAttribute("taskId",taskId);
            model.addAttribute("prevPageNo",pageNo-1);
            model.addAttribute("nextPageNo",pageNo+1);
            model.addAttribute("firstYn",pageNo==1?"Y":"N");
            model.addAttribute("lastYn",timeList.size()%pageSize!=1?"Y":"N");
            model.addAttribute("timeList",timeList);   
        }
        
        if(type.equals("install")) {
            String taskId = request.getParameter("taskId");

            SchedulerContext schedulerContext = this.mainService.getSchedulerManager().getSchedulerContext();
            TaskPack.Task task = schedulerContext.getTask(taskId);
            
            Map<String, Object> packageInfo = this.support.extractFileInfo(task.getTaskMetaObject().getEnvironment().getPackagePath());
            if(packageInfo!=null && !packageInfo.isEmpty()) {
	            packageInfo.put("comment",task.getTaskMetaObject().getEnvironment().getPackagePathComment());
	        	model.addAttribute("packageInfo",packageInfo);   	
            }
            
        	Map<String, Object> repositoryInfo = this.support.extractFileInfo(task.getTaskMetaObject().getEnvironment().getRepositoryPath());
            if(repositoryInfo!=null && !repositoryInfo.isEmpty()) {
	            repositoryInfo.put("comment",task.getTaskMetaObject().getEnvironment().getRepositoryPathComment());
	            model.addAttribute("repositoryInfo",repositoryInfo);            

	        	Map<String, Object> buildInfo = this.support.extractBuildInfo(String.valueOf(repositoryInfo.get("name")));
	            model.addAttribute("buildInfo",buildInfo);
            }            
        }
        
        return "guide."+ type;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}