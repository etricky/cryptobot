package com.etricky.cryptobot.core.common;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateFunctions {

	static ZonedDateTime zdt;
	static Long unixTime;
	final static ZoneId UTC = ZoneId.of("UTC");

	public static ZonedDateTime getZDTfromUnixTime(Long unixTime) {
		zdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(unixTime), UTC);
		log.debug("unixTime: {} zdt: {}", unixTime, zdt);
		return zdt;
	}

	public static Long getUnixTimeFromZDT(ZonedDateTime zdt) {
		unixTime = zdt.toEpochSecond();
		log.debug("zdt: {} unixTime: {}", zdt, unixTime);
		return unixTime;
	}

	public static ZonedDateTime getNow() {
		zdt = ZonedDateTime.now(UTC);
		log.debug("zdt: {}", zdt);
		return zdt;
	}
}
