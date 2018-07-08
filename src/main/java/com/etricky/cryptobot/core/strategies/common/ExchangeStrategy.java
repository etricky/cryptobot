package com.etricky.cryptobot.core.strategies.common;

import java.math.BigDecimal;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Decimal;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TradingRecord;

import com.etricky.cryptobot.core.exchanges.common.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.ExchangeExceptionRT;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.model.ExchangePK;
import com.etricky.cryptobot.model.OrderEntityType;
import com.etricky.cryptobot.model.OrdersEntity;
import com.etricky.cryptobot.model.TradesEntity;
import com.etricky.cryptobot.repositories.OrdersEntityRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Scope("prototype")
@Slf4j
public class ExchangeStrategy {
	public final static int NO_ACTION = 0;
	public final static int ENTER = 1;
	public final static int EXIT = 2;

	@Autowired
	protected JsonFiles jsonFiles;
	@Autowired
	private ApplicationContext appContext;
	@Autowired
	protected OrdersEntityRepository ordersEntityRepository;
	@Autowired
	TimeSeriesHelper timeSeriesHelper;

	private HashMap<String, AbstractStrategy> strategiesMap;
	private ExchangeEnum exchangeEnum;
	private CurrencyEnum currencyEnum;
	private TimeSeries exchangeTimeSeries;
	private TradingRecord exchangeTradingRecord;
	private int timeSeriesBar = 0, exchangeBarDuration = 0;

	public void initializeStrategies(ExchangeEnum exchangeEnum, CurrencyEnum currencyEnum) {
		log.debug("start");

		exchangeTradingRecord = new BaseTradingRecord();
		strategiesMap = new HashMap<String, AbstractStrategy>();

		jsonFiles.getExchangesJson().get(exchangeEnum.getName()).getStrategies().forEach((s) -> {
			log.debug("creating bean: {} for exchange: {} currency: {}", s.getBean(), exchangeEnum.getName(), currencyEnum.getShortName());

			this.exchangeEnum = exchangeEnum;
			this.currencyEnum = currencyEnum;

			AbstractStrategy strategy = (AbstractStrategy) appContext.getBean(s.getBean());
			strategy.initializeStrategy(exchangeEnum, s.getBean());

			strategiesMap.put(s.getBean(), strategy);

			if (s.getTimeSeriesBars().intValue() > timeSeriesBar) {
				timeSeriesBar = s.getTimeSeriesBars().intValue();
				exchangeBarDuration = s.getBarDurationSec().intValue();
			}

		});

		log.debug("timeSeriesBar: {} barDuration: {}", timeSeriesBar, exchangeBarDuration);

		exchangeTimeSeries = new BaseTimeSeries(exchangeEnum.getName());
		exchangeTimeSeries.setMaximumBarCount(timeSeriesBar);

		log.debug("done");
	}

	public void addHistoryTradeToTimeSeries(TradesEntity tradesEntity) throws ExchangeException {
		log.debug("start");

		// adds bar to exchange time series
		timeSeriesHelper.addTradeToTimeSeries(exchangeTimeSeries, tradesEntity, exchangeBarDuration);

		strategiesMap.forEach((bean, strat) -> {
			try {
				strat.addTradeToTimeSeries(tradesEntity);
			} catch (ExchangeException e1) {
				log.error("Exception: {}", e1);
				throw new ExchangeExceptionRT(e1);
			}
		});

		log.debug("done");
	}

	public void processStrategyForLiveTrade(TradesEntity tradesEntity) throws ExchangeException {
		String logAux = null;
		OrderEntityType orderType = null;
		int finalResult = NO_ACTION, auxResult = NO_ACTION, lowestBar = 0;

		log.debug("start");

		timeSeriesHelper.addTradeToTimeSeries(exchangeTimeSeries, tradesEntity, exchangeBarDuration);

		for (AbstractStrategy abstractStrategy : strategiesMap.values()) {

			auxResult = abstractStrategy.processStrategyForLiveTrade(tradesEntity, exchangeTradingRecord);

			// as the strategy with highest cadence (lowest bar duration) responds quicker
			// to changes has a higher priority over strategies with lowest cadence
			if (lowestBar < abstractStrategy.getBarDuration() && auxResult != NO_ACTION) {
				finalResult = auxResult;
				lowestBar = abstractStrategy.getBarDuration();
			}
		}

		log.debug("finalResult: {}", finalResult);

		if (finalResult != NO_ACTION) {

			int endIndex = exchangeTimeSeries.getEndIndex();
			Bar lastBar = exchangeTimeSeries.getLastBar();

			if (finalResult == ENTER && exchangeTradingRecord.enter(endIndex, lastBar.getClosePrice(), Decimal.ONE)) {
				logAux = "Enter";
				orderType = OrderEntityType.BUY;

				// TODO amount should correspond to the amount of currency that was traded
			} else {
				log.warn("trading record not updated on ENTER");
				finalResult = NO_ACTION;
			}

			if (finalResult == EXIT && exchangeTradingRecord.exit(endIndex, lastBar.getClosePrice(), Decimal.ONE)) {
				logAux = "Exit";
				orderType = OrderEntityType.SELL;

				// TODO amount should correspond to the amount of currency that was traded
			} else {
				log.warn("trading record not updated on EXIT");
				finalResult = NO_ACTION;
			}

			if (finalResult != NO_ACTION) {
				log.debug("storing order {} on index: {} price: {}", logAux, endIndex, lastBar.getClosePrice());

				// TODO order should only be stored after it has been completed in the exchange
				ordersEntityRepository.save(OrdersEntity.builder()
						.orderId(ExchangePK.builder().currency(currencyEnum.getShortName()).exchange(exchangeEnum.getName())
								.unixtime(tradesEntity.getTradeId().getUnixtime()).build())
						.index(BigDecimal.valueOf(endIndex)).orderType(orderType).price(BigDecimal.valueOf(lastBar.getClosePrice().doubleValue()))
						.timestamp(tradesEntity.getTimestamp()).amount(BigDecimal.valueOf(Decimal.ONE.longValue())).build());
			}
		} else {
			log.debug("no order occurred");
		}

		log.debug("done");
	}

	public void backTest() {
		// TODO implement this method

		// load trades from BD
		// create tradingRecord
		// select an analysis criteria, f.e., AnalysisCriterion criterion = new
		// TotalProfitCriterion();
		// run analysis criteria criterion.calculate(TimeSeries, TradingRecord)

	}
}
