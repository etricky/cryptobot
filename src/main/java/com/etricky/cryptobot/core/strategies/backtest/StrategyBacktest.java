package com.etricky.cryptobot.core.strategies.backtest;

import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.analysis.criteria.BuyAndHoldCriterion;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.analysis.criteria.VersusBuyAndHoldCriterion;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.common.threads.ThreadInfo;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeTrading;
import com.etricky.cryptobot.core.exchanges.common.ExchangeTrade;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeExceptionRT;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.StrategyResult;
import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;
import com.etricky.cryptobot.model.BacktestEntity;
import com.etricky.cryptobot.model.BacktestPK;
import com.etricky.cryptobot.model.TradeEntity;
import com.etricky.cryptobot.repositories.BacktestDataRepository;
import com.etricky.cryptobot.repositories.TradesData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
public class StrategyBacktest implements Runnable, UncaughtExceptionHandler {

	@Autowired
	protected JsonFiles jsonFiles;
	@Autowired
	private TradesData tradesData;
	@Autowired
	private Commands commands;
	@Autowired
	BacktestDataRepository backtestDataRepository;
	@Autowired
	private ApplicationContext appContext;

	private ArrayList<BacktestEntity> backtestEntityList = new ArrayList<>();
	private ArrayList<String> tradeCurrencies;
	private List<TradeEntity> dbTradesEntityList = new ArrayList<TradeEntity>();
	private Map<Integer, BacktestOrderInfo> backtestOrderInfoMap = new HashMap<>();
	private Map<String, Map<Integer, BacktestOrderInfo>> currencyBacktestOrderInfoMap = new HashMap<>();
	private Map<Integer, BacktestOrderInfo> tradeBacktestOrderInfoMap = new HashMap<>();

	private StrategyResult strategyResult;
	private ExchangeEnum exchangeEnum;
	private CurrencyEnum currencyEnum;
	private ExchangeTrade exchangeTrade;
	private BacktestMetaData backtestMetaData;
	private AnalysisCriterion criterion;

	private String tradeName, currencyName;
	private long startDateUnixTime, endDateUnixTime, backtestStart;
	private BigDecimal previousBalance = BigDecimal.ZERO;
	private Double criterionValue;

	public void initialize(ExchangeEnum exchangeEnum, String tradeName, long historyDays, ZonedDateTime startDate,
			ZonedDateTime endDate) {
		long auxHistoryDays;

		log.debug("start. exchangeEnum: {} tradeName: {} historyDays: {} startDate: {} endDate: {}", exchangeEnum,
				tradeName, historyDays, startDate, endDate);

		this.exchangeEnum = exchangeEnum;
		this.tradeName = tradeName;

		if (startDate == null) {
			if (historyDays == 0) {
				auxHistoryDays = jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getHistoryDays() * 86400;
			} else {
				auxHistoryDays = historyDays * 86400;
			}
			startDateUnixTime = DateFunctions.getUnixTimeNow() - auxHistoryDays;
			endDateUnixTime = DateFunctions.getUnixTimeNow();
		} else {
			startDateUnixTime = DateFunctions.getUnixTimeFromZDT(startDate);
			endDateUnixTime = DateFunctions.getUnixTimeFromZDT(endDate);
		}

		log.debug("done");
	}

	private void setExchangeTrade() {
		log.debug("start");

		exchangeTrade = (ExchangeTrade) appContext.getBean("exchangeTrade");

		// for each currencyPairs of the trade launch a trading bean
		tradeCurrencies = jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getTradeConfigsMap().get(tradeName)
				.getCurrencyPairs();

		tradeCurrencies.forEach(currencyPair -> {
			CurrencyEnum currencyEnum = CurrencyEnum.getInstanceByShortName(currencyPair).get();

			// gets a new exchange trading bean
			AbstractExchangeTrading abstractExchangeTrading = (AbstractExchangeTrading) appContext
					.getBean(exchangeEnum.getTradingBean());

			abstractExchangeTrading.initialize(exchangeEnum, currencyEnum, AbstractExchangeTrading.TRADE_LIVE,
					new ThreadInfo("backtest"));

			log.debug("create exchange trading bean for currencyPair: {}", currencyPair);

			exchangeTrade.addExchangeTrading(currencyEnum, abstractExchangeTrading);
		});

		log.debug("done");
	}

	private void executeStrategies() {

		// for each currency executes the strategies
		exchangeTrade.getExchangeCurrencyTradeMap().values().forEach(abstractTrading -> {
			currencyEnum = abstractTrading.getCurrencyEnum();
			currencyName = currencyEnum.getShortName();

			// load trades from BD for the currency
			dbTradesEntityList = tradesData.getTradesInPeriod(exchangeEnum.getName(), currencyName, startDateUnixTime,
					endDateUnixTime, true);

			if (dbTradesEntityList.size() != 0) {
				log.trace("for currency: {} got {} trades from database", currencyName, dbTradesEntityList.size());

				// creates a tradingRecord and timeSeries for the currency with the executed
				// strategies for each trade
				dbTradesEntityList.forEach(trade -> {

					strategyResult = abstractTrading.processStrategiesForLiveTrade(trade, false);

					if (strategyResult.getResult() != AbstractStrategy.NO_ACTION) {

						log.trace("adding new order for currency strategy: {}", strategyResult.getBeanName());

						BacktestOrderInfo backtestOrdersInfo = new BacktestOrderInfo(strategyResult);

						backtestOrdersInfo.setBalanceAndAmount(previousBalance, strategyResult.getFeePercentage());

						backtestOrderInfoMap.put(strategyResult.getTimeSeriesIndex(), backtestOrdersInfo);
						previousBalance = backtestOrdersInfo.getBalance();
						currencyBacktestOrderInfoMap.put(currencyName, backtestOrderInfoMap);

						// executes the exchangeTrade strategy
						strategyResult = exchangeTrade.executeTrade(strategyResult, true);

						backtestOrdersInfo = new BacktestOrderInfo(strategyResult);
						tradeBacktestOrderInfoMap.put(strategyResult.getTimeSeriesIndex(), backtestOrdersInfo);
					}

				});

			} else {
				log.error("no trades in database for currency: {}", currencyName);
				throw new ExchangeExceptionRT("Missing db trades for currency: " + currencyName);
			}
		});
	}

	private void processAndPrintBacktestData() {
		backtestMetaData = new BacktestMetaData();

		currencyBacktestOrderInfoMap.forEach((currName, orderInfoMap) -> {

			orderInfoMap.values().forEach(orderInfo -> {

				if (currencyName == null || !currencyName.equals(currName)) {
					currencyName = currName;
					log.info("-----------");
					log.info("CURRENCY ORDERS: {}", currencyName);
					log.info("-----------");
				}

				backtestMetaData.calculateMetaData(orderInfo);

				// stores the backtest data in the database
				// the unixtime is the time the backtest ran so it can store multiple runs
				backtestEntityList.add(BacktestEntity.builder()
						.orderId(BacktestPK.builder().currency(currencyName).exchange(exchangeEnum.getName())
								.unixtime(backtestStart)
								.index(BigDecimal.valueOf(orderInfo.getStrategyResult().getTimeSeriesIndex())).build())
						.orderType(orderInfo.getStrategyResult().getLastEntry().getType())
						.timestamp(orderInfo.getStrategyResult().getTradeEntity().getTimestamp())
						.orderUnixTime(BigDecimal
								.valueOf(orderInfo.getStrategyResult().getTradeEntity().getTradeId().getUnixtime()))
						.feeValue(orderInfo.getFeeValue())
						.deltaAmount(orderInfo.getAmount().subtract(backtestMetaData.getPreviousOrderAmount()))
						.amount(orderInfo.getAmount())
						.deltaBalance(orderInfo.getBalance().subtract(backtestMetaData.getPreviousOrderBalance()))
						.balance(orderInfo.getBalance()).closePrice(orderInfo.getStrategyResult().getClosePrice())
						.highPrice(orderInfo.getStrategyResult().getHighPrice())
						.lowPrice(orderInfo.getStrategyResult().getLowPrice())
						.strategy(orderInfo.getStrategyResult().getBeanName()).build());

				orderInfo.printOrder(backtestMetaData);
			});
		});

		log.info("-----------");
		log.info("TRADE ORDERS: {}", currencyName);
		log.info("-----------");

		tradeBacktestOrderInfoMap.values().forEach(orderInfo -> {

			backtestMetaData.calculateMetaData(orderInfo);

			// stores the backtest data in the database
			// the unixtime is the time the backtest ran so it can store multiple runs
			backtestEntityList.add(BacktestEntity.builder()
					.orderId(BacktestPK.builder().currency(exchangeTrade.getTradeName())
							.exchange(exchangeEnum.getName()).unixtime(backtestStart)
							.index(BigDecimal.valueOf(orderInfo.getStrategyResult().getTimeSeriesIndex())).build())
					.orderType(orderInfo.getStrategyResult().getLastEntry().getType())
					.timestamp(orderInfo.getStrategyResult().getTradeEntity().getTimestamp())
					.orderUnixTime(BigDecimal
							.valueOf(orderInfo.getStrategyResult().getTradeEntity().getTradeId().getUnixtime()))
					.feeValue(orderInfo.getFeeValue())
					.deltaAmount(orderInfo.getAmount().subtract(backtestMetaData.getPreviousOrderAmount()))
					.amount(orderInfo.getAmount())
					.deltaBalance(orderInfo.getBalance().subtract(backtestMetaData.getPreviousOrderBalance()))
					.balance(orderInfo.getBalance()).closePrice(orderInfo.getStrategyResult().getClosePrice())
					.highPrice(orderInfo.getStrategyResult().getHighPrice())
					.lowPrice(orderInfo.getStrategyResult().getLowPrice())
					.strategy(orderInfo.getStrategyResult().getBeanName()).build());

			orderInfo.printOrder(backtestMetaData);
		});

		backtestDataRepository.saveAll(backtestEntityList);
	}

	private void evaluation() {

		exchangeTrade.getExchangeCurrencyTradeMap().forEach((currName, abstractTrading) -> {
			log.info("-----------");
			log.info("CURRENCY PROFIT: {}", currName);
			log.info("-----------");

			// set and run analysis criteria
			criterion = new TotalProfitCriterion();
			criterionValue = criterion.calculate(abstractTrading.getCurrencyTimeSeries(),
					abstractTrading.getCurrencyTradingRecord());
			log.info("criteria: TotalProfitCriterion value: {}", criterionValue);

			criterion = new BuyAndHoldCriterion();
			criterionValue = criterion.calculate(abstractTrading.getCurrencyTimeSeries(),
					abstractTrading.getCurrencyTradingRecord());
			log.info("criteria: BuyAndHoldCriterion value: {}", criterionValue);

			criterion = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
			criterionValue = criterion.calculate(abstractTrading.getCurrencyTimeSeries(),
					abstractTrading.getCurrencyTradingRecord());
			log.info("criteria: VersusBuyAndHoldCriterion value: {}", criterionValue);
		});

		log.info("-----------");
		log.info("TRADE PROFIT: {}", exchangeTrade.getTradeName());
		log.info("-----------");

		// set and run analysis criteria
		criterion = new TotalProfitCriterion();
		criterionValue = criterion.calculate(exchangeTrade.getTradeTimeSeries(), exchangeTrade.getTradeTradingRecord());
		log.info("criteria: TotalProfitCriterion value: {}", criterionValue);

		criterion = new BuyAndHoldCriterion();
		criterionValue = criterion.calculate(exchangeTrade.getTradeTimeSeries(), exchangeTrade.getTradeTradingRecord());
		log.info("criteria: BuyAndHoldCriterion value: {}", criterionValue);

		criterion = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
		criterionValue = criterion.calculate(exchangeTrade.getTradeTimeSeries(), exchangeTrade.getTradeTradingRecord());
		log.info("criteria: VersusBuyAndHoldCriterion value: {}", criterionValue);

	}

	public void backTest() {

		log.debug("start");

		backtestStart = DateFunctions.getUnixTimeNow();

		setExchangeTrade();

		executeStrategies();

		processAndPrintBacktestData();

		evaluation();

		commands.sendMessage("Ended backtest for exchange: " + exchangeEnum.getName() + " tradeName: " + tradeName,
				true);
		log.debug("done");
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

		Thread.currentThread().setName("B_" + exchangeEnum.getName() + "_" + tradeName);
		backTest();

		log.debug("done");
	}
}
