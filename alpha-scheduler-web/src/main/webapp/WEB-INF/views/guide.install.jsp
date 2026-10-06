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
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%=systemTitle%></title>
    <link rel="icon" href="${context}/favicon.ico" type="image/x-icon">
    <style>
        body { font-size: 12px; font-family: Consolas; }
        table { border-collapse: collapse; width: 100%; font-size: 12px; }
        th { border: 1px solid black; padding: 8px; text-align: left; background-color: #f2f2f2; }
        td { border: 1px solid black; padding: 8px; text-align: left;}
    </style>
</head>
<body>
	<c:if test="${!empty packageInfo}">	
    <table>
        <thead>
            <tr>
                <th colspan=2>수행패키지 : ${packageInfo.name}</th>
            </tr>
        </thead>
        <tbody>
            <tr><td width=100>수행패키지</td><td>${packageInfo.path}</td></tr>   
            <tr><td>이름규칙</td><td>${packageInfo.comment}</td></tr>
            <tr><td>크기</td><td>${packageInfo.size}</td></tr>
            <tr><td>생성일시</td><td>${packageInfo.creationTime}</td></tr>
            <tr><td>수정일시</td><td>${packageInfo.lastModifiedTime}</td></tr>
            <tr><td>접근일시</td><td>${packageInfo.lastAccessTime}</td></tr>
        </tbody>
    </table>
	<br>
	</c:if>
	<c:if test="${!empty repositoryInfo}">	
    <table>
        <thead>
            <tr>
                <th colspan=2>배포패키지 : ${repositoryInfo.name}</th>
            </tr>
        </thead>
        <tbody>
            <tr><td width=100>배포패키지</td><td>${repositoryInfo.path}</td></tr>         
            <tr><td>이름규칙</td><td>${repositoryInfo.comment}</td></tr>
            <tr><td>크기</td><td>${repositoryInfo.size}</td></tr>
            <tr><td>생성일시</td><td>${repositoryInfo.creationTime}</td></tr>
            <tr><td>수정일시</td><td>${repositoryInfo.lastModifiedTime}</td></tr>
            <tr><td>접근일시</td><td>${repositoryInfo.lastAccessTime}</td></tr>
        </tbody>
    </table>
	<br>
	</c:if>
	<c:if test="${!empty buildInfo}">
    <table>
        <thead>
            <tr>
                <th colspan=2>빌드프로젝트 : ${buildInfo.project}</th>
            </tr>
        </thead>
        <tbody>
            <tr><td width=100>빌드번호</td><td>${buildInfo.no}</td></tr>
            <tr><td>빌드일시</td><td>${buildInfo.date}</td></tr>
            <tr><td>빌드프로파일</td><td>${buildInfo.profile}</td></tr>
            <tr><td>빌드버전</td><td>${buildInfo.version}</td></tr>
        </tbody>
    </table>
	<br>
	</c:if>
</body>
</html>
<%}%>