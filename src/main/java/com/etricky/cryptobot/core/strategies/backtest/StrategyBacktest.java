package com.etricky.cryptobot.core.strategies.backtest;

import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Order.OrderType;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.criteria.BuyAndHoldCriterion;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.analysis.criteria.VersusBuyAndHoldCriterion;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.exchanges.common.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.ExchangeExceptionRT;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.TradingStrategy;
import com.etricky.cryptobot.core.strategies.TrailingStopLossStrategy;
import com.etricky.cryptobot.core.strategies.backtest.BacktestOrdersInfo.BacktestData;
import com.etricky.cryptobot.core.strategies.common.ExchangeStrategy;
import com.etricky.cryptobot.model.TradesEntity;
import com.etricky.cryptobot.repositories.TradesData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
public class StrategyBacktest implements Runnable, UncaughtExceptionHandler {
	public final static int STRATEGY_ALL = 0;
	public final static int STRATEGY_STOP_LOSS = 1;
	public final static int STRATEGY_TRADING = 2;

	@Autowired
	protected JsonFiles jsonFiles;
	@Autowired
	private TradesData tradesData;
	@Autowired
	private ExchangeStrategy exchangeStrategy;
	@Autowired
	private Commands commands;

	private ExchangeEnum exchangeEnum;
	private CurrencyEnum currencyEnum;
	private long historyDays;
	private int choosedStrategies, posOrders = 0, negOrders = 0, totalOrders = 0, tradingBuys = 0, tradingSells = 0, stopLossBuys = 0,
			stopLossSells = 0;
	private ZonedDateTime startDate, endDate;
	private BigDecimal firstOrderPrice = BigDecimal.ZERO, firstOrderAmount = BigDecimal.ZERO, firstOrderBalance = BigDecimal.ZERO,
			previousOrderPrice = BigDecimal.ZERO, previousOrderAmount = BigDecimal.ZERO, previousOrderBalance = BigDecimal.ZERO,
			totalFees = BigDecimal.ZERO;

	public void initialize(ExchangeEnum exchangeEnum, CurrencyEnum currencyEnum, long historyDays, int choosedStrategies, ZonedDateTime startDate,
			ZonedDateTime endDate) {
		log.debug("start. exchangeEnum: {} currencyEnum: {} historyDays: {} startDate: {} endDate: {}", exchangeEnum, currencyEnum, historyDays,
				startDate, endDate);

		this.exchangeEnum = exchangeEnum;
		this.currencyEnum = currencyEnum;
		this.historyDays = historyDays;
		this.choosedStrategies = choosedStrategies;
		this.startDate = startDate;
		this.endDate = endDate;

		log.debug("done");
	}

	public void backTest() {
		List<TradesEntity> tradesEntityList = new ArrayList<TradesEntity>();
		TradingRecord tradingRecord = new BaseTradingRecord();
		TimeSeries timeSeries = new BaseTimeSeries("backtest");
		long startDateUnixTime, endDateUnixTime;
		Double value;
		TreeMap<Long, BacktestOrdersInfo> backtestOrderInfoMap = new TreeMap<>();

		log.debug("start. exchangeEnum: {} currencyEnum: {}", exchangeEnum, currencyEnum);

		exchangeStrategy.initializeStrategies(exchangeEnum, currencyEnum);

		if (startDate == null) {
			if (historyDays == 0) {
				historyDays = jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getHistoryDays() * 86400;
			} else {
				historyDays = historyDays * 86400;
			}
			startDateUnixTime = DateFunctions.getUnixTimeNow() - historyDays;
			endDateUnixTime = DateFunctions.getUnixTimeNow();
		} else {
			startDateUnixTime = DateFunctions.getUnixTimeFromZDT(startDate);
			endDateUnixTime = DateFunctions.getUnixTimeFromZDT(endDate);
		}

		// load trades from BD
		tradesEntityList = tradesData.getTradesInPeriod(exchangeEnum.getName(), currencyEnum.getShortName(), startDateUnixTime, endDateUnixTime,
				false);

		if (tradesEntityList.size() != 0) {
			log.debug("got {} trades from database", tradesEntityList.size());

			// create tradingRecord
			tradesEntityList.forEach(trade -> {
				try {
					exchangeStrategy.processStrategyForLiveTrade(trade, tradingRecord, timeSeries, true, choosedStrategies, backtestOrderInfoMap);
				} catch (ExchangeException e) {
					log.error("Exception: {}", e);
					throw new ExchangeExceptionRT(e);
				}
			});

			backtestOrderInfoMap.forEach((index, info) -> {

				if (firstOrderAmount == BigDecimal.ZERO) {
					firstOrderAmount = info.getAmount();
					firstOrderPrice = info.getClosePrice();
					firstOrderBalance = info.getBalance();
				}

				BacktestData backtestData = info.calculateAndprintOrder(index, firstOrderPrice, firstOrderAmount, firstOrderBalance,
						previousOrderPrice, previousOrderAmount, previousOrderBalance);

				totalFees = totalFees.add(info.getFeeValue());

				if (backtestData.getOrderResult() < 0) {
					negOrders++;
				} else {
					posOrders++;
				}

				if (backtestData.getStrategyDone().equals(TradingStrategy.STRATEGY_NAME)) {
					if (backtestData.getOrderType().equals(OrderType.BUY)) {
						tradingBuys++;
					} else {
						tradingSells++;
					}
				} else if (backtestData.getStrategyDone().equals(TrailingStopLossStrategy.STRATEGY_NAME)) {
					if (backtestData.getOrderType().equals(OrderType.BUY)) {
						stopLossBuys++;
					} else {
						stopLossSells++;
					}
				}
				previousOrderPrice = info.getClosePrice();
				previousOrderAmount = info.getAmount();
				previousOrderBalance = info.getBalance();
				totalOrders++;
			});

			log.info("totalOrders: {}, positive: {}, negative: {} fees: {}", totalOrders, posOrders, negOrders,
					totalFees.setScale(BacktestOrdersInfo.FEE_SCALE));
			log.info("strategy: {}, buys: {} sells: {}", TradingStrategy.STRATEGY_NAME, tradingBuys, tradingSells);
			log.info("strategy: {}, buys: {} sells: {}", TrailingStopLossStrategy.STRATEGY_NAME, stopLossBuys, stopLossSells);
			// set and run analysis criteria
			AnalysisCriterion criterion = new TotalProfitCriterion();
			value = criterion.calculate(timeSeries, tradingRecord);
			log.info("criteria: TotalProfitCriterion value: {}", value);

			criterion = new BuyAndHoldCriterion();
			value = criterion.calculate(timeSeries, tradingRecord);
			log.info("criteria: BuyAndHoldCriterion value: {}", value);

			criterion = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
			value = criterion.calculate(timeSeries, tradingRecord);
			log.info("criteria: VersusBuyAndHoldCriterion value: {}", value);
			commands.sendMessage("Ended backtest for exchange: " + exchangeEnum.getName() + " currency: " + currencyEnum.getShortName(), true);
		} else {
			log.debug("no trades in database");
		}
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.error("start. exception on thread:{}", t.getName());
		log.error("exception: {}", e);

		// sends the interrupt to itself
		if (t.isAlive() || !t.isInterrupted()) {
			log.debug("sending interrupt");
			t.interrupt();
		}

		log.debug("done");
	}

	@Override
	public void run() {
		log.debug("start");

		Thread.currentThread().setUncaughtExceptionHandler(this);

		Thread.currentThread().setName("B_" + exchangeEnum.getName() + "_" + currencyEnum.getShortName());
		backTest();

		log.debug("done");
	}
}
