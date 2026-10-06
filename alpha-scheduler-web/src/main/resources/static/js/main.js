///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class MainClassBuilder {
	
	SchedulerClass=function() {
		let mainObj=null;
		let basePath=null;
		let schedulerInfo=null;
		
		if(arguments.length===1) {this.mainObj=arguments[0];}			
		if(arguments.length===2) {this.mainObj=arguments[0];this.basePath=arguments[1];}
		
		this.consoleLog=function(){
			console.log(mainObj);
			console.log(basePath);
			console.log(schedulerInfo);	
		};		
		this.init=function(mainObj) {
			this.mainObj=mainObj;
		};
		this.getActionForm=function(action) {
			let targetForm=document.getElementById("schedulerActionForm");
			targetForm.action=this.basePath+action;
			targetForm["taskId"].value="";
			return targetForm;
		};
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

			if(document.getElementById("scheduler.start") !== null){document.getElementById("scheduler.start").disabled = true;}
			if(document.getElementById("scheduler.stop") !== null){document.getElementById("scheduler.stop").disabled = true;}

			if (this.schedulerInfo.schedulerStatus == "start" && document.getElementById("scheduler.start") !== null) {
				if(document.getElementById("scheduler.start") !== null){document.getElementById("scheduler.start").disabled = true;}
				if(document.getElementById("scheduler.stop") !== null){document.getElementById("scheduler.stop").disabled = false;}
				if(document.getElementById("task.clearHistory") !== null){document.getElementById("task.clearHistory").disabled = true;}
				if(document.getElementById("task.initConfig") !== null){document.getElementById("task.initConfig").disabled = true;}
				if(document.getElementById("task.scanDeploy") !== null){document.getElementById("task.scanDeploy").disabled = true;}
			}
			if (this.schedulerInfo.schedulerStatus == "stop" && document.getElementById("scheduler.stop") !== null) {
				if(document.getElementById("scheduler.start") !== null){document.getElementById("scheduler.start").disabled = false;}
				if(document.getElementById("scheduler.stop") !== null){document.getElementById("scheduler.stop").disabled = true;}
				if(document.getElementById("task.clearHistory") !== null){document.getElementById("task.clearHistory").disabled = false;}
				if(document.getElementById("task.initConfig") !== null){document.getElementById("task.initConfig").disabled = false;}
				if(document.getElementById("task.scanDeploy") !== null){document.getElementById("task.scanDeploy").disabled = false;}
			}

			document.getElementById("schedulerStatus").innerHTML=status;
			
			return;
		};
		this.changeStamp=function(schedulerChangeStamp) {
			let targetForm=document.getElementById("schedulerActionForm");
			targetForm["schedulerChangeStamp"].value=schedulerChangeStamp;
			return;
		};	
		this.start=function(confirmCheckValueYn) {
			if(confirmCheckValueYn=="Y"){
				let checkValue = String(Math.floor(Math.random()*1000000)).padStart(6, "0");
				let inputValue = prompt("스케줄러를 시작합니다.\n\n확인 입력이 필요합니다. (입력값 : " + checkValue + ")");
				if (inputValue != checkValue) {return;}		
			}else{
				if(!confirm("스케줄러를 시작합니다.")) {return;}	
			}
			this.getActionForm("/start").submit();
			return;
		};
		this.stop=function(confirmCheckValueYn) {
			if(confirmCheckValueYn=="Y"){
				let checkValue = String(Math.floor(Math.random()*1000000)).padStart(6, "0");
				let inputValue = prompt("스케줄러를 정지합니다.\n\n확인 입력이 필요합니다. (입력값 : " + checkValue + ")");
				if (inputValue != checkValue) {return;}
			}else{
				if(!confirm("스케줄러를 중지합니다.")) {return;}	
			}
			this.getActionForm("/stop").submit();
			return;
		};
	};

	SupportClass=function() {
		let mainObj=null;
		let basePath=null;
		
		if(arguments.length===1) {this.mainObj=arguments[0];}			
		if(arguments.length===2) {this.mainObj=arguments[0];this.basePath=arguments[1];}
		
		this.consoleLog=function(){
			console.log(mainObj);
			console.log(basePath);
		};				
		this.init=function(mainObj) {
			this.mainObj=mainObj;
		};
		this.getActionForm=function(action) {
			let targetForm=document.getElementById("schedulerActionForm");
			targetForm.action=this.basePath+action;
			targetForm["taskId"].value="";
			return targetForm;
		};
		this.enableAll=function(confirmCheckValueYn,enableYn) {
			if(confirmCheckValueYn=="Y"){
				let checkValue = String(Math.floor(Math.random()*1000000)).padStart(6, "0");
				let inputValue = prompt("현재 조회된 모든 타스크를 "+(enableYn=="Y"?"enable":"disable")+"합니다.\n\n확인 입력이 필요합니다. (입력값 : " + checkValue + ")");
				if (inputValue != checkValue) {return;}
			} else {		
				if(!confirm("현재 조회된 모든 타스크를 "+(enableYn=="Y"?"enable":"disable")+"합니다.")) {return;}	
			}			
			let targetForm=this.getActionForm("/support/enableAll");				
			targetForm["enableYn"].value=enableYn;
			targetForm.submit();
			return;
		};			
		this.clearHistory=function(confirmCheckValueYn) {
			if(confirmCheckValueYn=="Y"){
				let checkValue = String(Math.floor(Math.random()*1000000)).padStart(6, "0");
				let inputValue = prompt("모든 타스크의 수행로그를 삭제합니다.\n\n확인 입력이 필요합니다. (입력값 : " + checkValue + ")");
				if (inputValue != checkValue) {return;}
			}
			this.getActionForm("/support/clearHistory").submit();
			return;		
		};		
		this.initConfig=function(confirmCheckValueYn) {
			if(confirmCheckValueYn=="Y"){
				let checkValue = String(Math.floor(Math.random()*1000000)).padStart(6, "0");
				let inputValue = prompt("모든 타스크의 설정을 재구성 합니다.\n\n확인 입력이 필요합니다. (입력값 : " + checkValue + ")");
				if (inputValue != checkValue) {return;}
			}
			this.getActionForm("/support/initConfig").submit();
			return;
		};
		this.scanDeploy=function(confirmCheckValueYn) {
			if(confirmCheckValueYn=="Y"){
				let checkValue = String(Math.floor(Math.random()*1000000)).padStart(6, "0");
				let inputValue = prompt("배포/구성된 배치프로젝트를 적용 합니다.\n\n확인 입력이 필요합니다. (입력값 : " + checkValue + ")");
				if (inputValue != checkValue) {return;}
			}
			this.getActionForm("/support/scanDeploy").submit();	
		};
	};
			
	TaskClass=function() {
		let mainObj=null;
		let basePath=null;
		
		if(arguments.length===1) {this.mainObj=arguments[0];}			
		if(arguments.length===2) {this.mainObj=arguments[0];this.basePath=arguments[1];}
		
		this.consoleLog=function(){
			console.log(mainObj);
			console.log(basePath);
		};		
		this.init=function(mainObj) {
			this.mainObj=mainObj;
		};
		this.getActionForm=function(action) {
			let targetForm=document.getElementById("schedulerActionForm");
			targetForm.action=this.basePath+action;
			targetForm["taskId"].value="";
			return targetForm;
		};
		this.status=function(taskId,taskEnableYn,taskHiddenYn,taskDeleteYn,taskStatus,countBaseTime,executionCount,exceptionCount,interruptCount) {
			let addRunningDots=function(base,str) {
				if(str=="") {return "";}
				let addDots="<br>";
				let dotsCount=str.split(".").length;
				if(dotsCount>8) {dotsCount=1;}
				for(let i=0;i<dotsCount;i++) {addDots+=".";}
				return base+addDots;
			}
			let setInnerHTML=function(objName,value,color,countBaseTime) {
				let obj=document.getElementById(objName);
				if(obj==null) {return;}
				if(obj["assignValue"]==null) {obj["assignValue"]=value;}
				if(obj["assignValue"]==value) {obj.innerHTML=value;}
				if(obj["assignValue"]!=value) {obj.innerHTML="<font color="+color+"><b>"+value+"</b></font>";}
				obj["assignValue"]=value;
				if(countBaseTime!="") {obj["title"]="기준일:"+countBaseTime;}					
				return;
			}
			
			let status="";
			if(taskEnableYn=="N") {status+="disable";}
			if(taskEnableYn=="Y") {status+="<font color=green><b>enable</b></font>";}
			if(taskHiddenYn=="Y") {status+="<br><font color=#D3D3D3><b>hidden</b></font>";}
			if(taskDeleteYn=="Y") {status+="<br><font color=red><b>delete</b></font>";}
			if(taskStatus=="running") {status+=addRunningDots("<br><font color=blue><b>running</b></font>",document.getElementById(taskId+".status").innerHTML);}
			document.getElementById(taskId+".status").innerHTML=status;
			
			if(document.getElementById(taskId+".command")){
				document.getElementById(taskId+".command").value=(taskStatus=="running"?"중단":"실행");
			}
			
			setInnerHTML(taskId+".executionCount",executionCount,"blue",countBaseTime);
			setInnerHTML(taskId+".exceptionCount",exceptionCount,"red",countBaseTime);
			setInnerHTML(taskId+".interruptCount",interruptCount,"gray",countBaseTime);

			return;
		};
		this.filterLabel=function(){
			let createLabel = function(groupName,spanId,target1Obj,target2Obj,xText,xValue) {
				const spanObj = document.getElementById(spanId);
				if(!spanObj || !target1Obj || !target2Obj){return;}
				
				const isActive = target2Obj.value == xValue ? true : false;

			    const label = document.createElement("label");
			    label.textContent = xText;
				label.title = "검색영역선택: "+ xText;
				label.setAttribute("data-group", groupName);
			    label.style.cursor = "pointer";
				label.style.pointerEvents = isActive?"none":"auto";
			    label.style.textDecoration = isActive?"underline":"none";
				label.style.textDecorationColor = "green";
				label.style.textDecorationThickness = "3px";
				label.style.textUnderlineOffset = "3px";	
				
			    label.addEventListener("click", function () {
					if (label.style.textDecoration === "underline") return;					
			        const groupLabels = document.querySelectorAll("label[data-group="+groupName+"]");
			        groupLabels.forEach(l => {
						l.style.textDecoration = "none";
						l.style.pointerEvents = "auto";			
					});					
					label.style.pointerEvents = "none";
					label.style.textDecoration = "underline";
					label.style.textDecorationColor = "green";
					label.style.textDecorationThickness = "3px";
					label.style.textUnderlineOffset = "3px";				
					target1Obj.placeholder = xText;
					target2Obj.value = xValue;
			    });
				
			    spanObj.innerHTML = "";
			    spanObj.appendChild(label);
			};

			let target1Obj = document.getElementById("searchFilterText");
			let target2Obj = document.querySelector('input[name="searchFilterArea"]');
			if (target2Obj && target2Obj.value === "") {target2Obj.value="group";}
			
			createLabel("filterAreaGroup", "filterType1", target1Obj, target2Obj, "그룹", "group");
			createLabel("filterAreaGroup", "filterType2", target1Obj, target2Obj, "타스크" ,"name");
			createLabel("filterAreaGroup", "filterType3", target1Obj, target2Obj, "타스크설명", "description");
			
			return;
		};
		this.retrieveMain=function(refObj,key,initValue,containerId){
			if(refObj.id=="searchFilterText"){
				if(!refObj.hasAttribute('initWith')) {refObj.setAttribute('initWith',refObj.style.width);}
				if(refObj.value!=initValue && refObj.value.length > 10) {refObj.style.width="400px";}
				else{refObj.style.width=refObj.getAttribute('initWith');}
			}
			let container=document.getElementById(containerId);						
			let button=container.querySelector('button');				
			if(button!=null){
				button.style.display=(refObj.value.trim()==initValue)?"none":"inline-block";				
				if(key=="Enter" && refObj.value.trim()!=initValue){button.onclick();return;}
				return;
			}
			
			button=document.createElement("button");
			button.type="button";
			button.innerHTML="enter";
			button.metaObj=this;
			button.refObj=refObj;			
			button.onclick=function(){
				let targetForm=this.metaObj.getActionForm("/main");								
				let value=this.refObj.value.trim();
				if(this.refObj.id=="searchFilterText"){
					if(value=="인증키"){targetForm["authKeyListYn"].value="Y";}
					else{targetForm["searchFilterText"].value=(value==null || value=="")?"*":value;}
				}
				if(this.refObj.id=="pageSize"){
					targetForm["taskListPageSize"].value=value;
				}
				targetForm.submit();
				return;
			}
			container.appendChild(button);
			return;				
		};
		this.retrieveFilter=function(isEnable){
			let targetForm=this.getActionForm("/main");
			targetForm["hiddenFilterYn"].value=isEnable?"Y":"N";
			targetForm.submit();
			return;
		}
		this.retrieveNavi=function(direction){
			let targetForm=this.getActionForm("/main");
			if(direction=="prev"){
				if(targetForm["taskListPageNo"].value=="1"){return;}
				targetForm["taskListPageNo"].value=parseInt(targetForm["taskListPageNo"].value)-1;
			}
			if(direction=="next"){
				targetForm["taskListPageNo"].value=parseInt(targetForm["taskListPageNo"].value)+1;				
			}
			targetForm.submit();
			return;				
		};	
		this.enable=function(taskId,taskName,enableYn) {	
			if(!confirm(taskName+"을(를) "+(enableYn=="Y"?"enable":"disable")+"합니다.")) {return;}
			let targetForm=this.getActionForm("/enable");
			targetForm["taskId"].value=taskId;
			targetForm["enableYn"].value=enableYn;
			targetForm.submit();
			return;
		};
		this.command=function(element,taskId,taskName) {		
			if(element.value=="실행") {this.execute(taskId,taskName);}
			if(element.value=="중단") {this.interrupt(taskId,taskName);}
			return;
		};
		this.execute=function(taskId,taskName) {
			if(!confirm(taskName+"을(를) 실행합니다.")) {return;}
			let targetForm=this.getActionForm("/execute");
			targetForm["taskId"].value=taskId;
			targetForm.submit();
			return;
		};
		this.interrupt=function(taskId,taskName) {
			if(!confirm(taskName+"을(를) 중단합니다.")) {return;}
			let targetForm=this.getActionForm("/interrupt");
			targetForm["taskId"].value=taskId;
			targetForm.submit();
			return;				
		};			
		this.detail=function(taskId,taskName) {
			let targetForm=this.getActionForm("/detail");
			targetForm["taskId"].value=taskId;
			targetForm["taskName"].value=taskName;
			targetForm["activeTab"].value="tab1";
			targetForm.submit();
			return;
		};		
	};
}	


this.fncChangeTrColor=function(trObj) {
	let oldColor=trObj.style.backgroundColor;
	trObj.style.backgroundColor="#bbccdd";
	trObj.onmouseout=function() {trObj.style.backgroundColor=oldColor;}
	return;
}	

this.fncResizeInput=function(refObj){
	if(refObj.value.length > 20) {refObj.style.width="400px";}
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
