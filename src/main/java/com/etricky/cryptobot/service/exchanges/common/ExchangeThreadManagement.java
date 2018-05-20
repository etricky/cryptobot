package com.etricky.cryptobot.service.exchanges.common;

import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ExchangeThreadManagement {

	HashMap<String, ThreadInfo> threads = new HashMap<>();

	public void startThread(String exchange, String currency) {
		log.debug("start. exchange: {} currency: {}", exchange, currency);

		ExchangeEnum exchangeEnum = ExchangeEnum.getInstanceByName(exchange);
		CurrencyEnum currencyEnum = CurrencyEnum.getInstanceByShortName(currency);

		log.debug("done");
	}
}
