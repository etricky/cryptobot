package com.etricky.cryptobot.core.strategies.common;

import java.time.Duration;
import java.time.ZonedDateTime;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.BaseBar;
import org.ta4j.core.Decimal;
import org.ta4j.core.TimeSeries;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;
import com.etricky.cryptobot.model.TradeEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
public class TimeSeriesHelper {

	private BaseBar cachedBar;
	private TradeEntity errorTrade;

	private ZonedDateTime calculateBarEndTime(TradeEntity tradesEntity, int barDuration) throws ExchangeException {
		ZonedDateTime returnTime = null;
		int tradeMinute, tradeHour, endTime, barDurationAux;
		log.trace("start. barDuration: {}", barDuration);

		if (barDuration <= 3600) {
			if (3600 % barDuration != 0) {
				throw new ExchangeException("Invalid bar duration: " + barDuration);
			}

			barDurationAux = barDuration / 60; // in minutes
			tradeMinute = tradesEntity.getTimestamp().getMinute();

			for (int i = 0; i < 60 / barDurationAux; i++) {
				if (tradeMinute >= i * barDurationAux && tradeMinute < (i + 1) * barDurationAux) {
					endTime = (i + 1) * barDurationAux;
					returnTime = tradesEntity.getTimestamp().plusMinutes(endTime - tradeMinute);

					log.trace("endTime (m): {} returnTime: {}", endTime, DateFunctions.getStringFromZDT(returnTime));
					break;
				}
			}
		} else if (barDuration <= 86400) {
			if (86400 % barDuration != 0) {
				throw new ExchangeException("Invalid bar duration: " + barDuration);
			}

			barDurationAux = barDuration / 60 / 60; // in hours
			tradeMinute = tradesEntity.getTimestamp().getMinute();
			tradeHour = tradesEntity.getTimestamp().getHour();

			for (int i = 0; i < 86400 / barDuration; i++) {
				if (tradeHour >= i * barDurationAux && tradeHour < (i + 1) * barDurationAux) {
					endTime = (i + 1) * barDurationAux;
					returnTime = tradesEntity.getTimestamp().plusHours(endTime - tradeHour).minusMinutes(tradeMinute);

					log.trace("endTime (h): {} returnTime: {}", endTime, DateFunctions.getStringFromZDT(returnTime));
					break;
				}
			}
		} else {
			// days, weeks or months in multiple of days
			if (barDuration % 86400 != 0) {
				throw new ExchangeException("Invalid bar duration: " + barDuration);
			}

			barDurationAux = barDuration / 60 / 60 / 24 - 1; // in days. Includes the day of the trade
			tradeMinute = tradesEntity.getTimestamp().getMinute();
			tradeHour = tradesEntity.getTimestamp().getHour();
			returnTime = tradesEntity.getTimestamp().minusHours(tradeHour).minusMinutes(tradeMinute)
					.plusDays(barDurationAux);

			log.trace("returnTime: {}", DateFunctions.getStringFromZDT(returnTime));
		}

		log.trace("done");
		return returnTime;
	}

	public boolean addTradeToTimeSeries(TimeSeries timeSeries, TradeEntity tradesEntity, int barDuration)
			throws ExchangeException {
		boolean barAdded = false, cachedBarAdded = false;
		log.trace("start. timeSeries: {} tradesEntity: {} barDuration: {}", timeSeries.getName(), tradesEntity,
				barDuration);

		try {
			if (tradesEntity.isFakeTrade()) {
				log.trace("fake trade, not adding a bar");
			} else {

				if (barDuration > 60) {
					// checks if there's a bar for the current time
					if (cachedBar == null) {

						cachedBar = new BaseBar(Duration.ofSeconds(barDuration),
								calculateBarEndTime(tradesEntity, barDuration),
								Decimal.valueOf(tradesEntity.getOpenPrice()),
								Decimal.valueOf(tradesEntity.getHighPrice()),
								Decimal.valueOf(tradesEntity.getLowPrice()),
								Decimal.valueOf(tradesEntity.getClosePrice()), Decimal.ONE);

						log.trace("created new cached bar. endTime: {}",
								DateFunctions.getStringFromZDT(cachedBar.getEndTime()));
					}

					// verifies if current trade belongs to cached bar or must create a new
					if (tradesEntity.getTimestamp().isBefore(cachedBar.getEndTime())) {
						log.trace("adding trade in cached bar. endTime: {}",
								DateFunctions.getStringFromZDT(cachedBar.getEndTime()));

						cachedBar.addTrade(Decimal.ONE, Decimal.valueOf(tradesEntity.getClosePrice()));

					} else {
						log.trace("adding cached bar to timeseries {}", timeSeries.getName());
						timeSeries.addBar(cachedBar);
						barAdded = true;
						cachedBarAdded = true;

						cachedBar = new BaseBar(Duration.ofSeconds(barDuration),
								calculateBarEndTime(tradesEntity, barDuration),
								Decimal.valueOf(tradesEntity.getOpenPrice()),
								Decimal.valueOf(tradesEntity.getHighPrice()),
								Decimal.valueOf(tradesEntity.getLowPrice()),
								Decimal.valueOf(tradesEntity.getClosePrice()), Decimal.ONE);

						log.trace("created new cached bar. endTime: {}",
								DateFunctions.getStringFromZDT(cachedBar.getEndTime()));
					}
				} else {
					// a trade is created every 60s so it should be added to the timeSeries
					BaseBar bar = new BaseBar(Duration.ofSeconds(barDuration),
							tradesEntity.getTimestamp().plusSeconds(barDuration),
							Decimal.valueOf(tradesEntity.getOpenPrice()), Decimal.valueOf(tradesEntity.getHighPrice()),
							Decimal.valueOf(tradesEntity.getLowPrice()), Decimal.valueOf(tradesEntity.getClosePrice()),
							Decimal.ONE);

					timeSeries.addBar(bar);
					barAdded = true;

					log.trace("added new bar to timeseries {} endTime: {}", timeSeries.getName(),
							DateFunctions.getStringFromZDT(bar.getEndTime()));
				}
			}

			log.trace("done. trade: {} barAdded: {} cached: {}", tradesEntity, barAdded, cachedBarAdded);
			return barAdded;
		} catch (ExchangeException e1) {
			if (e1.getMessage().equalsIgnoreCase("Cannot add a bar with end time <= to series end time")
					&& errorTrade.equals(tradesEntity)) {
				log.warn("Igoring invalid bar: " + tradesEntity);
				return false;
			}

			log.error("Exception: {}", e1);
			log.error("trade: {}", tradesEntity);
			throw e1;
		}
	}
}
