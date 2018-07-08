package com.etricky.cryptobot.core.exchanges.bitstamp;

import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.exchanges.common.AbstractExchange;
import com.etricky.cryptobot.core.exchanges.common.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.common.ExchangeStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BitstampExchange extends AbstractExchange {

	public BitstampExchange(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles, ExchangeStrategy exchangeStrategy) {
		super(exchangeThreads, commands, jsonFiles, exchangeStrategy);
	}

	private void startTrade() {
		log.debug("start");

		log.debug("done");
	}

	@Override
	public void run() {
		log.debug("start");

		startTrade();

		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			log.debug("thread interrupted");

			stopTrade();
		}

		log.debug("done");
	}
}
