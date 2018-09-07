package com.etricky.cryptobot.core.strategies;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;
import com.etricky.cryptobot.core.strategies.rules.StopLossEntryRule;
import com.etricky.cryptobot.core.strategies.rules.StopLossExitRule;

import lombok.extern.slf4j.Slf4j;

@Component
// must be prototype as some indicator stores values in a cache
@Scope("prototype")
@Slf4j
public class StopLossStrategy extends AbstractStrategy {

	@Override
	public void createStrategy() {
		log.debug("start");

		ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);

		StopLossEntryRule stopLossEntryRule = new StopLossEntryRule(closePrice,
				jsonFiles.getStrategiesJsonMap().get(beanName));
		stopLossEntryRule.setAbstractStrategy(this);

		StopLossExitRule stopLossExitRule = new StopLossExitRule(closePrice,
				jsonFiles.getStrategiesJsonMap().get(beanName));
		stopLossExitRule.setAbstractStrategy(this);

		strategy = new BaseStrategy(beanName, stopLossEntryRule, stopLossExitRule,
				jsonFiles.getStrategiesJsonMap().get(beanName).getInitialPeriod().intValue()
						/ jsonFiles.getStrategiesJsonMap().get(beanName).getBarDurationSec().intValue());

		strategyType = STRATEGY_TYPE_STOP_LOSS;

		log.debug("done");
	}
}
