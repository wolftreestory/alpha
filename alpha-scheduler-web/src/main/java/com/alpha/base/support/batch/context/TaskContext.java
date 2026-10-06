package com.alpha.base.support.batch.context;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.alpha.base.support.batch.TaskPack;
import com.alpha.base.support.batch.TaskPack.Handler.InterruptHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskContext implements Serializable {
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final long serialVersionUID=1L;	
	
	private static Map<String,TaskPack.Task> threadTaskContext=new HashMap<>();
	private static InterruptHandler defaultHandler=null;	
    
    public static synchronized void setThreadTask(int hashcode,TaskPack.Task task) {threadTaskContext.put(String.valueOf(hashcode), task);}
    public static TaskPack.Task getThreadTask(int hashcode) {return threadTaskContext.get(String.valueOf(hashcode));}
    public static void removeThreadTask(int hashcode) {threadTaskContext.remove(String.valueOf(hashcode));}
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void catchInterrupt() throws InterruptedException {
    	catchInterrupt("");
		return;
    }

    public static void catchInterrupt(String message) throws InterruptedException {    	
    	TaskPack.Task task=TaskContext.getThreadTask(Thread.currentThread().hashCode());
    	if(task==null) {log.debug("task is null.");return;}
    	
    	if(task.getInterruptHandler()==null) {
    		synchronized(defaultHandler==null?task:defaultHandler) {
	    		if(defaultHandler==null) {
	    			defaultHandler=new InterruptHandler() {
	    				String message=null;
						@Override public boolean isEnable() {return true;}
						@Override public String getMessage() {return this.message;}
						@Override public void setMessage(String message) {this.message=message;}
						@Override public void action() throws Exception {}
	    			};
	    		}
	    		defaultHandler.setMessage(message);
    		}
    		catchInterrupt(defaultHandler);
    		return;
    	}
    	task.getInterruptHandler().setMessage(message);
    	catchInterrupt(task.getInterruptHandler());
		return;
    }

    public static void catchInterrupt(InterruptHandler handler) throws InterruptedException {    	
    	TaskPack.Task task=TaskContext.getThreadTask(Thread.currentThread().hashCode());
    	if(task==null) {log.debug("task is null.");return;}    	
    	task.setInterruptHandler(handler);	
    	if(Thread.interrupted()) {throw new InterruptedException(handler.getMessage());}
		return;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
}

