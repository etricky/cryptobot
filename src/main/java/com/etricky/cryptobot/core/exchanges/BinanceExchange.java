package com.etricky.cryptobot.core.exchanges;

import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.exchanges.common.ExchangeGeneric;
import com.etricky.cryptobot.core.exchanges.common.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.shell.ShellCommands;
import com.etricky.cryptobot.repositories.TradesEntityRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BinanceExchange extends ExchangeGeneric {

	public BinanceExchange(ExchangeThreads exchangeThreads, ShellCommands shellCommands,
			TradesEntityRepository tradesEntityRepository) {
		super(exchangeThreads, shellCommands, tradesEntityRepository);
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
