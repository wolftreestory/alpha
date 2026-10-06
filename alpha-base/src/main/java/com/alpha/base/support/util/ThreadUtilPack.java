package com.alpha.base.support.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ThreadUtilPack {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@FunctionalInterface
	public interface MetaFunction {public abstract Object execute();}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface ThreadUtil {

		Map<String, Object> execute(List<ThreadJob> list)  throws Exception;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static class ThreadJob {
		private String id;
		private MetaFunction metafunction;

		public ThreadJob(MetaFunction metafunction) {
			this(null,metafunction);
		}

		public ThreadJob(String id,MetaFunction metafunction) {
			this.id = id;
			this.metafunction = metafunction;
		}

		public String getId() {return id;}
		public void setId(String id) {this.id = id;}

		public MetaFunction getMetaFunction() {return metafunction;}
		public void setMetaFunction(MetaFunction metafunction) {this.metafunction = metafunction;}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static ThreadUtil getThreadUtil() {

		return new ThreadUtil() {

			@Override
			public Map<String, Object> execute(List<ThreadJob> list) throws Exception {
				
				List<JobRunnable> threadList = new ArrayList<>();
				Set<String> idSet = new HashSet<>();
				
				for(int i=0;i<list.size();i++) {
					ThreadJob job = list.get(i);
					
					if(StringUtils.isBlank(job.getId())) {job.setId("job"+i);}
					if(idSet.contains(job.getId())) {throw new RuntimeException("ThreadJob's id ["+job.getId()+"] is duplicate.");}
					threadList.add(new JobRunnable(job.getId(),job.getMetaFunction()));
					idSet.add(job.getId());
				}
				if(threadList==null || threadList.isEmpty()) {return  null;}
				
				log.debug(">> start.thread:{}", idSet.toString());
				long time = System.nanoTime();
				
				Map<String, Object> resultMerge = new HashMap<>();

				//하나의 loop로 병합 금지
				for(JobRunnable thread:threadList) {thread.start();}
				for(JobRunnable thread:threadList) {thread.join();}
				for(JobRunnable thread:threadList) {resultMerge.putAll(thread.getResult());}

				log.debug(">> end.thread:{}, executeTime(ms):{}", idSet.toString(),TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-time));

				return resultMerge;
			}};

	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class JobRunnable implements Runnable {
		private String id;
		private MetaFunction metaFunction;
		private Thread thread;
		private Object resultObj;
		
		public JobRunnable(String id, MetaFunction metaFunction){
			this.id = id;
			this.metaFunction = metaFunction;
			this.thread = new Thread(this, "thread."+id);
		}
		
		@Override
		public void run() {			
			log.debug(">> thread.start:{}", this.thread.getName());
			long time = System.nanoTime();
			this.resultObj = this.metaFunction.execute();
			log.debug(">> thread.end:{}, executeTime(ms):{}", this.thread.getName(),TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-time));	
		}

		public void start(){
			this.thread.start();
		}
		
		public void join() throws Exception{
			this.thread.join();
		}
		
		public Map<String,Object> getResult(){
			Map<String,Object> result = new HashMap<>();
			result.put(this.id, this.resultObj);
			return result;
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}