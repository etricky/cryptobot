package com.etricky.cryptobot.service.interfaces.jsonFiles;

public class ExchangeJson {
	String name;
	String historyDays;
	Currency[] currencies;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHistoryDays() {
		return historyDays;
	}

	public void setHistoryDays(String historyDays) {
		this.historyDays = historyDays;
	}

	public Currency[] getCurrencies() {
		return currencies;
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

	}
}
