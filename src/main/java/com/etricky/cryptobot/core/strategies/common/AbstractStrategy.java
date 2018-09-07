package com.etricky.cryptobot.core.strategies.common;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Order.OrderType;
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
import com.etricky.cryptobot.model.TradeEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractStrategy {
	public final static int NO_ACTION = 0;
	public final static int ENTER = 1;
	public final static int EXIT = 2;
	public final static int STRATEGY_ALL = 0;
	public final static int STRATEGY_STOP_LOSS = 1;
	public final static int STRATEGY_TRADING = 2;
	public final static String STRATEGY_TYPE_TRADING = "trading";
	public final static String STRATEGY_TYPE_STOP_LOSS = "stopLoss";

	@Autowired
	protected JsonFiles jsonFiles;
	@Autowired
	TimeSeriesHelper timeSeriesHelper;

	protected TimeSeries timeSeries;
	@Getter
	protected String beanName;
	@Getter
	protected int barDuration;
	@Getter
	protected String strategyType;

	@Getter
	protected BigDecimal highPrice = BigDecimal.ZERO, lowPrice = BigDecimal.ZERO, balance, amount, feeValue;
	@Getter
	@Setter
	protected BigDecimal exchangeBalance = BigDecimal.ZERO, exchangeAmount = BigDecimal.ZERO;
	protected Strategy strategy;

	private ExchangeEnum exchangeEnum;

	public abstract void createStrategy();

	public static String getOrderTypeString(int orderType) {
		if (orderType == ENTER) {
			return "ENTER";
		}
		return "EXIT";
	}

	public void initialize(ExchangeEnum exchangeEnum, String beanName) {
		log.debug("start. exchange: {} beanName: {}", exchangeEnum.getName(), beanName);

		this.beanName = beanName;
		this.exchangeEnum = exchangeEnum;

		StrategiesJson strategiesSettings = jsonFiles.getStrategiesJsonMap().get(beanName);
		barDuration = strategiesSettings.getBarDurationSec().intValue();

		timeSeries = new BaseTimeSeries(beanName);
		timeSeries.setMaximumBarCount(strategiesSettings.getTimeSeriesBars().intValue());

		createStrategy();

		log.debug("done");
	}

	public boolean addTradeToTimeSeries(TradeEntity tradeEntity) throws ExchangeException {
		boolean result = false;
		log.trace("start");

		try {
			result = timeSeriesHelper.addTradeToTimeSeries(timeSeries, tradeEntity, barDuration,
					jsonFiles.getExchangesJsonMap().get(exchangeEnum.getName()).getAllowFakeTrades());
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
	 * @param tradeEntity                   The trade values reported by the
	 *                                      exchange
	 * @param currencyTradingRecord         Record that holds the enter/exit orders
	 *                                      for the currency
	 * @param currencyTradingRecordEndIndex The last index in the trading record of
	 *                                      the currency
	 * @param balance                       Current available balance
	 * @param amount                        Current available amount
	 * @param feeValue                      The value of the fee that will be payed
	 *                                      to execute the order
	 * @param highPrice                     Highest price since last order
	 * @param lowPrice                      Lowest price since last order
	 * @param exchangeBalance               Balance available on the exchange
	 * @param exchangeAmount                Amount available on the exchange
	 * @param exchangeLastOrderType         The type of the last order executed on
	 *                                      the exchange for this trade
	 * @return the result of the execution of the strategy
	 * @throws ExchangeException
	 */
	public StrategyResult processStrategy(TradeEntity tradeEntity, TradingRecord currencyTradingRecord,
			int currencyTradingRecordEndIndex, BigDecimal balance, BigDecimal amount, BigDecimal feeValue,
			BigDecimal highPrice, BigDecimal lowPrice, BigDecimal exchangeBalance, BigDecimal exchangeAmount,
			OrderType exchangeLastOrderType) throws ExchangeException {
		StrategyResult strategyResult;
		int endIndex, result = NO_ACTION;
		Bar lastBar;

		log.trace(
				"start. currency: {} timeSeries: {} balance: {} amount: {} lastOrderBalance: {} lastOrderAmount: {} exchangeLastOrderType: {}",
				tradeEntity.getTradeId().getCurrency(), timeSeries.getName(), balance, amount, exchangeBalance,
				exchangeAmount, exchangeLastOrderType);

		this.balance = balance;
		this.amount = amount;
		this.highPrice = highPrice;
		this.lowPrice = lowPrice;
		this.exchangeBalance = exchangeBalance;
		this.exchangeAmount = exchangeAmount;

		// adds live trade to strategy time series and executes the strategies
		// considering the currency tradingRecord
		if (addTradeToTimeSeries(tradeEntity)) {

			endIndex = timeSeries.getEndIndex();
			lastBar = timeSeries.getLastBar();

			if (log.isTraceEnabled() && jsonFiles.getStrategiesJsonMap().get(strategy.getName()).getType()
					.equalsIgnoreCase(STRATEGY_TYPE_TRADING))
				debug(tradeEntity);

			if (currencyTradingRecord.getCurrentTrade().isNew()
					&& strategy.shouldEnter(endIndex, currencyTradingRecord)) {

//				currencyTradingRecord.enter(currencyTradingRecordEndIndex, lastBar.getClosePrice(), Decimal.ONE);
//				log.debug("ENTER -> strategy {} currency: {} price: {} indexes s/c: {}/{}", strategy.getName(),
//						tradeEntity.getTradeId().getCurrency(), tradeEntity.getClosePrice(), endIndex,
//						currencyTradingRecordEndIndex);
				result = ENTER;

			} else if (currencyTradingRecord.getCurrentTrade().isOpened()
					&& strategy.shouldExit(endIndex, currencyTradingRecord)) {

//				currencyTradingRecord.exit(currencyTradingRecordEndIndex, lastBar.getClosePrice(), Decimal.ONE);
//				log.debug("EXIT -> strategy {} currency: {} price: {} indexes s/c: {}/{}", strategy.getName(),
//						tradeEntity.getTradeId().getCurrency(), tradeEntity.getClosePrice(), endIndex,
//						currencyTradingRecordEndIndex);
				result = EXIT;
			}

		} else {
			log.trace("no bar added");
		}

		strategyResult = StrategyResult.builder().result(result).strategyName(beanName).barDuration(barDuration)
				.tradeEntity(tradeEntity).closePrice(tradeEntity.getClosePrice())
				.timeSeriesEndIndex(currencyTradingRecordEndIndex)
				.highPrice(highPrice.setScale(NumericFunctions.PRICE_SCALE, NumericFunctions.ROUNDING_MODE))
				.lowPrice(lowPrice.setScale(NumericFunctions.PRICE_SCALE, NumericFunctions.ROUNDING_MODE))
				.balance(balance.setScale(NumericFunctions.BALANCE_SCALE, NumericFunctions.ROUNDING_MODE))
				.amount(amount.setScale(NumericFunctions.AMOUNT_SCALE, NumericFunctions.ROUNDING_MODE))
				.feeValue(feeValue).build();

		log.trace("done. strategyResult: {}", strategyResult);
		return strategyResult;
	}

	private void debug(TradeEntity tradeEntity) {
		int endIndex = timeSeries.getEndIndex();
		ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);
		TripleEMAIndicator tema = new TripleEMAIndicator(closePrice,
				jsonFiles.getStrategiesJsonMap().get(beanName).getTimeFrameLong().intValue());
		DoubleEMAIndicator dema = new DoubleEMAIndicator(closePrice,
				jsonFiles.getStrategiesJsonMap().get(beanName).getTimeFrameShort().intValue());

		log.trace("index {} tema: {} dema: {} closePrice: {}/{}", timeSeries.getEndIndex(),
				NumericFunctions.convertToBigDecimal(tema.getValue(endIndex), 2),
				NumericFunctions.convertToBigDecimal(dema.getValue(endIndex), 2), tradeEntity.getClosePrice(),
				tradeEntity.getTimestamp());
	}
}
