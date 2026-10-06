package com.alpha.base.support.exception.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface StoreExceptionHandlerService {

	public void store(HttpServletRequest request, HttpServletResponse response, Exception exception);

}