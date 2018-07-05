package com.etricky.cryptobot.core.strategies.common;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.exchanges.common.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeEnum;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.model.TradesEntity;

import lombok.extern.slf4j.Slf4j;

@Component
@Scope("prototype")
@Slf4j
public class ExchangeStrategy {
	public final static int NO_ACTION = 0;
	public final static int ENTER = 1;
	public final static int EXIT = 2;
	@Autowired
	protected JsonFiles jsonFiles;
	@Autowired
	private ApplicationContext appContext;
	private HashMap<ExchangeEnum, HashMap<CurrencyEnum, StrategyGeneric>> strategiesMap = new HashMap<ExchangeEnum, HashMap<CurrencyEnum, StrategyGeneric>>();
	private boolean closeTrailingStopLoss;
	private ExchangeEnum exchangeEnum;
	private CurrencyEnum currencyEnum;

	public void initializeStrategies(ExchangeEnum exchangeEnum, CurrencyEnum currencyEnum) {
		log.debug("start");

		jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getStrategies().forEach((s) -> {
			log.debug("creating bean: {} for exchange: {} currency: {}", s.getBean(), exchangeEnum.getName(), currencyEnum.getShortName());

			this.exchangeEnum = exchangeEnum;
			this.currencyEnum = currencyEnum;

			StrategyGeneric strategy = (StrategyGeneric) appContext.getBean(s.getBean());
			strategy.setExchangeParameters(exchangeEnum, currencyEnum, s.getBean());

			HashMap<CurrencyEnum, StrategyGeneric> auxCurrencyMap = new HashMap<>();
			auxCurrencyMap.put(currencyEnum, strategy);
			strategiesMap.put(exchangeEnum, auxCurrencyMap);
		});

		log.debug("done");
	}

	public void addTradeToTimeSeries(TradesEntity tradesEntity) {
		log.debug("start");

		strategiesMap.forEach((e, m) -> {
			m.forEach((c, s) -> {
				s.addTradeToTimeSeries(tradesEntity);
			});
		});

		log.debug("done");
	}

	public void processLiveTrade(TradesEntity tradesEntity) {
		log.debug("start");

		strategiesMap.forEach((e, m) -> {
			m.forEach((c, s) -> {
				int result = s.processLiveTrade(tradesEntity);

				if (result == ENTER && s.beanName.equalsIgnoreCase("TradingStrategy")) {
					// most close trailing stop strategy
					closeTrailingStopLoss = true;
				}
			});
		});

		if (closeTrailingStopLoss) {
			log.debug("closing trade in trailing stop loss strategy");

			strategiesMap.get(exchangeEnum).get(currencyEnum).closeTrade();
		}

		log.debug("done");
	}

	public void backTest() {
		// TODO implement this method

		// load trades from BD
	}
}
