package com.etricky.cryptobot.core.exchanges.bitstamp.trading;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeTrading;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("bitstampTradingBean")
public class BitstampTrading extends AbstractExchangeTrading {

	public BitstampTrading(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles) {
		super(exchangeThreads, commands, jsonFiles);
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

			commands.sendMessage("Thread " + getThreadInfo().getThreadName() + " interrupted", true);
		}

		log.debug("done");
	}

	@Override
	public void processTradeHistory(Optional<ZonedDateTime> startPeriod, Optional<ZonedDateTime> endPeriod)
			throws ExchangeException {
		// TODO Auto-generated method stub

	}
}
