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
	/**
	 * Initial period for which strategies will ignore new trades. This ensures that
	 * when the strategy starts processing trades, the indicators values have been
	 * correctly calculated
	 * 
	 * @param age Number of seconds for which strategies should ignore new trades
	 * @return Number of seconds for which strategies should ignore new trades
	 */
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
