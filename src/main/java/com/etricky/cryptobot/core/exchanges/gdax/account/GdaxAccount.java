package com.etricky.cryptobot.core.exchanges.gdax.account;

import java.io.IOException;

import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.coinbasepro.CoinbaseProExchange;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.threads.ThreadExecutors;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeAccount;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("gdaxAccountBean")
public class GdaxAccount extends AbstractExchangeAccount {

	public GdaxAccount(ExchangeThreads exchangeThreads, JsonFiles jsonFiles, Commands commands,
			ThreadExecutors threadExecutors) throws ExchangeException {
		super(exchangeThreads, commands, jsonFiles, threadExecutors);

		connectToExchange();
	}

	private void connectToExchange() throws ExchangeException {
		log.debug("start");

		try {
			ExchangeSpecification exSpec = new CoinbaseProExchange().getDefaultExchangeSpecification();

			exchangeJson = this.jsonFiles.getExchangesJsonMap().get(ExchangeEnum.GDAX.getName());

			if (exchangeJson.getSandbox()) {
				exchangeKeys = jsonFiles.getExchangeKeys(ExchangeEnum.GDAX.getName() + "-sandbox");

//			exSpec.setSslUri("https://api-public.sandbox.pro.coinbase.com");
//			exSpec.setHost("api-public.sandbox.pro.coinbase.com");
//			exSpec.setPort(80);
//			exSpec.setExchangeName("CoinbasePro");

			} else {
				exchangeKeys = jsonFiles.getExchangeKeys(ExchangeEnum.GDAX.getName());
			}

			exSpec.setExchangeSpecificParametersItem("Use_Sandbox", exchangeJson.getSandbox());
			exSpec.setApiKey(exchangeKeys.getKey());
			exSpec.setSecretKey(exchangeKeys.getSecret());
			exSpec.setExchangeSpecificParametersItem("passphrase", exchangeKeys.getPassphrase());

			exchange = ExchangeFactory.INSTANCE.createExchange(exSpec);

			accountInfo = exchange.getAccountService().getAccountInfo();

			// starts the thread pool used to issue orders
			// threadExecutor.initializeThreadPool();
		} catch (IOException e) {
			log.error("Exception: {}", e);
			throw new ExchangeException(e);
		}

		log.debug("done");
	}
}
