package com.etricky.cryptobot.service.interfaces.jsonFiles;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JsonFiles {
	ExchangeJson[] exchangesJson;
	SlackJson slackJson;
	JsonFilesReader jsonReader;

	public JsonFiles(JsonFilesReader jsonReader) throws JsonParseException, JsonMappingException, IOException {
		log.debug("start");

		this.jsonReader = jsonReader;

		exchangesJson = jsonReader.getJsonObject("exchanges.json", ExchangeJson[].class);
		slackJson = jsonReader.getJsonObject("slack.key.json", SlackJson.class);

		log.debug("done");
	}

	public ExchangeJson[] getExchangesJson() {
		return exchangesJson;
	}

	public SlackJson getSlackJson() {
		return slackJson;
	}
}
