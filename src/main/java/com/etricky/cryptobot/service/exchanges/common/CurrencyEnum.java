package com.etricky.cryptobot.service.exchanges.common;

import org.knowm.xchange.currency.CurrencyPair;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum CurrencyEnum {
	BTC_EUR("btc_eur", "btc", "eur", CurrencyPair.BTC_EUR), ETH_EUR("eth_eur", "eth", "eur",
			CurrencyPair.ETH_EUR), LTC_EUR("ltc_eur", "ltc", "eur",
					CurrencyPair.XRP_EUR), XRP_EUR("xrp_eur", "xrp", "eur", CurrencyPair.XRP_EUR);

	String shortName;
	String baseCurrency;
	String quoteCurrency;
	CurrencyPair currencyPair;

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public String getBaseCurrency() {
		return baseCurrency;
	}

	public void setBaseCurrency(String baseCurrency) {
		this.baseCurrency = baseCurrency;
	}

	public String getQuoteCurrency() {
		return quoteCurrency;
	}

	public void setQuoteCurrency(String quoteCurrency) {
		this.quoteCurrency = quoteCurrency;
	}

	public CurrencyPair getCurrencyPair() {
		return currencyPair;
	}

	public void setCurrencyPair(CurrencyPair currencyPair) {
		this.currencyPair = currencyPair;
	}

	private CurrencyEnum(String shortName, String baseCurrency, String quoteCurrency, CurrencyPair currencyPair) {
		this.shortName = shortName;
		this.baseCurrency = baseCurrency;
		this.quoteCurrency = quoteCurrency;
		this.currencyPair = currencyPair;
	}

	public static CurrencyEnum getInstanceByShortName(String shortName) {
		log.debug("start. shortName: {}", shortName);

		if (shortName.equalsIgnoreCase(BTC_EUR.shortName)) {
			return CurrencyEnum.BTC_EUR;
		} else if (shortName.equalsIgnoreCase(ETH_EUR.shortName)) {
			return CurrencyEnum.ETH_EUR;
		} else if (shortName.equalsIgnoreCase(LTC_EUR.shortName)) {
			return CurrencyEnum.LTC_EUR;
		} else if (shortName.equalsIgnoreCase(XRP_EUR.shortName)) {
			return CurrencyEnum.XRP_EUR;
		} else {
			log.debug("no currency match");
			return null;
		}
	}
}
