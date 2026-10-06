package com.alpha.base.support.batch;
import com.alpha.base.config.batch.BatchProperties;
import com.alpha.base.support.aid.BeanAidPack.BeanAid;
import com.alpha.base.support.util.Base64UtilPack.Base64Util;
import com.alpha.base.support.util.JsonUtilPack.JsonUtil;
import com.alpha.base.support.util.LobConverterUtilPack.LobConverterUtil;
import com.alpha.base.support.util.ObjectMapperUtilPack.ObjectMapperUtil;
import com.alpha.base.support.util.SystemUtilPack.SystemUtil;
import com.alpha.base.support.util.TimeUtilPack.TimeUtil;
import com.alpha.base.support.util.TransactionUtilPack.TransactionUtil;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class BatchUtil {
	private BatchProperties batchProperties;
	private BeanAid beanAid;
	private Base64Util base64Util;
	private JsonUtil jsonUtil;
	private ObjectMapperUtil objectMapperUtil;
	private LobConverterUtil lobConverterUtil;
	private SystemUtil systemUtil;
	private TimeUtil timeUtil;
	private TransactionUtil transactionUtil;
}
