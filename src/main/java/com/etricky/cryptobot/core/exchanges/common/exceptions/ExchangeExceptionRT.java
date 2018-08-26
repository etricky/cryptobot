package com.etricky.cryptobot.core.exchanges.common.exceptions;

public class ExchangeExceptionRT extends RuntimeException {

	private static final long serialVersionUID = -8862528978689743696L;

	public ExchangeExceptionRT() {
		super();

	}

	public ExchangeExceptionRT(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);

	}

	public ExchangeExceptionRT(String message, Throwable cause) {
		super(message, cause);

	}

	public ExchangeExceptionRT(String message) {
		super(message);

	}

	public ExchangeExceptionRT(Throwable cause) {
		super(cause);

	}

}
