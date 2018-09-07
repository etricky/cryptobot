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
	long tradeHistoryDays;
	BigDecimal fee;
	Boolean sandbox;
	Boolean allowFakeTrades;
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
	public static class TradeStrategies {
		String strategyName;
		ArrayList<String> currencyPairs;
	}

	@Getter
	@Setter
	public static class TradeConfigs {
		String tradeName;
		ArrayList<TradeStrategies> strategies;

		public ArrayList<String> getCurrencyPairs() {
			ArrayList<String> currencyPairs = new ArrayList<>();
			strategies.forEach(strategy -> {
				strategy.currencyPairs.stream().forEach(currency -> {
					if (currencyPairs.isEmpty() || !currencyPairs.contains(currency)) {
						currencyPairs.add(currency);
					}
				});
			});

			return currencyPairs;
		}
	}

	public Map<String, TradeConfigs> getTradeConfigsMap() {
		return tradeConfigs.stream().collect(Collectors.toMap(TradeConfigs::getTradeName, Function.identity()));
	}
}
