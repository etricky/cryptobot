package com.etricky.cryptobot.core.exchanges.gdax;

import java.util.ArrayList;

import javax.annotation.PostConstruct;

import org.knowm.xchange.dto.marketdata.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.exchanges.common.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.ExchangeGeneric;
import com.etricky.cryptobot.core.exchanges.common.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.common.StrategyGeneric;

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
	private GdaxHistoryTrades gdaxHistoryTrades;
	@Autowired
	private GdaxLiveTrades gdaxLiveTrade;
	@Autowired
	private ApplicationContext appContext;

	public GdaxExchange(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles) {
		super(exchangeThreads, commands, jsonFiles);
	}

	@PostConstruct
	private void initiateAuxiliarBeans() {
		gdaxHistoryTrades.setGdaxExchange(this);
		gdaxLiveTrade.setGdaxExchange(this);

		strategies = new ArrayList<StrategyGeneric>();

		jsonFiles.getExchangesJson().get(ExchangeEnum.GDAX.getName()).getStrategies().forEach((s) -> {
			log.debug("creating bean: {}", s.getBean());
			StrategyGeneric strategy = (StrategyGeneric) appContext.getBean(s.getBean());
			strategy.setExchangeParameters(ExchangeEnum.GDAX, s.getBean(), jsonFiles);
			strategies.add(strategy);
		});
	}

	private void processTrade(Trade trade) {
		log.debug("start. trade: {}", trade);

		if (firstRun) {
			firstRun = false;
			// for log purposes changes the name of the thread
			Thread.currentThread().setName(threadInfo.getThreadName());
		}

		gdaxLiveTrade.processLiveTrade(trade);

		log.debug("done");

	}

	private void startTrade() throws ExchangeException {
		log.debug("start");

		// before getting any new trades it must fill the trade history
		// gdaxHistoryTrades.processTradeHistory();

		ProductSubscription productSubscription = ProductSubscription.create()
				.addTrades(threadInfo.getCurrencyEnum().getCurrencyPair()).build();

		exchange = StreamingExchangeFactory.INSTANCE.createExchange(GDAXStreamingExchange.class.getName());
		exchange.connect(productSubscription).blockingAwait();

		subscription = exchange.getStreamingMarketDataService()
				.getTrades(threadInfo.getCurrencyEnum().getCurrencyPair()).subscribe(trade -> {
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
		}

		log.debug("done");
	}

}
