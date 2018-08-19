package com.etricky.cryptobot.core.interfaces.jsonFiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExchangeJson {
	String name;
	long HistoryDays;
	BigDecimal fee;
	ArrayList<Currency> currencies;

	@Getter
	@Setter
	public static class Currency {
		String base_currency;
		String quote_currency;

		public String getShortName() {
			return base_currency + "_" + quote_currency;
		}

	}

	public Map<String, Currency> getCurrenciesMap() {
		return currencies.stream().collect(Collectors.toMap(Currency::getShortName, Function.identity()));
	}

}
