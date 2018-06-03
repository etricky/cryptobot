package com.etricky.cryptobot.core.exchanges.common;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
public enum ExchangeEnum {
	GDAX("gdax", "gdaxExchange", 120), BITSTAMP("bitstamp", "bitstampExchange", 1), BINANCE("binance",
			"binanceExchange", 1);

	private String name;
	private int historyDays;
	private String crytobotBean;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getHistoryDays() {
		return historyDays;
	}

	public void setHistoryDays(int historyDays) {
		this.historyDays = historyDays;
	}

	public String getCrytobotBean() {
		return crytobotBean;
	}

	public void setCrytobotBean(String crytobotClass) {
		this.crytobotBean = crytobotClass;
	}

	private ExchangeEnum(String name, String crytobotBean, int historyDays) {
		this.name = name;
		this.crytobotBean = crytobotBean;
		this.historyDays = historyDays;
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
