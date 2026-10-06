package com.alpha.xtra.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.alpha.base.BaseUtil;
import com.alpha.base.exception.BusinessException;
import com.alpha.base.support.util.RedisUtilPack.RedisHashRepository;
import com.alpha.base.support.util.RedisUtilPack.RedisListRepository;
import com.alpha.base.support.util.RedisUtilPack.RedisObjectRepository;
import com.alpha.base.support.util.RedisUtilPack.RedisSetRepository;
import com.alpha.xtra.service.RedisStoreService;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name="spring.redis.enabled", havingValue="true", matchIfMissing=false)
public class RedisStoreServiceImpl implements RedisStoreService {

	///////////////////////////////////////////////	///////////////////////////////////////////////////////////////////////////////
	
	@Override
	public void set(String message, long expireTime, String exceptionYn) {

		//vo
		DataVo vo = new DataVo();
		vo.setMessage(message);
		
		//map
		Map<String,DataVo> map = new HashMap<>();
		map.put("mapKey1", vo);

		//list
		List<DataVo> list = new ArrayList<>();
		list.add(vo);
		
		//objectRepo
		RedisObjectRepository objectRepo = BaseUtil.getRedisUtil().getRedisObjectRepository();
		objectRepo.put("key1", message, Duration.ofSeconds(60));
		objectRepo.put("key2", vo, Duration.ofSeconds(60));
		objectRepo.put("key3", map, Duration.ofSeconds(60));
		objectRepo.put("key4", list, Duration.ofSeconds(60));

		//hashRepo		
		RedisHashRepository hashRepo = BaseUtil.getRedisUtil().getRedisHashRepository();
		hashRepo.put("key","data1", message);
		hashRepo.put("key","data2", vo);
		hashRepo.put("key","data3", map);
		hashRepo.put("key","data4", list);
		hashRepo.setExpireTime("key", 60);
		
		//트렌젝션 rollback 테스틑용
		log.debug(">> exceptionYn:{}",exceptionYn);
		if(exceptionYn.equals("Y")) {			
			log.debug(">> BusinessException발생");			
			throw new BusinessException("오류");
		}
		
		//listRepo
		RedisListRepository listRepo = BaseUtil.getRedisUtil().getRedisListRepository();
		listRepo.rightPush("key", message+" 1");
		listRepo.rightPush("key", message+" 2");
		listRepo.rightPush("key", message+" 3");
		listRepo.rightPush("key", message+" 4");
		listRepo.setExpireTime("key", 60);
				
		//setRepo	
		RedisSetRepository setRepo = BaseUtil.getRedisUtil().getRedisSetRepository();
		setRepo.add("key",message,vo,map,list);
		setRepo.setExpireTime("key", 60);
	}

	@Override
	public Map<String, Object> get() {

		//objectRepoData
		RedisObjectRepository objectRepo = BaseUtil.getRedisUtil().getRedisObjectRepository();
		Map<String,Object> objectRepoData = new HashMap<>();
		objectRepoData.put("key1", objectRepo.get("key1"));
		objectRepoData.put("key2", objectRepo.get("key2",DataVo.class));
		objectRepoData.put("key3", objectRepo.get("key3",new TypeReference<Map<String,Object>>() {}));
		objectRepoData.put("key4", objectRepo.get("key4",new TypeReference<List<DataVo>>() {}));		
		log.debug(">> objectRepoData:{}",objectRepoData);

		//hashRepoData
		Map<String,Object> hashRepoData = BaseUtil.getRedisUtil().getRedisHashRepository().get("key");
		log.debug(">> hashRepoData:{}",hashRepoData);
		
		//listRepoData
		List<Object> listRepoData = BaseUtil.getRedisUtil().getRedisListRepository().get("key");
		log.debug(">> listRepoData:{}",listRepoData);

		//setRepoData
		Set<Object> setRepoData = BaseUtil.getRedisUtil().getRedisSetRepository().get("key");
		log.debug(">> setRepoData:{}",setRepoData);
		
		Map<String,Object> data = new HashMap<>();		
		data.put("objectRepoData", objectRepoData);
		data.put("hashRepoData", hashRepoData);
		data.put("listRepoData", listRepoData);
		data.put("setRepoData", setRepoData);
		
        return data;
	}

	@Override
    public Map<String,Object> increment(){
		
		String keyA = "keyA";
		RedisObjectRepository objectRepo = BaseUtil.getRedisUtil().getRedisObjectRepository();
		log.debug(">> objectRepo:{}",objectRepo.get(keyA));
		
		String keyB = "keyB";
		RedisHashRepository hashRepo = BaseUtil.getRedisUtil().getRedisHashRepository();
		log.debug(">> hashRepo:{}", hashRepo.get(keyB));
		
		String keyC = "keyC";
		RedisListRepository listRepo = BaseUtil.getRedisUtil().getRedisListRepository();
		log.debug(">> listRepo:{}", listRepo.get(keyC));

		String keyD = "keyD";
		RedisSetRepository setRepo = BaseUtil.getRedisUtil().getRedisSetRepository();
		log.debug(">> setRepo:{}", setRepo.get(keyD));

		int increment = objectRepo.get(keyA)==null?0:(int)objectRepo.get(keyA)+1;
		
		Integer results = BaseUtil.getRedisUtil().executeMulti(operations->{

			operations.watch(Arrays.asList(keyA,"name","phoneNo"));
			operations.multi();
			
			objectRepo.put(keyA,increment);
	        log.debug(">> executeMulti-key1:{}",objectRepo.get(keyA));
	        			
			objectRepo.put("name", "홍길동");			
	        log.debug(">> executeMulti-name:{}",objectRepo.get("name"));

			objectRepo.put("phoneNo", "010-9999-1234");			
	        log.debug(">> executeMulti-phoneNo:{}",objectRepo.get("phoneNo"));

	        List<Object> result = operations.exec();
	        log.debug(">> executeMulti-result:{}",result);
	        
	        return (Integer)objectRepo.get(keyA);
		});
		
		BaseUtil.getRedisUtil().executePipe((connection,keySerializer,valueSerializer)-> {
			connection.set(keySerializer.serialize("test1"),valueSerializer.serialize("A"));
			connection.set(keySerializer.serialize("test2"),valueSerializer.serialize("B"));
			connection.set(keySerializer.serialize("test3"),valueSerializer.serialize("C"));
			connection.set(keySerializer.serialize("test4"),valueSerializer.serialize("D"));
			connection.set(keySerializer.serialize("test5"),valueSerializer.serialize("E"));
			return null;
		});
		
		
        log.debug(">> name:{}",objectRepo.get("name"));
        log.debug(">> phoneNo:{}",objectRepo.get("phoneNo"));
		log.debug(">> key1:{}",results);
		
		objectRepo.put(keyA,results);
		hashRepo.put(keyB, "number", results);		
		listRepo.leftPush(keyC, results);
		setRepo.add(keyD, results);
		
		Map<String,Object> data = new HashMap<>();		
		data.put("increment1", objectRepo.get(keyA));
		data.put("increment2", hashRepo.get(keyB));
		data.put("increment3", listRepo.get(keyC));
		data.put("increment4", setRepo.get(keyD));

		return data;	
	}
	
}