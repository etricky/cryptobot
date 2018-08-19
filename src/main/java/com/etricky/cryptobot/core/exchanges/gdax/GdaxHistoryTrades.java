package com.etricky.cryptobot.core.exchanges.gdax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import com.etricky.cryptobot.core.exchanges.common.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.ExchangeExceptionRT;
import com.etricky.cryptobot.core.exchanges.common.ExchangeLock;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeJson;
import com.etricky.cryptobot.model.ExchangePK;
import com.etricky.cryptobot.model.TradesEntity;
import com.etricky.cryptobot.repositories.TradesData;
import com.etricky.cryptobot.repositories.TradesData.TradeGapPeriod;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
public class GdaxHistoryTrades {

	@Autowired
	private ExchangeLock exchangeLock;

	@Autowired
	private TradesData tradesData;

	private GdaxExchange gdaxExchange;
	private ExchangeJson exchangeJson;
	private Exchange gdaxXchange = null;
	private GDAXMarketDataService mds = null;

	private TradesEntity errorTrade;

	public void setGdaxExchange(GdaxExchange gdaxExchange, ExchangeJson exchangeJson) {
		this.gdaxExchange = gdaxExchange;
		this.exchangeJson = exchangeJson;
	}

	@RequiredArgsConstructor
	@Getter
	@Setter
	public static class GdaxTradePeriod {
		private long start;
		private long end;

		public GdaxTradePeriod(long start, long end) {
			this.start = start;
			this.end = end;
		}

	}

	public void processTradeHistory() throws ExchangeException {
		long endPeriod = 0, startPeriod = 0;
		List<TradesEntity> gdaxTrades, tradesEntityList = new ArrayList<TradesEntity>(), auxTradesEntityList = new ArrayList<TradesEntity>();
		TradesEntity lastTradeEntity = null, fakeTradeEntity;
		long fakeTradesCounter = 0;

		log.debug("start");

		endPeriod = DateFunctions.getUnixTimeNowToEvenMinute();
		do {
			if (startPeriod == 0) {
				startPeriod = endPeriod - exchangeJson.getHistoryDays() * 86400;
			} else {
				// adjusts the start period so it won't search since the beginning of the trade
				// history in the next iteration
				startPeriod = endPeriod;
				log.debug("setting startPeriod to previous endPeriod");
				endPeriod = DateFunctions.getUnixTimeNowToEvenMinute();
			}

			log.debug("startPeriod: {}/{} endPeriod: {}/{}", startPeriod, DateFunctions.getStringFromUnixTime(startPeriod), endPeriod,
					DateFunctions.getStringFromUnixTime(endPeriod));

			// retrieves all the gaps present in the database
			Optional<List<TradeGapPeriod>> tradeGapsOpt = tradesData.getTradeGaps(gdaxExchange.getThreadInfo().getExchangeEnum().getName(),
					gdaxExchange.getThreadInfo().getCurrencyEnum().getShortName(), startPeriod, endPeriod);

			if (tradeGapsOpt.isPresent()) {
				List<TradeGapPeriod> tradeGapsList = tradeGapsOpt.get();

				log.debug("gaps #: {}", tradeGapsList.size());

				for (TradeGapPeriod tradeGapPeriod : tradeGapsList) {

					log.debug("gap start: {}/{} end: {}/{}", tradeGapPeriod.getStart(),
							DateFunctions.getStringFromUnixTime(tradeGapPeriod.getStart()), tradeGapPeriod.getEnd(),
							DateFunctions.getStringFromUnixTime(tradeGapPeriod.getEnd()));
					// clears the variable
					lastTradeEntity = null;
					List<GdaxTradePeriod> tradePeriods = getGdaxTradePeriods(tradeGapPeriod.getStart(), tradeGapPeriod.getEnd());

					try {

						for (GdaxTradePeriod gdaxTradePeriod : tradePeriods) {
							exchangeLock.getLock(gdaxExchange.getThreadInfo().getExchangeEnum().getName());

							// besides ensuring that gdax rate limit is not reached, it also allows to stop
							// the thread if it's interrupted
							Thread.sleep(1000);

							// Gdax returns the newest trade first
							gdaxTrades = getGdaxHistoryTrades(gdaxTradePeriod.getStart(), gdaxTradePeriod.getEnd());

							exchangeLock.releaseLock(gdaxExchange.getThreadInfo().getExchangeEnum().getName());

							log.debug("got {} trades from gdax", gdaxTrades.size());

							if (gdaxTrades.size() == 0) {
								// no gaps returned by gdax for the period
								log.trace("gets the previous trade from the database. {}", gdaxTradePeriod.getStart() - 60);

								// retrieves the previous trade from the database to create the fake trade entry
								auxTradesEntityList = tradesData.getTradesInPeriod(gdaxExchange.getThreadInfo().getExchangeEnum().getName(),
										gdaxExchange.getThreadInfo().getCurrencyEnum().getShortName(), gdaxTradePeriod.getStart() - 60,
										gdaxTradePeriod.getStart() - 60, true);

								// creates the fake trades for the gap
								fakeTradeEntity = auxTradesEntityList.get(0).getFake();

								do {
									fakeTradeEntity.addMinute();
									log.debug("adding fake trade: {}", fakeTradeEntity);
									tradesEntityList.add(fakeTradeEntity);
									// generates a new object to be added to tradesEntityList
									fakeTradeEntity = fakeTradeEntity.getFake();
									fakeTradesCounter++;
								} while (fakeTradeEntity.getTradeId().getUnixtime() < gdaxTradePeriod.getEnd() - 60);

							} else {
								// Gdax returns the newest first so must reverse the list
								Collections.reverse(gdaxTrades);

								// verifies if first trade return by gdax matches the start period of the gap
								if (gdaxTradePeriod.getStart() != gdaxTrades.get(0).getTradeId().getUnixtime().longValue()) {
									log.trace("initial {} trades of are missing from gdax",
											(gdaxTrades.get(0).getTradeId().getUnixtime() - gdaxTradePeriod.getStart()) / 60);

									// retrieves the previous trade from the database to create the fake trade entry
									auxTradesEntityList = tradesData.getTradesInPeriod(gdaxExchange.getThreadInfo().getExchangeEnum().getName(),
											gdaxExchange.getThreadInfo().getCurrencyEnum().getShortName(), gdaxTradePeriod.getStart() - 60,
											gdaxTradePeriod.getStart() - 60, true);

									// creates the fake trades for the gap
									if (!auxTradesEntityList.isEmpty()) {
										fakeTradeEntity = auxTradesEntityList.get(0).getFake();

										do {
											fakeTradeEntity.addMinute();
											log.debug("adding fake trade: {}", fakeTradeEntity);
											tradesEntityList.add(fakeTradeEntity);
											fakeTradeEntity = fakeTradeEntity.getFake();
											fakeTradesCounter++;
											lastTradeEntity = fakeTradeEntity;
										} while (fakeTradeEntity.getTradeId().getUnixtime() < gdaxTradePeriod.getStart());
									}
								}

								for (TradesEntity gdaxTrade : gdaxTrades) {
									log.debug("gdax trade: {}", gdaxTrade);

									if (lastTradeEntity != null) {

										while (lastTradeEntity.getTradeId().getUnixtime()
												.longValue() != gdaxTrade.getTradeId().getUnixtime().longValue() - 60) {

											log.trace("missing trade. current: {} last: {} #: {}", gdaxTrade.getTradeId().getUnixtime(),
													lastTradeEntity.getTradeId().getUnixtime(),
													(gdaxTrade.getTradeId().getUnixtime() - lastTradeEntity.getTradeId().getUnixtime()) / 60);

											// duplicates last trade
											fakeTradeEntity = lastTradeEntity.getFake().addMinute();
											log.debug("adding fake trade: {}", fakeTradeEntity);
											tradesEntityList.add(fakeTradeEntity);
											lastTradeEntity = fakeTradeEntity;
											fakeTradesCounter++;
										}
									}

									log.trace("adding trade to be stored");
									tradesEntityList.add(gdaxTrade);
									lastTradeEntity = gdaxTrade;
								}

								// verifies if last trade return by gdax matches the end period of the gap
								if (gdaxTradePeriod.getEnd() - 60 != lastTradeEntity.getTradeId().getUnixtime().longValue()) {
									log.trace("final {} trades of are missing from gdax",
											(gdaxTradePeriod.getEnd() - 60 - lastTradeEntity.getTradeId().getUnixtime()) / 60);

									fakeTradeEntity = lastTradeEntity.getFake();

									do {
										fakeTradeEntity.addMinute();
										log.debug("adding fake trade: {}", fakeTradeEntity);
										tradesEntityList.add(fakeTradeEntity);
										fakeTradeEntity = fakeTradeEntity.getFake();
										fakeTradesCounter++;
										lastTradeEntity = fakeTradeEntity;
									} while (fakeTradeEntity.getTradeId().getUnixtime() < gdaxTradePeriod.getEnd() - 60);
								}
							}

							log.debug("storing {} trades fake: {} gdax: {}", tradesEntityList.size(), fakeTradesCounter, gdaxTrades.size());

							if (tradesEntityList.size() > 300) {
								log.error("storing over 300 trades");
								throw new ExchangeException("GDAX trades per period surpassed");
							}

							tradesData.getTradesEntityRepository().saveAll(tradesEntityList);

							tradesEntityList.clear();
							fakeTradesCounter = 0;
						}
					} catch (InterruptedException | IOException e) {
						log.error("Exception: {}", e);

						throw new ExchangeException(e);

					} finally {
						// just to ensure the lock is released
						exchangeLock.releaseLock(gdaxExchange.getThreadInfo().getExchangeEnum().getName());
					}
				}
			} else {
				log.debug("no trades missing");

			}

			log.debug("loading timeSeries with trades");

			tradesEntityList = tradesData.getTradesInPeriod(gdaxExchange.getThreadInfo().getExchangeEnum().getName(),
					gdaxExchange.getThreadInfo().getCurrencyEnum().getShortName(), startPeriod, endPeriod, false);

			tradesEntityList.forEach(trade -> {
				try {
					gdaxExchange.addHistoryTradeToTimeSeries(trade);
					errorTrade = trade;
				} catch (ExchangeException e1) {
					if (e1.getMessage().equalsIgnoreCase("Cannot add a bar with end time <= to series end time") && errorTrade.equals(trade)) {
						log.error("Igoring invalid bar: " + trade);
					} else {
						log.error("Exception: {}", e1);
						log.error("trade: {}", trade);
						throw new ExchangeExceptionRT(e1);
					}
				}
			});

			tradesEntityList.clear();
			fakeTradesCounter = 0;

			log.debug("now: {} endPeriod: {}", DateFunctions.getUnixTimeNowToEvenMinute(), endPeriod);
		} while (DateFunctions.getUnixTimeNowToEvenMinute() - endPeriod > 60);

		log.debug("done");

	}

	private List<TradesEntity> getGdaxHistoryTrades(long startPeriod, long endPeriod) throws IOException {
		log.debug("start. startPeriod: {}/{} endPeriod: {}/{}", startPeriod, DateFunctions.getStringFromUnixTime(startPeriod), endPeriod,
				DateFunctions.getStringFromUnixTime(endPeriod));

		String startPeriodString = DateFunctions.getStringFromUnixTime(startPeriod, DateFunctions.DATE_FORMAT_EXCHANGE);
		String endPeriodString = DateFunctions.getStringFromUnixTime(endPeriod, DateFunctions.DATE_FORMAT_EXCHANGE);

		if (gdaxXchange == null) {
			log.debug("setting up gdax exchange connection");
			gdaxXchange = ExchangeFactory.INSTANCE.createExchange(GDAXExchange.class.getName());
			mds = (GDAXMarketDataService) gdaxXchange.getMarketDataService();
		}

		GDAXCandle[] candles = mds.getGDAXHistoricalCandles(gdaxExchange.getThreadInfo().getCurrencyEnum().getCurrencyPair(), startPeriodString,
				endPeriodString, "60");

		List<TradesEntity> tradeList = Arrays.asList(candles).stream().map(c -> mapGDAXCandle(c)).collect(Collectors.toList());

		log.debug("done. # of trades returned: {} asked: {}", tradeList.size(), (endPeriod - startPeriod) / 60);

		return tradeList;
	}

	private TradesEntity mapGDAXCandle(GDAXCandle candle) {
		log.trace("start candle: {}", candle);

		TradesEntity tradeEntity = TradesEntity.builder().openPrice(candle.getOpen()).closePrice(candle.getClose()).lowPrice(candle.getLow())
				.highPrice(candle.getHigh()).timestamp(DateFunctions.getZDTFromDate(candle.getTime()))
				.tradeId(ExchangePK.builder().currency(gdaxExchange.getThreadInfo().getCurrencyEnum().getShortName())
						.exchange(gdaxExchange.getThreadInfo().getExchangeEnum().getName())
						.unixtime(DateFunctions.getUnixTimeFromdDate(candle.getTime())).build())
				.build();

		log.trace("done. tradeEntity: {}", tradeEntity);

		return tradeEntity;
	}

	public static List<GdaxTradePeriod> getGdaxTradePeriods(long startPeriod, long endPeriod) {
		List<GdaxTradePeriod> tradePeriods = new ArrayList<GdaxTradePeriod>();

		log.debug("start. startPeriod: {}/{} endPeriod: {}/{}", startPeriod, DateFunctions.getStringFromUnixTime(startPeriod), endPeriod,
				DateFunctions.getStringFromUnixTime(endPeriod));

		long _300minuteBlocks = (endPeriod - startPeriod) / 60 / 300;
		long remainder = (endPeriod - startPeriod) - (300 * 60) * _300minuteBlocks;
		log.debug("periods: {} remainder: {}", _300minuteBlocks, remainder);

		long auxStart = startPeriod, auxEnd;
		for (int i = 0; i < _300minuteBlocks; i++) {
			auxEnd = auxStart + 300 * 60;
			tradePeriods.add(new GdaxTradePeriod(auxStart, auxEnd));
			log.trace("trade period start: {}/{} end: {}/{}", auxStart, DateFunctions.getStringFromUnixTime(auxStart), auxEnd,
					DateFunctions.getStringFromUnixTime(auxEnd));
			auxStart = auxEnd;
		}

		if (remainder != 0) {
			tradePeriods.add(new GdaxTradePeriod(auxStart, auxStart + remainder));
			log.trace("final trade period start: {}/{} end: {}/{}", auxStart, DateFunctions.getStringFromUnixTime(auxStart), auxStart + remainder,
					DateFunctions.getStringFromUnixTime(auxStart + remainder));
		}

		log.debug("done");

		return tradePeriods;
	}
}
