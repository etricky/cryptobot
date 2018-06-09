package com.etricky.cryptobot.repositories;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.DateFunctions;

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

	private List<TradeGapPeriod> tradeGapList = new ArrayList<>();

	private void addGapToList(long start, long end) {
		log.debug("adding gap start: {}/{} end: {}/{}", start, DateFunctions.getStringFromUnixTime(start), end,
				DateFunctions.getStringFromUnixTime(end));
		tradeGapList.add(TradeGapPeriod.builder().start(start).end(end).build());
	}

	public Optional<List<TradeGapPeriod>> getTradeGap(String exchange, String currency, long startPeriod,
			long endPeriod) {
		long dataStartPeriod = 0, dataEndPeriod = 0;

		log.debug("start. exhange: {} currency: {} startPeriod: {}/{} endPeriod: {}/{}", exchange, currency,
				startPeriod, DateFunctions.getStringFromUnixTime(startPeriod), endPeriod,
				DateFunctions.getStringFromUnixTime(endPeriod));

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
}
