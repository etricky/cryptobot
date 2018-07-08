package com.etricky.cryptobot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.strategies.common.TimeSeriesHelper;
import com.etricky.cryptobot.model.TradesEntity;

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
		TradesEntity tradesEntity = new TradesEntity();
		

		Method method = TimeSeriesHelper.class.getDeclaredMethod("calculateBarEndTime", TradesEntity.class, Integer.TYPE);
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

		log.debug("done");
	}
}
