package com.etricky.cryptobot.core.exchanges.common;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExchangeProductMetaData {

	private String id;
	private BigDecimal minSize;
	private BigDecimal maxSize;
	private BigDecimal minPrice;
}
