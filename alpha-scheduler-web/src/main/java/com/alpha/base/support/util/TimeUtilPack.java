package com.alpha.base.support.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public final class TimeUtilPack {
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface TimeUtil {

		public String getCurrentTime();
		public String getCurrentTime(String format);

		public Date getDate(String date,String format);
		
		public String getFormatDate(long timestamp);
		public String getFormatDate(long timestamp,String format);
		
		public String getFormatDate(Date date);
		public String getFormatDate(Date date,String format);
		
		public String getFormatDate(String date,String sourceFormat,String toFormat);

		public String addYear(String date,int addYear);
		public String addYear(String date,String format,int addYear);
		
		public String addMonth(String date,int addMonth);
		public String addMonth(String date,String format,int addMonth);
		
		public String addDay(String date,int addDay);
		public String addDay(String date,String format,int addDay);

		public String addHour(String date,int addHour);
		public String addHour(String date,String format,int addHour);

		public String addMinute(String date,int addMinute);
		public String addMinute(String date,String format,int addMinute);

		public String addSecond(String date,int addSecond);
		public String addSecond(String date,String format,int addSecond);

		public String addDetailTime(String date,int addYear,int addMonth,int addDay,int addHour,int addMinute,int addSecond);
		public String addDetailTime(String date,String format,int addYear,int addMonth,int addDay,int addHour,int addMinute,int addSecond);

	 	public long getDifferenceDays(String date1,String date2);
	 	public long getDifferenceHours(String date1,String date2);
	 	public long getDifferenceMinutes(String date1,String date2);
	 	public long getDifferenceSeconds(String date1,String date2);
	 	public long getDifferenceMillis(String date1,String date2);
	 	public long getDifference(String date1,String format1,String date2,String format2);
	 	
	 	public int getDayOfWeek();
	 		 	
	 	public long getTimeStamp(String date,String format);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static TimeUtil getTimeUtil() {

		return new TimeUtil() {
			@Override
			public String getCurrentTime() {
				return this.getCurrentTime(null);
			}
			
			@Override
			public String getCurrentTime(String format) {
				String simpleDateFormat=format;
				if(simpleDateFormat==null || simpleDateFormat.trim().equals("")) {simpleDateFormat="yyyyMMddHHmmss";}	
				return (new SimpleDateFormat(simpleDateFormat).format(new Date(System.currentTimeMillis()))).toString();
			}

			@Override
			public Date getDate(String date,String format){
				Date result = null;
				try {
					result = (new SimpleDateFormat(format)).parse(StringUtils.rightPad((date==null)?"":date,format.length(),"0"));
				} catch (ParseException e) {
					log.error(">> exception skip: {}",e.getMessage());
					//e.printStackTrace();
				}
				return result;
			}

			@Override
			public String getFormatDate(long timestamp) {
				return this.getFormatDate(new Date(timestamp));
			}
			
			@Override
			public String getFormatDate(long timestamp,String format) {
				return this.getFormatDate(new Date(timestamp),format);		
			}
			
			@Override
			public String getFormatDate(Date date) {
				return this.getFormatDate(date,"yyyyMMddHHmmss");		
			}
			
			@Override
			public String getFormatDate(Date date,String format) {
				if(date==null) {return "";}
				if(format==null || format.equals("")) {return "";}
				return (new SimpleDateFormat(format).format(date)).toString();
			}
				
			@Override
			public String getFormatDate(String source,String sourceFormat,String toFormat) {
				Date date = this.getDate(source, sourceFormat);
				return this.getFormatDate(date,toFormat);
			}

			@Override
			public String addYear(String date,int addYear) {
				return this.addYear(date,"yyyyMMdd",addYear);
			}

			@Override
			public String addYear(String date,String format,int addYear) {
				return this.addDetailTime(date,format,addYear,0,0,0,0,0);
			}
			
			@Override
			public String addMonth(String date,int addMonth) {
				return this.addMonth(date,"yyyyMMdd",addMonth);		
			}
			
			@Override
			public String addMonth(String date,String format,int addMonth) {
				return this.addDetailTime(date,format,0,addMonth,0,0,0,0);		
			}

			@Override
			public String addDay(String date,int addDay) {
				return this.addDay(date,"yyyyMMdd",addDay);	
			}
			
			@Override
			public String addDay(String date,String format,int addDay) {
				return this.addDetailTime(date,format,0,0,addDay,0,0,0);	
			}

			@Override
			public String addHour(String date,int addHour) {
				return this.addHour(date,"yyyyMMdd",addHour);	
			}
			
			@Override
			public String addHour(String date,String format,int addHour) {
				return this.addDetailTime(date,format,0,0,0,addHour,0,0);	
			}

			@Override
			public String addMinute(String date,int addMinute) {
				return this.addMinute(date,"yyyyMMddHHmm",addMinute);
			}
			
			@Override
			public String addMinute(String date,String format,int addMinute) {
				return this.addDetailTime(date,format,0,0,0,0,addMinute,0);			
			}
			
			@Override
			public String addSecond(String date,int addSecond) {
				return this.addDetailTime(date,"yyyyMMddHHmmss",0,0,0,0,0,addSecond);					
			}
			
			@Override
			public String addSecond(String date,String format,int addSecond) {
				return this.addDetailTime(date,format,0,0,0,0,0,addSecond);					
			}

			@Override
			public String addDetailTime(String date,int addYear,int addMonth,int addDay,int addHour,int addMinute,int addSecond) {
				return this.addDetailTime(date,"yyyyMMddHHmmss",addYear,addMonth,addDay,addHour,addMinute,addSecond);
			}

			@Override
			public String addDetailTime(String date,String format,int addYear,int addMonth,int addDay,int addHour,int addMinute,int addSecond) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(this.getDate(date,format)); 
			
				if(addYear!=0) {cal.add(Calendar.YEAR, addYear);}
				if(addMonth!=0) {cal.add(Calendar.MONTH, addMonth);} 
				if(addDay!=0) {cal.add(Calendar.DATE, addDay);}
				if(addHour!=0) {cal.add(Calendar.HOUR, addHour);}
				if(addMinute!=0) {cal.add(Calendar.MINUTE, addMinute);}
				if(addSecond!=0) {cal.add(Calendar.SECOND, addSecond);}
				
				return this.getFormatDate(cal.getTime(),format);
			}

			@Override
		 	public long getDifferenceDays(String date1,String date2) {
				return this.getDifference(date1,"yyyyMMddHHmmssSSS",date2,"yyyyMMddHHmmssSSS")/(24*60*60*1000);
			}
		 	
			@Override
		 	public long getDifferenceHours(String date1,String date2) {
				return this.getDifference(date1,"yyyyMMddHHmmssSSS",date2,"yyyyMMddHHmmssSSS")/(60*60*1000);		 		
		 	}
		 	
			@Override
		 	public long getDifferenceMinutes(String date1,String date2) {
				return this.getDifference(date1,"yyyyMMddHHmmssSSS",date2,"yyyyMMddHHmmssSSS")/(60*1000);		 		
		 	}

			@Override
		 	public long getDifferenceSeconds(String date1,String date2) {
				return this.getDifference(date1,"yyyyMMddHHmmssSSS",date2,"yyyyMMddHHmmssSSS")/(1000);		 		
		 	}

			@Override
		 	public long getDifferenceMillis(String date1,String date2) {
				return this.getDifference(date1,"yyyyMMddHHmmssSSS",date2,"yyyyMMddHHmmssSSS");		 		
			}
			
			@Override
		 	public long getDifference(String date1,String format1,String date2,String format2) {
				return this.getDate(date2,format2).getTime()-this.getDate(date1,format1).getTime();
			}

			@Override
		 	public int getDayOfWeek() {
				Date date = new Date(System.currentTimeMillis());
				return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getDayOfWeek().getValue();
		 	}
						
			@Override
		 	public long getTimeStamp(String date,String format) {
				if(date==null || date.equals("")) {return 0;}
				if(format==null || format.equals("")) {return 0;}
				
				SimpleDateFormat dateFormat = new SimpleDateFormat(format);
				dateFormat.setLenient(false);
				
				Timestamp timstamp = null;
				try {
				    Date stringToDate = dateFormat.parse(date);
				    timstamp = new Timestamp(stringToDate.getTime());
				} catch (ParseException e) {
				    throw new RuntimeException(e.getMessage());
				}
				
				return timstamp.getTime();
			}

		};

	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}