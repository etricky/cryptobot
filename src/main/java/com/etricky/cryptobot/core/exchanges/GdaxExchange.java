package com.etricky.cryptobot.core.exchanges;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.exchanges.common.ExchangeGeneric;
import com.etricky.cryptobot.core.exchanges.common.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.shell.ShellCommands;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.gdax.GDAXStreamingExchange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
public class GdaxExchange extends ExchangeGeneric {
	boolean firstRun = true;

	@Autowired
	public GdaxExchange(ExchangeThreads exchangeThreads, ShellCommands shellCommands) {
		super(exchangeThreads, shellCommands);
	}

	private void startTrade() {

		log.debug("start");

		// TODO process trade history

		ProductSubscription productSubscription = ProductSubscription.create()
				.addTrades(threadInfo.getCurrencyEnum().getCurrencyPair()).build();

		exchange = StreamingExchangeFactory.INSTANCE.createExchange(GDAXStreamingExchange.class.getName());
		exchange.connect(productSubscription).blockingAwait();

		subscription = exchange.getStreamingMarketDataService()
				.getTrades(threadInfo.getCurrencyEnum().getCurrencyPair()).subscribe(trade -> {
					if (firstRun) {
						firstRun = false;
						Thread.currentThread().setName(threadInfo.getThreadName());
					}
					
					log.debug("trade: {}", trade);
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
		shellCommands.sendMessage("Started thread: " + threadInfo.getThreadName(), true);

		threadInfo.setThread(Thread.currentThread(), this);

		startTrade();

		try {
			log.debug("putting thread {} to sleep", threadInfo.getThreadName());
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			log.debug("thread interrupted");

			stopTrade();
		}

		log.debug("done");
	}

}
