package com.etricky.cryptobot.core.exchanges.gdax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.gdax.GDAXExchange;
import org.knowm.xchange.gdax.dto.marketdata.GDAXCandle;
import org.knowm.xchange.gdax.service.GDAXMarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.exchanges.common.ExchangeLock;
import com.etricky.cryptobot.model.TradesEntity;
import com.etricky.cryptobot.model.TradesPK;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
public class GdaxHistoryTrades {

	private GdaxExchange gdaxExchange;

	private ExchangeLock exchangeLock;

	@Autowired
	public void setExchangeLock(ExchangeLock exchangeLock) {
		this.exchangeLock = exchangeLock;
	}

	public void setGdaxExchange(GdaxExchange gdaxExchange) {
		this.gdaxExchange = gdaxExchange;
	}

	@RequiredArgsConstructor
	public static class GdaxTradePeriod {
		@Getter
		@Setter
		@NonNull
		private Long start;

		@Getter
		@Setter
		@NonNull
		private Long end;

	}

	public void processTradeHistory() throws IOException, InterruptedException {
		long startProcessUnixtTime = DateFunctions.getNowUnixTime();
		List<TradesEntity> tradesEntityList = new ArrayList<TradesEntity>();
		List<TradesEntity> gdaxTrades;
		TradesEntity lastTradeEntity = null, fakeTradeEntity;

		log.debug("start. startProcessUnixtTime: {}", startProcessUnixtTime);

		// TODO replace this by a method that returns the gaps and add a loop to fill
		// them
		long tradesNbr = gdaxExchange.getTradesEntityRepository().countTrades(
				gdaxExchange.getThreadInfo().getExchangeEnum().getName(),
				gdaxExchange.getThreadInfo().getCurrencyEnum().getShortName(), startProcessUnixtTime);
		log.debug("tradesNbr: {}", tradesNbr);

		if (tradesNbr == 0) {
			// no trades in history, must get all trades
			log.debug("no trades in history");
			long startPeriod = startProcessUnixtTime
					- (gdaxExchange.getThreadInfo().getExchangeEnum().getHistoryDays() * 86400);
			log.debug("startPeriod: {}/{}", startPeriod, DateFunctions.getStringFromUnixTime(startPeriod));

			try {

				List<GdaxTradePeriod> tradePeriods = getGdaxTradePeriods(startPeriod, startProcessUnixtTime);

				for (GdaxTradePeriod gdaxTradePeriod : tradePeriods) {
					exchangeLock.getLock(gdaxExchange.getThreadInfo().getExchangeEnum().getName());

					// Gdax returns the newest trade first
					gdaxTrades = getGdaxHistoryTrades(gdaxTradePeriod.getStart(), gdaxTradePeriod.getEnd());

					// besides ensuring that gdax rate limit is not reached, it also allows to stop
					// the thread if it's interrupted
					Thread.sleep(1000);
					exchangeLock.releaseLock(gdaxExchange.getThreadInfo().getExchangeEnum().getName());

					log.debug("got {} trades from gdax", gdaxTrades.size());

					// Gdax returns the newest first so must reverse the list
					Collections.reverse(gdaxTrades);

					for (TradesEntity tradeEntity : gdaxTrades) {
						// must store the trades in the database
						log.debug("gdax trade: {}", tradeEntity);

						if (lastTradeEntity != null) {

							while (lastTradeEntity.getTradeId().getUnixtime()
									.longValue() != tradeEntity.getTradeId().getUnixtime().longValue() - 60) {

								log.debug("missing trade. current: {} last: {} #: {}",
										tradeEntity.getTradeId().getUnixtime(),
										lastTradeEntity.getTradeId().getUnixtime(),
										(tradeEntity.getTradeId().getUnixtime()
												- lastTradeEntity.getTradeId().getUnixtime()) / 60);

								// duplicates last
								fakeTradeEntity = lastTradeEntity.getFake();
								fakeTradeEntity.addMinute();
								tradesEntityList.add(fakeTradeEntity);
								lastTradeEntity = fakeTradeEntity;
							}
						}

						log.debug("adding trade to be stored");
						tradesEntityList.add(tradeEntity);
						lastTradeEntity = tradeEntity;
					}

					log.debug("storing {} trades", tradesEntityList.size());
					gdaxExchange.getTradesEntityRepository().saveAll(tradesEntityList);
					tradesEntityList.clear();

				}
			} finally {
				exchangeLock.releaseLock(null);
			}

		}

		log.debug("done");

	}

	private List<TradesEntity> getGdaxHistoryTrades(long startPeriod, long endPeriod) throws IOException {
		Exchange gdax = null;
		GDAXMarketDataService mds = null;

		log.debug("start. startPeriod: {}/{} endPeriod: {}/{}", startPeriod,
				DateFunctions.getStringFromUnixTime(startPeriod), endPeriod,
				DateFunctions.getStringFromUnixTime(endPeriod));

		String startPeriodString = DateFunctions.getStringFromUnixTime(startPeriod);
		String endPeriodString = DateFunctions.getStringFromUnixTime(endPeriod);

		if (gdax == null) {
			log.debug("setting up gdax exchange connection");
			gdax = ExchangeFactory.INSTANCE.createExchange(GDAXExchange.class.getName());
			mds = (GDAXMarketDataService) gdax.getMarketDataService();
		}

		GDAXCandle[] candles = mds.getGDAXHistoricalCandles(
				gdaxExchange.getThreadInfo().getCurrencyEnum().getCurrencyPair(), startPeriodString, endPeriodString,
				"60");

		List<TradesEntity> tradeList = Arrays.asList(candles).stream().map(c -> mapGDAXCandle(c))
				.collect(Collectors.toList());

		log.debug("done");

		return tradeList;
	}

	private TradesEntity mapGDAXCandle(GDAXCandle candle) {
		log.debug("start candle: {}", candle);

		TradesEntity tradeEntity = TradesEntity.builder().openPrice(candle.getOpen()).closePrice(candle.getClose())
				.lowPrice(candle.getLow()).highPrice(candle.getHigh())
				.timestamp(DateFunctions.getZDTFromDate(candle.getTime()))
				.tradeId(TradesPK.builder().currency(gdaxExchange.getThreadInfo().getCurrencyEnum().getShortName())
						.exchange(gdaxExchange.getThreadInfo().getExchangeEnum().getName())
						.unixtime(DateFunctions.getUnixTimeFromdDate(candle.getTime())).build())
				.build();

		log.debug("done. tradeEntity: {}", tradeEntity);

		return tradeEntity;
	}

	public static List<GdaxTradePeriod> getGdaxTradePeriods(long startPeriod, long endPeriod) {
		List<GdaxTradePeriod> tradePeriods = new ArrayList<GdaxTradePeriod>();
		log.debug("start. startPeriod: {}/{} endPeriod: {}/{}", startPeriod,
				DateFunctions.getStringFromUnixTime(startPeriod), endPeriod,
				DateFunctions.getStringFromUnixTime(endPeriod));

		long periods = (endPeriod - startPeriod) / 60 / 300;
		long remainder = (endPeriod - startPeriod) / 60 % 300;
		log.debug("periods: {} remainder: {}", periods, remainder);

		long auxStart = startPeriod, auxEnd;
		for (int i = 0; i < periods; i++) {
			auxEnd = auxStart + 300 * 60;
			tradePeriods.add(new GdaxTradePeriod(auxStart, auxEnd));
			log.debug("trade period start: {}/{} end: {}/{}", auxStart, DateFunctions.getStringFromUnixTime(auxStart),
					auxEnd, DateFunctions.getStringFromUnixTime(auxEnd));
			auxStart = auxEnd;
		}

		if (remainder != 0) {
			tradePeriods.add(new GdaxTradePeriod(auxStart, auxStart + remainder));
			log.debug("final trade period start: {}/{} end: {}/{}", auxStart,
					DateFunctions.getStringFromUnixTime(auxStart), auxStart + remainder,
					DateFunctions.getStringFromUnixTime(auxStart + remainder));
		}

		log.debug("done");

		return tradePeriods;
	}

}
