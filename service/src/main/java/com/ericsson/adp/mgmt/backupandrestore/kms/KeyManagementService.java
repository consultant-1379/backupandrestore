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

import static com.ericsson.adp.mgmt.backupandrestore.util.RestTemplateFactory.Security.NONE;
import static com.ericsson.adp.mgmt.backupandrestore.util.RestTemplateFactory.Security.TLS;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ericsson.adp.mgmt.backupandrestore.caching.Cached;
import com.ericsson.adp.mgmt.backupandrestore.exception.KMSRequestFailedException;
import com.ericsson.adp.mgmt.backupandrestore.util.RestTemplateFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Service for interacting with KMS
 * */
@Service
public class KeyManagementService {
    public static final String REST_TEMPLATE_ID = "KMS";
    private static final String TOKEN_HEADER = "X-Vault-Token";
    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final Base64.Decoder decoder = Base64.getDecoder();

    private static final Logger log = LogManager.getLogger(KeyManagementService.class);

    private Cached<RestTemplate> templateCache;
    private String hostname;
    private int port;
    private boolean tlsEnabled;

    private final Map<String, Token> tokens = new ConcurrentHashMap<>();

    /**
     * Encrypt some data using the KMS
     * @param clearText - data to be encrypted
     * @param settings - settings of request
     * @return some encrypted data
     * */
    public String encrypt(final String clearText, final RequestSettings settings) {
        final ObjectNode requestData = new ObjectMapper()
                .createObjectNode()
                .put("plaintext", encoder.encodeToString(clearText.getBytes()));
        final RequestEntity<JsonNode> request = buildRequestEntity(
                settings, RequestType.ENCRYPT, requestData, TOKEN_HEADER, getToken(settings).clientToken);
        final ResponseEntity<JsonNode> response = getTemplate().exchange(request, JsonNode.class);
        successGuard(response);
        return Objects.requireNonNull(response.getBody()).get("data").get("ciphertext").textValue();
    }

    /**
     * Decrypt some data using the KMS
     * @param cipherText - data to be decrypted
     * @param settings - settings of request
     * @return some plaintext data
     * */
    public String decrypt(final String cipherText, final RequestSettings settings) {
        final ObjectNode requestData = new ObjectMapper()
                .createObjectNode()
                .put("ciphertext", cipherText);
        final RequestEntity<JsonNode> request = buildRequestEntity(
                settings, RequestType.DECRYPT, requestData, TOKEN_HEADER, getToken(settings).clientToken);
        final ResponseEntity<JsonNode> response = getTemplate().exchange(request, JsonNode.class);
        successGuard(response);
        return new String(decoder.decode(
                Objects.requireNonNull(response.getBody()).get("data").get("plaintext").textValue()));
    }

    /**
     * Setup the rest template used to talk to KMS
     * @param restTemplateFactory - the template factory to use
     * @param tlsEnabled - true if TLS is enabled, else false
     * */
    @Autowired
    public void setRestTemplateConfiguration(final RestTemplateFactory restTemplateFactory, @Value("${global.tls:true}") final boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
        templateCache = restTemplateFactory.getRestTemplate(REST_TEMPLATE_ID, tlsEnabled ? TLS : NONE);
    }

    @Value("${kms.hostname:eric-sec-key-management}")
    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    @Value("${kms.port:8200}")
    public void setPort(final int port) {
        this.port = port;
    }

    /**
     * Refresh token used to interact with KMS
     * @param settings - request settings
     * */
    public void refreshToken(final RequestSettings settings) {
        log.info("Refreshing KMS token");
        final Token token = buildToken(settings);
        tokens.put(settings.role, token);
    }

    private Token getToken(final RequestSettings settings) {
        Token token = tokens.get(settings.role);
        if (token == null || token.isExpired()) {
            token = buildToken(settings);
            tokens.put(settings.role, token);
        }
        return token;
    }

    private Token buildToken(final RequestSettings settings) {
        log.info("Building new KMS token");
        final ObjectNode requestData = new ObjectMapper()
                .createObjectNode()
                .put("role", settings.role)
                .put("jwt", getJwt(settings));
        final RequestEntity<JsonNode> request = buildRequestEntity(settings, RequestType.AUTH, requestData);
        final ResponseEntity<JsonNode> response = getTemplate().exchange(request, JsonNode.class);
        successGuard(response);
        final JsonNode authData = Objects.requireNonNull(response.getBody()).get("auth");
        return new Token(authData.get("client_token").asText(), authData.get("lease_duration").asInt());
    }

    private <T> RequestEntity<T> buildRequestEntity(
            final RequestSettings settings, final RequestType type, final T body, final String... headers) {
        final RequestEntity.BodyBuilder request =
                RequestEntity.post(getRequestUri(settings, type)).contentType(MediaType.APPLICATION_JSON);
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        return request.body(body);
    }

    private RestTemplate getTemplate() {
        return templateCache.get();
    }

    private String getJwt(final RequestSettings settings) {
        try {
            return Files.readString(settings.serviceAccountTokenMount);
        } catch (IOException e) {
            throw new KMSRequestFailedException("Failed to read jwt from " + settings.serviceAccountTokenMount, e);
        }
    }

    private URI getRequestUri(final RequestSettings settings, final RequestType type) {
        try {
            return new URIBuilder()
                    .setScheme(tlsEnabled ? "https" : "http")
                    .setHost(hostname)
                    .setPort(port)
                    .setPathSegments(type.getPathSegments(settings))
                    .build();
        } catch (URISyntaxException e) {
            throw new KMSRequestFailedException("Failed to build URI for request", e);
        }
    }

    private <T> void successGuard(final ResponseEntity<T> response) {
        if (response.getStatusCode().is4xxClientError() ||
            response.getStatusCode().is5xxServerError() ||
            !response.hasBody()) {
            throw new KMSRequestFailedException("KMS request failed, got response " + response);
        }
    }

    /**
     * Class to contain all the relevant settings for a KMS request
     * serviceAccountTokenMount is the location the service account jwt is mounted
     * role is the role being used for the request
     * keyName is the name of the key used to encrypt/decrypt
     * */
    public static class RequestSettings {
        public final Path serviceAccountTokenMount;
        public final String role;
        public final String keyName;

        /**
         * Constructor
         * @param serviceAccountTokenMount - location where SA jwt is mounted
         * @param role - role being used for request
         * @param keyName - name of key being used for request
         * */
        public RequestSettings(final Path serviceAccountTokenMount, final String role, final String keyName) {
            this.serviceAccountTokenMount = serviceAccountTokenMount;
            this.role = role;
            this.keyName = keyName;
        }
    }

    private static class Token {
        public final String clientToken;
        public final OffsetDateTime created;
        public final int leaseDurationSeconds;

        private Token(final String clientToken, final int leaseDurationSeconds) {
            this.clientToken = clientToken;
            this.leaseDurationSeconds = leaseDurationSeconds;
            created = OffsetDateTime.now();
        }

        public boolean isExpired() {
            final boolean isExpired = ChronoUnit.SECONDS.between(created, OffsetDateTime.now()) > (leaseDurationSeconds - 60);
            if (isExpired) {
                log.info("KMS Token has expired");
            }
            return isExpired;
        }
    }

    private enum RequestType {
        AUTH(s -> new String[]{"v1", "auth", "kubernetes", "login"}),
        ENCRYPT(s -> new String[]{"v1", "transit", "encrypt", s.keyName}),
        DECRYPT(s -> new String[]{"v1", "transit", "decrypt", s.keyName});

        private final Function<RequestSettings, String[]> genSegments;

        RequestType(final Function<RequestSettings, String[]> genSegments) {
            this.genSegments = genSegments;
        }

        public String[] getPathSegments(final RequestSettings settings) {
            return genSegments.apply(settings);
        }
    }
}
