package com.etricky.cryptobot.core.exchanges.common;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.IOrderFlags;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.StopOrder;
import org.knowm.xchange.service.trade.TradeService;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.core.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.common.exceptions.ExchangeExceptionRT;
import com.etricky.cryptobot.core.common.threads.ThreadInfo;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeOrderTypeEnum;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractExchangeOrders extends AbstractExchange implements Runnable {

	protected Map<String, ExchangeOrderMetaData> activeOrders;
	protected AbstractExchangeAccount abstractExchangeAccount;
	protected TradeService tradeService;

	private String msg;

	public AbstractExchangeOrders(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles) {
		super(exchangeThreads, commands, jsonFiles);
		activeOrders = new HashMap<>();
	}

	public void initialize(ExchangeEnum exchangeEnum, Optional<ThreadInfo> threadInfo,
			AbstractExchangeAccount abstractExchangeAccount, boolean loadOrders) throws ExchangeException {
		log.debug("start. exchange: {} loadOrders: {}", exchangeEnum.getName(), loadOrders);

		this.exchangeEnum = exchangeEnum;
		if (threadInfo.isPresent()) {
			this.threadInfo = threadInfo.get();
		}
		this.abstractExchangeAccount = abstractExchangeAccount;
		tradeService = abstractExchangeAccount.getExchange().getTradeService();

		// initiates the activeOrders map
		if (loadOrders) {
			loadOpenOrders();
		}

		log.debug("done");
	}

	public abstract Set<IOrderFlags> getOrderFlags(ExchangeOrderTypeEnum exchangeOrderOperation);

	/**
	 * Gets the current open orders in the exchange and adds them to the active
	 * orders map
	 * 
	 * @throws ExchangeException
	 */
	private void loadOpenOrders() throws ExchangeException {
		log.debug("start");

		getExchangeOpenOrders().stream().forEach(order -> {
			ExchangeOrderTypeEnum exchangeOrderType;

			Optional<CurrencyEnum> currencyEnum = CurrencyEnum.getInstanceByQuoteBase(
					order.getCurrencyPair().base.getCurrencyCode(), order.getCurrencyPair().counter.getCurrencyCode());

			if (currencyEnum.isPresent()) {
				if (order instanceof LimitOrder) {
					exchangeOrderType = ExchangeOrderTypeEnum.LIMIT;
				} else {
					exchangeOrderType = ExchangeOrderTypeEnum.STOP;
				}

				activeOrders.put(currencyEnum.get().getShortName(),
						ExchangeOrderMetaData.builder().id(order.getId())
								.currencyShortName(currencyEnum.get().getShortName()).orderType(order.getType())
								.exchangeOrderType(exchangeOrderType).build());

				log.trace("added order: {}", activeOrders.get(currencyEnum.get().getShortName()));
			} else {
				log.warn("order for invalid currency pair: {}", order);
			}
		});

		log.debug("done");
	}

	/**
	 * Gets all open orders in the exchange
	 * 
	 * @return All open orders
	 * @throws ExchangeException
	 */
	private List<Order> getExchangeOpenOrders() throws ExchangeException {
		List<Order> openOrders = null;

		log.trace("start");

		try {
			openOrders = tradeService.getOpenOrders().getAllOpenOrders();
		} catch (IOException e) {
			log.error("Exception: {}", e);
			throw new ExchangeException(e);
		}

		log.trace("done");
		return openOrders;
	}

	/**
	 * Creates a message with all the details of the open orders
	 * 
	 * @return Message with the details of all the open orders
	 * @throws ExchangeException
	 */
	public String getExchangeOpenOrdersString() throws ExchangeException {
		log.debug("start");

		msg = "";

		getExchangeOpenOrders().forEach(order -> {
			String aux = "";
			if (order instanceof LimitOrder) {
				aux = "\n\t LimitPrice: " + ((LimitOrder) order).getLimitPrice() + " ";
			} else if (order instanceof StopOrder) {
				aux = "\n\t StopPrice: " + ((StopOrder) order).getStopPrice() + "\n\t LimitPrice: "
						+ ((StopOrder) order).getLimitPrice() + " ";
			}
			msg += order.getClass().getSimpleName() + " " + order.getType() + "\n\t Status: " + order.getStatus()
					+ "\n\t CurrencyPair: " + order.getCurrencyPair() + "\n\t OriginalAmount: "
					+ order.getOriginalAmount() + "\n\t RemainingAmount: " + order.getRemainingAmount()
					+ "\n\t AveragePrice: " + order.getAveragePrice() + aux + "\n\t CumulativeAmount: "
					+ order.getCumulativeAmount() + "\n\t Fee: " + order.getFee() + "\n\t Id: " + order.getId()
					+ "\n\t Leverage: " + order.getLeverage() + "\n\t OrderFlags: " + order.getOrderFlags()
					+ "\n\t Timestamp: " + DateFunctions.getZDTFromDate(order.getTimestamp()) + "\n";
		});

		if (msg.length() == 0 || msg == null) {
			msg = "No open orders";
		}

		log.debug("done");
		return msg;

	}

	/**
	 * Verifies if an order exists for the specified Currency pair
	 * 
	 * @param currencyShortName Currency pair for which the order was placed
	 * @return True if exists, false otherwise
	 * @throws ExchangeException
	 */
	public boolean checkOrderExistsForCurrency(String currencyShortName) throws ExchangeException {
		boolean result;
		log.debug("start. currencyShortName: ", currencyShortName);

		if (activeOrders.containsKey(currencyShortName)) {
			result = true;
		} else {
			result = false;
		}

		log.debug("done. result: {}", result);
		return result;
	}

	/**
	 * Cancels all open orders
	 * 
	 * @return Message indicating the result for each order
	 * @throws ExchangeException
	 */
	public String cancelAllOrders() throws ExchangeException {
		log.debug("start");

		msg = "";

		activeOrders.forEach((curr, order) -> {
			try {
				if (cancelOrder(curr)) {
					msg += "Order canceled: " + order.getId() + "\n";
				} else {
					msg += "Order NOT canceled: " + order.getId() + "\n";
				}

			} catch (ExchangeException e) {
				log.error("Exception: {}", e);
				throw new ExchangeExceptionRT(e);
			}

		});

		if (msg.length() == 0 || msg == null) {
			msg = "No active orders";
		}

		log.debug("done");
		return msg;
	}

	/**
	 * Cancels an open order
	 * 
	 * @param currencyShortName Currency pair for which the order was placed
	 * @return True if order was canceled, false otherwise
	 * @throws ExchangeException
	 */
	public boolean cancelOrder(String currencyShortName) throws ExchangeException {
		boolean result = false;
		log.debug("start. currencyShortName: {}", currencyShortName);

		try {

			if (activeOrders.containsKey(currencyShortName)) {

				result = tradeService.cancelOrder(activeOrders.get(currencyShortName).getId());

				if (result) {
					log.debug("order id: {} has been canceled", activeOrders.get(currencyShortName).getId());
				} else {
					log.error("order id {} has not been canceled!", activeOrders.get(currencyShortName).getId());
					throw new ExchangeException("Order has not been canceled!");
				}
			} else {
				log.warn("Order doesn't exist for currency: {}", currencyShortName);
				activeOrders.remove(currencyShortName);
			}

			log.debug("done. result: {}", result);
			return result;

		} catch (IOException e) {
			log.error("Exception: {}", e);
			throw new ExchangeException(e);
		}
	}

	/**
	 * Creates an order
	 * 
	 * @param exchangeOrderType An order can be:
	 *                          <li>{@link ExchangeOrderTypeEnum#LIMIT} - A limit order lets you set
	 *                          your own price, as well as set some advanced order
	 *                          execution option</li>
	 *                          <li>{@link ExchangeOrderTypeEnum#MARKET} - A market order will
	 *                          execute immediately at the current market price</li>
	 *                          <li>{@link ExchangeOrderTypeEnum#STOP} - A stop order lets you
	 *                          specify the price at which the order should be
	 *                          executed and is useful for stop loss and similar
	 *                          strategies</li>
	 * @param orderType         Type of the order:
	 *                          <li>{@link OrderType#ASK}</li>
	 *                          <li>{@link OrderType#BID}</li>
	 * @param currencyEnum      Base and quote currency
	 * @param amount            Value of the order
	 * @param stopPrice         For Stop orders only, price at which the order
	 *                          should be executed
	 * @param stopLimitPrice    For Stop orders only, will automatically post a
	 *                          limit order at the limit price when the stop price
	 *                          is triggered
	 * @throws ExchangeException
	 */
	public boolean placeOrder(ExchangeOrderTypeEnum exchangeOrderType, OrderType orderType, CurrencyEnum currencyEnum,
			BigDecimal amount, Optional<BigDecimal> stopPrice, Optional<BigDecimal> stopLimitPrice)
			throws ExchangeException {
		String orderId = null;
		BigDecimal limitPrice;
		Set<IOrderFlags> orderFlags = new HashSet<>();
		Optional<ExchangeProductMetaData> productMetaData;
		boolean result = true;

		try {
			log.debug("start. exchangeOrderType: {} orderType: {} amount: {} price: {} ", exchangeOrderType, orderType,
					currencyEnum, amount, stopPrice);

			// validates if already exists an order for the same currency
			if (activeOrders.containsKey(currencyEnum.getShortName())) {
				log.warn("duplicate order");
				commands.sendMessage("Duplicate order for " + exchangeEnum.getName() + " and currency: " + currencyEnum,
						true);
			}

			orderFlags = getOrderFlags(exchangeOrderType);
			productMetaData = abstractExchangeAccount.getProductMetaData(currencyEnum);

			if (amount.compareTo(productMetaData.get().getMaxSize()) == 1
					|| amount.compareTo(productMetaData.get().getMinSize()) == -1) {
				result = false;
			} else {

				switch (exchangeOrderType) {
				case LIMIT:

					// gets the inside bid and ask
					OrderBook orderBook = abstractExchangeAccount.getExchange().getMarketDataService()
							.getOrderBook(currencyEnum.getCurrencyPair(), 1);
					log.debug("OrderBook Buy: {} Sell: {}", orderBook.getBids().get(0).getLimitPrice(),
							orderBook.getAsks().get(0).getLimitPrice());

					if (productMetaData.isPresent()) {

						// a limit order price has to be as close as possible of the best buy/sell in
						// the order book
						if (orderType == OrderType.ASK) {
							limitPrice = NumericFunctions.subtract(orderBook.getBids().get(0).getLimitPrice(),
									productMetaData.get().getMinPrice(), NumericFunctions.PRICE_SCALE);
						} else {
							limitPrice = orderBook.getAsks().get(0).getLimitPrice()
									.add(productMetaData.get().getMinPrice());
						}

						log.debug("limitPrice: {}", limitPrice);

						LimitOrder limitOrder = new LimitOrder.Builder(orderType, currencyEnum.getCurrencyPair())
								.originalAmount(amount).limitPrice(limitPrice).flags(orderFlags).build();
						orderId = tradeService.placeLimitOrder(limitOrder);

					} else {
						log.error("Invalid order on currency: {}", currencyEnum.getShortName());
						throw new ExchangeException("Invalid order on currency: " + currencyEnum.getShortName());
					}

					break;
				case MARKET:
					orderId = tradeService
							.placeMarketOrder(new MarketOrder.Builder(orderType, currencyEnum.getCurrencyPair())
									.originalAmount(amount).flags(orderFlags).build());

					break;
				case STOP:
					if (stopLimitPrice.isPresent()) {
						if (stopPrice.isPresent()) {
							orderId = tradeService
									.placeStopOrder(new StopOrder.Builder(orderType, currencyEnum.getCurrencyPair())
											.originalAmount(amount).limitPrice(stopLimitPrice.get())
											.stopPrice(stopPrice.get()).flags(orderFlags).build());
						} else {
							log.warn("stop order without stopPrice");
							commands.sendMessage("Stop order without stopLimitPrice for " + exchangeEnum.getName()
									+ " and currency: " + currencyEnum, true);
							result = false;
						}
					} else {
						log.warn("stop order without stopLimitPrice");
						commands.sendMessage("Stop order without stopLimitPrice for " + exchangeEnum.getName()
								+ " and currency: " + currencyEnum, true);
						result = false;
					}
				}

				if (result) {
					// stores the new order
					activeOrders.put(currencyEnum.getShortName(),
							ExchangeOrderMetaData.builder().currencyShortName(currencyEnum.getShortName())
									.exchangeOrderType(exchangeOrderType).orderType(orderType).id(orderId).build());

					log.debug("orderId: {}", orderId);
				}
			}

			log.debug("done");
			return result;
		} catch (Exception e) {
			log.error("Exception: {}", e);
			throw new ExchangeException(e);
		}

	}
}
