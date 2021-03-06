package com.etricky.cryptobot.core.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateFunctions {

	final static ZoneId UTC = ZoneId.of("UTC");
	public static final String DATE_FORMAT_LOG = "yyyy-MM-dd'T'HH:mmX'['VV']'";
	public static final String DATE_FORMAT_EXCHANGE = "yyyy-MM-dd'T'HH:mmX";

	public static ZonedDateTime getZDTfromUnixTime(long unixTime) {
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

	public static ZonedDateTime getZDTNow() {
		return ZonedDateTime.now(UTC);
	}

	public static long getUnixTimeNow() {
		return ZonedDateTime.now(UTC).toEpochSecond();
	}

	public static long getUnixTimeNowToEvenMinute() {
		long unixTime = getUnixTimeNow();
		return getZDTfromUnixTime(unixTime).minusSeconds(getZDTfromUnixTime(unixTime).getSecond()).toEpochSecond();
	}

	public static String getStringFromZDT(ZonedDateTime zdt, String logFormat) {
		return zdt.format(DateTimeFormatter.ofPattern(logFormat));
	}

	public static String getStringFromZDT(ZonedDateTime zdt) {
		return getStringFromZDT(zdt, DATE_FORMAT_LOG);
	}

	public static String getStringFromUnixTime(long unixTime, String logFormat) {
		return getStringFromZDT(getZDTfromUnixTime(unixTime), logFormat);
	}

	public static String getStringFromUnixTime(long unixTime) {
		return getStringFromUnixTime(unixTime, DATE_FORMAT_LOG);
	}

	public static ZonedDateTime getZDTfromStringDate(String date) throws ParseException {
		return getZDTFromDate(new SimpleDateFormat("yyyy-MM-dd").parse(date));
	}

}
