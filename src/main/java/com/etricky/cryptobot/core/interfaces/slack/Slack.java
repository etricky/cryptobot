package com.etricky.cryptobot.core.interfaces.slack;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeExceptionRT;
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

	private SlackSession session;
	private SlackChannel channel;
	@Autowired
	private ShellCommands shellCcommands;
	private static String slack = "slack";
	private SlackJson slackJson;

	public Slack(JsonFiles jsonFiles) {
		log.debug("creating slack channel");

		slackJson = jsonFiles.getSlackJson();
		session = SlackSessionFactory.createWebSocketSlackSession(slackJson.getKey());
		session.addMessagePostedListener(this);

		connect(false);

		log.debug("done");
	}

	private void connect(boolean reconnect) {
		try {
			session.connect();
			channel = session.findChannelByName(slackJson.getChannel()); // make sure bot is a member of the channel.
		} catch (Exception e) {
			if (!reconnect) {
				log.error("Exception: {}", e);
			}
		}
	}

	public void sendMessage(String message) {
		log.debug("start. message: {}", message);

		if (session.isConnected()) {
			session.sendMessage(channel, message);
		} else {
			log.warn("No connection to Slack!");
			connect(true);
		}

		log.debug("done");
	}

	public void disconnect() throws IOException {
		log.debug("start");

		if (session.isConnected()) {
			session.disconnect();
		} else {
			log.warn("No connection to Slack!");
			connect(true);
		}

		log.debug("done");
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
		} catch (Exception e) {
			log.error("Exception :{}", e);
			throw new ExchangeExceptionRT(e);
		}

		log.debug("done");
	}

	private void shellCommand(String command) {

		log.debug("start. command: {}", command);

		String message = shellCcommands.executeCommand(command, slack);

		if (message != null) {
			log.debug("command executed");
			sendMessage(message);
		}

		log.debug("done");
	}

}
