package com.alpha.base.config.batch;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix="alpha.batch")
@Getter @Setter
public class BatchProperties {

	public static enum TaskLogHistoryType {file,mysql,oracle};
	
	private Agent agent;
	private WebConsole webConsole;
	private ScanConfig scanConfig;
	private Scheduler scheduler;
	private Task task;
	
	@Getter @Setter
	public static class Agent {
		private String appMeta;
		private String appBase;
		private List<String> appPrefixList;
	}
	
	@Getter @Setter
	public static class WebConsole {
		private String title;
		private String titleHtml;
		private String pageSize;
		private String account;
		private String confirmCheckValueYn = "Y"; //메인화면에서 중요기능 실행시 확인코드입력여부
		private List<String> accountList;
		private List<String> groupList;
	}
	
	@Getter @Setter
	public static class Scheduler {
		private String schedulerRequestPath = "alpha"; // 접근주소 http://[~]/[servlet-context]/[schedulerRequestPath]/
		private String schedulerAutoStartYn = "N"; // 서버기동후 스케쥴러자동시작여부(Y|N)
		private String schedulerAutoStartDelay = "5000"; // 서버기동후 스케쥴러자동시작지연(ms)
		private String schedulerStopCheckTerm = "1000"; // 스케쥴러중지시 수행중인 작업 체크시간(ms)
		private int schedulerPoolSize = 0; //schedulerPoolSize (0 일경우  CPU의 코어 개수를 설정 함)
		private String executionLogFilePathPattern;
		private String exceptionLogFilePathPattern;
	}
	
	@Getter @Setter
	public static class Task {
		private String configLocation;
		private List<String> runnableServerList;
		private String contextLocation;
		private TaskLogHistoryType logStoreType;
		private LogPolicy logPolicy;
		private String supportTaskConfigInitYn="Y"; // 타스크컨피크 초기화(코드에 설정한 값으로) 여부
		private String supportOutterTaskParamConfigYn="Y"; // outterTask 파라메터설정 여부
		private String supportOutterTaskChangeApplyYn="Y"; // outterTask 변경사항적용(추가/삭제/수정) 여부

		@Getter @Setter
		public static class LogPolicy {
			private String logFileDbStoreYn; // 로그파일DB저장여부 (logHistory.type이 mysql,oracle 일 경우 유효함)
			private String logFileRemoveYn; // 로그파일삭제여부 (DB저장 후)
			private String logHistoryShrinkYn; // 로그이력축소여부
			private String logHistoryShrinkBaseDays = "10"; // 로그이력축소기준 days
			private String logHistoryShrinkBaseRows = "100"; // 로그이력축소기준 rows
		}			
	}

	@Getter @Setter
	public static class ScanConfig {
		private List<String> scanPatternList;
	}
}