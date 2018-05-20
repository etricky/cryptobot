package com.etricky.cryptobot.service.exchanges.common;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.service.interfaces.shell.ShellCommands;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExchangeThreads {
	@Autowired
	ShellCommands shellCommands;
	static HashMap<String, ThreadInfo> threadsMap = new HashMap<>();

	public void startThread(String exchange, String currency)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, ExchangeException {
		log.debug("start. exchange: {} currency: {}", exchange, currency);
		
		// validates if the thread already exists
		if (threadsMap.containsKey(ThreadInfo.getThreadKey(exchange, currency))) {
			log.debug("thread alraedy exists");
			shellCommands.sendMessage("Thread already exist", true);
		} else {
			ExchangeEnum exchangeEnum = ExchangeEnum.getInstanceByName(exchange);
			CurrencyEnum currencyEnum = CurrencyEnum.getInstanceByShortName(currency);
			ExchangeGeneric exchangeGeneric = ExchangeFactory.createExchange(exchangeEnum.getCrytobotClass());
			ThreadInfo threadInfo = new ThreadInfo(exchangeEnum, currencyEnum, exchangeGeneric);
			exchangeGeneric.startThread(threadInfo);
			threadsMap.put(threadInfo.getThreadKey(), threadInfo);
		}
		log.debug("done");

	}

	public void stopThread(ThreadInfo threadInfo) {
		log.debug("start. threadInfo: {}", threadInfo);

		threadInfo.getExchangeGeneric().interrupt();

		log.debug("done");
	}

	public void stopAllThreads() {
		log.debug("start");

		threadsMap.forEach((k, t) -> {
			log.debug("stopping thread: {}", k);
			t.getExchangeGeneric().interrupt();
		});

		log.debug("done");
	}

	public void removeThread(ThreadInfo threadInfo) {
		log.debug("start. threadInfo: {}", threadInfo);

		if (threadsMap.containsKey(threadInfo.getThreadKey())) {
			log.debug("thread exists and will be removed");
			threadsMap.remove(threadInfo.getThreadKey());
		} else
			log.debug("thread does not exist");

		log.debug("done");
	}
}
