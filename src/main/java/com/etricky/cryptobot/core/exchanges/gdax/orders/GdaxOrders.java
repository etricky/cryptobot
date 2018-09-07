package com.etricky.cryptobot.core.exchanges.gdax.orders;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.IOrderFlags;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.StopOrder;
import org.knowm.xchange.gdax.dto.trade.GDAXOrderFlags;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeOrders;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("gdaxOrdersBean")
public class GdaxOrders extends AbstractExchangeOrders {

	public GdaxOrders(ExchangeThreads exchangeThreads, JsonFiles jsonFiles, Commands commands) {
		super(exchangeThreads, commands, jsonFiles);
		limitPriceGap = BigDecimal.valueOf(0.01);
	}

	public void placeOrder(int exchangeOrderType, OrderType orderType, CurrencyPair currencyPair, BigDecimal amount,
			BigDecimal stopPrice, BigDecimal stopLimitPrice) throws IOException {
		String orderId = null;
		BigDecimal limitPrice;
		Set<IOrderFlags> orderFlags = new HashSet<>();

		log.debug("start. exchangeOrderType: {} orderType: {} amount: {} price: {} ", exchangeOrderType, orderType,
				currencyPair, amount, stopPrice);

		// validates if already exists an order for the same currency
		if (activeOrders.contains(currencyPair)) {
			log.warn("duplicate order");
			commands.sendMessage("Duplicate order for " + exchangeEnum.getName() + " and currency: " + currencyPair,
					true);
		} else {
			activeOrders.add(currencyPair);

			switch (exchangeOrderType) {
			case ORDER_LIMIT:
				orderFlags.add(GDAXOrderFlags.POST_ONLY);

				// gets the inside bid and ask
				OrderBook orderBook = gdaxAccount.getExchange().getMarketDataService().getOrderBook(currencyPair, 1);

				// a limit order price has to be as close as possible of the best buy/sell in
				// the order book
				if (orderType == OrderType.ASK) {
					limitPrice = NumericFunctions.subtract(orderBook.getBids().get(0).getLimitPrice(), limitPriceGap,
							NumericFunctions.PRICE_SCALE);
				} else {
					limitPrice = orderBook.getAsks().get(0).getLimitPrice().add(limitPriceGap);
				}

				log.debug("limitPrice: {}", limitPrice);

				LimitOrder limitOrder = new LimitOrder.Builder(orderType, currencyPair).originalAmount(amount)
						.limitPrice(limitPrice).flags(orderFlags).build();
				orderId = gdaxAccount.getExchange().getTradeService().placeLimitOrder(limitOrder);

				break;
			case ORDER_MARKET:
				orderId = gdaxAccount.getExchange().getTradeService()
						.placeMarketOrder(new MarketOrder.Builder(orderType, currencyPair).originalAmount(amount)
								.flags(orderFlags).build());

				break;
			case ORDER_STOP:
				orderId = gdaxAccount.getExchange().getTradeService()
						.placeStopOrder(new StopOrder.Builder(orderType, currencyPair).originalAmount(amount)
								.limitPrice(stopLimitPrice).stopPrice(stopPrice).flags(orderFlags).build());

			}
		}
		log.debug("done. orderId: {}", orderId);
	}

	public void getOpenOrders() {
		log.debug("start");

//		OpenOrders openOrders = gdaxAccount.getExchange().getTradeService().getOpenOrders();

		log.debug("done");
	}

	public void cancelOrder() {
		// exchange.getTradeService().cancelOrder(orderId);
	}

	@Override
	public void run() {
		try {

			log.debug("thread: {}", Thread.currentThread().getId());
			setThreadInfoData();

			commands.sendMessage("Started trade for exchange: " + exchangeEnum.getName(), true);

			log.debug("putting thread {} to sleep", threadInfo.getThreadName());
			wait();

		} catch (InterruptedException e) {
			log.debug("thread interrupted");

			commands.sendMessage("Thread " + getThreadInfo().getThreadName() + " interrupted", true);
		}

	}
}
