package com.etricky.cryptobot.model;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.repositories.TradesEntityRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CleanOldRecords {

	private TradesEntityRepository tradesEntityRepository;
	private JsonFiles jsonFiles;

	public CleanOldRecords(TradesEntityRepository tradesEntityRepository, JsonFiles jsonFiles) {
		this.tradesEntityRepository = tradesEntityRepository;
		this.jsonFiles = jsonFiles;
	}

	@Scheduled(cron = "0 0 0 * * *") // every day at midnight
	// @Scheduled(cron = "0 * * * * *") // every minute
	@Async
	public void cleanOldRecords() {

		long unixTime = DateFunctions.getUnixTimeFromZDT(
				DateFunctions.getZDTNow().minusDays(new Long(jsonFiles.getSettingsJson().getCleanOldRecords())));
		log.debug("unixTime: {}/{}", unixTime, DateFunctions.getZDTfromUnixTime(unixTime));

		int records = tradesEntityRepository.deleteOldRecords(unixTime);

		log.debug("done. records: {}", records);
	}
}
