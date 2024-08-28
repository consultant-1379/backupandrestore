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
package com.ericsson.adp.mgmt.backupandrestore.util;

import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.CMM_REST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.adp.mgmt.backupandrestore.exception.KeyStoreGenerationException;
import com.ericsson.adp.mgmt.backupandrestore.rest.health.HealthResponse;
import com.ericsson.adp.mgmt.backupandrestore.ssl.CertificateCA;
import com.ericsson.adp.mgmt.backupandrestore.ssl.CertificateKeyEntry;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;
import com.ericsson.adp.mgmt.backupandrestore.test.SslIntegrationTest;

public class RestTemplateFactoryTest extends SslIntegrationTest {
    protected final String BASE_URL_NONE = "http://localhost:7001";
    private static final String REST_TEMPLATE_ID = "RestTemplateConfigurationTest";
    private final String HEALTH_URL = BASE_URL + "/v1/health";
    private final String SIMPLE_HEALTH_URL = BASE_URL_NONE + "/v1/health";
    private final String INVALID_KEY_PATH = "src/test/resources/invalidPrivateKey.key";
    private final String INVALID_CERT_PATH = "src/test/resources/invalidCert.pem";
    private final String CA_PATH = "src/test/resources/ca.pem";
    private final String INVALID_KEYSTORE_PATH = "src/test/resource/invalidKeyStore.p12";
    private final String INVALID_KEYSTORE_PASSWORD = "irrelevant";
    private final int ONE_SECOND = 1;
    private final int DEFAULT_MAX_RETRIES = 20;

    @Autowired
    private KeyStoreService keyStoreService;
    /**
     * This class purposefully overwrites the CMM keystore configured in the keyStoreService,
     * in order to force an invalid cert failure from the restTemplate
     * */
    private class InvalidCMMKeyStoreConfiguration extends KeyStoreConfiguration {
        public InvalidCMMKeyStoreConfiguration() {
            keystorePassword = INVALID_KEYSTORE_PASSWORD;
            keyStoreFile = Path.of(INVALID_KEYSTORE_PATH);
            cert = new CertificateKeyEntry(getAlias().toString(), INVALID_KEY_PATH, INVALID_CERT_PATH);
            authorities = new HashSet<>();
            authorities.add(new CertificateCA(ApplicationConstantsUtils.SIP_TLS_ROOT_CERT_ALIAS, CA_PATH));
        }

        @Override
        public KeyStoreAlias getAlias() {
            return CMM_REST;
        }
    }

//    ADPPRG-171470 - Test failing as .key .crt in created in genCerts all created in one folder and cert select will be first .key found
//    needs to be cleaned up
//    @Test(expected = Test.None.class /* no exception expected */)
//    public void RestTemplateConfiguration_RestTemplate_retrieveValidTemplate(){
//        keyStoreService.generateKeyStores();
//        RestTemplateFactory restTemplateConfig = new RestTemplateFactory();
//        restTemplateConfig.setKeyStoreService(keyStoreService);
//        RestTemplate restTemplate = restTemplateConfig.getRestTemplate(REST_TEMPLATE_ID, RestTemplateFactory.Security.MTLS).get();
//
//        restTemplate.getForEntity(HEALTH_URL, HealthResponse.class);
//    }

//    ADPPRG-171470 - Test failing as .key .crt in created in genCerts all created in one folder and cert select will be first .key found
//    needs to be cleaned up
    @Test(expected = ResourceAccessException.class)
    public void RestTemplateConfiguration_RestMTLSTemplate_retry_totalRetries(){
        AtomicInteger numberCalls = new AtomicInteger();
        keyStoreService.generateKeyStores();
        RestTemplateFactory restTemplateConfig = new RestTemplateFactory();
        restTemplateConfig.setKeyStoreService(keyStoreService);
        final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(restTemplateConfig.getHttpClient(RestTemplateFactory.Security.MTLS, CMM_REST,
                ONE_SECOND, (statusCode) -> { numberCalls.incrementAndGet(); return statusCode == SC_OK;}));
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory (requestFactory);
        restTemplate.getForEntity(HEALTH_URL, HealthResponse.class);
        assertEquals(DEFAULT_MAX_RETRIES, numberCalls.get());
    }

    @Test
    public void RestTemplateConfiguration_RestSimpleTemplate_retry_totalRetries() throws IOException{
        final AtomicInteger numberCalls = new AtomicInteger();
        final RestTemplateFactory restTemplateConfig = new RestTemplateFactory();
        final RestTemplate restTemplate = new RestTemplate();
        CloseableHttpClient httpClient = restTemplateConfig.getSimpleClientHttpClient(
                ONE_SECOND, (statusCode) -> {
                    numberCalls.incrementAndGet();
                    return (statusCode == SC_OK) || (numberCalls.get() < DEFAULT_MAX_RETRIES);
                });

        final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        restTemplate.setRequestFactory (requestFactory);
        // will thrown the ResourceAccessException due the server.port is random
        restTemplate.getForEntity(SIMPLE_HEALTH_URL, HealthResponse.class);
        assertEquals(DEFAULT_MAX_RETRIES, numberCalls.get());
        httpClient.close();
    }

    @Test(expected = KeyStoreGenerationException.class)
    public void RestTemplateConfiguration_RestTemplate_retrieveInvalidTemplate_assertThrows() {
        new InvalidCMMKeyStoreConfiguration();//Overwrite the CMM config with the invalid one
        keyStoreService.generateKeyStores();
        RestTemplateFactory restTemplateConfig = new RestTemplateFactory();
        RestTemplate restTemplate = restTemplateConfig.getRestTemplate(REST_TEMPLATE_ID, RestTemplateFactory.Security.MTLS).get();
        restTemplate.getForEntity(HEALTH_URL, HealthResponse.class);
    }

    @Test(expected = Test.None.class)
    public void RestTemplateConfiguration_plaintTextConfiguration_getRestTemplate() {
        RestTemplateFactory configuration = new RestTemplateFactory();
        configuration.getRestTemplate(REST_TEMPLATE_ID, RestTemplateFactory.Security.NONE).get();
    }
}
