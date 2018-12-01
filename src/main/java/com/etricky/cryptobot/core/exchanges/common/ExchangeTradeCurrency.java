package com.etricky.cryptobot.core.exchanges.common;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Order.OrderType;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.PrecisionNum;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeExceptionRT;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.StrategyResult;
import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;
import com.etricky.cryptobot.model.TradeEntity;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("exchangeTradeCurrencies")
@Scope("prototype")
public class ExchangeTradeCurrency {
	public final static String BEAN_NAME = "exchangeTradeCurrencies";

	@Autowired
	private JsonFiles jsonFiles;
	@Autowired
	private ApplicationContext appContext;

	@Getter
	private String currencyName;
	private TradingRecord currencyTradingRecord;
	private StrategyResult strategyResult, auxStrategyResult;
	private int lowestBar = 0, tradingRecordEndIndex = 1;
	private Map<String, AbstractStrategy> strategiesMap = new HashMap<>();
	private BigDecimal highPrice = BigDecimal.ZERO, lowPrice = BigDecimal.ZERO, balance = BigDecimal.ZERO,
			amount = BigDecimal.ZERO, feeValue = BigDecimal.ZERO, feePercentage = BigDecimal.ZERO, deltaBalance,
			deltaAmount;
	@Getter
	private BigDecimal exchangeBalance = BigDecimal.ZERO, exchangeAmount = BigDecimal.ZERO;

	public void initialize(String exchangeName, String currencyName, String tradeName) {
		log.debug("start. exchange: {} tradeName: {} currency: {}", exchangeName, currencyName, tradeName);

		this.currencyName = currencyName;
		feePercentage = jsonFiles.getExchangesJsonMap().get(exchangeName).getFee();
		this.currencyTradingRecord = new BaseTradingRecord();

		jsonFiles.getExchangesJsonMap().get(exchangeName).getTradeConfigsMap().get(tradeName).getStrategies()
				.forEach(tradeStrategy -> {

					if (tradeStrategy.getCurrencyPairs().contains(currencyName)) {

						if (jsonFiles.getStrategiesJsonMap().containsKey(tradeStrategy.getStrategyName())) {

							// for each currency a strategy object must be created as they store a
							// timeSeries
							log.debug("creating strategy bean: {} for exchange: {} currency: {}",
									tradeStrategy.getStrategyName(), exchangeName, currencyName);

							AbstractStrategy abstractStrategy = (AbstractStrategy) appContext
									.getBean(tradeStrategy.getStrategyName());
							abstractStrategy.initialize(ExchangeEnum.getInstanceByName(exchangeName).get(),
									tradeStrategy.getStrategyName());

							strategiesMap.putIfAbsent(tradeStrategy.getStrategyName(), abstractStrategy);
						} else {
							log.error("Exchange trade invalid strategy: {}", tradeStrategy.getStrategyName());
							throw new ExchangeExceptionRT(
									"Exchange trade invalid strategy: " + tradeStrategy.getStrategyName());
						}
					} else {
						log.debug("strategy {} not applied to currency {}", tradeStrategy.getStrategyName(),
								currencyName);
					}

				});
		log.debug("done");
	}

	/**
	 * Adds a trade to the timeSeries of the strategy, currency and notifies the
	 * exchnangeTrades
	 * 
	 * @param tradeEntity The trade values reported by the exchange
	 */
	public void addTradeToTimeSeries(TradeEntity tradeEntity) {
		// used for historic trades
		log.trace("start. currency: {}", currencyName);

		strategiesMap.values().forEach(strategy -> {
			try {
				strategy.addTradeToTimeSeries(tradeEntity);
			} catch (ExchangeException e) {
				log.error("Exception: {}", e);
				throw new ExchangeExceptionRT(e);
			}
		});

		log.trace("done");
	}

	private void calculateBalanceAndAmount(TradeEntity tradeEntity, BigDecimal exchangeBalance,
			BigDecimal exchangeAmount) {
		OrderType currencyOrderType;

		log.trace("start. currency: {} balance: {} amount: {} exchangeBalance: {} exchangeAmount:{}",
				tradeEntity.getTradeId().getCurrency(), balance, amount, exchangeBalance, exchangeAmount);

		if (currencyTradingRecord.getLastOrder() == null
				|| currencyTradingRecord.getLastOrder().getType().equals(OrderType.SELL)) {
			currencyOrderType = OrderType.BUY;
		} else {
			currencyOrderType = OrderType.SELL;
		}

		if (currencyOrderType == OrderType.BUY) {

			balance = exchangeBalance;
			feeValue = NumericFunctions.percentage(feePercentage, balance, false);
			// available balance to be used in the buy
			balance = NumericFunctions.subtract(balance, feeValue, NumericFunctions.BALANCE_SCALE);
			amount = NumericFunctions.divide(balance, tradeEntity.getClosePrice(), NumericFunctions.AMOUNT_SCALE);

		} else if (currencyOrderType == OrderType.SELL) {
			amount = exchangeAmount;
			balance = amount.multiply(tradeEntity.getClosePrice());
			feeValue = NumericFunctions.percentage(feePercentage, balance, false);
			balance = NumericFunctions.subtract(balance, feeValue, NumericFunctions.BALANCE_SCALE);

		}

		deltaBalance = NumericFunctions.subtract(balance, exchangeBalance, NumericFunctions.BALANCE_SCALE);
		deltaAmount = NumericFunctions.subtract(amount, exchangeAmount, NumericFunctions.AMOUNT_SCALE);

		if (highPrice.compareTo(tradeEntity.getClosePrice()) < 0) {
			highPrice = tradeEntity.getClosePrice();
		}

		if (lowPrice.compareTo(tradeEntity.getClosePrice()) > 0 || lowPrice.equals(BigDecimal.ZERO)) {
			lowPrice = tradeEntity.getClosePrice();
		}

		log.trace("order: {} currency: {} balance: {}/{} amount: {}/{} feeValue: {}", currencyOrderType.name(),
				tradeEntity.getTradeId().getCurrency(), balance, deltaBalance, amount, deltaAmount, feeValue);
		log.trace("highPrice: {} lowPrice: {} exchangeBalance: {} exchangeAmount: {}", highPrice, lowPrice,
				exchangeBalance, exchangeAmount);
	}

	public void newTradingOrder(int orderType, String currencyName, BigDecimal closePrice, BigDecimal amount,
			String strategyName) {

		if (orderType == AbstractStrategy.ENTER) {
			currencyTradingRecord.enter(tradingRecordEndIndex++, PrecisionNum.valueOf(closePrice),
					PrecisionNum.valueOf(amount));
		} else {
			currencyTradingRecord.exit(tradingRecordEndIndex++, PrecisionNum.valueOf(closePrice),
					PrecisionNum.valueOf(amount));
		}

		log.debug("{} -> strategy {} currency: {} price: {} index c: {}",
				AbstractStrategy.getOrderTypeString(orderType), strategyName, currencyName, closePrice,
				tradingRecordEndIndex);
	}

	/**
	 * For each strategy, adds the trade to the strategy timeSeries and then
	 * executes the strategy. The execution uses the strategy timeSeries and the
	 * currency tradingRecord. It then returns the strategy result to be used by the
	 * ExchangeTrade and is this entity that decides if an order to the exchange
	 * should be made.
	 * 
	 * @param tradeEntity           The trade values reported by the exchange
	 * @param exchangeBalance       Balance available on the exchange
	 * @param exchangeAmount        Amount available on the exchange
	 * @param exchangeLastOrderType The type of the last order executed on the
	 *                              exchange for this trade
	 */
	public StrategyResult processStrategiesForLiveTrade(TradeEntity tradeEntity, BigDecimal exchangeBalance,
			BigDecimal exchangeAmount, OrderType exchangeLastOrderType) {

		lowestBar = 0;
		this.exchangeAmount = exchangeAmount;
		this.exchangeBalance = exchangeBalance;

		log.trace("start. tradeEntity:{}", tradeEntity);
		log.trace("start. currency: {} balance: {} amount: {} exchangeBalance: {} exchangeAmount: {} lastOrderType: {}",
				currencyName, balance, amount, exchangeBalance, exchangeAmount);

		calculateBalanceAndAmount(tradeEntity, exchangeBalance, exchangeAmount);

		strategyResult = StrategyResult.builder().balance(balance).amount(amount).build();

		strategiesMap.values().forEach(strategy -> {
			try {
				auxStrategyResult = strategy.processStrategy(tradeEntity, currencyTradingRecord, tradingRecordEndIndex,
						balance, amount, feeValue, highPrice, lowPrice, this.exchangeBalance, this.exchangeAmount,
						exchangeLastOrderType);
			} catch (ExchangeException e) {
				log.error("Exception: {}", e);
				throw new ExchangeExceptionRT(e);
			}

			// as the strategy with highest cadence (lowest bar duration) responds quicker
			// to changes it has a higher priority over strategies with lowest cadence
			if (lowestBar < auxStrategyResult.getBarDuration()
					&& auxStrategyResult.getResult() != AbstractStrategy.NO_ACTION) {
				strategyResult = auxStrategyResult;
				lowestBar = auxStrategyResult.getBarDuration();
			}
		});

		if (strategyResult.getResult() != AbstractStrategy.NO_ACTION) {
			highPrice = tradeEntity.getClosePrice();
			lowPrice = tradeEntity.getClosePrice();

			log.trace("order: {} balance: {}/{} amount: {}/{} feeValue: {} highPrice: {} lowPrice: {}",
					AbstractStrategy.getOrderTypeString(strategyResult.getResult()), balance, deltaBalance, amount,
					deltaAmount, feeValue, highPrice, lowPrice);
		}

		log.trace("done. highPrice: {} lowPrice: {}", highPrice, lowPrice);
		return strategyResult;
	}
}
