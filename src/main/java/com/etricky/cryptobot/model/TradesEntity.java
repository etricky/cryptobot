package com.etricky.cryptobot.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "Trades")
@Data
@Slf4j
public class TradesEntity {

	@EmbeddedId
	@NonNull
	private TradesPK tradId;
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

}
