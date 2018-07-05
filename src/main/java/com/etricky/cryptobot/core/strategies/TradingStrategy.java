package com.etricky.cryptobot.core.strategies;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.etricky.cryptobot.core.strategies.common.ExchangeStrategy;
import com.etricky.cryptobot.core.strategies.common.StrategyGeneric;
import com.etricky.cryptobot.model.TradesEntity;

import lombok.extern.slf4j.Slf4j;

@Component
@Scope("prototype")
@Slf4j
public class TradingStrategy extends StrategyGeneric {

	public TradingStrategy() {
		log.debug("start");

		tradingRecord = new BaseTradingRecord();

		buildStrategy();

		log.debug("done");
	}

	private void buildStrategy() {
		log.debug("start");

		ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);

		// strategy = new BaseStrategy(entryRule, exitRule);

		log.debug("done");
	}

	@Override
	public int processLiveTrade(TradesEntity tradesEntity) {
		int result = ExchangeStrategy.NO_ACTION;
		log.debug("start");

		if (timeSeries.getLastBar().inPeriod(tradesEntity.getTimestamp())) {
			log.debug("trade in the same period");
		}

		addTradeToTimeSeries(tradesEntity);

		log.debug("done. result: {}", result);
		return result;
	}

	@Override
	public void closeTrade() {
		// no need to implement this method
	}
}
