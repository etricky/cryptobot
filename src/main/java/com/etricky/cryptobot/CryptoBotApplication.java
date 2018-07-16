package com.etricky.cryptobot;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.etricky.cryptobot.core.common.ExitCode;
import com.etricky.cryptobot.core.interfaces.Commands;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@Slf4j
public class CryptoBotApplication {

	private static ConfigurableApplicationContext ctx;
	@Autowired
	Commands commands;
	@Autowired
	ExitCode exitCode;

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // It will set UTC timezone
		log.info("application running in UTC timezone: {}", ZonedDateTime.now()); // It will print UTC timezone

		commands.sendMessage("Started CryptoBot!!!", true);
	}

	@PreDestroy
	public void onDestroy() throws Exception {
		if (exitCode.getExitCode() == 0) {
			commands.sendMessage("Bye", true);
		} else {
			commands.sendMessage("Abnormal exit!!! " + exitCode.getExitCode(), true);
		}
	}

	public static void main(String[] args) {
		// SpringApplication app = new SpringApplication(CryptoBotApplication.class);

		try {
			ctx = SpringApplication.run(CryptoBotApplication.class, args);
			ctx.registerShutdownHook();
		} catch (Exception e) {
			log.error("Exception: {}", e);
		}
	}

	@EventListener(ApplicationFailedEvent.class)
	public void applicationFailedEvent() {
		ExitCode ex;

		log.debug("start");

		try {
			ex = new ExitCode();
			ex.setExitCode(ExitCode.EXIT_CODE_ERROR);
			commands.sendMessage("CryptoBot failed to start", true);
			commands.terminate(ex);
		} catch (IOException e) {
			log.error("Exception: {}", e);
		} finally {
			ctx.close();
			System.exit(ExitCode.EXIT_CODE_ERROR);
		}
	}
}
