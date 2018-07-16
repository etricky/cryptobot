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

	public static String STRATEGY_NAME = "tradingStrategy";

	@Override
	public void createStrategy() {

		log.debug("start");

		ClosePriceIndicator closePrice = new ClosePriceIndicator(getTimeSeries());
		TripleEMAIndicator tema = new TripleEMAIndicator(closePrice, getJsonFiles().getExchangesJson().get(getExchangeEnum().getName())
				.getStrategiesMap().get(getBeanName()).getTimeFrameShort().intValue());
		DoubleEMAIndicator dema = new DoubleEMAIndicator(closePrice, getJsonFiles().getExchangesJson().get(getExchangeEnum().getName())
				.getStrategiesMap().get(getBeanName()).getTimeFrameLong().intValue());

		// Rule entryRule = new CrossedUpIndicatorRule(dema, tema);
		// Rule exitRule = new CrossedDownIndicatorRule(dema, tema);
		Rule entryRule = new CrossedUpIndicatorRule(tema, dema);
		Rule exitRule = new CrossedDownIndicatorRule(tema, dema);
		setStrategy(new BaseStrategy(getBeanName(), entryRule, exitRule,
				getStrategiesSettings().getInitialPeriod().intValue() / getStrategiesSettings().getBarDurationSec().intValue()));

		log.debug("done");
	}
}
