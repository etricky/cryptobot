package com.etricky.cryptobot.core.strategies.common;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Decimal;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.DoubleEMAIndicator;
import org.ta4j.core.indicators.TripleEMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.interfaces.jsonFiles.StrategiesJson;
import com.etricky.cryptobot.core.strategies.StrategyResult;
import com.etricky.cryptobot.core.strategies.TradingStrategy;
import com.etricky.cryptobot.model.TradeEntity;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractStrategy {
	public final static int NO_ACTION = 0;
	public final static int ENTER = 1;
	public final static int EXIT = 2;
	public final static int STRATEGY_ALL = 0;
	public final static int STRATEGY_STOP_LOSS = 1;
	public final static int STRATEGY_TRADING = 2;

	@Autowired
	private JsonFiles jsonFiles;
	@Autowired
	TimeSeriesHelper timeSeriesHelper;

	@Getter
	protected TimeSeries timeSeries;
	@Getter
	protected String beanName;
	@Getter
	protected int barDuration;
	@Getter
	protected BigDecimal feePercentage;
	protected StrategiesJson strategiesSettings;
	protected Strategy strategy;

	private BigDecimal highPrice = BigDecimal.ZERO, lowPrice = BigDecimal.ZERO;

	public abstract void createStrategy();

	public void initialize(ExchangeEnum exchangeEnum, String beanName) {
		log.debug("start. exchange: {} beanName: {}", exchangeEnum.getName(), beanName);

		this.beanName = beanName;

		feePercentage = jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getFee();
		strategiesSettings = jsonFiles.getStrategiesJson().get(beanName);
		barDuration = strategiesSettings.getBarDurationSec().intValue();

		timeSeries = new BaseTimeSeries(beanName);
		timeSeries.setMaximumBarCount(strategiesSettings.getTimeSeriesBars().intValue());

		createStrategy();

		log.debug("done");
	}

	public boolean addTradeToStrategyTimeSeries(TradeEntity tradeEntity) throws ExchangeException {
		boolean result = false;
		log.trace("start");

		try {
			result = timeSeriesHelper.addTradeToTimeSeries(timeSeries, tradeEntity, barDuration);
		} catch (ExchangeException e1) {
			log.error("Exception: {}", e1);
			log.error("trade: {}", tradeEntity);
			throw new ExchangeException(e1);
		}

		log.trace("done. result: {}", result);
		return result;
	}

	public StrategyResult processStrategy(TradeEntity tradeEntity, TradingRecord tradingRecord)
			throws ExchangeException {
		StrategyResult strategyResult;
		int endIndex, result = NO_ACTION;
		Bar lastBar = timeSeries.getLastBar();

		log.trace("start. timeSeries: {}", timeSeries.getName());

		// adds live trade to strategy time series and executes the strategies
		if (addTradeToStrategyTimeSeries(tradeEntity)) {
			endIndex = timeSeries.getEndIndex();

			if (log.isDebugEnabled() && strategy.getName().equals(TradingStrategy.STRATEGY_NAME))
				debug(tradeEntity);

			if (tradingRecord.getCurrentTrade().isNew() && strategy.shouldEnter(endIndex, tradingRecord)) {
				// strategy should enter

				// fills the strategy trading record that will be used by exchangeTrade
				tradingRecord.enter(endIndex, lastBar.getClosePrice(), Decimal.ONE);
				log.debug("strategy {} should ENTER on index: {}", strategy.getName(), timeSeries.getEndIndex());
				result = ENTER;

			} else if (tradingRecord.getCurrentTrade().isOpened() && strategy.shouldExit(endIndex, tradingRecord)) {
				// strategy should exit

				tradingRecord.exit(endIndex, lastBar.getClosePrice(), Decimal.ONE);
				log.debug("strategy {} should EXIT on index: {}", strategy.getName(), timeSeries.getEndIndex());
				result = EXIT;
			}

			setHighPrice(highPrice, result);
			setLowPrice(lowPrice, result);

		} else {
			log.trace("no bar added");
		}

		strategyResult = StrategyResult.builder().result(result).beanName(beanName).barDuration(barDuration)
				.feePercentage(feePercentage).tradeEntity(tradeEntity).closePrice(tradeEntity.getClosePrice())
				.timeSeriesIndex(timeSeries.getEndIndex()).highPrice(highPrice).lowPrice(lowPrice).build();

		log.trace("done. strategyResult: {}", strategyResult);
		return strategyResult;
	}

	private void setHighPrice(BigDecimal value, int result) {
		if (result != NO_ACTION || highPrice.compareTo(value) < 0) {
			highPrice = value;
		}

	}

	private void setLowPrice(BigDecimal value, int result) {
		if (result != NO_ACTION || lowPrice.compareTo(value) > 0) {
			lowPrice = value;
		}

	}

	private void debug(TradeEntity tradeEntity) {
		int endIndex = timeSeries.getEndIndex();
		ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);
		TripleEMAIndicator tema = new TripleEMAIndicator(closePrice, strategiesSettings.getTimeFrameLong().intValue());
		DoubleEMAIndicator dema = new DoubleEMAIndicator(closePrice, strategiesSettings.getTimeFrameShort().intValue());

		log.debug("index {} tema: {} dema: {} closePrice: {}/{}", timeSeries.getEndIndex(),
				NumericFunctions.convertToBigDecimal(tema.getValue(endIndex), 2),
				NumericFunctions.convertToBigDecimal(dema.getValue(endIndex), 2), tradeEntity.getClosePrice(),
				tradeEntity.getTimestamp());
	}
}
