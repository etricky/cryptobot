package com.etricky.cryptobot.repositories;

import org.springframework.data.repository.CrudRepository;

import com.etricky.cryptobot.model.OrdersEntity;
import com.etricky.cryptobot.model.primaryKeys.ExchangePK;

public interface OrdersEntityRepository extends CrudRepository<OrdersEntity, ExchangePK> {

}
