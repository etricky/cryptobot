package com.etricky.cryptobot.core.interfaces.slack;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.interfaces.jsonFiles.SlackJson;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;

@Component
public class Slack {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	private SlackSession session;
	private SlackChannel channel;
	

	public Slack(JsonFiles jsonFiles) throws IOException {
		logger.debug("creating slack channel");

		SlackJson slackJson = jsonFiles.getSlackJson();
		session = SlackSessionFactory.createWebSocketSlackSession(slackJson.getKey());
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

}
