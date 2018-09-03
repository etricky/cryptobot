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
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeOrders;
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
	public final static String TRADING_THREAD = "T";
	public final static String ORDERS_THREAD = "O";
	public final static String BACKTEST_THREAD = "B";

	@Autowired
	private ApplicationContext appContext;

	@Autowired
	JsonFiles jsonFiles;

	@Autowired
	private ThreadExecutors threadExecutor;

	@Getter
	private Map<String, ExchangeTrade> exchangeTradeMap = new HashMap<>();
	private Map<String, AbstractExchangeTrading> exchangeCurrencyTradeThreadsMap = new HashMap<>();
	private Map<String, AbstractExchangeOrders> exchangeOrdersThreadsMap = new HashMap<>();
	private String exchangeTradeKey;
	private String exchangeCurrencyTradeThreadKey;
	private ThreadInfo threadInfo;
	private AbstractExchangeTrading abstractExchangeTrading;

	public int startExchangeTradingThread(String exchange, String tradeName, int tradeType) {
		ExchangeTrade exchangeTrade;
		AbstractExchangeOrders abstractExchangeOrders;

		log.debug("start. exchange: {} tradeName: {} tradeType:{}", exchange, tradeName, tradeType);

		setExchangeTradeKey(exchange, tradeName);

		// validates if the trade already exists
		if (exchangeTradeMap.containsKey(exchangeTradeKey)) {
			log.debug("trade already exists");
			return TRADE_THREAD_EXISTS;
		}

		ExchangeEnum exchangeEnum = ExchangeEnum.getInstanceByName(exchange).get();
		exchangeTrade = (ExchangeTrade) appContext.getBean("exchangeTrade");

		if (exchangeOrdersThreadsMap.containsKey(exchange)) {
			log.debug("exchange orders thread already exists");
			abstractExchangeOrders = exchangeOrdersThreadsMap.get(exchange);
		} else {
			// launch exchange orders thread if it doesn't exist
			log.debug("create exchange orders bean");

			abstractExchangeOrders = (AbstractExchangeOrders) appContext.getBean(exchangeEnum.getOrdersBean());

			threadInfo = new ThreadInfo(getThreadName(exchange, tradeName, ORDERS_THREAD, null));
			abstractExchangeOrders.initialize(exchangeEnum, threadInfo);

			exchangeOrdersThreadsMap.put(exchange, abstractExchangeOrders);

			// TODO start orders/account thread
		}

		// for each currencyPairs of the trade launch a thread
		ArrayList<String> tradeCurrencies = jsonFiles.getExchangesJson().get(exchange).getTradeConfigsMap()
				.get(tradeName).getCurrencyPairs();

		tradeCurrencies.forEach(currencyPair -> {
			CurrencyEnum currencyEnum = CurrencyEnum.getInstanceByShortName(currencyPair).get();
			setExchangeCurrencyTradeThreadKey(exchange, currencyEnum);

			if (exchangeCurrencyTradeThreadsMap.containsKey(exchangeCurrencyTradeThreadKey)) {
				log.debug("exchange currency thread {} already exists", exchangeCurrencyTradeThreadKey);

				abstractExchangeTrading = exchangeCurrencyTradeThreadsMap.get(exchangeCurrencyTradeThreadKey);
			} else {
				// gets a new exchange trading bean
				abstractExchangeTrading = (AbstractExchangeTrading) appContext.getBean(exchangeEnum.getTradingBean());

				// starts the trading thread
				ThreadInfo threadInfo = new ThreadInfo(
						getThreadName(exchange, tradeName, TRADING_THREAD, currencyEnum));
				abstractExchangeTrading.initialize(exchangeEnum, currencyEnum, tradeType, threadInfo);

				log.debug("create exchange trading bean for currencyPair: {}", currencyPair);

				exchangeCurrencyTradeThreadsMap.put(exchangeCurrencyTradeThreadKey, abstractExchangeTrading);
				threadExecutor.executeSingle(abstractExchangeTrading);
			}

			exchangeTrade.addExchangeTrading(currencyEnum, abstractExchangeTrading);
		});

		// for each new trade creates a new "exchangeTrade" object
		exchangeTrade.initialize(tradeName, exchangeEnum, abstractExchangeOrders, false);
		exchangeTradeMap.put(exchangeTradeKey, exchangeTrade);

		log.debug("done");
		return OK;
	}

	public void backtest(String exchange, String tradeName, long historyDays, ZonedDateTime startDate,
			ZonedDateTime endDate) {
		log.debug("start. exchange: {} tradeName: {} historyDays: {} startDate: {} endDate: {}", exchange, tradeName,
				historyDays, startDate, endDate);

		// gets a new backtest bean
		StrategyBacktest backtestBean = (StrategyBacktest) appContext.getBean("strategyBacktest");
		backtestBean.initialize(ExchangeEnum.getInstanceByName(exchange).get(), tradeName, historyDays, startDate,
				endDate);
		threadExecutor.executeSingle(backtestBean);

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
	}

	public static String getThreadName(String exchange, String tradeName, String threadType,
			CurrencyEnum currencyEnum) {
		if (threadType == TRADING_THREAD) {
			return threadType + "_" + exchange + "-" + tradeName + "-" + currencyEnum.getShortName();
		}
		return threadType + "_" + exchange;
	}

	int count = 0;

	private boolean threadShared(String exchange, String tradeName, String currencyEnum, String threadType) {
		boolean result = false;

		log.debug("start. exchange: {} tradeName: {} currencyEnum: {} threadType: {}", exchange, tradeName,
				currencyEnum, threadType);

		if (threadType == TRADING_THREAD) {
			exchangeTradeMap.values().forEach(trade -> {
				if (!trade.getTradeName().equalsIgnoreCase(tradeName)) {
					trade.getExchangeCurrencyTradeMap().values().forEach(trading -> {
						if (trading.getCurrencyEnum().getShortName().equalsIgnoreCase((currencyEnum))) {
							count++;
							log.debug("currency trade thread used by trade: {}", trade.getTradeName());
						}
					});
				}
			});
		} else {
			exchangeTradeMap.values().forEach(trade -> {
				if (!trade.getTradeName().equalsIgnoreCase(tradeName)
						&& trade.getExchangeOrders().getExchangeEnum().getName().equalsIgnoreCase(exchange)) {
					count++;
					log.debug("orders thread used by trade: {}", trade.getTradeName());
				}
			});
		}

		if (count > 1) {
			result = true;
		}

		log.debug("done. result: {}", result);
		return result;
	}

	public void stopExchangeThreads(String exchange) {
		log.debug("start. exchange: {}", exchange);

		exchangeTradeMap.values().stream().filter(trade -> trade.getExchangeEnum().getName().equalsIgnoreCase(exchange))
				.forEach((trade) -> {
					log.debug("stopping trade {}", trade.getTradeName());

					stopTradeThreads(exchange, trade.getTradeName());
				});

		log.debug("done");
	}

	public int stopTradeThreads(String exchange, String tradeName) {
		log.debug("start. exchange: {} tradeName: {}", exchange, tradeName);

		setExchangeTradeKey(exchange, tradeName);

		if (exchangeTradeMap.containsKey(exchangeTradeKey)) {

			// stops all currency threads first if not shared with another trade
			exchangeTradeMap.get(exchangeTradeKey).getExchangeCurrencyTradeMap().forEach((curr, exchangeTrading) -> {

				if (!threadShared(exchange, tradeName, curr, TRADING_THREAD)) {

					log.debug("sending interrupt to trading thread: {} id: {}",
							exchangeTrading.getThreadInfo().getThreadName(),
							exchangeTrading.getThreadInfo().getThread().getId());

					exchangeTrading.getThreadInfo().interrupt();
				}
			});

			if (!threadShared(exchange, tradeName, null, ORDERS_THREAD)) {
				log.debug("sending interrupt to orders thread: {} id: {}",
						exchangeTradeMap.get(exchangeTradeKey).getExchangeOrders().getThreadInfo().getThreadName(),
						exchangeTradeMap.get(exchangeTradeKey).getExchangeOrders().getThreadInfo().getThread().getId());

				exchangeTradeMap.get(exchangeTradeKey).getExchangeOrders().getThreadInfo().interrupt();
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
		log.debug("start");

		exchangeTradeMap.forEach((key, trade) -> {
			stopTradeThreads(trade.getExchangeOrders().getExchangeEnum().getName(), trade.getTradeName());
		});

		log.debug("done");
	}
}
