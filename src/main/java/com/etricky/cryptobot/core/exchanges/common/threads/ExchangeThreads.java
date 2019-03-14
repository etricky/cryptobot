package com.etricky.cryptobot.core.exchanges.common.threads;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.common.exceptions.ExchangeExceptionRT;
import com.etricky.cryptobot.core.common.threads.ThreadExecutors;
import com.etricky.cryptobot.core.common.threads.ThreadInfo;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeAccount;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeTrading;
import com.etricky.cryptobot.core.exchanges.common.ExchangeTrade;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.backtest.StrategyBacktest;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExchangeThreads {
	public final static int OK = 0;
	public final static int TRADE_THREAD_EXISTS = 1;
	public final static int TRADE_THREAD_NOT_EXISTS = 2;

	public final static String POOL_BACKTEST = "backtest_pool";

	public enum ThreadType {
		TRADING_THREAD("T"), ORDERS_THREAD("O"), ACCOUNT_THREAD("A"), BACKTEST_THREAD("B");

		@Getter
		protected String shortCode;

		private ThreadType(String shortCode) {
			this.shortCode = shortCode;
		}
	}

	@Autowired
	private ApplicationContext appContext;

	@Autowired
	JsonFiles jsonFiles;

	@Autowired
	private ThreadExecutors threadExecutor;

	@Getter
	private Map<String, ExchangeTrade> exchangeTradeMap = new HashMap<>();
	private Map<String, AbstractExchangeTrading> exchangeCurrencyTradeThreadsMap = new HashMap<>();
	private Map<String, AbstractExchangeAccount> exchangeAccountsThreadsMap = new HashMap<>();
	private String exchangeTradeKey;
	private String exchangeCurrencyTradeThreadKey;
	private AbstractExchangeTrading abstractExchangeTrading;
	private int countSharedThreads = 0;

	/**
	 * Starts the thread for a trade of a specific exchange.
	 * 
	 * @param exchange  Exchange to be used
	 * @param tradeName Name of the trade strategy
	 * @param tradeType Type of trading that should be performed:
	 *                  <ul>
	 *                  <li>FULL: adds past trades to the database, process all
	 *                  strategies for the trade and issues exchange orders</li>
	 *                  <li>DRY_RUN: adds past trades to the database, process all
	 *                  strategies but does not issues exchange orders</li>
	 *                  <li>HISTORY_ONLY: only adds past trades to the database</li>
	 *                  <li>BACKTEST: backtest scenario where all strategies are
	 *                  processed but no past trades are added to the database and
	 *                  no exchange orders are issued</li>
	 *                  </ul>
	 * @return
	 * @throws ExchangeException
	 */
	public int startExchangeTradingThread(String exchange, String tradeName, int tradeType) throws ExchangeException {
		ExchangeTrade exchangeTrade;
		AbstractExchangeAccount abstractExchangeAccount;
		int returnValue = OK;

		log.debug("start. exchange: {} tradeName: {} tradeType:{}", exchange, tradeName, tradeType);

		setExchangeTradeKey(exchange, tradeName);

		// validates if the trade already exists
		if (exchangeTradeMap.containsKey(exchangeTradeKey)) {
			log.debug("tradeName {} already exists", tradeName);
			returnValue = TRADE_THREAD_EXISTS;
		} else {

			ExchangeEnum exchangeEnum = ExchangeEnum.getInstanceByName(exchange).get();
			exchangeTrade = (ExchangeTrade) appContext.getBean("exchangeTrade");

			if (exchangeAccountsThreadsMap.containsKey(exchange)) {
				log.debug("exchange {} orders thread already exists", exchange);
				abstractExchangeAccount = exchangeAccountsThreadsMap.get(exchange);
			} else {
				// launch exchange orders thread if it doesn't exist
				log.debug("create exchange {}orders bean", exchange);

				abstractExchangeAccount = (AbstractExchangeAccount) appContext.getBean(exchangeEnum.getAccountBean());

				ThreadInfo threadInfo = new ThreadInfo(
						getThreadName(exchange, tradeName, ThreadType.ACCOUNT_THREAD, null));
				abstractExchangeAccount.initialize(exchangeEnum, Optional.of(threadInfo), true);

				exchangeAccountsThreadsMap.put(exchange, abstractExchangeAccount);

				threadExecutor.executeSingle(abstractExchangeAccount);
			}

			// for each new trade creates a new "exchangeTrade" object
			exchangeTrade.initialize(tradeName, exchangeEnum, abstractExchangeAccount, tradeType);
			exchangeTradeMap.put(exchangeTradeKey, exchangeTrade);

			// for each currencyPairs of the trade launch a thread
			ArrayList<String> tradeCurrencies = jsonFiles.getExchangesJsonMap().get(exchange).getTradeConfigsMap()
					.get(tradeName).getCurrencyPairs();

			tradeCurrencies.forEach(currencyPair -> {
				CurrencyEnum currencyEnum = CurrencyEnum.getInstanceByShortName(currencyPair).get();
				setExchangeCurrencyTradeThreadKey(exchange, currencyEnum);

				if (exchangeCurrencyTradeThreadsMap.containsKey(exchangeCurrencyTradeThreadKey)) {
					log.debug("exchange {} currency thread {} already exists", exchange,
							exchangeCurrencyTradeThreadKey);

					abstractExchangeTrading = exchangeCurrencyTradeThreadsMap.get(exchangeCurrencyTradeThreadKey);
				} else {
					// gets a new exchange trading bean
					abstractExchangeTrading = (AbstractExchangeTrading) appContext
							.getBean(exchangeEnum.getTradingBean());

					// starts the trading thread
					ThreadInfo threadInfo = new ThreadInfo(
							getThreadName(exchange, tradeName, ThreadType.TRADING_THREAD, currencyEnum));
					abstractExchangeTrading.initialize(exchangeEnum, currencyEnum, threadInfo, false);

					log.debug("create exchange {} trading bean for currencyPair: {}", exchange, currencyPair);

					exchangeCurrencyTradeThreadsMap.put(exchangeCurrencyTradeThreadKey, abstractExchangeTrading);
					threadExecutor.executeSingle(abstractExchangeTrading);
				}

				try {
					exchangeTrade.addExchangeTrading(currencyEnum, abstractExchangeTrading);
				} catch (ExchangeException e) {
					log.error("Exception: {}", e);
					throw new ExchangeExceptionRT(e);
				}
			});

			returnValue = OK;
		}

		log.debug("done");
		return returnValue;
	}

	public void backtest(String exchange, String tradeName, ZonedDateTime startDate, ZonedDateTime endDate)
			throws ExchangeException {
		log.debug("start. exchange: {} tradeName: {} startDate: {} endDate: {}", exchange, tradeName, startDate,
				endDate);

		// gets a new backtest bean
		StrategyBacktest backtestBean = (StrategyBacktest) appContext.getBean("strategyBacktest");
		backtestBean.initialize(ExchangeEnum.getInstanceByName(exchange).get(), tradeName, startDate, endDate);

		threadExecutor.initializeThreadPool(POOL_BACKTEST, 3,
				jsonFiles.getExchangesJsonMap().get(exchange).getTradeConfigs().size());
		threadExecutor.executeTaskOnThreadPool(POOL_BACKTEST, backtestBean);

		log.debug("done");
	}

	public HashMap<String, List<String>> getRunningTradesThreads() {
		HashMap<String, List<String>> runningTradesMap = new HashMap<String, List<String>>();

		exchangeTradeMap.values().forEach((trade) -> {
			String exchange = trade.getExchangeEnum().getName();

			if (runningTradesMap.containsKey(exchange)) {
				runningTradesMap.get(exchange).add(trade.getTradeName());
			} else {
				List<String> auxTrades = new ArrayList<String>();
				auxTrades.add(trade.getTradeName());
				runningTradesMap.put(exchange, auxTrades);
			}
		});

		return runningTradesMap;
	}

	private void setExchangeCurrencyTradeThreadKey(String exchange, CurrencyEnum currencyEnum) {
		exchangeCurrencyTradeThreadKey = exchange + "-" + currencyEnum.getShortName();
	}

	public String getExchangeTradeKey(String exchange, String tradeName) {
		return exchange + "-" + tradeName;
	}

	private void setExchangeTradeKey(String exchange, String tradeName) {
		exchangeTradeKey = exchange + "-" + tradeName;
		log.debug("done. exchangeTradeKey: {}", exchangeTradeKey);
	}

	public static String getThreadName(String exchange, String tradeName, ThreadType threadType,
			CurrencyEnum currencyEnum) {
		String threadName;
		if (threadType == ThreadType.TRADING_THREAD) {
			threadName = threadType + "_" + exchange + "-" + tradeName + "-" + currencyEnum.getShortName();
		} else {
			threadName = threadType + "_" + exchange;
		}

		log.debug("done. threadName: {}", threadName);
		return threadName;
	}

	private boolean threadShared(String exchange, String tradeName, String currencyEnum, ThreadType threadType) {
		boolean result = false;
		countSharedThreads = 0;

		log.debug("start. exchange: {} tradeName: {} currencyEnum: {} threadType: {}", exchange, tradeName,
				currencyEnum, threadType);

		if (threadType == ThreadType.TRADING_THREAD) {
			exchangeTradeMap.values().forEach(trade -> {
				if (!trade.getTradeName().equalsIgnoreCase(tradeName)) {
					trade.getExchangeTradeCurrencyMap().values().forEach(trading -> {
						if (trading.getCurrencyName().equalsIgnoreCase((currencyEnum))) {
							countSharedThreads++;
							log.debug("currency trade thread used by trade: {}", trade.getTradeName());
						}
					});
				}
			});
		} else if (threadType == ThreadType.ACCOUNT_THREAD) {
			exchangeTradeMap.values().forEach(trade -> {
				if (!trade.getTradeName().equalsIgnoreCase(tradeName)
						&& trade.getExchangeAccount().getExchangeEnum().getName().equalsIgnoreCase(exchange)) {
					countSharedThreads++;
					log.debug("account thread used by trade: {}", trade.getTradeName());
				}
			});
		}

		if (countSharedThreads > 0) {
			result = true;
		}

		log.debug("done. result: {}", result);
		return result;
	}

	public void stopExchangeThreads(String exchange) {
		log.debug("start. exchange: {}", exchange);
		List<String> auxList = new ArrayList<>();

		exchangeTradeMap.values().stream().filter(trade -> trade.getExchangeEnum().getName().equalsIgnoreCase(exchange))
				.forEach((trade) -> {

					// to avoid java.util.ConcurrentModificationException
					auxList.add(trade.getTradeName());
				});

		auxList.forEach(trade -> {
			log.debug("stopping trade {} for exchange: {}", trade, exchange);
			stopTradeThreads(exchange, trade);
		});

		log.debug("done");
	}

	/**
	 * Stops the trade thread for an exchange. If this is the last trade thread, it
	 * will also stop the account thread
	 * 
	 * @param exchange  Id of the exchange
	 * @param tradeName Name of the trade thread
	 * @return
	 */
	public synchronized int stopTradeThreads(String exchange, String tradeName) {
		log.debug("start. exchange: {} tradeName: {}", exchange, tradeName);

		setExchangeTradeKey(exchange, tradeName);

		if (exchangeTradeMap.containsKey(exchangeTradeKey)) {

			// stops all currency threads first if not shared with another trade
			exchangeTradeMap.get(exchangeTradeKey).getExchangeTradingMap().forEach((curr, exchangeTrading) -> {

				if (!threadShared(exchange, tradeName, curr, ThreadType.TRADING_THREAD)) {

					log.debug("sending interrupt to trading thread: {} id: {}",
							exchangeTrading.getThreadInfo().getThreadName(),
							exchangeTrading.getThreadInfo().getThread().getId());

					exchangeTrading.getThreadInfo().interrupt();

					setExchangeCurrencyTradeThreadKey(exchange, CurrencyEnum.getInstanceByShortName(curr).get());

					if (exchangeCurrencyTradeThreadsMap.containsKey(exchangeCurrencyTradeThreadKey)) {
						log.debug("removing currency {} trading", curr);
						exchangeCurrencyTradeThreadsMap.remove(exchangeCurrencyTradeThreadKey);
					}
				}
			});

			if (!threadShared(exchange, tradeName, null, ThreadType.ACCOUNT_THREAD)) {
				log.debug("sending interrupt to orders thread: {} id: {}",
						exchangeTradeMap.get(exchangeTradeKey).getExchangeAccount().getThreadInfo().getThreadName(),
						exchangeTradeMap.get(exchangeTradeKey).getExchangeAccount().getThreadInfo().getThread()
								.getId());

				exchangeTradeMap.get(exchangeTradeKey).getExchangeAccount().getThreadInfo().interrupt();
				exchangeAccountsThreadsMap.remove(exchange);
			}

			// removes the trade from the map
			log.debug("removing {} from exchangeTradeMap", exchangeTradeKey);
			exchangeTradeMap.remove(exchangeTradeKey);
		} else {
			log.debug("no trade {} found", exchangeTradeKey);
			return TRADE_THREAD_NOT_EXISTS;
		}

		log.debug("done");
		return OK;
	}

	public void stopAllThreads() {
		Map<String, List<String>> auxMap = new HashMap<>();

		log.debug("start");

		exchangeTradeMap.forEach((key, trade) -> {
			if (!auxMap.containsKey(key)) {
				List<String> auxList = new ArrayList<>();
				auxList.add(trade.getTradeName());
				auxMap.put(trade.getExchangeEnum().getName(), auxList);
			} else {
				auxMap.get(trade.getExchangeEnum().getName()).add(trade.getTradeName());
			}

		});

		auxMap.forEach((exchange, tradeList) -> {
			tradeList.forEach(trade -> {
				stopTradeThreads(exchange, trade);
			});

		});

		log.debug("done");
	}
}
