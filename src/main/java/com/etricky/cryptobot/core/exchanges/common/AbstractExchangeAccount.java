package com.etricky.cryptobot.core.exchanges.common;

import java.io.IOException;
import java.util.Map;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;

import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeJson;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeKeys;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractExchangeAccount extends AbstractExchange {

	@Getter
	protected Exchange exchange;
	protected ExchangeKeys exchangeKeys;
	protected ExchangeJson exchangeJson;
	protected String walletInfo;
	protected String exchangeName;

	public AbstractExchangeAccount(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles) {
		super(exchangeThreads, commands, jsonFiles);
	}

	public abstract void connectToExchange() throws JsonParseException, JsonMappingException, ExchangeException;

	public void initialize(ExchangeEnum exchangeEnum)
			throws JsonParseException, JsonMappingException, ExchangeException {
		log.debug("start. exchange: {}", exchangeEnum.getName());

		this.exchangeEnum = exchangeEnum;
		connectToExchange();

		log.debug("done");
	}

	public String getWalletInfo() throws IOException {
		log.debug("start");

		AccountInfo accountInfo = exchange.getAccountService().getAccountInfo();
		Wallet wallet = accountInfo.getWallet();
		walletInfo = "ID: " + wallet.getId() + "\n";
		Map<Currency, Balance> balanceMap = wallet.getBalances();
		balanceMap.forEach((curr, bal) -> {
			walletInfo += "Currency: " + curr + " Amount: " + bal.getAvailable() + "\n";
		});

		log.debug("done");
		return walletInfo;
	}

}
