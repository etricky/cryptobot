package com.etricky.cryptobot.core.exchanges.common;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.etricky.cryptobot.core.common.threads.ThreadInfo;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.model.TradeEntity;

import info.bitrich.xchangestream.core.StreamingExchange;
import io.reactivex.disposables.Disposable;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractExchangeTrading extends AbstractExchange implements Runnable {

	public static final int TRADE_FULL = 0;
	public static final int TRADE_HISTORY_ONLY = 1;
	public static final int TRADE_DRY_RUN = 2;
	public static final int TRADE_BACKTEST = 3;
	public static final String PROPERTY_LIVE_TRADE = "liveTrade";
	public static final String PROPERTY_HISTORY_TRADE = "historyTrade";

	protected Disposable subscription;
	protected StreamingExchange streamingExchange;
	protected String tradingBean;
	private PropertyChangeSupport liveTradeProperty;

	@Getter
	protected CurrencyEnum currencyEnum;
	@Getter
	protected boolean processingLiveTrades = false;

	public AbstractExchangeTrading(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles) {
		super(exchangeThreads, commands, jsonFiles);
	}

	public void initialize(ExchangeEnum exchangeEnum, CurrencyEnum currencyEnum, ThreadInfo threadInfo) {
		log.debug("start. exchange: {} currency: {} tradeType: {} threadInfo: {}", exchangeEnum, currencyEnum,
				threadInfo);

		this.currencyEnum = currencyEnum;
		this.exchangeEnum = exchangeEnum;
		this.threadInfo = threadInfo;

		liveTradeProperty = new PropertyChangeSupport(this);

		log.debug("done");
	}

	@Override
	protected void exchangeDisconnect() {
		log.debug("start");

		try {
			if (subscription != null && !subscription.isDisposed()) {
				subscription.dispose();
				log.debug("subscription disposed");
			}

			if (streamingExchange != null && streamingExchange.isAlive()) {
				log.debug("disconnect from exchange");
				// Disconnect from exchange (non-blocking)
				streamingExchange.disconnect().subscribe(() -> log.debug("Disconnected from exchange: {} currency: {}",
						exchangeEnum.getName(), currencyEnum.getShortName()));
			} else {
				log.debug("exchange is not alive!");
			}

			exchangeThreads.stopExchangeThreads(exchangeEnum.getName());

			commands.sendMessage("Stopped trading " + currencyEnum.getShortName() + " for exchange: "
					+ exchangeEnum.getTradingBean(), true);

		} catch (Exception e) {
			log.error("Exception: {}", e);
			commands.exceptionHandler(e);
		}

		log.debug("done");
	}

	/*
	 * Methods for the trading strategies
	 * 
	 */
	public void notifyListeners(@NonNull TradeEntity tradeEntity, boolean liveTrade) {
		log.trace("start. liveTrade:{}", liveTrade);

		liveTradeProperty.firePropertyChange(liveTrade ? PROPERTY_LIVE_TRADE : PROPERTY_HISTORY_TRADE, null,
				tradeEntity);

		log.trace("done");
	}

	/*
	 * Methods for the trading listeners
	 * 
	 */
	public void addListener(@NonNull PropertyChangeListener listener, int tradeType) throws ExchangeException {
		log.debug("start. tradeType: {}", tradeType);

		// as the exchange trading object is shared between all trades, if a new trade
		// starts that is incompatible with the current trading it must not start
		if (tradeType == AbstractExchangeTrading.TRADE_FULL) {
			if (historyOnlyTrade) {
				log.error("Incompatible trade type with existing history only trade");
				throw new ExchangeException("Incompatible trade type with existing history only trade");
			}

			historyOnlyTrade = false;
			fullTrade = true;
		} else if (tradeType == AbstractExchangeTrading.TRADE_HISTORY_ONLY
				|| tradeType == AbstractExchangeTrading.TRADE_BACKTEST) {
			if (fullTrade) {
				log.error("Incompatible trade type with existing full trade");
				throw new ExchangeException("Incompatible trade type with existing full trade");
			}
			historyOnlyTrade = true;
			fullTrade = false;
		}

		liveTradeProperty.addPropertyChangeListener(listener);

		log.debug("done");
	}
}
