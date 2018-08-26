package com.etricky.cryptobot.core.exchanges.common.enums;

import java.util.Optional;

import org.knowm.xchange.currency.CurrencyPair;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Getter
public enum CurrencyEnum {
	BTC_EUR("BTC_EUR", "BTC", "EUR", CurrencyPair.BTC_EUR), BTC_USD("BTC_USD", "BTC", "USD", CurrencyPair.BTC_USD),
	ETH_EUR("ETH_EUR", "ETH", "EUR", CurrencyPair.ETH_EUR), LTC_EUR("LTC_EUR", "LTC", "EUR", CurrencyPair.XRP_EUR),
	XRP_EUR("XRP_EUR", "XRP", "EUR", CurrencyPair.XRP_EUR);

	String shortName;
	String baseCurrency;
	String quoteCurrency;
	CurrencyPair currencyPair;

	private CurrencyEnum(String shortName, String baseCurrency, String quoteCurrency, CurrencyPair currencyPair) {
		this.shortName = shortName;
		this.baseCurrency = baseCurrency;
		this.quoteCurrency = quoteCurrency;
		this.currencyPair = currencyPair;
	}

	public static Optional<CurrencyEnum> getInstanceByShortName(String shortName) {
		log.debug("start. shortName: {}", shortName);

		if (shortName.equalsIgnoreCase(BTC_EUR.shortName)) {
			return Optional.of(CurrencyEnum.BTC_EUR);
		} else if (shortName.equalsIgnoreCase(BTC_USD.shortName)) {
			return Optional.of(CurrencyEnum.BTC_USD);
		} else if (shortName.equalsIgnoreCase(ETH_EUR.shortName)) {
			return Optional.of(CurrencyEnum.ETH_EUR);
		} else if (shortName.equalsIgnoreCase(LTC_EUR.shortName)) {
			return Optional.of(CurrencyEnum.LTC_EUR);
		} else if (shortName.equalsIgnoreCase(XRP_EUR.shortName)) {
			return Optional.of(CurrencyEnum.XRP_EUR);
		} else {
			log.debug("no currency match");
			return Optional.empty();
		}
	}
}
