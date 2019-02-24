package com.etricky.cryptobot;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.etricky.cryptobot.core.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeAccount;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;

import lombok.extern.slf4j.Slf4j;

//@RunWith(SpringJUnit4ClassRunner.class)
@RunWith(SpringRunner.class)
//@SpringBootTest
@ContextConfiguration(classes = CryptoBotApplication.class)
@Slf4j
public class CryptoBotTest {

	@Autowired
	ApplicationContext appContext;

	@Test
	public void getOrderBook() throws ExchangeException {
		log.info("start");
		System.out.println("STARTED");
		AbstractExchangeAccount abstractExchangeAccount = (AbstractExchangeAccount) appContext
				.getBean(ExchangeEnum.getInstanceByName("gdax").get().getAccountBean());
		abstractExchangeAccount.initialize(ExchangeEnum.getInstanceByName("gdax").get(), Optional.empty(), false);

		// gets the inside bid and ask
		OrderBook orderBook;
		try {
			orderBook = abstractExchangeAccount.getExchange().getMarketDataService()
					.getOrderBook(CurrencyEnum.BTC_USD.getCurrencyPair(), 1);
		} catch (IOException e) {
			log.error("Exception: {}", e);
			throw new ExchangeException(e);
		}

		log.info("OrderBook Buy: {} Sell: {}", orderBook.getBids().get(0).getLimitPrice(),
				orderBook.getAsks().get(0).getLimitPrice());

		assertNotNull(orderBook);
	}
}
