package com.etricky.cryptobot.repositories;

import org.springframework.data.repository.CrudRepository;

import com.etricky.cryptobot.model.ExchangePK;
import com.etricky.cryptobot.model.OrdersEntity;

public interface OrdersEntityRepository extends CrudRepository<OrdersEntity, ExchangePK> {

}
