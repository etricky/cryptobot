package com.etricky.cryptobot.core.exchanges.gdax;

import org.knowm.xchange.dto.marketdata.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.exchanges.common.ExchangeLock;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
public class GdaxLiveTrades {
	private GdaxExchange gdaxExchange;

	private ExchangeLock exchangeLock;

	@Autowired
	public void setExchangeLock(ExchangeLock exchangeLock) {
		this.exchangeLock = exchangeLock;
	}

	public void setGdaxExchange(GdaxExchange gdaxExchange) {
		this.gdaxExchange = gdaxExchange;
	}

	public void processLiveTrade(Trade trade) {
		log.debug("start. trade: {}", trade);

		log.debug("done");
	}
}
