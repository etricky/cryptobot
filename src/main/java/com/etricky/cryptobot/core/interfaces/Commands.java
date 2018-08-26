package com.etricky.cryptobot.core.interfaces;

import java.io.IOException;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.common.ExitCode;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeAccount;
import com.etricky.cryptobot.core.exchanges.common.AbstractExchangeTrading;
import com.etricky.cryptobot.core.exchanges.common.enums.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.enums.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.threads.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeJson;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.interfaces.slack.Slack;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Commands {
	public final static int OK = 0;
	public final static int EXCHANGE_INVALID = 1;
	public final static int CURRENCY_INVALID = 2;
	public final static int EXCHANGE_CURRENCY_PAIR_INVALID = 3;
	public final static int NO_CONFIG_EXCHANGE = 4;

	@Autowired
	ApplicationContext appContext;
	@Autowired
	ExitCode exitCode;
	@Autowired
	Slack slack;
	@Autowired
	ExchangeThreads exchangeThreads;
	@Autowired
	JsonFiles jsonFiles;
	@Autowired
	AbstractExchangeAccount abstractExchangeAccount;

	private String auxString = null;
	private boolean validCommand;

	public void startExchangeTrade(String exchange, String tradeName, int tradeType) {
		log.debug("start. exchange: {} tradeName: {} tradeType: {}", exchange, tradeName, tradeType);

		try {
			if (validateTrade(exchange, Optional.of(tradeName))) {
				sendMessage("Starting trade for exchange: " + exchange + " tradeName: " + tradeName, true);
				if (tradeType < AbstractExchangeTrading.TRADE_ALL || tradeType > AbstractExchangeTrading.TRADE_LIVE) {
					sendMessage("Trade type must be 0 - All, 1 - History or 2 - Live", true);
				} else {
					int result = exchangeThreads.startExchangeTradingThread(exchange, tradeName, tradeType);
					if (result == ExchangeThreads.TRADE_THREAD_EXISTS) {
						sendMessage("Thread already exist", true);
					}
				}
			} else
				log.debug("not a valid command");
		} catch (Exception e) {
			exceptionHandler(e);
		}

		log.debug("done");
	}

	public void backtest(String exchange, String tradeName, int historyDays, String startDate, String endDate) {
		ZonedDateTime _startDate = null, _endDate = null;

		log.debug("start. exchange: {} tradeName: {} historyDays: {} startDate: {} endDate: {}", exchange, tradeName,
				historyDays, startDate, endDate);

		validCommand = true;

		try {
			validCommand = validateTrade(exchange, Optional.of(tradeName));

			if (validCommand && historyDays < 0) {
				sendMessage("history days must be positive", true);
				validCommand = false;
			}

			if (validCommand && historyDays == 0) {
				try {
					_startDate = DateFunctions.getZDTfromStringDate(startDate);
				} catch (ParseException e) {
					sendMessage("Start date must be on format yyyy-mm-dd", true);
					validCommand = false;
				}

				try {
					_endDate = DateFunctions.getZDTfromStringDate(endDate);
				} catch (ParseException e) {
					sendMessage("End date must be on format yyyy-mm-dd", true);
					validCommand = false;
				}

				if (_startDate.isAfter(_endDate) || _startDate.isEqual(_endDate)) {
					sendMessage("Start date must be befor end date", true);
					validCommand = false;
				}
			}

			if (validCommand) {
				sendMessage("Starting backtest for exchange: " + exchange + " currency: " + tradeName + " historyDays: "
						+ historyDays + " startDate: " + DateFunctions.getStringFromZDT(_startDate) + " endDate: "
						+ DateFunctions.getStringFromZDT(_endDate), true);

				// ensures that the backtest has the latest configs
				reloadConfigs();

				exchangeThreads.backtest(exchange, tradeName, historyDays, _startDate, _endDate);
			}
		} catch (Exception e) {
			exceptionHandler(e);
		}

		log.debug("done");

	}

	public void stopExchangeTrade(String exchange, String tradeName) {

		log.debug("start. exchange: {} tradeName: {}", exchange, tradeName);

		try {
			sendMessage("Stopping trade for exchange: " + exchange + " tradeName: " + tradeName, true);

			if (validateTrade(exchange, Optional.of(tradeName))) {
				int result = exchangeThreads.stopTradeThreads(exchange, tradeName);
				if (result == ExchangeThreads.TRADE_THREAD_NOT_EXISTS) {
					sendMessage("no trade " + exchangeThreads.getExchangeTradeKey(exchange, tradeName) + " found",
							true);
				}
			} else
				log.debug("not a valid command");
		} catch (Exception e) {
			exceptionHandler(e);
		}

		log.debug("done");
	}

	public void listExchangeTradingThreads(boolean toExternalApp) {
		log.debug("start");

		try {
			HashMap<String, List<String>> auxList = exchangeThreads.getRunningTradesThreads();
			if (auxList.isEmpty()) {
				sendMessage("No exchange trading threads", toExternalApp);
			} else {
				auxString = "Current exchange trading threads:\n";
				auxList.keySet().forEach((exch) -> {
					auxString.concat(exch + "\n");
					auxList.get(exch).forEach((curr) -> {
						auxString.concat(" - " + curr + "\n");
					});
				});
				sendMessage(auxString, toExternalApp);
			}
		} catch (Exception e) {
			exceptionHandler(e);
		}

		log.debug("done");
	}

	public void quitApplication() {
		log.debug("start");

		try {
			sendMessage("Shutting down CryptoBot!!!", true);

			// stops all exchanges threads
			exchangeThreads.stopAllThreads();

			exitCode.setExitCode(0);
			terminate(exitCode);
		} catch (Exception e) {
			exceptionHandler(e);
		}

		log.debug("done");
	}

	public void reloadConfigs() {
		log.debug("start");

		try {
			jsonFiles.loadFiles();
		} catch (Exception e) {
			exceptionHandler(e);
		}

		log.debug("done");
	}

	public void printWalletInfo(String exchange) {
		log.debug("start. exchange: {}", exchange);

		try {
			if (validateTrade(exchange)) {
				// TODO must get the appropriate exchange account bean
				sendMessage(abstractExchangeAccount.getWalletInfo(), true);
			}
		} catch (IOException e) {
			exceptionHandler(e);
		}

		log.debug("done");
	}

	private boolean validateTrade(String exchange) {
		return validateTrade(exchange, Optional.empty());
	}

	private boolean validateTrade(String exchange, Optional<String> tradeName) {
		String _exchange = exchange.toUpperCase();
		String _tradeName;

		log.debug("start. exchange: {} tradeName: {}", exchange, tradeName);

		validCommand = true;

		Map<String, ExchangeJson> exchangeJsonMap = jsonFiles.getExchangesJson();

		if (ExchangeEnum.getInstanceByName(_exchange) == null) {
			sendMessage("Not a valid exchange", true);
			sendMessage("Valid exchanges:");
			Arrays.asList(ExchangeEnum.values()).forEach(e -> {
				sendMessage(" - " + e.getName());
			});
			validCommand = false;
		}

		if (tradeName.isPresent()) {
			_tradeName = tradeName.get().toUpperCase();

			if (exchangeJsonMap.containsKey(_exchange)) {
				ExchangeJson exchangeJson = exchangeJsonMap.get(_exchange);

				if (exchangeJson.getTradeConfigsMap().containsKey(_tradeName)) {
					ExchangeJson.TradeConfigs tradeConfigs = exchangeJson.getTradeConfigsMap().get(_tradeName);

					// validates the currency pairs
					tradeConfigs.getCurrencyPairs().forEach(curr -> {
						if (validCommand && !CurrencyEnum.getInstanceByShortName(curr).isPresent()) {
							sendMessage(curr + " not a valid currencyPair", true);
							sendMessage("Valid currency pairs:");
							Arrays.asList(CurrencyEnum.values()).forEach(c -> {
								sendMessage(" - " + c.getShortName());
							});
							validCommand = false;
						}

						// checks if the currency is valid for the exchange
						if (!exchangeJson.getCurrencyPairsMap().containsKey(curr)) {
							sendMessage(curr + " not a valid currency pair for this exchange", true);
							sendMessage("Valid currency pairs:");
							exchangeJson.getCurrencyPairs().forEach((cp) -> {
								sendMessage(" - " + cp.getShortName());
							});
							validCommand = false;
						}

					});
				} else {
					sendMessage("Trade config not yet configured", true);
					sendMessage("Valid trade configs:");
					exchangeJson.getTradeConfigs().forEach((t) -> {
						sendMessage(" - " + t);
					});
					validCommand = false;
				}
			} else {
				sendMessage("Exchange not yet configured", true);
				sendMessage("Valid exchanges:");
				exchangeJsonMap.forEach((n, e) -> {
					sendMessage(" - " + e.getName());
				});
				validCommand = false;
			}
		}
		log.debug("done. validCommand: {}", validCommand);
		return validCommand;
	}

	private void sendMessage(String msg) {
		sendMessage(msg, false);
	}

	public void sendMessage(String msg, boolean toExternalApp) {
		log.debug("start. msg: {} toExternalApp: {}", msg, toExternalApp);

		System.out.println(msg.concat("\n"));
		log.info(msg);

		if (toExternalApp)
			slack.sendMessage(msg);

		log.debug("done");
	}

	public void terminate(ExitCode exitCode) {
		log.debug("start. exitCode: {}", exitCode);

		try {
			SpringApplication.exit(appContext, exitCode);
			slack.disconnect();
			log.debug("Cryptobot exited");
		} catch (Exception e) {
			log.error("Error: {}", e);
		}

		System.exit(exitCode.getExitCode());
	}

	public void exceptionHandler(Exception e) {
		log.error("Exception: {}", e);

		terminate(new ExitCode());
	}

}
