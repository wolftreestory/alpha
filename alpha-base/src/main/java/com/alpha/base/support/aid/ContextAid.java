package com.alpha.base.support.aid;

import java.util.ArrayList;
import java.util.Collection;

import org.json.simple.JSONObject;

import com.alpha.base.advice.PageAdvice;
import com.alpha.base.context.MaskContext;
import com.alpha.base.context.PageContext;
import com.alpha.base.support.AbstractMeta;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ContextAid extends AbstractMeta {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final PageContext getPageContext() {
    	if(getThreadLocalMap().containsKey("pageContext")) {return (PageContext)getThreadLocalMap().get("pageContext");}    	
    	return getBean(PageContext.class);
    }

    public static final PageContext getPageContext(int totalCount) {
		PageContext context = getPageContext();
		if(context!=null) {context.setTotalCount(totalCount);}
	    return context;
    }

    public static final PageContext getPageContext(org.springframework.http.HttpHeaders header) {

		Collection<String> values = header.get(PageAdvice.getPagingKey());

		String value = new ArrayList<>(values).get(0);
		
		JSONObject jsonObj = MetaUtil.getJsonUtil().getJsonObject(MetaUtil.getBase64Util().getDecodeBase64String(value));
		if(jsonObj==null || jsonObj.isEmpty()) {throw new RuntimeException("json conversion error");}
		
		PageContext pageContext = new PageContext();
		pageContext.set(pageContext, key->jsonObj.containsKey(key),key->String.valueOf(jsonObj.get(key)));
	
		if(log.isDebugEnabled()) {
			log.debug(">> pageContext[response:header]:{}",pageContext.toString());
		}
		
		return pageContext;
	}
	
    public static final MaskContext getMaskContext() {
    	return MetaUtil.getBean(MaskContext.class);
    }	

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}