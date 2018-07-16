package com.etricky.cryptobot.core.exchanges.common;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.ThreadExecutors;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeJson;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.backtest.StrategyBacktest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExchangeThreads {
	public final static int OK = 0;
	public final static int EXCHANGE_INVALID = 1;
	public final static int CURRENCY_INVALID = 2;
	public final static int EXCHANGE_CURRENCY_PAIR_INVALID = 3;
	public final static int NO_CONFIG_EXCHANGE = 4;
	public final static int THREAD_EXISTS = 5;
	public final static int THREAD_NOT_EXISTS = 6;

	@Autowired
	private ApplicationContext appContext;

	@Autowired
	JsonFiles jsonFiles;

	@Autowired
	private ThreadExecutors threadExecutor;

	private String threadName;

	static HashMap<String, AbstractExchange> exchangeThreadsMap = new HashMap<>();

	public int startExchangeThreads(String exchange, String currency, int tradeType)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		log.debug("start. exchange: {} currency: {} tradeType:{}", exchange, currency, tradeType);

		setThreadName(exchange, currency, "T");

		// validates if the thread already exists
		if (exchangeThreadsMap.containsKey(threadName)) {
			log.debug("thread alraedy exists");
			return THREAD_EXISTS;
		} else {
			ExchangeEnum exchangeEnum = ExchangeEnum.getInstanceByName(exchange);
			CurrencyEnum currencyEnum = CurrencyEnum.getInstanceByShortName(currency);

			// gets a new exchange bean
			AbstractExchange abstractExchange = (AbstractExchange) appContext.getBean(exchangeEnum.getCrytobotBean());

			// starts the thread
			ThreadInfo threadInfo = new ThreadInfo(exchangeEnum, currencyEnum, abstractExchange, threadName);
			abstractExchange.setThreadInfo(threadInfo);
			abstractExchange.setTradeType(tradeType);

			threadExecutor.executeSingle(abstractExchange);

			exchangeThreadsMap.put(threadName, abstractExchange);
		}

		log.debug("done");
		return OK;
	}

	public void backtest(String exchange, String currency, long historyDays, int choosedStrategies, ZonedDateTime startDate, ZonedDateTime endDate) {
		log.debug("start. exchange: {} currency: {} historyDays: {}  startDate: {} endDate: {}", exchange, currency, historyDays, startDate, endDate);

		setThreadName(exchange, currency, "B");

		// gets a new backtest bean
		StrategyBacktest backtesteBean = (StrategyBacktest) appContext.getBean("strategyBacktest");
		backtesteBean.initialize(ExchangeEnum.getInstanceByName(exchange), CurrencyEnum.getInstanceByShortName(currency), historyDays,
				choosedStrategies, startDate, endDate);
		threadExecutor.executeSingle(backtesteBean);

		log.debug("done");
	}

	public HashMap<String, List<String>> getRunningThreads() {
		HashMap<String, List<String>> exchangeMap = new HashMap<String, List<String>>();

		exchangeThreadsMap.values().forEach((e) -> {
			String key = e.getThreadInfo().getExchangeEnum().getName();
			if (exchangeMap.containsKey(key)) {
				exchangeMap.get(key).add(e.getThreadInfo().getCurrencyEnum().getShortName());
			} else {
				List<String> auxCurr = new ArrayList<String>();
				auxCurr.add(e.getThreadInfo().getCurrencyEnum().getShortName());
				exchangeMap.put(key, auxCurr);
			}
		});

		return exchangeMap;
	}

	private String setThreadName(String exchange, String currency, String threadType) {
		threadName = threadType + "_" + exchange + "-" + currency;
		log.debug("thread name:{}", threadName);
		return threadName;
	}

	public int stopThread(String exchange, String currency) {
		log.debug("start. exchange: {} currency: {}", exchange, currency);

		setThreadName(exchange, currency, "T");

		if (exchangeThreadsMap.containsKey(threadName)) {
			log.debug("found thread: {} id: {}, sending interrupt", threadName,
					exchangeThreadsMap.get(threadName).getThreadInfo().getThread().getId());
			exchangeThreadsMap.get(threadName).getThreadInfo().interrupt();
		} else {
			log.debug("no thread {} found", threadName);
			return THREAD_NOT_EXISTS;
		}

		log.debug("done");
		return OK;
	}

	public void stopAllThreads() {
		log.debug("start");

		exchangeThreadsMap.forEach((k, t) -> {

			if (t.getThreadInfo().getThread().isAlive() && !t.getThreadInfo().getThread().isInterrupted()) {
				log.debug("stopping thread: {}", k);
				t.getThreadInfo().getThread().interrupt();
			} else
				log.debug("thread: {} is not alive", k);
		});

		log.debug("done");
	}

	public void removeThread(ThreadInfo threadInfo) {
		log.debug("start. threadInfo: {}", threadInfo);

		if (exchangeThreadsMap.containsKey(threadInfo.getThreadName())) {
			log.debug("thread exists and will be removed");
			exchangeThreadsMap.remove(threadInfo.getThreadName());
		} else
			log.debug("thread does not exist");

		log.debug("done");
	}

	public String getThreadName(String exchange, String currency) {
		setThreadName(exchange, currency, "T");
		return threadName;
	}

	public int validateExchangeCurreny(String exchange, String currency) {
		int result = OK;
		log.debug("start. exchange: {} currency: {}", exchange, currency);

		if (ExchangeEnum.getInstanceByName(exchange) == null) {
			result = EXCHANGE_INVALID;
		}

		if (CurrencyEnum.getInstanceByShortName(currency) == null) {
			result = CURRENCY_INVALID;
		}

		// checks if the currency is valid for the exchange
		Map<String, ExchangeJson> exchangeJson = jsonFiles.getExchangesJson();
		if (exchangeJson.containsKey(exchange.toUpperCase())) {
			if (!exchangeJson.get(exchange.toUpperCase()).getCurrenciesMap().containsKey(currency.toUpperCase())) {
				result = EXCHANGE_CURRENCY_PAIR_INVALID;
			}
		} else {
			result = NO_CONFIG_EXCHANGE;
		}

		log.debug("done. result: {}", result);
		return result;
	}
}
