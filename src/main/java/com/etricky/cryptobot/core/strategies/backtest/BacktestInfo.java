package com.etricky.cryptobot.core.strategies.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.ta4j.core.Order;
import org.ta4j.core.Order.OrderType;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.model.TradesEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@RequiredArgsConstructor
@Slf4j
public class BacktestInfo {

	@NonNull
	private String strategy;
	@NonNull
	private TradesEntity tradesEntity;
	@NonNull
	private Order order;
	@NonNull
	private BigDecimal highPrice;
	@NonNull
	private BigDecimal lowPrice;

	@Getter
	@AllArgsConstructor
	public class BacktestData {

		int orderResult;
		@NonNull
		String strategyDone;
		OrderType orderType;
	}

	public BacktestData calculateAndprintOrder(long index, BigDecimal lastOrderPrice, BigDecimal lastOrderAmount, BigDecimal lastOrderBalance,
			BigDecimal firstOrderPrice, BigDecimal firstOrderAmount, BigDecimal firstOrderBalance) {

		log.info("index: {} trade :: unixtime: {} timestamp: {} highPrice: {} lowPrice: {}", index, tradesEntity.getTradeId().getUnixtime(),
				tradesEntity.getTimestamp(), highPrice, lowPrice);
		log.info("strategy: {} order:: type: {} price: {} amount: {} balance: {}", strategy, order.getType(), order.getPrice(),
				NumericFunctions.convertToBigDecimal(order.getAmount(), 8),
				NumericFunctions.convertToBigDecimal(order.getPrice().multipliedBy(order.getAmount()), 2));

		log.info("order deltas ----");
		printValues(lastOrderPrice, lastOrderAmount, lastOrderBalance, true);

		log.info("totals ---- ");
		printValues(firstOrderPrice, firstOrderAmount, firstOrderBalance, false);
		log.info("--------------------");

		return new BacktestData(order.getAmount().multipliedBy(order.getPrice()).compareTo(lastOrderBalance.doubleValue()), strategy,
				order.getType());
	}

	private void printValues(BigDecimal price, BigDecimal amount, BigDecimal balance, boolean delta) {
		BigDecimal aux;
		aux = NumericFunctions.convertToBigDecimal(order.getPrice(), 2);
		log.info("\t\tprice: {}/{} :: {}%", aux.subtract(price).setScale(2, RoundingMode.HALF_UP), price.setScale(2, RoundingMode.HALF_UP),
				NumericFunctions.percentage(aux, price, delta));

		aux = NumericFunctions.convertToBigDecimal(order.getAmount(), 8);
		log.info("\t\tamount: {}/{} :: {}%", aux.subtract(amount).setScale(8, RoundingMode.HALF_UP), amount.setScale(8, RoundingMode.HALF_UP),
				NumericFunctions.percentage(aux, amount, delta));

		aux = NumericFunctions.convertToBigDecimal(order.getPrice().multipliedBy(order.getAmount()), 4);
		log.info("\t\tbalance: {}/{} :: {}%", aux.subtract(balance).setScale(4, RoundingMode.HALF_UP),
				price.multiply(amount).setScale(4, RoundingMode.HALF_UP), NumericFunctions.percentage(aux, balance, delta));
	}

	public BigDecimal getAmount() {
		return NumericFunctions.convertToBigDecimal(order.getAmount(), 8);
	}

	public BigDecimal getPrice() {
		return NumericFunctions.convertToBigDecimal(order.getPrice(), 2);
	}

	public BigDecimal getBalance() {
		return NumericFunctions.convertToBigDecimal(order.getAmount().multipliedBy(order.getPrice()), 4);
	}

}
