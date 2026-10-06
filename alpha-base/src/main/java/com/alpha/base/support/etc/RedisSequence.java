package com.alpha.base.support.etc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.alpha.base.support.AbstractMeta;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RedisSequence extends AbstractMeta {
		
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public enum SequenceType {elecSignKeyNo};
	
	private static final long DEFAULT_SPEED_DELAY_MILLS = 60;
	
	private static long speedDelayMills = DEFAULT_SPEED_DELAY_MILLS;
	
	private static String speedCheckStamp = null;
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static synchronized String getSequence(String sequenceKey,String baseDate,String seqformat) {
		//String baseDate = "2024"; //
    	//String seqformat = "7.3"; //7:baseDate부터 분을 표기(baseDate부터 19년),3:increment(0~999) => 1분에 1000개
		
		//[STEP1] sequence대역 설정
    	String temp[] = seqformat.split("\\.");
    	long startNumber = (long)Math.pow(10, Integer.parseInt(temp[0]));
    	long incrementMax = (long)Math.pow(10, Integer.parseInt(temp[1]));
    	
		//[STEP2] baseDate로 부터 baseOffset, speedOffset
		String currentTime = MetaUtil.getTimeUtil().getCurrentTime("yyyyMMddHHmmssSSS");
		long baseOffset=MetaUtil.getTimeUtil().getDifferenceMinutes(baseDate,currentTime);
		long speedOffset=MetaUtil.getTimeUtil().getDifferenceSeconds(speedCheckStamp==null?currentTime:speedCheckStamp, currentTime);		
		if(speedOffset>1) {speedDelayMills=0;}
		//log.debug(">> baseOffset:{},speedOffset:{}",baseOffset,speedOffset);	
    	
		//[STEP3] redis를 이용한 increment정보(sequence정보) 생성
    	threadSleepRandom(speedDelayMills,0); //초기지연 반드시 필요.(일괄 대략 발행시 지연 필요 함.)    	
    	List<Object> incrementInfo = getIncrementInfo(sequenceKey);		
		//log.debug(">> incrementInfo:{}",incrementInfo);	
		
		//[STEP4] increment정보(sequence정보)로 필요정보 추출
		long incrementSeq = Integer.valueOf(String.valueOf(incrementInfo.get(0)));
		String timeStamp = String.valueOf(incrementInfo.get(1));
		long timeStampOffset = MetaUtil.getTimeUtil().getDifferenceMinutes(baseDate, timeStamp);		
		//log.debug(">> incrementSeq:{},timeStamp:{},timeStampOffset:{}",incrementSeq,timeStamp,timeStampOffset);	
						
		//[STEP5] 속도지연값(진입 delay조정)
		long counter = (incrementSeq-Long.parseLong(timeStamp.substring(14))/60);
		long timeGap = MetaUtil.getTimeUtil().getDifferenceMillis(timeStamp, currentTime);
		long overRun = counter-(timeGap/60);
		if(overRun>0) {speedDelayMills = overRun*60;}
		log.debug(">> counter:{},timeGap:{},speedDelayMills:{}",counter,timeGap,speedDelayMills);
		
		//[STEP6] 필요시(max도달,분변경시) 초기화 처리
		boolean isEnable=false,sleep=false;
		if(!isEnable && (incrementSeq+1)==incrementMax) {isEnable=true;sleep=true;} //max
		if(!isEnable && baseOffset>timeStampOffset) {isEnable=true;} //분변경
		if(isEnable) {
			if(sleep) {
				long mills = 60*1000-timeGap;
				log.debug(">> clear sleep-mills:{}, timeGap:{}",mills,timeGap);
				if(timeGap>0) {threadSleepRandom(mills,0);}
			}
			clearIncrementInfo(sequenceKey);
			incrementSeq = 0L;
		}
		
		//[STEP7] 최종 sequence
		speedCheckStamp = MetaUtil.getTimeUtil().getCurrentTime("yyyyMMddHHmmssSSS");
		String sequence = String.valueOf((startNumber+baseOffset)*incrementMax+incrementSeq);
				
		return sequence;
    }			
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
    private static List<Object> getIncrementInfo(String sequenceKey) { 	
    	return getIncrementInfo(sequenceKey,null);
    }
    
    private static List<Object> getIncrementInfo(String sequenceKey,List<Object> incrementInfo) {
		String stampKey = sequenceKey+"-STAMP";
		
		//[increment] redis-increment
		if(incrementInfo==null || incrementInfo.isEmpty() || incrementInfo.get(0)==null) {
			List<Object> result = null;
			if(MetaUtil.getRedisUtil().isCluster()) {
				//TransactionStatus txStatus = BaseUtil.getTransactionUtil().start();
				StringRedisTemplate stringRedisTemplate = MetaUtil.getRedisUtil().getStringRedisTemplate();
				result = new ArrayList<>();
				result.add(stringRedisTemplate.opsForValue().increment(sequenceKey));
				result.add(stringRedisTemplate.opsForValue().get(stampKey));
				//BaseUtil.getTransactionUtil().commit(txStatus);
			}else {
				result = MetaUtil.getRedisUtil().executeMulti(operations->{			
					operations.watch(Arrays.asList(sequenceKey,stampKey));
					operations.multi();
					operations.opsForValue().increment(sequenceKey); //index:0
					operations.opsForValue().get(stampKey); // index:1
					return operations.exec();
				});
			}
			//log.debug(">> increment result:{}",result);
			
			boolean isEnable = true;
			if(isEnable && result==null) {isEnable = false;}
			if(isEnable && result.isEmpty()) {isEnable = false;}
			if(isEnable && result.get(0)==null) {isEnable = false;}
			if(isEnable) {
				incrementInfo = result;
			} else {
				threadSleepRandom(30,10);
				incrementInfo = getIncrementInfo(sequenceKey,result);
			}
		}
		
		//[init] redis 기동 후 첫 요청일 경우 초기화
		if(incrementInfo!=null && incrementInfo.get(1)==null) {	
			String stampValue = MetaUtil.getTimeUtil().getCurrentTime("yyyyMMddHHmmssSSS");
			long sequenceValue = Long.parseLong(stampValue.substring(14))/60;
			log.debug(">> init-stampValue:{},init-sequenceValue:{}",stampValue,sequenceValue);
			
			List<Object> result = null;
			if(MetaUtil.getRedisUtil().isCluster()) {
				//TransactionStatus txStatus = BaseUtil.getTransactionUtil().start();
				StringRedisTemplate stringRedisTemplate = MetaUtil.getRedisUtil().getStringRedisTemplate();
				stringRedisTemplate.opsForValue().set(sequenceKey, String.valueOf(sequenceValue));
				stringRedisTemplate.opsForValue().set(stampKey, stampValue);

				result = new ArrayList<>();
				result.add(sequenceValue);
				result.add(stampValue);
				//BaseUtil.getTransactionUtil().commit(txStatus);				
			}else {
				result = MetaUtil.getRedisUtil().executeMulti(operations->{
					operations.watch(Arrays.asList(sequenceKey,stampKey));
					operations.multi();
					operations.opsForValue().set(sequenceKey,sequenceValue); //index:0
					operations.opsForValue().set(stampKey,stampValue); //index:1
					return operations.exec();
				});
			}
			//log.debug(">> stamp result:{}",result);
			
			boolean isEnable = true;
			if(isEnable && result==null) {isEnable = false;}
			if(isEnable && result.isEmpty()) {isEnable = false;}
			if(isEnable && result.get(0)==null) {isEnable = false;}			
			if(isEnable) {
				incrementInfo.set(0, sequenceValue);
				incrementInfo.set(1, stampValue);
			} else {				
				threadSleepRandom(30,10);
				incrementInfo = getIncrementInfo(sequenceKey,incrementInfo);
			}		
		}

		return incrementInfo;
    }

    private static List<Object> clearIncrementInfo(String sequenceKey) {
		String stampKey = sequenceKey+"-STAMP";		
		String stampValue = MetaUtil.getTimeUtil().getCurrentTime("yyyyMMddHHmmssSSS");
		
		List<Object> result = null;
		if(MetaUtil.getRedisUtil().isCluster()) {	
			//TransactionStatus txStatus = BaseUtil.getTransactionUtil().start();
			StringRedisTemplate stringRedisTemplate = MetaUtil.getRedisUtil().getStringRedisTemplate();
			stringRedisTemplate.opsForValue().set(stampKey, stampValue);
			stringRedisTemplate.opsForValue().set(sequenceKey, "0");
			
			result = new ArrayList<>();
			result.add(stampValue);
			result.add(0L);
			//BaseUtil.getTransactionUtil().commit(txStatus);	
		} else {
			result = MetaUtil.getRedisUtil().executeMulti(operations->{
				operations.watch(Arrays.asList(stampKey));
				operations.multi();
				operations.opsForValue().set(stampKey,stampValue); //index:0
				operations.opsForValue().set(sequenceKey, 0L); //index:1
				return operations.exec();
			});
		}
		//log.debug(">> clear result:{}",result);

		boolean isEnable = true;
		if(isEnable && result==null) {isEnable = false;}
		if(isEnable && result.isEmpty()) {isEnable = false;}
		if(isEnable && result.get(0)==null) {isEnable = false;}			
		if(!isEnable) {
			threadSleepRandom(30,0);
			return clearIncrementInfo(sequenceKey);
		}
		
		return result;
    }
    
    private static void threadSleepRandom(long base,long randomScale) {
    	if(base==0) {return;}
		try {
			long millis = base+(long)(Math.random()*randomScale);
			//log.debug(">> sleep4sequence:{}",millis);
			Thread.sleep(millis);						
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }	

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}