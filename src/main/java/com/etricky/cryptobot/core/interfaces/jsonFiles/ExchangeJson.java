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
	Boolean sandbox;
	ArrayList<CurrencyPair> currencyPairs;
	ArrayList<OrderMinimums> orderMinimums;
	ArrayList<TradeConfigs> tradeConfigs;

	@Getter
	@Setter
	public static class CurrencyPair {
		String base_currency;
		String quote_currency;

		public String getShortName() {
			return base_currency + "_" + quote_currency;
		}

	}

	public Map<String, CurrencyPair> getCurrencyPairsMap() {
		return currencyPairs.stream().collect(Collectors.toMap(CurrencyPair::getShortName, Function.identity()));
	}

	@Getter
	@Setter
	public static class OrderMinimums {
		String currency;
		String value;
	}

	public Map<String, OrderMinimums> getOrderMinimumMap() {
		return orderMinimums.stream().collect(Collectors.toMap(OrderMinimums::getCurrency, Function.identity()));
	}

	@Getter
	@Setter
	public static class TradeConfigs {
		String tradeName;
		ArrayList<String> currencyPairs;
		ArrayList<String> strategies;
	}

	public Map<String, TradeConfigs> getTradeConfigsMap() {
		return tradeConfigs.stream().collect(Collectors.toMap(TradeConfigs::getTradeName, Function.identity()));
	}
}
