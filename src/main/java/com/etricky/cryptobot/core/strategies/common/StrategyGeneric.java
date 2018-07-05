package com.etricky.cryptobot.core.strategies.common;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Decimal;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TradingRecord;

import com.etricky.cryptobot.core.exchanges.common.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeEnum;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeJson;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.model.TradesEntity;
import com.etricky.cryptobot.repositories.OrdersEntityRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class StrategyGeneric {
	@Autowired
	protected JsonFiles jsonFiles;
	@Autowired
	protected OrdersEntityRepository ordersEntityRepository;
	protected ExchangeJson.Strategies strategiesSettings;
	protected String beanName;
	protected TimeSeries timeSeries;
	protected TradingRecord tradingRecord;
	protected Strategy strategy;
	protected ExchangeEnum exchangeEnum;
	protected CurrencyEnum currencyEnum;

	public void setExchangeParameters(ExchangeEnum exchangeEnum, CurrencyEnum currencyEnum, String beanName) {
		log.debug("start. exchangeEnum: {} beanName: {}", exchangeEnum.getName(), beanName);

		this.exchangeEnum = exchangeEnum;
		this.currencyEnum = currencyEnum;
		this.beanName = beanName;

		loadConfigs();

		log.debug("done");
	}

	private void loadConfigs() {
		log.debug("start");

		strategiesSettings = jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getStrategiesMap().get(beanName);

		timeSeries = new BaseTimeSeries(beanName);
		timeSeries.setMaximumBarCount(strategiesSettings.getTimeSeriesBars().intValue());
		tradingRecord = new BaseTradingRecord();

		log.debug("done");
	}

	public void addTradeToTimeSeries(TradesEntity tradesEntity) {
		log.debug("start");

		BaseBar bar = new BaseBar(Duration.ofSeconds(strategiesSettings.getBarDurationSec()), tradesEntity.getTimestamp(),
				Decimal.valueOf(tradesEntity.getOpenPrice()), Decimal.valueOf(tradesEntity.getHighPrice()),
				Decimal.valueOf(tradesEntity.getLowPrice()), Decimal.valueOf(tradesEntity.getClosePrice()), Decimal.valueOf(1));

		timeSeries.addBar(bar);

		log.debug("done");
	}

	public abstract int processLiveTrade(TradesEntity tradesEntity);

	public abstract void closeTrade();

}
