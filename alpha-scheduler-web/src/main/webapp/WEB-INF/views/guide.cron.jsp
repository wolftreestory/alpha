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

SchedulerManager schedulerManager=BaseUtil.getBean(SchedulerManager.class);
String schedulerRequestPath=request.getContextPath()+"/"+schedulerManager.getSchedulerRequestPath();

String mode=(String)request.getAttribute("mode");
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
%>
<%
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// default
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
if(mode.equals("default")) { %>
<c:set var="context" value="<%=request.getContextPath()%>"/>
<html>
<head>
    <title><%=systemTitle%></title>
    <link rel="icon" href="${context}/favicon.ico" type="image/x-icon">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>스프링 CRON 표현식</title>
    <style>
        body {font-size: 12px;}
        table {border-collapse: collapse; width: 100%; font-size: 12px;font-family: Consolas;}
        th, td {border: 1px solid black; padding: 8px; text-align: left;}
        th {background-color: #f2f2f2;}
    </style>
</head>
<body>
    <h3>스프링 CRON 표현식 : 필수 6개의 각 필드로 구성</h3>
    <table>
        <tr>
            <th>필드명</th>
            <th>값의 허용 범위</th>
            <th>허용된 특수문자</th>
        </tr>
        <tr>
            <td>초 (Seconds)</td>
            <td>0 ~ 59</td>
            <td>- * /</td>
        </tr>
        <tr>
            <td>분 (Minutes)</td>
            <td>0 ~ 59</td>
            <td>- * /</td>
        </tr>
        <tr>
            <td>시 (Hours)</td>
            <td>0 ~ 23</td>
            <td>- * /</td>
        </tr>
        <tr>
            <td>일 (Day of Month)</td>
            <td>1 ~ 31</td>
            <td>- * ? / L W</td>
        </tr>
        <tr>
            <td>월 (Month)</td>
            <td>1 ~ 12 or JAN ~ DEC</td>
            <td>- * /</td>
        </tr>
        <tr>
            <td>요일 (Day of Week)</td>
            <td>0 ~ 6 or SUN ~ SAT</td>
            <td>- * ? / L #</td>
        </tr>
        <tr>
            <td>년 (Year)</td>
            <td>생갹가능 or 1970 ~ 2099</td>
            <td>- * /</td>
        </tr>
    </table>

    <h3>특수문자 설명</h3>
	<ul style="list-style-type: none; font-size: 14px; font-family: Consolas;">
	    <li><strong>*</strong> : 모든 값 (예: "0 * * * * *" → 매분)</li>
	    <li><strong>,</strong> : 여러 값 지정 (예: "0 0 9,18 * * *" → 매일 오전 9시와 오후 6시)</li>
	    <li><strong>-</strong> : 범위 지정 (예: "0 0 9-17 * * *" → 매일 오전 9시부터 오후 5시까지)</li>
	    <li><strong>/</strong> : 주기 지정 (예: "0 */10 * * * *" → 10분 간격)</li>
	    <li><strong>?</strong> : 특정 값 없음 (예: "0 0 12 ? * MON" → 매주 월요일 정오)</li>
	    <li><strong>L</strong> : 마지막 값 (예: "0 0 12 L * ?" → 매월 마지막 날 정오)</li>
	    <li><strong>W</strong> : 가장 가까운 평일 (예: "0 0 9 15W * ?" → 매월 15일에서 가장 가까운 평일 오전 9시)</li>
	    <li><strong>#</strong> : 특정 요일 지정 (예: "0 0 9 * * 3#2" → 매월 두 번째 수요일 오전 9시)</li>
	</ul>
	
    <h3>예제 표현식</h3>
	<table>
    <tr>
        <th>CRON 표현식</th>
        <th>설명</th>
    </tr>
    <tr>
        <td>0 0/1 * * * *</td>
        <td>모든 요일, 매월, 매일 1분마다 0초</td>
    </tr>
    <tr>
        <td>0 0 12 * * *</td>
        <td>모든 요일, 매월, 매일 12:00:00</td>
    </tr>
    <tr>
        <td>0 15 10 * * *</td>
        <td>모든 요일, 매월, 아무 날이나 10:15:00</td>
    </tr>
    <tr>
        <td>0 * 14 * * *</td>
        <td>모든 요일, 매월, 매일, 14시 매분 0초</td>
    </tr>
    <tr>
        <td>0 0/5 14 * * *</td>
        <td>모든 요일, 매월, 매일, 14시 매 5분마다 0초</td>
    </tr>
    <tr>
        <td>0 0/5 14,18 * * *</td>
        <td>아무 요일, 매월, 매일, 14시, 18시 매 5분마다 0초</td>
    </tr>
    <tr>
        <td>0 0-5 14 * * *</td>
        <td>아무 요일, 매월, 매일, 14:00 부터 14:05까지 매 분 0초</td>
    </tr>
    <tr>
        <td>0 10,44 14 ? 3 WED</td>
        <td>3월의 매 주 수요일, 아무 날짜나 14:10:00, 14:44:00</td>
    </tr>
    <tr>
        <td>0 15 10 ? * MON-FRI</td>
        <td>월~금, 매월, 아무 날이나 10:15:00</td>
    </tr>
    <tr>
        <td>0 15 10 15 * ?</td>
        <td>아무 요일, 매월 15일 10:15:00</td>
    </tr>
    <tr>
        <td>0 15 10 L * ?</td>
        <td>아무 요일, 매월 마지막 날 10:15:00</td>
    </tr>
    <tr>
        <td>0 15 10 ? * 6L</td>
        <td>매월 마지막 금요일 아무 날이나 10:15:00</td>
    </tr>
    <tr>
        <td>0 15 10 ? * 6#3</td>
        <td>매월 3번째 금요일 아무 날이나 10:15:00</td>
    </tr>
</table>
<br>
</body>
</html>
<%}%>
<%
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// time
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
if(mode.equals("timetable")) { %>
<c:set var="context" value="<%=request.getContextPath()%>"/>
<html>
<head>
    <title><%=systemTitle%></title>
    <link rel="icon" href="${context}/favicon.ico" type="image/x-icon">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>스프링 CRON 수행시간표</title>
    <style>
        body {font-size: 13px;}
        table {border-collapse: collapse; padding:2px; width: 100%; font-size: 13px;font-family: Consolas;}
        th, td {border: 1px solid black; padding: 8px; text-align: center;}
        th {background-color: #f2f2f2;}
		tr:first-child {font-weight: bold;}
		.listControl {text-align:center;}
    </style>
</head>
<script>
    function changePage(pageNo) {
        window.location.href = '<%=schedulerRequestPath%>/guide?type=cron&mode=timetable&taskId=${taskId}&pageNo='+pageNo;
    }
</script>
<body>
    <table>
	<thead>    
    <tr><th width=60%>수행일시</th><th>남은시간</th></tr>
    </thead>
    <tbody>    
	<c:forEach var="item" items="${timeList}">	
	<tr><td>${item.executionTime}</td><td>${item.remainingTime}</td></tr>
	</c:forEach>
	</tbody>
	</table>
	<div class="listControl">	
	<input type="button" value="&lt;&lt;" style="width:50px;font-size:8px" onclick="changePage(${prevPageNo})" ${firstYn eq 'Y' ? 'disabled' : ''} />
	<input type="button" value="&gt;&gt;" style="width:50px;font-size:8px" onclick="changePage(${nextPageNo})" ${lastYn eq 'Y' ? 'disabled' : ''}/>
	</div>
</body>
</html>
<%}%>

