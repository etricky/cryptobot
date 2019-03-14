package com.etricky.cryptobot.core.exchanges.common;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.springframework.beans.factory.annotation.Autowired;

import com.etricky.cryptobot.core.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.common.exceptions.ExchangeExceptionRT;
import com.etricky.cryptobot.core.common.threads.ThreadExecutors;
import com.etricky.cryptobot.core.common.threads.ThreadInfo;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.FiatCurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeJson;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeKeys;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractExchangeAccount extends AbstractExchange implements Runnable {

	@Getter
	protected Exchange exchange;
	protected ExchangeKeys exchangeKeys;
	protected ExchangeJson exchangeJson;
	protected AccountInfo accountInfo;

	private String walletInfo;

	protected ThreadExecutors threadExecutors;
	@Autowired
	@Getter
	protected AbstractExchangeOrders abstractExchangeOrders;

	public AbstractExchangeAccount(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles,
			ThreadExecutors threadExecutors) throws ExchangeException {
		super(exchangeThreads, commands, jsonFiles);
		this.threadExecutors = threadExecutors;

		connectToExchange();
	}

	public abstract void connectToExchange() throws ExchangeException;

	/**
	 * Obtains the metadata of the exchange product for the specified CurrencyEnum.
	 * 
	 * @param currencyEnum The base and quote currency pair
	 * @return A ExchangeOrderMetaData optional
	 * @throws ExchangeException
	 */
	public abstract Optional<ExchangeProductMetaData> getProductMetaData(CurrencyEnum currencyEnum)
			throws ExchangeException;

	public void initialize(ExchangeEnum exchangeEnum, Optional<ThreadInfo> threadInfo, boolean loadOrders)
			throws ExchangeException {
		log.debug("start. exchange: {} loadOrders: {}", exchangeEnum.getName(), loadOrders);

		this.exchangeEnum = exchangeEnum;
		if (threadInfo.isPresent()) {
			this.threadInfo = threadInfo.get();
			threadExecutors.initializeBlockingQueue(exchangeEnum.getName());
		}

		abstractExchangeOrders = (AbstractExchangeOrders) appContext.getBean(exchangeEnum.getOrdersBean());
		abstractExchangeOrders.initialize(exchangeEnum, threadInfo, this, loadOrders);

		log.debug("done");
	}

	Optional<CurrencyPair> currencyPair, aux = Optional.empty();

	public String getWalletInfo() {
		log.debug("start");

		Wallet wallet = accountInfo.getWallet();
		walletInfo = "ID: " + wallet.getId() + "\n";
		Map<Currency, Balance> balanceMap = wallet.getBalances();
		List<CurrencyPair> currencies = exchange.getExchangeSymbols();

		balanceMap.forEach((curr, bal) -> {
			try {
				Ticker ticker;
				currencyPair = Optional.empty();
				aux = Optional.empty();

				walletInfo += "Currency: " + curr + " Amount: " + bal.getAvailable();
				log.debug("Currency: {} Amount: {}", curr, bal.getAvailable());

				currencies.stream().filter(x -> curr.getCurrencyCode().equals(x.base.getCurrencyCode()))
						.forEach(currPair -> {
							aux = Optional.of(currPair);
							if (currPair.counter.equals(FiatCurrencyEnum.EUR.getCurrency())) {
								currencyPair = Optional.of(currPair);
							}
						});

				if (!currencyPair.isPresent()) {
					currencyPair = aux;
				}

				log.debug("currencyPair: {}", currencyPair);

				if (!FiatCurrencyEnum.isFiatCurrency(curr)) {
					log.debug("not fiat currency");
					try {
						ticker = exchange.getMarketDataService().getTicker(currencyPair.get(), (Object[]) null);

						walletInfo += " Last Value: " + ticker.getLast() + "/"
								+ currencyPair.get().counter.getDisplayName() + " Balance: "
								+ bal.getAvailable().multiply(ticker.getLast()) + "\n";
					} catch (Exception e) {
						log.error("Unable to get ticker for {}", currencyPair);
						walletInfo += "\n";
					}
				} else {
					log.debug("fiat currency");
					walletInfo += "\n";
				}

			} catch (Exception e) {
				log.error("Exception: {}", e);
				throw new ExchangeExceptionRT(e);
			}
		});

		log.debug("done");
		return walletInfo;
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
