package com.etricky.cryptobot.core.exchanges.common;

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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExchangeThreads {
	public final static int EXCHANGE_INVALID = 1;
	public final static int CURRENCY_INVALID = 2;
	public final static int EXCHANGE_CURRENCY_PAIR_INVALID = 3;
	public final static int NO_CONFIG_EXCHANGE = 4;

	@Autowired
	private ApplicationContext appContext;
	
	@Autowired
	JsonFiles jsonFiles;

	@Autowired
	private ThreadExecutors threadExecutor;

	private String threadName;

	static HashMap<String, AbstractExchange> threadsMap = new HashMap<>();

	public int startExchangeThreads(String exchange, String currency)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		log.debug("start. exchange: {} currency: {}", exchange, currency);

		setThreadName(exchange, currency);

		// validates if the thread already exists
		if (threadsMap.containsKey(threadName)) {
			log.debug("thread alraedy exists");
			return 1;
		} else {
			ExchangeEnum exchangeEnum = ExchangeEnum.getInstanceByName(exchange);
			CurrencyEnum currencyEnum = CurrencyEnum.getInstanceByShortName(currency);

			// gets a new exchange bean
			AbstractExchange exchangeGeneric = (AbstractExchange) appContext.getBean(exchangeEnum.getCrytobotBean());

			// starts the thread
			ThreadInfo threadInfo = new ThreadInfo(exchangeEnum, currencyEnum, exchangeGeneric, threadName);
			exchangeGeneric.setThreadInfo(threadInfo);

			threadExecutor.executeSingle(exchangeGeneric);

			threadsMap.put(threadName, exchangeGeneric);
		}
		log.debug("done");
		return 0;
	}

	public HashMap<String, List<String>> getRunningThreads() {
		HashMap<String, List<String>> exchangeMap = new HashMap<String, List<String>>();

		threadsMap.values().forEach((e) -> {
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

	private String setThreadName(String exchange, String currency) {
		threadName = "T_" + exchange + "-" + currency;
		log.debug("thread name:{}", threadName);
		return threadName;
	}

	public int stopThread(String exchange, String currency) {
		log.debug("start. exchange: {} currency: {}", exchange, currency);

		setThreadName(exchange, currency);

		if (threadsMap.containsKey(threadName)) {
			log.debug("found thread: {} id: {}, sending interrupt", threadName,
					threadsMap.get(threadName).getThreadInfo().getThread().getId());
			threadsMap.get(threadName).getThreadInfo().interrupt();
		} else {
			log.debug("no thread {} found", threadName);
			return 1;
		}

		log.debug("done");
		return 0;
	}

	public void stopAllThreads() {
		log.debug("start");

		threadsMap.forEach((k, t) -> {

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

		if (threadsMap.containsKey(threadInfo.getThreadName())) {
			log.debug("thread exists and will be removed");
			threadsMap.remove(threadInfo.getThreadName());
		} else
			log.debug("thread does not exist");

		log.debug("done");
	}

	public String getThreadName(String exchange, String currency) {
		setThreadName(exchange, currency);
		return threadName;
	}

	public int validateExchangeCurreny(String exchange, String currency) {
		int result = 0;
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

		log.debug("done. result=: {}", result);
		return result;
	}
}
