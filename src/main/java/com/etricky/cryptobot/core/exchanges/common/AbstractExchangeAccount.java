package com.etricky.cryptobot.core.exchanges.common;

import java.math.BigDecimal;
import java.util.Map;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.marketdata.Ticker;

import com.etricky.cryptobot.core.common.threads.ThreadExecutors;
import com.etricky.cryptobot.core.common.threads.ThreadInfo;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeExceptionRT;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeJson;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeKeys;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractExchangeAccount extends AbstractExchange implements Runnable {

	public final static int QUOTE_CURRENCY = 0;
	public final static int BASE_CURRENCY = 1;

	@Getter
	protected Exchange exchange;
	protected ExchangeKeys exchangeKeys;
	protected ExchangeJson exchangeJson;
	protected AccountInfo accountInfo;

	private String walletInfo;

	protected ThreadExecutors threadExecutors;

	public AbstractExchangeAccount(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles,
			ThreadExecutors threadExecutors) {
		super(exchangeThreads, commands, jsonFiles);
		this.threadExecutors = threadExecutors;
	}

	public void initialize(ExchangeEnum exchangeEnum, ThreadInfo threadInfo) {
		log.debug("start. exchange: {}", exchangeEnum.getName());

		this.exchangeEnum = exchangeEnum;
		this.threadInfo = threadInfo;

		threadExecutors.initializeBlockingQueue(exchangeEnum.getName());

		log.debug("done");
	}

	public String getWalletInfo() {
		log.debug("start");

		Wallet wallet = accountInfo.getWallet();
		walletInfo = "ID: " + wallet.getId() + "\n";
		Map<Currency, Balance> balanceMap = wallet.getBalances();

		balanceMap.forEach((curr, bal) -> {
			try {

				if (CurrencyEnum.getInstanceByQuoteBase(curr.getCurrencyCode(), "EUR").isPresent()) {
					CurrencyPair currencyPair = CurrencyEnum.getInstanceByQuoteBase(curr.getCurrencyCode(), "EUR").get()
							.getCurrencyPair();

					Ticker ticker = exchange.getMarketDataService().getTicker(currencyPair, (Object[]) null);

					walletInfo += "Currency: " + curr + " Amount: " + bal.getAvailable() + " Balance: "
							+ bal.getAvailable().multiply(ticker.getLast()) + "\n";
				}

			} catch (Exception e) {
				log.error("Exception: {}", e);
				throw new ExchangeExceptionRT(e);
			}
		});

		log.debug("done");
		return walletInfo;
	}

	public BigDecimal[] getWallet(CurrencyEnum currencyEnum) throws ExchangeException {
		BigDecimal[] wallet = new BigDecimal[2];

		log.trace("start. currency: {}", currencyEnum.getShortName());

		if (accountInfo.getWallet().getBalances().containsKey(Currency.getInstance(currencyEnum.getBaseCurrency()))) {
			wallet[BASE_CURRENCY] = accountInfo.getWallet()
					.getBalance(Currency.getInstance(currencyEnum.getBaseCurrency())).getAvailable();
		} else {
			log.error("Invalid base currency: {}", currencyEnum.getBaseCurrency());
			throw new ExchangeException("Invalid base currency: " + currencyEnum.getBaseCurrency());
		}

		if (accountInfo.getWallet().getBalances().containsKey(Currency.getInstance(currencyEnum.getQuoteCurrency()))) {
			wallet[QUOTE_CURRENCY] = accountInfo.getWallet()
					.getBalance(Currency.getInstance(currencyEnum.getQuoteCurrency())).getAvailable();
		} else {
			log.error("Invalid quote currency: {}", currencyEnum.getQuoteCurrency());
			throw new ExchangeException("Invalid quote currency: " + currencyEnum.getQuoteCurrency());
		}

		log.trace("done. wallet: {}", (Object[]) wallet);
		return wallet;
	}

	@Override
	public void run() {
		log.info("start");

		try {
			setThreadInfoData();

			commands.sendMessage("Started account for exchange: " + exchangeEnum.getName(), true);

			threadExecutors.takeTask(exchangeEnum.getName());

		} catch (ExchangeException e) {
			log.error("Exception: {}", e);

			commands.sendMessage("Exception occurred on " + getThreadInfo().getThreadName() + ". Stopping thread",
					true);

			exchangeDisconnect();
		} catch (InterruptedException e) {
			log.debug("thread interrupted");

			commands.sendMessage("Thread " + getThreadInfo().getThreadName() + " interrupted", true);
		}

		log.info("done");
	}
}
