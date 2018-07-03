package com.etricky.cryptobot.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.etricky.cryptobot.core.common.DateFunctions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "Trades")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Slf4j
public class TradesEntity {

	@EmbeddedId
	@NonNull
	private ExchangePK tradeId;
	@NonNull
	private BigDecimal openPrice;
	@NonNull
	private BigDecimal closePrice;
	@NonNull
	private BigDecimal highPrice;
	@NonNull
	private BigDecimal lowPrice;
	@NonNull
	private ZonedDateTime timestamp;
	@Builder.Default
	private boolean fakeTrade = false;

	public TradesEntity getFake() {
		return TradesEntity.builder().fakeTrade(true).closePrice(closePrice).openPrice(openPrice).highPrice(highPrice)
				.lowPrice(lowPrice).timestamp(DateFunctions.getZDTfromUnixTime(tradeId.getUnixtime()))
				.tradeId(ExchangePK.builder().currency(tradeId.getCurrency()).exchange(tradeId.getExchange())
						.unixtime(tradeId.getUnixtime()).build())
				.build();
	}

	public TradesEntity addMinute() {
		tradeId.setUnixtime(tradeId.getUnixtime() + 60);
		timestamp = DateFunctions.getZDTfromUnixTime(tradeId.getUnixtime());
		log.debug("unixtime: {}/{}", tradeId.getUnixtime(), DateFunctions.getZDTfromUnixTime(tradeId.getUnixtime()));
		return this;
	}
}
