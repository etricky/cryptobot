package com.etricky.cryptobot.core.strategies;

import java.math.BigDecimal;

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
@Scope("prototype")
@Slf4j
public class TrailingStopLossStrategy extends AbstractStrategy {

	@Override
	public void createStrategy() {
		log.debug("start");

		ClosePriceIndicator closePrice = new ClosePriceIndicator(getTimeSeries());

		// TODO feeValue must be obtained by fee*amount to be traded
		Rule entryRule = new TraillingStopLossEntryRule(closePrice, Decimal.valueOf(getStrategiesSettings().getGainPercentage()),
				Decimal.valueOf(getStrategiesSettings().getFee().multiply(BigDecimal.TEN)));

		// TODO feeValue must be obtained by fee*amount to be traded
		Rule exitRule = new TraillingStopLossExitRule(closePrice, Decimal.valueOf(getStrategiesSettings().getLossPerc()),
				Decimal.valueOf(getStrategiesSettings().getGainPercentage()),
				Decimal.valueOf(getStrategiesSettings().getFee().multiply(BigDecimal.TEN)));

		setStrategy(new BaseStrategy(entryRule, exitRule));

		log.debug("done");
	}
}