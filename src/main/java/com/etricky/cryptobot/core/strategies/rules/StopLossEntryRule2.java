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
public class StopLossEntryRule2 extends AbstractRule {
	@Setter
	private AbstractStrategy abstractStrategy;

	private ClosePriceIndicator closePriceIndicator;
	private Num lossPercentage1;
	// private Num lossPercentage2;
	// private Num lossPercentage3;
	private Num gainPercentage1;
	private Num gainPercentage2;
	// private Num gainPercentage3;

	private StrategiesJson strategiesSettings;

	public StopLossEntryRule2(ClosePriceIndicator closePriceIndicator, StrategiesJson strategiesSettings) {
		this.closePriceIndicator = closePriceIndicator;
		this.gainPercentage1 = PrecisionNum.valueOf(strategiesSettings.getEntryGainPercentage1())
				.dividedBy(PrecisionNum.valueOf(100));
		this.gainPercentage2 = PrecisionNum.valueOf(strategiesSettings.getEntryGainPercentage2())
				.dividedBy(PrecisionNum.valueOf(100));
		// this.gainPercentage3 =
		// PrecisionNum.valueOf(strategiesSettings.getEntryGainPercentage3()).dividedBy(PrecisionNum.valueOf(100));

		this.lossPercentage1 = PrecisionNum.valueOf(strategiesSettings.getEntryLossPercentage1())
				.dividedBy(PrecisionNum.valueOf(100));
		// this.lossPercentage2 =
		// PrecisionNum.valueOf(strategiesSettings.getEntryLossPercentage2()).dividedBy(PrecisionNum.valueOf(100));
		// this.lossPercentage3 =
		// PrecisionNum.valueOf(strategiesSettings.getEntryLossPercentage3()).dividedBy(PrecisionNum.valueOf(100));

		this.strategiesSettings = strategiesSettings;

		log.debug("gainPercentage1: {} lossPercentage1: {} gainPercentage2: {}", gainPercentage1, lossPercentage1,
				gainPercentage2);
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean result = false;
		Num closePrice, sellPrice, lowPrice, gainValue, amount, initialAmount, balance, deltaAmount, cl, rule10;

		if (strategiesSettings.getEntryEnabled()) {
			log.trace("start. index: {}", index);

			if (tradingRecord.getCurrentTrade().isNew() && tradingRecord.getLastExit() != null) {
				initialAmount = PrecisionNum.valueOf(abstractStrategy.getExchangeAmount());
				// balance of the last sell minus the fee
				balance = PrecisionNum.valueOf(abstractStrategy.getBalance());
				closePrice = closePriceIndicator.getValue(index);
				sellPrice = tradingRecord.getLastExit() != null ? tradingRecord.getLastExit().getPrice()
						: PrecisionNum.valueOf(0);
				lowPrice = PrecisionNum.valueOf(abstractStrategy.getLowPrice());

				gainValue = lowPrice.multipliedBy(gainPercentage1);
				rule10 = lowPrice.plus(gainValue);
				amount = balance.dividedBy(closePrice);
				deltaAmount = amount.minus(initialAmount).dividedBy(amount);

				cl = closePrice.minus(lowPrice).dividedBy(closePrice);

				// closePrice > lowPrice * gain && closePrice < sellPrice
				// OR deltaAmount > loss
				// OR cl > gainPercentage2
				if (closePrice.isGreaterThanOrEqual(rule10) && closePrice.isLessThanOrEqual(sellPrice)
						|| (deltaAmount.isNegative() && deltaAmount.abs().isGreaterThanOrEqual(lossPercentage1))
						|| cl.isGreaterThanOrEqual(gainPercentage2)) {
					result = true;
				}

				log.trace("rule :: {} >= {} && cp < {} || {} >= {} || {} >= {} -> {}",
						NumericFunctions.convertToBigDecimal(closePrice, NumericFunctions.PRICE_SCALE),
						NumericFunctions.convertToBigDecimal(rule10, NumericFunctions.PRICE_SCALE),
						NumericFunctions.convertToBigDecimal(sellPrice, NumericFunctions.PRICE_SCALE),
						NumericFunctions.convertToBigDecimal(deltaAmount.abs(), NumericFunctions.PERCENTAGE_SCALE),
						NumericFunctions.convertToBigDecimal(lossPercentage1, NumericFunctions.PERCENTAGE_SCALE),
						NumericFunctions.convertToBigDecimal(cl, NumericFunctions.PERCENTAGE_SCALE),
						NumericFunctions.convertToBigDecimal(gainPercentage2, NumericFunctions.PERCENTAGE_SCALE),
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
