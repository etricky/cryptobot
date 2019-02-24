package com.etricky.cryptobot.core.exchanges.gdax.orders;

import java.util.HashSet;
import java.util.Set;

import org.knowm.xchange.coinbasepro.dto.trade.CoinbaseProOrderFlags;
import org.knowm.xchange.dto.Order.IOrderFlags;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeOrders;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeOrderTypeEnum;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("gdaxOrdersBean")
public class GdaxOrders extends AbstractExchangeOrders {

	public GdaxOrders(ExchangeThreads exchangeThreads, JsonFiles jsonFiles, Commands commands) {
		super(exchangeThreads, commands, jsonFiles);
	}

	@Override
	public void run() {
		try {

			log.debug("thread: {}", Thread.currentThread().getId());
			setThreadInfoData();

			commands.sendMessage("Started orders thread for exchange: " + exchangeEnum.getName(), true);

			log.debug("putting thread {} to sleep", threadInfo.getThreadName());
			wait();

		} catch (InterruptedException e) {
			log.debug("thread interrupted");

			commands.sendMessage("Thread " + getThreadInfo().getThreadName() + " interrupted", true);
		}

	}

	@Override
	public Set<IOrderFlags> getOrderFlags(ExchangeOrderTypeEnum exchangeOrderType) {
		Set<IOrderFlags> orderFlags = new HashSet<>();

		switch (exchangeOrderType) {
		case LIMIT:
			orderFlags.add(CoinbaseProOrderFlags.POST_ONLY);
		default:
			break;
		}
		return orderFlags;
	}

}
