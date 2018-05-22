package com.etricky.cryptobot.core.exchanges.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.ThreadTaskExecutor;
import com.etricky.cryptobot.core.interfaces.shell.ShellCommands;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExchangeThreads {
	@Autowired
	private ShellCommands shellCommands;

	@Autowired
	private ApplicationContext appContext;

	@Autowired
	private ThreadTaskExecutor threadExecutor;

	private String threadName;

	static HashMap<String, ExchangeGeneric> threadsMap = new HashMap<>();

	public void startThread(String exchange, String currency)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		log.debug("start. exchange: {} currency: {}", exchange, currency);

		setThreadName(exchange, currency);

		// validates if the thread already exists
		if (threadsMap.containsKey(threadName)) {
			log.debug("thread alraedy exists");
			shellCommands.sendMessage("Thread already exist", true);
		} else {
			ExchangeEnum exchangeEnum = ExchangeEnum.getInstanceByName(exchange);
			CurrencyEnum currencyEnum = CurrencyEnum.getInstanceByShortName(currency);

			// gets a new exchange bean
			ExchangeGeneric exchangeGeneric = (ExchangeGeneric) appContext.getBean(exchangeEnum.getCrytobotBean());

			// starts the thread
			ThreadInfo threadInfo = new ThreadInfo(exchangeEnum, currencyEnum, exchangeGeneric, threadName);
			exchangeGeneric.setThreadInfo(threadInfo);

			threadExecutor.execute(exchangeGeneric);

			threadsMap.put(threadName, exchangeGeneric);
		}
		log.debug("done");

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

	public void stopThread(String exchange, String currency) {
		log.debug("start. exchange: {} currency: {}", exchange, currency);

		setThreadName(exchange, currency);

		if (threadsMap.containsKey(threadName)) {
			log.debug("found thread: {}, sending interrupt", threadName);
			threadsMap.get(threadName).getThreadInfo().interrupt();
		} else {
			log.debug("no thread {} found", threadName);
			shellCommands.sendMessage("no thread " + threadName + " found", true);
		}

		log.debug("done");
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
}
