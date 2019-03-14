package com.etricky.cryptobot.core.interfaces.shell;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Shell;
import org.springframework.shell.result.ThrowableResultHandler;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.standard.commands.Help;
import org.springframework.shell.standard.commands.Quit;

import com.etricky.cryptobot.core.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.interfaces.Commands;

import lombok.extern.slf4j.Slf4j;

@ShellComponent
@Slf4j
public class ShellCommands implements Quit.Command {

	public enum ShellCommandsEnum {
		START("start", "s"), BACKTEST("backtest", "bt"), STOP("stop", "st"), LIST("list", "l"), QUIT("quit", "q"),
		HELP("help", null), STACKTRACE("stacktrace", null), RELOADFCONFIGS("reload", "rl"), WALLET("wallet", "w"),
		BACKFILL("backfill", "b"), LISTTRADES("listTrades", "lt"), LISTORDERS("listOrders", "lo"),
		CANCELALLORDERS("cancelOrdersAll", "coa"), CANCELORDER("cancelOrder", "co");

		public String command;
		public String shortCommand;

		public String getCommand() {
			return command;
		}

		public String getShortCommand() {
			return shortCommand;
		}

		ShellCommandsEnum(String command, String shortCommand) {
			this.command = command;
			this.shortCommand = shortCommand;
		}

		public static boolean validCommand(String command) {
			boolean result = false;
			if (command.equals(ShellCommandsEnum.START.command)) {
				result = true;
			} else if (command.equals(ShellCommandsEnum.QUIT.command)) {
				result = true;
			} else if (command.equals(ShellCommandsEnum.RELOADFCONFIGS.command)) {
				result = true;
			} else if (command.equals(ShellCommandsEnum.WALLET.command)) {
				result = true;
			} else if (command.equals(ShellCommandsEnum.HELP.command)) {
				result = true;
			} else if (command.equals(ShellCommandsEnum.STACKTRACE.command)) {
				result = true;
			} else if (command.equals(ShellCommandsEnum.BACKFILL.command)) {
				result = true;
			} else if (command.equals(ShellCommandsEnum.BACKTEST.command)) {
				result = true;
			} else if (command.equals(ShellCommandsEnum.STOP.command)) {
				result = true;
			} else if (command.equals(ShellCommandsEnum.LIST.command)) {
				result = true;
			} else if (command.equals(ShellCommandsEnum.LISTTRADES.command)) {
				result = true;
			} else if (command.equals(ShellCommandsEnum.LISTORDERS.command)) {
				result = true;
			} else if (command.equals(ShellCommandsEnum.CANCELALLORDERS.command)) {
				result = true;
			} else if (command.equals(ShellCommandsEnum.CANCELORDER.command)) {
				result = true;
			}
			return result;
		}

	}

	@Autowired
	Help shellHelp;
	@Autowired
	private ThrowableResultHandler throwableResultHandler;
	@Autowired
	Shell shell;

	private Commands commands;

	public ShellCommands(Commands commands) {
		log.debug("start");

		this.commands = commands;

		log.debug("done");
	}

	/**
	 * Executes the command that was entered in an external application
	 * 
	 * @param command Command to be executed
	 * @param source  External application identification
	 * @return Outcome of the command execution
	 */
	public String executeCommand(String command, String source) {
		boolean execute = false, validCommand = false;
		String txt = null;
		CharSequence cs;

		log.debug("start. command: {} source: {}", command, source);

		try {
			String[] arguments = command.split("\\s+");

			if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.START.command)
					|| arguments[0].equalsIgnoreCase(ShellCommandsEnum.START.shortCommand)) {

				log.debug("command matched: {}", ShellCommandsEnum.START.command);
				execute = true;

			} else if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.QUIT.command)
					|| arguments[0].equalsIgnoreCase(ShellCommandsEnum.QUIT.shortCommand)) {

				log.debug("command matched: {}", ShellCommandsEnum.QUIT.command);
				execute = true;

			} else if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.RELOADFCONFIGS.command)
					|| arguments[0].equalsIgnoreCase(ShellCommandsEnum.RELOADFCONFIGS.shortCommand)) {

				log.debug("command matched: {}", ShellCommandsEnum.RELOADFCONFIGS.command);
				execute = true;

			} else if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.BACKTEST.command)
					|| arguments[0].equalsIgnoreCase(ShellCommandsEnum.BACKTEST.shortCommand)) {

				log.debug("command matched: {}", ShellCommandsEnum.BACKTEST.command);
				execute = true;

			} else if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.STOP.command)
					|| arguments[0].equalsIgnoreCase(ShellCommandsEnum.STOP.shortCommand)) {

				log.debug("command matched: {}", ShellCommandsEnum.STOP.command);
				execute = true;

			} else if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.WALLET.command)
					|| arguments[0].equalsIgnoreCase(ShellCommandsEnum.WALLET.shortCommand)) {

				log.debug("command matched: {}", ShellCommandsEnum.WALLET.command);
				execute = true;

			} else if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.BACKFILL.command)
					|| arguments[0].equalsIgnoreCase(ShellCommandsEnum.BACKFILL.shortCommand)) {

				log.debug("command matched: {}", ShellCommandsEnum.BACKFILL.command);
				execute = true;

			} else if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.LIST.command)
					|| arguments[0].equalsIgnoreCase(ShellCommandsEnum.LIST.shortCommand)) {

				log.debug("command matched: {}", ShellCommandsEnum.LIST.command);
				listTradingThreads(true);

			} else if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.LISTTRADES.command)
					|| arguments[0].equalsIgnoreCase(ShellCommandsEnum.LISTTRADES.shortCommand)) {

				log.debug("command matched: {}", ShellCommandsEnum.LISTTRADES.command);
				listExchangeTrades(true);

			} else if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.LISTORDERS.command)
					|| arguments[0].equalsIgnoreCase(ShellCommandsEnum.LISTORDERS.shortCommand)) {

				log.debug("command matched: {}", ShellCommandsEnum.LISTORDERS.command);
				listExchangeOrders(arguments[1], true);

			} else if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.CANCELALLORDERS.command)
					|| arguments[0].equalsIgnoreCase(ShellCommandsEnum.CANCELALLORDERS.shortCommand)) {

				log.debug("command matched: {}", ShellCommandsEnum.CANCELALLORDERS.command);
				cancelExchangeOrders(arguments[1], true);

			} else if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.CANCELORDER.command)
					|| arguments[0].equalsIgnoreCase(ShellCommandsEnum.CANCELORDER.shortCommand)) {

				log.debug("command matched: {}", ShellCommandsEnum.CANCELALLORDERS.command);
				cancelExchangeOrder(arguments[1], arguments[2], true);

			} else if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.HELP.command)) {

				log.debug("command matched: {}", ShellCommandsEnum.HELP.command);

				try {
					// remove the command otherwise it would execute help help
					if (arguments.length == 2) {

						if (ShellCommandsEnum.validCommand(arguments[1])) {
							command = arguments[1];
							validCommand = true;
						} else {
							log.debug("invalid help command");
							txt = "Invalid command!!!\n\n";
							cs = shellHelp.help(null);
							txt = txt.concat(cs.toString());
						}
					} else if (arguments.length > 2) {
						log.debug("invalid help command");
						txt = "Invalid command!!!\n\n";
						cs = shellHelp.help(null);
						txt = txt.concat(cs.toString());
					} else {
						command = null;
						validCommand = true;
					}

					if (validCommand) {
						// executes shell help command of the command provided
						cs = shellHelp.help(command);
						txt = cs.toString();
					}
				} catch (IOException e) {
					log.error("Exception: {}", e);
					throw new ExchangeException(e);
				}

			} else if (arguments[0].equalsIgnoreCase(ShellCommandsEnum.STACKTRACE.command)) {
				log.debug("command matched: {}", ShellCommandsEnum.STACKTRACE.command);

				if (throwableResultHandler.getLastError() != null) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					txt = throwableResultHandler.getLastError().getMessage().concat("\n");
					throwableResultHandler.getLastError().printStackTrace(pw);
					txt = txt.concat(sw.toString());
				}

			} else {
				log.debug("command didn't matched");
				txt = "Invalid command!!!\n\n";
				cs = shellHelp.help(null);
				txt = txt.concat(cs.toString());
			}

			if (execute) {
				ShellInputParser shellInputParser = new ShellInputParser(command);
				commands.sendMessage("command from " + command + " source: " + source, false);
				shell.run(shellInputParser);
			}
		} catch (Exception e) {
			commands.exceptionHandler(e);
		}

		log.debug("done");
		return txt;
	}

	@ShellMethod(value = "Starts processing a trade from an exchange", key = { "start", "s" })
	public void start(String exchange, String tradeName,
			@ShellOption(defaultValue = "0", help = "0 - All, 1 - History Only, 2 - Dry Run Trades") int tradeType) {

		log.debug("start. exchange: {} tradeName: {} tradeType: {}", exchange, tradeName, tradeType);

		commands.startExchangeTrade(exchange.toUpperCase(), tradeName.toUpperCase(), tradeType);

		log.debug("done");
	}

	@ShellMethod(value = "Reloads config files", key = { "reload", "rl" })
	public void reload() {
		log.debug("start");

		commands.reloadConfigs();

		log.debug("done");
	}

	@ShellMethod(value = "Ends CryptoBot.", key = { "quit", "exit", "shutdown", "q" })
	public void quit() {
		log.debug("start");

		commands.quitApplication();

		log.debug("done");
	}

	@ShellMethod(value = "Wallet info.", key = { "wallet", "w" })
	public void wallet(String exchange) {
		log.debug("start. exchange: {}");

		commands.printWalletInfo(exchange);

		log.debug("done");
	}

	@ShellMethod(value = "Backtests the strategy for a trade from an exchange. Parameters: History Days (Optional)", key = {
			"backtest", "bt" })
	public void backtest(String exchange,
			@ShellOption(defaultValue = "All", help = "All or a specific trade name") String tradeName,
			@ShellOption(defaultValue = "0", help = "0 - period defined by start and end date, > 0 - period from now minus history days") int historyDays,
			@ShellOption(defaultValue = "1970-01-01", help = "Start date with format yyyy-mm-dd") String startDate,
			@ShellOption(defaultValue = "1970-01-02", help = "End date with format yyyy-mm-dd") String endDate) {

		log.debug("start. exchange: {} tradeName: {} historyDays: {} startDate: {} endDate: {}", exchange, tradeName,
				historyDays, startDate, endDate);

		commands.backtest(exchange, tradeName, historyDays, startDate, endDate);

		log.debug("done");
	}

	@ShellMethod(value = "Backfills trade history", key = { "backfill", "b" })
	public void backfill(String exchange) {

		log.debug("start. exchange: {}", exchange);

		commands.backFill(exchange.toUpperCase());

		log.debug("done");
	}

	@ShellMethod(value = "Stops processing a trade from an exchange", key = { "stop", "st" })
	public void stop(String exchange,
			@ShellOption(defaultValue = "All", help = "All or a specific trade name") String tradeName) {

		log.debug("start. exchange: {} tradeName: {}", exchange, tradeName);

		commands.stopExchangeTrade(exchange, tradeName);

		log.debug("done");
	}

	@ShellMethod(value = "List current running trades for each exchange", key = { "list", "l" })
	public void list() {
		log.debug("start");

		listTradingThreads(false);

		log.debug("done");
	}

	@ShellMethod(value = "List available trades for each exchange", key = { "listTrades", "lt" })
	public void listTrades() {
		log.debug("start");

		listExchangeTrades(false);

		log.debug("done");
	}

	@ShellMethod(value = "List open orders in the exchange", key = { "listOrders", "lo" })
	public void listExchangeOrders(String exchange) {
		log.debug("start");

		listExchangeOrders(exchange, false);

		log.debug("done");
	}

	@ShellMethod(value = "Cancels all open orders in the exchange", key = { "cancelOrdersAll", "coa" })
	public void cancelExchangeOrders(String exchange) {
		log.debug("start");

		cancelExchangeOrders(exchange, false);

		log.debug("done");
	}

	@ShellMethod(value = "Cancels a open orders in the exchange", key = { "cancelOrder", "co" })
	public void cancelExchangeOrder(String exchange, String currencyPair) {
		log.debug("start");

		cancelExchangeOrder(exchange, currencyPair, false);

		log.debug("done");
	}

	@ShellMethod(value = "Runs a generic test", key = { "test" })
	public void genericTest(@ShellOption(defaultValue = "") String arg1, @ShellOption(defaultValue = "") String arg2,
			@ShellOption(defaultValue = "") String arg3, @ShellOption(defaultValue = "") String arg4,
			@ShellOption(defaultValue = "") String arg5) {
		String[] args = new String[5];
		log.debug("start");

		args[0] = arg1;
		args[1] = arg2;
		args[2] = arg3;
		args[3] = arg4;
		args[4] = arg5;

		commands.genericTest(args);

		log.debug("done");
	}

	//
	//
	//
	// PRIVATE METHODS
	public void cancelExchangeOrders(String exchange, boolean toExternalApp) {
		log.debug("start. exchange: {} toExternalApp: {}", exchange, toExternalApp);

		commands.cancelExchangeOrders(exchange, toExternalApp);

		log.debug("done");
	}

	public void cancelExchangeOrder(String exchange, String currencyPair, boolean toExternalApp) {
		log.debug("start. exchange: {} orderId: {} toExternalApp: {}", exchange, currencyPair, toExternalApp);

		commands.cancelExchangeOrder(exchange, currencyPair, toExternalApp);

		log.debug("done");
	}

	private void listExchangeOrders(String exchange, boolean toExternalApp) {
		log.debug("start. exchange: {} toExternalApp: {}", exchange, toExternalApp);

		commands.listExchangeOrders(exchange, toExternalApp);

		log.debug("done");
	}

	private void listExchangeTrades(boolean toExternalApp) {
		log.debug("start. toExternalApp: {}", toExternalApp);

		commands.listExchangeTrades(toExternalApp);

		log.debug("done");
	}

	private void listTradingThreads(boolean toExternalApp) {
		log.debug("start");

		commands.listExchangeTradingThreads(toExternalApp);

		log.debug("done");
	}
}
