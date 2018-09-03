package com.etricky.cryptobot.core.exchanges.gdax.account;

import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.gdax.GDAXExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.threads.ThreadExecutors;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeAccount;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("gdaxAccountBean")
public class GdaxAccount extends AbstractExchangeAccount {

	public GdaxAccount(ExchangeThreads exchangeThreads, JsonFiles jsonFiles, Commands commands) {
		super(exchangeThreads, commands, jsonFiles);
	}

	@Autowired
	private ThreadExecutors threadExecutor;

	public void connectToExchange() throws JsonParseException, JsonMappingException, ExchangeException {
		log.debug("start");

		ExchangeSpecification exSpec = new GDAXExchange().getDefaultExchangeSpecification();

		exchangeJson = this.jsonFiles.getExchangesJson().get(ExchangeEnum.GDAX.getName());

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

		// starts the thread pool used to issue orders
		threadExecutor.initializeThreadPool();

		log.debug("done");

	}

	@Override
	protected void exchangeDisconnect() {
		// TODO order exchangeDisconnect
	}

}
