package com.etricky.cryptobot.core.interfaces.jsonFiles;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExchangeJson {
	String name;
	Currency[] currencies;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Currency[] getCurrencies() {
		return currencies;
	}

	public Map<String, Currency> getCurrenciesMap() {
		return Arrays.stream(currencies).collect(Collectors.toMap(Currency::getShortName, Function.identity()));
	}

	public void setCurrencies(Currency[] currencies) {
		this.currencies = currencies;
	}

	public static class Currency {
		String base_currency;
		String quote_currency;

		public String getBase_currency() {
			return base_currency;
		}

		public void setBase_currency(String base_currency) {
			this.base_currency = base_currency;
		}

		public String getQuote_currency() {
			return quote_currency;
		}

		public void setQuote_currency(String quote_currency) {
			this.quote_currency = quote_currency;
		}

		public String getShortName() {
			return base_currency + "_" + quote_currency;
		}

	}
}
