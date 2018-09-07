package com.etricky.cryptobot.core.exchanges.gdax.trading;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.knowm.xchange.dto.marketdata.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeTrading;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeExceptionRT;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.gdax.GDAXStreamingExchange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("gdaxTradingBean")
@Scope("prototype")
public class GdaxTrading extends AbstractExchangeTrading {
	boolean firstRun = true;

	@Autowired
	private GdaxHistoryTrades gdaxHistoryTrades;
	@Autowired
	private GdaxLiveTrades gdaxLiveTrade;

	public GdaxTrading(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles) {
		super(exchangeThreads, commands, jsonFiles);
		this.tradingBean = "gdaxTradingBean";
	}

	@PostConstruct
	private void initiateAuxiliarBeans() {
		gdaxHistoryTrades.setGdaxTrading(this);
		gdaxLiveTrade.setGdaxTrading(this);
	}

	private void processTrade(Trade trade) throws ExchangeException {
		log.debug("start. trade: {}", trade);

		if (firstRun) {
			firstRun = false;
			// for log purposes changes the name of the thread
			Thread.currentThread().setName(threadInfo.getThreadName());
		}

		gdaxLiveTrade.processLiveTrade(trade);

		log.debug("done");
	}

	public void processTradeHistory(Optional<ZonedDateTime> startPeriod, Optional<ZonedDateTime> endPeriod)
			throws ExchangeException {
		gdaxHistoryTrades.processTradeHistory(startPeriod, endPeriod);
	}

	private void startTrade() throws ExchangeException {
		log.debug("start");

		// before getting any new trades it must fill the trade history
		processTradeHistory(Optional.empty(), Optional.empty());

		if (!historyOnlyTrade) {
			ProductSubscription productSubscription = ProductSubscription.create()
					.addTrades(currencyEnum.getCurrencyPair()).build();

			streamingExchange = StreamingExchangeFactory.INSTANCE.createExchange(GDAXStreamingExchange.class.getName());
			streamingExchange.connect(productSubscription).blockingAwait();

			processingLiveTrades = true;

			try {
				subscription = streamingExchange.getStreamingMarketDataService()
						.getTrades(currencyEnum.getCurrencyPair()).subscribe(trade -> {
							try {
								processTrade(trade);
							} catch (Exception e) {
								log.error("ERROR in processing trade: ", e);
								exchangeDisconnect();
								throw new ExchangeExceptionRT(e);
							}
						}, throwable -> {
							log.error("ERROR in getting trades: ", throwable);
							exchangeDisconnect();
							throw new ExchangeExceptionRT(throwable);
						});
			} catch (Exception e) {
				log.error("ERROR in processing trade: ", e);
				exchangeDisconnect();
				throw new ExchangeExceptionRT(e);
			}
		} else {
			log.debug("skipping live trades");
		}

		log.debug("done");
	}

	@Override
	public void run() {
		log.debug("start");

		try {
			log.debug("thread: {}", Thread.currentThread().getId());
			setThreadInfoData();

			startTrade();

			commands.sendMessage("Started trading trade for exchange: " + exchangeEnum.getName() + " currency: "
					+ currencyEnum.getShortName(), true);

			log.debug("putting thread {} to sleep", threadInfo.getThreadName());
			Thread.sleep(Long.MAX_VALUE);

		} catch (InterruptedException e) {
			log.debug("thread interrupted");

			commands.sendMessage("Thread " + getThreadInfo().getThreadName() + " interrupted", true);

		} catch (ExchangeException e) {
			log.error("Exception: {}", e);

			commands.sendMessage("Exception occurred on " + getThreadInfo().getThreadName() + ". Stopping thread",
					true);

			exchangeDisconnect();
		} catch (ExchangeExceptionRT e) {
			log.error("Exception: {}", e);

			commands.sendMessage("Exception occurred on " + getThreadInfo().getThreadName() + ". Stopping thread",
					true);

			exchangeDisconnect();
		}

		log.debug("done");
	}
}
