package com.etricky.cryptobot.core.exchanges.common;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.knowm.xchange.currency.CurrencyPair;

import com.etricky.cryptobot.core.common.threads.ThreadInfo;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.exchanges.gdax.account.GdaxAccount;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractExchangeOrders extends AbstractExchange implements Runnable {

	public static final int ORDER_LIMIT = 1;
	public static final int ORDER_MARKET = 2;
	public static final int ORDER_STOP = 3;

	protected BigDecimal limitPriceGap;
	protected Set<CurrencyPair> activeOrders;
	protected GdaxAccount gdaxAccount;

	public AbstractExchangeOrders(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles) {
		super(exchangeThreads, commands, jsonFiles);
		activeOrders = new HashSet<>();
	}

	public void initialize(ExchangeEnum exchangeEnum, ThreadInfo threadInfo) {
		log.debug("start. exchange: {}", exchangeEnum.getName());

		this.exchangeEnum = exchangeEnum;
		this.threadInfo = threadInfo;

		log.debug("done");
	}

	@Override
	protected void exchangeDisconnect() {
		log.debug("start");

		exchangeThreads.stopExchangeThreads(exchangeEnum.getName());

		commands.sendMessage("Stopped orders for exchange: " + exchangeEnum.getTradingBean(), true);

		log.debug("done");
	}
}
