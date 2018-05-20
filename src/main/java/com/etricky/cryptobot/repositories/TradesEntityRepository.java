package com.etricky.cryptobot.repositories;

import org.springframework.data.repository.CrudRepository;

import com.etricky.cryptobot.model.TradesEntity;
import com.etricky.cryptobot.model.TradesPK;

public interface TradesEntityRepository extends CrudRepository<TradesEntity, TradesPK> {

}