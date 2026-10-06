package com.alpha.craft.console.service.impl;

import org.springframework.stereotype.Service;

import com.alpha.base.support.batch.SchedulerManagerPack.SchedulerManager;
import com.alpha.craft.console.service.ConsoleServicePack.MainService;

@Service
public class MainServiceImpl implements MainService {

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private SchedulerManager schedulerManager;
	
	public MainServiceImpl(SchedulerManager schedulerManager){
		this.schedulerManager = schedulerManager;
	}
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
    public SchedulerManager getSchedulerManager() {
        return schedulerManager;
    }
    
	@Override
	public boolean isEnableRequest(String requestPath) {
		SchedulerManager schedulerManage=this.getSchedulerManager();
        boolean isEnable=true;
        if(isEnable && requestPath==null) {isEnable=false;}      
        if(isEnable && requestPath.equals("")) {isEnable=false;}   
        if(isEnable && schedulerManage==null) {isEnable=false;}
        if(isEnable && schedulerManage.getSchedulerRequestPath()==null) {isEnable=false;}
        if(isEnable && schedulerManage.getSchedulerRequestPath().equals("")) {isEnable=false;}
        if(isEnable && schedulerManage.getSchedulerRequestPath().equals(requestPath)==false) {isEnable=false;}
        return isEnable;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}