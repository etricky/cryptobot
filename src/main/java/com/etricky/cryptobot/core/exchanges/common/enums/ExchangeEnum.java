package com.etricky.cryptobot.core.exchanges.common.enums;

import java.util.Optional;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Getter
public enum ExchangeEnum {
	GDAX("GDAX", "gdaxTradingBean", "gdaxOrdersBean", "gdaxAccountBean"),
	BITSTAMP("BITSTAMP", "bitstampTradingBean", "bitstampOrdersBean", "bitstampAccountBean"),
	BINANCE("BINANCE", "binanceTradingBean", "binanceOrdersBean", "binanceAccountBean");

	private String name;
	private String tradingBean;
	private String ordersBean;
	private String accountBean;

	private ExchangeEnum(String name, String tradingBean, String ordersBean, String accountBean) {
		this.name = name;
		this.tradingBean = tradingBean;
		this.ordersBean = ordersBean;
		this.accountBean = accountBean;
	}

	public static Optional<ExchangeEnum> getInstanceByName(String name) {
		log.debug("start. name: {}", name);

		if (name.equalsIgnoreCase(ExchangeEnum.GDAX.name)) {
			return Optional.of(ExchangeEnum.GDAX);
		} else if (name.equalsIgnoreCase(ExchangeEnum.BINANCE.name)) {
			return Optional.of(ExchangeEnum.BINANCE);
		} else if (name.equalsIgnoreCase(ExchangeEnum.BITSTAMP.name)) {
			return Optional.of(ExchangeEnum.BITSTAMP);
		} else {
			log.debug("no exchange match");
			return Optional.empty();
		}

	}

}
