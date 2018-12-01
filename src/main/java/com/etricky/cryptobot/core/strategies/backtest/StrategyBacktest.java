package com.etricky.cryptobot.core.strategies.backtest;

import java.lang.Thread.UncaughtExceptionHandler;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.common.threads.ThreadInfo;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeAccount;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeTrading;
import com.etricky.cryptobot.core.exchanges.common.ExchangeTrade;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeExceptionRT;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.StrategyResult;
import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;
import com.etricky.cryptobot.model.BacktestEntity;
import com.etricky.cryptobot.model.BacktestResultsEntity;
import com.etricky.cryptobot.model.TradeEntity;
import com.etricky.cryptobot.model.TradesData;
import com.etricky.cryptobot.model.pks.BacktestPK;
import com.etricky.cryptobot.repositories.BacktestRepository;
import com.etricky.cryptobot.repositories.BacktestResultsRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
public class StrategyBacktest implements Runnable, UncaughtExceptionHandler {

	@Autowired
	private JsonFiles jsonFiles;
	@Autowired
	private TradesData tradesData;
	@Autowired
	private Commands commands;
	@Autowired
	private BacktestRepository backtestRepository;
	@Autowired
	private BacktestResultsRepository backtestResultsRepository;
	@Autowired
	private ApplicationContext appContext;

	private List<BacktestEntity> backtestEntityList = new ArrayList<>();
	private Map<String, BacktestResultsEntity> backtestResultsMap = new HashMap<>();
	private Map<String, BacktestMetaData> backtestResultsMetaDataMap = new HashMap<>();
	private List<String> tradeCurrencies;
	private List<TradeEntity> dbTradeEntityList = new ArrayList<TradeEntity>();
	private Map<Long, List<TradeEntity>> dbTradeEntityMap = new TreeMap<>();

	private List<BacktestOrderInfo> currencyOrderInfoList = new ArrayList<>();
	private Map<String, List<BacktestOrderInfo>> backtestCurrencyOrderInfoMap = new TreeMap<>();

	private List<BacktestOrderInfo> tradeOrderInfoList = new ArrayList<>();

	private ExchangeEnum exchangeEnum;
	private ExchangeTrade exchangeTrade;
	private BacktestMetaData backtestMetaData;

	private String tradeName, currencyName;
	private final String TRADE_KEY = "trade_key";
	private long startDateUnixTime, endDateUnixTime, backtestStart, strategyInitialPeriod = 0;
	private long counter, backtestResultsIndex = 0;
	private ZonedDateTime startDate, endDate;

	public void initialize(ExchangeEnum exchangeEnum, String tradeName, ZonedDateTime startDate,
			ZonedDateTime endDate) {

		log.debug("start. exchangeEnum: {} tradeName: {}  startDate: {} endDate: {}", exchangeEnum, tradeName,
				startDate, endDate);

		this.exchangeEnum = exchangeEnum;
		this.tradeName = tradeName;

		jsonFiles.getExchangesJsonMap().get(exchangeEnum.getName()).getTradeConfigsMap().get(tradeName).getStrategies()
				.forEach(tradeStrategy -> {
					if (jsonFiles.getStrategiesJsonMap().containsKey(tradeStrategy.getStrategyName())) {
						if (strategyInitialPeriod == 0 || strategyInitialPeriod < jsonFiles.getStrategiesJsonMap()
								.get(tradeStrategy.getStrategyName()).getInitialPeriod())
							strategyInitialPeriod = jsonFiles.getStrategiesJsonMap()
									.get(tradeStrategy.getStrategyName()).getInitialPeriod();
					}
				});

		log.debug("strategyInitialPeriod: {}", strategyInitialPeriod);

		this.startDate = startDate.minusSeconds(strategyInitialPeriod);
		this.endDate = endDate;
		startDateUnixTime = DateFunctions.getUnixTimeFromZDT(this.startDate);
		endDateUnixTime = DateFunctions.getUnixTimeFromZDT(this.endDate);

		log.debug("done. startDate: {}/{} endDate: {}/{}", this.startDate, startDateUnixTime, this.endDate,
				endDateUnixTime);
	}

	/**
	 * Private static method to avoid that multiple trades for the same currency
	 * process the trade history
	 * 
	 * @param abstractExchangeTrading Exchange trading object that has the method to
	 *                                process the history trade
	 * @param startDate               Initial date of the trade history period to be
	 *                                processed
	 * @param endDate                 Final date of the trade history period to be
	 *                                processed
	 * @throws ExchangeException
	 */
	private synchronized static void processTradeHistory(AbstractExchangeTrading abstractExchangeTrading,
			ZonedDateTime startDate, ZonedDateTime endDate) throws ExchangeException {
		abstractExchangeTrading.processTradeHistory(Optional.of(startDate), Optional.of(endDate));
	}

	private void setExchangeTrade() {
		log.debug("start");

		exchangeTrade = (ExchangeTrade) appContext.getBean("exchangeTrade");

		AbstractExchangeAccount abstractExchangeAccount = (AbstractExchangeAccount) appContext
				.getBean(exchangeEnum.getAccountBean());

		abstractExchangeAccount.initialize(exchangeEnum, new ThreadInfo("backtest"));

		exchangeTrade.initialize(tradeName, exchangeEnum, abstractExchangeAccount,
				AbstractExchangeTrading.TRADE_BACKTEST);

		// for each currencyPairs of the trade launch a trading bean
		tradeCurrencies = jsonFiles.getExchangesJsonMap().get(exchangeEnum.getName()).getTradeConfigsMap()
				.get(tradeName).getCurrencyPairs();

		tradeCurrencies.forEach(currencyPair -> {
			CurrencyEnum currencyEnum = CurrencyEnum.getInstanceByShortName(currencyPair).get();

			try {
				// gets a new exchange trading bean
				AbstractExchangeTrading abstractExchangeTrading = (AbstractExchangeTrading) appContext
						.getBean(exchangeEnum.getTradingBean());

				abstractExchangeTrading.initialize(exchangeEnum, currencyEnum, new ThreadInfo("backtest"));

				StrategyBacktest.processTradeHistory(abstractExchangeTrading, startDate, endDate);

				log.debug("create exchange trading bean for currencyPair: {}", currencyPair);

				exchangeTrade.addExchangeTrading(currencyEnum, abstractExchangeTrading);
			} catch (ExchangeException e) {
				log.error("Exception: {}", e);
				throw new ExchangeExceptionRT(e);
			}
		});

		log.debug("done");
	}

	private void executeStrategies() {
		log.debug("start");

		// for each currency create an ordered map with the trades
		exchangeTrade.getExchangeTradeCurrencyMap().values().forEach(tradeCurrency -> {

			String currencyName = tradeCurrency.getCurrencyName();

			// load trades from BD for the currency
			dbTradeEntityList = tradesData.getTradesInPeriod(exchangeEnum.getName(), currencyName, startDateUnixTime,
					endDateUnixTime, jsonFiles.getExchangesJsonMap().get(exchangeEnum.getName()).getAllowFakeTrades());

			if (dbTradeEntityList.size() != 0) {
				log.debug("for currency {} got {} trades from database", currencyName, dbTradeEntityList.size());

				dbTradeEntityList.forEach(trade -> {

					// list of trades sorted by time
					if (dbTradeEntityMap.containsKey(trade.getTradeId().getUnixtime())) {
						dbTradeEntityMap.get(trade.getTradeId().getUnixtime()).add(trade);
					} else {
						List<TradeEntity> auxTradeList = new ArrayList<>();
						auxTradeList.add(trade);
						dbTradeEntityMap.put(trade.getTradeId().getUnixtime(), auxTradeList);
					}

				});

			} else {
				log.error("no trades in database for currency: {}", currencyName);
				throw new ExchangeExceptionRT("Missing db trades for currency: " + currencyName);
			}
		});

		log.debug("processing trades for all currencies");

		dbTradeEntityMap.values().forEach(tradeList -> {
			tradeList.forEach(trade -> {

				StrategyResult[] strategyResult = exchangeTrade.evaluateTrade(trade);

				if (strategyResult[ExchangeTrade.TRADE] != null
						&& strategyResult[ExchangeTrade.TRADE].getResult() != AbstractStrategy.NO_ACTION) {
					log.trace("adding new trade order. result: {}", strategyResult[ExchangeTrade.TRADE].getResult());

					tradeOrderInfoList.add(new BacktestOrderInfo(strategyResult[ExchangeTrade.TRADE]));
				}

				// for each currency, stores the orders
				if (strategyResult[ExchangeTrade.CURRENCY] != null
						&& strategyResult[ExchangeTrade.CURRENCY].getResult() != AbstractStrategy.NO_ACTION) {

					log.trace("adding new currency order. result: {}",
							strategyResult[ExchangeTrade.CURRENCY].getResult());

					BacktestOrderInfo backtestOrderInfo = new BacktestOrderInfo(strategyResult[ExchangeTrade.CURRENCY]);

					if (backtestCurrencyOrderInfoMap.containsKey(trade.getTradeId().getCurrency())) {
						backtestCurrencyOrderInfoMap.get(trade.getTradeId().getCurrency()).add(backtestOrderInfo);
					} else {
						currencyOrderInfoList = new ArrayList<>();
						currencyOrderInfoList.add(backtestOrderInfo);
						backtestCurrencyOrderInfoMap.put(trade.getTradeId().getCurrency(), currencyOrderInfoList);
					}
				}
			});
		});

		log.debug("done");
	}

	private void storeBacktestResult(BacktestOrderInfo orderInfo, BacktestMetaData backtestMetaData, String key) {

		BacktestPK backtestPK = BacktestPK.builder().exchange(exchangeEnum.getName()).tradeName(tradeName)
				.runTime(backtestStart).index(backtestResultsIndex++).tradeData(key.equals(TRADE_KEY) ? true : false)
				.build();

		backtestResultsMap.put(key, BacktestResultsEntity.builder().backtestId(backtestPK).tradeStart(startDate)
				.tradeEnd(endDate)
				.currency(key.equals(TRADE_KEY) ? tradeName
						: orderInfo.getStrategyResult().getTradeEntity().getTradeId().getCurrency())
				.initialBalance(backtestMetaData.getFirstOrderBalance())
				.initialAmount(backtestMetaData.getFirstOrderAmount())
				.initialPrice(backtestMetaData.getFirstOrderPrice())
				.finalBalance(orderInfo.getStrategyResult().getBalance())
				.finalAmount(orderInfo.getStrategyResult().getAmount())
				.finalPrice(orderInfo.getStrategyResult().getClosePrice())
				.deltaBalance(NumericFunctions.percentage(orderInfo.getStrategyResult().getBalance(),
						backtestMetaData.getFirstOrderBalance(), false))
				.deltaAmount(NumericFunctions.percentage(orderInfo.getStrategyResult().getAmount(),
						backtestMetaData.getFirstOrderAmount(), false))
				.posBalanceOrders(backtestMetaData.getPosBalanceOrders())
				.negBalanceOrders(backtestMetaData.getNegBalanceOrders())
				.posAmountOrders(backtestMetaData.getPosAmountOrders())
				.negAmountOrders(backtestMetaData.getNegAmountOrders()).totalOrders(backtestMetaData.getTotalOrders())
				.currencyOrders(backtestMetaData.getCurrencyOrders()).tradingBuys(backtestMetaData.getTradingBuys())
				.tradingSells(backtestMetaData.getTradingSells()).stopLossBuys(backtestMetaData.getStopLossBuys())
				.stopLossSells(backtestMetaData.getStopLossSells()).totalFees(backtestMetaData.getTotalFees())
				.multiCurrency(jsonFiles.getExchangesJsonMap().get(exchangeEnum.getName()).getTradeConfigsMap()
						.get(tradeName).getCurrencyPairs().size() > 1)
				.build());

		backtestResultsMetaDataMap.put(key, backtestMetaData);
	}

	private void processAndPrintBacktestData() {
		log.debug("start");
		currencyName = null;

		backtestCurrencyOrderInfoMap.forEach((currName, orderInfoList) -> {
			backtestMetaData = new BacktestMetaData();

			orderInfoList.forEach((orderInfo) -> {
				if (currencyName == null || !currencyName.equals(currName)) {
					log.info("-----------");
					log.info("CURRENCY ORDERS: {}", currName);
					log.info("-----------");
					currencyName = currName;
				}

				backtestMetaData.calculateMetaData(orderInfo, jsonFiles);

				// stores the backtest data for each currency in the database
				BacktestPK backtestPK = BacktestPK.builder().exchange(exchangeEnum.getName()).tradeName(tradeName)
						.runTime(backtestStart).index((long) orderInfo.getStrategyResult().getTimeSeriesEndIndex())
						.build();

				backtestEntityList.add(BacktestEntity.builder().orderId(backtestPK).currency(currName)
						.strategy(orderInfo.getStrategyResult().getStrategyName())
						.ordertime(orderInfo.getStrategyResult().getTradeEntity().getTimestamp())
						.orderUnixTime(orderInfo.getStrategyResult().getTradeEntity().getTradeId().getUnixtime())
						.orderType(orderInfo.getStrategyResult().getLastOrder().getType())
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
			if (orderInfoList.size() > 0) {
				storeBacktestResult(orderInfoList.get(orderInfoList.size() - 1), backtestMetaData, currName);
			} else {
				log.debug("no currency trade!!!");
			}
		});

		log.info("-----------");
		log.info("TRADE ORDERS: {}", tradeName);
		log.info("-----------");

		backtestMetaData = new BacktestMetaData();
		counter = 1;

		tradeOrderInfoList.forEach(orderInfo -> {

			backtestMetaData.calculateMetaData(orderInfo, jsonFiles);

			// stores the backtest data in the database
			BacktestPK backtestPK = BacktestPK.builder().exchange(exchangeEnum.getName()).tradeName(tradeName)
					.runTime(backtestStart).index(counter++).tradeData(true).build();

			backtestEntityList.add(BacktestEntity.builder().orderId(backtestPK)
					.currency(orderInfo.getStrategyResult().getTradeEntity().getTradeId().getCurrency())
					.strategy(orderInfo.getStrategyResult().getStrategyName())
					.ordertime(orderInfo.getStrategyResult().getTradeEntity().getTimestamp())
					.orderUnixTime(orderInfo.getStrategyResult().getTradeEntity().getTradeId().getUnixtime())
					.orderType(orderInfo.getStrategyResult().getLastOrder().getType())
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
		if (tradeOrderInfoList.size() > 0) {
			storeBacktestResult(tradeOrderInfoList.get(tradeOrderInfoList.size() - 1), backtestMetaData, TRADE_KEY);
		} else {
			log.debug("no currency trade!!!");
		}

		backtestRepository.saveAll(backtestEntityList);

		log.debug("done");
	}

	private void evaluation() {
		log.debug("start");

		exchangeTrade.getExchangeTradeCurrencyMap().forEach((currName, abstractTrading) -> {
			log.info("-----------");
			log.info("CURRENCY PROFIT: {}", currName);
			log.info("-----------");
			if (!backtestResultsMap.isEmpty()) {
				log.info("balance: {}/{}/{}% amount: {}/{}/{}%", backtestResultsMap.get(currName).getFinalBalance(),
						backtestResultsMap.get(currName).getInitialBalance(),
						NumericFunctions.percentage(backtestResultsMap.get(currName).getFinalBalance(),
								backtestResultsMap.get(currName).getInitialBalance(), false),
						backtestResultsMap.get(currName).getFinalAmount(),
						backtestResultsMap.get(currName).getInitialAmount(),
						NumericFunctions.percentage(backtestResultsMap.get(currName).getFinalAmount(),
								backtestResultsMap.get(currName).getInitialAmount(), false));

				backtestResultsMetaDataMap.get(currName).printMetaData();
			} else {
				log.info("no currency orders!");
			}
		});

		log.info("-----------");
		log.info("TRADE PROFIT: {}", tradeName);
		log.info("-----------");
		if (!backtestResultsMap.isEmpty()) {
			log.info("balance: {}/{}/{}% amount: {}/{}/{}%", backtestResultsMap.get(TRADE_KEY).getFinalBalance(),
					backtestResultsMap.get(TRADE_KEY).getInitialBalance(),
					NumericFunctions.percentage(backtestResultsMap.get(TRADE_KEY).getFinalBalance(),
							backtestResultsMap.get(TRADE_KEY).getInitialBalance(), false),
					backtestResultsMap.get(TRADE_KEY).getFinalAmount(),
					backtestResultsMap.get(TRADE_KEY).getInitialAmount(),
					NumericFunctions.percentage(backtestResultsMap.get(TRADE_KEY).getFinalAmount(),
							backtestResultsMap.get(TRADE_KEY).getInitialAmount(), false));

			backtestResultsMetaDataMap.get(TRADE_KEY).printMetaData();
		} else {
			log.info("no trade orders!");
		}

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

	private void interruptBacktest(Thread t, Throwable e) {
		log.error("start. exception on thread:{}", t.getName());
		log.error("exception: {}", e);

		commands.sendMessage("Backtest " + tradeName + " Exception: " + e.toString(), true);

		// sends the interrupt to itself
		if (t.isAlive() || !t.isInterrupted()) {
			log.debug("sending interrupt");
			t.interrupt();
		}

		log.debug("done");
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		interruptBacktest(t, e);
	}

	@Override
	public void run() {
		log.info("start");

		Thread.currentThread().setUncaughtExceptionHandler(this);

		Thread.currentThread().setName("B_" + exchangeEnum.getName() + "_" + tradeName);
		backTest();

		log.info("done");
	}
}
