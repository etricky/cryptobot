package com.etricky.cryptobot.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

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
	private BigDecimal price;
	
	@NonNull
	private BigDecimal amount;
	
	@NonNull
	@Enumerated(EnumType.STRING)
	private OrderType orderType;
}
