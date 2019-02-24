package com.etricky.cryptobot.core.interfaces.jsonFiles;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.common.exceptions.ExchangeException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JsonFiles {
	ExchangeJson[] exchangesJson;
	SlackJson slackJson;
	SettingsJson settingsJson;
	JsonFilesReader jsonReader;
	StrategiesJson[] strategiesJson;

	public JsonFiles(JsonFilesReader jsonReader) throws ExchangeException {
		log.debug("start");

		this.jsonReader = jsonReader;
		initialize();

		log.debug("done");
	}

	public Map<String, ExchangeJson> getExchangesJsonMap() {
		Map<String, ExchangeJson> mapExc = Arrays.asList(exchangesJson).stream()
				.collect(Collectors.toMap(ExchangeJson::getName, Function.identity()));
		return mapExc;
	}

	public Map<String, StrategiesJson> getStrategiesJsonMap() {
		Map<String, StrategiesJson> mapExc = Arrays.asList(strategiesJson).stream()
				.collect(Collectors.toMap(StrategiesJson::getBean, Function.identity()));
		return mapExc;
	}

	public SlackJson getSlackJson() {
		return slackJson;
	}

	public SettingsJson getSettingsJson() {
		return settingsJson;
	}

	public void initialize() throws ExchangeException {
		log.debug("start");

		exchangesJson = jsonReader.getJsonObject("exchanges.json", ExchangeJson[].class);
		strategiesJson = jsonReader.getJsonObject("strategies.json", StrategiesJson[].class);
		slackJson = jsonReader.getJsonObject("slack.key.json", SlackJson.class);
		settingsJson = jsonReader.getJsonObject("settings.json", SettingsJson.class);

		log.debug("done");
	}

	public ExchangeKeys getExchangeKeys(String exchange) throws ExchangeException {
		log.debug("exchange: {}", exchange);

		return jsonReader.getJsonObject(exchange.toLowerCase() + ".key.json", ExchangeKeys.class);
	}
}
