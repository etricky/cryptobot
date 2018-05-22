package com.etricky.cryptobot.core.interfaces.jsonFiles;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonFilesReader {
	private static ObjectMapper mapper;

	public JsonFilesReader() {
		mapper = new ObjectMapper();
	}

	public <T> T getJsonObject(String path, String jsonFile, Class<T> classObject)
			throws JsonParseException, JsonMappingException, IOException {

		return mapper.readValue(new ClassPathResource(path + jsonFile).getFile(), classObject);
	}

	public <T> T getJsonObject(String jsonFile, Class<T> classObject)
			throws JsonParseException, JsonMappingException, IOException {
		return getJsonObject("configFiles/", jsonFile, classObject);
	}
}
