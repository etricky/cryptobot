package com.etricky.cryptobot.core.exchanges.common;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
public enum ExchangeEnum {
	GDAX("gdax", "org.knowm.xchange.gdax.GDAXExchange", "gdaxExchange"), BITSTAMP(
			"bitstamp", "org.knowm.xchange.bitstamp.BitstampExchange",
			"bitstampExchange"), BINANCE("binance",
					"org.knowm.xchange.binance.BitstampExchange", "binanceExchange");

	private String name;
	private String xchangeClass;
	private String crytobotBean;

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

	public String getCrytobotBean() {
		return crytobotBean;
	}

	public void setCrytobotBean(String crytobotClass) {
		this.crytobotBean = crytobotClass;
	}

	private ExchangeEnum(String name, String xchangeClass, String crytobotBean) {
		this.name = name;
		this.xchangeClass = xchangeClass;
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
