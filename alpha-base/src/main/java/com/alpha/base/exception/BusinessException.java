package com.alpha.base.exception;

import org.springframework.http.HttpStatus;

import com.alpha.base.advice.ExceptionAdvice.ExceptionMessageFunction;

public class BusinessException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private final String message;

	private final HttpStatus httpStatus;

	public String getMessage() {
		return message;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public BusinessException(ExceptionMessageFunction messageFunction) {
		this(messageFunction.getMessage());
	}

	public BusinessException(String message) {
		this(message,HttpStatus.CONFLICT);
	}

	public BusinessException(ExceptionMessageFunction messageFunction, HttpStatus httpStatus) {
		this(messageFunction.getMessage(),httpStatus);
	}

	public BusinessException(String message, HttpStatus httpStatus) {
		super(message);
		this.message = message;
		this.httpStatus = httpStatus;
	}

}