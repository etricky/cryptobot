package com.etricky.cryptobot.core.strategies;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.indicators.DoubleEMAIndicator;
import org.ta4j.core.indicators.TripleEMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;
import com.etricky.cryptobot.core.strategies.rules.TradingEntryRule;
import com.etricky.cryptobot.core.strategies.rules.TradingExitRule;

import lombok.extern.slf4j.Slf4j;

@Component
// must be prototype as some indicator stores values in a cache
@Scope("prototype")
@Slf4j
public class TradingStrategyCustom extends AbstractStrategy {

	@Override
	public void createStrategy() {

		log.debug("start");

		ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(timeSeries);
		TripleEMAIndicator tema = new TripleEMAIndicator(closePriceIndicator,
				jsonFiles.getStrategiesJsonMap().get(beanName).getTimeFrameShort().intValue());
		DoubleEMAIndicator dema = new DoubleEMAIndicator(closePriceIndicator,
				jsonFiles.getStrategiesJsonMap().get(beanName).getTimeFrameLong().intValue());

		TradingEntryRule entryRule = new TradingEntryRule(closePriceIndicator,
				jsonFiles.getStrategiesJsonMap().get(beanName), tema, dema);
		entryRule.setAbstractStrategy(this);

		TradingExitRule exitRule = new TradingExitRule(closePriceIndicator,
				jsonFiles.getStrategiesJsonMap().get(beanName), tema, dema);
		exitRule.setAbstractStrategy(this);

		strategy = new BaseStrategy(beanName, entryRule, exitRule,
				jsonFiles.getStrategiesJsonMap().get(beanName).getInitialPeriod().intValue()
						/ jsonFiles.getStrategiesJsonMap().get(beanName).getBarDurationSec().intValue());

		strategyType = STRATEGY_TYPE_TRADING;

		log.debug("done");
	}
}
