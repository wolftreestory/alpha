package com.alpha.base.exception;

import org.springframework.http.HttpStatus;

import com.alpha.base.advice.ExceptionAdvice.ExceptionMessageFunction;

public class SystemException extends RuntimeException{

	private static final long serialVersionUID = 1L;
	
	private final String message;

	private final HttpStatus httpStatus;

	public String getMessage() {
		return message;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public SystemException(ExceptionMessageFunction messageFunction) {
		this(messageFunction.getMessage());
	}
	
	public SystemException(String message) {
		this(message,HttpStatus.INTERNAL_SERVER_ERROR);
	}

	public SystemException(ExceptionMessageFunction messageFunction, HttpStatus httpStatus) {
		this(messageFunction.getMessage(),httpStatus);
	}
	
	public SystemException(String message, HttpStatus httpStatus) {
		super(message);
		this.message = message;
		this.httpStatus = httpStatus;
	}

}