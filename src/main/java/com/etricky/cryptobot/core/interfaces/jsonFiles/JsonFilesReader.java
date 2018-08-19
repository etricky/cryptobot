package com.etricky.cryptobot.core.interfaces.jsonFiles;

import java.io.File;
import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import com.etricky.cryptobot.core.exchanges.common.ExchangeException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JsonFilesReader {
	private static ObjectMapper mapper;
	// @Autowired
	// private ApplicationContext appContext;

	public JsonFilesReader() {
		mapper = new ObjectMapper();
	}

	public <T> T getJsonObject(String path, String jsonFile, Class<T> classObject)
			throws JsonParseException, JsonMappingException, ExchangeException {

		try {
			// return mapper.readValue(new ClassPathResource(path + jsonFile).getFile(),
			// classObject);

			// Resource[] resource = appContext.getResources("classpath*:" + path + jsonFile);
			// log.debug("resource: {}",resource[0].getURL());
			// return mapper.readValue(resource[0].getInputStream(), classObject);

			File file = ResourceUtils.getFile(path + jsonFile);

			if (file != null && file.exists()) {
				log.debug("found file: {}", file.getAbsolutePath());
				return mapper.readValue(file, classObject);
			}

			ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			Resource[] resource = resolver.getResources("classpath*:" + path + jsonFile);
			log.debug("resource: {}", resource[0].getURL());
			return mapper.readValue(resource[0].getInputStream(), classObject);
		} catch (IOException e) {
			log.error("Resource: {} Exception: {}", path + jsonFile, e);
			throw new ExchangeException(e);
		}
	}

	public <T> T getJsonObject(String jsonFile, Class<T> classObject) throws JsonParseException, JsonMappingException, ExchangeException {
		return getJsonObject("configFiles/", jsonFile, classObject);
	}
}
