package com.etricky.cryptobot.model;

import java.io.Serializable;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Embeddable
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class TradesPK implements Serializable {

	private static final long serialVersionUID = 1L;

	@NonNull
	private String exchange;
	@NonNull
	private String currency;
	@NonNull
	private Long unixtime;
}
