package com.alpha.scheduler.vo;

import java.io.Serializable;

import com.alpha.base.context.SchedulerContext.TriggerType;
import com.alpha.scheduler.vo.ExecuteInfoPack.ExecuteRequest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

public class ScedulerInfoPack{

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Data
	@Schema(description="스케쥴 작업 정보 객체")
	public static class AgentWork implements Serializable {
		private static final long serialVersionUID = 1L;
		
		@Schema(description="이름")
		private String name;
		
		@Schema(description="설명")
		private String description;
		
		@Schema(description="트리거 타입")
		private TriggerType triggerType;
		
		@Schema(description="트리거 값")
		private String triggerValue;

		@Schema(description="트리거 시작일(yyyyMMddHHmmss)")
		private String triggerStartDate;

		@Schema(description="트리거 종료일(yyyyMMddHHmmss)")
		private String triggerEndDate;

		@Schema(description="프로세스 실행요청정보 객체")
		private ExecuteRequest executeRequest;		
	}

	/* 
	------------------------------------------------------------------
	스프링 CRON 표현식 : 6개의 각 필드로 구성
	------------------------------------------------------------------
	필드명			값의 허용 범위 						허용된 특수문자
	------------------------------------------------------------------
	초  (Seconds)	0 ~ 59 							- * /
	분  (Minutes)	0 ~ 59 							- * /
	시  (Hours)		0 ~ 23 							- * /
	일  (Day)		1 ~ 31 							- * ? / L W
	월  (Month)		1 ~ 12 or JAN ~ DEC				- * /
	요일 (Week)		0 ~ 6 or SUN ~ SAT(7도 일요일) 	- * ? / L #	
	------------------------------------------------------------------
	* : 모든 값을 뜻합니다.
	? : 특정한 값이 없음을 뜻합니다. 
	- : 범위를 뜻합니다. (예) 월요일에서 수요일까지는 MON-WED로 표현
	, : 특별한 값일 때만 동작 (예) 월,수,금 MON,WED,FRI 
	/ : 시작시간 / 단위  (예) 0분부터 매 5분 0/5
	L : 일에서 사용하면 마지막 일, 요일에서는 마지막 요일(토요일)
	W : 가장 가까운 평일 (예) 15W는 15일에서 가장 가까운 평일 (월 ~ 금)을 찾음
	# : 몇째주의 무슨 요일을 표현 (예) 3#2 : 2번째주 수요일
	------------------------------------------------------------------
	"0 0/1 * * * *" : 모든 요일, 매월, 매일 1분마다 0초
	"0 0 12 * * *" : 모든 요일, 매월, 매일 12:00:00
	"0 15 10 * * *" : 모든 요일, 매월, 아무 날이나 10:15:00 
	"0 * 14 * * *" : 모든 요일, 매월, 매일, 14시 매분 0초 
	"0 0/5 14 * * *" : 모든 요일, 매월, 매일, 14시 매 5분마다 0초 
	"0 0/5 14,18 * * *" : 아무 요일, 매월, 매일, 14시, 18시 매 5분마다 0초 
	"0 0-5 14 * * *" : 아무 요일, 매월, 매일, 14:00 부터 매 14:05까지 매 분 0초 
	"0 10,44 14 ? 3 WED" : 3월의 매 주 수요일, 아무 날짜나 14:10:00, 14:44:00 
	"0 15 10 ? * MON-FRI" : 월~금, 매월, 아무 날이나 10:15:00 
	"0 15 10 15 * ?" : 아무 요일, 매월 15일 10:15:00 
	"0 15 10 L * ?" : 아무 요일, 매월 마지막 날 10:15:00 
	"0 15 10 ? * 6L" : 매월 마지막 금요일 아무 날이나 10:15:00
	"0 15 10 ? * 6#3" : 매월 3번째 금요일 아무 날이나 10:15:00
	------------------------------------------------------------------
	*/				
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}