package com.etricky.cryptobot.core.interfaces;

import java.io.IOException;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.DateFunctions;
import com.etricky.cryptobot.core.common.ExitCode;
import com.etricky.cryptobot.core.exchanges.common.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeJson;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.interfaces.slack.Slack;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Commands {

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

	private String auxString = null;

	public void startExchangeCurrencyTrade(String exchange, String currency, int tradeType)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		log.debug("start. exchange: {} currency: {} tradeType: {}", exchange, currency, tradeType);

		if (validateExchangeCurreny(exchange, currency)) {
			sendMessage("Starting trade for exchange: " + exchange + " currency: " + currency, true);
			if (tradeType < 0 || tradeType > 2) {
				sendMessage("Trade type must be 0 - All, 1 - History or 2 - Live", true);
			} else {
				int result = exchangeThreads.startExchangeThreads(exchange, currency, tradeType);
				if (result == ExchangeThreads.THREAD_EXISTS) {
					sendMessage("Thread already exist", true);
				}
			}
		} else
			log.debug("not a valid command");

		log.debug("done");
	}

	public void backtest(String exchange, String currency, int historyDays, int choosedStrategies, String startDate, String endDate) {
		ZonedDateTime _startDate = null, _endDate = null;
		boolean validCommand;
		log.debug("start. exchange: {} currency: {} historyDays: {} choosedStrategies: {} startDate: {} endDate: {}", exchange, currency, historyDays,
				choosedStrategies, startDate, endDate);

		validCommand = validateExchangeCurreny(exchange, currency);

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

		if (validCommand && (choosedStrategies < 0 || choosedStrategies > 2)) {
			sendMessage("Strategy must be 0 - All, 1 - Stop Loss or 2 - Trading", true);
		}

		if (validCommand) {
			sendMessage("Starting backtest for exchange: " + exchange + " currency: " + currency + " historyDays: " + historyDays + " startDate: "
					+ DateFunctions.getStringFromZDT(_startDate) + " endDate: " + DateFunctions.getStringFromZDT(_endDate), true);
			exchangeThreads.backtest(exchange, currency, historyDays, choosedStrategies, _startDate, _endDate);
		}

		log.debug("done");

	}

	public void stopExchangeCurrencyTrade(String exchange, String currency) {

		log.debug("start. exchange: {} currency: {}", exchange, currency);

		sendMessage("Stopping trade for exchange: " + exchange + " currency: " + currency, true);

		if (validateExchangeCurreny(exchange, currency)) {
			int result = exchangeThreads.stopThread(exchange, currency);
			if (result == ExchangeThreads.THREAD_NOT_EXISTS) {
				sendMessage("no thread " + exchangeThreads.getThreadName(exchange, currency) + " found", true);
			}
		} else
			log.debug("not a valid command");

		log.debug("done");
	}

	public void listExchangeCurrency(boolean toExternalApp) {
		log.debug("start");

		HashMap<String, List<String>> auxList = exchangeThreads.getRunningThreads();
		if (auxList.isEmpty()) {
			sendMessage("No running exchange/currencies", toExternalApp);
		} else {
			auxString = "Current exchange/currencies:\n";
			auxList.keySet().forEach((exch) -> {
				auxString.concat(exch + "\n");
				auxList.get(exch).forEach((curr) -> {
					auxString.concat(" - " + curr + "\n");
				});
			});
			sendMessage(auxString, toExternalApp);
		}

		log.debug("done");
	}

	public void quitApplication() throws IOException {
		log.debug("start");

		sendMessage("Shutting down CryptoBot!!!", true);

		// stops all exchanges threads
		exchangeThreads.stopAllThreads();

		exitCode.setExitCode(0);
		terminate(exitCode);

		log.debug("done");
	}

	private boolean validateExchangeCurreny(String exchange, String currency) {
		boolean validCommand = true;
		log.debug("start. exchange: {} currency: {}", exchange, currency);

		Map<String, ExchangeJson> exchangeJson = jsonFiles.getExchangesJson();

		switch (exchangeThreads.validateExchangeCurreny(exchange, currency)) {
		case ExchangeThreads.EXCHANGE_INVALID:
			sendMessage("Not a valid exchange", true);
			sendMessage("Valid exchanges:");
			Arrays.asList(ExchangeEnum.values()).forEach(e -> {
				sendMessage(" - " + e.getName());
			});
			validCommand = false;
			break;

		case ExchangeThreads.CURRENCY_INVALID:
			sendMessage("Not a valid currency", true);
			sendMessage("Valid currencies:");
			Arrays.asList(CurrencyEnum.values()).forEach(c -> {
				sendMessage(" - " + c.getShortName());
			});
			validCommand = false;
			break;
		case ExchangeThreads.EXCHANGE_CURRENCY_PAIR_INVALID:
			sendMessage("Not a valid currency for this exchange", true);
			sendMessage("Valid currencies:");
			exchangeJson.get(exchange).getCurrencies().forEach((c) -> {
				sendMessage(" - " + c.getShortName());
			});
			validCommand = false;
			break;
		case ExchangeThreads.NO_CONFIG_EXCHANGE:
			sendMessage("Exchange not yet configured", true);
			sendMessage("Valid exchanges:");
			exchangeJson.forEach((n, e) -> {
				sendMessage(" - " + e.getName());
			});
			validCommand = false;
			break;
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

		if (toExternalApp)
			slack.sendMessage(msg);

		log.debug("done");
	}

	public void terminate(ExitCode exitCode) throws IOException {
		log.debug("start. exitCode: {}", exitCode);

		
		SpringApplication.exit(appContext, exitCode);
		slack.disconnect();
		log.debug("Cryptobot exited");

		System.exit(exitCode.getExitCode());
	}

}
