package com.etricky.cryptobot.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.repositories.TradesEntityRepository;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TradesData {

	@Autowired
	@Getter
	TradesEntityRepository tradesEntityRepository;
	@Autowired
	private JsonFiles jsonFiles;

	@RequiredArgsConstructor
	@Builder
	public static class TradeGapPeriod {
		@Getter
		@Setter
		@NonNull
		private Long start;

		@Getter
		@Setter
		@NonNull
		private Long end;

	}

	private List<TradeGapPeriod> tradeGapList;

	private void addGapToList(long start, long end) {
		log.debug("adding gap start: {}/{} end: {}/{}", start, DateFunctions.getStringFromUnixTime(start), end,
				DateFunctions.getStringFromUnixTime(end));
		tradeGapList.add(TradeGapPeriod.builder().start(start).end(end).build());
	}

	public Optional<List<TradeGapPeriod>> getTradeGaps(String exchange, String currency, long startPeriod,
			long endPeriod) {
		long dataStartPeriod = 0, dataEndPeriod = 0;

		log.debug("start. exhange: {} currency: {} startPeriod: {}/{} endPeriod: {}/{}", exchange, currency,
				startPeriod, DateFunctions.getStringFromUnixTime(startPeriod), endPeriod,
				DateFunctions.getStringFromUnixTime(endPeriod));

		// ensures a clean gap list
		tradeGapList = new ArrayList<>();

		// checks if there's any data
		List<Object[]> dataValues = tradesEntityRepository.getFirstLastTrade(exchange, currency, startPeriod);

		dataStartPeriod = ((BigInteger) dataValues.get(0)[0]).longValue();
		dataEndPeriod = ((BigInteger) dataValues.get(0)[1]).longValue();

		log.debug("dataStartPeriod: {}/{} dataEndPeriod: {}/{}", dataStartPeriod,
				DateFunctions.getStringFromUnixTime(dataStartPeriod), dataEndPeriod,
				DateFunctions.getStringFromUnixTime(dataEndPeriod));

		if (dataStartPeriod == 0) {
			log.debug("no trades in db");

			addGapToList(startPeriod, endPeriod);
		} else {

			if (startPeriod < dataStartPeriod) {
				log.debug("initial gap");

				addGapToList(startPeriod, dataStartPeriod);
			}

			log.debug("trades in db, searching for gaps since startPeriod");

			tradesEntityRepository.getGaps(exchange, currency, dataStartPeriod, dataEndPeriod).ifPresent(gapList -> {
				log.debug("gapList size: {}", gapList.size());
				gapList.forEach(gap -> {
					addGapToList(((BigInteger) gap[0]).longValue(), ((BigInteger) gap[1]).longValue());
				});
			});

			if (dataEndPeriod < endPeriod) {
				log.debug("final gap");

				addGapToList(dataEndPeriod + 60, endPeriod);
			}
		}

		log.debug("done");
		return Optional.ofNullable(tradeGapList);
	}

	public List<TradeEntity> getTradesInPeriod(String exchange, String currency, long startPeriod, long endPeriod,
			boolean includeFake) {
		log.debug("start. exhange: {} currency: {} startPeriod: {}/{} endPeriod: {}/{} includeFake: {}", exchange,
				currency, startPeriod, DateFunctions.getStringFromUnixTime(startPeriod), endPeriod,
				DateFunctions.getStringFromUnixTime(endPeriod), includeFake);
		List<TradeEntity> tradesList;

		if (includeFake) {
			tradesList = tradesEntityRepository.getAllTradesInPeriod(exchange, currency, startPeriod, endPeriod);
		} else {
			tradesList = tradesEntityRepository.getTradesInPeriodNoFake(exchange, currency, startPeriod, endPeriod);
		}

		log.debug("done. tradesList: {}", tradesList.size());
		return tradesList;
	}

	@Scheduled(cron = "0 0 0 * * *") // every day at midnight
	// @Scheduled(cron = "0 * * * * *") // every minute
	@Async
	public void cleanOldRecords() {
		long days = 0;
		int records = 0;

		log.debug("start");

		if (jsonFiles.getSettingsJson().getCleanOldRecords()) {
			days = jsonFiles.getSettingsJson().getNbrOldRecords();
			if (days != 0) {
				long unixTime = DateFunctions.getUnixTimeFromZDT(DateFunctions.getZDTNow().minusDays(days));
				log.debug("unixTime: {}/{}", unixTime, DateFunctions.getZDTfromUnixTime(unixTime));

				records = tradesEntityRepository.deleteOldRecords(unixTime);
			}
		}

		log.debug("done. records: {}", records);
	}
}
