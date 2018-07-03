package com.etricky.cryptobot.core.strategies.common;

import org.springframework.stereotype.Component;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.etricky.cryptobot.model.TradesEntity;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TradingStrategy extends StrategyGeneric {

	TradingRecord tradingRecord;

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
	public void processLiveTrade(TradesEntity tradesEntity) {
		log.debug("start");
		if (timeSeries.getLastBar().inPeriod(tradesEntity.getTimestamp())) {
			log.debug("trade in the same period");
		}

		addTradeToTimeSeries(tradesEntity);

		log.debug("done");
	}
}
