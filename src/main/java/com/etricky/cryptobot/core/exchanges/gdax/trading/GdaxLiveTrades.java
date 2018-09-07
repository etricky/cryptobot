package com.etricky.cryptobot.core.exchanges.gdax.trading;

import java.util.List;
import java.util.Optional;

import org.knowm.xchange.dto.marketdata.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.model.TradeEntity;
import com.etricky.cryptobot.model.TradesData;
import com.etricky.cryptobot.model.pks.ExchangePK;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
public class GdaxLiveTrades {
	@Autowired
	private TradesData tradesData;

	private GdaxTrading gdaxTrading;
	private TradeEntity lastTradeEntity = null;
	private long lastTradeUnixTime = 0;

	public void setGdaxTrading(GdaxTrading gdaxTrading) {
		this.gdaxTrading = gdaxTrading;
	}

	public void processLiveTrade(Trade trade) throws ExchangeException {
		long missingTrades = 0;

		log.debug("start. trade: {}", trade);

		long now = DateFunctions.getUnixTimeNowToEvenMinute();

		if (lastTradeEntity == null) {
			log.debug("first trade of the session");

			mapGdaxTradeToTradeEntity(trade, now);
			lastTradeUnixTime = now;
		} else {
			log.debug("last: {}/{} current: {}/{}", lastTradeUnixTime,
					DateFunctions.getStringFromUnixTime(lastTradeUnixTime), now,
					DateFunctions.getStringFromUnixTime(now));

			if (now - lastTradeUnixTime == 0) {
				log.debug("last trade was in the same minute");

				// updates current trade data
				updateTradeData(trade);
			} else {
				log.debug("first trade of current minute");

				// stores previous trade
				storeTradeData(lastTradeEntity);

				// checks for missing trades
				if (now - lastTradeUnixTime > 60) {
					// removes 60s of the current minute
					missingTrades = (now - lastTradeUnixTime - 60) / 60;

					log.debug("missing trades #: {}", missingTrades);

					if (missingTrades > 2) {
						log.debug("possible network failure has occurred!");

						// gets the last trades from the exchange
						gdaxTrading.processTradeHistory(
								Optional.of(DateFunctions.getZDTfromUnixTime(lastTradeUnixTime)),
								Optional.of(DateFunctions.getZDTfromUnixTime(now)));

						List<TradeEntity> listTradeEntity = tradesData.getTradesInPeriod(
								gdaxTrading.getExchangeEnum().getName(), gdaxTrading.getCurrencyEnum().getShortName(),
								lastTradeUnixTime, now, gdaxTrading.getExchangeJson().getAllowFakeTrades());

						listTradeEntity.forEach(tradeEntity -> {
							storeTradeData(tradeEntity);
						});
					} else {

						// creates fake trade
						TradeEntity fakeTradeEntity = lastTradeEntity.getFake().addMinute();
						for (int i = 0; i < (now - lastTradeUnixTime - 60) / 60; i++) {
							// stores fake trade
							storeTradeData(fakeTradeEntity);
							// creates new fake trade
							fakeTradeEntity = fakeTradeEntity.getFake().addMinute();
						}
					}
				}

				// creates new trade for the current minute
				lastTradeEntity = mapGdaxTradeToTradeEntity(trade, now);
				lastTradeUnixTime = now;
			}
		}

		log.debug("done");
	}

	private void storeTradeData(@NonNull TradeEntity tradeEntity) {
		log.debug("start. trade: {}", tradeEntity);

		// stores the trade in the database
		tradesData.getTradesEntityRepository().save(tradeEntity);

		// executes the trading strategies for the new trade
		gdaxTrading.notifyListeners(tradeEntity, true);

		log.debug("done");
	}

	private void updateTradeData(Trade trade) {
		log.debug("start");

		if (lastTradeEntity.getHighPrice().compareTo(trade.getPrice()) == -1) {
			lastTradeEntity.setHighPrice(trade.getPrice());
			log.debug("new high price: {}", lastTradeEntity.getHighPrice());
		}

		if (lastTradeEntity.getLowPrice().compareTo(trade.getPrice()) == 1) {
			lastTradeEntity.setHighPrice(trade.getPrice());
			log.debug("new low price: {}", lastTradeEntity.getLowPrice());
		}

		if (lastTradeEntity.getClosePrice().compareTo(trade.getPrice()) != 0) {
			lastTradeEntity.setClosePrice(trade.getPrice());
			log.debug("close price: {}", lastTradeEntity.getClosePrice());
		}

		log.debug("done");
	}

	private TradeEntity mapGdaxTradeToTradeEntity(Trade trade, long now) {
		log.trace("start");

		return TradeEntity.builder().openPrice(trade.getPrice()).closePrice(trade.getPrice()).lowPrice(trade.getPrice())
				.highPrice(trade.getPrice()).timestamp(DateFunctions.getZDTfromUnixTime(now))
				.tradeId(ExchangePK.builder().currency(gdaxTrading.getCurrencyEnum().getShortName())
						.exchange(gdaxTrading.getExchangeEnum().getName()).unixtime(now).build())
				.build();

	}
}
