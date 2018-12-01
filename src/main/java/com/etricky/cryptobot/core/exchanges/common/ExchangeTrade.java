package com.etricky.cryptobot.core.exchanges.common;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Order.OrderType;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.PrecisionNum;

import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeExceptionRT;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.StrategyResult;
import com.etricky.cryptobot.core.strategies.common.AbstractStrategy;
import com.etricky.cryptobot.model.TradeEntity;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("exchangeTrade")
@Scope("prototype")
public class ExchangeTrade extends AbstractExchange implements PropertyChangeListener {
	public final static String EXCHANGE_TRADE = "exchangeTrade";
	public final static int TRADE = 1, CURRENCY = 0;
	@Autowired
	private ApplicationContext appContext;

	@Getter
	private Map<String, ExchangeTradeCurrency> exchangeTradeCurrencyMap = new HashMap<>();
	@Getter
	private Map<String, AbstractExchangeTrading> exchangeTradingMap = new HashMap<>();
	@Getter
	private AbstractExchangeAccount exchangeAccount;
	@Getter
	private TradingRecord tradeTradingRecord;
	@Getter
	private StrategyResult[] returnStrategyResult;
	@Getter
	private String tradeName;

	private int tradingRecordEndIndex = 1, tradeType;
	private String previousEntryOrderCurrency, quoteCurrency;
	private boolean multiCurrency, liveTrade;
	private BigDecimal exchangeBalance = BigDecimal.ZERO, exchangeAmount = BigDecimal.ZERO;

	public ExchangeTrade(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles) {
		super(exchangeThreads, commands, jsonFiles);
	}

	/**
	 * Initializes internal variables
	 * 
	 * @param tradeName       Name of the trade strategy
	 * @param exchangeEnum    Exchange that will be used
	 * @param exchangeAccount AbstractExchangeAccount object which holds the
	 *                        connection to the exchange
	 * @param tradeType       Type of trading that should be performed:
	 *                        <ul>
	 *                        <li>FULL: adds past trades to the database, process
	 *                        all strategies for the trade and issues exchange
	 *                        orders</li>
	 *                        <li>DRY_RUN: adds past trades to the database, process
	 *                        all strategies but does not issues exchange
	 *                        orders</li>
	 *                        <li>HISTORY_ONLY: only adds past trades to the
	 *                        database</li>
	 *                        <li>BACKTEST: backtest scenario where all strategies
	 *                        are processed but no past trades are added to the
	 *                        database and no exchange orders are issued</li>
	 *                        </ul>
	 */
	public void initialize(String tradeName, ExchangeEnum exchangeEnum, AbstractExchangeAccount exchangeAccount,
			int tradeType) {
		log.debug("start. tradeName: {} exchange: {} tradeType: {}", tradeName, exchangeEnum.getName(), tradeType);

		this.exchangeEnum = exchangeEnum;
		this.exchangeAccount = exchangeAccount;
		this.tradeName = tradeName;
		this.tradeType = tradeType;

		if (tradeType == AbstractExchangeTrading.TRADE_TYPE_FULL) {
			dryRunTrade = false;
			historyOnlyTrade = false;
			fullTrade = true;
			backtestTrade = false;
		} else if (tradeType == AbstractExchangeTrading.TRADE_TYPE_DRY_RUN) {
			dryRunTrade = true;
			historyOnlyTrade = false;
			fullTrade = false;
			backtestTrade = false;
		} else if (tradeType == AbstractExchangeTrading.TRADE_TYPE_HISTORY_ONLY) {
			dryRunTrade = false;
			historyOnlyTrade = true;
			fullTrade = false;
			backtestTrade = false;
		} else if (tradeType == AbstractExchangeTrading.TRADE_BACKTEST) {
			dryRunTrade = false;
			historyOnlyTrade = false;
			fullTrade = false;
			backtestTrade = true;
			exchangeBalance = BigDecimal.valueOf(100);
		}

		initializeCurrencies();

		log.debug("done. dryRunTrade: {} historyOnlyTrade: {} fullTrade: {} backtestTrade: {}", dryRunTrade,
				historyOnlyTrade, fullTrade, backtestTrade);
	}

	private void initializeCurrencies() {
		log.debug("start");

		tradeTradingRecord = new BaseTradingRecord();
		multiCurrency = jsonFiles.getExchangesJsonMap().get(exchangeEnum.getName()).getTradeConfigsMap().get(tradeName)
				.getCurrencyPairs().size() > 1 ? true : false;
		quoteCurrency = null;

		jsonFiles.getExchangesJsonMap().get(exchangeEnum.getName()).getTradeConfigsMap().get(tradeName)
				.getCurrencyPairs().forEach(currencyName -> {
					log.debug("processing currency: {}", currencyName);

					if (quoteCurrency == null || CurrencyEnum.getInstanceByShortName(currencyName).get()
							.getQuoteCurrency().equalsIgnoreCase(quoteCurrency)) {

						ExchangeTradeCurrency exchangeTradeCurrency = (ExchangeTradeCurrency) appContext
								.getBean(ExchangeTradeCurrency.BEAN_NAME);

						exchangeTradeCurrency.initialize(exchangeEnum.getName(), currencyName, tradeName);

						exchangeTradeCurrencyMap.put(currencyName, exchangeTradeCurrency);

						quoteCurrency = CurrencyEnum.getInstanceByShortName(currencyName).get().getQuoteCurrency();

						// TODO must provide the currency balance
					} else {
						log.error("Quote currency mismatch. quoteCurrency: {} vs {} for currency: {}", quoteCurrency,
								CurrencyEnum.getInstanceByShortName(currencyName).get().getQuoteCurrency(),
								currencyName);
						throw new ExchangeExceptionRT("Quote currency mismatch. quoteCurrency: " + quoteCurrency
								+ " vs " + CurrencyEnum.getInstanceByShortName(currencyName).get().getQuoteCurrency()
								+ " for currency: " + currencyName);
					}
				});

		log.debug("done");
	}

	/**
	 * Adds a new AbstractExchangeTrading object to the trade. It also registers
	 * this trade as a listener for new exchange trades processed by the
	 * AbstractExchangeTrading object
	 * 
	 * @param currencyEnum            Currency being processed
	 * @param abstractExchangeTrading Abstract class object that processes exchange
	 *                                trades
	 * @throws ExchangeException
	 */
	public void addExchangeTrading(CurrencyEnum currencyEnum, AbstractExchangeTrading abstractExchangeTrading)
			throws ExchangeException {
		log.debug("start. exchange: {} currency: {}", abstractExchangeTrading.getExchangeEnum().getName(),
				currencyEnum.getShortName());

		abstractExchangeTrading.addListener(this, tradeType);
		exchangeTradingMap.put(currencyEnum.getShortName(), abstractExchangeTrading);

		log.debug("done");
	}

	@Override
	protected void exchangeDisconnect() {
		// no need to implement on this class
	}

	/**
	 * Adds the trade to the time series used by the strategies applied to the
	 * currency
	 * 
	 * @param tradeEntity The trade values reported by the exchange
	 */
	private void addTradeToTimeSeries(TradeEntity tradeEntity) {
		log.trace("start");

		// adds the trade to the currency TimeSeries which, in turn, adds to the
		// strategies TimeSeries
		exchangeTradeCurrencyMap.get(tradeEntity.getTradeId().getCurrency()).addTradeToTimeSeries(tradeEntity);

		log.trace("done");
	}

	/**
	 * Creates a new exchange order and updates the trade trading record and the
	 * currency trading record
	 * 
	 * @param orderType    BUY or SELL order
	 * @param currencyName Currency that was processed
	 * @param price        Currency price that triggered the order
	 * @param strategyName Strategy that triggered the order
	 */
	public void newTradingOrder(int orderType, String currencyName, BigDecimal price, String strategyName) {

		log.trace("start. orderType:{} currency: {} price: {}", orderType, currencyName, price);

		// adds the order to the exchange trading record
		if (orderType == AbstractStrategy.ENTER) {
			tradeTradingRecord.enter(tradingRecordEndIndex++, PrecisionNum.valueOf(price),
					PrecisionNum.valueOf(exchangeAmount));
		} else {
			tradeTradingRecord.exit(tradingRecordEndIndex++, PrecisionNum.valueOf(price),
					PrecisionNum.valueOf(exchangeAmount));
		}

		log.info("{} strategy: {} currency {} balance: {} amount: {}", AbstractStrategy.getOrderTypeString(orderType),
				strategyName, currencyName, exchangeBalance, exchangeAmount);

		// updates the currency trading record with the new trade
		exchangeTradeCurrencyMap.get(currencyName).newTradingOrder(orderType, currencyName, price, exchangeAmount,
				strategyName);

		log.debug("----------");

		log.trace("done");
	}

	/**
	 * Verifies if all currencies or a specific currency is processing live trades
	 * 
	 * @param currencyName Optional parameter with the Currency to be verified. If
	 *                     not present, it will verify if all currencies are already
	 *                     processing live trades
	 * @return If a currency is specified, returns if that currency trading is
	 *         processing or not live trades. If a currency is not specified,
	 *         returns if all trades are processing or not live trades
	 */
	private boolean checkLiveTrading(Optional<String> currencyName) {
		log.trace("start. currencyName: {}", currencyName);

		if (backtestTrade) {
			log.trace("setting live trade for backtest");
			liveTrade = true;
		} else {
			if (currencyName.isPresent()) {
				liveTrade = exchangeTradingMap.get(currencyName.get()).isProcessingLiveTrades();
			} else {
				liveTrade = true;

				exchangeTradingMap.values().forEach(abstractTrading -> {
					if (!abstractTrading.isProcessingLiveTrades()) {
						liveTrade = false;
						log.trace("currency {} NOT on live trades", abstractTrading.getCurrencyEnum().getShortName());
					} else {
						log.trace("currency {} ON live trades", abstractTrading.getCurrencyEnum().getShortName());
					}
				});
			}
		}
		log.trace("done. liveTrade: {}", liveTrade);
		return liveTrade;
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
	 * @param tradeEntity The trade values reported by the exchange
	 * @return a StrategyResult with the orders that have been made to the exchange
	 */
	public StrategyResult[] evaluateTrade(TradeEntity tradeEntity) {
		int result = AbstractStrategy.NO_ACTION;
		OrderType exchangeLastOrderType;

		log.trace("start. backtest: {}", backtestTrade);

		returnStrategyResult = new StrategyResult[2];

		if (tradeTradingRecord.getLastOrder() == null
				|| tradeTradingRecord.getLastOrder().getType().equals(OrderType.BUY)) {
			exchangeLastOrderType = OrderType.BUY;
		} else {
			exchangeLastOrderType = OrderType.SELL;
		}

		if (checkLiveTrading(Optional.of(tradeEntity.getTradeId().getCurrency()))) {
			// process the strategies
			returnStrategyResult[CURRENCY] = exchangeTradeCurrencyMap.get(tradeEntity.getTradeId().getCurrency())
					.processStrategiesForLiveTrade(tradeEntity, exchangeBalance, exchangeAmount, exchangeLastOrderType);
		} else {
			log.trace("currency not processing live trade");
			addTradeToTimeSeries(tradeEntity);
			returnStrategyResult[CURRENCY] = StrategyResult.builder().result(AbstractStrategy.NO_ACTION).build();
		}

		// verifies if all currency are already processing live trades
		if (checkLiveTrading(Optional.empty())) {

			switch (returnStrategyResult[CURRENCY].getResult()) {
			case AbstractStrategy.ENTER:
				if (tradeTradingRecord.getCurrentTrade().isNew()) {
					if (!multiCurrency) {

						if (!dryRunTrade) {
							// TODO create order to buy
						} else {
							log.trace("not putting order to exchange - DRY_RUN");
						}

						result = AbstractStrategy.ENTER;
					} else {
						if (!dryRunTrade) {
							// TODO create order to buy
						} else {
							log.trace("not putting order to exchange - DRY_RUN");
						}

						result = AbstractStrategy.ENTER;

						previousEntryOrderCurrency = tradeEntity.getTradeId().getCurrency();
					}
				}

				break;
			case AbstractStrategy.EXIT:
				if (tradeTradingRecord.getCurrentTrade().isOpened()) {
					if (!multiCurrency) {

						if (!dryRunTrade) {
							// TODO create order to sell
						} else {
							log.trace("not putting order to exchange - DRY_RUN");
						}

						result = AbstractStrategy.EXIT;
					} else {
						if (previousEntryOrderCurrency.equals(tradeEntity.getTradeId().getCurrency())) {

							if (!dryRunTrade) {
								// TODO create order to sell
							} else {
								log.trace("not putting order to exchange - DRY_RUN");
							}

							result = AbstractStrategy.EXIT;
						} else {
							log.trace("exit is for different currency of last buy");
						}
					}
				}

				break;
			default:
				log.trace("no buy/sell order required");
				break;
			}
		} else {
			log.trace("not all currencies on live trades");
		}

		if (backtestTrade) {
			if (result != AbstractStrategy.NO_ACTION) {
				exchangeAmount = returnStrategyResult[CURRENCY].getAmount();
				exchangeBalance = returnStrategyResult[CURRENCY].getBalance();

				newTradingOrder(result, tradeEntity.getTradeId().getCurrency(), tradeEntity.getClosePrice(),
						returnStrategyResult[CURRENCY].getStrategyName());

				returnStrategyResult[CURRENCY].setLastOrder(tradeTradingRecord.getLastOrder());

				// copies the strategyResult to a new object to be returned
				returnStrategyResult[TRADE] = returnStrategyResult[CURRENCY].toBuilder().build();
				returnStrategyResult[TRADE].setResult(result);

			} else {
				// no trade was made with this currency
				returnStrategyResult[CURRENCY].setResult(AbstractStrategy.NO_ACTION);
			}
		}

		log.trace("done");
		return returnStrategyResult;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		log.trace("start");

		TradeEntity tradeEntity = (TradeEntity) evt.getNewValue();
		log.trace("start. source: {}", evt.getSource().getClass().getName());

		if (evt.getPropertyName().equals(AbstractExchangeTrading.PROPERTY_LIVE_TRADE)) {
			evaluateTrade(tradeEntity);
		} else if (evt.getPropertyName().equals(AbstractExchangeTrading.PROPERTY_HISTORY_TRADE)) {
			addTradeToTimeSeries(tradeEntity);
		}

		log.trace("done");
	}
}
