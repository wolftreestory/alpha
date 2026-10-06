package com.alpha.scheduler.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

public class ExecuteInfoPack{

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Data
	@Schema(description="프로세스 실행요청정보 객체")
	public static class ExecuteRequest implements Serializable {
		private static final long serialVersionUID = 1L;
		
		@Schema(description="배치 잡이름")
		private String jobName;	
				
		@Schema(description="배치 잡파라메터 static")
		private Map<String,Object> jobParameters;

		@Schema(description="배치 잡파라메터 dynamic")
		private String jobParametersApiUrl;

		@Schema(description="배치 수행결과 콜백")
		private String callbackApiUrl;
		
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Data
	@Schema(description="프로세스 실행응답정보 객체")
	public static class ExecuteResponse implements Serializable {
		private static final long serialVersionUID = 1L;
		
		@Schema(description="배치 잡이름")
		private String jobName;	
		
		@Schema(description="프로세스 실행ID : agent를 통해 수행된 jvm 프로세스 구별 값")	
		private long processExecuteId;	
		
		@Schema(description="프로세스 실행일시")
		private Date processExecuteDate;
		
		@Schema(description="콘솔 출력 : agent를 통해 수행된 프로세스의 콘솔 출력 값")
		private String console;	
		
		@Schema(description="오류 출력 : agent를 통해 수행된 프로세스의 오류 출력 값")
		private String error;	
			
		@Schema(description="종료 값 : agent를 통해 수행된 프로세스의 종료 값, if(exitValue==0) 정상처리 else 비정상처리")
		private String exitValue;
		
		@Schema(description="프로세스 실행상태 객체")
		private ExecuteStatus processStatus;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Data
	@Schema(description="프로세스 실행상태 객체")
	public static class ExecuteStatus implements Serializable {
		private static final long serialVersionUID = 1L;
		
		@Schema(description="프로세스 user")
		private String uid;	
		
		@Schema(description="프로세스 번호")
		private String pid;	
		
		@Schema(description="프로세스 부모번호")
		private String ppid;	
		
		@Schema(description="프로세스 cpu사용률")
		private String cpu;	
		
		@Schema(description="프로세스 시작시간")
		private String stime;	
		
		@Schema(description="프로세스 수행터미널번호")
		private String tty;	
		
		@Schema(description="프로세스 수행시간")
		private String time;	
		
		@Schema(description="프로세스 명령어")
		private String cmd;	
		
		@Schema(description="프로세스 jvm")
		private String jvm;	
		
		@Schema(description="프로세스 jvm에서 실행되는 jar어플리케이션(스프링배치) 패키지")
		private String jar;	
		
		@Schema(description="프로세스 jvm에서 할당 메모리")
		private String memory;
		
		@Schema(description="프로세스 jvm에서 실행되는 jar어플리케이션(스프링배치) spring.profiles.active")
		private String profile;	
		
		@Schema(description="프로세스 jvm에서 실행되는 jar어플리케이션(스프링배치) spring.batch.job.names")
		private String jobName;

		@Schema(description="프로세스 jvm에서 실행되는 jar어플리케이션(스프링배치) jobParameters-encode")
		private String jobParameters;
		
		@Schema(description="프로세스 jvm에서 실행되는 jar어플리케이션(스프링배치) jobParameters-decode")
		private String jobParametersDecode;
	}	

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}