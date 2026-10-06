package com.alpha.opus.task.xtra;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.alpha.base.support.batch.TaskConfigPack;
import com.alpha.base.support.batch.SchedulerManagerPack.SchedulerManager;
import com.alpha.base.support.batch.TaskConfigPack.TaskConfig;
import com.alpha.base.support.batch.TaskConfigPack.TaskProperties;
import com.alpha.base.support.batch.TaskPack.AbstractTask;
import com.alpha.base.support.util.SystemUtilPack.SystemUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile({"local","dev"})
public class SampleTask02 extends AbstractTask {
		
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public SampleTask02(SchedulerManager schedulerManager,SystemUtil systemUtil) {
		        
        TaskConfig taskConfig = TaskConfigPack.getTaskConfig(schedulerManager.getDefaultTaskConfig());

        TaskProperties properties = new TaskProperties();
        properties.setProperty("sleep", "100");
        taskConfig.setBizProps(properties);
        
        List<String> logExceptPatternList = new ArrayList<>();
        logExceptPatternList.add("jdbc.sqlonly");
        logExceptPatternList.add("jdbc.resultsettable");        
        taskConfig.setLogExceptPatternList(logExceptPatternList);

	    this.setGroup("xtra");
        this.setName("샘플타스크02");
        this.setDescription("예제2 fixedDelay");
        this.setEnableYn(systemUtil.isLocal() ? "N" : "Y");
        this.setEnableYn("Y");
        
        this.setFixedDelay("5s"); //5000 mills
        this.setTaskConfig(taskConfig); //미설정시 defaultTaskConfig가 적용됨
        this.setHiddenYn("Y");

        schedulerManager.addTask(this);
	}
    
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void task() throws Exception {

		int sleep=Integer.parseInt(this.getTaskConfigBizProperty("sleep"));
		
		for(int i=0;i<10;i++) {        	
            Thread.sleep(sleep);
            log.debug("sleep: {}",i);
        }	    
        
		log.debug(">> SampleTask03 *** fixedDelay");
        
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
}