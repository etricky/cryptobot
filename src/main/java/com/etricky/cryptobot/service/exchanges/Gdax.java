package com.etricky.cryptobot.service.exchanges;

import com.etricky.cryptobot.service.exchanges.common.ExchangeGeneric;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.gdax.GDAXStreamingExchange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Gdax extends ExchangeGeneric {

	private void startTrade() {
		log.debug("start");

		// TODO process trade history
		shellCommands.sendMessage("Started thread: " + threadInfo.getThreadKey(), true);
		ProductSubscription productSubscription = ProductSubscription.create()
				.addTrades(threadInfo.getCurrencyEnum().getCurrencyPair()).build();

		exchange = StreamingExchangeFactory.INSTANCE.createExchange(GDAXStreamingExchange.class.getName());
		exchange.connect(productSubscription).blockingAwait();

		subscription = exchange.getStreamingMarketDataService()
				.getTrades(threadInfo.getCurrencyEnum().getCurrencyPair()).subscribe(trade -> {
					log.info("TRADE: {}", trade);
					// TODO process live trades
				}, throwable -> {
					log.error("ERROR in getting trades: ", throwable);
					stopTrade();
				});

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
