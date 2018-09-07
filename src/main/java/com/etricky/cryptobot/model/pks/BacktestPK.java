package com.etricky.cryptobot.model.pks;

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
public class BacktestPK implements Serializable {

	private static final long serialVersionUID = 1L;

	@NonNull
	private String exchange;
	@NonNull
	private String tradeName;
	@NonNull
	private Long runTime;
	@NonNull
	private Long index;
	@NonNull
	@Builder.Default
	private Boolean tradeData = false;
}
