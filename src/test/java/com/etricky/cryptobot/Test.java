package com.etricky.cryptobot;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.ta4j.core.BaseBar;
import org.ta4j.core.num.PrecisionNum;

public class Test {

	public static void main(String[] args) {
		// 2018-02-01T00:00:00Z

		System.out.println("start");

		new BaseBar(ZonedDateTime.of(2018, 01, 1, 12, 0, 0, 0, ZoneId.systemDefault()), 100.0, 100.0, 100.0, 100.0,
				1060, PrecisionNum::valueOf);

		System.out.println("end");

	}
}
