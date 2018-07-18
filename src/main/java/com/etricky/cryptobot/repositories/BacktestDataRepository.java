package com.etricky.cryptobot.repositories;

import org.springframework.data.repository.CrudRepository;

import com.etricky.cryptobot.model.BacktestEntity;
import com.etricky.cryptobot.model.ExchangePK;

public interface BacktestDataRepository extends CrudRepository<BacktestEntity, ExchangePK> {

}
