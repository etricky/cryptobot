package com.etricky.cryptobot;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

import com.etricky.cryptobot.core.common.exceptions.ExchangeException;
import com.etricky.cryptobot.core.interfaces.jsonFiles.ExchangeJson;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFilesReader;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class Json {

	public static void main(String[] args) {
		JsonFilesReader jsonFilesReader = new JsonFilesReader();
		File file;
		URL url;
		try {
			System.out.println("Working Directory = " + System.getProperty("user.dir"));

			file = new File("configFiles/exchanges.json");

			if (file != null && file.exists()) {
				System.out.println("exists");
				System.out.println("path: " + file.getAbsolutePath());
			} else {
				System.out.println("null file");
			}

			file = ResourceUtils.getFile("configFiles/exchanges.json");

			if (file != null && file.exists()) {
				System.out.println("exists");
				System.out.println("path: " + file.getAbsolutePath());
			} else {
				System.out.println("null file");
			}

			url = ResourceUtils.getURL("configFiles/exchanges2.json");
			if (url != null) {
				System.out.println("URL path: " + url.getPath());
			} else {
				System.out.println("empty URL");
			}

			url = ClassUtils.getDefaultClassLoader().getResource("configFiles/exchanges.json");

			if (url != null) {
				System.out.println("URL path: " + url.getPath());
			} else {
				System.out.println("empty URL");
			}

			file = new ClassPathResource("configFiles/exchanges.json").getFile();

			if (file != null && file.exists()) {
				System.out.println("exists");
				System.out.println("path: " + file.getAbsolutePath());
			} else {
				System.out.println("null file");
			}

			ExchangeJson[] exchanges = jsonFilesReader.getJsonObject("exchanges.json", ExchangeJson[].class);

			Arrays.asList(exchanges).forEach(e -> {
				System.out.println("name:" + e.getName());
				e.getCurrencyPairs().forEach(c -> {
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
