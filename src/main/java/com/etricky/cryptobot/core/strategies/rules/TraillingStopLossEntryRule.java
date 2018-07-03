package com.etricky.cryptobot.core.strategies.rules;

import org.ta4j.core.Decimal;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.AbstractRule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TraillingStopLossEntryRule extends AbstractRule {

	private ClosePriceIndicator closePrice;
	private Decimal gainPercentage;
	private Decimal feeValue;

	public TraillingStopLossEntryRule(ClosePriceIndicator closePrice, Decimal gainPercentage, Decimal feeValue) {
		this.closePrice = closePrice;
		this.gainPercentage = gainPercentage;
		this.feeValue = feeValue;
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean result = false;
		log.debug("start. index: {}", index);

		// closePrice > sellPrice + gainPerc + fee
		if (closePrice.getValue(index).isGreaterThan(tradingRecord.getLastOrder().getPrice()
				.plus(gainPercentage.multipliedBy(tradingRecord.getLastOrder().getPrice())).plus(feeValue))) {

			result = true;
		}

		log.trace("rule :: closePrice: {} > sellPrice {} + gainPercentage: {} + fee: {}", closePrice.getValue(index),
				tradingRecord.getLastOrder().getPrice(),
				gainPercentage.multipliedBy(tradingRecord.getLastOrder().getPrice()), feeValue);

		log.debug("done. result: {}", result);
		return result;
	}

}
