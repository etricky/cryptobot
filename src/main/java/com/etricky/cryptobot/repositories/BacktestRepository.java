package com.etricky.cryptobot.repositories;

import org.springframework.data.repository.CrudRepository;

import com.etricky.cryptobot.model.BacktestEntity;
import com.etricky.cryptobot.model.primaryKeys.BacktestPK;

public interface BacktestRepository extends CrudRepository<BacktestEntity, BacktestPK> {

}
