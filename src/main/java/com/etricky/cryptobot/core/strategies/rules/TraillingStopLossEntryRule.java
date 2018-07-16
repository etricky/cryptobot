package com.etricky.cryptobot.core.strategies.rules;

import org.ta4j.core.Decimal;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.AbstractRule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TraillingStopLossEntryRule extends AbstractRule {

	private ClosePriceIndicator closePriceIndicator;
	private Decimal gainPercentage;
	private Decimal feePercentage;

	public TraillingStopLossEntryRule(ClosePriceIndicator closePriceIndicator, Decimal gainPercentage, Decimal feePercentage) {
		this.closePriceIndicator = closePriceIndicator;
		this.gainPercentage = gainPercentage.dividedBy(100);
		this.feePercentage = feePercentage.dividedBy(100);
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean result = false;
		Decimal closePrice, feeValue, gainValue, sellPrice;
		log.trace("start. index: {}", index);

		// closePrice > sellPrice + gainPerc + fee
		if (tradingRecord.getCurrentTrade().isNew() && tradingRecord.getLastExit() != null) {
			closePrice = closePriceIndicator.getValue(index);
			sellPrice = tradingRecord.getLastExit().getPrice();

			gainValue = sellPrice.multipliedBy(gainPercentage);
			feeValue = closePrice.multipliedBy(feePercentage);

			if (closePrice.isGreaterThan(sellPrice.plus(gainValue).plus(feeValue))) {
				result = true;
			}

			log.debug("rule :: {} > {} -> {}", closePrice, sellPrice.plus(gainValue).plus(feeValue), result);
			log.debug("\t\t cp: {} sp: {} gp: {} fee: {}", closePrice, sellPrice, gainValue, feeValue);
		} else {
			log.trace("no trading record");
		}

		log.trace("done. result: {}", result);
		return result;
	}

}
