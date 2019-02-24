package com.etricky.cryptobot.core.exchanges.common.enums;

import org.knowm.xchange.currency.Currency;

import lombok.Getter;

@Getter
public enum FiatCurrencyEnum {
	EUR("EUR", Currency.EUR), USD("USD", Currency.USD), GBP("GBP", Currency.GBP),
	USDC("USDC", Currency.getInstance("USDC"));

	String fiatCurrency;
	Currency currency;

	private FiatCurrencyEnum(String fiatCurrency, Currency currency) {
		this.fiatCurrency = fiatCurrency;
		this.currency = currency;
	}

	public static boolean isFiatCurrency(Currency currency) {
		if (currency.equals(EUR.getCurrency())) {
			return true;
		} else if (currency.equals(USD.getCurrency())) {
			return true;
		} else if (currency.equals(GBP.getCurrency())) {
			return true;
		} else if (currency.equals(USDC.getCurrency())) {
			return true;
		} else {
			return false;
		}
	}
}
