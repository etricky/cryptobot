package com.etricky.cryptobot.core.strategies.rules;

import org.ta4j.core.Decimal;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.AbstractRule;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.interfaces.jsonFiles.StrategiesJson;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TraillingStopLossEntryRule extends AbstractRule {

	private ClosePriceIndicator closePriceIndicator;
	private Decimal lossPercentage1;
	// private Decimal lossPercentage2;
	// private Decimal lossPercentage3;
	private Decimal gainPercentage1;
	// private Decimal gainPercentage2;
	// private Decimal gainPercentage3;
	private Decimal feePercentage;

	private StrategiesJson strategiesSettings;

	public TraillingStopLossEntryRule(ClosePriceIndicator closePriceIndicator, Decimal feePercentage,
			StrategiesJson strategiesSettings) {
		this.closePriceIndicator = closePriceIndicator;
		this.gainPercentage1 = Decimal.valueOf(strategiesSettings.getEntryGainPercentage1()).dividedBy(100);
		// this.gainPercentage2 =
		// Decimal.valueOf(strategiesSettings.getEntryGainPercentage2()).dividedBy(100);
		// this.gainPercentage3 =
		// Decimal.valueOf(strategiesSettings.getEntryGainPercentage3()).dividedBy(100);

		this.lossPercentage1 = Decimal.valueOf(strategiesSettings.getEntryLossPercentage1()).dividedBy(100);
		// this.lossPercentage2 =
		// Decimal.valueOf(strategiesSettings.getEntryLossPercentage2()).dividedBy(100);
		// this.lossPercentage3 =
		// Decimal.valueOf(strategiesSettings.getEntryLossPercentage3()).dividedBy(100);

		this.feePercentage = feePercentage.dividedBy(100);

		this.strategiesSettings = strategiesSettings;
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean result = false;
		Decimal closePrice, sellPrice, lowPrice, feeValue, gainValue, amount, initialAmount, initialBalance,
				deltaAmount, rule10;

		if (strategiesSettings.getEntryEnabled()) {
			log.trace("start. index: {}", index);

			if (tradingRecord.getCurrentTrade().isNew() && tradingRecord.getLastExit() != null) {
				initialAmount = tradingRecord.getLastExit().getAmount();
				initialBalance = tradingRecord.getLastExit().getPrice().multipliedBy(initialAmount);

				// exit and entry fees
				feeValue = initialBalance.multipliedBy(feePercentage);
				initialBalance = initialBalance.minus(feeValue);
				feeValue = initialBalance.multipliedBy(feePercentage);
				initialBalance = initialBalance.minus(feeValue);

				closePrice = closePriceIndicator.getValue(index);
				sellPrice = tradingRecord.getLastExit().getPrice();
				lowPrice = getLowPrice(index, tradingRecord, closePriceIndicator);

				gainValue = lowPrice.multipliedBy(gainPercentage1);
				rule10 = lowPrice.plus(gainValue);
				amount = initialBalance.dividedBy(closePrice);
				deltaAmount = amount.minus(initialAmount).dividedBy(amount);

				// closePrice > lowPrice * gain && closePrice < sellPrice
				// OR deltaAmount > loss
				if (closePrice.isGreaterThanOrEqual(rule10) && closePrice.isLessThanOrEqual(sellPrice)
						|| (deltaAmount.isNegative() && deltaAmount.abs().isGreaterThanOrEqual(lossPercentage1))) {
					result = true;
				}

				log.debug("rule :: {} >= {} && cp < {} || {} >= {} -> {}",
						NumericFunctions.convertToBigDecimal(closePrice, NumericFunctions.PRICE_SCALE),
						NumericFunctions.convertToBigDecimal(rule10, NumericFunctions.PRICE_SCALE),
						NumericFunctions.convertToBigDecimal(sellPrice, NumericFunctions.PRICE_SCALE),
						NumericFunctions.convertToBigDecimal(deltaAmount.abs(), NumericFunctions.PERCENTAGE_SCALE),
						NumericFunctions.convertToBigDecimal(lossPercentage1, NumericFunctions.PERCENTAGE_SCALE),
						result);
				log.debug("\t\tlp:{} sp: {} fee: {}",
						NumericFunctions.convertToBigDecimal(lowPrice, NumericFunctions.PRICE_SCALE),
						NumericFunctions.convertToBigDecimal(sellPrice, NumericFunctions.PRICE_SCALE),
						NumericFunctions.convertToBigDecimal(feeValue, NumericFunctions.PRICE_SCALE));

			} else {
				log.trace("no trading record");
			}
		}

		log.trace("done. result: {}", result);
		// result = false;
		return result;
	}

	private Decimal getLowPrice(int index, TradingRecord tradingRecord, ClosePriceIndicator closePriceIndicator) {
		int sellIndex = tradingRecord.getLastExit().getIndex();
		Decimal lowPrice = tradingRecord.getLastExit().getPrice();
		for (int i = sellIndex; i <= index; i++) {
			if (lowPrice.isGreaterThan(closePriceIndicator.getValue(i))) {
				lowPrice = closePriceIndicator.getValue(i);
			}
		}
		return lowPrice;
	}
}
