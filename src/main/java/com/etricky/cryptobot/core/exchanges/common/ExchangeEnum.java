package com.etricky.cryptobot.core.exchanges.common;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Getter
public enum ExchangeEnum {
	GDAX("GDAX", "gdaxExchange"), BITSTAMP("BITSTAMP", "bitstampExchange"), BINANCE("BINANCE", "binanceExchange");

	private String name;
	private String crytobotBean;

	private ExchangeEnum(String name, String crytobotBean) {
		this.name = name;
		this.crytobotBean = crytobotBean;
	}

	public static ExchangeEnum getInstanceByName(String name) {
		log.debug("start. name: {}", name);

		if (name.equalsIgnoreCase(ExchangeEnum.GDAX.name)) {
			return ExchangeEnum.GDAX;
		} else if (name.equalsIgnoreCase(ExchangeEnum.BINANCE.name)) {
			return ExchangeEnum.BINANCE;
		} else if (name.equalsIgnoreCase(ExchangeEnum.BITSTAMP.name)) {
			return ExchangeEnum.BITSTAMP;
		} else {
			log.debug("no exchange match");
			return null;
		}

	}

}
