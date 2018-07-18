package com.etricky.cryptobot.model;

import java.math.BigDecimal;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

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
	private ExchangePK orderId;

	@NonNull
	private BigDecimal index;
	@NonNull
	private BigDecimal amount;
	@NonNull
	private BigDecimal balance;
	@NonNull
	private BigDecimal deltaBalance;
	@NonNull
	private BigDecimal feeValue;
}
