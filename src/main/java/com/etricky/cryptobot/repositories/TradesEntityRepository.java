package com.etricky.cryptobot.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.etricky.cryptobot.model.TradesEntity;
import com.etricky.cryptobot.model.TradesPK;

public interface TradesEntityRepository extends CrudRepository<TradesEntity, TradesPK> {

	@Query("select count(1) as tradenbr from TradesEntity where exchange=?1 and currency=?2 and unixtime >=?3")
	long countTrades(String gdax, String currency, Long unixtimestamp);
}