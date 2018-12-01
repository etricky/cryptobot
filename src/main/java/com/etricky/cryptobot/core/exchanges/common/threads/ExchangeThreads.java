package com.etricky.cryptobot.core.exchanges.common.threads;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.threads.ThreadExecutors;
import com.etricky.cryptobot.core.common.threads.ThreadInfo;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeAccount;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeTrading;
import com.etricky.cryptobot.core.exchanges.common.ExchangeTrade;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeExceptionRT;
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
	public final static String TRADING_THREAD = "T";
	public final static String ORDERS_THREAD = "O";
	public final static String ACOUNT_THREAD = "A";
	public final static String BACKTEST_THREAD = "B";
	public final static String POOL_BACKTEST = "backtest_pool";

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

	public int startExchangeTradingThread(String exchange, String tradeName, int tradeType) {
		ExchangeTrade exchangeTrade;
		AbstractExchangeAccount abstractExchangeAccount;
		int returnValue = OK;

		log.debug("start. exchange: {} tradeName: {} tradeType:{}", exchange, tradeName, tradeType);

		setExchangeTradeKey(exchange, tradeName);

		// validates if the trade already exists
		if (exchangeTradeMap.containsKey(exchangeTradeKey)) {
			log.debug("trade already exists");
			returnValue = TRADE_THREAD_EXISTS;
		} else {

			ExchangeEnum exchangeEnum = ExchangeEnum.getInstanceByName(exchange).get();
			exchangeTrade = (ExchangeTrade) appContext.getBean("exchangeTrade");

			if (exchangeAccountsThreadsMap.containsKey(exchange)) {
				log.debug("exchange orders thread already exists");
				abstractExchangeAccount = exchangeAccountsThreadsMap.get(exchange);
			} else {
				// launch exchange orders thread if it doesn't exist
				log.debug("create exchange orders bean");

				abstractExchangeAccount = (AbstractExchangeAccount) appContext.getBean(exchangeEnum.getAccountBean());

				ThreadInfo threadInfo = new ThreadInfo(getThreadName(exchange, tradeName, ACOUNT_THREAD, null));
				abstractExchangeAccount.initialize(exchangeEnum, threadInfo);

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
					log.debug("exchange currency thread {} already exists", exchangeCurrencyTradeThreadKey);

					abstractExchangeTrading = exchangeCurrencyTradeThreadsMap.get(exchangeCurrencyTradeThreadKey);
				} else {
					// gets a new exchange trading bean
					abstractExchangeTrading = (AbstractExchangeTrading) appContext
							.getBean(exchangeEnum.getTradingBean());

					// starts the trading thread
					ThreadInfo threadInfo = new ThreadInfo(
							getThreadName(exchange, tradeName, TRADING_THREAD, currencyEnum));
					abstractExchangeTrading.initialize(exchangeEnum, currencyEnum, threadInfo);

					log.debug("create exchange trading bean for currencyPair: {}", currencyPair);

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

	public static String getThreadName(String exchange, String tradeName, String threadType,
			CurrencyEnum currencyEnum) {
		String threadName;
		if (threadType == TRADING_THREAD) {
			threadName = threadType + "_" + exchange + "-" + tradeName + "-" + currencyEnum.getShortName();
		} else {
			threadName = threadType + "_" + exchange;
		}

		log.debug("done. threadName: {}", threadName);
		return threadName;
	}

	private boolean threadShared(String exchange, String tradeName, String currencyEnum, String threadType) {
		boolean result = false;
		countSharedThreads = 0;

		log.debug("start. exchange: {} tradeName: {} currencyEnum: {} threadType: {}", exchange, tradeName,
				currencyEnum, threadType);

		if (threadType == TRADING_THREAD) {
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
		} else if (threadType == ACOUNT_THREAD) {
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
			log.debug("stopping trade {}", trade);
			stopTradeThreads(exchange, trade);
		});

		log.debug("done");
	}

	public int stopTradeThreads(String exchange, String tradeName) {
		log.debug("start. exchange: {} tradeName: {}", exchange, tradeName);

		setExchangeTradeKey(exchange, tradeName);

		if (exchangeTradeMap.containsKey(exchangeTradeKey)) {

			// stops all currency threads first if not shared with another trade
			exchangeTradeMap.get(exchangeTradeKey).getExchangeTradingMap().forEach((curr, exchangeTrading) -> {

				if (!threadShared(exchange, tradeName, curr, TRADING_THREAD)) {

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

			if (!threadShared(exchange, tradeName, null, ACOUNT_THREAD)) {
				log.debug("sending interrupt to orders thread: {} id: {}",
						exchangeTradeMap.get(exchangeTradeKey).getExchangeAccount().getThreadInfo().getThreadName(),
						exchangeTradeMap.get(exchangeTradeKey).getExchangeAccount().getThreadInfo().getThread()
								.getId());

				exchangeTradeMap.get(exchangeTradeKey).getExchangeAccount().getThreadInfo().interrupt();
				exchangeAccountsThreadsMap.remove(exchange);
			}

			// removes the trade from the map
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
