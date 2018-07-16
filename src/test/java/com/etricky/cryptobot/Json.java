package com.etricky.cryptobot;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.springframework.core.io.ClassPathResource;

import com.etricky.cryptobot.core.exchanges.common.ExchangeException;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeJson;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFilesReader;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class Json {

	public static void main(String[] args) {
		JsonFilesReader jsonFilesReader = new JsonFilesReader();

		try {

			File file = new ClassPathResource("configFiles/exchanges.json").getFile();

			if (file.exists())
				System.out.println("exists");

			ExchangeJson[] exchanges = jsonFilesReader.getJsonObject("exchanges.json", ExchangeJson[].class);

			Arrays.asList(exchanges).forEach(e -> {
				System.out.println("name:" + e.getName());
				e.getCurrencies().forEach(c -> {
					System.out.println("base:" + c.getBase_currency());
					System.out.println("quote:" + c.getQuote_currency());
				});
			});

		} catch (

		JsonParseException e) {

			e.printStackTrace();
		} catch (JsonMappingException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (ExchangeException e1) {

			e1.printStackTrace();
		}
	}
}
