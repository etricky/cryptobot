package com.etricky.cryptobot.core.strategies.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.annotation.Nonnull;

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
public class BacktestOrdersInfo {

	@NonNull
	private String strategy;
	@NonNull
	private TradesEntity tradesEntity;
	@NonNull
	private Order order;
	@NonNull
	private BigDecimal highPriceSinceLastOrder;
	@NonNull
	private BigDecimal lowPriceSinceLastOrder;
	@Nonnull
	private BigDecimal closePrice;
	@Nonnull
	private BigDecimal feeValue;
	@Nonnull
	private BigDecimal balance;
	@Nonnull
	private BigDecimal amount;

	@Getter
	@AllArgsConstructor
	public class BacktestData {

		// order increased or decreased the balance
		int balanceResult;
		int amountResult;
		@NonNull
		String strategyDone;
		@NonNull
		OrderType orderType;
	}

	public BacktestData calculateAndprintOrder(long index, BigDecimal firstOrderPrice, BigDecimal firstOrderAmount, BigDecimal firstOrderBalance,
			BigDecimal previousOrderPrice, BigDecimal previousOrderAmount, BigDecimal previousOrderBalance) {

		log.info("index: {} trade :: unixtime: {} timestamp: {} highPrice: {} lowPrice: {}", index, tradesEntity.getTradeId().getUnixtime(),
				tradesEntity.getTimestamp(), highPriceSinceLastOrder, lowPriceSinceLastOrder);
		log.info("strategy: {} order:: type: {} price: {} amount: {} balance: {} fee: {}", strategy, order.getType(), closePrice, amount, balance,
				feeValue);

		log.info("order deltas ----");
		printDeltaValues(previousOrderPrice, previousOrderAmount, previousOrderBalance, true);

		log.info("totals deltas ---- ");
		printDeltaValues(firstOrderPrice, firstOrderAmount, firstOrderBalance, false);
		log.info("--------------------");

		return new BacktestData(balance.compareTo(previousOrderBalance), amount.compareTo(previousOrderAmount), strategy, order.getType());
	}

	private void printDeltaValues(BigDecimal deltaPrice, BigDecimal deltaAmount, BigDecimal deltaBalance, boolean deltaPercentage) {

		log.info("\t\tprice: {}/{}/{} :: {}%", closePrice.subtract(deltaPrice).setScale(NumericFunctions.PRICE_SCALE, RoundingMode.HALF_UP),
				closePrice, deltaPrice.setScale(NumericFunctions.PRICE_SCALE, RoundingMode.HALF_UP),
				NumericFunctions.percentage(closePrice, deltaPrice, deltaPercentage));

		log.info("\t\tamount: {}/{}/{} :: {}%", amount.subtract(deltaAmount).setScale(NumericFunctions.AMOUNT_SCALE, RoundingMode.HALF_UP), amount,
				deltaAmount.setScale(NumericFunctions.AMOUNT_SCALE, RoundingMode.HALF_UP),
				NumericFunctions.percentage(amount, deltaAmount, deltaPercentage));

		log.info("\t\tbalance: {}/{}/{} :: {}%", balance.subtract(deltaBalance).setScale(NumericFunctions.BALANCE_SCALE, RoundingMode.HALF_UP),
				balance, deltaBalance.setScale(NumericFunctions.BALANCE_SCALE, RoundingMode.HALF_UP),
				NumericFunctions.percentage(balance, deltaBalance, deltaPercentage));
	}
}
