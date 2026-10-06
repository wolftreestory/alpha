package com.alpha.batch.config;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.springframework.util.StringUtils;

import ch.qos.logback.core.FileAppender;

public class BatchFileAppender<E> extends FileAppender<E> {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static String logUniqueId = "";
	
	private String batchLogFilePattern = "";

	private String traceTokenKeyName = "alpha.agent.trace.token";
	
	private String notePathKeyName = "alpha.agent.note.path";

	private static Set<String> metaLogDataSet = new HashSet<>();
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
    public void setMaxHistory(int maxHistory) {    	
    	this.cleanLogFile(this.batchLogFilePattern,maxHistory);
    }
    
    public void setTraceTokenKeyName(String traceTokenKeyName) {    	
    	this.traceTokenKeyName = traceTokenKeyName;
    }

    public void setNotePathKeyName(String notePathKeyName) {    	
    	this.notePathKeyName = notePathKeyName;
    }

    public void setBatchLogFilePattern(String batchLogFilePattern) {	
    	if(batchLogFilePattern==null || batchLogFilePattern.equals("")) {return;}
    	
    	this.batchLogFilePattern = batchLogFilePattern;
    	    	
    	if(batchLogFilePattern.indexOf("@date@")==-1) {
    		super.setFile(batchLogFilePattern);
    		return;
    	}

		String traceTokenValue = "";
		if(StringUtils.hasText(this.traceTokenKeyName)) {
			String traceToken = System.getProperty(this.traceTokenKeyName);
			if(StringUtils.hasText(traceToken)) {traceTokenValue = traceToken.split(":")[1];}
		}
    	//System.out.println("traceKeyValue: "+traceKeyValue);
		//-Dalpha.agent.metalog.path=/app/package/alpha-batch-pack1-dummyJob/package/.metalog1234
		String notePathValue = "";
		if(StringUtils.hasText(this.notePathKeyName)) {
			notePathValue = System.getProperty(this.notePathKeyName);
		}
    	//System.out.println("traceKeyValue: "+traceKeyValue);

    	synchronized(logUniqueId){
    		if(logUniqueId.equals("")) {
	    		logUniqueId = System.getProperty("alpha.jvm.start.date", (new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(System.currentTimeMillis()))).toString());
		    	if(StringUtils.hasText(traceTokenValue)) {
		    		logUniqueId = logUniqueId+"-"+traceTokenValue;
		    	}	    		
	    	}	    		    	
    	}
    	//System.out.println("logUniqueId: "+logUniqueId);
    	
    	String batchLogFile = batchLogFilePattern;    	
    	batchLogFile = batchLogFile.replace(",","-");
    	batchLogFile = batchLogFile.replace("@date@",logUniqueId);
    	super.setFile(batchLogFile);
        //System.out.println("logFile: "+super.getFile());

		metaLogDataSet.add("logfile:"+super.getFile().toString());

        if(StringUtils.hasText(notePathValue)) {
    		this.createMetaLogFile(notePathValue, metaLogDataSet);        
    	}        
    }

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void cleanLogFile(String batchLogFilePattern, int maxCount) {
    	if(batchLogFilePattern==null || batchLogFilePattern.equals("")) {return;}
    	if(batchLogFilePattern.indexOf("@date@")==-1) {return;}
    	    	
    	int fileNameLength = this.extractFileName(batchLogFilePattern.replace("@date@", "yyyyMMddHHmmss")).length();
    	String filePattern = this.extractFileName(batchLogFilePattern);
    	String directory = batchLogFilePattern.replace(filePattern,"");

    	//System.out.println("fileNameLength: "+fileNameLength);
    	//System.out.println("filePattern: "+filePattern);
    	//System.out.println("directory: "+directory);
    	
    	File logDirectory = new File(directory);
    	
    	for(File file : logDirectory.listFiles()) {
    		
    		if(file.length() == 0 && file.getName().indexOf(logUniqueId)==-1) {
    			//System.out.println("delete log size zero: "+file.getName());
    			file.delete();
    		}
    	}
    	
    	File files[] = logDirectory.listFiles(new FilenameFilter() {
    		String patterns[] = filePattern.split("@date@");
    		String startPattern = patterns[0];
    		String endPattern = patterns[1];
        	
    	    @Override
    	    public boolean accept(File dir, String name) {
    	    	return (name.length()==fileNameLength && name.startsWith(startPattern) && name.endsWith(endPattern))?true:false;
    	    }
    	});
    	Arrays.sort(files,LastModifiedFileComparator.LASTMODIFIED_REVERSE);

    	int skip = maxCount;
    	for(File file : files) {    		
    		if(skip>1) {skip--;continue;}
    		file.delete();
			//System.out.println("delete logFile: "+ file.getName());
    	}
    }

	private String extractFileName(String path) {
		path = path.trim();
		String name = path.substring(path.lastIndexOf("\\")+1, path.length());
		if(path.indexOf("/")>-1) {name = path.substring(path.lastIndexOf("/")+1, path.length());}
		return name;
	}

	private void createMetaLogFile(String filePath, Set<String> set) {

        Path directoryPath = Paths.get(filePath).getParent();

	    try {
	        File directory = new File(directoryPath.toString());
	        if(!directory.exists()){directory.mkdirs();}
        
	        FileWriter writer = new FileWriter(new File(filePath));
	        set.forEach(i-> {
				try {writer.write(i+System.lineSeparator());} catch (IOException e) {e.printStackTrace();}
			});
	        writer.close();
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
    }
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}

