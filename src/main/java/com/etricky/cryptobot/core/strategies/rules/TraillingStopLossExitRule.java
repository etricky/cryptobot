package com.etricky.cryptobot.core.strategies.rules;

import org.ta4j.core.Decimal;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighestValueIndicator;
import org.ta4j.core.trading.rules.AbstractRule;

public class TraillingStopLossExitRule extends AbstractRule {

	private ClosePriceIndicator closePrice;
	private Decimal lossPercentage;
	private Decimal gainPercentage;
	private Decimal feeValue;
	private Decimal highPrice;

	public TraillingStopLossExitRule(ClosePriceIndicator closePrice, Decimal lossPercentage, Decimal gainPercentage,
			Decimal feeValue) {
		this.closePrice = closePrice;
		this.lossPercentage = lossPercentage;
		this.gainPercentage = gainPercentage;
		this.feeValue = feeValue;
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean result = false;
		log.debug("start. index: {}", index);

		// (closePrice < highPrice - lossPerc - fee AND
		// highPrice > buyPrice + gainPerc)
		// OR closePrice < buyPrice - lossPerc

		if (tradingRecord.getCurrentTrade().isClosed()) {
			log.debug("trade is closed");
		} else {

			highPrice = new HighestValueIndicator(closePrice, index - tradingRecord.getLastEntry().getIndex())
					.getValue(index);

			if (closePrice.getValue(index).isLessThan(
					highPrice.minus(closePrice.getValue(index).multipliedBy(lossPercentage)).minus(feeValue))) {

				if (highPrice.isGreaterThan(tradingRecord.getLastOrder().getPrice()
						.plus(gainPercentage.multipliedBy(tradingRecord.getLastOrder().getPrice())))) {

					result = true;
				}
			}

			if (closePrice.getValue(index).isLessThan(tradingRecord.getLastOrder().getPrice()
					.minus(tradingRecord.getLastOrder().getPrice().multipliedBy(lossPercentage)))) {

				result = true;
			}

			log.trace("rule :: closePrice {} < highPrice: {} - lossPerc: {} - fee: {}", closePrice.getValue(index),
					highPrice, closePrice.getValue(index).multipliedBy(lossPercentage), feeValue);
			log.trace("\tAND highPrice: {} > buyPrice: {} + gainPerc: {}", highPrice,
					tradingRecord.getLastOrder().getPrice(),
					gainPercentage.multipliedBy(tradingRecord.getLastOrder().getPrice()));
			log.trace("\tOR closePrice {} < buyPrice: {} - lossPerc: {}", closePrice.getValue(index),
					tradingRecord.getLastOrder().getPrice(),
					tradingRecord.getLastOrder().getPrice().multipliedBy(lossPercentage));
		}
		log.debug("done. result: {}", result);
		return result;

	}

}
