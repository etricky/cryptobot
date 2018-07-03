package com.etricky.cryptobot.core.interfaces.jsonFiles;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.exchanges.common.ExchangeException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JsonFiles {
	ExchangeJson[] exchangesJson;
	SlackJson slackJson;
	SettingsJson settingsJson;
	JsonFilesReader jsonReader;

	public JsonFiles(JsonFilesReader jsonReader) throws JsonParseException, JsonMappingException, ExchangeException {
		log.debug("start");

		this.jsonReader = jsonReader;
		loadFiles();

		log.debug("done");
	}

	public Map<String, ExchangeJson> getExchangesJson() {
		Map<String, ExchangeJson> mapExc = Arrays.asList(exchangesJson).stream()
				.collect(Collectors.toMap(ExchangeJson::getName, Function.identity()));
		return mapExc;
	}

	public SlackJson getSlackJson() {
		return slackJson;
	}

	public SettingsJson getSettingsJson() {
		return settingsJson;
	}

	public void loadFiles() throws JsonParseException, JsonMappingException, ExchangeException {
		log.debug("start");

		exchangesJson = jsonReader.getJsonObject("exchanges.json", ExchangeJson[].class);
		slackJson = jsonReader.getJsonObject("slack.key.json", SlackJson.class);
		settingsJson = jsonReader.getJsonObject("settings.json", SettingsJson.class);

		log.debug("done");
	}
}
