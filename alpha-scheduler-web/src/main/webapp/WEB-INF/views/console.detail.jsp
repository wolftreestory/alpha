<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.alpha.base.BaseUtil"%>
<%@ page import="com.alpha.base.config.batch.BatchProperties"%>
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

String line="<div class='line'><table class='table'><tr class='tr'><td class='td1'></td></tr><tr class='tr'><td class='td2'></td></tr><tr class='tr'><td class='td1'></td></tr></table></div>";
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
%>
<c:set var="context" value="<%=request.getContextPath()%>"/>
<c:set var="schedulerRequestPath" value="<%=schedulerRequestPath%>"/>
<html>
  <head>
    <title><%=systemTitle%></title>
    <link rel="icon" href="${context}/favicon.ico" type="image/x-icon">
	<link href="${context}/css/detail.css" rel="stylesheet" /> 	  
	<script src="${context}/js/detail.js"></script>
	<script type="text/javascript">
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	this.detailClassBuilder = new DetailClassBuilder();
	this.scheduler = new this.detailClassBuilder.SchedulerClass(this,"${schedulerRequestPath}");
	this.task = new this.detailClassBuilder.TaskClass(this,"${schedulerRequestPath}","${executionListRownum}");	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	</script>
  </head>
  <body>
	<div class="center">
	
	<div class="titleInfo">
	<c:if test="${loginMode ne 'innerView'}"><p><%=systemTitleHtml%> <%=systemEnvMode%></p></c:if>
	<table><tr><td><span id="schedulerStatus"></span></td></tr></table>
	</div>
		
	<div class="tab">
		<table>
		<tr>
			<td style="text-align:left">
		  	<button class="tabLinks left active" id="tab1.btn" onclick="fncSetTab('tab1')">상세</button>
		  	<button class="tabLinks left" id="tab2.btn" onclick="fncSetTab('tab2')">수행설정</button>
		  	<c:if test="${adminYn eq 'Y'}"><button class="tabLinks left" id="tab3.btn" onclick="fncSetTab('tab3')">로그설정</button></c:if>		  	
		  	<button class="tabLinks left" id="tab4.btn" onclick="fncSetTab('tab4')">로그뷰</button>
			</td>
			<td style="text-align:right">
			<button class="tabLinks right" id="main.btn" onclick="window.location.href='${schedulerRequestPath}/main';">메인</button>
			<button class="tabLinks right" id="reflash.btn" onclick="task.reflash('${task.taskId}');">새로고침</button>
			</td>
		</tr>
		</table>
	</div>

	<div id="tab1" class="tabContent">
		<div class="tabContent1">	
			<table>
				<tr>
				<td><b><c:if test="${!empty task.group}"><c:if test="${not empty task.groupCode}">${task.groupCode}&nbsp;:&nbsp;</c:if><c:if test="${not empty task.groupLabel}">${task.groupLabel}&nbsp;<font color="#cccccc">|</font>&nbsp;</c:if>${task.group}&nbsp;<font color="#cccccc">|</font>&nbsp;</c:if>${task.name}</b></td>				
				<td style="text-align:right;"><c:if test="${detailViewCheckYn eq 'Y'}"><input type="button" style="width:60px;font-size:13px;" value="초기화" onclick="task.configInit('${task.name}');"/></c:if></td>
				</tr>
			</table>
			<%=line%>
			<table>
				<tr>
					<td style="width:150px;">타스크설명</td>
					<td>${task.description}</td>
				</tr>
				<c:if test="${task.domain eq 'outter'}">
				<tr>
					<td style="width:150px;">스프링배치</td>
					<td><span style="font-family:verdana,sans-serif;font-size:13px;"><span style="color:blue;font-weight:bold">${task.taskMetaObject.jobName}</span>&nbsp;<font size=-1 color=#777777>/</font>&nbsp;클래스&nbsp;:&nbsp;${task.taskMetaObject.jobConfigClass}</span></td>
				</tr>
				<tr>
					<td style="width:150px;">애플리케이션</td>
					<td><span style="font-family:verdana,sans-serif;font-size:13px;">${task.taskMetaObject.appComment}</span>&nbsp;<a href="#" style="text-decoration-line:none;" onclick="fncModalPopup('애플리케이션 설치정보','${schedulerRequestPath}/guide?type=install&mode=default&taskId=${task.taskId}',800,600);">[설치정보]</a></span></td>
				</tr>
				<tr>
					<td style="width:150px;">수행스크립트</td>
					<td><span style="font-family:verdana,sans-serif;font-size:13px;">${task.taskMetaObject.environment.executionScript}</span></td>
				</tr>
				</c:if>
				<tr>
					<td style="width:150px;">수행스케쥴</td>
					<td><c:if test="${empty task.triggerType}">n/a</c:if>
						<c:if test="${task.triggerType eq 'cron'}">${task.triggerType} : ${task.cronExpression} <span style="color:gray">(⏰ ${task.triggerAnalysis})&nbsp;<a href="#" style="text-decoration-line:none;" onclick="fncModalPopup('CRON 수행시간표','${schedulerRequestPath}/guide?type=cron&mode=timetable&taskId=${task.taskId}',350,510);">[수행시간표]</a></span></c:if>
						<c:if test="${task.triggerType eq 'fixedDelay'}">${task.triggerType} : ${task.fixedDelay} <span style="color:gray">(⏰ 이전 작업이 끝난 후 ${task.triggerAnalysis} 지난 후에 다음 작업을 실행)</span></c:if>
						<c:if test="${task.triggerType eq 'fixedRate'}">${task.triggerType} : ${task.fixedRate} <span style="color:gray">(⏰ 이전 작업의 완료 여부와 관계없이 ${task.triggerAnalysis} 간격으로 작업을 수행 (수행시간 > 설정값, rate 없이 바로 실행))</span></c:if>
					</td>
				</tr>
				<c:if test="${task.taskConfig.scheduleTermEnableYn eq 'Y' and not empty task.taskConfig.scheduleTermList}">
				<tr>
					<td style="width:150px;">수행기간</td>
					<td><span style="font-family:verdana,sans-serif;font-size:12px;"><c:forEach var="item" items="${task.taskConfig.scheduleTermList}" varStatus="status"><c:if test="${item.valid}"><span style="color:green;font-weight:bold;">${item.start} - ${item.end}</span></c:if><c:if test="${!item.valid}"><span style="color:gray;font-weight:bold;">${item.start} - ${item.end}</span></c:if><c:if test="${!status.last}">&nbsp;<span style="color:gray;">|</span>&nbsp;</c:if><c:if test="${(status.count % 5) == 0}"><br></c:if></c:forEach></span></td>
				</tr>
				</c:if>
				<tr>
					<td style="width:150px;">상태</td>
					<td><span id="${task.taskId}.status" style="font-family:verdana,sans-serif;font-size:13px;"></span>				
					<c:if test="${task.block && task.nameDupYn eq 'Y'}">이름중복</c:if>
					<c:if test="${task.block && task.executionServerYn eq 'N'}">수행가능서버 없음</c:if>
					</td>
				</tr>
				<tr>
					<td style="width:150px;">수행/오류/중단</td>
					<td>수행 : ${task.executionCount} <font size=-1 color=#777777>/</font> 오류 : <font color="red">${task.exceptionCount}</font> <font size=-1 color=#777777>/</font> 중단 : ${task.interruptCount}&nbsp;&nbsp;<span id="taskCountBaseTime"></span></td>
				</tr>
		
				<c:if test="${detailViewCheckYn eq 'Y' && !empty task.taskConfig.bizProps}">
				<tr>
					<td style="width:150px;"><span id="taskBizPropsToggle" onclick="task.toggleDiv('수행설정','taskBizPropsToggle','taskBizPropsFlag','taskBizPropsDetail')" style="cursor:default" onMouseOver="this.style.color='blue'" onMouseOut="this.style.color=''">수행설정&nbsp;+&nbsp;</span></td>
					<td>
					<div id="taskBizPropsFlag"><c:out value='${task.taskConfig.bizProps}'/></div>
					<div id="taskBizPropsDetail" class="sub1" style="display:none;">
						<span style="color:green">타스크 수행시 필요한 값</span>
						<div class="gap3"></div>
						<table>
						<c:forEach var="item" items="${task.taskConfig.bizProps}" varStatus="status">
							<tr><td><c:out value='${item}'/></td></tr>	
						</c:forEach>
						</table>
					</div>		
				</tr>
				</c:if>	
							
				<c:if test="${detailViewCheckYn eq 'Y' && !empty task.taskConfig.runnableServerList}">
				<tr>
					<td style="width:150px;"><span id="taskRunnableServerListToggle" onclick="task.toggleDiv('수행서버','taskRunnableServerListToggle','taskRunnableServerListFlag','taskRunnableServerListDetail')" style="cursor:default" onMouseOver="this.style.color='blue'" onMouseOut="this.style.color=''">수행서버&nbsp;+&nbsp;</span></td>
					<td>
					<div id="taskRunnableServerListFlag">			
						<c:if test="${task.taskConfig.runnableServerCheckEnableYn eq 'Y'}">수행서버 체크함</c:if><c:if test="${task.taskConfig.runnableServerCheckEnableYn eq 'N'}">수행서버 체크하지 않음</c:if>
					</div>	
					<div id="taskRunnableServerListDetail" class="sub1" style="display:none;">
						<span style="color:green"><c:if test="${task.taskConfig.runnableServerCheckEnableYn eq 'Y'}">수행서버 체크함</c:if><c:if test="${task.taskConfig.runnableServerCheckEnableYn eq 'N'}">수행서버 체크하지 않음</c:if></span>
						<div class="gap3"></div>
						<table>
						<c:forEach var="item" items="${task.taskConfig.runnableServerList}" varStatus="status">
							<tr><td><c:out value='${item}'/></td></tr>	
						</c:forEach>
						</table>
					</div>		
				</tr>
				</c:if>	
												
				<c:if test="${detailViewCheckYn eq 'Y' && !empty task.taskConfig.logEnableYn}">				
				<tr>
					<c:if test="${task.taskConfig.logEnableYn eq 'Y'}">
					<td style="width:150px;"><span id="taskLogConfigToggle" onclick="task.toggleDiv('로그설정','taskLogConfigToggle','taskLogConfigFlag','taskLogConfigDetail')" style="cursor:default" onMouseOver="this.style.color='blue'" onMouseOut="this.style.color=''">로그설정&nbsp;+&nbsp;</span></td>
					<td>
					<div id="taskLogConfigFlag">
						<c:if test="${task.taskConfig.logEnableYn eq 'Y'}">수행시 개별로그 생성</c:if><c:if test="${task.taskConfig.logEnableYn eq 'N'}">none</c:if>
					</div>
					<div id="taskLogConfigDetail" class="sub1" style="display:none;">
						<span style="color:green"><c:if test="${task.taskConfig.logEnableYn eq 'Y'}">수행시 개별로그 생성</c:if><c:if test="${task.taskConfig.logEnableYn eq 'N'}">none</c:if></span>
						<div class="gap3"></div>
						<table>
							<tr><td width="650">수행로그 : <c:out value='${task.taskConfig.executionLogFilePathPattern}'/></td><td>level : <c:out value='${task.taskConfig.executionLogLevel}'/></td></tr>
							<tr><td width="650">오류로그 : <c:out value='${task.taskConfig.exceptionLogFilePathPattern}'/></td><td>level : <c:out value='${task.taskConfig.exceptionLogLevel}'/></td></tr>
						</table>
					</div>
					</td>
					</c:if>
					<c:if test="${task.taskConfig.logEnableYn eq 'N'}">
					<td style="width:150px;">로그설정</td>	
					<td>-</td>		
					</c:if>
				</tr>
				</c:if>
						
				<c:if test="${detailViewCheckYn eq 'Y' && task.taskConfig.logEnableYn eq 'Y'}">
				<tr>
					<td style="width:150px;"><span id="taskLogExceptPatternToggle" onclick="task.toggleDiv('로그제외목록','taskLogExceptPatternToggle','taskLogExceptFlag','taskLogExceptDetail')" style="cursor:default" onMouseOver="this.style.color='blue'" onMouseOut="this.style.color=''">로그제외목록&nbsp;+&nbsp;</span></td>
					<td>
					<div id="taskLogExceptFlag">
						<c:if test="${task.taskConfig.logExceptEnableYn eq 'Y'}">적용</c:if><c:if test="${task.taskConfig.logExceptEnableYn eq 'N'}">미적용</c:if>
					</div>
					<div id="taskLogExceptDetail" class="sub1" style="display:none;">
						<span style="color:green"><c:if test="${task.taskConfig.logExceptEnableYn eq 'Y'}">적용</c:if><c:if test="${task.taskConfig.logExceptEnableYn eq 'N'}">미적용</c:if></span>
						<div class="gap3"></div>
						<table>
						<c:forEach var="item" items="${task.taskConfig.logExceptPatternList}" varStatus="status">
							<tr><td><c:out value='${item}'/></td></tr>	
						</c:forEach>
						</table>
					</div>
					</td>
				</tr>
				</c:if>
				
				<c:if test="${detailViewCheckYn eq 'Y' && task.taskConfig.logEnableYn eq 'Y' and !empty task.taskConfig.logPolicyProps}">
				<tr>
					<td style="width:150px;"><span id="taskLogPolicyToggle" onclick="task.toggleDiv('로그처리정책','taskLogPolicyToggle','taskLogPolicyFlag','taskLogPolicyDetail')" style="cursor:default" onMouseOver="this.style.color='blue'" onMouseOut="this.style.color=''">로그처리정책&nbsp;+&nbsp;</span></td>
					<td>
					<div id="taskLogPolicyFlag">						
						<c:if test="${task.taskConfig.logPolicyProps.globalPolicyYn eq 'Y'}">기본정책</c:if><c:if test="${task.taskConfig.logPolicyProps.globalPolicyYn eq 'N'}">개별정책</c:if>
					</div>
					<div id="taskLogPolicyDetail" class="sub1" style="display:none;">
						<span style="color:green"><c:if test="${task.taskConfig.logPolicyProps.globalPolicyYn eq 'Y'}">&nbsp;기본정책</c:if><c:if test="${task.taskConfig.logPolicyProps.globalPolicyYn eq 'N'}">개별정책</c:if></span>
						<div class="gap3"></div>
						<c:if test="${task.taskConfig.logPolicyProps.globalPolicyYn eq 'Y'}"><c:set var="taskLogPolicyProps" value="${globalLogPolicyProps}"/></c:if>
						<c:if test="${task.taskConfig.logPolicyProps.globalPolicyYn eq 'N'}"><c:set var="taskLogPolicyProps" value="${task.taskConfig.logPolicyProps}"/></c:if>						
						<table>
						<tr><td>타스크 수행 시 스탬프를 출력 : ${taskLogPolicyProps.logStampPrintYn}<c:if test="${empty taskLogPolicyProps.logStampPrintYn}">N</c:if><c:if test="${taskLogPolicyProps.logHistoryShrinkYn eq 'Y'}"><font color="#606060">&nbsp;[&nbsp;수행시작:${taskLogPolicyProps.logStampStartPrintYn}<c:if test="${empty taskLogPolicyProps.logStampStartPrintYn}">N</c:if>,&nbsp;수행오류:${taskLogPolicyProps.logStampExceptionPrintYn}<c:if test="${empty taskLogPolicyProps.logStampExceptionPrintYn}">N</c:if>,&nbsp;수행시간:${taskLogPolicyProps.logStampAroundPrintYn}<c:if test="${empty taskLogPolicyProps.logStampAroundPrintYn}">N</c:if>,&nbsp;수행종료:${taskLogPolicyProps.logStampEndPrintYn}<c:if test="${empty taskLogPolicyProps.logStampEndPrintYn}">N</c:if>&nbsp;]</font></c:if></td></tr>
						<tr><td>타스크 수행 후 로그 파일 DB저장 : ${taskLogPolicyProps.logFileDbStoreYn}<c:if test="${empty taskLogPolicyProps.logFileDbStoreYn}">N</c:if></td></tr>						
						<tr><td>타스크 수행 후 로그 파일 삭제 : ${taskLogPolicyProps.logFileRemoveYn}<c:if test="${empty taskLogPolicyProps.logFileRemoveYn}">N</c:if></td></tr>
						<tr><td>타스크 수행 후 로그 이력 축소 : ${taskLogPolicyProps.logHistoryShrinkYn}<c:if test="${empty taskLogPolicyProps.logHistoryShrinkYn}">N</c:if><c:if test="${taskLogPolicyProps.logHistoryShrinkYn eq 'Y'}"><font color="#606060">&nbsp;[&nbsp;days:${taskLogPolicyProps.logHistoryShrinkBaseDays}, rows:${taskLogPolicyProps.logHistoryShrinkBaseRows}&nbsp;]</font></c:if></td></tr>
						</table>
						<c:remove var="taskLogPolicyProps"/>
					</div>
					</td>
				</tr>
				</c:if>

				<c:if test="${detailViewCheckYn eq 'Y'}">
				<tr>
					<td style="width:150px;">등록/수정</td>
					<td>
					<div id="taskDateInfoFlag"></div>	
					<div id="taskDateInfoDetail" class="subX" style="display:block;">
						<div class="gap3"></div>
						<table>
							<tr><td style="width:25px;text-align:right;">create&nbsp;:</td><td><c:out value='${task.createDate}'/></td></tr>	
							<tr><td style="width:25px;text-align:right;">update&nbsp;:</td><td><c:out value='${task.updateDate}'/></td></tr>	
						</table>
					</div>		
				</tr>
				</c:if>

 			</table>
			
			<%=line%>
			<table>
			<tr>
				<td>
				<c:if test="${task.enableYn eq 'N'}"><input type="button" style="width:60px;font-size:13px" value="활성" onclick="task.enable('${task.name}','Y');"/></c:if>
				<c:if test="${task.enableYn eq 'Y'}"><input type="button" style="width:60px;font-size:13px" value="비활성" onclick="task.enable('${task.name}','N');"/></c:if>
				<input type="button" id="${task.taskId}.command" style="width:60px;font-size:13px" value="-" onclick="task.command(this,'${task.name}');"/>
				</td>
				<td style="text-align:right;">
				<span style="font-family:verdana,sans-serif;font-size:14px;">
				<label><input type="checkbox" id="detailViewCheckYn" style="vertical-align:top;" <c:if test="${detailViewCheckYn eq 'Y'}">checked</c:if> onclick="task.detailView(this)">상세보기</label>
				</span>				
			</tr>
			</table>
		</div>
	</div>
	
	<div id="tab2" class="tabContent">
		<div class="tabContent2">
			<table>
				<tr>
				<td><b><c:if test="${!empty task.group}"><c:if test="${not empty task.groupCode}">${task.groupCode}&nbsp;:&nbsp;</c:if><c:if test="${not empty task.groupLabel}">${task.groupLabel}&nbsp;<font color="#cccccc">|</font>&nbsp;</c:if>${task.group}&nbsp;<font color="#cccccc">|</font>&nbsp;</c:if>${task.name}</b></td>				
				<td style="text-align:right;">&nbsp;</td>
				</tr>
			</table>
			<%=line%>
			<table>
				<tr>
				<td>타스크 수행에 직접적인 영향을 주는 설정 (적합하지 않은 설정시 수행오류가 발생할 수 있음, 변경시 적용가능)</td>
				</tr>
			</table>
			<!-- 스케쥴정보 -->
			<%=line%>
			<table>
			<tr>
				<td><b>수행스케쥴</b><span id="schedulerChangeEnable"></span></td>
			</tr>
			<tr>
				<td style="border:none;padding:0px;">
				<table>
				<tr>
				<td class="sch1">&nbsp;<input type="radio" id="${task.taskId}.cronExpression@c" <c:if test="${not empty task.cronExpression}">checked</c:if> onclick="task.scheduleMode(this);"><a href="#" style="text-decoration-line:none;" id="${task.taskId}.cronExpression@a" onclick="task.scheduleMode(this);">CronExpression</a></td>
				<td class="sch2"><input type="text" value=":" class="textTypeX" disabled readonly><input type="text" id="${task.taskId}.cronExpression@v" value="<c:out value='${task.cronExpression}'/>" class="textType21" onkeyup="task.configScheduleChangeCheck(this,'${task.cronExpression}');">&nbsp;<span id="${task.taskId}.cronExpression@s"></span></td>
				<td class="sch3">cron표현식을 사용하여 시점 또는 주기로 실행 <a href="#" style="text-decoration-line:none;" onclick="fncModalPopup('CRON 가이드','${schedulerRequestPath}/guide?type=cron',1000,800);">[guide]</a></td>
				<td>&nbsp;</td>
				</tr></table>
				<div class="gap3"></div>
				<table><tr>
				<td class="sch1">&nbsp;<input type="radio" id="${task.taskId}.fixedDelay@c" <c:if test="${not empty task.fixedDelay}">checked</c:if> onclick="task.scheduleMode(this);"><a href="#" style="text-decoration-line:none;" id="${task.taskId}.fixedDelay@a" onclick="task.scheduleMode(this);">FixedDelay</a></td>
				<td class="sch2"><input type="text" value=":" class="textTypeX" disabled readonly><input type="text" id="${task.taskId}.fixedDelay@v" value="<c:out value='${task.fixedDelay}'/>" class="textType21" onkeyup="task.configScheduleChangeCheck(this,'${task.fixedDelay}');" title="(ex) 1000:1초, 10s:10초, 5m:5분, 1h:1시간" >&nbsp;<span id="${task.taskId}.fixedDelay@s"></span></td>
				<td class="sch3">고정지연 : 이전 작업이 끝난 후 일정 시간이 지난 후에 다음 작업을 실행</td>
				<td>&nbsp;</td>
				</tr></table>
				<div class="gap3"></div>
				<table><tr>
				<td class="sch1">&nbsp;<input type="radio" id="${task.taskId}.fixedRate@c" <c:if test="${not empty task.fixedRate}">checked</c:if> onclick="task.scheduleMode(this);"><a href="#" style="text-decoration-line:none;" id="${task.taskId}.fixedRate@a" onclick="task.scheduleMode(this);">FixedRate</a></td>
				<td class="sch2"><input type="text" value=":" class="textTypeX" disabled readonly><input type="text" id="${task.taskId}.fixedRate@v" value="<c:out value='${task.fixedRate}'/>" class="textType21" onkeyup="task.configScheduleChangeCheck(this,'${task.fixedRate}');" title="(ex) 1000:1초, 10s:10초, 5m:5분, 1h:1시간" >&nbsp;<span id="${task.taskId}.fixedRate@s"></span></td>
				<td class="sch3">고정주기 : 이전 작업의 완료 여부와 관계없이 일정한 간격으로 작업을 수행 (수행시간 > 설정값, rate 없이 바로 실행)</td>
				<td>&nbsp;</td>
				</tr>
				</table>
				<div class="gap3"></div>
				<table><tr>
				<td class="sch1">&nbsp;<label><input type="checkbox" id="${task.taskId}.taskConfig.scheduleTermEnableYn@c" <c:if test="${task.taskConfig.scheduleTermEnableYn eq 'Y'}">checked</c:if> style="vertical-align:middle;height:11px" onclick="task.configScheduleTermEnable('${task.taskId}.taskConfig.scheduleTermEnableYn@c',this);">Date (from-to)</label></td>				
				<td class="sch2"><input type="text" value=":" class="textTypeX" disabled readonly><input type="date" id="${task.taskId}.scheduleTermList.new.start" class="textType22" <c:if test="${task.taskConfig.scheduleTermEnableYn eq 'N'}">disabled readonly</c:if>><input type="text" value="-" class="textTypeY" disabled readonly><input type="date" id="${task.taskId}.scheduleTermList.new.end" class="textType22" <c:if test="${task.taskConfig.scheduleTermEnableYn eq 'N'}">disabled readonly</c:if>><c:if test="${task.taskConfig.scheduleTermEnableYn eq 'Y' and schedulerStatus eq 'stop'}">&nbsp;<a href="#" onClick="task.configScheduleTerm('add','${task.taskId}.scheduleTermList.new.start','${task.taskId}.scheduleTermList.new.end');">추가</a></c:if></td>
				<td class="sch3">기간설정 : 설정한 실행주기(CronExpression,FixedDelay,FixedRate)가 적용되는 기간</td>
				<td>&nbsp;</td>
				</tr>
				</table>
				<c:if test="${!empty task.taskConfig.scheduleTermList}">
				<c:forEach var="item" items="${task.taskConfig.scheduleTermList}" varStatus="status">
				<div class="gap3"></div>
				<table><tr>
				<td class="sch1" style="text-align:right;"><span style="color:gray">${item.status}</span>&nbsp;</td>
				<td class="sch2"><input type="text" value=":" class="textTypeX" disabled readonly><input type="date" id="${task.taskId}.scheduleTermList.${status.count}.start" value="<c:out value='${item.start}'/>" class="textType22" disabled readonly><input type="text" value="-" class="textTypeY" disabled readonly><input type="date" id="${task.taskId}.scheduleTermList.${status.count}.end" value="<c:out value='${item.end}'/>" class="textType22" disabled readonly><c:if test="${task.taskConfig.scheduleTermEnableYn eq 'Y' and schedulerStatus eq 'stop'}">&nbsp;<a href="#" onClick="task.configScheduleTerm('remove','${task.taskId}.scheduleTermList.${status.count}.start','${task.taskId}.scheduleTermList.${status.count}.end');">삭제</a></c:if></td>
				<td class="sch3"></td>
				<td>&nbsp;</td>
				</tr>
				</table>
				</c:forEach>
				</c:if>
				</td>
				<script type="text/javascript">
				task.scheduleMode("${task.taskId}.initBackup");
				task.scheduleMode("${task.taskId}.schedulerStatus:<c:out value="${schedulerStatus}"/>");
				</script>	
			</tr>
			</table>

			<c:if test="${!empty task.taskConfig.bizProps}">
			<!-- 수행프로퍼티/배치잡파라메터 -->
			<%=line%>
			<table>
			<tr>
				<c:if test="${task.domain eq 'inner'}"><td><b>수행프로퍼티</b></td></c:if>
				<c:if test="${task.domain eq 'outter' and supportOutterTaskParamConfigYn eq 'Y'}"><td><b>배치잡 고정 파라메터</b>&nbsp;<label><input type="checkbox" style="vertical-align:top;" id="paramExtensionConfigCheckYn" style="vertical-align:middle;height:11px" <c:if test="${paramExtensionConfigCheckYn eq 'Y'}">checked</c:if> onclick="task.configOutterOptionExpansion('paramExtensionConfigCheckYn',this)">추가설정</label></td></c:if>
				<c:if test="${task.domain eq 'outter' and supportOutterTaskParamConfigYn eq 'N'}"><td><b>배치잡 고정 파라메터</b></td></c:if>
			</tr>
			<tr>
				<td style="border:none;padding:0px;">
				<c:forEach var="item" items="${task.taskConfig.bizPropsDesc}" varStatus="status">
				<input type="hidden" id="${item.key}@bizPropsDesc" value="<c:out value='${item.value}'/>" class="textType23">
				</c:forEach>
				<c:forEach var="item" items="${task.taskConfig.bizProps}" varStatus="status">
				<table>
				<tr>
				<td class="sch1"><input type="text" id="${task.taskId}.taskConfig.bizProps.${status.count}.key" value="<c:out value='${item.key}'/>" class="textType22" disabled readonly></td>
				<td class="sch2"><input type="text" value=":" class="textTypeX" disabled readonly><input type="text" id="${task.taskId}.taskConfig.bizProps.${status.count}.value" value="<c:out value='${item.value}'/>" class="textType23" onkeyup="task.configBizPropsChangeCheck(this,'<c:out value='${item.value}'/>','${task.taskId}.taskConfig.bizProps.${status.count}')">&nbsp;<span id="${task.taskId}.taskConfig.bizProps.${status.count}.span"></span></td>
				<td class="sch3"><span id="${task.taskId}.taskConfig.bizProps.${status.count}.desc"></span></td>
				<td>&nbsp;<script type="text/javascript">task.setBizPropsDesc("${item.key}@bizPropsDesc","${task.taskId}.taskConfig.bizProps.${status.count}.desc");</script></td>
				</tr>
				</table>
				<c:if test="${!status.last}"><div class="gap3"></div></c:if>
				</c:forEach>
				</td>
			</tr>
			</table>
			
			<c:if test="${task.domain eq 'outter' and supportOutterTaskParamConfigYn eq 'Y' and paramExtensionConfigCheckYn eq 'Y'}">			
			<%=line%>
			<table>
			<tr>
				<td><b>배치잡 가변 파라메터(apiUrl)</b></td>
			</tr>
			<tr>
				<td style="border:none;padding:0px;">
				<table>
				<tr>
				<input type="hidden" id="${task.taskId}.jobParametersApiUrl.hidden" value="<c:out value='${task.taskConfig.jobParametersApiUrl}'/>" >
				<input type="hidden" id="${task.taskId}.jobParametersApiUrlEnableYn.hidden" value="<c:out value='${task.taskConfig.jobParametersApiUrlEnableYn}'/>" >
				<input type="hidden" id="${task.taskId}.jobParametersApiUrlOverlapYn.hidden" value="<c:out value='${task.taskConfig.jobParametersApiUrlOverlapYn}'/>" >
				<td class="sch4"><textarea id="${task.taskId}.jobParametersApiUrl" onkeyup="task.configOutterOptionChangeCheck('${task.taskId}.jobParametersApiUrl','${task.taskId}.jobParametersApiUrl.span',this);">${task.taskConfig.jobParametersApiUrl}</textarea></td>
				<td>
					<table>
					<tr><td class="sch3">&nbsp;배치잡 수행시 API호출하여 응답결과를 배치잡 파라메터에 적용 (응답:json)</td></tr>
					<tr><td class="sch3">&nbsp;<label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.jobParametersApiUrlEnableYn" <c:if test="${task.taskConfig.jobParametersApiUrlEnableYn eq 'Y'}">checked</c:if> onclick="task.configOutterOptionChangeCheck('${task.taskId}.jobParametersApiUrl','${task.taskId}.jobParametersApiUrl.span','${task.taskId}.jobParametersApiUrlEnableYn',this);">활성</label>&nbsp;&nbsp;&nbsp;&nbsp;<label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.jobParametersApiUrlOverlapYn" <c:if test="${task.taskConfig.jobParametersApiUrlOverlapYn eq 'Y'}">checked</c:if> onclick="task.configOutterOptionChangeCheck('${task.taskId}.jobParametersApiUrl','${task.taskId}.jobParametersApiUrl.span','${task.taskId}.jobParametersApiUrlOverlapYn',this);">병합</label>&nbsp;('배치잡 고정 파라메터'에 overlap)</td></tr>
					</table>
				</td>
				<td class="sch5"><span id="${task.taskId}.jobParametersApiUrl.span"></span></td>				
				</tr>
				</table>
				</td>
			</tr>
			</table>
			
			<%=line%>
			<table>
			<tr>
				<td><b>배치잡 수행결과 callback(apiUrl)</b></td>
			</tr>
			<tr>
				<td style="border:none;padding:0px;">
				<table>
				<tr>
				<input type="hidden" id="${task.taskId}.callbackApiUrl.hidden" value="<c:out value='${task.taskConfig.callbackApiUrl}'/>" >
				<input type="hidden" id="${task.taskId}.callbackApiUrlEnableYn.hidden" value="<c:out value='${task.taskConfig.callbackApiUrlEnableYn}'/>" >
				<td class="sch4"><textarea id="${task.taskId}.callbackApiUrl" onkeyup="task.configOutterOptionChangeCheck('${task.taskId}.callbackApiUrl','${task.taskId}.callbackApiUrl.span',this);">${task.taskConfig.callbackApiUrl}</textarea></td>
				<td>
					<table>
					<tr><td class="sch3">&nbsp;배치잡 수행후 API호출하여 처리결과를 전달 (배치잡 개발코드에서 콜백처리 구현, doCallBack(..))</td></tr>
					<tr><td class="sch3">&nbsp;<label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.callbackApiUrlEnableYn" <c:if test="${task.taskConfig.callbackApiUrlEnableYn eq 'Y'}">checked</c:if> onclick="task.configOutterOptionChangeCheck('${task.taskId}.callbackApiUrl','${task.taskId}.callbackApiUrl.span','${task.taskId}.callbackApiUrlEnableYn',this);">활성</label></td></tr>
					</table>
				</td>
				<td class="sch5"><span id="${task.taskId}.callbackApiUrl.span"></span></td>
				</tr>
				</table>
				</td>
			</tr>
			</c:if>
			</table>
			</c:if>
			
			<c:if test="${task.domain eq 'outter' and supportOutterTaskParamConfigYn eq 'Y' }">			
			<%=line%>
			<table>
			<tr>
				<td><b>배치잡 JVM 메모리</b></td>
			</tr>
			<tr>
				<td style="border:none;padding:0px;">
				<table>
				<tr>
				<input type="hidden" id="${task.taskId}.jvmMemory.hidden" value="<c:out value='${task.taskConfig.jvmMemory}'/>" >
				<input type="hidden" id="${task.taskId}.jvmMemoryEnableYn.hidden" value="<c:out value='${task.taskConfig.jvmMemoryEnableYn}'/>" >
				<td class="sch4"><textarea id="${task.taskId}.jvmMemory" onkeyup="task.configOutterOptionChangeCheck('${task.taskId}.jvmMemory','${task.taskId}.jvmMemory.span',this);">${task.taskConfig.jvmMemory}</textarea></td>								
				<td>
					<table>
					<tr><td class="sch3">&nbsp;배치잡 수행시 jvm에 적용할 메모리 (default: -Xms256m -Xmx512m)</td></tr>
					<tr><td class="sch3">&nbsp;<label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.jvmMemoryEnableYn" <c:if test="${task.taskConfig.jvmMemoryEnableYn eq 'Y'}">checked</c:if> onclick="task.configOutterOptionChangeCheck('${task.taskId}.jvmMemory','${task.taskId}.jvmMemory.span','${task.taskId}.jvmMemoryEnableYn',this);">활성</label><td></tr>
					</table>
				</td>
				<td class="sch5"><span id="${task.taskId}.jvmMemory.span"></span></td>
				</tr>
				</table>
				</td>
			</tr>
			</table>			
			</c:if>
			
			<c:if test="${empty task.taskConfig.bizProps}">
			<%=line%>
			<table>
			<tr>
				<td><b>수행프로퍼티 없음</b></td>
			</tr>
			</table>
			</c:if>
						
			<c:if test="${task.taskConfig.saveImmediatelyYn eq 'N'}">
			<%=line%>
			<table>
			<tr>
				<td class="command">
				<input type="button" style="width:60px;font-size:13px;" value="초기화" onclick="task.configLoad('수행설정','bizProps,scheduleType,scheduleValue');"/>
				<input type="button" style="width:60px;font-size:13px;" value="저장" onclick="task.configSave('수행설정','bizProps,scheduleType,scheduleValue');"/>				
				</td>
				<td class="guide"></td>
				<td>&nbsp;</td>
			</tr>
			</table>
			</c:if>

		</div>
	</div>

	<div id="tab3" class="tabContent">
		<div class="tabContent3">
			<table>
				<tr>
				<td><b><c:if test="${!empty task.group}"><c:if test="${not empty task.groupCode}">${task.groupCode}&nbsp;:&nbsp;</c:if><c:if test="${not empty task.groupLabel}">${task.groupLabel}&nbsp;<font color="#cccccc">|</font>&nbsp;</c:if>${task.group}&nbsp;<font color="#cccccc">|</font>&nbsp;</c:if>${task.name}</b></td>				
				<td style="text-align:right;">&nbsp;</td>
				</tr>
			</table>
			<%=line%>	
			<table>
				<tr>
				<td>
				<input type="checkbox" style="vertical-align:top;" id="${task.taskId}.taskConfig.logEnableYn" <c:if test="${task.taskConfig.logEnableYn eq 'Y'}">checked</c:if> onclick="task.configLogEnable('${task.taskId}.taskConfig.logEnableYn');">
				<c:if test="${task.taskConfig.logEnableYn eq 'Y'}">수행시 개별 로그 생성 (체크 해제시 [로그파일/레벨],[로그제외목록],[로그처리정책] 적용/설정 불가능)</c:if>
				<c:if test="${task.taskConfig.logEnableYn eq 'N'}">수행시 개별 로그 생성 (체크시 [로그파일/레벨],[로그제외목록],[로그처리정책] 적용/설정 가능)</c:if>
				<c:if test="${task.domain eq 'outter'}"><br><span style="color:gray;">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;이 타스크는 스케쥴러와 독립된 프로세스로 수행됩니다. 따라서, 본 설정은 스케쥴러 로그에만 적용됩니다.</span></c:if>
				</td>
				</tr>
			</table>
			
			<c:if test="${task.taskConfig.logEnableYn eq 'Y'}">
			
			<!-- 로그파일/레벨 -->
			<%=line%>
			<table>
			<tr>
				<td><b>로그파일/레벨 (trace, debug, info, warn, error)</b></td>
			</tr>
			<tr>			
				<td style="border:none;padding:0px;">
				<input type="text" value="실행 로그" class="textType33" disabled readonly><input type="text" value=":" class="textTypeX" disabled readonly><input type="text" id="${task.taskId}.taskConfig.executionLogFilePathPattern.value" value="<c:out value='${task.taskConfig.executionLogFilePathPattern}'/>" class="textType34">&nbsp;<input type="text" id="${task.taskId}.taskConfig.executionLogLevel.value" value="<c:out value='${task.taskConfig.executionLogLevel}'/>" class="textType35">&nbsp;<a href="#" onClick="task.configLogFileLevel('executionLog','${task.taskId}.taskConfig.executionLogFilePathPattern.value','${task.taskId}.taskConfig.exceptionLogFilePathPattern.value','${task.taskId}.taskConfig.executionLogLevel.value')">적용</a>
				<div class="gap3"></div>
				</td>
			</tr>
			<tr>
				<td style="border:none;padding:0px;">
				<input type="text" value="오류 로그" class="textType33" disabled readonly><input type="text" value=":" class="textTypeX" disabled readonly><input type="text" id="${task.taskId}.taskConfig.exceptionLogFilePathPattern.value" value="<c:out value='${task.taskConfig.exceptionLogFilePathPattern}'/>" class="textType34">&nbsp;<input type="text" id="${task.taskId}.taskConfig.exceptionLogLevel.value" value="<c:out value='${task.taskConfig.exceptionLogLevel}'/>" class="textType35">&nbsp;<a href="#" onClick="task.configLogFileLevel('exceptionLog','${task.taskId}.taskConfig.exceptionLogFilePathPattern.value','${task.taskId}.taskConfig.executionLogFilePathPattern.value','${task.taskId}.taskConfig.exceptionLogLevel.value')">적용</a>
				</td>
			</tr>
			</table>
															
			<!-- 로그제외목록 -->
			<%=line%>
			<table>
			<tr>
				<td><b>로그제외목록 (logger classpath)</b>&nbsp;<label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.taskConfig.logExceptEnableYn" <c:if test="${task.taskConfig.logExceptEnableYn eq 'Y'}">checked</c:if> style="vertical-align:middle;height:11px" onclick="task.configLogExceptEnable('${task.taskId}.taskConfig.logExceptEnableYn',this);">적용</label></td>
			</tr>
			<tr>
				<td style="border:none;padding:0px;">
				<c:forEach var="item" items="${task.taskConfig.logExceptPatternList}" varStatus="status">
				<input type="text" id="${task.taskId}.taskConfig.logExceptPatternList.${status.count}" value="<c:out value='${item}'/>" class="textType31" disabled readonly>&nbsp;<a href="#" onClick="task.configLogExceptPattern('remove','${task.taskId}.taskConfig.logExceptPatternList.${status.count}');">삭제</a><br>
				<div class="gap3"></div>
				</c:forEach>
				<input type="text" id="${task.taskId}.taskConfig.logExceptPatternList.new" class="textType32">&nbsp;<a href="#" onClick="task.configLogExceptPattern('add','${task.taskId}.taskConfig.logExceptPatternList.new');">추가</a><br>
				</td>
			</tr>
			</table>
	
			<!-- 로그처리정책 -->
			<c:if test="${!empty task.taskConfig.logPolicyProps}">
			<%=line%>
			<table>
			<tr>
				<td><b>로그처리정책</b>&nbsp;<label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.taskConfig.logPolicyProps.globalPolicyYn" <c:if test="${task.taskConfig.logPolicyProps.globalPolicyYn eq 'Y'}">checked</c:if> style="vertical-align:middle;height:11px" onclick="task.configLogPolicyProps('globalPolicyYn','${task.taskId}.taskConfig.logPolicyProps.globalPolicyYn',this);">기본정책</label></td>
			</tr>
			<tr>
				<td>
				<div class="sub1">	
				<table>
				<c:if test="${task.taskConfig.logPolicyProps.globalPolicyYn eq 'Y'}"><c:set var="taskLogPolicyProps" value="${globalLogPolicyProps}"/>
				<c:if test="${configEnableProps.logStampPrintYn eq 'Y'}"><tr><td><label><input type="checkbox" style="vertical-align:top;" <c:if test="${taskLogPolicyProps.logStampPrintYn eq 'Y'}">checked</c:if> disabled>&nbsp;타스크 수행 시 스탬프를 출력합니다.</label><c:if test="${taskLogPolicyProps.logStampPrintYn eq 'Y'}">&nbsp;[&nbsp;<label><input type="checkbox" style="vertical-align:top;" <c:if test="${taskLogPolicyProps.logStampStartPrintYn eq 'Y'}">checked</c:if> disabled>수행시작</label>,&nbsp;<label><input type="checkbox" style="vertical-align:top;" <c:if test="${taskLogPolicyProps.logStampExceptionPrintYn eq 'Y'}">checked</c:if> disabled>수행오류</label>,&nbsp;<label><input type="checkbox" style="vertical-align:top;" <c:if test="${taskLogPolicyProps.logStampAroundPrintYn eq 'Y'}">checked</c:if> disabled>수행시간</label>,&nbsp;<label><input type="checkbox" style="vertical-align:top;" <c:if test="${taskLogPolicyProps.logStampEndPrintYn eq 'Y'}">checked</c:if> disabled>수행종료</label>&nbsp;]</c:if></td></tr></c:if>
				<c:if test="${configEnableProps.logFileDbStoreYn eq 'Y'}"><tr><td><label><input type="checkbox" style="vertical-align:top;" <c:if test="${taskLogPolicyProps.logFileDbStoreYn eq 'Y'}">checked</c:if> disabled>&nbsp;타스크 수행 후 로그 파일을 DB에 저장합니다.</label></td></tr></c:if>
				<c:if test="${configEnableProps.logFileRemoveYn eq 'Y'}"><tr><td><label><input type="checkbox" style="vertical-align:top;" <c:if test="${taskLogPolicyProps.logFileRemoveYn eq 'Y'}">checked</c:if> disabled>&nbsp;타스크 수행 후 로그 파일을 삭제합니다.</label></td></tr></c:if>
				<c:if test="${configEnableProps.logHistoryShrinkYn eq 'Y'}"><tr><td><label><input type="checkbox" style="vertical-align:top;" <c:if test="${taskLogPolicyProps.logHistoryShrinkYn eq 'Y'}">checked</c:if> disabled>&nbsp;타스크 수행 후 로그 이력을 축소합니다.</label><c:if test="${taskLogPolicyProps.logHistoryShrinkYn eq 'Y'}"><font color="#606060">&nbsp;[&nbsp;days:<input type="text" value="${taskLogPolicyProps.logHistoryShrinkBaseDays}" class="textTypeA" disabled>,&nbsp;rows:<input type="text" value="${taskLogPolicyProps.logHistoryShrinkBaseRows}" class="textTypeA" disabled>&nbsp;]</font></c:if></td></tr></c:if>
				</c:if>
				<c:if test="${task.taskConfig.logPolicyProps.globalPolicyYn eq 'N'}"><c:set var="taskLogPolicyProps" value="${task.taskConfig.logPolicyProps}"/>
				<c:if test="${configEnableProps.logStampPrintYn eq 'Y'}"><tr><td><label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.taskConfig.logPolicyProps.logStampPrintYn" <c:if test="${taskLogPolicyProps.logStampPrintYn eq 'Y'}">checked</c:if> onclick="task.configLogPolicyProps('logStampPrintYn','${task.taskId}.taskConfig.logPolicyProps.logStampPrintYn');">&nbsp;타스크 수행 시 스탬프를 출력합니다.</label><c:if test="${taskLogPolicyProps.logStampPrintYn eq 'Y'}">&nbsp;[&nbsp;<label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.taskConfig.logPolicyProps.logStampStartPrintYn" <c:if test="${taskLogPolicyProps.logStampStartPrintYn eq 'Y'}">checked</c:if>  onclick="task.configLogPolicyProps('logStampStartPrintYn','${task.taskId}.taskConfig.logPolicyProps.logStampStartPrintYn');">수행시작</label>,&nbsp;<label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.taskConfig.logPolicyProps.logStampExceptionPrintYn" <c:if test="${taskLogPolicyProps.logStampExceptionPrintYn eq 'Y'}">checked</c:if> onclick="task.configLogPolicyProps('logStampExceptionPrintYn','${task.taskId}.taskConfig.logPolicyProps.logStampExceptionPrintYn');">수행오류</label>,&nbsp;<label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.taskConfig.logPolicyProps.logStampAroundPrintYn" <c:if test="${taskLogPolicyProps.logStampAroundPrintYn eq 'Y'}">checked</c:if> onclick="task.configLogPolicyProps('logStampAroundPrintYn','${task.taskId}.taskConfig.logPolicyProps.logStampAroundPrintYn');">수행시간</label>,&nbsp;<label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.taskConfig.logPolicyProps.logStampEndPrintYn" <c:if test="${taskLogPolicyProps.logStampEndPrintYn eq 'Y'}">checked</c:if> onclick="task.configLogPolicyProps('logStampEndPrintYn','${task.taskId}.taskConfig.logPolicyProps.logStampEndPrintYn');">수행종료</label>&nbsp;]</c:if></td></tr></c:if>
				<c:if test="${configEnableProps.logFileDbStoreYn eq 'Y'}"><tr><td><label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.taskConfig.logPolicyProps.logFileDbStoreYn" <c:if test="${taskLogPolicyProps.logFileDbStoreYn eq 'Y'}">checked</c:if> onclick="task.configLogPolicyProps('logFileDbStoreYn','${task.taskId}.taskConfig.logPolicyProps.logFileDbStoreYn');">&nbsp;타스크 수행 후 로그 파일을 DB에 저장합니다.</label></td></tr></c:if>
				<c:if test="${configEnableProps.logHistoryShrinkYn eq 'Y'}"><tr><td><label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.taskConfig.logPolicyProps.logFileRemoveYn" <c:if test="${taskLogPolicyProps.logFileRemoveYn eq 'Y'}">checked</c:if> onclick="task.configLogPolicyProps('logFileRemoveYn','${task.taskId}.taskConfig.logPolicyProps.logFileRemoveYn');">&nbsp;타스크 수행 후 로그 파일을 삭제합니다.</label></td></tr></c:if>
				<c:if test="${configEnableProps.logFileRemoveYn eq 'Y'}"><tr><td><label><input type="checkbox" style="vertical-align:top;" id="${task.taskId}.taskConfig.logPolicyProps.logHistoryShrinkYn" <c:if test="${taskLogPolicyProps.logHistoryShrinkYn eq 'Y'}">checked</c:if> onclick="task.configLogPolicyProps('logHistoryShrinkYn','${task.taskId}.taskConfig.logPolicyProps.logHistoryShrinkYn');">&nbsp;타스크 수행 후 로그 이력을 축소합니다.</label><c:if test="${taskLogPolicyProps.logHistoryShrinkYn eq 'Y'}"><font color="#606060">&nbsp;[&nbsp;days:<input type="text" id="${task.taskId}.taskConfig.logPolicyProps.logHistoryShrinkBaseDays" value="${taskLogPolicyProps.logHistoryShrinkBaseDays}" class="textTypeB">&nbsp;<a href="#" onClick="task.configLogPolicyProps('logHistoryShrinkBaseDays','${task.taskId}.taskConfig.logPolicyProps.logHistoryShrinkBaseDays');">적용</a>,&nbsp;rows:<input type="text" id="${task.taskId}.taskConfig.logPolicyProps.logHistoryShrinkBaseRows" value="${taskLogPolicyProps.logHistoryShrinkBaseRows}" class="textTypeB">&nbsp;<a href="#" onClick="task.configLogPolicyProps('logHistoryShrinkBaseRows','${task.taskId}.taskConfig.logPolicyProps.logHistoryShrinkBaseRows');">적용</a>&nbsp;]</font></c:if></td></tr></c:if>
				</c:if>
				</table>
				</div>
				</td>
			</tr>	
			</table>
			</c:if>

			</c:if>

			<c:if test="${task.taskConfig.saveImmediatelyYn eq 'N'}">
			<%=line%>
			<table>
			<tr>
				<td class="command">
				<input type="button" style="width:60px;font-size:13px;" value="초기화" onclick="task.configLoad('로그설정','logEnableYn,executionLogLevel,exceptionLogLevel,executionLogFilePathPattern,exceptionLogFilePathPattern,logExceptEnableYn,logExceptPatternList,logPolicyProps');"/>
				<input type="button" style="width:60px;font-size:13px;" value="저장" onclick="task.configSave('로그설정','logEnableYn,executionLogLevel,exceptionLogLevel,executionLogFilePathPattern,exceptionLogFilePathPattern,logExceptEnableYn,logExceptPatternList,logPolicyProps');"/>				
				</td>
				<td class="guide"></td>
				<td>&nbsp;</td>
			</tr>
			</table>
			</c:if>
		</div>
	</div>
	
	<div id="tab4" class="tabContent">
		<div class="tabContent4">
			<table>
				<tr>
				<td><b><c:if test="${!empty task.group}"><c:if test="${not empty task.groupCode}">${task.groupCode}&nbsp;:&nbsp;</c:if><c:if test="${not empty task.groupLabel}">${task.groupLabel}&nbsp;<font color="#cccccc">|</font>&nbsp;</c:if>${task.group}&nbsp;<font color="#cccccc">|</font>&nbsp;</c:if>${task.name}</b></td>				
				<td style="text-align:right;">&nbsp;</td>
				</tr>
			</table>
			<%=line%>
			<div id="taskExecutionLogDiv">
				<div style="height:8px"></div>
				<table class="top"><tr><td><span id="taskExecutionLogTitle">&nbsp;[--] 수행로그</span></td><td style="text-align:right"><span id="taskExecutionLogLinkTop"></span></td></tr></table>
				<iframe name="taskExecutionLogFrame" id="taskExecutionLogFrame" style="width:1372px;height:150px;border:1px solid #e5e5e5;border-top:1px solid #9f9f9f;"></iframe>
				<table class="bottom"><tr><td><span id="taskExecutionLogOption"></span></td><td style="text-align:right"><span id="taskExecutionLogLinkBottom"></span></td></tr></table>
			</div>
			<div id="taskExceptionLogDiv" style="display:none">
				<div style="height:8px"></div>
				<table class="top"><tr><td><span id="taskExceptionLogTitle">&nbsp;[--] 수행오류</span></td><td style="text-align:right"><span id="taskExceptionLogLinkTop"></span></td></tr></table>			
				<iframe name="taskExceptionLogFrame" id="taskExceptionLogFrame" style="width:1372px;height:150px;border:1px solid #e5e5e5;border-top:1px solid #9f9f9f;"></iframe>
				<table class="bottom"><tr><td><span id="taskExceptionLogOption"></span></td><td style="text-align:right"><span id="taskExceptionLogLinkBottom"></span></td></tr></table>
			</div>
			<div id="taskExecutionEnvDiv" style="display:none">
				<div style="height:8px"></div>
				<table class="top"><tr><td><span id="taskExecutionEnvTitle">&nbsp;[--] 수행환경정보</span></td><td style="text-align:right"><span id="taskExecutionEnvLinkTop"></span></td></tr></table>
				<iframe name="taskExecutionEnvFrame" id="taskExecutionEnvFrame" style="width:1372px;height:100px;border:1px solid #e5e5e5;border-top:1px solid #9f9f9f;"></iframe>
				<table class="bottom"><tr><td style="text-align:right"><span id="taskExecutionEnvLinkBottom"></span></td></tr></table>
			</div>
			<c:if test="${logHistoryShrinkYn eq 'Y' && !empty taskExecutionList}">
			<%=line%>	
			<table>
				<tr>
				<td>
				<span style="font-family:verdana,sans-serif;font-size:14px;">
				<input type="button" style="width:120px;font-size:13px;vertical-align:middle;" value="수행이력삭제" onclick="task.clearHistory('${task.name}');"/>
				<label><input type="checkbox" style="vertical-align:middle;" id="deleteAllYn">전체</label>
				</span>
				</td>
				<td style="text-align:right;">
				<span style="font-family:verdana,sans-serif;font-size:14px;">
				<label><input type="checkbox" style="vertical-align:top;" id="executionEnvViewCheckYn" <c:if test="${executionEnvViewCheckYn eq 'Y'}">checked</c:if> onClick="task.envInfo()">수행환경정보</label>
				</span>
				</td>
				</tr>
			</table>
			</c:if>
		</div>
	</div>
	
	<div class="historyList">
		<br>
		<form id="taskExecutionListForm">
			<label><input type="checkbox" name="errorOnlyCheck" <c:if test="${executionListFilterErrorYn eq 'Y'}">checked</c:if>><sapn style="font-family:verdana,sans-serif;font-size:13px;">오류</label></sapn>&nbsp;&nbsp;
			<label><input type="checkbox" name="interruptOnlyCheck" <c:if test="${executionListFilterInterruptYn eq 'Y'}">checked</c:if>><sapn style="font-family:verdana,sans-serif;font-size:13px;">중단</label></sapn>&nbsp;&nbsp;
			<input type="text" name="executionListRownum" maxlength="3" value="${executionListRownum}" style="width:40px;height:10pt;font-size:13px;text-align:center;vertical-align:middle" onKeyDown="if(event.keyCode==13) {task.executionList('taskExecutionListForm');return false;}">
			<input type="button" value="조회" style="width:50px;font-size:13px;vertical-align:middle" onclick="task.executionList('taskExecutionListForm');"/>
		</form>
		<table>
		    <colgroup>
				<col width="150">
				<col width="180">
				<col width="200">
				<col width="200">
				<col width="110">
				<col width="110">
				<col width="110">
				<col>
		    </colgroup>  
			<tr>
				<th style="text-align:center">수행번호</th>
				<th style="text-align:center">수행서버</th>
				<th style="text-align:center">시작일시</th>
				<th style="text-align:center">종료일시</th>
				<th style="text-align:right"><span>수행시간<font size="-2">(ms)</font></span></th>	    
				<th style="text-align:right"><span>수행로그<font size="-2">(bytes)</font></span></th>
				<th style="text-align:center">오류/중단</th>	    
				<th style="text-align:left">수행프로퍼티</th>
			</tr>
			<c:if test="${!empty taskExecutionList}">
		    <c:forEach var="item" items="${taskExecutionList}" varStatus="status">
		    <tr onClick="fncLogTab('${item.executionNo}','<c:if test="${!empty item.executionLog}">Y</c:if>','${item.exceptionYn}')" onMouseOver="fncChangeTrColor(this)">
				<c:if test="${status.first}">
				<td style="text-align:center"><span id="${task.taskId}.autoReflashTaskExecutionNo">${item.executionNo}</span></td>
				<td style="text-align:center"><p x-ms-format-detection="none">${item.executionServer}</p></td>
				<td style="text-align:center">${item.executionStartTimeFormat}</td>
				<td style="text-align:center"><c:if test="${!empty item.executionEndTimeFormat}">${item.executionEndTimeFormat}</c:if><c:if test="${empty item.executionTime}"><span id="${task.taskId}.autoReflashStatus"></span></c:if></td>
				<td style="text-align:right"><c:if test="${!empty item.executionTime}"><fmt:formatNumber value="${item.executionTime}" groupingUsed="true"/></c:if><c:if test="${empty item.executionTime}"><span id="${task.taskId}.autoReflashTimeFlow"></span></c:if>&nbsp;</td>
				<td style="text-align:right"><span id="${task.taskId}.autoReflashLogSize"><c:if test="${!empty item.executionLog}"><fmt:formatNumber value="${item.executionLogSize}" groupingUsed="true"/></c:if><c:if test="${empty item.executionLog}">-</c:if></span>&nbsp;</td>
				<td style="text-align:center"><c:if test="${item.exceptionYn eq 'Y'}"><c:if test="${item.exceptionType eq 'error'}"><span style="color:red">오류</span></c:if><c:if test="${item.exceptionType eq 'interrupt'}"><span style="color:gray">중단</span></c:if></c:if><c:if test="${item.exceptionYn eq 'N'}">-</c:if>&nbsp;</td>
				<td style="text-align:center"><textarea id="${item.executionNo}.bizProps" readonly disabled onmouseover="this.style.borderColor='#FFC533'" onmouseout="this.style.borderColor='#606060'"></textarea></td>
				</c:if>
				<c:if test="${!status.first}">
				<td style="text-align:center">${item.executionNo}</td>
				<td style="text-align:center"><p x-ms-format-detection="none">${item.executionServer}</p></td>
				<td style="text-align:center">${item.executionStartTimeFormat}</td>
				<td style="text-align:center">${item.executionEndTimeFormat}</td>
				<td style="text-align:right"><c:if test="${!empty item.executionTime}"><fmt:formatNumber value="${item.executionTime}" groupingUsed="true"/></c:if><c:if test="${empty item.executionTime}">-</c:if>&nbsp;</td>
				<td style="text-align:right"><span id="${item.executionNo}.lazyLog"><c:if test="${!empty item.executionLog}"><fmt:formatNumber value="${item.executionLogSize}" groupingUsed="true"/></c:if><c:if test="${empty item.executionLog}">-</c:if></span>&nbsp;</td>		      
				<td style="text-align:center"><c:if test="${item.exceptionYn eq 'Y'}"><c:if test="${item.exceptionType eq 'error'}"><span style="color:red">오류</span></c:if><c:if test="${item.exceptionType eq 'interrupt'}"><span style="color:gray">중단</span></c:if></c:if><c:if test="${item.exceptionYn eq 'N'}">-</c:if></td>
				<td style="text-align:center"><textarea id="${item.executionNo}.bizProps" readonly disabled onmouseover="this.style.borderColor='#FFC533'" onmouseout="this.style.borderColor='#606060'"></textarea></td>
				</c:if>		    
				<script type="text/javascript">
				this.task.addJson("executionConfig",<c:out value="${item.executionNo}"/>,<c:out value="${item.executionConfig}" escapeXml="false"/><c:if test="${empty item.executionConfig}">null</c:if>);
				this.task.addJson("executionPostProcess",<c:out value="${item.executionNo}"/>,<c:out value="${item.executionPostProcess}" escapeXml="false"/><c:if test="${empty item.executionPostProcess}">null</c:if>);
				this.task.setValueAtElement("${item.executionNo}.bizProps",JSON.stringify(this.executionConfig[<c:out value="${item.executionNo}"/>].bizProps));
				</script>
		    </tr>
		    </c:forEach>
		    </c:if>
		    <c:forEach var="i" begin="1" end="${executionListRownum-fn:length(taskExecutionList)}" step="1">
		    <tr onMouseOver="fncChangeTrColor(this)">
				<td>&nbsp;<br>&nbsp;</td>
				<td></td>
				<td></td>
				<td></td>
				<td></td>
				<td></td>
				<td></td>
				<td></td>
		    </tr>
		    </c:forEach>	
		    <tr>    	
				<td colspan=8 style="border:1px solid #77aaaa;font-size:14px;text-align:left;padding:0px;color:#8899bb;height:2px;background-color:#8899bb;"></td>
			</tr>
		</table>
		<br>	
	</div>

	<form id="taskActionForm" method="post">						
		<input type="hidden" name="actionPage" value="${actionPage}"/>
		<input type="hidden" name="activeTab" value="${actionTab}"/>
		<input type="hidden" name="taskId" value="${task.taskId}">
		<input type="hidden" name="taskName" value="<c:out value="${task.name}"/>">
		<input type="hidden" name="taskChangeStamp" value="${taskChangeStamp}">
		<input type="hidden" name="configTarget">
		<input type="hidden" name="executionNo">
		<input type="hidden" name="enableYn">
		<input type="hidden" name="logEnableCheck">
		<input type="hidden" name="logType">
		<input type="hidden" name="logLevel">
		<input type="hidden" name="logFilePathPattern">
		<input type="hidden" name="logExceptEnableYn">
		<input type="hidden" name="logExceptLoggerPattern">
		<input type="hidden" name="logExceptCommandType">
		<input type="hidden" name="scheduleTermEnableYn">
		<input type="hidden" name="scheduleTermCommandType">
		<input type="hidden" name="scheduleTermValue">
		<input type="hidden" name="scheduleType">
		<input type="hidden" name="scheduleValue">
		<input type="hidden" name="propertyKey">		
		<input type="hidden" name="propertyValue">
		<input type="hidden" name="paramExtensionConfigCheckYn">
		<input type="hidden" name="outterOptionType">
		<input type="hidden" name="jobParametersApiUrl">
		<input type="hidden" name="jobParametersApiUrlOverlapYn">
		<input type="hidden" name="jobParametersApiUrlEnableYn">
		<input type="hidden" name="callbackApiUrl">
		<input type="hidden" name="callbackApiUrlEnableYn">
		<input type="hidden" name="jvmMemory">
		<input type="hidden" name="jvmMemoryEnableYn">
		<input type="hidden" name="deleteAllYn">
		<input type="hidden" name="detailViewCheckYn" value="${detailViewCheckYn}">		
		<input type="hidden" name="executionEnvViewCheckYn" value="${executionEnvViewCheckYn}">
		<input type="hidden" name="executionListRownum" value="${executionListRownum}">
		<input type="hidden" name="executionListFilterErrorYn" value="${executionListFilterErrorYn}">
		<input type="hidden" name="executionListFilterInterruptYn" value="${executionListFilterInterruptYn}">
	</form>	

	<script type="text/javascript">
	this.fncSetTab("<c:out value="${activeTab}"/>");
	this.scheduler.status(<c:out value="${schedulerInfo}" escapeXml="false"/>);
	this.task.setSaveImmediatelyYn("<c:out value="${task.taskConfig.saveImmediatelyYn}"/>");
	this.task.setDefaultConstant(this.scheduler.getDefaultConstant());
	this.task.status("<c:out value="${task.taskId}"/>","<c:out value="${task.enableYn}"/>","<c:out value="${task.hiddenYn}"/>","<c:out value="${task.deleteYn}"/>","<c:out value="${task.status}"/>","<c:out value="${task.countBaseTime}"/>");	
	</script>

	<!-- hidden Frame -->
	<c:if test="${loginMode eq 'innerView'}"><iframe width="997" height="0" style="border:none" src="${context}/doRefreshSession"></iframe></c:if>
	<c:if test="${autoReflashYn eq 'Y'}"><iframe width="1082" height="0" style="border:none" src="${schedulerRequestPath}/detail?meta=status"></iframe></c:if>
	
	</div>
  </body>

</html>
<aidTag:alertMessage/>
