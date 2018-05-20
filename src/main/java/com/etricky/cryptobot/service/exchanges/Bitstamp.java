package com.etricky.cryptobot.service.exchanges;

import com.etricky.cryptobot.service.exchanges.common.ExchangeGeneric;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Bitstamp extends ExchangeGeneric {

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
