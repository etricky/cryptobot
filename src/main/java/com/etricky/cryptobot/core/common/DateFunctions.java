package com.etricky.cryptobot.core.common;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateFunctions {

	final static ZoneId UTC = ZoneId.of("UTC");

	public static ZonedDateTime getZDTfromUnixTime(Long unixTime) {
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(unixTime), UTC);
	}

	public static Long getUnixTimeFromZDT(ZonedDateTime zdt) {
		return zdt.toEpochSecond();
	}

	public static Long getUnixTimeFromdDate(Date date) {
		return ZonedDateTime.ofInstant(date.toInstant(), UTC).toEpochSecond();
	}

	public static ZonedDateTime getZDTFromDate(Date date) {
		return ZonedDateTime.ofInstant(date.toInstant(), UTC);
	}

	public static ZonedDateTime getNowZDT() {
		return ZonedDateTime.now(UTC);
	}

	public static long getUnixTimeNow() {
		return ZonedDateTime.now(UTC).toEpochSecond();
	}

	public static long getUnixTimeNowToEvenMinute() {
		long unixTime = getUnixTimeNow();
		return getZDTfromUnixTime(unixTime).minusSeconds(getZDTfromUnixTime(unixTime).getSecond()).toEpochSecond();
	}

	public static String getStringFromZDT(ZonedDateTime zdt) {
		return zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX"));
	}

	public static String getStringFromUnixTime(long unixTime) {
		return getStringFromZDT(getZDTfromUnixTime(unixTime));
	}

}
