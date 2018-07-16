package com.etricky.cryptobot.core.interfaces.slack;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.exchanges.common.ExchangeException;
import com.etricky.cryptobot.core.exchanges.common.ExchangeExceptionRT;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.interfaces.jsonFiles.SlackJson;
import com.etricky.cryptobot.core.interfaces.shell.ShellCommands;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Slack implements SlackMessagePostedListener {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	private SlackSession session;
	private SlackChannel channel;
	@Autowired
	private ShellCommands shellCcommands;

	public Slack(JsonFiles jsonFiles) throws IOException {
		logger.debug("creating slack channel");

		SlackJson slackJson = jsonFiles.getSlackJson();
		session = SlackSessionFactory.createWebSocketSlackSession(slackJson.getKey());
		session.addMessagePostedListener(this);
		session.connect();
		channel = session.findChannelByName(slackJson.getChannel()); // make sure bot is a member of the channel.

		logger.debug("done");
	}

	public void sendMessage(String message) {
		logger.debug("start. message: {}", message);

		session.sendMessage(channel, message);

		logger.debug("done");
	}

	public void disconnect() throws IOException {
		logger.debug("start");

		session.disconnect();

		logger.debug("done");
	}

	@Override
	public void onEvent(SlackMessagePosted event, SlackSession session) {
		SlackChannel channelOnWhichMessageWasPosted = event.getChannel();
		String messageContent = event.getMessageContent();
		SlackUser messageSender = event.getSender();

		log.debug("start");

		if (session.sessionPersona().getId().equals(event.getSender().getId())) {
			log.trace("msg not sent by bot: {}", messageSender);
			return;
		}

		if (!channel.getId().equals(event.getChannel().getId())) {
			log.trace("msg not sent in bot channel: {}", channelOnWhichMessageWasPosted);
			return;
		}

		try {
			shellCommand(messageContent);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException | ExchangeException e) {
			log.error("Exception :{}", e);
			throw new ExchangeExceptionRT(e);
		}

		log.debug("done");
	}

	private void shellCommand(String command)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, ExchangeException {

		log.debug("start. command: {}", command);

		String message = shellCcommands.executeCommand(command);

		if (message != null) {
			log.debug("command executed");
			sendMessage(message);
		}

		log.debug("done");
	}

}
