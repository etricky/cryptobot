package com.etricky.cryptobot.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.etricky.cryptobot.core.common.NumericFunctions;
import com.etricky.cryptobot.model.pks.BacktestPK;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "BacktestResults")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestResultsEntity {
	@EmbeddedId
	@NonNull
	private BacktestPK backtestId;
	@NonNull
	private String currency;
	private String notes;
	@NonNull
	private ZonedDateTime tradeStart;
	@NonNull
	private ZonedDateTime tradeEnd;
	@NonNull
	@Builder.Default
	@Column(precision = 12, scale = NumericFunctions.BALANCE_SCALE)
	private BigDecimal initialBalance = BigDecimal.ZERO;
	@NonNull
	@Builder.Default
	@Column(precision = 12, scale = NumericFunctions.BALANCE_SCALE)
	private BigDecimal finalBalance = BigDecimal.ZERO;
	@NonNull
	@Builder.Default
	@Column(precision = 12, scale = NumericFunctions.PERCENTAGE_SCALE)
	private BigDecimal deltaBalance = BigDecimal.ZERO;
	@NonNull
	@Builder.Default
	@Column(precision = 12, scale = NumericFunctions.AMOUNT_SCALE)
	private BigDecimal initialAmount = BigDecimal.ZERO;
	@NonNull
	@Builder.Default
	@Column(precision = 12, scale = NumericFunctions.AMOUNT_SCALE)
	private BigDecimal finalAmount = BigDecimal.ZERO;
	@NonNull
	@Builder.Default
	@Column(precision = 12, scale = NumericFunctions.PERCENTAGE_SCALE)
	private BigDecimal deltaAmount = BigDecimal.ZERO;
	@NonNull
	@Builder.Default
	@Column(precision = 12, scale = NumericFunctions.PRICE_SCALE)
	private BigDecimal initialPrice = BigDecimal.ZERO;
	@NonNull
	@Builder.Default
	@Column(precision = 12, scale = NumericFunctions.PRICE_SCALE)
	private BigDecimal finalPrice = BigDecimal.ZERO;
	@Builder.Default
	private Integer posBalanceOrders = 0;
	@Builder.Default
	private Integer negBalanceOrders = 0;
	@Builder.Default
	private Integer posAmountOrders = 0;
	@Builder.Default
	private Integer negAmountOrders = 0;
	@Builder.Default
	private Integer totalOrders = 0;
	private String currencyOrders;
	@Builder.Default
	private Integer tradingBuys = 0;
	@Builder.Default
	private Integer tradingSells = 0;
	@Builder.Default
	private Integer stopLossBuys = 0;
	@Builder.Default
	private Integer stopLossSells = 0;
	@Builder.Default
	private BigDecimal totalFees = BigDecimal.ZERO;
}
