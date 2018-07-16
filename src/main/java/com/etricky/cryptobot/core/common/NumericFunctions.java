package com.etricky.cryptobot.core.common;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.ta4j.core.Decimal;

public class NumericFunctions {
	private static final BigDecimal _100 = BigDecimal.valueOf(100);
	private static final MathContext MC_PER = new MathContext(10, RoundingMode.HALF_UP);

	public static BigDecimal convertToBigDecimal(Decimal value) {
		return BigDecimal.valueOf(value.doubleValue());
	}

	public static BigDecimal convertToBigDecimal(Decimal value, int decimalPart) {
		return BigDecimal.valueOf(value.doubleValue()).setScale(decimalPart, RoundingMode.HALF_UP);
	}

	public static BigDecimal percentage(BigDecimal value, BigDecimal baseValue, boolean delta) {
		if (baseValue.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		} else if (delta) {
			return value.multiply(_100).divide(baseValue, MC_PER).subtract(_100);
		} else {
			return value.multiply(_100).divide(baseValue, MC_PER).setScale(2, RoundingMode.HALF_UP);
		}
	}
}