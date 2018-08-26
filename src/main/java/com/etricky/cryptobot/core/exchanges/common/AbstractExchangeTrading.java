package com.etricky.cryptobot.core.exchanges.common;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;

import org.ta4j.core.TimeSeries;
import org.ta4j.core.TradingRecord;

import com.etricky.cryptobot.core.common.threads.ThreadInfo;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeExceptionRT;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.StrategyResult;
import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;
import com.etricky.cryptobot.core.strategies.common.TimeSeriesHelper;
import com.etricky.cryptobot.model.TradeEntity;

import info.bitrich.xchangestream.core.StreamingExchange;
import io.reactivex.disposables.Disposable;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractExchangeTrading extends AbstractExchange implements Runnable {

	public static final int TRADE_ALL = 0;
	public static final int TRADE_HISTORY = 1;
	public static final int TRADE_LIVE = 2;
	public static final String PROPERTY_TIME_SERIES = "timeSeries";
	public static final String PROPERTY_TRADING_RECORD = "tradingRecord";

	protected Disposable subscription;
	protected StreamingExchange streamingExchange;
	protected int tradeType;

	private PropertyChangeSupport liveTradeProperty;
	private TimeSeriesHelper timeSeriesHelper;
	private StrategyResult strategyResult, auxStrategyResult;
	private int lowestBar = 0;
	@Getter
	private HashMap<String, AbstractStrategy> strategiesMap;
	@Getter
	protected TradingRecord currencyTradingRecord;
	@Getter
	protected TimeSeries currencyTimeSeries;
	@Getter
	protected int barDuration = 0;

	@Getter
	protected CurrencyEnum currencyEnum;

	public AbstractExchangeTrading(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles,
			TimeSeriesHelper timeSeriesHelper) {
		super(exchangeThreads, commands, jsonFiles);
		this.timeSeriesHelper = timeSeriesHelper;
	}

	public void initialize(ExchangeEnum exchangeEnum, CurrencyEnum currencyEnum, int tradeType, ThreadInfo threadInfo) {
		log.debug("start. exchange: {} currency: {} tradeType: {} threadInfo: {}", exchangeEnum, currencyEnum,
				tradeType, threadInfo);

		this.currencyEnum = currencyEnum;
		this.exchangeEnum = exchangeEnum;
		this.tradeType = tradeType;
		this.threadInfo = threadInfo;
		liveTradeProperty = new PropertyChangeSupport(this);
		strategiesMap = new HashMap<>();

		log.debug("done");
	}

	public void addStrategy(String strategyBean, AbstractStrategy abstractStrategy) {
		log.debug("start. strategy: {}", strategyBean);

		strategiesMap.putIfAbsent(strategyBean, abstractStrategy);

		if (barDuration < abstractStrategy.getBarDuration()) {
			barDuration = abstractStrategy.getBarDuration();
		}

		log.debug("done. barDuration: {}", barDuration);
	}

	@Override
	protected void exchangeDisconnect() {
		log.debug("start");

		try {
			if (subscription != null && !subscription.isDisposed()) {
				subscription.dispose();
				log.debug("subscription disposed");
			}

			if (streamingExchange != null && streamingExchange.isAlive()) {
				log.debug("disconnect from exchange");
				// Disconnect from exchange (non-blocking)
				streamingExchange.disconnect().subscribe(() -> log.debug("Disconnected from exchange: {} currency: {}",
						exchangeEnum.getName(), currencyEnum.getShortName()));
			} else {
				log.debug("exchange is not alive!");
			}
		} catch (Exception e) {
			log.error("Exception: {}", e);
		}

		exchangeThreads.stopExchangeThreads(exchangeEnum.getName());

		commands.sendMessage(
				"Stopped trading " + currencyEnum.getShortName() + " for exchange: " + exchangeEnum.getTradingBean(),
				true);

		log.debug("done");
	}

	/*
	 * Methods for the trading strategies
	 * 
	 */
	// used for historic trades
	public void addTradeToTimeSeries(TradeEntity tradeEntity) {
		log.trace("start");

		strategiesMap.values().forEach(strategy -> {
			try {
				strategy.addTradeToStrategyTimeSeries(tradeEntity);

				timeSeriesHelper.addTradeToTimeSeries(currencyTimeSeries, tradeEntity, barDuration);

				addTradeToCurrencyTimeSeries(tradeEntity, strategy);
			} catch (ExchangeException e) {
				log.error("Exception: {}", e);
				throw new ExchangeExceptionRT(e);
			}
		});

		log.trace("done");
	}

	private void addTradeToCurrencyTimeSeries(TradeEntity tradeEntity, AbstractStrategy strategy)
			throws ExchangeException {
		if (strategy.getBarDuration() == barDuration) {
			timeSeriesHelper.addTradeToTimeSeries(currencyTimeSeries, tradeEntity, barDuration);

			liveTradeProperty.firePropertyChange(PROPERTY_TIME_SERIES, null,
					StrategyResult.builder().barDuration(barDuration).tradeEntity(tradeEntity).build());
		}
	}

	public void processStrategiesForLiveTrade(TradeEntity tradeEntity) {
		processStrategiesForLiveTrade(tradeEntity, false);
	}

	public StrategyResult processStrategiesForLiveTrade(TradeEntity tradeEntity, boolean backtest) {
		lowestBar = 0;
		strategyResult = null;
		log.trace("start. backtest: {}", backtest);

		strategiesMap.values().forEach(strategy -> {
			try {
				addTradeToCurrencyTimeSeries(tradeEntity, strategy);

				auxStrategyResult = strategy.processStrategy(tradeEntity, currencyTradingRecord);
			} catch (ExchangeException e) {
				log.error("Exception: {}", e);
				throw new ExchangeExceptionRT(e);
			}

			// as the strategy with highest cadence (lowest bar duration) responds quicker
			// to changes it has a higher priority over strategies with lowest cadence
			if (lowestBar < auxStrategyResult.getBarDuration()
					&& auxStrategyResult.getResult() != AbstractStrategy.NO_ACTION) {
				strategyResult = auxStrategyResult;
				lowestBar = auxStrategyResult.getBarDuration();
			}
		});

		// notifies exchangeTrade of the strategy executions
		if (!backtest && strategyResult.getResult() != AbstractStrategy.NO_ACTION) {
			liveTradeProperty.firePropertyChange(PROPERTY_TRADING_RECORD, null, strategyResult);
		}

		log.trace("done");
		return strategyResult;
	}

	/*
	 * Methods for the trading listeners
	 * 
	 */
	public void addListener(@NonNull PropertyChangeListener listener) {
		log.debug("start");

		liveTradeProperty.addPropertyChangeListener(currencyEnum.getShortName(), listener);

		log.debug("done");
	}
}
