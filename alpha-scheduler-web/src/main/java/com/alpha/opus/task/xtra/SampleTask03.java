package com.alpha.opus.task.xtra;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.alpha.base.BaseUtil;
import com.alpha.base.support.batch.TaskConfigPack;
import com.alpha.base.support.batch.SchedulerManagerPack.SchedulerManager;
import com.alpha.base.support.batch.TaskConfigPack.TaskConfig;
import com.alpha.base.support.batch.TaskConfigPack.TaskProperties;
import com.alpha.base.support.batch.TaskPack.AbstractTask;
import com.alpha.base.support.batch.context.TaskContext;
import com.alpha.base.support.util.SystemUtilPack.SystemUtil;
import com.alpha.opus.domain.xtra.board.BoardServicePack.BoardService;
import com.alpha.opus.domain.xtra.board.vo.BoardVo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile({"local","dev"})
public class SampleTask03 extends AbstractTask {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public SampleTask03(SchedulerManager schedulerManager, SystemUtil systemUtil) {

		// 프로퍼티 설명이 있는 경우
		TaskProperties properties = new TaskProperties();
		properties.setProperty("loopCnt","1000","반복횟수");
		properties.setProperty("errorPoint","0","오류발생지점 설정, loopCnt보다 작아야 함");
		properties.setProperty("sleep","10","슬립카운트(단위:ms)");

		TaskConfig taskConfig = TaskConfigPack.getTaskConfig(schedulerManager.getDefaultTaskConfig(), properties);
		//taskConfig.setSaveImmediatelyYn("N");
		taskConfig.setScheduleTermEnableYn("Y");
		taskConfig.addScheduleTerm("2024-06-22", "2024-06-29");
	    this.setGroup("xtra");
	    this.setName("샘플타스크03");
	    this.setDescription("예제3 cronExpression");
	    this.setEnableYn(systemUtil.isLocal() ? "N" : "Y");
        this.setEnableYn("Y");
	    
	    this.setCronExpression("0 0/1 * * * ?");
	    this.setTaskConfig(taskConfig); //미설정시 defaultTaskConfig가 적용됨
        this.setHiddenYn("Y");
        
	    schedulerManager.addTask(this);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Autowired
	private BoardService boardService;
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void task() throws Exception {
		
		int max=Integer.parseInt(this.getTaskConfigBizProperty("loopCnt"));
		
		int sleep=Integer.parseInt(this.getTaskConfigBizProperty("sleep"));
		
		int errorPoint=Integer.parseInt(this.getTaskConfigBizProperty("errorPoint"));

		this.setInterruptHandler(new InterruptHandler() {
			String message=null;
			@Override public boolean isEnable() {return true;}
			@Override public String getMessage() {return this.message;}
			@Override public void setMessage(String message) {this.message=message;}
			@Override public void action() throws Exception {
				log.debug("** 타스크 중단됨 **");
				//throw new RuntimeException("타스크 중단됨");
			}
		});

		for(int i=0;i<max;i++) {

			if(sleep>0) {Thread.sleep(sleep);}
			
			log.debug("loop(반복): {}",i);

        	TaskContext.catchInterrupt("interrupted at task:"+this.getName());

			if(errorPoint>0 && errorPoint<max) {
				if(errorPoint==i) {throw new RuntimeException("error-test");}
			}
			
		}

		List<BoardVo> list = boardService.getList(new BoardVo(0));
		for(BoardVo item:list) {
			String json = BaseUtil.getJsonUtil().toJson(item);
			String enc = BaseUtil.getCryptoUtil().getAES256().encryption(json);
			log.debug(">> {}",enc);
		}
		
		log.debug(">> SampleTask03 *** Cron");
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}

