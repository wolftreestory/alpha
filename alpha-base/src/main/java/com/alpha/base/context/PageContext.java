package com.alpha.base.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.alpha.base.advice.PageAdvice;
import com.alpha.base.support.AbstractMeta;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequestScope
@ConditionalOnExpression("T(com.alpha.base.PageContext).isEnabled()")
@ConditionalOnWebApplication
public class PageContext extends AbstractMeta {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static class PageInfo {
		private int rowSize = 10; 
		private int pageNo = 0; //current
		private int totalPage = 0;
		private int start = 1;
		private int last = 1;
		private int totalCount = 0;
		
		public int getRowSize() {return rowSize;}
		public void setRowSize(int rowSize) {this.rowSize = rowSize;}
		public int getPageNo() {return pageNo;}
		public void setPageNo(int pageNo) {this.pageNo = pageNo;}
		public int getTotalPage() {return totalPage;}
		public void setTotalPage(int totalPage) {this.totalPage = totalPage;}
		public int getStart() {return start;}
		public void setStart(int start) {this.start = start;}
		public int getLast() {return last;}
		public void setLast(int last) {this.last = last;}
		public int getTotalCount() {return totalCount;}
		public void setTotalCount(int totalCount) {this.totalCount = totalCount;}
		@Override
		public String toString() {
			return "PageInfo [rowSize=" + rowSize + ", pageNo=" + pageNo + ", totalPage=" + totalPage + ", start="+ start + ", last=" + last + ", totalCount=" + totalCount + "]";
		}				
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final static String[] pageParams = {"pageNo","rowSize","totalCount"};	

	private PageInfo pageInfo;
	
	public PageContext() {
		this.pageInfo = new PageInfo();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public int getRowSize() {return this.pageInfo.getRowSize();}
	public void setRowSize(int rowSize) {this.pageInfo.setRowSize(rowSize);}

	public int getPageNo() {return this.pageInfo.getPageNo();}
	public void setPageNo(int pageNo) {this.pageInfo.setPageNo(pageNo);}

	public int getTotalPage() {return this.pageInfo.getTotalPage();}
	public void setTotalPage(int totalPage) {this.pageInfo.setTotalPage(totalPage);}

	public int getStart() {return this.pageInfo.getStart();}
	public int getLast() {return this.pageInfo.getLast();}
	public int getTotalCount() {return this.pageInfo.getTotalCount();}
	public void setTotalCount(int totalCount) {
		this.pageInfo.setTotalCount(totalCount);

		int start=0;
		if(this.getPageNo()>1) {
			start=(this.pageInfo.getPageNo()-1)*this.getRowSize()+1;
			start=start>=2?start-1:start;
		}
		this.pageInfo.setStart(start);
		this.pageInfo.setLast((start-1)+this.getRowSize());		
		this.pageInfo.setTotalPage((totalCount/this.getRowSize())+(totalCount%this.getRowSize()>0?1:0));
	}
		
	public boolean isLast() {
		if(this.getTotalPage()==0) {return false;}
		return this.getTotalPage()-this.getPageNo()>0?false:true;
	}
	
	public boolean isPaging() {
		return this.getPageNo() > 0;
	}
	
	public Map<String,Object> toMap() {
		Map<String,Object> map = new HashMap<>();
		map.put("rowSize",this.getRowSize());
		map.put("pageNo",this.getPageNo());
		map.put("totalPage",this.getTotalPage());
		map.put("totalCount",this.getTotalCount());
		//map.put("start",this.getStart());
		//map.put("last",this.getLast());
				
		return map;		
	}

	public String toJsonBase64() {		
		return MetaUtil.getBase64Util().getEncodeBase64String(this.toJson());	
	}
	
	public String toJson() {
		return new JSONObject(this.toMap()).toJSONString();		
	}
	
	@Override
	public String toString() {
		return "PageContext"+this.pageInfo.toString();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void set(CheckerFunction checker, DataFunction data) {
		this.set(this, checker, data);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static PageContext get(Map<String, Collection<String>> header){	
		String pagingKey = PageAdvice.getPagingKey();
	    if(!header.containsKey(pagingKey)) {return null;}
	    
		Collection<String> values = header.get(pagingKey);
		if(values==null || values.isEmpty()) {return null;}
		
		//JSONObject jsonObj=getJsonObject(new ArrayList<>(values).get(0));
		JSONObject jsonObj = MetaUtil.getJsonUtil().getJsonObject(new ArrayList<>(values).get(0));
		if(jsonObj==null || jsonObj.isEmpty()) {return null;}
		
		PageContext pageContext = new PageContext();
		pageContext.set(pageContext, key->jsonObj.containsKey(key),key->String.valueOf(jsonObj.get(key)));
	
		if(log.isDebugEnabled()) {
			log.debug(">> pageContext[ref:header]:{}",pageContext.toString());
		}
		
		return pageContext;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@FunctionalInterface
	public interface CheckerFunction {public abstract boolean contains(String key);}

	@FunctionalInterface
	public interface DataFunction {public abstract String get(String key);}
	
	public void set(PageContext pageContext, CheckerFunction checker, DataFunction data) {
		List<String> keyList = Arrays.asList(pageParams);
		for(String key : keyList) {
			if(!checker.contains(key)) {continue;}
			
			String value = data.get(key);

			//log.debug(">> key:value=[{}:{}]",key,value);

			if(value==null || value.equals("")) {continue;}
			else if(key.equalsIgnoreCase(pageParams[0])) {pageContext.setPageNo(Integer.valueOf(value));}
			else if(key.equalsIgnoreCase(pageParams[1])) {pageContext.setRowSize(Integer.valueOf(value));}
			else if(key.equalsIgnoreCase(pageParams[2])) {pageContext.setTotalCount(Integer.valueOf(value));}				
		}		
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
