package com.alpha.scheduler.vo;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

public class BatchInfoPack{

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Data
	@Schema(description="배치어플리이션 정보 객체 : 어플리이션이 구성된 디렉토리를 scan한 정보")
	public static class BatchApplication implements Serializable {
		private static final long serialVersionUID = 1L;
		
		@Schema(description="배치어플리이션 이름")
		private String appName;
		
		@Schema(description="배치어플리이션 잡이름")
		private String jobName;
		
		@Schema(description="배치어플리이션 프로파일(dev|prod)")
		private String profile;
	
		@Schema(description="배치어플리이션 구동 메모리")
		private String memory;
	
		@Schema(description="배치어플리이션 수행 스크립트(bash)")
		private String script;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Data
	@Schema(description="배치어플리이션 실행파라메터 정보 객체 : 배치어플리이션이 수행된 파라메터정보를 가짐")
	public static class BatchExecuteParameter implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private String appName;
		private String jobName;	
		private String executeDate;	
		private String executeParameters;
	}	

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}