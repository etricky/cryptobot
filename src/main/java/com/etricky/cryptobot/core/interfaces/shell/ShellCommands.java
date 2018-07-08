package com.etricky.cryptobot.core.interfaces.shell;

import java.io.IOException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.commands.Quit;

import com.etricky.cryptobot.core.interfaces.Commands;

import lombok.extern.slf4j.Slf4j;

@ShellComponent
@Slf4j
public class ShellCommands implements Quit.Command {

	private Commands commands;

	public ShellCommands(Commands commands) {
		log.debug("start");

		this.commands = commands;
		commands.sendMessage("Started CryptoBot!!!", true);

		log.debug("done");
	}

	@ShellMethod(value = "Starts processing a currency from an exchange", key = { "start", "s" })
	public void start(String exchange, String currency)
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {

		log.debug("start. exchange: {} currency: {}", exchange, currency);

		commands.startExchangeCurrency(exchange, currency);

		log.debug("done");
	}

	@ShellMethod(value = "Stops processing a currency from an exchange", key = { "stop", "st" })
	public void stop(String exchange, String currency) {

		log.debug("start. exchange: {} currency: {}", exchange, currency);

		commands.stopExchangeCurrency(exchange, currency);

		log.debug("done");
	}

	@ShellMethod(value = "List current running currencies for each exchange", key = { "list", "l" })
	public void list() {
		log.debug("start");

		commands.listExchangeCurrency();

		log.debug("done");
	}

	@ShellMethod(value = "Ends CryptoBot.", key = { "quit", "exit", "shutdown", "q" })
	public void quit() throws IOException {
		log.debug("start");

		commands.quitApplication();

		log.debug("done");
	}
}
