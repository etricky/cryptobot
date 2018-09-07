package com.etricky.cryptobot.core.strategies.rules;

import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.DoubleEMAIndicator;
import org.ta4j.core.indicators.TripleEMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.AbstractRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.interfaces.jsonFiles.StrategiesJson;
import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradingEntryRule extends AbstractRule {
	@Setter
	private AbstractStrategy abstractStrategy;

	private ClosePriceIndicator closePriceIndicator;
	private Decimal lossPercentage1;
	// private Decimal lossPercentage2;
	// private Decimal lossPercentage3;

	// private Decimal gainPercentage1;
	// private Decimal gainPercentage2;
	// private Decimal gainPercentage3;

	private StrategiesJson strategiesSettings;
	private Rule entryRule;

	public TradingEntryRule(ClosePriceIndicator closePriceIndicator, StrategiesJson strategiesSettings,
			TripleEMAIndicator tema, DoubleEMAIndicator dema) {
		this.closePriceIndicator = closePriceIndicator;

		// this.gainPercentage1 =
		// Decimal.valueOf(strategiesSettings.getEntryGainPercentage1()).dividedBy(100);
		// this.gainPercentage2 =
		// Decimal.valueOf(strategiesSettings.getEntryGainPercentage2()).dividedBy(100);
		// this.gainPercentage3 =
		// Decimal.valueOf(strategiesSettings.getEntryGainPercentage3()).dividedBy(100);

		this.lossPercentage1 = Decimal.valueOf(strategiesSettings.getEntryLossPercentage1()).dividedBy(100);
		// this.lossPercentage2 =
		// Decimal.valueOf(strategiesSettings.getEntryLossPercentage2()).dividedBy(100);
		// this.lossPercentage3 =
		// Decimal.valueOf(strategiesSettings.getEntryLossPercentage3()).dividedBy(100);

		this.strategiesSettings = strategiesSettings;
		entryRule = new CrossedUpIndicatorRule(tema, dema);

		log.debug("lossPercentage1: {}", lossPercentage1);
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean result = false;
		Decimal closePrice, rule10, deltaPrice;

		if (strategiesSettings.getEntryEnabled()) {
			log.trace("start. index: {}", index);

			if (tradingRecord.getCurrentTrade().isNew()) {

				closePrice = closePriceIndicator.getValue(index);
				deltaPrice = closePrice.minus(
						tradingRecord.getLastOrder() == null ? Decimal.ZERO : tradingRecord.getLastOrder().getPrice());
				rule10 = deltaPrice.dividedBy(closePrice);

				// crossUp && (deltaPrice < 0 OR deltaPrice >= gainPercentage)
				if (entryRule.isSatisfied(index, tradingRecord)
						&& (rule10.isLessThan(Decimal.ZERO) || rule10.isGreaterThanOrEqual(lossPercentage1))) {
					result = true;
				}

				log.trace("rule :: {} <0 || >= {} -> {}",
						NumericFunctions.convertToBigDecimal(rule10, NumericFunctions.PRICE_SCALE),
						NumericFunctions.convertToBigDecimal(lossPercentage1, NumericFunctions.PERCENTAGE_SCALE),
						result);
			} else {
				log.trace("no trading record");
			}
		}

		log.trace("done. result: {}", result);
		// result = false;
		return result;
	}
}
