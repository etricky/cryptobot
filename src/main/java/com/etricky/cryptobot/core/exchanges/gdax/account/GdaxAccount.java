package com.etricky.cryptobot.core.exchanges.gdax.account;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.coinbasepro.CoinbaseProExchange;
import org.knowm.xchange.coinbasepro.service.CoinbaseProMarketDataService;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.common.threads.ThreadExecutors;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeAccount;
import com.etricky.cryptobot.core.exchanges.common.ExchangeProductMetaData;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("gdaxAccountBean")
public class GdaxAccount extends AbstractExchangeAccount {

	private Optional<ExchangeProductMetaData> exchangeOrderMetaData;
	private Map<String, Optional<ExchangeProductMetaData>> metaDataMap = new HashMap<>();
	private ZonedDateTime metaDataLastRefresh;

	public GdaxAccount(ExchangeThreads exchangeThreads, JsonFiles jsonFiles, Commands commands,
			ThreadExecutors threadExecutors) throws ExchangeException {
		super(exchangeThreads, commands, jsonFiles, threadExecutors);
	}

	@Override
	public void connectToExchange() throws ExchangeException {
		log.debug("start");

		try {
			ExchangeSpecification exSpec = new CoinbaseProExchange().getDefaultExchangeSpecification();

			exchangeJson = this.jsonFiles.getExchangesJsonMap().get(ExchangeEnum.GDAX.getName());

			if (exchangeJson.getSandbox()) {
				exchangeKeys = jsonFiles.getExchangeKeys(ExchangeEnum.GDAX.getName() + "-sandbox");

				exSpec.setSslUri("https://api-public.sandbox.pro.coinbase.com");
				exSpec.setHost("api-public.sandbox.pro.coinbase.com");
				exSpec.setPort(80);
				exSpec.setExchangeName("CoinbasePro");

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

	/**
	 * Gets the metadata for the exchange products. If it has been obtained for over
	 * 24 hours it will refresh the data.
	 * 
	 * @throws ExchangeException
	 */
	private void getProductsMetaData() throws ExchangeException {
		ZonedDateTime period = DateFunctions.getZDTNow().minusDays(1);
		log.debug("start");

		try {
			if (metaDataLastRefresh == null || metaDataLastRefresh.isBefore(period)) {
				log.debug("refreshing metadata");

				CoinbaseProMarketDataService coinbaseProMarketDataService = (CoinbaseProMarketDataService) exchange
						.getMarketDataService();

				Arrays.stream(coinbaseProMarketDataService.getCoinbaseProProducts()).forEach(product -> {
					ExchangeProductMetaData exchangeOrderMetaData = ExchangeProductMetaData.builder()
							.minSize(product.getBaseMinSize()).maxSize(product.getBaseMaxSize())
							.minPrice(product.getQuoteIncrement()).id(product.getId()).build();

					Optional<CurrencyEnum> currencyEnum = CurrencyEnum.getInstanceByQuoteBase(product.getBaseCurrency(),
							product.getTargetCurrency());

					if (currencyEnum.isPresent()) {

						metaDataMap.put(currencyEnum.get().getShortName(), Optional.of(exchangeOrderMetaData));

						log.debug("adding product to map: {}", exchangeOrderMetaData);
					} else {
						log.debug("skipping product: {}", exchangeOrderMetaData);
					}
				});

				metaDataLastRefresh = DateFunctions.getZDTNow();
			}
		} catch (IOException e) {
			log.error("Exception: {}", e);
			throw new ExchangeException(e);
		}

		log.debug("done");
	}

	@Override
	public Optional<ExchangeProductMetaData> getProductMetaData(CurrencyEnum currencyEnum) throws ExchangeException {
		exchangeOrderMetaData = Optional.empty();

		log.debug("start. currencyEnum: {}", currencyEnum);

		getProductsMetaData();

		if (metaDataMap.containsKey(currencyEnum.getShortName())) {
			exchangeOrderMetaData = metaDataMap.get(currencyEnum.getShortName());
			log.debug("found product: {}", exchangeOrderMetaData.get().toString());
		} else {
			log.debug("no product found");
		}

		log.debug("done");
		return exchangeOrderMetaData;
	}
}
