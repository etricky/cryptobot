package com.etricky.cryptobot;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.etricky.cryptobot.core.common.ExitCode;
import com.etricky.cryptobot.core.interfaces.slack.Slack;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@Slf4j
public class CryptoBotApplication {

	@Autowired
	Slack slack;

	@Autowired
	ApplicationContext appContext;

	public static void main(String[] args) {
		// SpringApplication app = new SpringApplication(CryptoBotApplication.class);

		ConfigurableApplicationContext ctx = SpringApplication.run(CryptoBotApplication.class, args);
		ctx.registerShutdownHook();
	}

	public void terminate(ExitCode exitCode) throws IOException {
		log.debug("start. exitCode: {}", exitCode);

		slack.sendMessage("Bye!!!");
		slack.disconnect();
		SpringApplication.exit(appContext, exitCode);

		log.debug("Exited CryptoBot!!!");
		System.exit(exitCode.getExitCode());
	}
}
