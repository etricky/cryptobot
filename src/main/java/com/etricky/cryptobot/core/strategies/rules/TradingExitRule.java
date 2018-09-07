package com.etricky.cryptobot.core.strategies.rules;

import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.DoubleEMAIndicator;
import org.ta4j.core.indicators.TripleEMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.AbstractRule;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.interfaces.jsonFiles.StrategiesJson;
import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradingExitRule extends AbstractRule {

	private ClosePriceIndicator closePriceIndicator;

	@Setter
	private AbstractStrategy abstractStrategy;

	private Decimal lossPercentage1;
	// private Decimal lossPercentage2;
	// private Decimal lossPercentage3;

	// private Decimal gainPercentage1;
	// private Decimal gainPercentage2;
	// private Decimal gainPercentage3;

	private StrategiesJson strategiesSettings;
	private Rule exitRule;

	public TradingExitRule(ClosePriceIndicator closePriceIndicator, StrategiesJson strategiesSettings,
			TripleEMAIndicator tema, DoubleEMAIndicator dema) {
		this.closePriceIndicator = closePriceIndicator;

		// this.gainPercentage1 =
		// Decimal.valueOf(strategiesSettings.getExitGainPercentage1()).dividedBy(100);
		// this.gainPercentage2 =
		// Decimal.valueOf(strategiesSettings.getExitGainPercentage2()).dividedBy(100);
		// this.gainPercentage3 =
		// Decimal.valueOf(strategiesSettings.getExitGainPercentage3()).dividedBy(100);

		this.lossPercentage1 = Decimal.valueOf(strategiesSettings.getExitLossPercentage1()).dividedBy(100);
		// this.lossPercentage2 =
		// Decimal.valueOf(strategiesSettings.getExitLossPercentage2()).dividedBy(100);
		// this.lossPercentage3 =
		// Decimal.valueOf(strategiesSettings.getExitLossPercentage3()).dividedBy(100);

		this.strategiesSettings = strategiesSettings;
		exitRule = new CrossedDownIndicatorRule(tema, dema);

		log.debug("lossPercentage1: {}", lossPercentage1);
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean result = false;
		Decimal closePrice, rule10, deltaPrice;

		if (strategiesSettings.getExitEnabled()) {
			log.trace("start. index: {}", index);

			if (tradingRecord.getCurrentTrade().isClosed()) {
				log.trace("trade is closed");
			} else {
				if (tradingRecord.getCurrentTrade().getEntry() != null) {

					closePrice = closePriceIndicator.getValue(index);
					deltaPrice = closePrice.minus(tradingRecord.getLastOrder() == null ? Decimal.ZERO
							: tradingRecord.getLastOrder().getPrice());
					rule10 = deltaPrice.dividedBy(closePrice);

					// crossDown && (deltaPrice > 0 OR deltaPrice <= lossPercentage)
					if (exitRule.isSatisfied(index, tradingRecord) && (rule10.isGreaterThan(Decimal.ZERO)
							|| rule10.abs().isGreaterThanOrEqual(lossPercentage1))) {
						result = true;
					}

					log.trace("rule :: {} >0 || <= {} -> {}",
							NumericFunctions.convertToBigDecimal(rule10, NumericFunctions.PRICE_SCALE),
							NumericFunctions.convertToBigDecimal(lossPercentage1, NumericFunctions.PERCENTAGE_SCALE),
							result);
				}
			}
		}

		log.trace("done. result: {}", result);
		// result = false;
		return result;
	}

}
