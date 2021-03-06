package com.etricky.cryptobot.core.strategies.backtest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.ta4j.core.Order.OrderType;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.strategies.TradingStrategy;
import com.etricky.cryptobot.core.strategies.TrailingStopLossStrategy;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BacktestMetaData {
	// order increased or decreased the balance
	private int balanceResult, amountResult;
	private final int BUY = 0, SELL = 1;
	private Map<String, Integer[]> currencyOrdersMap = new HashMap<>();

	@Getter
	private BigDecimal firstOrderPrice = BigDecimal.ZERO, firstOrderAmount = BigDecimal.ZERO,
			firstOrderBalance = BigDecimal.ZERO, totalFees = BigDecimal.ZERO;

	@Getter
	@Setter
	private BigDecimal previousOrderPrice = BigDecimal.ZERO, previousOrderAmount = BigDecimal.ZERO,
			previousOrderBalance = BigDecimal.ZERO;
	@Getter
	private int posBalanceOrders = 0, negBalanceOrders = 0, posAmountOrders = 0, negAmountOrders = 0, totalOrders = 0,
			tradingBuys = 0, tradingSells = 0, stopLossBuys = 0, stopLossSells = 0;

	private String currencyOrders = "orders ::";

	public void calculateMetaData(BacktestOrderInfo orderInfo) {
		String strategyBeanName,
				currencyName = orderInfo.getStrategyResult().getTradeEntity().getTradeId().getCurrency();
		OrderType orderType;

		orderInfo.getStrategyResult().setBalanceAndAmount(previousOrderBalance, previousOrderAmount);

		if (firstOrderAmount == BigDecimal.ZERO) {
			firstOrderPrice = orderInfo.getStrategyResult().getClosePrice();
			firstOrderAmount = orderInfo.getStrategyResult().getAmount();
			firstOrderBalance = orderInfo.getStrategyResult().getBalance();
		}

		if (!currencyOrdersMap.containsKey(currencyName)) {
			Integer[] ordersArray = { 0, 0 };
			currencyOrdersMap.put(currencyName, ordersArray);
		}

		balanceResult = orderInfo.getStrategyResult().getBalance().compareTo(previousOrderBalance);
		amountResult = orderInfo.getStrategyResult().getAmount().compareTo(previousOrderAmount);
		strategyBeanName = orderInfo.getStrategyResult().getStrategyName();
		orderType = orderInfo.getStrategyResult().getLastOrder().getType();

		totalFees = totalFees.add(orderInfo.getStrategyResult().getFeeValue());

		if (balanceResult < 0) {
			negBalanceOrders++;
		} else {
			posBalanceOrders++;
		}

		if (amountResult < 0) {
			negAmountOrders++;
		} else {
			posAmountOrders++;
		}

		if (orderType.equals(OrderType.BUY)) {
			if (strategyBeanName.equals(TradingStrategy.STRATEGY_NAME)) {
				tradingBuys++;
			} else if (strategyBeanName.equals(TrailingStopLossStrategy.STRATEGY_NAME)) {
				stopLossBuys++;
			}

			currencyOrdersMap.get(currencyName)[BUY]++;
		} else if (orderType.equals(OrderType.SELL)) {
			if (strategyBeanName.equals(TradingStrategy.STRATEGY_NAME)) {
				tradingSells++;
			} else if (strategyBeanName.equals(TrailingStopLossStrategy.STRATEGY_NAME)) {
				stopLossSells++;
			}

			currencyOrdersMap.get(currencyName)[SELL]++;
		}

		totalOrders++;
	}

	public String getCurrencyOrders() {
		currencyOrdersMap.forEach((currName, array) -> {
			currencyOrders = currencyOrders + " currency " + currName + " buys: " + array[BUY] + " sells: "
					+ array[SELL];
		});
		return currencyOrders;
	}

	public void printMetaData() {
		log.info("totalOrders: {}, posBalance: {}, negBalance: {}, posAmount: {}, negAmount: {} fees: {}", totalOrders,
				posBalanceOrders, negBalanceOrders, posAmountOrders, negAmountOrders,
				totalFees.setScale(NumericFunctions.FEE_SCALE));
		log.info("strategy: {}, buys: {} sells: {}", TradingStrategy.STRATEGY_NAME, tradingBuys, tradingSells);
		log.info("strategy: {}, buys: {} sells: {}", TrailingStopLossStrategy.STRATEGY_NAME, stopLossBuys,
				stopLossSells);
		log.info(currencyOrders);
	}
}
