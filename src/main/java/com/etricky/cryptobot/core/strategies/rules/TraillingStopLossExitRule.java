package com.etricky.cryptobot.core.strategies.rules;

import org.ta4j.core.Decimal;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.AbstractRule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TraillingStopLossExitRule extends AbstractRule {

	private ClosePriceIndicator closePriceIndicator;
	private Decimal lossPercentage;
	private Decimal gainPercentage;
	private Decimal feePercentage;

	public TraillingStopLossExitRule(ClosePriceIndicator closePrice, Decimal lossPercentage, Decimal gainPercentage, Decimal feePercentage) {
		this.closePriceIndicator = closePrice;
		this.lossPercentage = lossPercentage.dividedBy(100);
		this.gainPercentage = gainPercentage.dividedBy(100);
		this.feePercentage = feePercentage.dividedBy(100);
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean result = false;
		Decimal highPrice, buyPrice, closePrice, feeValue, gainValue, highPriceLossValue, buyPriceLossValue, rule1, rule2, rule3;
		log.trace("start. index: {}", index);

		// closePrice < highPrice - lossPerc - fee AND highPrice > buyPrice + gainPerc + fee
		// OR closePrice < buyPrice - lossPerc

		if (tradingRecord.getCurrentTrade().isClosed()) {
			log.trace("trade is closed");
		} else {
			if (tradingRecord.getCurrentTrade().getEntry() != null && tradingRecord.getCurrentTrade().getEntry().getAmount() != null) {

				highPrice = getHighPrice(index, tradingRecord, closePriceIndicator);
				log.trace("barCount: {} max: {}", closePriceIndicator.getTimeSeries().getBarCount(),
						closePriceIndicator.getTimeSeries().getMaximumBarCount());
				log.trace("highPrice :: entry index: {}", tradingRecord.getLastEntry().getIndex());
				buyPrice = tradingRecord.getCurrentTrade().getEntry().getPrice();
				closePrice = closePriceIndicator.getValue(index);

				gainValue = buyPrice.multipliedBy(gainPercentage);
				highPriceLossValue = highPrice.multipliedBy(lossPercentage);
				buyPriceLossValue = tradingRecord.getCurrentTrade().getEntry().getPrice().multipliedBy(lossPercentage);
				feeValue = closePrice.multipliedBy(tradingRecord.getLastEntry().getAmount()).multipliedBy(feePercentage);

				rule1 = highPrice.minus(highPriceLossValue).minus(feeValue);
				rule2 = buyPrice.plus(gainValue).plus(feeValue);
				rule3 = buyPrice.minus(buyPriceLossValue);

				if (closePrice.isLessThan(rule1) && highPrice.isGreaterThan(rule2) || closePrice.isLessThan(rule3)) {
					result = true;
				}

				log.debug("rule :: {} < {} AND {} > {} OR {} < {} -> {}", closePrice, rule1, highPrice, rule2, closePrice, rule3, result);
				log.debug("\t\tcp: {} fee: {} bp: {} bpl: {} hp: {} hpl: {} gv: {}", closePrice, feeValue, buyPrice, buyPriceLossValue, highPrice,
						highPriceLossValue, gainValue);
			}
		}
		log.trace("done. result: {}", result);
		return result;
	}

	private Decimal getHighPrice(int index, TradingRecord tradingRecord, ClosePriceIndicator closePriceIndicator) {
		int buyIndex = tradingRecord.getLastEntry().getIndex();
		Decimal highPrice = Decimal.ZERO;
		for (int i = buyIndex; i <= index; i++) {
			if (highPrice.isLessThan(closePriceIndicator.getValue(i))) {
				highPrice = closePriceIndicator.getValue(i);
			}
		}
		return highPrice;
	}
}
