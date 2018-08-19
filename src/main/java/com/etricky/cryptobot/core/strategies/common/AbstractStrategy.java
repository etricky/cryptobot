package com.etricky.cryptobot.core.strategies.common;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.DoubleEMAIndicator;
import org.ta4j.core.indicators.TripleEMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.exchanges.common.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeException;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.interfaces.jsonFiles.StrategiesJson;
import com.etricky.cryptobot.core.strategies.TradingStrategy;
import com.etricky.cryptobot.model.TradesEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractStrategy {
	public final static int NO_ACTION = 0;
	public final static int ENTER = 1;
	public final static int EXIT = 2;

	@Autowired
	@Getter
	private JsonFiles jsonFiles;
	@Autowired
	TimeSeriesHelper timeSeriesHelper;

	protected StrategiesJson strategiesSettings;
	@Getter
	private BigDecimal feePercentage;
	@Getter
	private String beanName;
	@Getter
	private TimeSeries timeSeries;
	@Getter
	private int barDuration;
	@Setter
	private Strategy strategy;
	@Getter
	private ExchangeEnum exchangeEnum;

	public abstract void createStrategy();

	public void initializeStrategy(ExchangeEnum exchangeEnum, String beanName) {
		log.debug("start. exchangeEnum: {} beanName: {}", exchangeEnum.getName(), beanName);

		this.beanName = beanName;
		this.exchangeEnum = exchangeEnum;

		feePercentage = jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getFee();
		strategiesSettings = jsonFiles.getStrategiesJson().get(beanName);
		barDuration = strategiesSettings.getBarDurationSec().intValue();

		timeSeries = new BaseTimeSeries(beanName);
		timeSeries.setMaximumBarCount(strategiesSettings.getTimeSeriesBars().intValue());

		createStrategy();

		log.debug("done");
	}

	public void addTradeToTimeSeries(TradesEntity tradesEntity) throws ExchangeException {
		log.trace("start");

		timeSeriesHelper.addTradeToTimeSeries(timeSeries, tradesEntity, barDuration);

		log.trace("done");
	}

	public int processStrategyForLiveTrade(TradesEntity tradesEntity, TradingRecord tradingRecord, long globalIndex) throws ExchangeException {
		int result = NO_ACTION;

		log.trace("start. timeSeries: {}", timeSeries.getName());

		if (timeSeriesHelper.addTradeToTimeSeries(timeSeries, tradesEntity, barDuration)) {
			int endIndex = timeSeries.getEndIndex();

			if (strategy.getName().equals(TradingStrategy.STRATEGY_NAME))
				debug(tradesEntity, tradingRecord);

			if (tradingRecord.getCurrentTrade().isNew() && strategy.shouldEnter(endIndex, tradingRecord)) {
				// strategy should enter
				log.debug("strategy {} should ENTER on index: {}", strategy.getName(), globalIndex);
				result = ENTER;

			} else if (tradingRecord.getCurrentTrade().isOpened() && strategy.shouldExit(endIndex, tradingRecord)) {
				// strategy should exit
				log.debug("strategy {} should EXIT on index: {}", strategy.getName(), globalIndex);
				result = EXIT;
			}
		} else {
			log.trace("no bar added");
		}

		log.trace("done. result: {}", result);
		return result;
	}

	public void debug(TradesEntity tradesEntity, TradingRecord tradingRecord) {
		int endIndex = timeSeries.getEndIndex();
		ClosePriceIndicator closePrice = new ClosePriceIndicator(getTimeSeries());
		TripleEMAIndicator tema = new TripleEMAIndicator(closePrice, strategiesSettings.getTimeFrameLong().intValue());
		DoubleEMAIndicator dema = new DoubleEMAIndicator(closePrice, strategiesSettings.getTimeFrameShort().intValue());

		log.debug("index {} tema: {} dema: {} closePrice: {}/{}", timeSeries.getEndIndex(),
				NumericFunctions.convertToBigDecimal(tema.getValue(endIndex), 2), NumericFunctions.convertToBigDecimal(dema.getValue(endIndex), 2),
				tradesEntity.getClosePrice(), tradesEntity.getTimestamp());
	}
}
