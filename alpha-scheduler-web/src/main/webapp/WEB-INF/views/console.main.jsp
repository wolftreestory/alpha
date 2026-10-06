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

if(!systemEnvMode.equals("prod")) {systemTitle=systemTitle+" ("+systemEnvMode+")";}
if(systemEnvMode.equals("prod")) {systemEnvMode="";}
SchedulerManager schedulerManager=BaseUtil.getBean(SchedulerManager.class);
String schedulerRequestPath=request.getContextPath()+"/"+schedulerManager.getSchedulerRequestPath();
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
%>
<c:set var="context" value="<%=request.getContextPath()%>"/>
<c:set var="schedulerRequestPath" value="<%=schedulerRequestPath%>"/>
<html>
  <head>
  	<link rel="icon" href="${context}/favicon.ico" type="image/x-icon">
    <title><%=systemTitle%></title>
	<link href="${context}/css/main.css" rel="stylesheet" />
	<script src="${context}/js/main.js"></script>
	<script type="text/javascript">
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	this.mainClassBuilder=new MainClassBuilder();
	this.scheduler=new this.mainClassBuilder.SchedulerClass(this,"${schedulerRequestPath}");
	this.support=new this.mainClassBuilder.SupportClass(this,"${schedulerRequestPath}");
	this.task=new this.mainClassBuilder.TaskClass(this,"${schedulerRequestPath}");
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	</script>
  </head>
  
  <body>
  	<div class="center">
  	
	<div class="titleInfo">
	<c:if test="${loginMode ne 'innerView'}"><p><%=systemTitleHtml%> <%=systemEnvMode%></p></c:if>	
	<table>
	<tr>
	<td class="filter">	
	<input type="text" id="searchFilterText" value="${searchFilterText}" class="textType" placeholder="total:${taskListSize}" onKeyup="task.retrieveMain(this,event.key,'${searchFilterText}','searchFilterTextSpan');"><span id="searchFilterTextSpan"/>
	<script>fncResizeInput(document.getElementById('searchFilterText'));</script>
	</td>
	<td class="status"><span id="schedulerStatus"></span></td>
	</tr>
	</table>
	</div>

	<div class="taskList">
		<table>
		    <colgroup>
		    	<col width="170">
		    	<col width="260">
		    	<col>
		    	<col width="180">
		    	<col width="80">
		    	<col width="80">
		    	<col width="80">
		    	<col width="80">
		    	<col width="200">
		    </colgroup>  
			<tr>
			  <th style="text-align:center"><span id="filterType1">그룹</span></th>
			  <th style="text-align:center"><span id="filterType2">타스크</span></th>
			  <th><span id="filterType3">타스크설명</span></th>
			  <th style="text-align:center"><span id="filterType4">실행규칙</span></th>
			  <th style="text-align:center"><span id="filterType5">상태</span></th>
			  <th style="text-align:center"><span id="filterType6">실행</span></th>
			  <th style="text-align:center"><span id="filterType7">오류</span></th>
			  <th style="text-align:center"><span id="filterType8">중단</span></th>
			  <th style="text-align:center"><input type="button" value="새로고침" style="width:192px;font-size:12px" onclick="window.location.reload(true)"/></th>		    
			</tr>
		    <c:forEach var="task" items="${taskList}" varStatus="status">
		    <tr onMouseOver="javascript:fncChangeTrColor(this)">    	
		      <td style="text-align:center">
				<c:if test="${not empty task.groupCode}">${task.groupCode}&nbsp;:&nbsp;</c:if><c:if test="${not empty task.groupLabel}">${task.groupLabel}</c:if>		      
				<c:if test="${not empty task.groupCode || not empty task.groupLabel}"><br></c:if>
				<font style="font-size:14px; color:#777777">${task.group}</font>		      
		      </td>
		      <td style="text-align:center">${task.name}</td>
		      <td><div class="taskDescription"><a id="${task.taskId}"><textarea readonly disabled onmouseover="this.style.borderColor='#FFC533'" onmouseout="this.style.borderColor='#555555'">${task.description}</textarea></a></div></td>
		      <td style="text-align:center">
				<c:if test="${empty task.triggerType}">n/a</c:if><c:if test="${not empty task.triggerType}">${task.triggerType}:${task.cronExpression}${task.fixedRate}${task.fixedDelay}</c:if>
				<c:if test="${task.taskConfig.scheduleTermEnableYn eq 'Y'}"><c:set var="scheduleTermAlive" value="${task.taskConfig.scheduleTermAlive}" /><br><span title="${task.taskConfig.scheduleTermDesc}">수행기간:<c:if test="${scheduleTermAlive}"><span style="color:green;font-weight:bold;">유효</span></c:if><c:if test="${!scheduleTermAlive}"><span style="color:gray;font-weight:bold;">만료</span></c:if></span></c:if>				
		      </td>
		      <td style="text-align:center"><span id="${task.taskId}.status"></span></td>
		      <td><span id="${task.taskId}.executionCount"/></td>
		      <td><span id="${task.taskId}.exceptionCount"/></td>
		      <td><span id="${task.taskId}.interruptCount"/></td>
		      <td>
				<c:if test="${!task.block}">
				<c:if test="${task.enableYn eq 'N'}"><input type="button" style="width:60px;font-size:12px" value="활성" onclick="task.enable('${task.taskId}','${task.name}','Y');"/></c:if>
				<c:if test="${task.enableYn eq 'Y'}"><input type="button" style="width:60px;font-size:12px" value="비활성" onclick="task.enable('${task.taskId}','${task.name}','N');"/></c:if>
				<input type="button" id="${task.taskId}.command" style="width:60px;font-size:12px" value="-" onclick="task.command(this,'${task.taskId}','${task.name}');"/>
				<input type="button" id="${task.taskId}.detail" style="width:60px;font-size:12px" value="상세" onclick="task.detail('${task.taskId}','${task.name}');"/>		
				</c:if>
				<c:if test="${task.block && task.nameDupYn eq 'Y'}">타스크 중복</c:if>
				<c:if test="${task.block && task.runnableServerYn eq 'N'}">수행가능서버 없음</c:if>
		      </td>
		    </tr>
		    </c:forEach>
		    <tr>    	
		      <td colspan=9 style="border:1px solid #8899bb;text-align:left;padding:0px;color:#f0f0f0;height:2px;background-color:#8899bb;"></td>
			</tr>			      
		</table>
		
		<form id="schedulerActionForm" method="post">
			<input type="hidden" name="actionPage" value="${actionPage}"/>	
			<input type="hidden" name="schedulerChangeStamp" value="${schedulerChangeStamp}"/>
			<input type="hidden" name="taskListPageSize" value="${taskListPageSize}"/>
			<input type="hidden" name="taskListPageNo" value="${taskListPageNo}"/>
			<input type="hidden" name="hiddenFilterYn" value="${hiddenFilterYn}">
			<input type="hidden" name="searchFilterArea" value="${searchFilterArea}"/>
			<input type="hidden" name="searchFilterText" value="${searchFilterText}"/>
			<input type="hidden" name="authKeyListYn">
			<input type="hidden" name="taskId">
			<input type="hidden" name="taskName">
			<input type="hidden" name="activeTab">			
			<input type="hidden" name="enableYn">
			<input type="hidden" name="allEnableYn">
		</form>		
	</div>

	<!-- navigator button -->
	<div class="taskListControl">
		<table>
	    <colgroup>
	    	<col>
	    	<col width="200">
	    </colgroup>
	    <tr>
	    <td class="pageSize">
	    <input type="text" id="pageSize" class="pageSize" value='<c:out value="${taskListPageSize}"/>' placeholder="페이지크기" title="페이지크기" onKeyup="task.retrieveMain(this,event.key,'${taskListPageSize}','pageSizeSpan');" /><span id="pageSizeSpan"/>	    
	    </td>
	    <td class="navigator">
	    <input type="text" class="pageInfo" value='<c:out value="${taskListPageInfo}"/>' disabled />	    
		<input type="button" value="이전" style="width:60px;font-size:12px" onclick="task.retrieveNavi('prev');" ${taskListPageFirstYn eq 'Y' ? 'disabled' : ''} />
		<input type="button" value="다음" style="width:60px;font-size:12px" onclick="task.retrieveNavi('next');" ${taskListPageLastYn eq 'Y' ? 'disabled' : ''} />
		</td>
		</tr>
		</table>
	</div>
	
	<br>
		
	<!-- bottom button -->
	<div class="schedulerCommand">
		<table>
	    <tr>
	    <td>
	    <input type="button" id="scheduler.start" value="스케쥴러 시작" onclick="scheduler.start('${confirmCheckValueYn}');"/>
	    <input type="button" id="scheduler.stop" value="스케쥴러 중지" onclick="scheduler.stop('${confirmCheckValueYn}');"/>
		&nbsp;<font color="#cccccc">|</font>&nbsp;
		<input type="button" id="task.all.enableY" value="전체 활성" onclick="support.enableAll('${confirmCheckValueYn}','Y');"/>
		<input type="button" id="task.all.enableN" value="전체 비활성" onclick="support.enableAll('${confirmCheckValueYn}','N');"/>		
		<c:if test="${adminYn eq 'Y'}">
		&nbsp;<font color="#cccccc">|</font>&nbsp;	    
	    <c:if test="${isExistHidden eq true and hiddenFilterYn eq 'Y'}"><input type="button" id="task.retrieveFilter.false" value="숨김항목 보이기" onclick="task.retrieveFilter(false);"></c:if>
	    <c:if test="${isExistHidden eq true and hiddenFilterYn eq 'N'}"><input type="button" id="task.retrieveFilter.true" value="숨김항목 가리기" onclick="task.retrieveFilter(true);"></c:if>
		<c:if test="${logHistoryShrinkYn eq 'Y'}"><input type="button" id="task.clearHistory" value="수행이력 삭제" onclick="support.clearHistory('${confirmCheckValueYn}');"/></c:if>
		<c:if test="${supportTaskConfigInitYn eq 'Y'}"><input type="button" id="task.initConfig" value="타스크 초기화" onclick="support.initConfig('${confirmCheckValueYn}');"/></c:if>
		<c:if test="${supportOutterTaskChangeApplyYn eq 'Y'}"><input type="button" id="task.scanDeploy" value="타스크 업데이트" onclick="support.scanDeploy('${confirmCheckValueYn}');"/></c:if>
		&nbsp;<font color="#cccccc">|</font>&nbsp;
		</c:if>
		<c:if test="${adminYn eq 'N'}">
		&nbsp;<font color="#cccccc">|</font>&nbsp;
		<c:if test="${supportOutterTaskChangeApplyYn eq 'Y'}"><input type="button" id="task.scanDeploy" value="타스크 업데이트" onclick="support.scanDeploy('${confirmCheckValueYn}');"/></c:if>
		&nbsp;<font color="#cccccc">|</font>&nbsp;
		</c:if>		
		<c:if test="${loginMode ne 'innerView'}"><input type="button" value="로그아웃" onclick="window.location.href='${context}/doLogout';"/></c:if>
		</td>
		</tr>
		</table>
	</div>
	
	<c:if test="${!empty authKeyList}">
	<div id="authkeyDiv" class="authKey">
		<br>
		<table>
		<c:forEach var="item" items="${authKeyList}" varStatus="status">
		<tr><td colspan="2"><hr></td></tr>		
		<tr>
		<td class="key"><span><input type="text" value='<c:out value="${item.userId}"/>' class="type1" readonly />&nbsp;:&nbsp;</span></td>
		<td class="value"><span><c:out value="${item.authKey}"/></span></td>
		</tr>
		</c:forEach>
		<tr><td align="right" colspan="2">
		<hr>
		<input type="button" value="close" class="type2" onclick="document.getElementById('authkeyDiv').style.visibility='hidden';"/>
		</td></tr>
		</table>
	</div>
	</c:if>
	
	<script type="text/javascript">
	this.scheduler.status(<c:out value="${schedulerInfo}" escapeXml="false"/>);
	<c:forEach var="task" items="${taskList}" varStatus="status">
	this.task.status("<c:out value="${task.taskId}"/>","<c:out value="${task.enableYn}"/>","<c:out value="${task.hiddenYn}"/>","<c:out value="${task.deleteYn}"/>","<c:out value="${task.status}"/>","<c:out value="${task.countBaseTime}"/>","<fmt:formatNumber value='${task.executionCount}' groupingUsed='true'/>","<fmt:formatNumber value='${task.exceptionCount}' groupingUsed='true'/>","<fmt:formatNumber value='${task.interruptCount}' groupingUsed='true'/>");
	</c:forEach>
	this.task.filterLabel();
	</script>
		
	<!-- hidden Frame -->
	<c:if test="${loginMode eq 'innerView'}"><iframe width="1082" height="0" style="border:none" src="${context}/doRefreshSession"/></c:if>
	<c:if test="${autoReflashYn eq 'Y'}"><iframe width="1082" height="0" style="border:none" src="${schedulerRequestPath}/main?meta=status"/></c:if>

	</div>
  </body>

</html>
<aidTag:alertMessage/>	
