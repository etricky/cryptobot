package com.etricky.cryptobot.repositories;

import org.springframework.data.repository.CrudRepository;

import com.etricky.cryptobot.model.BacktestResultsEntity;
import com.etricky.cryptobot.model.primaryKeys.BacktestPK;

public interface BacktestResultsRepository extends CrudRepository<BacktestResultsEntity, BacktestPK> {

}
