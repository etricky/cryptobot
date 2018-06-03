package com.etricky.cryptobot.core.exchanges.gdax;

import org.knowm.xchange.dto.marketdata.Trade;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GdaxLiveTrades {
	private GdaxExchange gdaxExchange;

	public GdaxLiveTrades(GdaxExchange gdaxExchange) {
		this.gdaxExchange = gdaxExchange;
	}

	public void processLiveTrade(Trade trade) {
		log.debug("start. trade: {}", trade);

		log.debug("done");
	}
}
