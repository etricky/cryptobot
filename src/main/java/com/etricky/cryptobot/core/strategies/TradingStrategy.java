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
@Scope("prototype")
@Slf4j
public class TradingStrategy extends AbstractStrategy {

	@Override
	public void createStrategy() {
		log.debug("start");

		ClosePriceIndicator closePrice = new ClosePriceIndicator(getTimeSeries());
		TripleEMAIndicator tema = new TripleEMAIndicator(closePrice, 10);
		DoubleEMAIndicator dema = new DoubleEMAIndicator(closePrice, 20);

		Rule entryRule = new CrossedDownIndicatorRule(tema, dema);
		Rule exitRule = new CrossedUpIndicatorRule(tema, dema);

		setStrategy(new BaseStrategy(entryRule, exitRule));

		log.debug("done");
	}
}
