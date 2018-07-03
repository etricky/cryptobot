package com.etricky.cryptobot.core.strategies.common;

import java.time.Duration;
import java.util.Map;

import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Decimal;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TradingRecord;

import com.etricky.cryptobot.core.exchanges.common.ExchangeEnum;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeJson;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.model.TradesEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class StrategyGeneric {
	protected Map<String, ExchangeJson.Strategies> strategiesMap;
	protected Duration barDuration;
	protected String beanName;
	protected Long timeSeriesBars;
	protected TradingRecord globalTradingRecord;
	protected TimeSeries timeSeries;
	protected JsonFiles jsonFiles;
	protected ExchangeEnum exchangeEnum;

	public void setExchangeParameters(ExchangeEnum exchangeEnum, String beanName, JsonFiles jsonFiles) {
		log.debug("start. exchangeEnum: {} beanName: {}", exchangeEnum.getName(), beanName);

		this.exchangeEnum = exchangeEnum;
		this.beanName = beanName;
		this.jsonFiles = jsonFiles;

		loadConfigs();

		log.debug("done");
	}

	public void addTradeToTimeSeries(TradesEntity tradesEntity) {
		log.debug("start");

		BaseBar bar = new BaseBar(barDuration, tradesEntity.getTimestamp(), Decimal.valueOf(tradesEntity.getOpenPrice()),
				Decimal.valueOf(tradesEntity.getHighPrice()), Decimal.valueOf(tradesEntity.getLowPrice()),
				Decimal.valueOf(tradesEntity.getClosePrice()), Decimal.valueOf(1));

		timeSeries.addBar(bar);

		log.debug("done");
	}

	public abstract void processLiveTrade(TradesEntity tradesEntity);

	private void loadConfigs() {
		log.debug("start");

		strategiesMap = jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getStrategiesMap();
		timeSeries = new BaseTimeSeries(beanName);
		timeSeriesBars = strategiesMap.get(beanName).getTimeSeriesBars();
		barDuration = Duration.ofSeconds(strategiesMap.get(beanName).getBarDurationSec());
		timeSeries.setMaximumBarCount(timeSeriesBars.intValue());

		log.debug("done");
	}
}
