package com.alpha.base.support.exception.service;

import java.util.Map;

public interface StoreExceptionMetaService {

	public void doStore(Exception exception, Map<String,Object> metaMap);
}