package com.etricky.cryptobot.service.exchanges.common;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
public enum ExchangeEnum {
	GDAX("gdax", "org.knowm.xchange.gdax.GDAXExchange", "com.etricky.cryptobot.service.exchanges.Gdax"), BITSTAMP(
			"bitstamp", "org.knowm.xchange.bitstamp.BitstampExchange",
			"com.etricky.cryptobot.service.exchanges.Bitstamp"), BINANCE("binance",
					"org.knowm.xchange.binance.BitstampExchange", "com.etricky.cryptobot.service.exchanges.Binance");

	private String name;
	private String xchangeClass;
	private String crytobotClass;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getXchangeClass() {
		return xchangeClass;
	}

	public void setXchangeClass(String xchangeClass) {
		this.xchangeClass = xchangeClass;
	}

	public String getCrytobotClass() {
		return crytobotClass;
	}

	public void setCrytobotClass(String crytobotClass) {
		this.crytobotClass = crytobotClass;
	}

	private ExchangeEnum(String name, String xchangeClass, String crytobotClass) {
		this.name = name;
		this.xchangeClass = xchangeClass;
		this.crytobotClass = crytobotClass;
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
