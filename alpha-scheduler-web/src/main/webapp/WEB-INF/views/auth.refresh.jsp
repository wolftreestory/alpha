<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html>
<head>
<title></title>
</head>
<body>
	maxInactiveInterval:${maxInactiveInterval}(s)
	<br> refreshSessionTimeout:${refreshSessionTimeout}(ms)
	<br>
	<script type="text/javascript">
	function fncRefreshSession() {window.location.reload(true);}
	var refreshSessionTimeout=${refreshSessionTimeout};
	setTimeout("fncRefreshSession()",refreshSessionTimeout);
	</script>
</body>
</html>
