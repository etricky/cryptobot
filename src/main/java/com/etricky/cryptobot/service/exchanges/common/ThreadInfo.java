package com.etricky.cryptobot.service.exchanges.common;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class ThreadInfo {

	@NonNull
	private ExchangeEnum exchangeEnum;
	@NonNull
	private CurrencyEnum currencyEnum;
	@NonNull
	private ExchangeGeneric exchangeGeneric;

	public String getThreadKey() {
		return "T_" + exchangeEnum.getName() + "_" + currencyEnum.getShortName();
	}

	public static String getThreadKey(String exchange, String currency) {
		return "T_" + exchange + "-" + currency;
	}
}
