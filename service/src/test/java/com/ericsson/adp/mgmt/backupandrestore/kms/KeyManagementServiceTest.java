/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/

package com.ericsson.adp.mgmt.backupandrestore.kms;

import com.ericsson.adp.mgmt.backupandrestore.caching.Cached;
import com.ericsson.adp.mgmt.backupandrestore.util.RestTemplateFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

public class KeyManagementServiceTest {
    private static final Path fakeJwt = Path.of(System.getProperty("java.io.tmpdir"),"fakeJwt");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Base64.Encoder encoder =  Base64.getEncoder();

    private KeyManagementService keyManagementService;
    private KeyManagementService.RequestSettings settings;
    private RestTemplateFactory templateFactory;
    private Cached<RestTemplate> template;

    @Before
    public void prepare() throws IOException {
        Files.write(fakeJwt, Collections.singleton("this is fake jwt data used for testing"));

        keyManagementService = new KeyManagementService();
        templateFactory = EasyMock.createMock(RestTemplateFactory.class);
        final RestTemplate inner = EasyMock.createMock(RestTemplate.class);
        template = new Cached<>(() -> inner);
        expect(templateFactory.getRestTemplate(KeyManagementService.REST_TEMPLATE_ID, RestTemplateFactory.Security.NONE))
                .andReturn(template).anyTimes();
        replay(templateFactory);
        settings = new KeyManagementService.RequestSettings(fakeJwt, "irrelevant role", "irrelevant key name");

        keyManagementService.setHostname("irrelevanthostname");
        keyManagementService.setPort(8200);
        keyManagementService.setRestTemplateConfiguration(templateFactory, false);
    }

    @Test
    public void encryptData() {
        expect(template.get().exchange(anyObject(), eq(JsonNode.class)))
                .andReturn(getTokenResponse("irrelevant_token", 30)).once();
        expect(template.get().exchange(anyObject(), eq(JsonNode.class)))
                .andReturn(getEncryptResponse("irrelevant_ciphertext")).once();
        replay(template.get());

        assertEquals("irrelevant_ciphertext", keyManagementService.encrypt("irrelevant plaintext", settings));

        verify(template.get());
    }

    @Test
    public void decryptData() {
        expect(template.get().exchange(anyObject(), eq(JsonNode.class)))
                .andReturn(getTokenResponse("irrelevant_token", 30)).once();
        expect(template.get().exchange(anyObject(), eq(JsonNode.class)))
                .andReturn(getDecryptResponse(encoder.encodeToString("irrelevant_plaintext".getBytes()))).once();
        replay(template.get());

        assertEquals("irrelevant_plaintext", keyManagementService.decrypt("irrelevant plaintext", settings));

        verify(template.get());
    }

    @Test
    public void encryptTwice_tokenDoesntExpire_getNewTokenAndDoRequest() throws InterruptedException {
        expect(template.get().exchange(anyObject(), eq(JsonNode.class)))
                .andReturn(getTokenResponse("irrelevant_token", 120)).once();
        expect(template.get().exchange(anyObject(), eq(JsonNode.class)))
                .andReturn(getEncryptResponse("irrelevant_ciphertext")).times(2);

        replay(template.get());

        assertEquals("irrelevant_ciphertext", keyManagementService.encrypt("irrelevant plaintext", settings));
        assertEquals("irrelevant_ciphertext", keyManagementService.encrypt("irrelevant plaintext", settings));

        verify(template.get());
    }


    @Test
    public void encryptAfterTokenRefresh() throws InterruptedException {
        expect(template.get().exchange(anyObject(), eq(JsonNode.class)))
                .andReturn(getTokenResponse("irrelevant_token", 120)).times(1);
        expect(template.get().exchange(anyObject(), eq(JsonNode.class)))
                .andReturn(getEncryptResponse("irrelevant_ciphertext")).times(1);
        replay(template.get());

        keyManagementService.refreshToken(settings);
        assertEquals("irrelevant_ciphertext", keyManagementService.encrypt("irrelevant plaintext", settings));

        verify(template.get());
    }

    @Test
    public void encryptTwice_tokenExpires_getNewTokenAndDoRequest() throws InterruptedException {
        expect(template.get().exchange(anyObject(), eq(JsonNode.class)))
                .andReturn(getTokenResponse("irrelevant_token", 62)).once(); // Token is treated as "expired" when lease has < 60 seconds left
        expect(template.get().exchange(anyObject(), eq(JsonNode.class)))
                .andReturn(getEncryptResponse("irrelevant_ciphertext")).once();
        expect(template.get().exchange(anyObject(), eq(JsonNode.class)))
                .andReturn(getTokenResponse("irrelevant_token", 120)).once();
        expect(template.get().exchange(anyObject(), eq(JsonNode.class)))
                .andReturn(getEncryptResponse("irrelevant_ciphertext")).once();

        replay(template.get());

        assertEquals("irrelevant_ciphertext", keyManagementService.encrypt("irrelevant plaintext", settings));
        final OffsetDateTime start = OffsetDateTime.now();
        Awaitility.waitAtMost(5, TimeUnit.SECONDS)
            .until(() -> OffsetDateTime.now().isAfter(start.plusSeconds(4))); // Workaround for sonarqubes pointless rules about thread.sleep
        assertEquals("irrelevant_ciphertext", keyManagementService.encrypt("irrelevant plaintext", settings));

        verify(template.get());
    }

    @After
    public void cleanup() throws IOException {
        if (Files.exists(fakeJwt)) {
            Files.delete(fakeJwt);
        }
    }

    private ResponseEntity<JsonNode> getTokenResponse(final String token, final int lease) {
        final JsonNode resData = mapper.createObjectNode()
                .set("auth", mapper.createObjectNode()
                .put("client_token", token)
                .put("lease_duration", lease));
        return ResponseEntity.ok(resData);
    }

    private ResponseEntity<JsonNode> getEncryptResponse(final String ciphertext) {
        final JsonNode resData = mapper.createObjectNode()
                .set("data", mapper.createObjectNode()
                .put("ciphertext", ciphertext));
        return ResponseEntity.ok(resData);
    }

    private ResponseEntity<JsonNode> getDecryptResponse(final String plaintext) {
        final JsonNode resData = mapper.createObjectNode()
                .set("data", mapper.createObjectNode()
                        .put("plaintext", plaintext));
        return ResponseEntity.ok(resData);
    }
}
