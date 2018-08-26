package com.etricky.cryptobot.core.strategies;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;
import com.etricky.cryptobot.core.strategies.rules.TraillingStopLossEntryRule;
import com.etricky.cryptobot.core.strategies.rules.TraillingStopLossExitRule;

import lombok.extern.slf4j.Slf4j;

@Component
// must be prototype as some indicator stores values in a cache
@Scope("prototype")
@Slf4j
public class TrailingStopLossStrategy extends AbstractStrategy {

	public static String STRATEGY_NAME = "trailingStopLossStrategy";

	@Override
	public void createStrategy() {
		log.debug("start");

		ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);

		Rule entryRule = new TraillingStopLossEntryRule(closePrice, Decimal.valueOf(feePercentage), strategiesSettings);

		Rule exitRule = new TraillingStopLossExitRule(closePrice, Decimal.valueOf(feePercentage), strategiesSettings);

		strategy = new BaseStrategy(beanName, entryRule, exitRule,
				strategiesSettings.getInitialPeriod().intValue() / strategiesSettings.getBarDurationSec().intValue());

		log.debug("done");
	}
}
