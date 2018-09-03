package com.etricky.cryptobot.core.exchanges.common;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
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
	@Getter
	private StrategyResult strategyResult;
	@Getter
	private String tradeName;

	private int timeSeriesBar = 0, barDuration = 0;
	private boolean backtest;
	private BigDecimal previousBalance = BigDecimal.ZERO, previousAmount = BigDecimal.ZERO, feePercentage;

	public ExchangeTrade(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles) {
		super(exchangeThreads, commands, jsonFiles);
	}

	public void initialize(String tradeName, ExchangeEnum exchangeEnum, AbstractExchangeOrders exchangeOrders,
			boolean backtest) {
		log.debug("start. tradeName: {} exchange: {} backtest: {}", tradeName, exchangeEnum, backtest);

		this.exchangeEnum = exchangeEnum;
		this.exchangeOrders = exchangeOrders;
		this.tradeName = tradeName;
		this.backtest = backtest;
		feePercentage = jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getFee();

		initializeStrategies();

		log.debug("done");
	}

	private void initializeStrategies() {
		log.debug("start");

		tradeTradingRecord = new BaseTradingRecord();
		tradeTimeSeries = new BaseTimeSeries(exchangeEnum.getName());
		strategiesMap = new HashMap<String, AbstractStrategy>();
		boolean multiCurrency = jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getTradeConfigsMap()
				.get(tradeName).getCurrencyPairs().size() > 1 ? true : false;

		jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getTradeConfigsMap().get(tradeName).getStrategies()
				.forEach(strategyBean -> {
					log.debug("creating strategy bean: {} for exchange: {}", strategyBean, exchangeEnum.getName());

					if (jsonFiles.getStrategiesJson().containsKey(strategyBean)) {
						StrategiesJson strategySettings = jsonFiles.getStrategiesJson().get(strategyBean);

						AbstractStrategy abstractStrategy = (AbstractStrategy) appContext.getBean(strategyBean);
						abstractStrategy.initialize(exchangeEnum, strategyBean);

						strategiesMap.put(strategyBean, abstractStrategy);

						if (barDuration == 0 || strategySettings.getBarDurationSec().intValue() < barDuration) {
							timeSeriesBar = strategySettings.getTimeSeriesBars().intValue();
							barDuration = strategySettings.getBarDurationSec().intValue();
						}

						if (multiCurrency && barDuration % strategySettings.getBarDurationSec().intValue() == 0) {
							log.error("barDuration of multicurrency trade must be compatible");
							throw new ExchangeExceptionRT("barDuration of multicurrency trade must be compatible");
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
		log.trace("start");

		// adds the trade data to tradeTimeSeries
		try {
			if (strategyResult.getBarDuration() == barDuration) {
				if (tradeTimeSeries.isEmpty() || tradeTimeSeries.getLastBar().getEndTime()
						.isBefore(strategyResult.getTradeEntity().getTimestamp())) {
					timeSeriesHelper.addTradeToTimeSeries(tradeTimeSeries, strategyResult.getTradeEntity(), barDuration,
							jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getAllowFakeTrades());
				} else {
					log.trace("trade {} before tradeTimeSeries end {}", strategyResult.getTradeEntity().getTimestamp(),
							tradeTimeSeries.getLastBar().getEndTime());
				}
			} else {
				log.trace("different barDuration {}/{}", barDuration, strategyResult.getBarDuration());
			}
		} catch (ExchangeException e1) {
			log.error("Exception: {}", e1);
			log.error("trade: {}", strategyResult.getTradeEntity());
			throw new ExchangeExceptionRT(e1);
		}

		log.trace("done");
	}

	/**
	 * Based on the strategy result for a given currency, it verifies if an order
	 * should be made. This entity keeps its own tradingRecord with all orders,
	 * regardless of the currency. It also has its own timeSeries as it is necessary
	 * for the tradingRecord to add a new trade.
	 * 
	 * If this exchangeTrade only has one currency, the strategy result will trigger
	 * an exchange order. If there are multiple currencies, the following rules
	 * applies:
	 * <ul>
	 * <li>ENTER: if no order or last order is EXIT, a BUY order is issued</li>
	 * <li>EXIT: if last order is ENTER for the same currency, a SELL order is
	 * issued</li>
	 * </ul>
	 * 
	 * @param currencyStrategyResult The result of executing the strategies for a
	 *                               currency
	 * @return a StrategyResult with the orders that have been made to the exchange
	 */
	public StrategyResult executeTrade(StrategyResult currencyStrategyResult) {
		int result = AbstractStrategy.NO_ACTION;
		log.trace("start. backtest: {}", backtest);

		switch (currencyStrategyResult.getResult()) {
		case AbstractStrategy.ENTER:
			if (exchangeCurrencyTradeMap.size() == 1) {
				log.info("ENTER single strategy: {} currency {}", currencyStrategyResult.getStrategyName(),
						currencyStrategyResult.getTradeEntity().getTradeId().getCurrency());

				// TODO create order to buy

				tradeTradingRecord.enter(tradeTimeSeries.getEndIndex(),
						Decimal.valueOf(currencyStrategyResult.getTradeEntity().getClosePrice()), Decimal.ZERO);
				result = AbstractStrategy.ENTER;
			} else {
				if (tradeTradingRecord.getCurrentTrade().isNew()) {
					log.info("ENTER multi strategy: {} currency {}", currencyStrategyResult.getStrategyName(),
							currencyStrategyResult.getTradeEntity().getTradeId().getCurrency());

					// TODO create order to buy

					tradeTradingRecord.enter(tradeTimeSeries.getEndIndex(),
							Decimal.valueOf(currencyStrategyResult.getTradeEntity().getClosePrice()), Decimal.ZERO);
					result = AbstractStrategy.ENTER;
				}
			}

			break;
		case AbstractStrategy.EXIT:
			if (exchangeCurrencyTradeMap.size() == 1) {
				log.info("EXIT single strategy: {} currency {}", currencyStrategyResult.getStrategyName(),
						currencyStrategyResult.getTradeEntity().getTradeId().getCurrency());

				// TODO create order to sell

				tradeTradingRecord.exit(tradeTimeSeries.getEndIndex(),
						Decimal.valueOf(currencyStrategyResult.getTradeEntity().getClosePrice()), Decimal.ZERO);
				result = AbstractStrategy.EXIT;
			} else {
				if (tradeTradingRecord.getCurrentTrade().isOpened()) {
					log.info("EXIT multi strategy: {} currency {}", currencyStrategyResult.getStrategyName(),
							currencyStrategyResult.getTradeEntity().getTradeId().getCurrency());

					// TODO create order to sell

					tradeTradingRecord.exit(tradeTimeSeries.getEndIndex(),
							Decimal.valueOf(currencyStrategyResult.getTradeEntity().getClosePrice()), Decimal.ZERO);
					result = AbstractStrategy.EXIT;
				}
			}

			break;
		default:
			log.trace("no buy/sell order required");
			break;
		}

		if (backtest && currencyStrategyResult.getResult() != AbstractStrategy.NO_ACTION) {
			strategyResult = currencyStrategyResult.toBuilder().build();

			strategyResult.setResult(result);
			strategyResult.setFeePercentage(feePercentage);
			strategyResult.setBalanceAndAmount(previousBalance, previousAmount);

			previousBalance = strategyResult.getBalance();
			previousAmount = strategyResult.getAmount();
		} else {
			strategyResult = currencyStrategyResult;
		}

		log.trace("done");
		return strategyResult;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		log.trace("start");

		StrategyResult auxStrategyResult = (StrategyResult) evt.getNewValue();
		log.trace("start. source: {} strategy: {} result: {}", evt.getSource().getClass().getName(),
				auxStrategyResult.getStrategyName(), auxStrategyResult.getResult());

		if (evt.getPropertyName().equals(AbstractExchangeTrading.PROPERTY_TRADING_RECORD)) {
			executeTrade(auxStrategyResult);
		} else if (evt.getPropertyName().equals(AbstractExchangeTrading.PROPERTY_TIME_SERIES)) {
			addTradeToTimeSeries(auxStrategyResult);
			strategyResult = auxStrategyResult;
		}

		log.trace("done");
	}
}
