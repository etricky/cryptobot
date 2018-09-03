package com.etricky.cryptobot.core.strategies.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.strategies.StrategyResult;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class BacktestOrderInfo {

	@NonNull
	@Getter
	private StrategyResult strategyResult;

	private BigDecimal feeValue, balance, amount;

	public void printOrder(BacktestMetaData backtestMetaData) {
		feeValue = strategyResult.getFeeValue();
		balance = strategyResult.getBalance();
		amount = strategyResult.getAmount();

		log.info("index: {} trade :: unixtime: {} timestamp: {}", strategyResult.getTimeSeriesEndIndex(),
				strategyResult.getTradeEntity().getTradeId().getUnixtime(),
				strategyResult.getTradeEntity().getTimestamp());

		log.info("strategy: {} order:: type: {} price: {} amount: {} balance: {} fee: {}",
				strategyResult.getStrategyName(), strategyResult.getLastEntry().getType(),
				strategyResult.getClosePrice(), amount, balance, feeValue);

		log.info("order deltas ----");

		printDeltaValues(backtestMetaData.getPreviousOrderPrice(), backtestMetaData.getPreviousOrderAmount(),
				backtestMetaData.getPreviousOrderBalance(), true);

		log.info("totals deltas ---- ");

		printDeltaValues(backtestMetaData.getFirstOrderPrice(), backtestMetaData.getFirstOrderAmount(),
				backtestMetaData.getFirstOrderBalance(), false);

		log.info("--------------------");

		backtestMetaData.setPreviousOrderPrice(strategyResult.getClosePrice());
		backtestMetaData.setPreviousOrderAmount(strategyResult.getAmount());
		backtestMetaData.setPreviousOrderBalance(strategyResult.getBalance());

	}

	private void printDeltaValues(BigDecimal deltaPrice, BigDecimal deltaAmount, BigDecimal deltaBalance,
			boolean deltaPercentage) {

		log.info("\t\tprice: {}/{}/{} :: {}%",
				NumericFunctions.subtract(strategyResult.getClosePrice(), deltaPrice, NumericFunctions.PRICE_SCALE),
				strategyResult.getClosePrice(), deltaPrice.setScale(NumericFunctions.PRICE_SCALE, RoundingMode.HALF_UP),
				NumericFunctions.percentage(strategyResult.getClosePrice(), deltaPrice, deltaPercentage));

		log.info("\t\tamount: {}/{}/{} :: {}%",
				NumericFunctions.subtract(amount, deltaAmount, NumericFunctions.AMOUNT_SCALE), amount,
				deltaAmount.setScale(NumericFunctions.AMOUNT_SCALE, RoundingMode.HALF_UP),
				NumericFunctions.percentage(amount, deltaAmount, deltaPercentage));

		log.info("\t\tbalance: {}/{}/{} :: {}%",
				NumericFunctions.subtract(balance, deltaBalance, NumericFunctions.BALANCE_SCALE), balance,
				deltaBalance.setScale(NumericFunctions.BALANCE_SCALE, RoundingMode.HALF_UP),
				NumericFunctions.percentage(balance, deltaBalance, deltaPercentage));
	}
}
