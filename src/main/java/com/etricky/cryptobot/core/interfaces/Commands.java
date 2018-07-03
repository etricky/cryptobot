package com.etricky.cryptobot.core.interfaces;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.CryptoBotApplication;
import com.etricky.cryptobot.core.common.ExitCode;
import com.etricky.cryptobot.core.exchanges.common.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeJson;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.interfaces.slack.Slack;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Commands {

	@Autowired
	CryptoBotApplication cryptobotApp;
	@Autowired
	ExitCode exitCode;
	@Autowired
	Slack slack;
	@Autowired
	ExchangeThreads exchangeThreads;
	@Autowired
	JsonFiles jsonFiles;

	public void startExchangeCurrency(String exchange, String currency)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		log.debug("start. exchange: {} currency: {}", exchange, currency);

		if (validateExchangeCurreny(exchange, currency)) {
			sendMessage("Starting currency for exchange: " + exchange + " currency: " + currency, true);
			int result = exchangeThreads.startExchangeThreads(exchange, currency);
			if (result == 1) {
				sendMessage("Thread already exist", true);
			}
		} else
			log.debug("not a valid command");

		log.debug("done");
	}

	public void stopExchangeCurrency(String exchange, String currency) {

		log.debug("start. exchange: {} currency: {}", exchange, currency);

		sendMessage("Stopping currency for exchange: " + exchange + " currency: " + currency, true);

		if (validateExchangeCurreny(exchange, currency)) {
			int result = exchangeThreads.stopThread(exchange, currency);
			if (result == 1) {
				sendMessage("no thread " + exchangeThreads.getThreadName(exchange, currency) + " found", true);
			}
		} else
			log.debug("not a valid command");

		log.debug("done");
	}

	public void listExchangeCurrency() {
		log.debug("start");

		HashMap<String, List<String>> auxList = exchangeThreads.getRunningThreads();
		if (auxList.isEmpty()) {
			sendMessage("No running exchange/currencies");
		} else {
			sendMessage("Current exchange/currencies:");
			auxList.keySet().forEach((exch) -> {
				sendMessage(exch);
				auxList.get(exch).forEach((curr) -> {
					sendMessage(" - " + curr);
				});
			});
		}

		log.debug("done");
	}

	public void quitExchangeCurrency() throws IOException {
		log.debug("start");

		sendMessage("Shutting down CryptoBot!!!", true);

		// stops all exchanges threads
		exchangeThreads.stopAllThreads();

		exitCode.setExitCode(0);
		cryptobotApp.terminate(exitCode);

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

	public void sendMessage(String msg, boolean toSlack) {
		log.debug("start. msg: {} toSlack: {}", msg, toSlack);

		System.out.println(msg);

		if (toSlack)
			slack.sendMessage(msg);

		log.debug("done");
	}
}
