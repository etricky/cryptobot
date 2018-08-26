package com.etricky.cryptobot.core.strategies.backtest;

import java.math.BigDecimal;

import org.ta4j.core.Order.OrderType;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.strategies.TradingStrategy;
import com.etricky.cryptobot.core.strategies.TrailingStopLossStrategy;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BacktestMetaData {
	// order increased or decreased the balance
	private int balanceResult;
	private int amountResult;

	@Getter
	private BigDecimal firstOrderPrice = BigDecimal.ZERO, firstOrderAmount = BigDecimal.ZERO,
			firstOrderBalance = BigDecimal.ZERO, previousOrderPrice = BigDecimal.ZERO,
			previousOrderAmount = BigDecimal.ZERO, previousOrderBalance = BigDecimal.ZERO, totalFees = BigDecimal.ZERO;

	private int posBalanceOrders = 0, negBalanceOrders = 0, posAmountOrders = 0, negAmountOrders = 0, totalOrders = 0,
			tradingBuys = 0, tradingSells = 0, stopLossBuys = 0, stopLossSells = 0;

	public void calculateMetaData(BacktestOrderInfo orderInfo) {
		String strategyBeanName;
		OrderType orderType;

		if (firstOrderAmount == BigDecimal.ZERO) {
			firstOrderPrice = orderInfo.getStrategyResult().getClosePrice();
			firstOrderAmount = orderInfo.getAmount();
			firstOrderBalance = orderInfo.getBalance();
		}

		balanceResult = orderInfo.getBalance().compareTo(previousOrderBalance);
		amountResult = orderInfo.getAmount().compareTo(previousOrderAmount);
		strategyBeanName = orderInfo.getStrategyResult().getBeanName();
		orderType = orderInfo.getStrategyResult().getLastEntry().getType();

		totalFees = totalFees.add(orderInfo.getFeeValue());

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

		if (strategyBeanName.equals(TradingStrategy.STRATEGY_NAME)) {
			if (orderType.equals(OrderType.BUY)) {
				tradingBuys++;
			} else {
				tradingSells++;
			}
		} else if (strategyBeanName.equals(TrailingStopLossStrategy.STRATEGY_NAME)) {
			if (orderType.equals(OrderType.BUY)) {
				stopLossBuys++;
			} else {
				stopLossSells++;
			}
		}

		previousOrderPrice = orderInfo.getStrategyResult().getClosePrice();
		previousOrderAmount = orderInfo.getAmount();
		previousOrderBalance = orderInfo.getBalance();
		totalOrders++;
	}

	void printMetaData() {
		log.info("totalOrders: {}, posBalance: {}, negBalance: {}, posAmount: {}, negAmount: {} fees: {}", totalOrders,
				posBalanceOrders, negBalanceOrders, posAmountOrders, negAmountOrders,
				totalFees.setScale(NumericFunctions.FEE_SCALE));
		log.info("strategy: {}, buys: {} sells: {}", TradingStrategy.STRATEGY_NAME, tradingBuys, tradingSells);
		log.info("strategy: {}, buys: {} sells: {}", TrailingStopLossStrategy.STRATEGY_NAME, stopLossBuys,
				stopLossSells);
	}
}
