package com.etricky.cryptobot.service.exchanges.common;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadInfo {

	private ExchangeEnum exchangeEnum;
	private CurrencyEnum currencyEnum;
	private Thread thread;
	private ExchangeGeneric exchangeGeneric;
}
