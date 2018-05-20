package com.etricky.cryptobot.service.exchanges;

import org.knowm.xchange.currency.CurrencyPair;

import com.etricky.cryptobot.service.exchanges.common.ExchangeGeneric;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.gdax.GDAXStreamingExchange;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Gdax extends ExchangeGeneric implements Runnable {

	@Override
	public void startTradeThread() {
		log.debug("start");

		ProductSubscription productSubscription = ProductSubscription.create().addTrades(CurrencyPair.BTC_USD).build();

		StreamingExchange exchange = StreamingExchangeFactory.INSTANCE
				.createExchange(GDAXStreamingExchange.class.getName());
		exchange.connect(productSubscription).blockingAwait();

		Disposable subscription = exchange.getStreamingMarketDataService().getTrades(CurrencyPair.BTC_USD)
				.subscribe(trade -> {
					log.info("TRADE: {}", trade);
				}, throwable -> log.error("ERROR in getting trades: ", throwable));

		log.debug("done");
	}

	@Override
	public void stopTradeThread() {
		log.debug("start");

		log.debug("done");
	}

	@Override
	public void run() {
		log.debug("start");

		startTradeThread();

		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			log.debug("thread interrupted");

			stopTradeThread();

		}

		log.debug("done");
	}

}
