package com.alpha.opus.task.xtra;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.alpha.base.support.batch.SchedulerManagerPack.SchedulerManager;
import com.alpha.base.support.batch.TaskPack.AbstractTask;
import com.alpha.base.support.util.SystemUtilPack.SystemUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile({"local","dev"})
public class SampleTask01 extends AbstractTask {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public SampleTask01(SchedulerManager schedulerManager,SystemUtil systemUtil) {

	    this.setGroup("xtra");
        this.setName("샘플타스크01");
        this.setDescription("예제1 fixedRate");
        this.setEnableYn(systemUtil.isLocal() ? "N" : "Y");
        this.setEnableYn("Y");
        this.setFixedRate("3000"); //mills
        this.setHiddenYn("Y");
	    schedulerManager.addTask(this);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void task() throws Exception {
		
		log.debug(">> SampleTask01 *** fixedRate");
		
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
