package com.etricky.cryptobot.core.strategies;

import java.math.BigDecimal;

import org.ta4j.core.Order;
import org.ta4j.core.Order.OrderType;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;
import com.etricky.cryptobot.model.TradeEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Builder(toBuilder = true)
@Slf4j
public class StrategyResult {
	private TradeEntity tradeEntity;
	private String strategyName;
	@Builder.Default
	@Setter
	private int result = AbstractStrategy.NO_ACTION, timeSeriesEndIndex = 0, barDuration = 0;
	@Builder.Default
	private BigDecimal closePrice = BigDecimal.ZERO;
	@Builder.Default
	private BigDecimal lowPrice = BigDecimal.ZERO;
	@Builder.Default
	private BigDecimal highPrice = BigDecimal.ZERO;
	@Builder.Default
	private BigDecimal balance = BigDecimal.ZERO;
	@Builder.Default
	private BigDecimal amount = BigDecimal.ZERO;
	@Builder.Default
	@Setter
	private BigDecimal feePercentage = BigDecimal.ZERO;

	private BigDecimal feeValue;
	private Order lastOrder;

	public void setBalanceAndAmount(BigDecimal previousBalance, BigDecimal previousAmount) {
		BigDecimal deltaBalance;
		BigDecimal deltaAmount;
		feeValue = BigDecimal.ZERO;

		log.trace("start. previousBalance: {} previousAmount:{}", previousBalance, previousAmount);

		if (lastOrder.getType() == OrderType.BUY) {
			// first trade of the backtest
			if (previousBalance.equals(BigDecimal.ZERO)) {
				balance = BigDecimal.valueOf(100);
			} else {
				balance = previousBalance;
			}

			feeValue = NumericFunctions.percentage(feePercentage, balance, false);
			// available balance to be used in the buy
			balance = NumericFunctions.subtract(balance, feeValue, NumericFunctions.BALANCE_SCALE);
			amount = NumericFunctions.divide(balance, closePrice, NumericFunctions.AMOUNT_SCALE);
		}

		if (lastOrder.getType() == OrderType.SELL) {
			amount = previousAmount;
			balance = amount.multiply(closePrice);
			feeValue = NumericFunctions.percentage(feePercentage, balance, false);
			balance = NumericFunctions.subtract(balance, feeValue, NumericFunctions.BALANCE_SCALE);
		}

		deltaBalance = NumericFunctions.subtract(balance, previousBalance, NumericFunctions.BALANCE_SCALE);
		deltaAmount = NumericFunctions.subtract(amount, previousAmount, NumericFunctions.AMOUNT_SCALE);

		log.debug("order: {} balance: {}/{} amount: {}/{} feeValue: {}", lastOrder.getType(), balance, deltaBalance,
				amount, deltaAmount, feeValue);
		log.debug("----------");
	}
}
