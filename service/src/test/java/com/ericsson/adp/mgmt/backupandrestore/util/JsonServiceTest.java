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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.ericsson.adp.mgmt.backupandrestore.cminterface.nacm.NACMRole;
import com.ericsson.adp.mgmt.backupandrestore.exception.JsonParsingException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.validation.constraints.NotNull;

public class JsonServiceTest {

    private JsonService jsonService;

    @Before
    public void setup() {
        jsonService = new JsonService();
    }

    @Test
    public void toJsonString_object_objectAsJsonString() throws Exception {
        final Map<String, Object> objectToBeTransformed = new HashMap<>();
        objectToBeTransformed.put("a", 1);
        objectToBeTransformed.put("b", "2");

        final String jsonString = jsonService.toJsonString(objectToBeTransformed);

        assertEquals("{\"a\":1,\"b\":\"2\"}", jsonString);
    }

    @Test(expected = JsonParsingException.class)
    public void toJsonString_exceptionWhileParsing_throwsException() throws Exception {
        final String result = jsonService.toJsonString(new NonSerializableClass(null));
        assertTrue(result.equalsIgnoreCase("{}"));
    }

    @Test
    public void parseJsonString_jsonString_optionalWithJsonObject() throws Exception {
        final Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("a", 1);
        expectedMap.put("b", "2");

        final Optional<JsonNode> result = jsonService.parseJsonString("{\"a\":1,\"b\":\"2\"}", JsonNode.class);

        assertTrue(result.isPresent());

        final JsonNode jsonNode = result.get();

        assertEquals(1, jsonNode.get("a").asInt());
        assertEquals("2", jsonNode.get("b").asText());
    }

    @Test
    public void parseJsonString_errorWhileParsing_returnsEmptyOptional() throws Exception {
        final Optional<String> result = jsonService.parseJsonString("{\"a\":1,}", String.class);

        assertFalse(result.isPresent());
    }

    @Test
    public void parseJsonFromClassPathResource_classPathResource_returnsOptionalWithObject() throws Exception {
        final Optional<NACMRole> result = jsonService.parseJsonFromClassPathResource("system-admin.json", NACMRole.class);

        assertTrue(result.isPresent());
    }

    @Test
    public void parseJsonStringAndFetchValue_hasKey_FetchesValue() throws IOException {
        final String jsonString = Files.readString(new ClassPathResource("system-admin.json").getFile().toPath());
        assertEquals("ericsson-brm-1-system-admin", jsonService.parseJsonStringAndFetchValue(jsonString, null, "name").get());
    }

    @Test
    public void parseJsonStringAndFetchValue_doesNotHaveKey_FetchesValue() throws IOException {
        final String jsonString = Files.readString(new ClassPathResource("system-admin.json").getFile().toPath());
        assertEquals(Optional.empty(), jsonService.parseJsonStringAndFetchValue(jsonString, null, "val"));
    }
    
    @Test
    public void parseJsonStringAndFetchValue_hasNodePath_FetchesValue() throws IOException {
        final String jsonString = "{\n"
                + "        \"name\": \"ericsson-brm\",\n"
                + "        \"title\": \"ericsson-brm\",\n"
                + "        \"jsonSchema\": {\n"
                + "                \"type\": \"object\",\n"
                + "                \"title\": \"ericsson-brm\",\n"
                + "                \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                + "                \"eric-adp-version\": \"1.3.0\"\n"
                + "        }\n"
                + "}" ;
        assertEquals("1.3.0", jsonService.parseJsonStringAndFetchValue(jsonString, "jsonSchema", "eric-adp-version").get());
    }

    @Test
    public void parseJsonStringAndFetchValue_invalidJsonString_returnsEmptyOptional() throws IOException {
        final String jsonString = "{\n"
                + "    \"group\": [\n"
                + "        \"system-admin\"\n"
                + "    ],\n"
                + "    \"name\": \"ericsson-brm-1-system-admin\",\n"
                + "    \"rule\": [\n"
                + "        {\n"
                + "            \"name\": \"ericsson-brm-system-admin-rule\",\n"
                + "    ]\n"
                + "}";
        assertEquals(Optional.empty(), jsonService.parseJsonStringAndFetchValue(jsonString, null, "name"));
    }

    @JsonSerialize(using = NonSerializableClassSerializer.class)
    private class NonSerializableClass {
        private String data;

        public NonSerializableClass(String data) {
            this.data = data;
        }
        public String getData() {
            return data;
        }
    }

    class NonSerializableClassSerializer extends JsonSerializer<NonSerializableClass> {
        @Override
        public void serialize(NonSerializableClass value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            throw new JsonProcessingException("NonSerializableClass cannot be serialized") {};
            
        }
    }

}
