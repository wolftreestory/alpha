class DetailClassBuilder {
	
	SchedulerClass=function() {
		let mainObj=null;
		let basePath=null;
		let schedulerInfo=null;
		
		if(arguments.length===1) {this.mainObj=arguments[0];}			
		if(arguments.length===2) {this.mainObj=arguments[0];this.basePath=arguments[1];}
	
		this.consoleLog=function() {
			console.log(mainObj);
			console.log(basePath);
			console.log(schedulerInfo);		
		};			
		this.init=function(mainObj) {this.mainObj=mainObj;};
		this.getSchedulerInfo=function() {return this.schedulerInfo;};		
		this.getDefaultConstant=function() {return this.schedulerInfo["defaultConstant"];};		
		
		this.status=function(schedulerInfo) {
			this.schedulerInfo=schedulerInfo;

			let status="";
			status+="&nbsp;";
			status+="서버:";
			status+="<span style=\"font-weight:bold;\">";
			status+=this.schedulerInfo.serverIpPort;
			status+="</span>";
			status+="&nbsp;";			
			status+="<span style=\"font-size:11px;color:#999999\">(";				
			status+=(this.schedulerInfo.serverInstanceCode!=""?this.schedulerInfo.serverInstanceCode:"-");
			status+=")</span>";				
			status+=",&nbsp;";	
			status+="상태:";
			if(this.schedulerInfo.schedulerStatus=="") {status+="-";}
			if(this.schedulerInfo.schedulerStatus=="start") {status+="<span style=\"color:blue;font-weight:bold\">start</span>";}
			if(this.schedulerInfo.schedulerStatus=="stop") {status+="<span style=\"color:red;font-weight:bold\">stop</span>";}
			status+="&nbsp;";
			status+="<span style=\"font-size:11px;color:#444444\">(";
			status+=(this.schedulerInfo.schedulerStartTime==null?"start:-":"start:"+this.schedulerInfo.schedulerStartTime);
			status+=",&nbsp;";	
			status+=(this.schedulerInfo.schedulerStopTime==null?"stop:-":"stop:"+this.schedulerInfo.schedulerStopTime);
			status+=")</span>";
			status+=",&nbsp;";	
			status+="접속:";	
			status+=("<span style=\"font-weight:bold\">"+this.schedulerInfo.userInfo.userId+"</span>");
			status+=(this.schedulerInfo.userInfo.description==null?"":"&nbsp;<span style=\"font-size:11px;color:#444444\">("+this.schedulerInfo.userInfo.description+")</span>");

			document.getElementById("schedulerStatus").innerHTML=status;
			
			status="";
			if(this.schedulerInfo.schedulerStatus=="start") {status+=" : <span style=\"color:red;font-weight:bold\">변경불가능</span>";}
			if(this.schedulerInfo.schedulerStatus=="stop") {
				status+=" : <span style=\"color:green;font-weight:bold\">변경가능</span>";				
			}
			document.getElementById("schedulerChangeEnable").innerHTML=status;
			
			return;
		};
	};
	
	TaskClass=function() {
		let mainObj=null;
		let basePath=null;
		let executionListRownum=5;
		let saveImmediatelyYn=null;
		let defaultConstant=null;
		
		if(arguments.length===1) {this.mainObj=arguments[0];}			
		if(arguments.length===2) {this.mainObj=arguments[0];this.basePath=arguments[1];}
		if(arguments.length===3) {this.mainObj=arguments[0];this.basePath=arguments[1];this.executionListRownum=arguments[2];}

		this.consoleLog=function(){
			console.log(mainObj);
			console.log(basePath);
			console.log(executionListRownum);		
			console.log(executionListRownum);		
			console.log(saveImmediatelyYn);		
			console.log(defaultConstant);	
		};
		this.init=function(mainObj) {this.mainObj=mainObj;};
		this.setSaveImmediatelyYn=function(saveImmediatelyYn) {this.saveImmediatelyYn=saveImmediatelyYn;};
		this.setDefaultConstant=function(defaultConstant) {this.defaultConstant=defaultConstant;};
		this.setValueAtElement=function(elementId,value) {if (value === undefined) {value="";}	document.getElementById(elementId).value=value;};
		this.setHtmlAtElement=function(elementId,html) {document.getElementById(elementId).innerHTML=html;};			
		this.getConfigValue=function(executionNo,key) {return this.getConfig(executionNo)[key];};
		this.setConfigValue=function(executionNo,key,value) {this.getConfig(executionNo)[key]=value;};			
		this.getConfig=function(executionNo) {
			let configInfo=this.getJson("configInfo",executionNo);
			if(configInfo!=null) {return configInfo;}
			this.addJson("configInfo",executionNo,JSON.parse(JSON.stringify(this.defaultConstant))); //deepCopy
			return this.getConfig(executionNo);
		};
		this.addJson=function(name,executionNo,value) {
			if(executionNo==null || value==null) {return;}
			if(typeof(executionNo)!="string") {executionNo=String(executionNo);}
			if(typeof(value)!="object") {value=JSON.parse(value);}
			if(typeof(value)=="object") {value=JSON.parse(JSON.stringify(value));} //deepCopy
			if(this.mainObj[name]==null) {this.mainObj[name]= {};}
			this.mainObj[name][executionNo]=value;	
		};
		this.getJson=function(name,executionNo) {
			if(executionNo==null) {return;}
			if(typeof(executionNo)!="string") {executionNo=String(executionNo);}
			if(this.mainObj[name]==null) {return null;}
			return this.mainObj[name][executionNo];
		};
		this.sortJson=function(obj) {
		  if (obj === null || typeof obj !== "object") {return obj;}
		  if (Array.isArray(obj)) {
		    const newArr = obj.map(el => this.sortJson(el));
		    return newArr.sort((a, b) => {
		      if (typeof a === "number" && typeof b === "number") {return a - b;}
		      const sa = JSON.stringify(a);
		      const sb = JSON.stringify(b);
		      return sa < sb ? -1 : sa > sb ? 1 : 0;
		    });
		  }
		  const ordered = {};
		  Object.keys(obj).sort().forEach(key => {ordered[key] = this.sortJson(obj[key]);});
		  return ordered;
		};
		this.toggleDiv=function(baseTitle,refToggleId,refToggleDiv1Id,refToggleDiv2Id) {
			let toggleTag=document.getElementById(refToggleId);
			let toggleDiv1=document.getElementById(refToggleDiv1Id);
			let toggleDiv2=document.getElementById(refToggleDiv2Id);
			
			if(toggleDiv1.style.display=="none") {toggleDiv1.style.display="block";toggleDiv2.style.display="none";toggleTag.innerHTML=baseTitle+"&nbsp;+&nbsp;";return;}
			if(toggleDiv2.style.display=="none") {toggleDiv2.style.display="block";toggleDiv1.style.display="none";toggleTag.innerHTML=baseTitle+"&nbsp;-&nbsp;";return;}
			return;
		};			
		this.getActionForm=function(action) {
			let targetForm=document.getElementById("taskActionForm");
			targetForm.action=this.basePath+action;
			return targetForm;
		};
		this.changeStamp=function(changeStamp) {
			let targetForm=document.getElementById("taskActionForm");
			targetForm["taskChangeStamp"].value=changeStamp;
			return;
		};	
		this.enable=function(taskName,enableYn) {
			if(!confirm("\""+taskName+"\"을(를) "+(enableYn=="Y"?"enable":"disable")+"합니다.")) {return;}
			const targetForm=this.getActionForm("/enable");
			targetForm["enableYn"].value=enableYn;
			targetForm.submit();
			return;
		};
		this.command=function(element,taskName) {
			if(element.value=="실행") {this.execute(taskName);}
			if(element.value=="중단") {this.interrupt(taskName);}
			return;
		};
		this.execute=function(taskName) {
			if(!confirm("\""+taskName+"\"을(를) 수행합니다.")) {return;}
			const targetForm=this.getActionForm("/execute");
			targetForm.submit();
			return;
		};
		this.interrupt=function(taskName) {
			if(!confirm("\""+taskName+"\"을(를) 중단합니다.")) {return;}
			const targetForm=this.getActionForm("/interrupt");
			targetForm.submit();
			return;				
		};
		this.resetCount=function() {
			if(!confirm("집계카운트 및 기준일을 초기화합니다.")) {return;}
			const targetForm=this.getActionForm("/resetCount");
			targetForm.submit();
			return;
		};			
		this.reflash=function(taskId) {
			const targetForm=this.getActionForm("/detail");
			for(let i=0;i<targetForm.elements.length;i++) {targetForm.elements[i].value="";}			
			targetForm["taskId"].value=taskId;
			targetForm.submit();
			return;
		};
		this.checkboxLink=function(refFormName,checkboxId){
			let targetObj=document.getElementById(refFormName)[checkboxId];
			targetObj.checked=!targetObj.checked;
			return;
		};
		this.executionList=function(refFormName,executionListRownum) {
			const targetForm=this.getActionForm("/detail");
			targetForm["executionListFilterErrorYn"].value=(document.getElementById(refFormName)["errorOnlyCheck"].checked?"Y":"N");
			targetForm["executionListFilterInterruptYn"].value=(document.getElementById(refFormName)["interruptOnlyCheck"].checked?"Y":"N");
			if(arguments.length==1) {targetForm["executionListRownum"].value=document.getElementById(refFormName)["executionListRownum"].value;}		
			if(arguments.length==2) {targetForm["executionListRownum"].value=executionListRownum;targetForm["executionListFilterErrorYn"].value="N";}
			if(isNaN(targetForm["executionListRownum"].value)) {alert("숫자를 입력하세요.");return;}
			targetForm.submit();
			return;
		};
		this.executionListReflash=function(taskId,taskStatus,autoReflashStep,loggedExecutionNo,lazyPostProcessEndYn) {
			if(this.lastTaskStatus==null) {this.lastTaskStatus=taskStatus;}
			if(this.lastAutoReflashStep==null) {this.lastAutoReflashStep=autoReflashStep;}
			let checkType="";
			let isEnable=false;
			if(!isEnable && (autoReflashStep=="" && document.getElementById(taskId+".autoReflashLogSize")==null))  {isEnable=true;checkType="0";} //fast-completion
			if(!isEnable && (autoReflashStep=="" && document.getElementById(taskId+".autoReflashLogSize").innerHTML=="-"))  {isEnable=true;checkType="0";} //fast-completion
			if(!isEnable && (autoReflashStep=="run" && document.getElementById(taskId+".autoReflashStatus")==null)) {isEnable=true;checkType="1";console.log("checkType"+checkType);} //run-start		
			if(!isEnable && (autoReflashStep=="run" && this.lastTaskStatus!=taskStatus)) {isEnable=true;checkType="2";} //run-end
			if(!isEnable && (this.lastAutoReflashStep!=autoReflashStep)) {isEnable=true;checkType="3";} //log-start	
			if(!isEnable && (autoReflashStep=="log" && document.getElementById(taskId+".autoReflashTaskExecutionNo").innerHTML==loggedExecutionNo)) {isEnable=true;checkType="4";} //log-end
			if(!isEnable && (autoReflashStep=="lazy" && lazyPostProcessEndYn=="Y")) {isEnable=true;checkType="5";} //lazy-end
			
			this.lastTaskStatus=taskStatus;
			this.lastAutoReflashStep=autoReflashStep;				
			if(!isEnable) {return;}
			
			this.executionList("taskExecutionListForm",Number(this.executionListRownum)>5?5:this.executionListRownum);
			return;
		};
		this.lazyLogReflash=function(lazyLogInfo){
			let baseInfo=this.lazyLogReflash.currentInfo!=null?this.lazyLogReflash.currentInfo:lazyLogInfo;
			for(const key in baseInfo){
				let refElement=document.getElementById(key+".lazyLog");
				if(refElement!=null){refElement.innerHTML=lazyLogInfo[key]==null?baseInfo[key]:"<font color=blue>"+lazyLogInfo[key]+"</font>";}
			}
			this.lazyLogReflash.currentInfo=lazyLogInfo;
			return;
		};
		this.status=function(taskId,taskEnableYn,taskHiddenYn,taskDeleteYn,taskStatus,taskCountBaseTime,timeFlow,executionLogSize) {
			let addRunningDots=function(base,str,maxDot) {
				if(str=="") {return "";}
				let addDots="";
				let dotsCount=str.split(".").length;
				if(dotsCount>maxDot) {dotsCount=1;}
				for(let i=0;i<dotsCount;i++) {addDots+=".";}
				return base+addDots;
			};
			let getTaskCountBaseTime=function(value) {
				let getSpanHtml=function(id,text,option) {return "<span id=\""+id+"\" "+option+">"+text+"</span>";};
				if(value=="") {return "";}
				let html="<font size=\"-1\">";
				html+="[ 카운트시작일시 : "+value+" ]&nbsp;";
				html+=getSpanHtml("taskCountBaseTimeReset","reset","style=\"cursor:default\" onMouseOver=\"this.style.color='blue'\" onMouseOut=\"this.style.color=''\"");
				html+="</font>";
				return html;
			};
			
			let status="";
			if(taskEnableYn=="N") {status+="disable";}
			if(taskEnableYn=="Y") {status+="<font color=green><b>enable</b></font>";}
			if(taskHiddenYn=="Y") {status+="&nbsp;|&nbsp;<font color=#D3D3D3><b>hidden</b></font>";}
			if(taskDeleteYn=="Y") {status+="&nbsp;|&nbsp;<font color=red><b>delete</b></font>";}
			if(taskStatus=="running") {status+=("&nbsp;/&nbsp;<font color=blue><b>running"+addRunningDots("",document.getElementById(taskId+".status").innerHTML,3)+"</b></font>");}
			document.getElementById(taskId+".status").innerHTML=status;
			document.getElementById(taskId+".command").value=(taskStatus=="running"?"중단":"실행");					

			let autoReflashStatus=document.getElementById(taskId+".autoReflashStatus");		
			if(autoReflashStatus!=null && taskStatus!="running") {autoReflashStatus.innerHTML="";}
			if(autoReflashStatus!=null && taskStatus=="running") {autoReflashStatus.innerHTML="<font color=blue><b>running"+addRunningDots("",autoReflashStatus.innerHTML,3)+"</b></font>";}
			
			if(arguments.length>=7 && timeFlow!="") {
				let autoReflashTimeFlow=document.getElementById(taskId+".autoReflashTimeFlow");
				if(autoReflashTimeFlow!=null && taskStatus!="running") {autoReflashTimeFlow.innerHTML="";}
				if(autoReflashTimeFlow!=null && taskStatus=="running") {autoReflashTimeFlow.innerHTML="<font color=blue><b>"+timeFlow+"</b></font>";}
			}
			
			if(arguments.length>=8 && executionLogSize!="") {
				let autoReflashLogSize=document.getElementById(taskId+".autoReflashLogSize");
				if(autoReflashLogSize!=null && taskStatus!="ready") {autoReflashLogSize.innerHTML="-";}
				if(autoReflashLogSize!=null && taskStatus=="ready") {autoReflashLogSize.innerHTML=autoReflashLogSize.innerHTML.indexOf(executionLogSize)==-1?"<font color=blue><b>"+executionLogSize+"</b></font>":executionLogSize;}
			}
			let taskObj=this;
			document.getElementById("taskCountBaseTime").innerHTML=getTaskCountBaseTime(taskCountBaseTime);
			if(document.getElementById("taskCountBaseTimeReset")!=null) {
				document.getElementById("taskCountBaseTimeReset").onclick=function() {taskObj.resetCount();};				
			}				
			return;
		};
		this.detailView=function(obj) {
			let targetObj=document.getElementById("detailViewCheckYn");
			if(obj.type!="checkbox"){targetObj.checked=!targetObj.checked;}
			const targetForm=this.getActionForm("/detail");
			targetForm["detailViewCheckYn"].value=(targetObj.checked?"Y":"N");	
			targetForm.submit();
			return;
		};
		this.configInit=function(info){
			if(!confirm((info+"을(를) 소스코드 설정으로 초기화 합니다.").trim())) {return;}
			const targetForm=this.getActionForm("/config/init");
			targetForm.submit();
			return;
		};
		this.configLoad=function(info,configTarget) {
			if(!confirm((info+"을 저장한 값으로 초기화 합니다.").trim())) {return;}
			const targetForm=this.getActionForm("/config/load");
			targetForm["configTarget"].value=configTarget;
			targetForm.submit();
			return;
		};
		this.configSave=function(info,configTarget) {
			if(!confirm((info+"을 저장합니다.\n초기화/서버기동시에 저장한 값으로 구성됩니다.").trim())) {return;}
			const targetForm=this.getActionForm("/config/save");
			targetForm["configTarget"].value=configTarget;
			targetForm.submit();
			return;
		};
		this.configLogEnable=function(refKeyId) {				
			let addMsg=this.saveImmediatelyYn=="N"?"\n설정파일에 적용시 저장을 해야 합니다.":"";		
			if(!confirm("변경사항을 적용합니다."+addMsg)) {if(document.getElementById(refKeyId).type=="checkbox") {document.getElementById(refKeyId).checked=!document.getElementById(refKeyId).checked;}return;}				
			const targetForm=this.getActionForm("/config/logEnable");
			targetForm["logEnableCheck"].value=document.getElementById(refKeyId).checked;
			targetForm.submit();		
			return;
		};
		this.configLogFileLevel=function(logType,refFileValueId,refFileExclusiveValueId,refLevelValueId) {				
			if(document.getElementById(refFileValueId).value.trim()=="") {document.getElementById(refFileValueId).value="";return;}
			if(document.getElementById(refLevelValueId).value.trim()=="") {document.getElementById(refLevelValueId).value="";return;}				

			let logFilePathPattern=document.getElementById(refFileValueId).value.trim();
			let logFileExclusive=document.getElementById(refFileExclusiveValueId).value.trim();
			if(logFilePathPattern.split("*").length!=2) {alert("파일패턴에 '*'는 한개만 적용 가능합니다.");return;}				
			if(logFilePathPattern.substring(logFilePathPattern.lastIndexOf("."),logFilePathPattern.length)!=".log") {alert("파일확장자가 올바르지 않습니다.\n\n확장자 : log");return;}
			if(logFilePathPattern.toLowerCase()==logFileExclusive.toLowerCase()) {alert("실행로그와 오류로그의 파일패턴은 서로 달라야합니다.\n\n대소문자구분 없음");return;}
			
			let logLevel=document.getElementById(refLevelValueId).value.trim().toLowerCase();				
			let definedLevels=(logType=="executionLog"?"|trace|debug|info|":"|warn|error|");
			if(definedLevels.indexOf("|"+logLevel+"|")==-1) {alert("로그레벨이 올바르지 않습니다.\n\n실행로그레벨 : trace, debug, info\n오류로그레벨 : warn, error");return;}
			
			let addMsg=this.saveImmediatelyYn=="N"?"\n설정파일에 적용시 저장을 해야 합니다.":"";		
			if(!confirm("변경사항을 적용합니다."+addMsg)) {return;}
			const targetForm=this.getActionForm("/config/logFileLevel");
			targetForm["logType"].value=logType;
			targetForm["logFilePathPattern"].value=logFilePathPattern;
			targetForm["logLevel"].value=logLevel;
			targetForm.submit();
			return;
		};
		this.configLogExceptEnable=function(refId,obj) {
			let targetObj=document.getElementById(refId);
			if(obj!=null && obj.type!="checkbox"){targetObj.checked=!targetObj.checked;}				
			let addMsg=this.saveImmediatelyYn=="N"?"\n설정파일에 적용시 저장을 해야 합니다.":"";		
			if(!confirm("변경사항을 적용합니다."+addMsg)) {if(targetObj.type=="checkbox") {targetObj.checked=!targetObj.checked;}return;}
			const targetForm=this.getActionForm("/config/logExceptEnable");
			targetForm["logExceptEnableYn"].value=targetObj.checked?"Y":"N";
			targetForm.submit();		
			return;
		};
		this.configLogExceptPattern=function(commandType,refValueId) {
			if(document.getElementById(refValueId).value.trim()=="") {document.getElementById(refValueId).value="";return;}
			let addMsg=this.saveImmediatelyYn=="N"?"\n설정파일에 적용시 저장을 해야 합니다.":"";		
			if(!confirm("변경사항을 적용합니다."+addMsg)) {return;}
			const targetForm=this.getActionForm("/config/logExceptPattern");
			targetForm["logExceptCommandType"].value=commandType;
			targetForm["logExceptLoggerPattern"].value=document.getElementById(refValueId).value.trim();
			targetForm.submit();		
			return;
		};
		this.configLogPolicyProps=function(propertyKey,refId,obj) {
			let targetObj=document.getElementById(refId);
			if(obj!=null && obj.type!="checkbox"){targetObj.checked=!targetObj.checked;}
			let addMsg=(this.saveImmediatelyYn=="N"?"\n설정파일에 적용시 저장을 해야 합니다.":"");		
			if(!confirm("변경사항을 적용합니다."+addMsg)) {if(targetObj.type=="checkbox") {targetObj.checked=!targetObj.checked;}return;}									
			const targetForm=this.getActionForm("/config/logPolicyProps");
			let propertyValue=targetObj.value;
			if(targetObj.type=="checkbox") {propertyValue=targetObj.checked?"Y":"N";}
			if(propertyKey=="logHistoryShrinkBaseDays" && propertyValue!="" && (isNaN(propertyValue) || Number(propertyValue)<=0)) {alert("days에 숫자를 입력하세요.\n 0보다 커야합니다.\n 미적용시 blank");return;}
			if(propertyKey=="logHistoryShrinkBaseRows" && propertyValue!="" && (isNaN(propertyValue) || Number(propertyValue)<=0)) {alert("rows에 숫자를 입력하세요.\n 0보다 커야합니다.\n 미적용시 blank");return;}								
			if(propertyValue=="") {propertyValue="-99";}				
			targetForm["propertyKey"].value=propertyKey;
			targetForm["propertyValue"].value=propertyValue;
			targetForm.submit();		
			return;
		};
		this.configScheduleTermEnable=function(refId,obj) {
			let targetObj=document.getElementById(refId);
			if(obj!=null && obj.type!="checkbox"){targetObj.checked=!targetObj.checked;}
			let aidMsg=targetObj.checked?"수행 주기는 설정된 기간을 기준으로 적용됩니다.":"수행 주기에 기간 제한을 해제합니다.";
			let addMsg=this.saveImmediatelyYn=="N"?"\n설정파일에 적용시 저장을 해야 합니다.":"";
			if(!confirm(aidMsg+addMsg)) {if(targetObj.type=="checkbox") {targetObj.checked=!targetObj.checked;}return;}
			const targetForm=this.getActionForm("/config/scheduleTermEnable");
			targetForm["scheduleTermEnableYn"].value=targetObj.checked?"Y":"N";
			targetForm.submit();		
			return;
		};
		this.configScheduleTerm=function(commandType,sRefValueId,eRefValueId) {
			if(document.getElementById(sRefValueId).value.trim()=="") {document.getElementById(sRefValueId).value="";return;}
			if(document.getElementById(eRefValueId).value.trim()=="") {document.getElementById(eRefValueId).value="";return;}				
			if(commandType=="add"){
				const nowDate = new Date(); nowDate.setHours(0,0,0,0);
				const startDate = new Date(document.getElementById(sRefValueId).value); startDate.setHours(0,0,0,0);
				const endDate = new Date(document.getElementById(eRefValueId).value); endDate.setHours(0,0,0,0);								
				if(startDate < nowDate || endDate < nowDate) {alert("오늘 이후 날짜를 입력해야 합니다.");return;}				
				if(startDate > endDate) {alert("시작일은 종요일보다 이전이여야 합니다.");return;}				
				let addMsg=this.saveImmediatelyYn=="N"?"\n설정파일에 적용시 저장을 해야 합니다.":"";		
				if(!confirm("수행 주기에 기간을 적용합니다."+addMsg)) {return;}
			}				
			const targetForm=this.getActionForm("/config/scheduleTerm");
			targetForm["scheduleTermCommandType"].value=commandType;
			targetForm["scheduleTermValue"].value=document.getElementById(sRefValueId).value.trim()+"|"+document.getElementById(eRefValueId).value.trim();
			targetForm.submit();
			return;
		};		
		this.configScheduleChangeCheck=function(obj,value) {
			let objId=obj.id;
			let taskId=objId.split(".")[0];
			let spanTag=document.getElementById(objId.split("@")[0]+"@s");
			let getAnchorHtml=function(taskId,refValueId) {return "<a href=# onClick=\"task.configSchedule('"+taskId+"','"+refValueId+"')\">적용</a>";};
			spanTag.innerHTML=obj.value.trim()==value?"":getAnchorHtml(taskId,obj.id);
			return;
		};			
		this.configSchedule=function(taskId,refValueId){
			console.log(taskId);
			let scheduleType=refValueId.split("@")[0].split(".")[1];
			let scheduleValue=document.getElementById(refValueId).value;
			if(scheduleValue.trim()=="") {alert(scheduleType+"의 스케쥴적용 값이 없습니다.");return;}
			const targetForm=this.getActionForm("/config/schedule");
			let taskName = targetForm["taskName"].value;				
			let addMsg=(this.saveImmediatelyYn=="N"?"\n설정파일에 적용시 저장을 해야 합니다.":"");				
			if(!confirm("\""+taskName+"\"의 수행스케쥴을 변경합니다."+addMsg)) {return;}				
			targetForm["scheduleType"].value=scheduleType;
			targetForm["scheduleValue"].value=scheduleValue;				
			targetForm.submit();				
			return;
		};			
		this.configBizPropsChangeCheck=function(obj,checkValue,baseRefId) {
			let spanTag=document.getElementById(baseRefId+".span");
			let getAnchorHtml=function(refKeyId,refValueId) {return "<a href=# onClick=\"task.configBizProps('"+refKeyId+"','"+refValueId+"')\">적용</a>";};
			spanTag.innerHTML=obj.value.trim()==checkValue?"":getAnchorHtml(baseRefId+".key",baseRefId+".value");
			return;
		};
		this.configBizProps=function(refKeyId,refValueId) {
			let addMsg=(this.saveImmediatelyYn=="N"?"\n설정파일에 적용시 저장을 해야 합니다.":"");		
			if(!confirm("변경사항을 적용합니다."+addMsg)) {return;}
			const targetForm=this.getActionForm("/config/bizProps");
			targetForm["propertyKey"].value=document.getElementById(refKeyId).value;
			targetForm["propertyValue"].value=document.getElementById(refValueId).value;
			targetForm.submit();		
			return;
		};
		this.configOutterOptionExpansion=function(refId,obj) {
			let targetObj=document.getElementById(refId);
			if(obj!=null && obj.type!="checkbox"){targetObj.checked=!targetObj.checked;}
			const targetForm=this.getActionForm("/detail");
			targetForm["paramExtensionConfigCheckYn"].value=(targetObj.checked?"Y":"N");
			targetForm.submit();
			return;
		};						
		this.configOutterOptionChangeCheck=function(optionId,spanId,refId,obj) {
			let targetObj=document.getElementById(refId);
			if(obj!=null && obj.type!="checkbox"){targetObj.checked=!targetObj.checked;}
			
			let taskId=optionId.split(".")[0];
			let optionScope=optionId.split(".")[1];

			let changeCheck=function(taskId,optionScope){
				if(document.getElementById(taskId+"."+optionScope).type == "checkbox"){
					if(document.getElementById(taskId+"."+optionScope+".hidden").value.trim()==""){document.getElementById(taskId+"."+optionScope+".hidden").value="N";}
					let value = document.getElementById(taskId+"."+optionScope).checked ? "Y" : "N";
					return value == document.getElementById(taskId+"."+optionScope+".hidden").value ? false : true;					
				}
				return document.getElementById(taskId+"."+optionScope).value.trim() == document.getElementById(taskId+"."+optionScope+".hidden").value ? false : true;
			}
			let doReset=function(taskId,optionScope){
				if(document.getElementById(taskId+"."+optionScope).type == "checkbox"){
					if(document.getElementById(taskId+"."+optionScope+".hidden").value.trim()==""){document.getElementById(taskId+"."+optionScope+".hidden").value="N";}
					document.getElementById(taskId+"."+optionScope).checked = document.getElementById(taskId+"."+optionScope+".hidden").value=="Y" ? true : false;
				}
				document.getElementById(taskId+"."+optionScope).value = document.getElementById(taskId+"."+optionScope+".hidden").value;
			}
			
			let isChange=false;
			if(optionScope=="jobParametersApiUrl"){
				if(!isChange && changeCheck(taskId,"jobParametersApiUrl")) {isChange=true;}
				if(!isChange && changeCheck(taskId,"jobParametersApiUrlEnableYn")) {isChange=true;}
				if(!isChange && changeCheck(taskId,"jobParametersApiUrlOverlapYn")) {isChange=true;}
				if(!isChange){
					doReset(taskId,"jobParametersApiUrl");
					doReset(taskId,"jobParametersApiUrlEnableYn");
					doReset(taskId,"jobParametersApiUrlOverlapYn");
				}		
			}
			if(optionScope=="callbackApiUrl"){
				if(!isChange && changeCheck(taskId,"callbackApiUrl")) {isChange=true;}
				if(!isChange && changeCheck(taskId,"callbackApiUrlEnableYn")) {isChange=true;}
				if(!isChange){
					doReset(taskId,"callbackApiUrl");
					doReset(taskId,"callbackApiUrlEnableYn");
				}
			}
			if(optionScope=="jvmMemory"){
				if(!isChange && changeCheck(taskId,"jvmMemory")) {isChange=true;}
				if(!isChange && changeCheck(taskId,"jvmMemoryEnableYn")) {isChange=true;}
				if(!isChange){
					doReset(taskId,"jvmMemory");
					doReset(taskId,"jvmMemoryEnableYn");
				}
				let isDefault=true;
				if(isDefault && !document.getElementById(taskId+".jvmMemoryEnableYn").checked){isDefault=false;}
				if(isDefault && document.getElementById(taskId+".jvmMemory").value.trim()!=""){isDefault=false;}
				if(isDefault){document.getElementById(taskId+".jvmMemory").value="-Xms256m -Xmx512m";}
			}
			let spanTag=document.getElementById(spanId);				
			let getAnchorHtml=function(taskId,optionScope) {return "<a href=# onClick=\"task.configOutterOptionChange('"+taskId+"','"+optionScope+"')\">적용</a>";};
			spanTag.innerHTML=isChange?getAnchorHtml(taskId,optionScope):"";
			return;	
		};
		this.configOutterOptionChange=function(taskId,optionScope) {
			let isValid1=function(v) {
				if(v===""){return true;}
			    const pattern = /^(https?|ftp):\/\/[^\s/$.?#].[^\s]*$/;
			    return pattern.test(v);
			};
			let isValid2=function(v) {
			    const pattern = /^-Xms\d+[kKmMgG]?\s+-Xmx\d+[kKmMgG]?$/;
			    return pattern.test(v);
			};
			let isValid3=function(v) {
				const numbers = v.match(/\d+/g).map(Number);
				return numbers[0] <= numbers[1] ? true : false;
			};
			let isValid4=function(v) {
				const units = v.match(/[a-zA-Z]+/g);
				return !(units && units.length === 2 && units[0] === units[1]);
			};				
			const targetForm=this.getActionForm("/config/outterOption");				
			if(optionScope=="jobParametersApiUrl"){
				let isEnable=true, msg="";
				if(isEnable && document.getElementById(taskId+".jobParametersApiUrlOverlapYn").checked && document.getElementById(taskId+".jobParametersApiUrl").value.trim()==""){isEnable=false; msg="활성시, '배치잡 가변 파라메터(apiUrl)'을 입력해야 합니다.";}					
				if(isEnable && document.getElementById(taskId+".jobParametersApiUrlEnableYn").checked && document.getElementById(taskId+".jobParametersApiUrl").value.trim()==""){isEnable=false; msg="병합시, '배치잡 가변 파라메터(apiUrl)'을 입력해야 합니다.";}
				if(isEnable && !isValid1(document.getElementById(taskId+".jobParametersApiUrl").value)){isEnable=false; msg="'배치잡 가변 파라메터(apiUrl)', 올바른 URL이 아닙니다.";}
				if(!isEnable){alert(msg);return;}
				
				targetForm["outterOptionType"].value="optionType1";
				targetForm["jobParametersApiUrlEnableYn"].value=document.getElementById(taskId+".jobParametersApiUrlEnableYn").checked?"Y":"N";
				targetForm["jobParametersApiUrlOverlapYn"].value=document.getElementById(taskId+".jobParametersApiUrlOverlapYn").checked?"Y":"N";	
				targetForm["jobParametersApiUrl"].value=document.getElementById(taskId+".jobParametersApiUrl").value;
				
			}
			if(optionScope=="callbackApiUrl"){
				let isEnable=true, msg="";
				if(isEnable && document.getElementById(taskId+".callbackApiUrlEnableYn").checked && document.getElementById(taskId+".callbackApiUrl").value.trim()==""){isEnable=false; msg="활성시, '배치잡 수행결과 callback(apiUrl)'을 입력해야 합니다.";}
				if(isEnable && !isValid1(document.getElementById(taskId+".callbackApiUrl").value)){isEnable=false; msg="'배치잡 수행결과 callback(apiUrl)', URL이 아닙니다.";}
				if(!isEnable){alert(msg);return;}

				targetForm["outterOptionType"].value="optionType2";
				targetForm["callbackApiUrlEnableYn"].value=document.getElementById(taskId+".callbackApiUrlEnableYn").checked?"Y":"N";
				targetForm["callbackApiUrl"].value=document.getElementById(taskId+".callbackApiUrl").value;
			}
			if(optionScope=="jvmMemory"){					
				if(document.getElementById(taskId+".jvmMemory").value.trim()!=""){
					let isEnable=true, msg="";
					if(isEnable && !isValid2(document.getElementById(taskId+".jvmMemory").value)){isEnable=false; msg="'배치잡 JVM 메모리', 올바른 JVM 메모리 설정이 아닙니다.\n ex) 최소:256m ~ 최대:512m = -Xms256m -Xmx512m";}
					if(isEnable && !isValid3(document.getElementById(taskId+".jvmMemory").value)){isEnable=false; msg="'배치잡 JVM 메모리', Xms설정크기 <= Xmx설정크기";}											
					if(isEnable && !isValid4(document.getElementById(taskId+".jvmMemory").value)){isEnable=false; msg="'배치잡 JVM 메모리', 설정단위 불일치";}											
					if(!isEnable){alert(msg);return;}						
				}
				
				targetForm["outterOptionType"].value="optionType3";
				targetForm["jvmMemoryEnableYn"].value=document.getElementById(taskId+".jvmMemoryEnableYn").checked?"Y":"N";
				targetForm["jvmMemory"].value=document.getElementById(taskId+".jvmMemory").value;
			}
			let addMsg=(this.saveImmediatelyYn=="N"?"\n설정파일에 적용시 저장을 해야 합니다.":"");
			if(!confirm("변경사항을 적용합니다."+addMsg)) {return;}
			targetForm.submit();		
			return;
		};	
		this.scheduleMode=function(obj){
			let getAnchorHtml=function(taskId,refValueId) {return "<a href=# onClick=\"task.scheduleChange('"+taskId+"','"+refValueId+"')\">적용</a>";};
			let setBackup=function(taskId,type){
				let targetObj=document.getElementById(taskId+"."+type+"@v");
				targetObj.backup=targetObj.value;
				targetObj.initValue=targetObj.value;					
				targetObj.disabled=targetObj.value==""?true:false;
				return;
			}
			let setMode=function(taskId,type,checked){
				let check=document.getElementById(taskId+"."+type+"@c");
				let input=document.getElementById(taskId+"."+type+"@v");
				let span=document.getElementById(taskId+"."+type+"@s");
				
				check.checked=checked;
				input.disabled=!checked;					
				if(input.value!=""){input.backup=input.value;}						
				input.value="";
				span.innerHTML="";
				if(checked){
					input.focus();
					input.value=input.backup;
					if(input.value!=input.initValue){span.innerHTML=getAnchorHtml(taskId,input.id);}
				}
				return;
			}
			let setLock=function(taskId,type){
				document.getElementById(taskId+"."+type+"@c").disabled=true;
				document.getElementById(taskId+"."+type+"@a").onclick=null;
				document.getElementById(taskId+"."+type+"@v").disabled=true;
				document.getElementById(taskId+"."+type+"@v").readonly=true;
				return;
			}
			
			if(typeof obj == "string" && obj.indexOf("initBackup")>-1){
				let taskId=obj.split(".")[0];
				setBackup(taskId,"cronExpression");
				setBackup(taskId,"fixedDelay");
				setBackup(taskId,"fixedRate");
				return;
			}
			if(typeof obj == "string" && obj.indexOf("schedulerStatus:start")>-1){
				let taskId=obj.split(".")[0];
				setLock(taskId,"cronExpression");
				setLock(taskId,"fixedDelay");
				setLock(taskId,"fixedRate");
								
				document.getElementById(taskId+"."+"taskConfig.scheduleTermEnableYn@c").disabled=true;
				document.getElementById(taskId+"."+"taskConfig.scheduleTermEnableYn@a").onclick=null;
				document.getElementById(taskId+"."+"scheduleTermList.new.start").disabled=true;
				document.getElementById(taskId+"."+"scheduleTermList.new.end").disabled=true;
				
				return;
			}				
			if(typeof obj == "object"){
				let objId=obj.id;
				let taskId=objId.split(".")[0];
				setMode(taskId,"cronExpression",objId.indexOf("cronExpression")>-1?true:false);
				setMode(taskId,"fixedDelay",objId.indexOf("fixedDelay")>-1?true:false);
				setMode(taskId,"fixedRate",objId.indexOf("fixedRate")>-1?true:false);
				return;
			}
		};
		this.setBizPropsDesc=function(descId,descSpanTagId){
			let descObj=document.getElementById(descId);
			if(descObj==null){return;}
			let spanTag=document.getElementById(descSpanTagId);
			spanTag.innerHTML=descObj.value;
			return;
		};
		this.clearHistory=function(taskName) {
			let executionNo="";
			let deleteAllYn="N";
			
			let msg="\""+taskName+"\" 수행로그를 삭제합니다.";				
			if(document.getElementById("deleteAllYn").checked) {
				deleteAllYn="Y";
				msg+="\n 삭제대상 : 전체";					
				msg+="\n 집계카운트 : 초기화";			
			} else {
				executionNo=document.getElementById("taskExecutionLogDiv").executionNo;
				if(executionNo==null) {alert("삭제할 대상을 선택하세요.");return;}
				msg+="\n 삭제대상 : "+executionNo;
				msg+="\n 집계카운트 : 반영없음"
			}
			if(!confirm(msg)) {return;}
			
			const targetForm=this.getActionForm("/support/clearHistory");
			targetForm["executionNo"].value=executionNo;
			targetForm["deleteAllYn"].value=deleteAllYn;
			targetForm.submit();
			return;		
		};
		this.logInfo=function(executionNo,executionLogYn,exceptionLogYn,logSeq) {
			let thisObj=this;
			let getLogExtractMode=function(taskObj) {return taskObj.getConfigValue(executionNo,"logExtractMode");}
			let getLogExtractSize=function(taskObj) {return document.getElementById(taskObj.getConfigValue(executionNo,"logExtractSizeKey")).value;}
			let printExecutionLog=function(taskObj,executionNo,logSeq,sameCheckYn) {
				if(sameCheckYn=="Y" && document.getElementById("taskExecutionLogDiv").executionNo==executionNo){
					document.getElementById("taskExecutionLogDiv").style.display="block";
					return;
				}
				document.getElementById("taskExecutionLogDiv").style.display="block";			
				document.getElementById("taskExecutionLogTitle").innerHTML="&nbsp;["+executionNo+"] 수행로그&nbsp;&nbsp;";		
				let logType = taskObj.getLogType(taskObj,executionNo,document.getElementById("taskExecutionLogTitle"));						
				taskObj.logInfoLinkTop(executionNo,logType);
				taskObj.logInfoLinkBottom(executionNo,logType);
				taskObj.logInfoOption(executionNo,logType);
				document.getElementById("taskExecutionLogFrame").contentDocument.body.innerHTML="";
				document.getElementById("taskExecutionLogFrame").src=thisObj.basePath+"/meta/logInfo?executionNo="+executionNo+"&logType="+logType+"&logSeq="+logSeq+"&logExtractMode="+getLogExtractMode(taskObj)+"&logExtractSize="+getLogExtractSize(taskObj);
				document.getElementById("taskExecutionLogDiv").executionNo=executionNo;	
			}
			let printExceptionLog=function(taskObj,executionNo,logSeq,sameCheckYn) {
				if(sameCheckYn=="Y" && document.getElementById("taskExceptionLogDiv").executionNo==executionNo){
					document.getElementById("taskExceptionLogDiv").style.display="block";
					return;
				}
				document.getElementById("taskExceptionLogDiv").style.display="block";
				document.getElementById("taskExceptionLogTitle").innerHTML="&nbsp;["+executionNo+"] 수행오류&nbsp;&nbsp;";
				let logType="exceptionLog";
				taskObj.logInfoLinkTop(executionNo,logType);
				taskObj.logInfoLinkBottom(executionNo,logType);
				taskObj.logInfoOption(executionNo,logType);
				document.getElementById("taskExceptionLogFrame").contentDocument.body.innerHTML="";
				document.getElementById("taskExceptionLogFrame").src=thisObj.basePath+"/meta/logInfo?executionNo="+executionNo+"&logType="+logType+"&logSeq="+logSeq+"&logExtractMode="+getLogExtractMode(taskObj)+"&logExtractSize="+getLogExtractSize(taskObj);
				document.getElementById("taskExceptionLogDiv").executionNo=executionNo;
			}

			console.log("arguments.length:"+arguments.length);
			console.log("executionNo:"+executionNo+",executionLogYn:"+executionLogYn+",exceptionLogYn:"+exceptionLogYn+",logSeq:"+logSeq);

			if(arguments.length==1) {
				let executionNo=arguments[0];
				printExecutionLog(this,executionNo,1,"N");
				return;
			}				
			if(arguments.length==2) {
				let executionNo=arguments[0];
				let logType=arguments[1];
				if(logType=="executionLog") {printExecutionLog(this,executionNo,1,"N");}
				if(logType=="exceptionLog") {printExceptionLog(this,executionNo,1,"N");}
				return;
			}
			if(arguments.length==3) {
				let executionNo=arguments[0];
				//let executionLogYn=arguments[1];
				let exceptionLogYn=arguments[2];				
				document.getElementById("taskExceptionLogDiv").style.display="none";
				printExecutionLog(this,executionNo,1,"Y");
				if(exceptionLogYn=="Y") {printExceptionLog(this,executionNo,1,"Y");}
				return;
			}
			if(arguments.length==4) {
				let executionNo=arguments[0];
				let executionLogYn=arguments[1];
				let exceptionLogYn=arguments[2];
				let logSeq=arguments[3];
				if(executionLogYn=="Y") {printExecutionLog(this,executionNo,logSeq,"N");}
				if(exceptionLogYn=="Y") {printExceptionLog(this,executionNo,logSeq,"N");}
				return;
			}
			return;
		};
		this.getLogType=function(taskObj,executionNo,container){
			let logType="executionLog";
			
			let jsonObj = taskObj.getJson("executionPostProcess",executionNo);
			
			let metaLogFileCount = jsonObj.metaLogFileCount;
			let metaLogFileInfo = jsonObj.metaLogFileInfo;									
			if(metaLogFileCount==0 || !metaLogFileInfo){return logType;}
			
			let options = metaLogFileInfo.includes(",") ? metaLogFileInfo.split(",") : [metaLogFileInfo]
			options.sort().unshift(jsonObj.executionLogFilePath);

			let selectBox = document.createElement("select");
			options.forEach((logPath,index) => {
			    let option = document.createElement("option");
			    option.value = index;
			    option.textContent = logPath;
			    selectBox.appendChild(option);
			});
			selectBox.style.cssText = "background-color: #e5e5e5; border: 1px solid #cccccc; padding: 1px; font-size: 12px;";
			selectBox.addEventListener("change", function() {
				taskObj["metaLogExecutionNo"]=executionNo;
				taskObj["metaLogSelectedIndex"]=this.selectedIndex;
				taskObj.logInfo(executionNo);
				}
			);				
			container.appendChild(selectBox);	

			let isEnable=true;
			if(isEnable && (taskObj["metaLogExecutionNo"] == null || taskObj["metaLogExecutionNo"] != executionNo)) {isEnable=false;}
			if(isEnable && (taskObj["metaLogSelectedIndex"] == null || taskObj["metaLogSelectedIndex"] == 0)) {isEnable=false;}
			if(isEnable){
				selectBox.selectedIndex = taskObj["metaLogSelectedIndex"];
				logType="metaLog"+selectBox.selectedIndex;
			}

			return logType;
				
		};
		this.logInfoLinkTop=function(executionNo,logType) {
			let thisObj=this;
			let linkArea=null,metaFrame=null;
			if(logType.startsWith("metaLog")) {linkArea=document.getElementById("taskExecutionLogLinkTop");metaFrame=document.getElementById("taskExecutionLogFrame");}
			if(logType.startsWith("executionLog")) {linkArea=document.getElementById("taskExecutionLogLinkTop");metaFrame=document.getElementById("taskExecutionLogFrame");}
			if(logType.startsWith("exceptionLog")) {linkArea=document.getElementById("taskExceptionLogLinkTop");metaFrame=document.getElementById("taskExceptionLogFrame");}	
			if(linkArea==null || metaFrame==null) {return;}
			
			let splitBar="&nbsp;<font color=#9f9f9f>|</font>&nbsp;";
			let option="style=\"cursor:default\" onMouseOver=\"this.style.color='blue'\" onMouseOut=\"this.style.color=''\"";
			let getInputHtml=function(id,value,option) {return "<input type=\"text\" id=\""+id+"\" "+option+" value=\""+value+"\">";};
			let getSpanHtml=function(id,name,option) {return "<span id=\""+id+"\" "+option+">"+name+"</span>";};
			
			let infoSpanId=executionNo+"."+logType+".infoSpanId";
			let findInputId=executionNo+"."+logType+".findInputId";
			let findSpanId=executionNo+"."+logType+".findSpanId";
			let downloadSpanId=executionNo+"."+logType+".logInfoDownload";
			let heightSpanId=executionNo+"."+logType+".logInfoHeight";	
			let heightBase=150;
			
			let linkHtml="<font face=verdana size=-1>";
			linkHtml+=getSpanHtml(infoSpanId,"","")+"&nbsp;";
			linkHtml+=getInputHtml(findInputId,"","class='textType44'")+"&nbsp;";
			linkHtml+=getSpanHtml(findSpanId,"",option);
			linkHtml+=splitBar;
			linkHtml+=getSpanHtml(downloadSpanId,"download",option);
			linkHtml+=splitBar;
			linkHtml+=getSpanHtml(heightSpanId,"&ltH&gt",option);
			linkHtml+="</font>&nbsp;&nbsp;";
			
			linkArea.innerHTML=linkHtml;
			
			if(metaFrame.heightSpanId==null || metaFrame.heightSpanId!=heightSpanId) {metaFrame.style.height=heightBase+"px";}
			metaFrame.heightSpanId=heightSpanId;

			let scrollInto=function(target){
				if(target==null){return;}
				let baseHeight=Number(metaFrame.style.height.replace("px",""));
				let targetRect=target.getBoundingClientRect();
				let checkHeight=Math.ceil(target.getBoundingClientRect().top+(targetRect.height*1.5));
				if(checkHeight<0){target.scrollIntoView(true);}
				if(baseHeight<checkHeight){target.scrollIntoView(false);}
				return;
			};
			let selectFocus=function(target){
				if(target==null){return;}
				let findObj=document.getElementById(findSpanId);
				metaFrame.contentDocument.getElementById("findData"+findObj["xFocus"]).style.background="#ffffff";
				findObj["xFocus"]=Number(target.id.replace("findData",""));
				target.style.background="#cfcfcf";
				scrollInto(target);
				return;
			};				
			let findData=function(keyward){
				let dataFrame=metaFrame.contentDocument;
				let findObj=document.getElementById(findSpanId);
				let infoObj=document.getElementById(infoSpanId);
				if(findObj["xKeyward"]!=keyward){findObj["xCount"]=null;findObj["xFocus"]=null;findObj["xKeyward"]=null;}
				if(findObj["xKeyward"]==null){
					let limitLength=1024*1024*2.5;
					let logDataObj=dataFrame.getElementById("logData");
					let logData=logDataObj["initData"]!=null?logDataObj["initData"]:logDataObj.innerHTML;
					if(logData.length>limitLength){alert("검색 가능한 데이타크기를 초과하였습니다.\n\nlimit : "+limitLength);return;}
					if(logDataObj["initData"]==null){logDataObj["initData"]=logDataObj.innerHTML;}						
					logDataObj.innerHTML=logData.split(keyward).join("<span class='findData'>"+keyward+"</span>");
					let findList=dataFrame.getElementsByClassName("findData");
					if(findList.length==0){infoObj.innerHTML="dose not exist";infoObj.style.color="red";logDataObj.scrollIntoView();return;}						
					for(let i=0;i<findList.length;i++){				
						findList[i]["id"]="findData"+i;
						findList[i].onclick=function(){selectFocus(this);}
					}
					infoObj.innerHTML="count:"+findList.length;
					infoObj.style.color="green";
					findObj["xFocus"]=-1;
					findObj["xCount"]=findList.length;
					findObj["xKeyward"]=keyward;
				}
				findObj["xFocus"]=Number(findObj["xFocus"]);
				findObj["xCount"]=Number(findObj["xCount"]);					

				let release=dataFrame.getElementById("findData"+findObj["xFocus"]);
				if(release!=null){release.style.background="#ffffff";}

				if(findObj["xCount"]-findObj["xFocus"]==1){findObj["xFocus"]=-1;}

				findObj["xFocus"]=findObj["xFocus"]+1;
				let target=dataFrame.getElementById("findData"+findObj["xFocus"]);
				if(target==null){findObj["xFocus"]=findObj["xFocus"]-1;target=release;}
				if(target!=null){target.style.background="#cfcfcf";}
				
				scrollInto(target==null?release:target);
				findObj.scrollIntoView(false);					
				return;
			};
			let initFinder=function(){
				let infoObj=document.getElementById(infoSpanId);
				let findObj=document.getElementById(findSpanId);
				let keywardObj=document.getElementById(findInputId);
				if(keywardObj.value.trim()=="") {
					infoObj.innerHTML="";
					findObj.innerHTML="";
					keywardObj.value="";
					let logDataObj=metaFrame.contentDocument.getElementById("logData");				
					if(logDataObj!=null && logDataObj["initData"]!=null){logDataObj.innerHTML=logDataObj["initData"];}
					findObj["xCount"]=null;findObj["xFocus"]=null;findObj["xKeyward"]=null;
					return;
				}
				findObj.innerHTML="find";
				findObj.onclick=function() {findData(keywardObj.value);};
				return;
			};
			
			document.getElementById(findInputId).onclick=function(){initFinder();return;};
			document.getElementById(findInputId).onkeydown=function(e){initFinder();if(e.key==="Enter"){findData(this.value);}return;};
			document.getElementById(downloadSpanId).onclick=function() {
				metaFrame.src=thisObj.basePath+"/meta/logInfoDownload?executionNo="+executionNo+"&logType="+logType;
				return;
			};
			document.getElementById(heightSpanId).onclick=function() {
				let max=450;
				let height=Number(metaFrame.style.height.replace("px",""));
				let magnification=height>450?1:height/heightBase+1;
				metaFrame.style.height=(heightBase*magnification)+"px";
				document.getElementById(heightSpanId).innerHTML=(magnification>(max/heightBase)?"-H-":"&ltH&gt");
				return;
			};
			return;
		};
		this.logInfoLinkBottom=function(executionNo,logType,logSeq,logPrintEnableYn) {
			let linkArea=null;
			if(logType.startsWith("metaLog")) {linkArea=document.getElementById("taskExecutionLogLinkBottom");}
			if(logType.startsWith("executionLog")) {linkArea=document.getElementById("taskExecutionLogLinkBottom");}
			if(logType.startsWith("exceptionLog")) {linkArea=document.getElementById("taskExceptionLogLinkBottom");}	
			if(linkArea==null) {return;}

			if(arguments.length==2) {
				linkArea.innerHTML="<font face=verdana size=-1>loading...</font>&nbsp;&nbsp;";
				return;
			}

			let splitBar="&nbsp;<font color=#9f9f9f>|</font>&nbsp;";
			let option="style=\"cursor:default\" onMouseOver=\"this.style.color='blue'\" onMouseOut=\"this.style.color=''\"";
			let getSpanHtml=function(id,text,option) {return "<span id=\""+id+"\" "+option+">"+text+"</span>";};
			let getInputHtml=function(id,value,option) {return "<input type=\"text\" id=\""+id+"\" "+option+" value=\""+value+"\">";};
			
			let prevSpanId=executionNo+"."+logType+".logInfoNavi.prev";	
			let curInputId=executionNo+"."+logType+".logInfoNavi.cur";	
			let nextSpanId=executionNo+"."+logType+".logInfoNavi.next";		
			
			let linkHtml="<font face=verdana size=-1>";
			linkHtml+=getSpanHtml(prevSpanId,"prev",(logSeq<2?"":option));
			linkHtml+=splitBar+getInputHtml(curInputId,logSeq,"class='textType41'")+splitBar;
			linkHtml+=getSpanHtml(nextSpanId,"next",(logPrintEnableYn=="N"?"":option));
			linkHtml+="</font>&nbsp;&nbsp;";
			
			linkArea.innerHTML=linkHtml;
							
			let taskObj=this;
			let executionLogYn=(logType=="executionLog"?"Y":"N");
			let exceptionLogYn=(logType=="exceptionLog"?"Y":"N");

			if(logType.startsWith("metaLog")){
				executionLogYn="Y";
				exceptionLogYn="N";
			}

			document.getElementById(prevSpanId).onclick=function() {
				if(logSeq<2) {return;}
				taskObj.logInfo(executionNo,executionLogYn,exceptionLogYn,logSeq-1);
				return;
			};
			document.getElementById(curInputId).onkeyup=function(e) {					
				if(e.key==="Enter") {return;}
				if(isNaN(this.value)) {alert("숫자를 입력하세요.");return;}
				if(Number(this.value)<1) {return;}
				if(logPrintEnableYn=="N" && Number(this.value)>=logSeq) {return;}
				taskObj.logInfo(executionNo,executionLogYn,exceptionLogYn,this.value);
				return;
			};			
			document.getElementById(nextSpanId).onclick=function() {
				if(logPrintEnableYn=="N") {return;}
				taskObj.logInfo(executionNo,executionLogYn,exceptionLogYn,logSeq+1);
				return;
			};	
			
			return;
		};	
		this.logInfoOption=function(executionNo,logType) {
			let optionArea=null;
			if(logType.startsWith("metaLog")) {optionArea=document.getElementById("taskExecutionLogOption");}
			if(logType.startsWith("executionLog")) {optionArea=document.getElementById("taskExecutionLogOption");}
			if(logType.startsWith("exceptionLog")) {optionArea=document.getElementById("taskExceptionLogOption");}	
			if(optionArea==null) {return;}

			//let splitBar="&nbsp;<font color=#9f9f9f>|</font>&nbsp;";
			let option="style=\"cursor:default\" onMouseOver=\"this.style.color='blue'\" onMouseOut=\"this.style.color=''\"";
			let getSpanHtml=function(id,text,option) {return "<span id=\""+id+"\" "+option+">"+text+"</span>";};
			let getInputHtml=function(id,value,option) {return "<input type=\"text\" id=\""+id+"\" "+option+" value=\""+value+"\">";};
			let checkNumder=function(value) {if(isNaN(value)) {alert("숫자를 입력하세요.");return false;}return Number(value)<1?false:true;};
			
			let taskObj=this;
			let logFileDbStoreYn=this.getJson("executionPostProcess",executionNo).logFileDbStoreYn;
			let logExtractMode=this.getConfigValue(executionNo,"logExtractMode");

			if(logFileDbStoreYn=="N") {
				this.setConfigValue(executionNo,"logExtractMode","line");
				
				let initValue=this.getConfigValue(executionNo,"logLineSize");
				let toggleId=executionNo+"."+logType+".logExtractMode.toggle.line";
				let inputId=executionNo+"."+logType+".logExtractSize.input.line";
				let reloadId=executionNo+"."+logType+".logExtractSize.reload.line";
				let reloadAction=function() {
					let curValue=document.getElementById(inputId).value;		
					if(curValue=="") {return;}
					if(!checkNumder(curValue)) {document.getElementById(inputId).value=initValue;return;}						
					taskObj.setConfigValue(executionNo,"logLineSize",curValue);
					taskObj.logInfo(executionNo,logType);
					return;
				}
				
				let html="<font face=verdana size=-1>"+getSpanHtml(toggleId,"lines","")+" : "+getInputHtml(inputId,initValue,"class='textType42'")+"&nbsp;"+getSpanHtml(reloadId,"",option)+"&nbsp;</font>";
				this.setConfigValue(executionNo,"logExtractSizeKey",inputId);
				optionArea.innerHTML=html;					
				document.getElementById(toggleId).onclick=function() {};	
				document.getElementById(inputId).onkeyup=function() {document.getElementById(reloadId).innerHTML=this.value==initValue?"":"reload";};	
				document.getElementById(inputId).onkeydown=function(e) {if(e.key==="Enter") {reloadAction();}};
				document.getElementById(reloadId).onclick=function() {reloadAction();}
			}				
			if(logFileDbStoreYn=="Y" && logExtractMode=="block") {
				let initValue=this.getConfigValue(executionNo,"logBlockSize");
				let toggleId=executionNo+"."+logType+".logExtractMode.toggle.block";
				let inputId=executionNo+"."+logType+".logExtractSize.input.block";
				let reloadId=executionNo+"."+logType+".logExtractSize.reload.block";
				let reloadAction=function() {
					let curValue=document.getElementById(inputId).value;		
					if(curValue=="") {return;}
					if(!checkNumder(curValue)) {document.getElementById(inputId).value=initValue;return;}						
					taskObj.setConfigValue(executionNo,"logChunkSize",curValue);
					taskObj.logInfo(executionNo,logType);
					return;
				}
				let html="<font face=verdana size=-1>"+getSpanHtml(toggleId,"block",option)+" > "+getInputHtml(inputId,initValue,"class='textType43' disabled readonly")+"&nbsp;"+getSpanHtml(reloadId,"",option)+"&nbsp;</font>";
				this.setConfigValue(executionNo,"logExtractSizeKey",inputId);
				optionArea.innerHTML=html;
				document.getElementById(toggleId).onclick=function() {taskObj.setConfigValue(executionNo,"logExtractMode","chunk");taskObj.logInfoOption(executionNo,logType);};			
				document.getElementById(inputId).onkeyup=function() {document.getElementById(reloadId).innerHTML=this.value==initValue?"":"reload";};	
				document.getElementById(inputId).onkeydown=function(e) {if(e.key==="Enter") {reloadAction();}};
				document.getElementById(reloadId).onclick=function() {reloadAction();}	
			}
			if(logFileDbStoreYn=="Y" && logExtractMode=="chunk") {
				let initValue=this.getConfigValue(executionNo,"logChunkSize");
				let toggleId=executionNo+"."+logType+".logExtractMode.toggle.chunk";
				let inputId=executionNo+"."+logType+".logExtractSize.input.chunk";
				let reloadId=executionNo+"."+logType+".logExtractSize.reload.chunk";
				let reloadAction=function() {
					let curValue=document.getElementById(inputId).value;		
					if(curValue=="") {return;}
					if(!checkNumder(curValue)) {document.getElementById(inputId).value=initValue;return;}						
					taskObj.setConfigValue(executionNo,"logChunkSize",curValue);
					taskObj.logInfo(executionNo,logType);
					return;
				}
									
				let html="<font face=verdana size=-1>"+getSpanHtml(toggleId,"bytes",option)+" : "+getInputHtml(inputId,initValue,"class='textType42'")+"&nbsp;"+getSpanHtml(reloadId,"",option)+"&nbsp;</font>";
				this.setConfigValue(executionNo,"logExtractSizeKey",inputId);
				optionArea.innerHTML=html;
				document.getElementById(toggleId).onclick=function() {taskObj.setConfigValue(executionNo,"logExtractMode","block");taskObj.logInfoOption(executionNo,logType);};	
				document.getElementById(inputId).onkeyup=function() {document.getElementById(reloadId).innerHTML=this.value==initValue?"":"reload";};	
				document.getElementById(inputId).onkeydown=function(e) {if(e.key==="Enter") {reloadAction();}};
				document.getElementById(reloadId).onclick=function() {reloadAction();}					
			}
			return;
		};
		this.envInfo=function(executionNo,outTarget) {
			let targetObj=this;
			if(arguments.length==0) {
				document.getElementById("taskActionForm")["executionEnvViewCheckYn"].value=(document.getElementById("executionEnvViewCheckYn").checked?"Y":"N");
				this.envInfo(document.getElementById("taskExecutionEnvDiv").executionNo);
				return;
			}
			if(arguments.length==1) {
				document.getElementById("taskExecutionEnvDiv").executionNo=executionNo;
				if(document.getElementById("executionEnvViewCheckYn").checked) {
					document.getElementById("taskExecutionEnvFrame").contentDocument.body.innerHTML="";
					document.getElementById("taskExecutionEnvFrame").src=targetObj.basePath+"/meta/envInfo?executionNo="+executionNo;
				} else {
					document.getElementById("taskExecutionEnvDiv").style.display="none";
				}
				return;				
			}				
			
			document.getElementById("taskExecutionEnvDiv").style.display="block";
			document.getElementById("taskExecutionEnvTitle").innerHTML="&nbsp;["+executionNo+"] 수행환경정보";	
			this.envInfoLinkTop(executionNo,outTarget);
			
			let line="<hr style='border:solid 0.5px #e5e5e5'>";
			let executionConfig=this.getJson("executionConfig",executionNo);
			let executionPostProcess=this.getJson("executionPostProcess",executionNo);
			
			let outHtml="<font size=-1 face=verdana>";
			if(executionConfig!="") {outHtml+="<b>executionConfig</b><br>"+JSON.stringify(executionConfig)+line;}				
			if(executionPostProcess!="") {outHtml+="<b>executionPostProcess</b><br>"+JSON.stringify(executionPostProcess)+line;}	
			outHtml+="</font>&nbsp;&nbsp;";
			
			outTarget.innerHTML=outHtml;
			
			return;
		};			
		this.envInfoLinkTop=function(executionNo,outTarget) {
			let linkArea=document.getElementById("taskExecutionEnvLinkTop");
			let metaFrame=document.getElementById("taskExecutionEnvFrame");

			let taskObj=this;
			let splitBar="&nbsp;<font color=#9f9f9f>|</font>&nbsp;";
			let option="style=\"cursor:default\" onMouseOver=\"this.style.color='blue'\" onMouseOut=\"this.style.color=''\"";
			let getSpanHtml=function(id,name,option) {return "<span id=\""+id+"\" "+option+">"+name+"</span>";};
			
			let parsingSpanId=executionNo+".envInfoParsing";
			let heightSpanId=executionNo+".envInfoHeight";	
			let heightBase=100;

			let linkHtml="<font face=verdana size=-1>";
			linkHtml+=getSpanHtml(parsingSpanId,"parsing",option);
			linkHtml+=splitBar;
			linkHtml+=getSpanHtml(heightSpanId,"&ltH&gt",option);
			linkHtml+="</font>&nbsp;&nbsp;";
			
			linkArea.innerHTML=linkHtml;
			
			if(metaFrame.heightSpanId==null || metaFrame.heightSpanId!=heightSpanId) {metaFrame.style.height=heightBase+"px";}
			metaFrame.heightSpanId=heightSpanId;

			document.getElementById(parsingSpanId).onclick=function() {					
				let line="<hr style='border:solid 0.5px #e5e5e5'>";					
				let executionConfig=taskObj.sortJson(taskObj.getJson("executionConfig",executionNo));
				let executionPostProcess=taskObj.sortJson(taskObj.getJson("executionPostProcess",executionNo));
				
				let outHtml="<pre><font size=-1 face=verdana>";
				if(executionConfig!="") {outHtml+="<b>executionConfig</b>:"+JSON.stringify(executionConfig,null,"\t")+line;}				
				if(executionPostProcess!="") {outHtml+="<b>executionPostProcess</b>:"+JSON.stringify(executionPostProcess,null,"\t")+line;}	
				outHtml+="</font></pre>";
				
				outTarget.innerHTML=outHtml;					
				return;
			};
			document.getElementById(heightSpanId).onclick=function() {
				let max=400;
				let height=Number(metaFrame.style.height.replace("px",""));
				let magnification=height>max?1:height/heightBase+1;
				metaFrame.style.height=(heightBase*magnification)+"px";
				document.getElementById(heightSpanId).innerHTML=(magnification>(max/heightBase)?"-H-":"&ltH&gt");
				return;
			};
			return;
		};			
	};

}
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

this.fncChangeTrColor=function(trObj) {
	let oldColor=trObj.style.backgroundColor;
	trObj.style.backgroundColor="#bbccdd";
	trObj.onmouseout=function() {trObj.style.backgroundColor=oldColor;};
	return;
}

this.fncSetTab=function(tabId) {
	if(tabId==null || tabId=="") {tabId="tab1";}
	
	if(document.getElementById(tabId)!=null && document.getElementById(tabId).style.display=="block"){return;}
	
	let targetForm=document.getElementById("taskActionForm");
	if(targetForm["activeTab"].value==tabId) {return;}

    let tabContent=document.getElementsByClassName("tabContent");
    for(let i=0;i<tabContent.length;i++) {tabContent[i].style.display="none";}

    let tabLinks=document.getElementsByClassName("tabLinks");
    for(let i=0;i<tabLinks.length;i++) {
    	tabLinks[i].className=tabLinks[i].className.replace(" active", "");
    	tabLinks[i].className=tabLinks[i].className.replace(" transition", "");
    }
    
    document.getElementById(tabId+".btn").className+=" active transition";
    document.getElementById(tabId).style.display="block";
	targetForm["activeTab"].value=tabId;	    		
    
    return;
}

this.fncLogTab=function(executionNo,executionLogYn,exceptionLogYn) {
	this.fncSetTab("tab4");
	this.task.logInfo(executionNo,executionLogYn,exceptionLogYn);
	this.task.envInfo(executionNo);
	return;
}

this.fncModalPopup=function(title,view,width,height) {

	// CSS 스타일 추가
	const style = document.createElement('style');
	style.textContent = `
		.modal {display:none;position:fixed;top:0;left:0;width:100%;height:100%;background-color:rgba(0,0,0,0.5);}
		.modal-content {position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);background-color:#fff;border-radius:8px;box-shadow:04px8pxrgba(0,0,0,0.2);display:flex;flex-direction:column;}
		.modal-header {background-color:#ecf4f9;padding:8px;height:15px;display:flex;justify-content:space-between;align-items:center;border-top-left-radius:5px;border-top-right-radius:5px;}
		.modal-body {flex:1;padding:0;text-align:center;overflow:hidden;}
		.modal-bottom {background-color:#ecf4f9;height:10px;display:flex;justify-content:space-between;align-items:center;border-bottom-left-radius:5px;border-bottom-right-radius:5px;}
		.close-button {font-size:14px;cursor:pointer;color:#000;background-color:transparent;border:none;}
		iframe {width:100%;height:100%;border:none;}
	  `;
	document.head.appendChild(style);
	
	// 모달 생성
	const modal = document.createElement("div");
	modal.className = "modal";
	
	const modalContent = document.createElement("div");
	modalContent.className = "modal-content";
    modalContent.style.width = width+"px";
    modalContent.style.height = height+"px";
    
	const modalHeader = document.createElement("div");
	modalHeader.className = "modal-header";
	
	const modalTitle = document.createElement("h5");
	modalTitle.textContent = title;
	
	const closeButton = document.createElement("button");
	closeButton.className = "close-button";
	closeButton.textContent = "X";
	closeButton.onclick = () => {
	  document.body.removeChild(modal);
	};
	
	const modalBody = document.createElement("div");
	modalBody.className = "modal-body";
	
	// iframe을 사용해 표시
	const iframe = document.createElement("iframe");
	iframe.src = view;
	iframe.style.width = "100%";
	iframe.style.height = "100%";
	iframe.style.border = "none";

	const modalBottom = document.createElement("div");
	modalBottom.className = "modal-bottom";

	// 모달 조립
	modalBody.appendChild(iframe);
	modalHeader.appendChild(modalTitle);
	modalHeader.appendChild(closeButton);
	modalContent.appendChild(modalHeader);
	modalContent.appendChild(modalBody);
	modalContent.appendChild(modalBottom);
	modal.appendChild(modalContent);
	
    // 마우스 움직임 감지 후 1분 후 자동 닫기
    let timeoutId;
    const resetTimer = () => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => {
            if (document.body.contains(modal)) {
                document.body.removeChild(modal);
            }
        }, 60000); // 60초
    };
    document.addEventListener("mousemove", resetTimer);
    resetTimer(); // 최초 시작 시 타이머 설정
	
    // 드래그 기능 추가	    
    let isDragging = false, startX, startY, initialX, initialY;
    modalHeader.onmousedown = (e) => {
        isDragging = true;
        startX = e.clientX;
        startY = e.clientY;
        initialX = modalContent.offsetLeft;
        initialY = modalContent.offsetTop;

        document.onmousemove = (e) => {
            if (!isDragging) return;
            let dx = e.clientX - startX;
            let dy = e.clientY - startY;
            modalContent.style.left = initialX + dx + "px";
            modalContent.style.top = initialY + dy + "px";
            modalContent.style.position = "absolute";
        };

        document.onmouseup = () => {
            isDragging = false;
            document.onmousemove = null;
            document.onmouseup = null;
        };
    };

    // 모달 추가 및 표시
    document.body.appendChild(modal);
    modal.style.display = "block";
    
	return;
}