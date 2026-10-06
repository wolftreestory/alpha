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
    <link rel="icon" href="/batch/favicon.ico" type="image/x-icon">
	<style>

	.line {width:100%;}
	.line table.table {width:100%;height:1px;border:0px;padding:0px;margin-left:auto;margin-right:auto;background-color:#ffffff;}
	.line tr.tr {border:none;}	
	.line td.td1 {height:7px;border:0px;padding:0px;background-color:#ffffff;}	
	.line td.td2 {height:1px;border:0px;padding:0px;background-color:#acacac;}	

	.content {width:900px;display:block;padding:12px 12px;border:1px solid #ccc;border-top:none;}

	.content1 {width:100%;text-align:left}
	.content1 table {width:100%;border:none;padding:0px;font-family:verdana,sans-serif;border-collapse:collapse;}
	.content1 tr {border-top:1px solid #dfdfdf;border-bottom:1px solid #dfdfdf;}
	.content1 tr:last-child {border-bottom:none;}
	.content1 td {border:none;padding:none;color:#000000;height:24px;font-size:14px;text-align:left;}
	.content1 input {font-family:verdana,sans-serif;font-size:13px;vertical-align:middle;}
		
	</style>  
	  
	<script type="text/javascript">

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	</script>
  </head>
  <body>
  
	<div class=content>
		<div class="content1">
			<table>
				<tr>
					<td style="width:150px;">타스크그룹</td>
					<td><input type="text"></td>
				</tr>			
				<tr>
					<td style="width:150px;">타스크이름</td>
					<td><input type="text"></td>
				</tr>
				<tr>
					<td style="width:150px;">타스크설명</td>
					<td><input type="text"></td>
				</tr>
				<tr>
					<td style="width:150px;">수행스케쥴</td>
					<td><input type="text"></td>
				</tr>
 			</table>
			
			<%=line%>
		</div>
	</div>
  
  </body>
  
</html>
<aidTag:alertMessage/>
  