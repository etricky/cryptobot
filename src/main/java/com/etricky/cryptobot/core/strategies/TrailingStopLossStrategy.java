package com.etricky.cryptobot.core.strategies;

import java.math.BigDecimal;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.etricky.cryptobot.core.strategies.common.ExchangeStrategy;
import com.etricky.cryptobot.core.strategies.common.StrategyGeneric;
import com.etricky.cryptobot.core.strategies.rules.TraillingStopLossEntryRule;
import com.etricky.cryptobot.core.strategies.rules.TraillingStopLossExitRule;
import com.etricky.cryptobot.model.ExchangePK;
import com.etricky.cryptobot.model.OrderType;
import com.etricky.cryptobot.model.OrdersEntity;
import com.etricky.cryptobot.model.TradesEntity;

import lombok.extern.slf4j.Slf4j;

@Component
@Scope("prototype")
@Slf4j
public class TrailingStopLossStrategy extends StrategyGeneric {

	public TrailingStopLossStrategy() {
		log.debug("start");

		buildStrategy();

		log.debug("done");
	}

	private void buildStrategy() {
		log.debug("start");

		ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);

		// TODO feeValue must be obtained by fee*amount to be traded
		Rule entryRule = new TraillingStopLossEntryRule(closePrice, Decimal.valueOf(strategiesSettings.getGainPercentage()),
				Decimal.valueOf(strategiesSettings.getFee().multiply(BigDecimal.TEN)));

		// TODO feeValue must be obtained by fee*amount to be traded
		Rule exitRule = new TraillingStopLossExitRule(closePrice, Decimal.valueOf(strategiesSettings.getLossPerc()),
				Decimal.valueOf(strategiesSettings.getGainPercentage()), Decimal.valueOf(strategiesSettings.getFee().multiply(BigDecimal.TEN)));

		strategy = new BaseStrategy(entryRule, exitRule);

		log.debug("done");
	}

	@Override
	public int processLiveTrade(TradesEntity tradesEntity) {

		String logAux = null;
		OrderType orderType = null;
		int result = ExchangeStrategy.NO_ACTION;
		log.debug("start");

		addTradeToTimeSeries(tradesEntity);

		int endIndex = timeSeries.getEndIndex();
		Bar lastBar = timeSeries.getLastBar();
		if (strategy.shouldOperate(endIndex, tradingRecord) && strategy.shouldEnter(endIndex)) {
			// strategy should enter
			log.debug("strategy should ENTER on " + endIndex);

			if (tradingRecord.enter(endIndex, lastBar.getClosePrice(), Decimal.ONE)) {
				logAux = "Entered";
				orderType = OrderType.BUY;
				result = ExchangeStrategy.ENTER;
				// TODO amount should correspond to the amount of currency that was traded
			} else {
				log.warn("trading record not updated on enter!");
			}
		} else if (strategy.shouldOperate(endIndex, tradingRecord) && strategy.shouldExit(endIndex)) {
			// strategy should exit
			log.debug("strategy should EXIT on " + endIndex);

			if (tradingRecord.exit(endIndex, lastBar.getClosePrice(), Decimal.ONE)) {
				logAux = "Exited";
				orderType = OrderType.SELL;
				result = ExchangeStrategy.EXIT;
				// TODO amount should correspond to the amount of currency that was traded

			} else {
				log.warn("trading record not updated on exit!");
			}
		}

		if (result != ExchangeStrategy.NO_ACTION) {
			log.debug("{} on index: {} price={}", logAux, endIndex, lastBar.getClosePrice());

			ordersEntityRepository.save(OrdersEntity.builder()
					.orderId(ExchangePK.builder().currency(currencyEnum.getShortName()).exchange(exchangeEnum.getName())
							.unixtime(tradesEntity.getTradeId().getUnixtime()).build())
					.amount(BigDecimal.valueOf(Decimal.ONE.longValue())).index(BigDecimal.valueOf(endIndex)).orderType(orderType)
					.price(BigDecimal.valueOf(lastBar.getClosePrice().doubleValue())).timestamp(tradesEntity.getTimestamp()).build());
		}

		log.debug("done. result: {}", result);
		return result;
	}

	@Override
	public void closeTrade() {
		log.debug("start");

		if (tradingRecord.getCurrentTrade().isOpened()) {
			log.debug("closing trade in trading record");

			tradingRecord.exit(timeSeries.getEndIndex(), timeSeries.getLastBar().getClosePrice(), Decimal.ZERO);
		}

		log.debug("done");
	}

}
