package com.etricky.cryptobot.model;

import java.io.Serializable;

import javax.persistence.Embeddable;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Embeddable
@Data
@Slf4j
public class TradesPK implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@NonNull
	private String exchange;
	@NonNull
	private String currency;
	@NonNull
	private Long unixtime;

}
