package com.etricky.cryptobot.core.exchanges.common;

import org.knowm.xchange.dto.Order.OrderType;

import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeOrderTypeEnum;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExchangeOrderMetaData {
	private String id;
	private String currencyShortName;
	private OrderType orderType;
	private ExchangeOrderTypeEnum exchangeOrderType;
}
