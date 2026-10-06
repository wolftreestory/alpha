<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.alpha.base.BaseUtil"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="aidTag" uri="/WEB-INF/tlds/aidTagLib.tld" %>    
<%
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
String systemEnvMode=BaseUtil.getPropertiesUtil().getProperty("spring.profiles.active");
String systemTitle=BaseUtil.getPropertiesUtil().getProperties().getProperty("alpha.batch.webConsole.title","BATCH");
String systemTitleHtml=BaseUtil.getPropertiesUtil().getProperties().getProperty("alpha.batch.webConsole.titleHtml","console");

if(!systemEnvMode.equals("prod")) {systemTitle=systemTitle+" ("+systemEnvMode+")";}
if(systemEnvMode.equals("prod")) {systemEnvMode="";}
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
%>
<c:set var="context" value="<%=request.getContextPath()%>"/>
<html>
  <head>
    <title><%=systemTitle%></title>
    <link rel="icon" href="${context}/favicon.ico" type="image/x-icon">    
	<style>
	.loginForm {}
	.loginForm table.line {width:300px;height:1px;border:0px;border-spacing:0px;padding:0px;background-color:#acacac;margin-left:auto;margin-right:auto;}
	.loginForm td.line {height:1px;border:0px;padding:0px;}	
	.loginForm table.input {width:300px;height:1px;border:0px;border-spacing:0px;padding:3px;;margin-left:auto;margin-right:auto;}
	.loginForm td.input {width:80px;text-align:right;font-family:verdana,sans-serif;font-size:12px;}
	.loginForm input {font-size:12px;}
	.loginForm input.userId {width:150px;height:18px}
	.loginForm input.userPw {width:150px;height:18px}
	.loginForm input.submit {width:158px;vertical-align:middle"}
	
	.authKey span {font-family:consolas;font-size:9px;color:#000000;}
	.authKey input.type1 {width:80px;height:15px;border:none;border-radius:5px;padding-left:10px;padding-right:10px;font-family:consolas;font-size:10px;color:#555555;background-color:#efefef;vertical-align:middle;text-align:center;}	
	</style>
</head>	
<body>
<br>
<br>
<br>
<br>
<div style="width:100%;text-align:center;">
<span style="font-family:verdana,sans-serif;font-size:25px;font-weight:bold;"><%=systemTitleHtml%> <%=systemEnvMode%></span>
</div>
<br>
<div class="loginForm">
	<form id="form" method="post" action="./doLogin">
	<input type="hidden" name="authKey" class="authKey">

	<table class="line"><tr><td class="line"></td></tr></table>
	<table class="input"><tr><td class="input">아이디:</td><td><input type="text" name="userId" class="userId"></td></tr></table>

	<table class="line"><tr><td class="line"></td></tr></table>
	<table class="input"><tr><td class="input">비밀번호:</td><td><input type="password" name="userPw" class="userPw"></td></tr></table>

	<table class="line"><tr><td class="line"></td></tr></table>
	<table class="input"><tr><td class="input"></td><td><input type="submit" value="로그인" class="submit"></td></tr></table>
	</form>
</div>

<c:if test="${systemEnvMode eq 'local' or systemEnvMode eq 'dev'}">
<div id="authkeyDiv" class="authKey">
	<script type="text/javascript">
	var doAction=function(authKey){
		var targetForm=document.getElementById("form");
		targetForm["authKey"].value=authKey;
		targetForm.submit();
	}
	</script>
	<br>
	<center>
	<c:forEach var="item" items="${authKeyList}" varStatus="status">
	<c:if test="${!(systemEnvMode eq 'dev' and item.userId eq 'admin')}">	
	<input type="button" value='<c:out value="${item.userId}"/>' class="type1" onclick='doAction("<c:out value="${item.authKey}"/>")'/>&nbsp;
	</c:if>
	</c:forEach>
	</center>
</div>
</c:if>
	
</body>
</html>

<aidTag:alertMessage/>