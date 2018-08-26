package com.etricky.cryptobot.core.exchanges.gdax.trading;

import org.knowm.xchange.dto.marketdata.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.model.ExchangePK;
import com.etricky.cryptobot.model.TradeEntity;
import com.etricky.cryptobot.repositories.TradesData;

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

	public void processLiveTrade(Trade trade) {
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
					log.debug("missing trades #: {}", (now - lastTradeUnixTime - 60) / 60);

					// creates fake trade
					TradeEntity fakeTradeEntity = lastTradeEntity.getFake().addMinute();
					for (int i = 0; i < (now - lastTradeUnixTime - 60) / 60; i++) {
						// stores fake trade
						storeTradeData(fakeTradeEntity);
						// creates new fake trade
						fakeTradeEntity = fakeTradeEntity.getFake().addMinute();
					}
				}

				// creates new trade for the current minute
				mapGdaxTradeToTradeEntity(trade, now);
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
		gdaxTrading.processStrategiesForLiveTrade(tradeEntity);

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

	private void mapGdaxTradeToTradeEntity(Trade trade, long now) {
		log.debug("start");

		lastTradeEntity = TradeEntity.builder().openPrice(trade.getPrice()).closePrice(trade.getPrice())
				.lowPrice(trade.getPrice()).highPrice(trade.getPrice()).timestamp(DateFunctions.getZDTfromUnixTime(now))
				.tradeId(ExchangePK.builder().currency(gdaxTrading.getCurrencyEnum().getShortName())
						.exchange(gdaxTrading.getExchangeEnum().getName()).unixtime(now).build())
				.build();

		log.debug("done");
	}
}
