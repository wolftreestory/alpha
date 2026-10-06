package com.alpha.base.support.etc;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Sweeper  {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final Logger logger=LoggerFactory.getLogger(Sweeper.class);
		
	private static String[] replaceEashCRLF1 = new String[] {"\n","%0a","%0A","\r","%0d","%0D"};
	private static String[] replaceEashCRLF2 = new String[] {"","","","","",""};
	
	private static Pattern[] xssPatterns = new Pattern[] {
		Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE)
		,Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
		,Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
		,Pattern.compile("</script>", Pattern.CASE_INSENSITIVE)
		,Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
		,Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
		,Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
		,Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE)
		,Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE)
		,Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
		,Pattern.compile("src/", Pattern.CASE_INSENSITIVE)
	};

	private static Pattern[] sqlInjectionPatterns = new Pattern[] {
			Pattern.compile("#", Pattern.CASE_INSENSITIVE)
			,Pattern.compile(">", Pattern.CASE_INSENSITIVE)
			,Pattern.compile("<", Pattern.CASE_INSENSITIVE)
			,Pattern.compile("=", Pattern.CASE_INSENSITIVE)
			,Pattern.compile("[$]", Pattern.CASE_INSENSITIVE)
			,Pattern.compile("\"", Pattern.CASE_INSENSITIVE)
			,Pattern.compile("[|]", Pattern.CASE_INSENSITIVE)
			,Pattern.compile("'", Pattern.CASE_INSENSITIVE)
			,Pattern.compile("%", Pattern.CASE_INSENSITIVE)
			,Pattern.compile(";", Pattern.CASE_INSENSITIVE)
			,Pattern.compile("--", Pattern.CASE_INSENSITIVE)
			,Pattern.compile("[.][.]/", Pattern.CASE_INSENSITIVE)
			,Pattern.compile(" select ", Pattern.CASE_INSENSITIVE)
			,Pattern.compile(" update ", Pattern.CASE_INSENSITIVE)
			,Pattern.compile(" delete ", Pattern.CASE_INSENSITIVE)
			,Pattern.compile(" insert ", Pattern.CASE_INSENSITIVE)
			,Pattern.compile(" where ", Pattern.CASE_INSENSITIVE)
			,Pattern.compile(" from ", Pattern.CASE_INSENSITIVE)
			,Pattern.compile(" create ", Pattern.CASE_INSENSITIVE)
			,Pattern.compile(" drop ", Pattern.CASE_INSENSITIVE)
			,Pattern.compile(" or ", Pattern.CASE_INSENSITIVE)
		};
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String[] cleanXSS(String[] values) {
    	if(values==null || values.length==0) {return values;}
    	String[] cleans = Arrays.copyOf(values, values.length);
    	for(int i=0;i<values.length;i++) {
    		cleans[i]=cleanXSS(values[i]);
    	}
    	return cleans;
    }
	
    public static String cleanXSS(String value) {
    	if(value==null) {return null;}
		String cleanValue=value.replaceAll("\0", "");
		for(Pattern pattern : xssPatterns) {
			if(pattern.matcher(cleanValue).find()) {
				cleanValue=cleanValue.replaceAll("<","&lt;").replaceAll(">","&gt;");
				logger.debug("pattern:{},:inValue:{}",pattern.pattern(),value);
				break;
			}
		}		
    	logger.debug("cleanXSS:{}",cleanValue);
        return cleanValue;
    };

    public static String cleanSqlInjection(String inValue) {
    	if(inValue==null) {return null;}
		String cleanValue=inValue.replaceAll("\0", "");
		
		if(cleanValue.toUpperCase().startsWith("<P>") && cleanValue.toUpperCase().endsWith("</P>")) {
			return cleanValue;
		}
		
		for(Pattern pattern : sqlInjectionPatterns) {
			if(pattern.matcher(cleanValue.replaceAll("[&]lt[;]","").replaceAll("[&]gt[;]","")).find()) {
				cleanValue="<P>"+cleanValue+"</P>";
				logger.debug("pattern: {},:cleanValue: {}",pattern.pattern(),cleanValue);
				break;
			}
		}
		
    	logger.debug("cleanSqlInjection in: {},out: {}",inValue,cleanValue);
    	return cleanValue;
    }
    
    public static String getEacapeSqlInjectionBlockTag(String inValue) {
        if(inValue==null) {return null;}
        if(inValue.toUpperCase().startsWith("<P>") && inValue.toUpperCase().endsWith("</P>")) {
            return inValue.substring(3,inValue.length()-4);            
        }
        return inValue;
    }
    public static Map<String,String> removeCRLF(Map<String,String> map) {
    	if(map==null || map.isEmpty()) {return map;}
    	Map<String,String> result = new HashMap<>();
    	map.entrySet().forEach(e-> result.put(removeCRLF(e.getKey()), removeCRLF(e.getValue())));
    	return result;
   } 

    public static String removeCRLF(String str) {
    	return StringUtils.replaceEach(str,replaceEashCRLF1,replaceEashCRLF2);
    } 

    public static String normalizePath(String path) {
        if(path == null){return null;}
    	return URI.create(path).normalize().toString();
    }

    public static String normalizeURI(String uri) {
        if(uri == null){return null;}
    	return URI.create(uri).normalize().toString();
    }

    public static String normalizeFilename(String filename) {
        if(filename == null){return null;}
    	return FilenameUtils.normalize(filename);
    } 

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}