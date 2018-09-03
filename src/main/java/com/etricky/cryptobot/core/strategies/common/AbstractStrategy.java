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

	protected TimeSeries timeSeries;
	@Getter
	protected String beanName;
	@Getter
	protected int barDuration;

	protected StrategiesJson strategiesSettings;
	protected Strategy strategy;
	protected BigDecimal feePercentage;

	private ExchangeEnum exchangeEnum;
	private BigDecimal highPrice = BigDecimal.ZERO, lowPrice = BigDecimal.ZERO;

	public abstract void createStrategy();

	public void initialize(ExchangeEnum exchangeEnum, String beanName) {
		log.debug("start. exchange: {} beanName: {}", exchangeEnum.getName(), beanName);

		this.beanName = beanName;
		this.exchangeEnum = exchangeEnum;

		strategiesSettings = jsonFiles.getStrategiesJson().get(beanName);
		barDuration = strategiesSettings.getBarDurationSec().intValue();
		feePercentage = jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getFee();

		timeSeries = new BaseTimeSeries(beanName);
		timeSeries.setMaximumBarCount(strategiesSettings.getTimeSeriesBars().intValue());

		createStrategy();

		log.debug("done");
	}

	public boolean addTradeToStrategyTimeSeries(TradeEntity tradeEntity) throws ExchangeException {
		boolean result = false;
		log.trace("start");

		try {
			result = timeSeriesHelper.addTradeToTimeSeries(timeSeries, tradeEntity, barDuration,
					jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getAllowFakeTrades());
		} catch (ExchangeException e1) {
			log.error("Exception: {}", e1);
			log.error("trade: {}", tradeEntity);
			throw new ExchangeException(e1);
		}

		log.trace("done. result: {}", result);
		return result;
	}

	/**
	 * For a given trade, it executes the strategy to verify if it should enter or
	 * exit. This is done using its own timeSeries which holds the trade history,
	 * the currencyTradingRecord that contains the recent orders for the currency
	 * 
	 * @param tradeEntity                The trade values reported by the exchange
	 * @param currencyTradingRecord      Record that holds the enter/exit orders for
	 *                                   the currency
	 * @param currencyTimeSeriesEndIndex The last index in the timeSeries of the
	 *                                   currency
	 * @return the result of the execution of the strategy
	 * @throws ExchangeException
	 */
	public StrategyResult processStrategy(TradeEntity tradeEntity, TradingRecord currencyTradingRecord,
			int currencyTimeSeriesEndIndex) throws ExchangeException {
		StrategyResult strategyResult;
		int endIndex, result = NO_ACTION;
		Bar lastBar;

		log.trace("start. currency: {} timeSeries: {}", tradeEntity.getTradeId().getCurrency(), timeSeries.getName());

		// adds live trade to strategy time series and executes the strategies
		// considering the currency tradingRecord
		if (addTradeToStrategyTimeSeries(tradeEntity)) {
			endIndex = timeSeries.getEndIndex();

			lastBar = timeSeries.getLastBar();

			if (log.isTraceEnabled() && strategy.getName().equals(TradingStrategy.STRATEGY_NAME))
				debug(tradeEntity);

			if (currencyTradingRecord.getCurrentTrade().isNew()
					&& strategy.shouldEnter(endIndex, currencyTradingRecord)) {
				// strategy should enter

				// fills the strategy trading record that will be used by exchangeTrade
				currencyTradingRecord.enter(currencyTimeSeriesEndIndex, lastBar.getClosePrice(), Decimal.ONE);
				log.debug("ENTER -> strategy {} currency: {} price: {} indexes: {}/{}", strategy.getName(),
						tradeEntity.getTradeId().getCurrency(), tradeEntity.getClosePrice(), endIndex,
						currencyTimeSeriesEndIndex);
				result = ENTER;

			} else if (currencyTradingRecord.getCurrentTrade().isOpened()
					&& strategy.shouldExit(endIndex, currencyTradingRecord)) {
				// strategy should exit

				currencyTradingRecord.exit(currencyTimeSeriesEndIndex, lastBar.getClosePrice(), Decimal.ONE);
				log.debug("EXIT -> strategy {} currency: {} price: {} indexes s/c: {}/{}", strategy.getName(),
						tradeEntity.getTradeId().getCurrency(), tradeEntity.getClosePrice(), endIndex,
						currencyTimeSeriesEndIndex);
				result = EXIT;
			}

		} else {
			log.trace("no bar added");
		}

		strategyResult = StrategyResult.builder().result(result).strategyName(beanName).barDuration(barDuration)
				.tradeEntity(tradeEntity).closePrice(tradeEntity.getClosePrice())
				.timeSeriesEndIndex(currencyTimeSeriesEndIndex).highPrice(highPrice).lowPrice(lowPrice)
				.feePercentage(feePercentage).lastOrder(currencyTradingRecord.getLastOrder()).build();

		setHighPrice(highPrice, result);
		setLowPrice(lowPrice, result);

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

		log.trace("index {} tema: {} dema: {} closePrice: {}/{}", timeSeries.getEndIndex(),
				NumericFunctions.convertToBigDecimal(tema.getValue(endIndex), 2),
				NumericFunctions.convertToBigDecimal(dema.getValue(endIndex), 2), tradeEntity.getClosePrice(),
				tradeEntity.getTimestamp());
	}
}
