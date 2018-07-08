package com.etricky.cryptobot.core.exchanges.gdax;

import javax.annotation.PostConstruct;

import org.knowm.xchange.dto.marketdata.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.exchanges.common.AbstractExchange;
import com.etricky.cryptobot.core.exchanges.common.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.ExchangeExceptionRT;
import com.etricky.cryptobot.core.exchanges.common.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.common.ExchangeStrategy;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.gdax.GDAXStreamingExchange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
public class GdaxExchange extends AbstractExchange {
	boolean firstRun = true;

	@Autowired
	private GdaxHistoryTrades gdaxHistoryTrades;
	@Autowired
	private GdaxLiveTrades gdaxLiveTrade;

	public GdaxExchange(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles, ExchangeStrategy exchangeStrategy) {
		super(exchangeThreads, commands, jsonFiles, exchangeStrategy);
	}

	@PostConstruct
	private void initiateAuxiliarBeans() {
		gdaxHistoryTrades.setGdaxExchange(this, jsonFiles.getExchangesJson().get(ExchangeEnum.GDAX.getName()));
		gdaxLiveTrade.setGdaxExchange(this);
	}

	private void processTrade(Trade trade) {
		log.debug("start. trade: {}", trade);

		if (firstRun) {
			firstRun = false;
			// for log purposes changes the name of the thread
			Thread.currentThread().setName(threadInfo.getThreadName());
		}

		// gdaxLiveTrade.processLiveTrade(trade);

		log.debug("done");

	}

	private void startTrade() throws ExchangeException {
		log.debug("start");

		exchangeStrategy.initializeStrategies(ExchangeEnum.GDAX, threadInfo.getCurrencyEnum());

		// before getting any new trades it must fill the trade history
		gdaxHistoryTrades.processTradeHistory();

		ProductSubscription productSubscription = ProductSubscription.create().addTrades(threadInfo.getCurrencyEnum().getCurrencyPair()).build();

		exchange = StreamingExchangeFactory.INSTANCE.createExchange(GDAXStreamingExchange.class.getName());
		exchange.connect(productSubscription).blockingAwait();

		subscription = exchange.getStreamingMarketDataService().getTrades(threadInfo.getCurrencyEnum().getCurrencyPair()).subscribe(trade -> {
			processTrade(trade);
		}, throwable -> {
			log.error("ERROR in getting trades: ", throwable);
			stopTrade();
			throw new ExchangeException(throwable);
		});

		log.debug("done");
	}

	@Override
	public void run() {
		log.debug("start");

		try {
			log.debug("thread: {}", Thread.currentThread().getId());
			setThreadInfoData();

			startTrade();

			commands.sendMessage("Started thread: " + threadInfo.getThreadName(), true);

			log.debug("putting thread {} to sleep", threadInfo.getThreadName());
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			log.debug("thread interrupted");

			stopTrade();
		} catch (ExchangeException e) {
			log.error("Exception: {}", e);

			stopTrade();
		} catch (ExchangeExceptionRT e) {
			log.error("Exception: {}", e);

			stopTrade();
		}

		log.debug("done");
	}

}
