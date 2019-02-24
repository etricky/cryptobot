package com.etricky.cryptobot.core.interfaces;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.knowm.xchange.coinbasepro.service.CoinbaseProMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeAccount;
import com.etricky.cryptobot.core.exchanges.common.ExchangeProductMetaData;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CryptoBotTest {
	@Autowired
	ApplicationContext appContext;

	public void runTest(String[] args) throws ExchangeException {
		log.debug("start: {}", (Object[]) args);

		switch (args[0].toLowerCase()) {
		case "orderbook":
			orderBookTest();
			break;
		case "prods":
			coinbaseProducts();
			break;
		default:
			break;
		}

		log.debug("done");
	}

	public void orderBookTest() throws ExchangeException {
		log.debug("start");
		CurrencyPair currencyPair;

		AbstractExchangeAccount abstractExchangeAccount = (AbstractExchangeAccount) appContext
				.getBean(ExchangeEnum.getInstanceByName("gdax").get().getAccountBean());
		abstractExchangeAccount.initialize(ExchangeEnum.getInstanceByName("gdax").get(), Optional.empty(), false);

		// gets the inside bid and ask
		OrderBook orderBook;
		try {
			currencyPair = CurrencyEnum.BTC_USD.getCurrencyPair();
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
			AbstractExchangeAccount abstractExchangeAccount = (AbstractExchangeAccount) appContext
					.getBean(ExchangeEnum.getInstanceByName("gdax").get().getAccountBean());
			abstractExchangeAccount.initialize(ExchangeEnum.getInstanceByName("gdax").get(), Optional.empty(), false);

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
