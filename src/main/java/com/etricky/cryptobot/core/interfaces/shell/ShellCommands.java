package com.etricky.cryptobot.core.interfaces.shell;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.commands.Quit;

import com.etricky.cryptobot.CryptoBotApplication;
import com.etricky.cryptobot.core.common.ExitCode;
import com.etricky.cryptobot.core.exchanges.common.CurrencyEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeEnum;
import com.etricky.cryptobot.core.exchanges.common.ExchangeThreads;
import com.etricky.cryptobot.core.interfaces.slack.Slack;

import lombok.extern.slf4j.Slf4j;

@ShellComponent
@Slf4j
public class ShellCommands implements Quit.Command {
	Slack slack;
	CryptoBotApplication cryptobotApp;
	ExitCode exitCode;
	ExchangeThreads exchangeThreads;

	public ShellCommands(Slack slack, CryptoBotApplication cryptobotApp, ExitCode exitCode,
			ExchangeThreads exchangeThreads) {
		log.debug("start");

		this.slack = slack;
		this.cryptobotApp = cryptobotApp;
		this.exitCode = exitCode;
		this.exchangeThreads = exchangeThreads;
		sendMessage("Started CryptoBot!!!", true);

		log.debug("done");
	}

	@ShellMethod(value = "Starts processing a currency from an exchange", key = { "start", "s" })
	public void start(String exchange, String currency) throws IOException, ClassNotFoundException,
			InstantiationException, IllegalAccessException {

		log.debug("start. exchange: {} currency: {}", exchange, currency);

		sendMessage("Starting currency for exchange: " + exchange + " currency: " + currency, true);

		if (validateExchangeCurreny(exchange, currency)) {
			exchangeThreads.startThread(exchange, currency);
		} else
			log.debug("not a valid command");

		log.debug("done");
	}

	@ShellMethod(value = "Stops processing a currency from an exchange", key = { "stop", "st" })
	public void stop(String exchange, String currency) {

		log.debug("start. exchange: {} currency: {}", exchange, currency);

		sendMessage("Stopping currency for exchange: " + exchange + " currency: " + currency, true);

		if (validateExchangeCurreny(exchange, currency)) {
			exchangeThreads.stopThread(exchange, currency);
		} else
			log.debug("not a valid command");

		log.debug("done");
	}

	@ShellMethod(value = "List current running currencies for each exchange", key = {"list","l"})
	public void list() {
		log.debug("start");

		HashMap<String, List<String>> auxList = exchangeThreads.getRunningThreads();
		if (auxList.isEmpty()) {
			sendMessage("No running exchange/currencies");
		} else {
			sendMessage("Current exchange/currencies:");
			auxList.keySet().forEach((k) -> {
				sendMessage(k);
				auxList.get(k).forEach((curr) -> {
					sendMessage(" - " + curr);
				});
			});
		}
		
		log.debug("done");
	}

	@ShellMethod(value = "Ends CryptoBot.", key = { "quit", "exit", "shutdown", "q" })
	public void quit() throws IOException {
		log.debug("start");

		sendMessage("Shuting down CryptoBot!!!", true);

		// stops all exchanges threads
		exchangeThreads.stopAllThreads();

		exitCode.setExitCode(0);
		cryptobotApp.terminate(exitCode);

		log.debug("done");
	}

	private boolean validateExchangeCurreny(String exchange, String currency) {
		boolean validCommand = true;

		if (ExchangeEnum.getInstanceByName(exchange) == null) {
			sendMessage("Not a valid exchange", true);
			sendMessage("Valid exchanges:");
			Arrays.asList(ExchangeEnum.values()).forEach(e -> {
				sendMessage(" - " + e.getName());
			});
			validCommand = false;
		}

		if (CurrencyEnum.getInstanceByShortName(currency) == null) {
			sendMessage("Not a valid currency", true);
			sendMessage("Valid currencies:");
			Arrays.asList(CurrencyEnum.values()).forEach(c -> {
				sendMessage(" - " + c.getShortName());
			});
			validCommand = false;
		}

		return validCommand;
	}

	private void sendMessage(String msg) {
		sendMessage(msg, false);
	}

	public void sendMessage(String msg, boolean toSlack) {
		System.out.println(msg);

		if (toSlack)
			slack.sendMessage(msg);
	}

}