package com.etricky.cryptobot;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.etricky.cryptobot.core.common.DateFunctions;

public class Test {

	public static void main(String[] args) {
		// 2018-02-01T00:00:00Z

		System.out.println(DateFunctions.getNowZDT().format(DateTimeFormatter.ISO_INSTANT));

		ZonedDateTime date = ZonedDateTime.now(ZoneId.systemDefault());
		System.out.println(date);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'H:mX");
		System.out.println(date.format(formatter));
		
		date = DateFunctions.getNowZDT();
		formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'H:mX");
		System.out.println(date.format(formatter));
		
		System.out.println(DateFunctions.getStringFromUnixTime(DateFunctions.getUnixTimeFromZDT(date)).toString());

	}
}
