package com.alpha.batch.meta;

import java.io.Serializable;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public abstract class AbstractSchedulerMetaTaskInfoPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static enum ScheduleType {CRON_EXPRESSION, FIXED_RATE, FIXED_DELAY, NONE}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Getter @Setter @ToString
	public static class TaskMetaObject implements Serializable {
		private static final long serialVersionUID = 1L;
		private String projectName;		
		private String appName;
		private String jobName;
		private String jobConfigClass;
		private String jobDescription;
		private Set<String> jobParameters;	
		private TaskMetaObject.Schedule schedule;		
		private TaskMetaObject.Environment environment;
	
		@Getter @Setter @ToString
		public static class Schedule {
			private ScheduleType scheduleType;
			private String scheduleValue;
		}

		@Getter @Setter @ToString
		public static class Environment {
			private String executionScript;
			private String packagePath;
			private String repositoryPath;
		}		
	}
	

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}