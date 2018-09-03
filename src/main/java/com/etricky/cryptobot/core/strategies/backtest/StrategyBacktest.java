package com.etricky.cryptobot.core.strategies.backtest;

import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.analysis.criteria.BuyAndHoldCriterion;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.analysis.criteria.VersusBuyAndHoldCriterion;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.common.threads.ThreadInfo;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeOrders;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeTrading;
import com.etricky.cryptobot.core.exchanges.common.ExchangeTrade;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeExceptionRT;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.StrategyResult;
import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;
import com.etricky.cryptobot.model.BacktestEntity;
import com.etricky.cryptobot.model.BacktestResultsEntity;
import com.etricky.cryptobot.model.TradeEntity;
import com.etricky.cryptobot.model.TradesData;
import com.etricky.cryptobot.model.primaryKeys.BacktestPK;
import com.etricky.cryptobot.repositories.BacktestRepository;
import com.etricky.cryptobot.repositories.BacktestResultsRepository;

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
	BacktestRepository backtestRepository;
	@Autowired
	BacktestResultsRepository backtestResultsRepository;
	@Autowired
	private ApplicationContext appContext;

	private List<BacktestEntity> backtestEntityList = new ArrayList<>();
	private Map<String, BacktestResultsEntity> backtestResultsMap = new HashMap<>();
	private List<String> tradeCurrencies;
	private List<TradeEntity> dbTradesEntityList = new ArrayList<TradeEntity>();

	private List<BacktestOrderInfo> currencyOrderInfoList = new ArrayList<>();
	private List<BacktestOrderInfo> currencyOrderInfoListAux = new ArrayList<>();
	private Map<String, List<BacktestOrderInfo>> backtestCurrencyOrderInfoMap = new TreeMap<>();

	private List<BacktestOrderInfo> tradeOrderInfoList = new ArrayList<>();

	private Map<ZonedDateTime, BacktestOrderInfo> timeBasedCurrencyOrderInfoMap = new TreeMap<>();

	private ExchangeEnum exchangeEnum;
	private CurrencyEnum currencyEnum;
	private ExchangeTrade exchangeTrade;
	private BacktestMetaData backtestMetaData;
	private AnalysisCriterion criterion;

	private String tradeName, currencyName, auxCurrencyName;
	private final String TRADE_KEY = "trade_key";
	private long startDateUnixTime, endDateUnixTime, backtestStart;
	private Double criterionValue;
	private long counter, backtestResultsIndex = 0;

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

		AbstractExchangeOrders abstractExchangeOrders = (AbstractExchangeOrders) appContext
				.getBean(exchangeEnum.getOrdersBean());

		ThreadInfo threadInfo = new ThreadInfo(
				ExchangeThreads.getThreadName(exchangeEnum.getName(), tradeName, ExchangeThreads.ORDERS_THREAD, null));
		abstractExchangeOrders.initialize(exchangeEnum, threadInfo);

		// TODO start orders/account thread

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

		exchangeTrade.initialize(tradeName, exchangeEnum, abstractExchangeOrders, true);

		log.debug("done");
	}

	private void executeStrategies() {
		log.debug("start");

		// for each currency executes the strategies
		exchangeTrade.getExchangeCurrencyTradeMap().values().forEach(abstractTrading -> {
			currencyEnum = abstractTrading.getCurrencyEnum();
			currencyName = currencyEnum.getShortName();

			// load trades from BD for the currency
			dbTradesEntityList = tradesData.getTradesInPeriod(exchangeEnum.getName(), currencyName, startDateUnixTime,
					endDateUnixTime, jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getAllowFakeTrades());

			if (dbTradesEntityList.size() != 0) {
				log.trace("for currency: {} got {} trades from database", currencyName, dbTradesEntityList.size());

				// for each trade gets and stores the orders for each currency and for trade
				dbTradesEntityList.forEach(trade -> {

					StrategyResult strategyResult = abstractTrading.processStrategiesForLiveTrade(trade, true);

					if (strategyResult.getResult() != AbstractStrategy.NO_ACTION) {

						log.trace("adding new currency order. result: {}", strategyResult.getResult());

						BacktestOrderInfo backtestOrderInfo = new BacktestOrderInfo(strategyResult);

						// this timeSeries index belongs to the currency
						currencyOrderInfoList.add(backtestOrderInfo);

						ZonedDateTime key = strategyResult.getTradeEntity().getTimestamp();

						// for the multicurrency scenario, there may be several orders being created for
						// the same time so the key must have an offset of a second so it can be stored.
						// The offset is calculated in a random manner to avoid that the order by which
						// the currencies are processed is the same every time
						while (timeBasedCurrencyOrderInfoMap.containsKey(key)) {
							if (new Random().nextInt(11) > 5) {
								key = key.plusSeconds(1);
							} else {
								key = key.minusSeconds(1);
							}
						}

						timeBasedCurrencyOrderInfoMap.put(key, backtestOrderInfo);
					}
				});

			} else {
				log.error("no trades in database for currency: {}", currencyName);
				throw new ExchangeExceptionRT("Missing db trades for currency: " + currencyName);
			}

			// for each currency, stores the orders
			currencyOrderInfoListAux.addAll(currencyOrderInfoList);
			backtestCurrencyOrderInfoMap.put(currencyName, currencyOrderInfoListAux);
			currencyOrderInfoList.clear();
		});

		log.debug("done with currencies, executing trade");

		// for each currency order result, calls the exchangeTrade for its result
		timeBasedCurrencyOrderInfoMap.keySet().forEach(zdt -> {

			StrategyResult strategyResult = exchangeTrade
					.executeTrade(timeBasedCurrencyOrderInfoMap.get(zdt).getStrategyResult());

			if (strategyResult.getResult() != AbstractStrategy.NO_ACTION) {
				log.trace("adding new trade order. result: {}", strategyResult.getResult());

				tradeOrderInfoList.add(new BacktestOrderInfo(strategyResult));
			}
		});

		log.debug("done");
	}

	private void storeBacktestResult(BacktestOrderInfo orderInfo, BacktestMetaData backtestMetaData, String key) {

		BacktestPK backtestPK = BacktestPK.builder().exchange(exchangeEnum.getName()).tradeName(tradeName)
				.runTime(backtestStart).index(backtestResultsIndex++).tradeData(key.equals(TRADE_KEY) ? true : false)
				.build();

		backtestResultsMap.put(key, BacktestResultsEntity.builder().backtestId(backtestPK)
				.currency(orderInfo.getStrategyResult().getTradeEntity().getTradeId().getCurrency())
				.initialBalance(backtestMetaData.getFirstOrderBalance())
				.initialAmount(backtestMetaData.getFirstOrderAmount())
				.initialPrice(backtestMetaData.getFirstOrderPrice())
				.finalBalance(orderInfo.getStrategyResult().getBalance())
				.finalAmount(orderInfo.getStrategyResult().getAmount())
				.finalPrice(orderInfo.getStrategyResult().getClosePrice())
				.posBalanceOrders(backtestMetaData.getPosBalanceOrders())
				.negBalanceOrders(backtestMetaData.getNegBalanceOrders())
				.posAmountOrders(backtestMetaData.getPosAmountOrders())
				.negAmountOrders(backtestMetaData.getNegAmountOrders()).totalOrders(backtestMetaData.getTotalOrders())
				.tradingBuys(backtestMetaData.getTradingBuys()).tradingSells(backtestMetaData.getTradingSells())
				.stopLossBuys(backtestMetaData.getStopLossBuys()).stopLossSells(backtestMetaData.getStopLossSells())
				.totalFees(backtestMetaData.getTotalFees()).build());
	}

	private void processAndPrintBacktestData() {
		log.debug("start");
		auxCurrencyName = null;

		backtestCurrencyOrderInfoMap.forEach((currName, orderInfoList) -> {
			backtestMetaData = new BacktestMetaData();

			orderInfoList.forEach((orderInfo) -> {
				if (auxCurrencyName == null || !auxCurrencyName.equals(currName)) {
					log.info("-----------");
					log.info("CURRENCY ORDERS: {}", currName);
					log.info("-----------");
					auxCurrencyName = currName;
				}
				backtestMetaData.calculateMetaData(orderInfo);

				// stores the backtest data for each currency in the database
				BacktestPK backtestPK = BacktestPK.builder().exchange(exchangeEnum.getName()).tradeName(tradeName)
						.runTime(backtestStart).index((long) orderInfo.getStrategyResult().getTimeSeriesEndIndex())
						.build();

				backtestEntityList.add(BacktestEntity.builder().orderId(backtestPK).currency(currName)
						.strategy(orderInfo.getStrategyResult().getStrategyName())
						.ordertime(orderInfo.getStrategyResult().getTradeEntity().getTimestamp())
						.orderUnixTime(orderInfo.getStrategyResult().getTradeEntity().getTradeId().getUnixtime())
						.orderType(orderInfo.getStrategyResult().getLastEntry().getType())
						.closePrice(orderInfo.getStrategyResult().getClosePrice())
						.highPrice(orderInfo.getStrategyResult().getHighPrice())
						.lowPrice(orderInfo.getStrategyResult().getLowPrice())
						.amount(orderInfo.getStrategyResult().getAmount())
						.deltaAmount(NumericFunctions.subtract(orderInfo.getStrategyResult().getAmount(),
								backtestMetaData.getPreviousOrderAmount(), NumericFunctions.AMOUNT_SCALE))
						.balance(orderInfo.getStrategyResult().getBalance())
						.deltaBalance(NumericFunctions.subtract(orderInfo.getStrategyResult().getBalance(),
								backtestMetaData.getPreviousOrderBalance(), NumericFunctions.BALANCE_SCALE))
						.feeValue(orderInfo.getStrategyResult().getFeeValue()).build());

				orderInfo.printOrder(backtestMetaData);
			});

			// stores the last order info for the currency
			storeBacktestResult(orderInfoList.get(orderInfoList.size() - 1), backtestMetaData, currName);
		});

		log.info("-----------");
		log.info("TRADE ORDERS: {}", tradeName);
		log.info("-----------");

		backtestMetaData = new BacktestMetaData();
		counter = 1;

		tradeOrderInfoList.forEach(orderInfo -> {

			backtestMetaData.calculateMetaData(orderInfo);

			// stores the backtest data in the database
			BacktestPK backtestPK = BacktestPK.builder().exchange(exchangeEnum.getName()).tradeName(tradeName)
					.runTime(backtestStart).index(counter++).tradeData(true).build();

			backtestEntityList.add(BacktestEntity.builder().orderId(backtestPK)
					.currency(orderInfo.getStrategyResult().getTradeEntity().getTradeId().getCurrency())
					.strategy(orderInfo.getStrategyResult().getStrategyName())
					.ordertime(orderInfo.getStrategyResult().getTradeEntity().getTimestamp())
					.orderUnixTime(orderInfo.getStrategyResult().getTradeEntity().getTradeId().getUnixtime())
					.orderType(orderInfo.getStrategyResult().getLastEntry().getType())
					.closePrice(orderInfo.getStrategyResult().getClosePrice())
					.highPrice(orderInfo.getStrategyResult().getHighPrice())
					.lowPrice(orderInfo.getStrategyResult().getLowPrice())
					.amount(orderInfo.getStrategyResult().getAmount())
					.deltaAmount(NumericFunctions.subtract(orderInfo.getStrategyResult().getAmount(),
							backtestMetaData.getPreviousOrderAmount(), NumericFunctions.AMOUNT_SCALE))
					.balance(orderInfo.getStrategyResult().getBalance())
					.deltaBalance(NumericFunctions.subtract(orderInfo.getStrategyResult().getBalance(),
							backtestMetaData.getPreviousOrderBalance(), NumericFunctions.BALANCE_SCALE))
					.feeValue(orderInfo.getStrategyResult().getFeeValue()).build());

			orderInfo.printOrder(backtestMetaData);
		});

		// stores the last order info for the trade
		storeBacktestResult(tradeOrderInfoList.get(tradeOrderInfoList.size() - 1), backtestMetaData, TRADE_KEY);

		backtestRepository.saveAll(backtestEntityList);

		log.debug("done");
	}

	private void evaluation() {
		log.debug("start");

		exchangeTrade.getExchangeCurrencyTradeMap().forEach((currName, abstractTrading) -> {
			log.info("-----------");
			log.info("CURRENCY PROFIT: {}", currName);
			log.info("-----------");

			backtestResultsMap.get(currName).printMetaData();

			// set and run analysis criteria
			criterion = new TotalProfitCriterion();
			criterionValue = criterion.calculate(abstractTrading.getCurrencyTimeSeries(),
					abstractTrading.getCurrencyTradingRecord());
			log.info("criteria: TotalProfitCriterion value: {}", criterionValue);
			backtestResultsMap.get(currName).setTotalProfit(BigDecimal.valueOf(criterionValue));

			criterion = new BuyAndHoldCriterion();
			criterionValue = criterion.calculate(abstractTrading.getCurrencyTimeSeries(),
					abstractTrading.getCurrencyTradingRecord());
			log.info("criteria: BuyAndHoldCriterion value: {}", criterionValue);
			backtestResultsMap.get(currName).setBuyAndHold(BigDecimal.valueOf(criterionValue));

			criterion = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
			criterionValue = criterion.calculate(abstractTrading.getCurrencyTimeSeries(),
					abstractTrading.getCurrencyTradingRecord());
			log.info("criteria: VersusBuyAndHoldCriterion value: {}", criterionValue);
			backtestResultsMap.get(currName).setVersusBuyAndHold(BigDecimal.valueOf(criterionValue));
		});

		log.info("-----------");
		log.info("TRADE PROFIT: {}", tradeName);
		log.info("-----------");

		backtestResultsMap.get(TRADE_KEY).printMetaData();

		// set and run analysis criteria
		criterion = new TotalProfitCriterion();
		criterionValue = criterion.calculate(exchangeTrade.getTradeTimeSeries(), exchangeTrade.getTradeTradingRecord());
		log.info("criteria: TotalProfitCriterion value: {}", criterionValue);
		backtestResultsMap.get(TRADE_KEY).setTotalProfit(BigDecimal.valueOf(criterionValue));

		criterion = new BuyAndHoldCriterion();
		criterionValue = criterion.calculate(exchangeTrade.getTradeTimeSeries(), exchangeTrade.getTradeTradingRecord());
		log.info("criteria: BuyAndHoldCriterion value: {}", criterionValue);
		backtestResultsMap.get(TRADE_KEY).setBuyAndHold(BigDecimal.valueOf(criterionValue));

		criterion = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
		criterionValue = criterion.calculate(exchangeTrade.getTradeTimeSeries(), exchangeTrade.getTradeTradingRecord());
		log.info("criteria: VersusBuyAndHoldCriterion value: {}", criterionValue);
		backtestResultsMap.get(TRADE_KEY).setVersusBuyAndHold(BigDecimal.valueOf(criterionValue));

		backtestResultsRepository.saveAll(backtestResultsMap.values());

		log.debug("done");
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
