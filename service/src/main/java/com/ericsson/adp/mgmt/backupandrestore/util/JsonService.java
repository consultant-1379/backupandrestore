/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.exception.JsonParsingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * Responsible for parsing json.
 */
@Service
public class JsonService {

    private static final Logger logger = LogManager.getLogger(JsonService.class);

    /**
     * Creates json string based on object.
     * @param object to be converted.
     * @return json string.
     */
    public static String toJsonString(final Object object) {
        try {
            return getObjectMapper().writeValueAsString(object);
        } catch (final Exception e) {
            throw new JsonParsingException(e);
        }
    }

    /**
     * Creates object based on json string.
     * @param jsonString to be parsed.
     * @param classToParse object's class.
     * @param <T> class to be returned
     * @return object.
     */
    public <T> Optional<T> parseJsonString(final String jsonString, final Class<T> classToParse) {
        try {
            return Optional.of(getObjectMapper().readValue(jsonString, classToParse));
        } catch (final Exception e) {
            logger.error("Error parsing string <{}> - {}", jsonString, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Creates object based on class path resource.
     * @param file to be parsed.
     * @param classToParse object's class.
     * @param <T> class to be returned
     * @return object.
     */
    public <T> Optional<T> parseJsonFromClassPathResource(final String file, final Class<T> classToParse) {
        try {
            return Optional.of(getObjectMapper().readValue(new ClassPathResource(file).getInputStream(), classToParse));
        } catch (final Exception e) {
            logger.error("Error parsing file {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parses json string into JsonObject and fetches specified value from it
     * @param jsonString to be parsed.
     * @param nodePath node to look for the key if key is not in head node
     * @param key to be fetched from json object
     * @return Optional String required value in Json according to key and node path
     */
    public Optional<String> parseJsonStringAndFetchValue(final String jsonString, final String nodePath, final String key) {
        try {
            final ObjectNode jsonObject = getObjectMapper().readValue(jsonString, ObjectNode.class);
            if (jsonObject.has(key)) {
                return Optional.of(jsonObject.get(key).textValue());
            }
            if (nodePath != null) {
                final JsonNode node = jsonObject.findPath(nodePath);
                if ((!node.isMissingNode()) && node.has(key)) {
                    return Optional.of(node.get(key).textValue());
                }
            }
            return Optional.empty();
        } catch (final Exception e) {
            logger.error("Error fetching <{}> from json string <{}> - {}", key, jsonString, e.getMessage());
            return Optional.empty();
        }
    }

    private static ObjectMapper getObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

}
