package com.etricky.cryptobot.core.interfaces.jsonFiles;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StrategiesJson {
	String type;
	String bean;
	Integer barDurationSec;
	Integer initialPeriod;
	Long timeSeriesBars;
	Boolean entryEnabled;
	Boolean exitEnabled;
	Boolean allowMultiCurrency;

	BigDecimal timeFrameLong;
	BigDecimal timeFrameShort;

	BigDecimal entryLossPercentage1;
	BigDecimal entryLossPercentage2;
	BigDecimal entryLossPercentage3;

	BigDecimal entryGainPercentage1;
	BigDecimal entryGainPercentage2;
	BigDecimal entryGainPercentage3;

	BigDecimal exitLossPercentage1;
	BigDecimal exitLossPercentage2;
	BigDecimal exitLossPercentage3;

	BigDecimal exitGainPercentage1;
	BigDecimal exitGainPercentage2;
	BigDecimal exitGainPercentage3;

}
