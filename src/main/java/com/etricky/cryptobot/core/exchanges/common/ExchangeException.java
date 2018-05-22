package com.etricky.cryptobot.core.exchanges.common;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExchangeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8862528978689743696L;

	public ExchangeException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ExchangeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

	public ExchangeException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public ExchangeException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public ExchangeException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
