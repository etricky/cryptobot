package com.etricky.cryptobot.core.strategies;

import java.math.BigDecimal;

import org.ta4j.core.Order;

import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;
import com.etricky.cryptobot.model.TradeEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StrategyResult {
	private TradeEntity tradeEntity;
	private String beanName;
//	protected TimeSeries timeSeries;
//	protected TradingRecord tradingRecord;
	@Builder.Default
	private int result = AbstractStrategy.NO_ACTION, timeSeriesIndex = 0, barDuration = 0;
	@Builder.Default
	private BigDecimal closePrice = BigDecimal.ZERO;
	@Builder.Default
	private BigDecimal lowPrice = BigDecimal.ZERO;
	@Builder.Default
	private BigDecimal highPrice = BigDecimal.ZERO;
	@Builder.Default
	private BigDecimal feePercentage = BigDecimal.ZERO;
	private Order lastOrder, lastEntry, lastExit;

}
