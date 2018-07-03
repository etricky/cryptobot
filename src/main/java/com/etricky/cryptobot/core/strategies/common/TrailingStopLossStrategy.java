package com.etricky.cryptobot.core.strategies.common;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.etricky.cryptobot.core.strategies.rules.TraillingStopLossEntryRule;
import com.etricky.cryptobot.core.strategies.rules.TraillingStopLossExitRule;
import com.etricky.cryptobot.model.TradesEntity;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TrailingStopLossStrategy extends StrategyGeneric {

	TradingRecord tradingRecord;
	Strategy strategy;

	public TrailingStopLossStrategy() {
		log.debug("start");

		tradingRecord = new BaseTradingRecord();

		buildStrategy();

		log.debug("done");
	}

	private void buildStrategy() {
		log.debug("start");

		ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);

		// TODO feeValue must be obtained by fee*amount to be traded
		Rule entryRule = new TraillingStopLossEntryRule(closePrice, Decimal.valueOf(strategiesMap.get(beanName).getGainPercentage()),
				Decimal.valueOf(strategiesMap.get(beanName).getFee().multiply(BigDecimal.TEN)));

		// TODO feeValue must be obtained by fee*amount to be traded
		Rule exitRule = new TraillingStopLossExitRule(closePrice, Decimal.valueOf(strategiesMap.get(beanName).getLossPerc()),
				Decimal.valueOf(strategiesMap.get(beanName).getGainPercentage()),
				Decimal.valueOf(strategiesMap.get(beanName).getFee().multiply(BigDecimal.TEN)));

		strategy = new BaseStrategy(entryRule, exitRule);

		log.debug("done");
	}

	@Override
	public void processLiveTrade(TradesEntity tradesEntity) {

		String logAux = null;
		log.debug("start");

		addTradeToTimeSeries(tradesEntity);

		int endIndex = timeSeries.getEndIndex();
		Bar lastBar = timeSeries.getLastBar();
		if (strategy.shouldOperate(endIndex, tradingRecord) && strategy.shouldEnter(endIndex)) {
			// strategy should enter
			log.debug("strategy should ENTER on " + endIndex);

			if (tradingRecord.enter(endIndex, lastBar.getClosePrice(), Decimal.ONE)) {
				logAux = "Entered";
				// TODO amount should correspond to the amount of currency that was traded
			}
		} else if (strategy.shouldOperate(endIndex, tradingRecord) && strategy.shouldExit(endIndex)) {
			// strategy should exit
			log.debug("strategy should EXIT on " + endIndex);

			if (tradingRecord.exit(endIndex, lastBar.getClosePrice(), Decimal.ONE)) {
				logAux = "Exited";
				// TODO amount should correspond to the amount of currency that was traded
			}
		}

		log.debug("{} on index: {} price={}", logAux, endIndex, tradesEntity.getClosePrice());

		log.debug("done");
	}

}
