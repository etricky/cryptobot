package com.etricky.cryptobot.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.ta4j.core.Order;

import com.etricky.cryptobot.core.common.NumericFunctions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "BacktestData")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class BacktestEntity {
	@EmbeddedId
	@NonNull
	private BacktestPK orderId;
	@NonNull
	private ZonedDateTime timestamp;
	@NonNull
	private BigDecimal orderUnixTime;
	@NonNull
	@Enumerated
	private Order.OrderType orderType;
	@NonNull
	@Column(precision = 12, scale = NumericFunctions.BALANCE_SCALE)
	private BigDecimal closePrice;
	@NonNull
	@Column(precision = 12, scale = NumericFunctions.BALANCE_SCALE)
	private BigDecimal highPrice;
	@NonNull
	@Column(precision = 12, scale = NumericFunctions.BALANCE_SCALE)
	private BigDecimal lowPrice;
	@NonNull
	@Column(precision = 12, scale = NumericFunctions.AMOUNT_SCALE)
	private BigDecimal amount;
	@NonNull
	@Column(precision = 12, scale = NumericFunctions.AMOUNT_SCALE)
	private BigDecimal deltaAmount;
	@NonNull
	@Column(precision = 12, scale = NumericFunctions.BALANCE_SCALE)
	private BigDecimal balance;
	@NonNull
	private BigDecimal deltaBalance;
	@NonNull
	@Column(precision = 12, scale = NumericFunctions.FEE_SCALE)
	private BigDecimal feeValue;
	@NonNull
	private String strategy;
}
