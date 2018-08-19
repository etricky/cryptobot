package com.etricky.cryptobot.core.strategies.common;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Decimal;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TradingRecord;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.exchanges.common.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.ExchangeExceptionRT;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.TradingStrategy;
import com.etricky.cryptobot.core.strategies.TrailingStopLossStrategy;
import com.etricky.cryptobot.core.strategies.backtest.BacktestOrdersInfo;
import com.etricky.cryptobot.core.strategies.backtest.StrategyBacktest;
import com.etricky.cryptobot.model.ExchangePK;
import com.etricky.cryptobot.model.OrderEntityType;
import com.etricky.cryptobot.model.OrdersEntity;
import com.etricky.cryptobot.model.TradesEntity;
import com.etricky.cryptobot.repositories.OrdersEntityRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Scope("prototype")
@Slf4j
public class ExchangeStrategy {

	@Autowired
	protected JsonFiles jsonFiles;
	@Autowired
	private ApplicationContext appContext;
	@Autowired
	protected OrdersEntityRepository ordersEntityRepository;
	@Autowired
	TimeSeriesHelper timeSeriesHelper;

	private HashMap<String, AbstractStrategy> strategiesMap;
	private ExchangeEnum exchangeEnum;
	private CurrencyEnum currencyEnum;
	private TimeSeries exchangeTimeSeries;
	private TradingRecord exchangeTradingRecord;
	private int timeSeriesBar = 0, exchangeBarDuration = 0;
	private TradesEntity lastTradesEntity;
	private BigDecimal highPrice = BigDecimal.ZERO, lowPrice = BigDecimal.ZERO, feePercentage = BigDecimal.ZERO;
	private Decimal lastOrderBalance = Decimal.ZERO;

	public void initializeStrategies(ExchangeEnum exchangeEnum, CurrencyEnum currencyEnum) {
		log.debug("start");

		exchangeTradingRecord = new BaseTradingRecord();
		strategiesMap = new HashMap<String, AbstractStrategy>();

		feePercentage = jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getFee().divide(BigDecimal.valueOf(100));
		jsonFiles.getStrategiesJson().forEach((bean, strategySettings) -> {
			log.debug("creating bean: {} for exchange: {} currency: {}", bean, exchangeEnum.getName(), currencyEnum.getShortName());

			this.exchangeEnum = exchangeEnum;
			this.currencyEnum = currencyEnum;

			AbstractStrategy abstractStrategy = (AbstractStrategy) appContext.getBean(bean);
			abstractStrategy.initializeStrategy(exchangeEnum, bean);

			strategiesMap.put(bean, abstractStrategy);

			if (strategySettings.getTimeSeriesBars().intValue() > timeSeriesBar) {
				timeSeriesBar = strategySettings.getTimeSeriesBars().intValue();
				exchangeBarDuration = strategySettings.getBarDurationSec().intValue();
			}

		});

		log.debug("timeSeriesBar: {} barDuration: {}", timeSeriesBar, exchangeBarDuration);

		exchangeTimeSeries = new BaseTimeSeries(exchangeEnum.getName());
		exchangeTimeSeries.setMaximumBarCount(timeSeriesBar);

		log.debug("done");
	}

	public void addHistoryTradeToTimeSeries(TradesEntity tradesEntity) throws ExchangeException {
		log.trace("start. tradesEntity: {}", tradesEntity);

		// adds bar to exchange time series
		timeSeriesHelper.addTradeToTimeSeries(exchangeTimeSeries, tradesEntity, exchangeBarDuration);

		strategiesMap.forEach((bean, strat) -> {
			try {
				strat.addTradeToTimeSeries(tradesEntity);
			} catch (ExchangeException e1) {
				log.error("Exception: {}", e1);
				log.error("trade: {} lastTradesEntity: {}", tradesEntity, lastTradesEntity);
				throw new ExchangeExceptionRT(e1);
			}
		});

		lastTradesEntity = tradesEntity;
		log.trace("done");
	}

	public void processStrategyForLiveTrade(TradesEntity tradesEntity) throws ExchangeException {
		processStrategyForLiveTrade(tradesEntity, exchangeTradingRecord, exchangeTimeSeries, false, StrategyBacktest.STRATEGY_ALL, null);
	}

	public void processStrategyForLiveTrade(TradesEntity tradesEntity, TradingRecord tradingRecord, TimeSeries timeSeries, boolean backtest,
			int choosedStrategies, TreeMap<Long, BacktestOrdersInfo> backtestInfoMap) throws ExchangeException {
		String logAux = null, auxStrategy = null;
		OrderEntityType orderType = null;
		int finalResult = AbstractStrategy.NO_ACTION, auxResult = AbstractStrategy.NO_ACTION, lowestBar = 0;
		Decimal amount = Decimal.ZERO, balance = Decimal.ZERO, feeValue = Decimal.ZERO;

		log.trace("start. backtest: {} choosedStrategies: {}", backtest, choosedStrategies);

		timeSeriesHelper.addTradeToTimeSeries(timeSeries, tradesEntity, exchangeBarDuration);

		for (AbstractStrategy abstractStrategy : strategiesMap.values()) {

			if (choosedStrategies == StrategyBacktest.STRATEGY_ALL
					|| (choosedStrategies == StrategyBacktest.STRATEGY_TRADING
							&& abstractStrategy.getBeanName().equalsIgnoreCase(TradingStrategy.STRATEGY_NAME))
					|| (choosedStrategies == StrategyBacktest.STRATEGY_STOP_LOSS
							&& abstractStrategy.getBeanName().equalsIgnoreCase(TrailingStopLossStrategy.STRATEGY_NAME))) {

				auxResult = abstractStrategy.processStrategyForLiveTrade(tradesEntity, tradingRecord, timeSeries.getEndIndex());

				// as the strategy with highest cadence (lowest bar duration) responds quicker
				// to changes it has a higher priority over strategies with lowest cadence
				if (lowestBar < abstractStrategy.getBarDuration() && auxResult != AbstractStrategy.NO_ACTION) {
					finalResult = auxResult;
					auxStrategy = abstractStrategy.getBeanName();
					lowestBar = abstractStrategy.getBarDuration();

					log.debug("strategy: {} result: {}", auxStrategy, auxResult);
				}
			} else {
				log.trace("skipping strategy: {}", abstractStrategy.getBeanName());
			}
		}

		log.trace("finalResult: {}", finalResult);

		if (backtest) {
			setHighPrice(tradesEntity.getClosePrice(), false);
			setLowPrice(tradesEntity.getClosePrice(), false);
		}

		if (finalResult != AbstractStrategy.NO_ACTION) {

			int endIndex = timeSeries.getEndIndex();
			Bar lastBar = timeSeries.getLastBar();

			if (finalResult == AbstractStrategy.ENTER) {
				// first trade of the backtest
				if (backtest) {
					if (tradingRecord.getLastExit() == null) {
						balance = Decimal.valueOf(100);
					} else {
						balance = Decimal.valueOf(backtestInfoMap.lastEntry().getValue().getBalance());
					}

					feeValue = balance.multipliedBy(feePercentage);
					// available balance to be used in the buy
					balance = balance.minus(feeValue);
					amount = balance.dividedBy(lastBar.getClosePrice());

				} else {
					// TODO amount should correspond to the amount in the exchange divided by the
					// current price
				}

				if (tradingRecord.enter(endIndex, lastBar.getClosePrice(), amount)) {
					logAux = "Enter";
					orderType = OrderEntityType.BUY;

				} else {
					log.warn("trading record not updated on ENTER");
					finalResult = AbstractStrategy.NO_ACTION;
				}
			}

			if (finalResult == AbstractStrategy.EXIT) {
				if (backtest) {
					amount = tradingRecord.getLastEntry().getAmount();
					balance = amount.multipliedBy(lastBar.getClosePrice());
					feeValue = balance.multipliedBy(feePercentage);
					balance = balance.minus(feeValue);
				} else {
					// TODO amount should correspond to the amount in the exchange divided by the
					// current price
				}

				if (tradingRecord.exit(endIndex, lastBar.getClosePrice(), amount)) {
					logAux = "Exit";
					orderType = OrderEntityType.SELL;

				} else {
					log.warn("trading record not updated on EXIT");
					finalResult = AbstractStrategy.NO_ACTION;
				}
			}

			if (finalResult != AbstractStrategy.NO_ACTION) {
				log.info("executed order :: strategy: {} order: {} on index: {} price: {} amount: {} balance: {} delta: {} fee: {}", auxStrategy,
						logAux, endIndex, lastBar.getClosePrice(), NumericFunctions.convertToBigDecimal(amount, NumericFunctions.AMOUNT_SCALE),
						NumericFunctions.convertToBigDecimal(balance, NumericFunctions.BALANCE_SCALE),
						NumericFunctions.convertToBigDecimal(balance.minus(lastOrderBalance), NumericFunctions.BALANCE_SCALE),
						NumericFunctions.convertToBigDecimal(feeValue, NumericFunctions.FEE_SCALE));

				if (!backtest) {
					// TODO order should only be stored after it has been completed in the exchange
					// TODO send slack message
					ordersEntityRepository.save(OrdersEntity.builder()
							.orderId(ExchangePK.builder().currency(currencyEnum.getShortName()).exchange(exchangeEnum.getName())
									.unixtime(tradesEntity.getTradeId().getUnixtime()).build())
							.index(BigDecimal.valueOf(endIndex)).orderType(orderType).price(BigDecimal.valueOf(lastBar.getClosePrice().doubleValue()))
							.timestamp(tradesEntity.getTimestamp()).amount(BigDecimal.valueOf(Decimal.ONE.longValue())).build());
				} else {
					backtestInfoMap.put(Long.valueOf(endIndex),
							new BacktestOrdersInfo(auxStrategy, tradesEntity, tradingRecord.getLastOrder(), highPrice, lowPrice,
									NumericFunctions.convertToBigDecimal(lastBar.getClosePrice(), NumericFunctions.PRICE_SCALE),
									NumericFunctions.convertToBigDecimal(feeValue, NumericFunctions.FEE_SCALE),
									NumericFunctions.convertToBigDecimal(balance, NumericFunctions.BALANCE_SCALE),
									NumericFunctions.convertToBigDecimal(amount, NumericFunctions.AMOUNT_SCALE)));
					// resets the high/low price after each order
					setHighPrice(tradesEntity.getClosePrice(), true);
					setLowPrice(tradesEntity.getClosePrice(), true);
					lastOrderBalance = balance;
				}
			}
		} else {
			log.trace("no order occurred");
		}

		log.trace("done");
	}

	private void setHighPrice(BigDecimal value, boolean force) {
		if (force || highPrice.compareTo(value) < 0) {
			highPrice = value;
		}
	}

	private void setLowPrice(BigDecimal value, boolean force) {
		if (force || lowPrice.compareTo(value) > 0) {
			lowPrice = value;
		}
	}
}
