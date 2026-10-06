package com.alpha.base.support.util;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alpha.base.context.MaskContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MaskUtilPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface MaskUtil {
		public static final char MASK_CHAR='*';
		
		default public String maskName(String value) {return this.maskName(value,MASK_CHAR);}
		default public String maskJuminNo(String value) {return this.maskJuminNo(value,MASK_CHAR);}
		default public String maskDriverLicense(String value) {return this.maskDriverLicense(value,MASK_CHAR);}
		default public String maskPassPort(String value) {return this.maskPassPort(value,MASK_CHAR);}

		default public String maskPhone(String value) {return this.maskPhone(value,MASK_CHAR);}
		default public String maskEmail(String value) {return this.maskEmail(value,MASK_CHAR);}
		default public String maskIp(String value) {return this.maskIp(value,MASK_CHAR);}
		default public String maskAddress(String value) {return this.maskAddress(value,MASK_CHAR);}
		default public String maskBankAccount(String value) {return this.maskBankAccount(value,MASK_CHAR);}
		default public String maskCreditCard(String value) {return this.maskCreditCard(value,MASK_CHAR);}
		default public String maskMoney(String value) {return this.maskMoney(value,MASK_CHAR);}
		
		default public String maskFront(String value, int length) {return this.maskFront(value,length,MASK_CHAR);}
		default public String maskMiddle(String value, int start, int end) {return this.maskMiddle(value,start,end,null,MASK_CHAR);}
		default public String maskBack(String value, int length) {return this.maskBack(value,length,MASK_CHAR);}
		default public String maskAll(String value) {return this.maskAll(value,MASK_CHAR);}
		
		public String maskName(String value, char maskChar);
		public String maskJuminNo(String value, char maskChar);
		public String maskDriverLicense(String value, char maskChar);
		public String maskPassPort(String value, char maskChar);
		
		public String maskPhone(String value, char maskChar);
		public String maskEmail(String value, char maskChar);
		public String maskIp(String value, char maskChar);
		public String maskAddress(String value, char maskChar);
		public String maskBankAccount(String value, char maskChar);
		public String maskCreditCard(String value, char maskChar);
		public String maskMoney(String value, char maskChar);
		
		public String maskFront(String value, int length, char maskChar);
		public String maskMiddle(String value, int start, int end, String splitRegex, char maskChar);
		public String maskBack(String value, int length, char maskChar);
		public String maskAll(String value, char maskChar);

		public String maskRegex(String value, String regex, char maskChar);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
		
	public static MaskUtil getMaskUtil(MaskContext maskContext) {
		
		return new MaskUtil() {

			@Override
			public String maskName(String value, char maskChar) {
				if(!this.isApply(value)) {return value;}
				
				if(!this.isHangul(value)){
					if(value.length()>5) {return this.maskMiddle(value,2,2,(value.indexOf(" ")>1?" ":null),maskChar);}
					else {return this.maskAuto(value, maskChar);}
				}
				
				int length = value.length();
				
				if(length<2) {return value;}
				
				if(length<5) {
					int  pos = length==2?1:length-2;
					return value.substring(0,pos)+this.maskFront(value.substring(pos),1);
				}

				return this.maskMiddle(value,2,value.length()<7?1:2,(value.indexOf(" ")>1?" ":null),maskChar);
			}
			
			@Override
			public String maskJuminNo(String value, char maskChar) {
				if(!this.isApply(value)) {return value;}
				
				// 생년월일, 성별 이외 뒤 6자리 마스킹 : 800101-1******
				Pattern pattern = Pattern.compile("^(\\d{2}([0]\\d|[1][0-2])([0][1-9]|[1-2]\\d|[3][0-1])[-]*[1-4])(\\d{6})$"); 
				Matcher matcher = pattern.matcher(value); 
				if(!matcher.find()) {return this.maskForeignerNo(value,maskChar);}
				return new StringBuffer(matcher.group(1)).append(this.getMaskString(6,maskChar)).toString(); 
			}

			@Override
			public String maskDriverLicense(String value, char maskChar) {
				if(!this.isApply(value)) {return value;}	
				
				// 운전면허번호, 앞3자리, 뒤4자리 이외 마스킹 : 대구 0*-****56-71, 11-0*-****56-71
				Pattern pattern = Pattern.compile("^(\\d{2}|[가-힣]{2})[.-[ ]]?(\\d{2})[.-[ ]]?(\\d{6})[.-[ ]]?(\\d{2})$"); 
				Matcher matcher = pattern.matcher(value); 
				if(!matcher.find()) {return value;}

				StringBuilder builder= new StringBuilder();
		        for(int i=1;i<= matcher.groupCount();i++) {
		        	if(i==2) {
		        		builder.append(this.maskBack(matcher.group(i), 1, maskChar));
		        	}else if(i==3) {
		        		builder.append(this.maskFront(matcher.group(i), 4, maskChar));
		        	}else {
		        		builder.append(matcher.group(i));
		        	}
		        	
		        	builder.append(i<matcher.groupCount()?"-":"");
	
		        }
				return builder.toString();
			}

			@Override
			public String maskPassPort(String value, char maskChar) {
				if(!this.isApply(value)) {return value;}
				
				// 뒤 4자리 이외 마스킹 : *****4678
				Pattern pattern = Pattern.compile("^([a-zA-Z]{1}[0-9a-zA-Z]{1})(\\d{3})(\\d{4})$"); 
				Matcher matcher = pattern.matcher(value); 
				if(!matcher.find()) {return value;}

				return this.getMaskString(value.length()-4, maskChar)+matcher.group(3);
			}

			@Override
			public String maskPhone(String value, char maskChar) {
				if(!this.isApply(value)) {return value;}

	        	char zero='0';
	        	
				// 국번 뒤 2자리 마스킹, 번호 뒤 2자리 마스킹 : 010-99**-12**, 0100-99**-12**,070-99**-12**,0700-99**-12**, 02-****-1234, +82-10-****-5678
				String regExp = "^(\\d{3})[.-[ ]]?(\\d{3,4})[.-[ ]]?(\\d{4})$";
				if(value.charAt(3)==zero) {regExp = "^(\\d{3,4})[.-[ ]]?(\\d{4})[.-[ ]]?(\\d{4})$";}
				else if(value.startsWith("02")) {regExp = "^(\\d{2})[.-[ ]]?(\\d{3,4})[.-[ ]]?(\\d{4})$";}
				else if(value.startsWith("+82")) {regExp="^(\\+82)[.-[ ]]?([1]\\d{1}|[2]|[3-9]\\d{1})?[.-[ ]]?(\\d{3,4})[.-[ ]]?(\\d{4})$";}

	        	log.debug(">> regExp:{}",regExp);

	        	Pattern pattern = Pattern.compile(regExp);
				Matcher matcher = pattern.matcher(value);
				if(!matcher.find()) {return value;}
				
				String result = "";
				int groupCount = matcher.groupCount();
	        	//log.debug(">> groupCount:{}",groupCount);

		        for(int i=1;i<=groupCount;i++) {
		        	//log.debug(">> matcher.group(i):{}",matcher.group(i));
		        	if(matcher.group(i)==null) {continue;}
		        	if(i==1) {
		        		String temp=matcher.group(i);
		        		if(temp.startsWith("01") || temp.startsWith("07")){temp=matcher.group(i).substring(0,3);}
		        		else if(temp.lastIndexOf("00")!=-1) {temp=temp.substring(0,temp.length()-2);}
		        		else if(temp.lastIndexOf("0")!=-1) {temp=temp.substring(0,temp.length()-1);}		
		        		result += temp;
		        	} else {
		        		if(i==groupCount-2) {
		        			result += matcher.group(i);
		        		}
			        	if(i==groupCount-1) {
			        		String temp = this.maskBack(matcher.group(i), 2, maskChar);
			        		while(temp.startsWith("0")) {temp = temp.substring(1);}			        				
			        		result += temp;
			        	}
			        	if(i==groupCount-0) {
			        		result += this.maskBack(matcher.group(i), 2, maskChar);
			        	}       	
		        	}
		        	result += i<matcher.groupCount()?"-":"";
		        }

				return result;
			}

			@Override
			public String maskEmail(String value, char maskChar) {
				if(!this.isApply(value)) {return value;}
				
				// 계정 앞 3자리 외 마스킹 : abc****@hanmail.net
				Pattern pattern = Pattern.compile("^([a-zA-Z0-9._%+-]+)(@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6})$");	
				Matcher matcher = pattern.matcher(value);
				if(!matcher.find()) {return value;}
				
				StringBuilder builder= new StringBuilder();
				if(matcher.group(1).length()>3) {
					builder.append(this.maskBack(matcher.group(1),matcher.group(1).length()-3,maskChar));
				}else{					
					builder.append(this.maskAuto(matcher.group(1),maskChar));
				}
				builder.append(matcher.group(2));
				return builder.toString();
			}

			@Override
			public String maskIp(String value, char maskChar) {		
				if(!this.isApply(value)) {return value;}
				String result = this.maskIpv4(value, maskChar);
				return result.equals(value)?this.maskIpv6(value, maskChar):result;
			}
			
			@Override
			public String maskAddress(String value, char maskChar) {
				if(!this.isApply(value)) {return value;}				
				if(value.indexOf(" ")==-1) {return this.maskRegex(value, "[0-9]", maskChar).trim();}
				
				int startIndex = -1;
				String[] parts = value.split(" ");
				for(int i=0;i<parts.length;i++) {
					if(Pattern.compile("([0-9])").matcher(parts[i]).find()) {startIndex=i;break;}
				}
			
				String result = value;
				if(startIndex==-1) {
					int pos=-1;
					if(pos==-1 && (value.indexOf("로")>-1 && value.indexOf("로")==value.lastIndexOf("로"))) {pos = value.indexOf("로");} //순서변경 금지 
					if(pos==-1 && (value.indexOf("읍")>-1 && value.indexOf("읍")==value.lastIndexOf("읍"))) {pos = value.indexOf("읍");} //순서변경 금지
					if(pos==-1 && (value.indexOf("면")>-1 && value.indexOf("면")==value.lastIndexOf("면"))) {pos = value.indexOf("면");} //순서변경 금지
					if(pos==-1 && (value.indexOf("동")>-1 && value.indexOf("동")==value.lastIndexOf("동"))) {pos = value.indexOf("동");} //순서변경 금지
					if(pos==-1 && (value.indexOf("로 ")>-1 && value.indexOf("로 ")==value.lastIndexOf("로 "))) {pos = value.indexOf("로 ");} //순서변경 금지 
					if(pos==-1 && (value.indexOf("읍 ")>-1 && value.indexOf("읍 ")==value.lastIndexOf("읍 "))) {pos = value.indexOf("읍 ");} //순서변경 금지
					if(pos==-1 && (value.indexOf("면 ")>-1 && value.indexOf("면 ")==value.lastIndexOf("면 "))) {pos = value.indexOf("면 ");} //순서변경 금지
					if(pos==-1 && (value.indexOf("동 ")>-1 && value.indexOf("동 ")==value.lastIndexOf("동 "))) {pos = value.indexOf("동 ");} //순서변경 금지
					if(pos!=-1) {result = value.substring(0,pos+1)+this.maskRegex(value.substring(pos+1), "\\S", maskChar);}
				}else {
					String block1="",block2="";
					for(int i=0;i<parts.length;i++) {
						if(i<startIndex) {
							block1=block1+" "+parts[i];
						}else {
							block2=block2+" "+parts[i];
						}	
					}
					//log.debug(">> startIndex:{}",startIndex);
					//log.debug(">> block1:{}",block1);
					//log.debug(">> block2:{}",block2);
					//result = block1+" "+this.maskRegex(block2.trim(), "\\S", maskChar);
					result = block1+" "+maskChar+maskChar+maskChar; //마스킹되어야 하는 부분을 모두 *** 3자리로 표현
				}

				return result.trim();
			}

			@Override
			public String maskCreditCard(String value, char maskChar) {
				if(!this.isApply(value)) {return value;}
				
				int group4Cnt=value.replace("-","").replace(".","").length()-12;

				//앞3자리, 뒤4자리 이외 마스킹 : 123*-****-****-1234
				Pattern pattern = Pattern.compile("^([234569][0-9]{3})[-.[ ]]?([0-9]{4})[-.[ ]]?([0-9]{4})[-.[ ]]?([0-9]{"+group4Cnt+"})$");					
				Matcher matcher = pattern.matcher(value);
				if(!matcher.find()) {return value;}
				
				StringBuilder builder= new StringBuilder();
				builder.append(this.maskBack(matcher.group(1),1, maskChar));
				builder.append("-");
				builder.append(this.getMaskString(4,maskChar));
				builder.append("-");
				builder.append(this.getMaskString(4,maskChar));
				builder.append("-");
				builder.append(matcher.group(4));
				return builder.toString();
			}

			@Override
			public String maskMoney(String value, char maskChar) {
				if(!this.isApply(value)) {return value;}
				
				// 뒤 3자리 외 마스킹 : ***,***,000
				String temp = value.replace(",","");
				if(!Pattern.matches("^[0-9]*$", temp)) {return value;}				
				temp = NumberFormat.getInstance().format(Integer.parseInt(temp));
				return this.maskMiddle(temp, 0, 3, ",", maskChar);
			}
			
			@Override
			public String maskBankAccount(String value, char maskChar) {
				if(!this.isApply(value)) {return value;}
				
				//앞3자리, 뒤4자리 이외 마스킹 : 123-***-**3456
				if(value.length()<8) {return value;}
				return this.maskMiddle(value,3,4,(value.indexOf("-")>0?"-":null),maskChar);
			}

			@Override
			public String maskFront(String value, int length, char maskChar) {
				if(!this.isApply(value)) {return value;}
				if(value.length()<length) {return this.maskAll(value);}
				return this.getMaskString(length, maskChar)+value.substring(length);
			}

			@Override
			public String maskMiddle(String value, int sRemainCnt, int eRemainCnt, String splitRegex, char maskChar) {
				if(!this.isApply(value)) {return value;}
				
				if(value == null || value.equals("")){return value;}
				if(value.length()<sRemainCnt+eRemainCnt) {return value;}
				
				if(splitRegex==null || splitRegex.equals("")) {
					StringBuilder builder= new StringBuilder();
					builder.append(value.substring(0,sRemainCnt));
					builder.append(this.getMaskString(value.length()-(sRemainCnt+eRemainCnt), maskChar));
					builder.append(value.substring(value.length()-eRemainCnt));
					return builder.toString();
				
				}
				
				String array[] = value.split(splitRegex);
				int sIndex=0;
				int eIndex=array.length-1;
				
				if(array[sIndex].length()>sRemainCnt) {array[sIndex]=this.maskBack(array[sIndex], array[sIndex].length()-sRemainCnt, maskChar);}
				if(array[eIndex].length()>eRemainCnt) {array[eIndex]=this.maskFront(array[eIndex], array[eIndex].length()-eRemainCnt, maskChar);}
				
				StringBuilder builder= new StringBuilder();
				for(int i=0;i<array.length;i++) {
					builder.append((i==sIndex||i==eIndex)?array[i]:this.maskAll(array[i]));
					builder.append(i<eIndex?splitRegex:"");
				}
				return builder.toString();
			}
			
			@Override
			public String maskBack(String value, int length, char maskChar) {
				if(!this.isApply(value)) {return value;}
				if(value.length()<length) {return this.maskAll(value);}
				return value.substring(0,value.length()-length)+this.getMaskString(length, maskChar);
			}
			
			@Override
			public String maskAll(String value, char maskChar) {
				if(!this.isApply(value)) {return value;}
				return this.getMaskString(value.length(), maskChar);
			}

			@Override
			public String maskRegex(String value, String regex, char maskChar) {
				if(!this.isApply(value)) {return value;}
				return value.replaceAll(regex,String.valueOf(maskChar));
			}

			private String maskForeignerNo(String value, char maskChar) {
				// 생년월일, 성별 이외 뒤 6자리 마스킹 : 800101-1******
				Pattern pattern = Pattern.compile("^(\\d{2}([0]\\d|[1][0-2])([0][1-9]|[1-2]\\d|[3][0-1])[-]*[5-8])(\\d{4})(\\d{1})(\\d{1})$"); 
				Matcher matcher = pattern.matcher(value);
				if(!matcher.find()) {return value;}
				StringBuilder builder= new StringBuilder();
				builder.append(matcher.group(1));
				builder.append(this.getMaskString(6,maskChar));
				return builder.toString();
			}
						
			private String maskIpv4(String value, char maskChar) {				
				//127.123.xxx.253
				Pattern patternIPv4 = Pattern.compile("^([01]?\\d?\\d|2[0-4]\\d|25[0-5])\\.([01]?\\d?\\d|2[0-4]\\d|25[0-5])\\.([01]?\\d?\\d|2[0-4]\\d|25[0-5])\\.([01]?\\d?\\d|2[0-4]\\d|25[0-5])$");				
				Matcher matcherIPv4 = patternIPv4.matcher(value);
				if(!matcherIPv4.find()) {return value;}
				
				String result = "";
		        for(int i=1;i<= matcherIPv4.groupCount();i++) {		            
		            result += i==3?this.getMaskString(3, maskChar):matcherIPv4.group(i);
		            result += i<matcherIPv4.groupCount()?".":"";
		        }
				return result;
			}
			
			private String maskIpv6(String value, char maskChar) {	
				//0000:0000:0000:0000:0000:0000:0000:0000
				Pattern patternIPv6 = Pattern.compile("^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]).){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]).){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))$");
				Matcher matcherIPv6 = patternIPv6.matcher(value);
				if(!matcherIPv6.find()) {return value;}
				
				String array[]=value.split(":");				
				int applyIndex=array.length-1;
				int applyCount=2;
				
				while(true) {
					if(!array[applyIndex].equals("")) {
						if(array[applyIndex].indexOf(".")!=-1) {
							array[applyIndex]=this.maskIpv4(array[applyIndex],maskChar);
							applyCount=0;
						}else{
							array[applyIndex]=this.getMaskString(array[applyIndex].length(),maskChar);
							applyCount--;
						}
					}						
					if(applyCount==0 || applyIndex==0) {break;}
					applyIndex--;						
				}
				
				StringBuilder builder = new StringBuilder();
		        for(int i=0;i<array.length;i++) {
		        	builder.append(array[i]);
		        	builder.append(array.length-i==1?"":":");
		        }
				return builder.toString();					
			}
			
			private String maskAuto(String value, char maskChar) {
				if(value.length()==1) {return String.valueOf(maskChar);}
				int maskCount=value.length()/2+value.length()%2;
				int sRemainCnt=(maskCount==1?1:maskCount/2);
				int eRemainCnt=value.length()-(sRemainCnt+maskCount);
				return this.maskMiddle(value,sRemainCnt,eRemainCnt,null,maskChar);
			}
			
			private String getMaskString(int length, char maskChar) {
				char[] array = new char[length];
				Arrays.fill(array,maskChar);
				return String.valueOf(array);
			}

			private boolean isHangul(String value) {
				return value==null?false:value.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");
			}
			
			private boolean isApply(String value) {
				if(value==null || value.equals("")) {return false;}
				return maskContext==null?true:maskContext.isMaskApply();
			}
		};
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}