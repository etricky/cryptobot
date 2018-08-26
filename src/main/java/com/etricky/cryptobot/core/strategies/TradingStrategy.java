package com.etricky.cryptobot.core.strategies;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.DoubleEMAIndicator;
import org.ta4j.core.indicators.TripleEMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;

import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;

import lombok.extern.slf4j.Slf4j;

@Component
// must be prototype as some indicator stores values in a cache
@Scope("prototype")
@Slf4j
public class TradingStrategy extends AbstractStrategy {

	public static String STRATEGY_NAME = "tradingStrategy";

	@Override
	public void createStrategy() {

		log.debug("start");

		ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);
		TripleEMAIndicator tema = new TripleEMAIndicator(closePrice, strategiesSettings.getTimeFrameShort().intValue());
		DoubleEMAIndicator dema = new DoubleEMAIndicator(closePrice, strategiesSettings.getTimeFrameLong().intValue());

		Rule entryRule = new CrossedUpIndicatorRule(tema, dema);
		Rule exitRule = new CrossedDownIndicatorRule(tema, dema);
		strategy = new BaseStrategy(beanName, entryRule, exitRule,
				strategiesSettings.getInitialPeriod().intValue() / strategiesSettings.getBarDurationSec().intValue());

		log.debug("done");
	}
}
