package com.etricky.cryptobot.core.strategies.rules;

import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.AbstractRule;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.interfaces.jsonFiles.StrategiesJson;
import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StopLossExitRule2 extends AbstractRule {

	private ClosePriceIndicator closePriceIndicator;

	@Setter
	private AbstractStrategy abstractStrategy;

	private Num lossPercentage1;
	private Num lossPercentage2;
	// private Num lossPercentage3;

	private Num gainPercentage1;
	private Num gainPercentage2;
	// private Num gainPercentage3;

	private StrategiesJson strategiesSettings;

	public StopLossExitRule2(ClosePriceIndicator closePriceIndicator, StrategiesJson strategiesSettings) {
		this.closePriceIndicator = closePriceIndicator;

		this.gainPercentage1 = PrecisionNum.valueOf(strategiesSettings.getExitGainPercentage1())
				.dividedBy(PrecisionNum.valueOf(100));
		this.gainPercentage2 = PrecisionNum.valueOf(strategiesSettings.getExitGainPercentage2())
				.dividedBy(PrecisionNum.valueOf(100));
		// this.gainPercentage3 =
		// PrecisionNum.valueOf(strategiesSettings.getExitGainPercentage3()).dividedBy(PrecisionNum.valueOf(100));

		this.lossPercentage1 = PrecisionNum.valueOf(strategiesSettings.getExitLossPercentage1())
				.dividedBy(PrecisionNum.valueOf(100));
		this.lossPercentage2 = PrecisionNum.valueOf(strategiesSettings.getExitLossPercentage2())
				.dividedBy(PrecisionNum.valueOf(100));
		// this.lossPercentage3 =
		// PrecisionNum.valueOf(strategiesSettings.getExitLossPercentage3()).dividedBy(PrecisionNum.valueOf(100));

		this.strategiesSettings = strategiesSettings;

		log.debug("gainPercentage1: {} lossPercentage1: {} gainPercentage2: {} lossPercentage2: {}", gainPercentage1,
				lossPercentage1, gainPercentage2, lossPercentage2);
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean result = false;
		Num highPrice, buyPrice, closePrice, highPriceLossValue, hb, hc, rule10;

		if (strategiesSettings.getExitEnabled()) {
			log.trace("start. index: {}", index);

			if (tradingRecord.getCurrentTrade().isClosed()) {
				log.trace("trade is closed");
			} else {
				if (tradingRecord.getCurrentTrade().getEntry() != null) {

					log.trace("index: {}", tradingRecord.getLastEntry().getIndex());
					highPrice = PrecisionNum.valueOf(abstractStrategy.getHighPrice());
					buyPrice = tradingRecord.getLastEntry().getPrice();
					closePrice = closePriceIndicator.getValue(index);

					hb = highPrice.minus(buyPrice).dividedBy(buyPrice);
					highPriceLossValue = highPrice.multipliedBy(lossPercentage1);
					rule10 = highPrice.minus(highPriceLossValue);

					hc = highPrice.minus(closePrice).dividedBy(closePrice);

					// HighBuy < gainPercentage1 AND close < HighPrice - loss
					// OR HighBuy > gainPercentage2 AND HC < lossPercentage2
					if ((hb.isLessThanOrEqual(gainPercentage1) && closePrice.isLessThanOrEqual(rule10))
							|| hb.isGreaterThanOrEqual(gainPercentage2) && hc.isLessThanOrEqual(lossPercentage2)) {
						result = true;
					}

					log.trace("rule :: {} <= {} AND {} <= {} OR {} >= {} AND {} <= {}-> {} ",
							NumericFunctions.convertToBigDecimal(hb, NumericFunctions.PERCENTAGE_SCALE),
							NumericFunctions.convertToBigDecimal(gainPercentage1, NumericFunctions.PERCENTAGE_SCALE),
							NumericFunctions.convertToBigDecimal(closePrice, NumericFunctions.PRICE_SCALE),
							NumericFunctions.convertToBigDecimal(rule10, NumericFunctions.PRICE_SCALE),
							NumericFunctions.convertToBigDecimal(hb, NumericFunctions.PERCENTAGE_SCALE),
							NumericFunctions.convertToBigDecimal(gainPercentage2, NumericFunctions.PERCENTAGE_SCALE),
							NumericFunctions.convertToBigDecimal(hc, NumericFunctions.PERCENTAGE_SCALE),
							NumericFunctions.convertToBigDecimal(lossPercentage2, NumericFunctions.PERCENTAGE_SCALE),
							result);
					log.trace("\t\tbp: {} hp: {} hpl: {}",
							NumericFunctions.convertToBigDecimal(buyPrice, NumericFunctions.PRICE_SCALE),
							NumericFunctions.convertToBigDecimal(highPrice, NumericFunctions.PRICE_SCALE),
							NumericFunctions.convertToBigDecimal(highPriceLossValue, NumericFunctions.PRICE_SCALE));

				}
			}
		}

		log.trace("done. result: {}", result);
		// result = false;
		return result;
	}

}
