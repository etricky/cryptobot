package com.etricky.cryptobot.core.strategies.rules;

import org.ta4j.core.Decimal;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.AbstractRule;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.interfaces.jsonFiles.StrategiesJson;
import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StopLossEntryRule extends AbstractRule {
	@Setter
	private AbstractStrategy abstractStrategy;

	private ClosePriceIndicator closePriceIndicator;
	private Decimal lossPercentage1;
	// private Decimal lossPercentage2;
	// private Decimal lossPercentage3;
	private Decimal gainPercentage1;
	// private Decimal gainPercentage2;
	// private Decimal gainPercentage3;

	private StrategiesJson strategiesSettings;

	public StopLossEntryRule(ClosePriceIndicator closePriceIndicator, StrategiesJson strategiesSettings) {
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

		this.strategiesSettings = strategiesSettings;

		log.debug("gainPercentage1: {} lossPercentage1: {}", gainPercentage1, lossPercentage1);
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean result = false;
		Decimal closePrice, sellPrice, lowPrice, gainValue, amount, initialAmount, balance, deltaAmount, rule10;

		if (strategiesSettings.getEntryEnabled()) {
			log.trace("start. index: {}", index);

			if (tradingRecord.getCurrentTrade().isNew() && tradingRecord.getLastExit() != null) {
				initialAmount = Decimal.valueOf(abstractStrategy.getExchangeAmount());
				// balance of the last sell minus the fee
				balance = Decimal.valueOf(abstractStrategy.getBalance());
				closePrice = closePriceIndicator.getValue(index);
				sellPrice = tradingRecord.getLastExit() != null ? tradingRecord.getLastExit().getPrice() : Decimal.ZERO;
				lowPrice = Decimal.valueOf(abstractStrategy.getLowPrice());

				gainValue = lowPrice.multipliedBy(gainPercentage1);
				rule10 = lowPrice.plus(gainValue);
				amount = balance.dividedBy(closePrice);
				deltaAmount = amount.minus(initialAmount).dividedBy(amount);

				// closePrice > lowPrice * gain && closePrice < sellPrice
				// OR deltaAmount > loss
				if (closePrice.isGreaterThanOrEqual(rule10) && closePrice.isLessThanOrEqual(sellPrice)
						|| (deltaAmount.isNegative() && deltaAmount.abs().isGreaterThanOrEqual(lossPercentage1))) {
					result = true;
				}

				log.trace("rule :: {} >= {} && cp < {} || {} >= {} -> {}",
						NumericFunctions.convertToBigDecimal(closePrice, NumericFunctions.PRICE_SCALE),
						NumericFunctions.convertToBigDecimal(rule10, NumericFunctions.PRICE_SCALE),
						NumericFunctions.convertToBigDecimal(sellPrice, NumericFunctions.PRICE_SCALE),
						NumericFunctions.convertToBigDecimal(deltaAmount.abs(), NumericFunctions.PERCENTAGE_SCALE),
						NumericFunctions.convertToBigDecimal(lossPercentage1, NumericFunctions.PERCENTAGE_SCALE),
						result);
				log.trace("\t\tlp:{} sp: {}",
						NumericFunctions.convertToBigDecimal(lowPrice, NumericFunctions.PRICE_SCALE),
						NumericFunctions.convertToBigDecimal(sellPrice, NumericFunctions.PRICE_SCALE));

			} else {
				log.trace("no trading record");
			}
		}

		log.trace("done. result: {}", result);
		// result = false;
		return result;
	}
}
