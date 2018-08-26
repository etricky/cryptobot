package com.etricky.cryptobot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.strategies.common.TimeSeriesHelper;
import com.etricky.cryptobot.model.TradeEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeSeriesHelperTest {

	public static void main(String[] args) {
		try {
			new TimeSeriesHelperTest().runTest();
		} catch (NoSuchMethodException e) {

			e.printStackTrace();
		} catch (SecurityException e) {

			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		} catch (IllegalArgumentException e) {

			e.printStackTrace();
		} catch (InvocationTargetException e) {

			e.printStackTrace();
		}
	}

	public void runTest()
			throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		log.debug("start");

		ZonedDateTime zdt;
		int aux;
		TimeSeriesHelper tsh = new TimeSeriesHelper();
		TradeEntity tradesEntity = new TradeEntity();

		Method method = TimeSeriesHelper.class.getDeclaredMethod("calculateBarEndTime", TradeEntity.class, Integer.TYPE);
		method.setAccessible(true);

		zdt = DateFunctions.getZDTNow();
		log.debug("zdt: {}", DateFunctions.getStringFromZDT(zdt));
		tradesEntity.setTimestamp(zdt);

		zdt = (ZonedDateTime) method.invoke(tsh, tradesEntity, new Integer(15 * 60));

		zdt = DateFunctions.getZDTNow();
		aux = zdt.getMinute();
		zdt = zdt.minusMinutes(aux).plusMinutes(60 * 0 - 1);
		log.debug("zdt: {}", DateFunctions.getStringFromZDT(zdt));
		tradesEntity.setTimestamp(zdt);

		zdt = (ZonedDateTime) method.invoke(tsh, tradesEntity, new Integer(1 * 3600));

		//////////////////////////
		log.debug("test1 ----------");
		zdt = DateFunctions.getZDTfromUnixTime(1509238740);
		log.debug("zdt: {}", DateFunctions.getStringFromZDT(zdt));
		tradesEntity.setTimestamp(zdt);

		for (int i = 0; i < 3; i++) {
			tradesEntity.setTimestamp(tradesEntity.getTimestamp().plusMinutes(1));
			log.debug("i: {} zdt: {} zdt_s: {}", i, DateFunctions.getStringFromZDT(tradesEntity.getTimestamp()), tradesEntity.getTimestamp());
		}

		//////////////////////////
		log.debug("test2 ----------");
		zdt = DateFunctions.getZDTfromUnixTime(1509231600);
		log.debug("zdt: {}", DateFunctions.getStringFromZDT(zdt));
		tradesEntity.setTimestamp(zdt);

		zdt = (ZonedDateTime) method.invoke(tsh, tradesEntity, new Integer(3600));

		log.debug("zdt: {} zdt_: {}", DateFunctions.getStringFromZDT(zdt), zdt);

		//////////////////////////
		log.debug("test3 ----------");
		zdt = DateFunctions.getZDTfromUnixTime(1509235200);
		log.debug("zdt: {} zdt_: {}", DateFunctions.getStringFromZDT(zdt), zdt);

		zdt = (ZonedDateTime) method.invoke(tsh, tradesEntity, new Integer(3600));
		tradesEntity.setTimestamp(zdt);

		log.debug("zdt: {} zdt_: {} trade: {}", DateFunctions.getStringFromZDT(zdt), zdt,
				DateFunctions.getStringFromZDT(tradesEntity.getTimestamp()));

		//////////////////////////
		log.debug("test4 ----------");
		zdt = DateFunctions.getZDTfromUnixTime(1509238740);
		log.debug("zdt: {} zdt_: {}", DateFunctions.getStringFromZDT(zdt), zdt);

		for (int i = 0; i < 3; i++) {
			zdt = (ZonedDateTime) method.invoke(tsh, tradesEntity, new Integer(3600));
			tradesEntity.setTimestamp(zdt);

			log.debug("zdt: {} zdt_: {} trade: {}", DateFunctions.getStringFromZDT(zdt), zdt,
					DateFunctions.getStringFromZDT(tradesEntity.getTimestamp()));
		}

		//////////////////////////
		log.debug("test5 ----------");
		try {
			zdt = DateFunctions.getZDTfromStringDate("2017-10-29");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		log.debug("zdt: {} zdt_: {} unix: {}", DateFunctions.getStringFromZDT(zdt), zdt, DateFunctions.getUnixTimeFromZDT(zdt));

		zdt = (ZonedDateTime) method.invoke(tsh, tradesEntity, new Integer(3600));
		tradesEntity.setTimestamp(zdt);

		log.debug("zdt: {} zdt_: {} trade: {}", DateFunctions.getStringFromZDT(zdt), zdt,
				DateFunctions.getStringFromZDT(tradesEntity.getTimestamp()));

		//////////////////////////
		log.debug("test6 ----------");

		Date date = new Date(1509235200l * 1000);
		log.debug("date: {}", date);
		zdt = DateFunctions.getZDTFromDate(date);

		log.debug("zdt: {} zdt_: {} unix: {}", DateFunctions.getStringFromZDT(zdt), zdt, DateFunctions.getUnixTimeFromZDT(zdt));

		zdt = (ZonedDateTime) method.invoke(tsh, tradesEntity, new Integer(3600));
		tradesEntity.setTimestamp(zdt);

		log.debug("zdt: {} zdt_: {} trade: {}", DateFunctions.getStringFromZDT(zdt), zdt,
				DateFunctions.getStringFromZDT(tradesEntity.getTimestamp()));

		TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // It will set UTC timezone
		date = new Date(1509235200l * 1000);
		log.debug("date: {}", date);
		zdt = DateFunctions.getZDTFromDate(date);
		log.debug("zdt: {} zdt_: {} unix: {}", DateFunctions.getStringFromZDT(zdt), zdt,
				DateFunctions.getUnixTimeFromdDate(date));

		log.debug("done");
	}
}
