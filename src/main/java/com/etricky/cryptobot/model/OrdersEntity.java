package com.etricky.cryptobot.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import com.etricky.cryptobot.core.common.NumericFunctions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "Orders")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class OrdersEntity {

	@EmbeddedId
	@NonNull
	private ExchangePK orderId;

	@NonNull
	private ZonedDateTime timestamp;
	
	@NonNull
	private BigDecimal index;
	
	@NonNull
	@Column(precision = 12, scale = NumericFunctions.BALANCE_SCALE)
	private BigDecimal price;
	
	@NonNull
	@Column(precision = 12, scale = NumericFunctions.AMOUNT_SCALE)
	private BigDecimal amount;

	@NonNull
	@Column(precision = 12, scale = NumericFunctions.FEE_SCALE)
	private BigDecimal fee;
	
	@NonNull
	@Enumerated(EnumType.STRING)
	private OrderEntityType orderType;
}
