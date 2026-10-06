<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.alpha.base.BaseUtil"%>
<%@ page import="com.alpha.base.support.batch.SchedulerManagerPack.SchedulerManager"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="aidTag" uri="/WEB-INF/tlds/aidTagLib.tld" %>
<%
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
String systemEnvMode=BaseUtil.getPropertiesUtil().getProperty("spring.profiles.active");
String systemTitle=BaseUtil.getPropertiesUtil().getProperties().getProperty("alpha.batch.webConsole.title","BATCH");
String systemTitleHtml=BaseUtil.getPropertiesUtil().getProperties().getProperty("alpha.batch.webConsole.titleHtml","console");

if(systemEnvMode.equals("prod")) {systemEnvMode="";}
SchedulerManager schedulerManager=BaseUtil.getBean(SchedulerManager.class);
String schedulerRequestPath=request.getContextPath()+"/"+schedulerManager.getSchedulerRequestPath();

String metaType=(String)request.getAttribute("metaType");
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
%>
<c:set var="context" value="<%=request.getContextPath()%>"/>
<c:set var="schedulerRequestPath" value="<%=schedulerRequestPath%>"/>

<%
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// dummy
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
if(metaType.equals("dummy")) { %>
<html>
<head>
<link rel="icon" href="/scheduler/favicon.ico" type="image/x-icon">
</head>
<body>
</body>
</html>
<%}%>

<%
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// metaInfo
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
if(metaType.equals("envInfo")) { %>
<html>
<head>
<link rel="icon" href="/scheduler/favicon.ico" type="image/x-icon">
</head>
<body>
<div id="envInfo"></div>
<script type="text/javascript">
parent.task.envInfo("<c:out value="${executionNo}"/>",document.getElementById("envInfo"));
</script>
</body>
</html>
<%}%>

<% 
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// logInfo
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
if(metaType.equals("logInfo")) { 
	//String executionNo=(String)request.getAttribute("executionNo");
	//String logType=(String)request.getAttribute("logType");
	//String logSeq=(String)request.getAttribute("logSeq");
	String logPrintEnableYn=(String)request.getAttribute("logPrintEnableYn");
	//String logPrintBlockSize=(String)request.getAttribute("logPrintBlockSize");
%>
<html>
<head>
<link rel="icon" href="/scheduler/favicon.ico" type="image/x-icon">
<style>
.findData {color:blue;font-weight:bold;}	

</style>
<script type="text/javascript">
parent.task.logInfoLinkBottom("<c:out value="${executionNo}"/>","<c:out value="${logType}"/>",Number("<c:out value="${logSeq}"/>"),"<c:out value="${logPrintEnableYn}"/>");
</script>
</head>
<body>
<%if(logPrintEnableYn.equals("Y")) {%><pre style="font-family:courier,monospace;font-size:13px;"><span id="logData"><c:out value="${logInfo.logData}"/></span></pre><%}%>
<%if(logPrintEnableYn.equals("N")) {%><c:if test="${logSeq > 1}"><pre><font face=verdana size=-1>- end -</font></pre></c:if><c:if test="${logSeq==1}"><font face=verdana size=-1>로그없음</font></c:if><%}%>
<!-- <font face=verdana size=-1>출력 가능 한 크기를 초과 하였습니다.(limit:<fmt:formatNumber value="${logPrintBlockSize}" groupingUsed="true"/>)<br>다운로드 하세요.</font> --> 
</body>
</html>
<%}%>

<%
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// status
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
if(metaType.equals("status")) {
%>
<html>
<head>
<link rel="icon" href="/scheduler/favicon.ico" type="image/x-icon">
<script type="text/javascript">
<c:if test="${actionPage eq 'main'}">
	parent.scheduler.changeStamp("<c:out value="${schedulerChangeStamp}"/>");
	parent.scheduler.status(<c:out value="${schedulerInfo}" escapeXml="false"/>);
	<c:forEach var="task" items="${taskList}" varStatus="status">
	parent.task.status("<c:out value="${task.taskId}"/>","<c:out value="${task.enableYn}"/>","<c:out value="${task.hiddenYn}"/>","<c:out value="${task.deleteYn}"/>","<c:out value="${task.status}"/>","<c:out value="${task.countBaseTime}"/>","<fmt:formatNumber value='${task.executionCount}' groupingUsed='true'/>","<fmt:formatNumber value='${task.exceptionCount}' groupingUsed='true'/>","<fmt:formatNumber value='${task.interruptCount}' groupingUsed='true'/>");
	</c:forEach>
</c:if>
<c:if test="${actionPage eq 'detail'}">
	parent.task.changeStamp("<c:out value="${taskChangeStamp}"/>");	
	parent.task.status("<c:out value="${task.taskId}"/>","<c:out value="${task.enableYn}"/>","<c:out value="${task.hiddenYn}"/>","<c:out value="${task.deleteYn}"/>","<c:out value="${task.status}"/>","<c:out value="${task.countBaseTime}"/>","<fmt:formatNumber value='${autoReflashTaskExecutionInfo.executionTimeFlow}' groupingUsed='true'/>","<fmt:formatNumber value='${autoReflashTaskExecutionInfo.executionLogSize}' groupingUsed='true'/>");
	
	<c:if test="${lazyPostProcessEndYn eq 'N'}">
	var lazyLogInfo={};
	<c:forEach var="taskExecutionInfo" items="${taskExecutionList}" varStatus="status">
	<c:if test="${empty taskExecutionInfo.executionPostProcess}">
	lazyLogInfo["${taskExecutionInfo.executionNo}"]="<fmt:formatNumber value='${taskExecutionInfo.executionLogSize}' groupingUsed='true'/>";
	</c:if>
	</c:forEach>
	parent.task.lazyLogReflash(lazyLogInfo);
	</c:if>
	
	parent.task.executionListReflash("<c:out value="${task.taskId}"/>","<c:out value="${task.status}"/>","<c:out value="${autoReflashStep}"/>","<c:out value="${loggedExecutionNo}"/>","<c:out value="${lazyPostProcessEndYn}"/>");
</c:if>
<c:if test="${autoReflashYn eq 'Y'}">
	setTimeout(function() {window.location.reload(true);},2500);
</c:if>
</script>
</head>
<body>
<font face=verdana size=-1>actionPage:${actionPage},autoReflashYn:${autoReflashYn},autoReflashStep:${autoReflashStep},loggedExecutionNo:${loggedExecutionNo}</font>
</body>
</html>
<%}%>

<aidTag:alertMessage/>