package com.etricky.cryptobot.core.interfaces;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import org.knowm.xchange.coinbasepro.service.CoinbaseProMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeAccount;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeOrders;
import com.etricky.cryptobot.core.exchanges.common.ExchangeProductMetaData;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeOrderTypeEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CryptoBotTest {
	@Autowired
	ApplicationContext appContext;
	AbstractExchangeAccount abstractExchangeAccount;

	public void runTest(String[] args) throws ExchangeException {
		log.debug("start: {}", (Object[]) args);

		switch (args[0].toLowerCase()) {
		case "orderbook":
			orderBookTest();
			break;
		case "prods":
			coinbaseProducts();
			break;
		case "order":
			placeOrder(args);
			break;
		default:
			break;
		}

		log.debug("done");
	}

	private void loadGdaxAccount() throws ExchangeException {
		abstractExchangeAccount = (AbstractExchangeAccount) appContext
				.getBean(ExchangeEnum.getInstanceByName("gdax").get().getAccountBean());
		abstractExchangeAccount.initialize(ExchangeEnum.getInstanceByName("gdax").get(), Optional.empty(), false);
	}

	/**
	 * @param args Arguments to place an order:<br>
	 *             <ul>
	 *             <li>Arg1: ExchangeOrderType
	 *             <ul style="padding-left:20px">
	 *             <li>{@link ExchangeOrderTypeEnum#LIMIT} - A limit order lets you set your own
	 *             price, as well as set some advanced order execution option</li>
	 *             <li>{@link ExchangeOrderTypeEnum#MARKET} - A market order will execute
	 *             immediately at the current market price</li>
	 *             <li>{@link ExchangeOrderTypeEnum#STOP} - A stop order lets you specify the price
	 *             at which the order should be executed and is useful for stop loss
	 *             and similar strategies</li>
	 *             </ul>
	 *             </li>
	 *             <li>Arg2: OrderType
	 *             <ul>
	 *             <li>{@link OrderType#ASK}</li>
	 *             <li>{@link OrderType#BID}</li>
	 *             </ul>
	 *             <li>Arg3: Currency Short Name</li>
	 *             <li>Arg4: Amount</li></li>
	 *             </ul>
	 * @throws ExchangeException
	 */
	public void placeOrder(String[] args) throws ExchangeException {
		try {

			ExchangeOrderTypeEnum exchangeOrderType = ExchangeOrderTypeEnum.valueOf(args[1]);
			OrderType orderType = OrderType.valueOf(args[2]);
			String currency = args[3];
			BigDecimal amount = BigDecimal.valueOf(Double.parseDouble(args[4]));

			log.debug("start. exchangeOrderType: {} orderType: {} currency: {} amount: {}", exchangeOrderType,
					orderType, currency, amount);

			loadGdaxAccount();
			AbstractExchangeOrders abstractExchangeOrders = (AbstractExchangeOrders) appContext
					.getBean(ExchangeEnum.getInstanceByName("gdax").get().getOrdersBean());
			abstractExchangeOrders.initialize(ExchangeEnum.GDAX, Optional.empty(), abstractExchangeAccount, true);

			abstractExchangeOrders.placeOrder(exchangeOrderType, orderType,
					CurrencyEnum.getInstanceByShortName(currency).get(), amount, Optional.empty(), Optional.empty());
		} catch (Exception e) {
			log.error("Exception: {}", e);
			throw new ExchangeException(e);
		}

		log.debug("done");
	}

	public void orderBookTest() throws ExchangeException {
		log.debug("start");
		CurrencyPair currencyPair;

		// gets the inside bid and ask
		OrderBook orderBook;
		try {
			currencyPair = CurrencyEnum.BTC_USD.getCurrencyPair();
			loadGdaxAccount();
			orderBook = abstractExchangeAccount.getExchange().getMarketDataService().getOrderBook(currencyPair, 1);

			log.debug("OrderBook currencyPair: {} Buy: {} Sell: {}", currencyPair,
					orderBook.getBids().get(0).getLimitPrice(), orderBook.getAsks().get(0).getLimitPrice());

			currencyPair = new CurrencyPair("BAT", "USDC");
			orderBook = abstractExchangeAccount.getExchange().getMarketDataService().getOrderBook(currencyPair, 1);

			log.debug("OrderBook currencyPair: {} Buy: {} Sell: {}", currencyPair,
					orderBook.getBids().get(0).getLimitPrice(), orderBook.getAsks().get(0).getLimitPrice());
		} catch (IOException e) {
			log.error("Exception: {}", e);
			throw new ExchangeException(e);
		}

		log.debug("done");
	}

	public void coinbaseProducts() throws ExchangeException {
		log.debug("start");

		try {
			loadGdaxAccount();

			CoinbaseProMarketDataService coinbaseProMarketDataService = (CoinbaseProMarketDataService) abstractExchangeAccount
					.getExchange().getMarketDataService();

			Arrays.stream(coinbaseProMarketDataService.getCoinbaseProProducts()).forEach(prod -> {
				log.debug("baseCurrency: {}", prod.getBaseCurrency());
				log.debug("baseMaxSize: {}", prod.getBaseMaxSize());
				log.debug("baseMinSize: {}", prod.getBaseMinSize());
				log.debug("id: {}", prod.getId());
				log.debug("quoteIncrement: {}", prod.getQuoteIncrement());
				log.debug("targetCurrency: {}", prod.getTargetCurrency());
				log.debug("----");

			});

			Optional<ExchangeProductMetaData> metaData = abstractExchangeAccount
					.getProductMetaData(CurrencyEnum.BAT_USDC);

			if (metaData.isPresent()) {
				log.debug("found product");
				log.debug("metaData: {}", metaData.get().toString());
			} else {
				log.debug("product not found!");
			}

		} catch (IOException e) {
			log.error("Exception: {}", e);
			throw new ExchangeException(e);
		}

		log.debug("done");
	}
}
