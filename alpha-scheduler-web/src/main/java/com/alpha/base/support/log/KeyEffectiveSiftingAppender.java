package com.alpha.base.support.log;

import org.slf4j.MDC;

import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;

public final class KeyEffectiveSiftingAppender extends SiftingAppender {
	
    @Override
    protected void append(ILoggingEvent event) {
    	String key=super.getDiscriminatorKey();

    	boolean isEnable=true;
    	if(isEnable && key==null) {isEnable=false;}
    	if(isEnable && MDC.get(key)==null) {isEnable=false;}
    	if(isEnable && MDC.get(key).equals("")) {isEnable=false;}
    	if(isEnable) {super.append(event);}

        if(super.eventMarksEndOfLife(event)) {super.getAppenderTracker().removeStaleComponents(Long.MAX_VALUE);}
        
        return;
    }


}