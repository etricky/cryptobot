package com.etricky.cryptobot.core.exchanges.common;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Decimal;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TradingRecord;

import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeExceptionRT;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.interfaces.jsonFiles.StrategiesJson;
import com.etricky.cryptobot.core.strategies.StrategyResult;
import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;
import com.etricky.cryptobot.core.strategies.common.TimeSeriesHelper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("exchangeTrade")
@Scope("prototype")
public class ExchangeTrade extends AbstractExchange implements PropertyChangeListener {
	public final static String EXCHANGE_TRADE = "exchangeTrade";

	@Autowired
	private ApplicationContext appContext;

	@Autowired
	protected TimeSeriesHelper timeSeriesHelper;

	@Getter
	private Map<String, AbstractExchangeTrading> exchangeCurrencyTradeMap = new HashMap<>();
	private HashMap<String, AbstractStrategy> strategiesMap;
	@Getter
	private AbstractExchangeOrders exchangeOrders;

	@Getter
	private TimeSeries tradeTimeSeries;
	@Getter
	private TradingRecord tradeTradingRecord;

	private int timeSeriesBar = 0, barDuration = 0;
	@Getter
	private String tradeName;

	public ExchangeTrade(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles) {
		super(exchangeThreads, commands, jsonFiles);
	}

	public void initialize(String tradeName, ExchangeEnum exchangeEnum, AbstractExchangeOrders exchangeOrders) {
		log.debug("start. tradeName: {} exchange: {}", tradeName, exchangeEnum);

		this.exchangeEnum = exchangeEnum;
		this.exchangeOrders = exchangeOrders;
		this.tradeName = tradeName;

		initializeStrategies();

		log.debug("done");
	}

	private void initializeStrategies() {
		log.debug("start");

		tradeTradingRecord = new BaseTradingRecord();
		tradeTimeSeries = new BaseTimeSeries(exchangeEnum.getName());
		strategiesMap = new HashMap<String, AbstractStrategy>();

		jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getTradeConfigsMap().get(tradeName).getStrategies()
				.forEach(strategyBean -> {
					log.debug("creating strategy bean: {} for exchange: {}", strategyBean, exchangeEnum.getName());

					if (jsonFiles.getStrategiesJson().containsKey(strategyBean)) {
						StrategiesJson strategySettings = jsonFiles.getStrategiesJson().get(strategyBean);

						AbstractStrategy abstractStrategy = (AbstractStrategy) appContext.getBean(strategyBean);
						abstractStrategy.initialize(exchangeEnum, strategyBean);

						strategiesMap.put(strategyBean, abstractStrategy);

						if (strategySettings.getTimeSeriesBars().intValue() > timeSeriesBar) {
							timeSeriesBar = strategySettings.getTimeSeriesBars().intValue();
							barDuration = strategySettings.getBarDurationSec().intValue();
						}

						// adds each strategy to the exchange tradings
						exchangeCurrencyTradeMap.values().forEach((exchangeTrading) -> {
							exchangeTrading.addStrategy(strategyBean, abstractStrategy);
						});

					} else {
						log.error("Exchange trade invalid strategy: {}", strategyBean);
						throw new ExchangeExceptionRT("Exchange trade invalid strategy: " + strategyBean);
					}
				});

		log.debug("timeSeriesBar: {} barDuration: {}", timeSeriesBar, barDuration);

		tradeTimeSeries.setMaximumBarCount(timeSeriesBar);

		log.debug("done");
	}

	public void addExchangeTrading(CurrencyEnum currencyEnum, AbstractExchangeTrading abstractExchangeTrading) {
		log.debug("start. exchange: {} currency: {}", abstractExchangeTrading.getExchangeEnum().getName(),
				currencyEnum.getShortName());

		abstractExchangeTrading.addListener(this);
		exchangeCurrencyTradeMap.put(currencyEnum.getShortName(), abstractExchangeTrading);

		log.debug("done");
	}

	@Override
	protected void exchangeDisconnect() {
		// no need to implement on this class
	}

	private void addTradeToTimeSeries(StrategyResult strategyResult) {
		log.debug("start");

		// adds the trade data to tradeTimeSeries
		try {
			if (strategyResult.getBarDuration() == barDuration) {
				timeSeriesHelper.addTradeToTimeSeries(tradeTimeSeries, strategyResult.getTradeEntity(), barDuration);
			}
		} catch (ExchangeException e1) {
			log.error("Exception: {}", e1);
			log.error("trade: {}", strategyResult.getTradeEntity());
			throw new ExchangeExceptionRT(e1);
		}

		log.debug("done");
	}

	public StrategyResult executeTrade(StrategyResult strategyResult, boolean backtest) {
		StrategyResult auxStrategyResult;
		int result = AbstractStrategy.NO_ACTION;
		log.trace("start. backtest: {}", backtest);

		switch (strategyResult.getResult()) {
		case AbstractStrategy.ENTER:
			if (exchangeCurrencyTradeMap.size() == 1) {
				log.trace("single currency trade");

				// TODO create order to buy

				tradeTradingRecord.enter(tradeTimeSeries.getEndIndex(),
						Decimal.valueOf(strategyResult.getTradeEntity().getClosePrice()), Decimal.ZERO);
				result = AbstractStrategy.ENTER;
			} else {
				log.trace("multi currency trade");

				if (tradeTradingRecord.getCurrentTrade().isNew()) {

					// TODO create order to buy

					tradeTradingRecord.enter(tradeTimeSeries.getEndIndex(),
							Decimal.valueOf(strategyResult.getTradeEntity().getClosePrice()), Decimal.ZERO);
					result = AbstractStrategy.ENTER;
				}
			}

			break;
		case AbstractStrategy.EXIT:
			if (exchangeCurrencyTradeMap.size() == 1) {
				log.trace("single currency trade");

				// TODO create order to sell

				tradeTradingRecord.exit(tradeTimeSeries.getEndIndex(),
						Decimal.valueOf(strategyResult.getTradeEntity().getClosePrice()), Decimal.ZERO);
				result = AbstractStrategy.EXIT;
			} else {
				log.trace("multi currency trade");

				if (tradeTradingRecord.getCurrentTrade().isOpened()) {

					// TODO create order to sell

					tradeTradingRecord.exit(tradeTimeSeries.getEndIndex(),
							Decimal.valueOf(strategyResult.getTradeEntity().getClosePrice()), Decimal.ZERO);
					result = AbstractStrategy.EXIT;

				}
			}

			break;
		default:
			log.trace("no buy/sell order required");
			break;
		}

		auxStrategyResult = StrategyResult.builder().result(result).beanName(EXCHANGE_TRADE).barDuration(barDuration)
				.feePercentage(strategyResult.getFeePercentage()).tradeEntity(strategyResult.getTradeEntity())
				.closePrice(strategyResult.getClosePrice()).timeSeriesIndex(tradeTimeSeries.getEndIndex()).build();

		log.trace("done");
		return auxStrategyResult;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		log.trace("start");

		StrategyResult strategyResult = (StrategyResult) evt.getNewValue();
		log.trace("start. source: {} strategy: {} result: {}", evt.getSource().getClass().getName(),
				strategyResult.getBeanName(), strategyResult.getResult());

		if (evt.getPropertyName().equals(AbstractExchangeTrading.PROPERTY_TRADING_RECORD)) {
			executeTrade(strategyResult, false);
		} else if (evt.getPropertyName().equals(AbstractExchangeTrading.PROPERTY_TRADING_RECORD)) {
			addTradeToTimeSeries(strategyResult);
		}

		log.trace("done");
	}
}
