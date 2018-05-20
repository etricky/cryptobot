package com.etricky.cryptobot.service.exchanges.common;

public class ExchangeFactory {
	public static ExchangeGeneric createExchange(String exchangeClassName)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, ExchangeException {

		Class<?> exchangeProviderClass = Class.forName(exchangeClassName);

		if (!ExchangeGeneric.class.isAssignableFrom(exchangeProviderClass)) {
			throw new ExchangeException("Class '" + exchangeClassName + "' does not implement ExchangeGeneric");
		}

		return (ExchangeGeneric) exchangeProviderClass.newInstance();
	}

}
