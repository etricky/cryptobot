package com.etricky.cryptobot;

import org.knowm.xchange.currency.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.gdax.GDAXStreamingExchange;
import io.reactivex.disposables.Disposable;

public class GdaxXstreamExample {
	private static final Logger LOG = LoggerFactory.getLogger(GdaxXstreamExample.class);

	public static void main(String[] args) {
		ProductSubscription productSubscription = ProductSubscription.create().addTrades(CurrencyPair.BTC_USD).build();

		StreamingExchange exchange = StreamingExchangeFactory.INSTANCE
				.createExchange(GDAXStreamingExchange.class.getName());
		exchange.connect(productSubscription).blockingAwait();

		// exchange.getStreamingMarketDataService().getOrderBook(CurrencyPair.BTC_USD).subscribe(orderBook
		// -> {
		// LOG.info("First ask: {}", orderBook.getAsks().get(0));
		// LOG.info("First bid: {}", orderBook.getBids().get(0));
		// }, throwable -> LOG.error("ERROR in getting order book: ", throwable));

		// exchange.getStreamingMarketDataService().getTicker(CurrencyPair.ETH_USD).subscribe(ticker
		// -> {
		// LOG.info("TICKER: {}", ticker);
		// }, throwable -> LOG.error("ERROR in getting ticker: ", throwable));

		Disposable subscription = exchange.getStreamingMarketDataService().getTrades(CurrencyPair.BTC_USD)
				.subscribe(trade -> {
					LOG.info("TRADE: {}", trade);
				}, throwable -> LOG.error("ERROR in getting trades: ", throwable));

		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Unsubscribe from data order book.
		LOG.debug("dispose");
		subscription.dispose();

		LOG.debug("disconnect");
		// Disconnect from exchange (non-blocking)
		exchange.disconnect().subscribe(() -> LOG.info("Disconnected from the Exchange"));

		System.exit(0);
	}
}