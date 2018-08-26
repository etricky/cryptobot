package com.etricky.cryptobot.core.strategies.rules;

import org.ta4j.core.Decimal;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.AbstractRule;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.interfaces.jsonFiles.StrategiesJson;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TraillingStopLossExitRule extends AbstractRule {

	private ClosePriceIndicator closePriceIndicator;

	private Decimal lossPercentage1;
	// private Decimal lossPercentage2;
	// private Decimal lossPercentage3;

	private Decimal gainPercentage1;
	// private Decimal gainPercentage2;
	// private Decimal gainPercentage3;

	private Decimal feePercentage;

	private StrategiesJson strategiesSettings;

	public TraillingStopLossExitRule(ClosePriceIndicator closePrice, Decimal feePercentage,
			StrategiesJson strategiesSettings) {
		this.closePriceIndicator = closePrice;

		this.gainPercentage1 = Decimal.valueOf(strategiesSettings.getExitGainPercentage1()).dividedBy(100);
		// this.gainPercentage2 =
		// Decimal.valueOf(strategiesSettings.getExitGainPercentage2()).dividedBy(100);
		// this.gainPercentage3 =
		// Decimal.valueOf(strategiesSettings.getExitGainPercentage3()).dividedBy(100);

		this.lossPercentage1 = Decimal.valueOf(strategiesSettings.getExitLossPercentage1()).dividedBy(100);
		// this.lossPercentage2 =
		// Decimal.valueOf(strategiesSettings.getExitLossPercentage2()).dividedBy(100);
		// this.lossPercentage3 =
		// Decimal.valueOf(strategiesSettings.getExitLossPercentage3()).dividedBy(100);

		this.feePercentage = feePercentage.dividedBy(100);

		this.strategiesSettings = strategiesSettings;

		log.debug("feePercentage: {} gainPercentage1: {} lossPercentage1: {}", this.feePercentage, gainPercentage1,
				lossPercentage1);
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean result = false;
		Decimal highPrice, buyPrice, closePrice, highPriceLossValue, rule10;

		if (strategiesSettings.getExitEnabled()) {
			log.trace("start. index: {}", index);

			if (tradingRecord.getCurrentTrade().isClosed()) {
				log.trace("trade is closed");
			} else {
				if (tradingRecord.getCurrentTrade().getEntry() != null
						&& tradingRecord.getCurrentTrade().getEntry().getAmount() != null) {

					log.trace("index: {}", tradingRecord.getLastEntry().getIndex());
					highPrice = getHighPrice(index, tradingRecord, closePriceIndicator);
					buyPrice = tradingRecord.getCurrentTrade().getEntry().getPrice();
					closePrice = closePriceIndicator.getValue(index);

					Decimal hb = highPrice.minus(buyPrice).dividedBy(buyPrice);
					highPriceLossValue = highPrice.multipliedBy(lossPercentage1);
					rule10 = highPrice.minus(highPriceLossValue);

					if (hb.isLessThanOrEqual(gainPercentage1) && closePrice.isLessThanOrEqual(rule10)) {
						result = true;
					}

					log.debug("rule :: {} <= {} AND {} <= {} -> {}",
							NumericFunctions.convertToBigDecimal(hb, NumericFunctions.PERCENTAGE_SCALE),
							NumericFunctions.convertToBigDecimal(gainPercentage1, NumericFunctions.PERCENTAGE_SCALE),
							NumericFunctions.convertToBigDecimal(closePrice, NumericFunctions.PRICE_SCALE),
							NumericFunctions.convertToBigDecimal(rule10, NumericFunctions.PRICE_SCALE), result);
					log.debug("\t\tbp: {} hp: {} hpl: {}",
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
