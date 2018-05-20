package com.etricky.cryptobot.service.interfaces.shell;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.commands.Quit;

import com.etricky.cryptobot.CryptoBotApplication;
import com.etricky.cryptobot.service.common.ExitCode;
import com.etricky.cryptobot.service.interfaces.slack.Slack;

@ShellComponent
public class ShellCommands implements Quit.Command {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	Slack slack;
	CryptoBotApplication cryptobotApp;
	ExitCode exitCode;

	public ShellCommands(Slack slack, CryptoBotApplication cryptobotApp, ExitCode exitCode) {
		logger.debug("start");

		this.slack = slack;
		this.cryptobotApp = cryptobotApp;
		this.exitCode = exitCode;
		sendMessage("Started CryptoBot!!!", true);

		logger.debug("done");
	}

	@ShellMethod(value = "Starts processing a currency from an exchange", key = { "start", "s" })
	public void start(String exchange, String currency) throws IOException {
		logger.debug("start. exchange: {} currency: {}",exchange,currency);
		
		sendMessage("Starting thread for exchange: " + exchange + " currency: " + currency, true);

		sendMessage("Started", true);
		
		logger.debug("done");
	}

	@ShellMethod(value = "Ends CryptoBot.", key = { "quit", "exit", "shutdown", "q" })
	public void quit() throws IOException {
		logger.debug("start");
		
		sendMessage("Shuting down CryptoBot!!!", true);

		exitCode.setExitCode(0);
		cryptobotApp.terminate(exitCode);
		
		logger.debug("done");
	}

	private void sendMessage(String msg, boolean toSlack) {
		System.out.println(msg);

		if (toSlack)
			slack.sendMessage(msg);
	}

}
