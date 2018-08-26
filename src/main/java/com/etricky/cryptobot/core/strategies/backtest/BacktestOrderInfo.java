package com.etricky.cryptobot.core.strategies.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.ta4j.core.Order;
import org.ta4j.core.Order.OrderType;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.strategies.StrategyResult;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@RequiredArgsConstructor
@Slf4j
public class BacktestOrderInfo {

	@NonNull
	private StrategyResult strategyResult;

	private BigDecimal feeValue;
	private BigDecimal balance;
	private BigDecimal amount;

	public void setBalanceAndAmount(BigDecimal previousBalance, BigDecimal feePercentage) {
		Order order = strategyResult.getLastOrder();

		if (order.getType() == OrderType.BUY) {
			// first trade of the backtest
			if (strategyResult.getLastExit() == null) {
				balance = BigDecimal.valueOf(100);
			} else {
				balance = previousBalance;
			}

			feeValue = balance.multiply(feePercentage);
			// available balance to be used in the buy
			balance = balance.subtract(feeValue);
			amount = balance.divide(strategyResult.getClosePrice());

		}

		if (order.getType() == OrderType.SELL) {
			amount = BigDecimal.valueOf(strategyResult.getLastEntry().getAmount().doubleValue());
			balance = amount.multiply(strategyResult.getClosePrice());
			feeValue = balance.multiply(feePercentage);
			balance = balance.subtract(feeValue);
		}

		log.trace("balance: {} amount: {}", balance, amount);
	}

	public void printOrder(BacktestMetaData backtestMetaData) {

		log.info("index: {} trade :: unixtime: {} timestamp: {}", strategyResult.getTimeSeriesIndex(),
				strategyResult.getTradeEntity().getTradeId().getUnixtime(),
				strategyResult.getTradeEntity().getTimestamp());

		log.info("strategy: {} order:: type: {} price: {} amount: {} balance: {} fee: {}", strategyResult.getBeanName(),
				strategyResult.getLastEntry().getType(), strategyResult.getClosePrice(), amount, balance, feeValue);

		log.info("order deltas ----");

		printDeltaValues(backtestMetaData.getPreviousOrderPrice(), backtestMetaData.getPreviousOrderAmount(),
				backtestMetaData.getPreviousOrderBalance(), true);

		log.info("totals deltas ---- ");

		printDeltaValues(backtestMetaData.getFirstOrderPrice(), backtestMetaData.getFirstOrderAmount(),
				backtestMetaData.getFirstOrderBalance(), false);

		log.info("--------------------");

	}

	private void printDeltaValues(BigDecimal deltaPrice, BigDecimal deltaAmount, BigDecimal deltaBalance,
			boolean deltaPercentage) {

		log.info("\t\tprice: {}/{}/{} :: {}%",
				strategyResult.getClosePrice().subtract(deltaPrice).setScale(NumericFunctions.PRICE_SCALE,
						RoundingMode.HALF_UP),
				strategyResult.getClosePrice(), deltaPrice.setScale(NumericFunctions.PRICE_SCALE, RoundingMode.HALF_UP),
				NumericFunctions.percentage(strategyResult.getClosePrice(), deltaPrice, deltaPercentage));

		log.info("\t\tamount: {}/{}/{} :: {}%",
				amount.subtract(deltaAmount).setScale(NumericFunctions.AMOUNT_SCALE, RoundingMode.HALF_UP), amount,
				deltaAmount.setScale(NumericFunctions.AMOUNT_SCALE, RoundingMode.HALF_UP),
				NumericFunctions.percentage(amount, deltaAmount, deltaPercentage));

		log.info("\t\tbalance: {}/{}/{} :: {}%",
				balance.subtract(deltaBalance).setScale(NumericFunctions.BALANCE_SCALE, RoundingMode.HALF_UP), balance,
				deltaBalance.setScale(NumericFunctions.BALANCE_SCALE, RoundingMode.HALF_UP),
				NumericFunctions.percentage(balance, deltaBalance, deltaPercentage));
	}
}
