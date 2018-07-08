package com.etricky.cryptobot.core.strategies.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TradingRecord;

import com.etricky.cryptobot.core.exchanges.common.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeException;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeJson;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.model.TradesEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractStrategy {
	@Autowired
	private JsonFiles jsonFiles;
	@Autowired
	TimeSeriesHelper timeSeriesHelper;
	
	@Getter
	private ExchangeJson.Strategies strategiesSettings;
	@Getter
	private String beanName;
	@Getter
	private TimeSeries timeSeries;
	@Getter
	private int barDuration;
	@Getter
	@Setter
	private Strategy strategy;
	
	public abstract void createStrategy();

	public void initializeStrategy(ExchangeEnum exchangeEnum, String beanName) {
		log.debug("start. exchangeEnum: {} beanName: {}", exchangeEnum.getName(), beanName);

		this.beanName = beanName;

		strategiesSettings = jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getStrategiesMap().get(beanName);
		barDuration = strategiesSettings.getBarDurationSec().intValue();

		timeSeries = new BaseTimeSeries(beanName);
		timeSeries.setMaximumBarCount(strategiesSettings.getTimeSeriesBars().intValue());

		createStrategy();
		
		log.debug("done");
	}

	public void addTradeToTimeSeries(TradesEntity tradesEntity) throws ExchangeException {
		log.debug("start");

		timeSeriesHelper.addTradeToTimeSeries(timeSeries, tradesEntity, barDuration);

		log.debug("done");
	}

	public int processStrategyForLiveTrade(TradesEntity tradesEntity, TradingRecord tradingRecord) throws ExchangeException {
		int result = ExchangeStrategy.NO_ACTION;

		log.debug("start");

		if (timeSeriesHelper.addTradeToTimeSeries(timeSeries, tradesEntity, barDuration)) {
			int endIndex = timeSeries.getEndIndex();
			if (tradingRecord.getCurrentTrade().isNew() && strategy.shouldEnter(endIndex)) {
				// strategy should enter
				log.debug("strategy should ENTER on index {}", endIndex);
				result = ExchangeStrategy.ENTER;

			} else if (tradingRecord.getCurrentTrade().isOpened() && strategy.shouldExit(endIndex)) {
				// strategy should exit
				log.debug("strategy should EXIT on index {}", endIndex);
				result = ExchangeStrategy.EXIT;
			}
		} else {
			log.debug("no bar added");
		}

		log.debug("done. result: {}", result);
		return result;
	}

}
