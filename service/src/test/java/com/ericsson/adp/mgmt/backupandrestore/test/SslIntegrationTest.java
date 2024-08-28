/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.test;

import static org.junit.Assume.assumeFalse;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.junit.BeforeClass;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.ericsson.adp.mgmt.backupandrestore.util.OSUtils;

@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@TestPropertySource(locations = "classpath:sslTest.properties")
@TestPropertySource("classpath:sslTest.properties")

public abstract class SslIntegrationTest {

    private String trustedCaPath;
    private String trustStorePassword;
    protected RestTemplate trusted;
    protected RestTemplate untrusted;
    @LocalServerPort
    private int serverTLSPort;

    protected final String BASE_URL = "https://localhost:7002";

    @PostConstruct
    public void initializeSslIntegrationTest() throws Exception {
        this.trusted = getTrustedRestTemplate();
        this.untrusted = new RestTemplate();
    }

    @BeforeClass
    public static void validateOSBeforeClass() {
         assumeFalse("Skipping these tests on Windows OS. Reference ADPPRG-39077.", OSUtils.isWindows());
    }

    private RestTemplate getTrustedRestTemplate() throws Exception {
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(new File(trustedCaPath), trustStorePassword.toCharArray()).build();
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext,
                NoopHostnameVerifier.INSTANCE);
        Registry<ConnectionSocketFactory> socketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory> create()
                        .register("https", socketFactory)
                        .register("http", new PlainConnectionSocketFactory())
                        .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(socketFactory)
                        .build()).build();
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(requestFactory);
    }

    @Value("${bro.trustedCa.path:}")
    public void setTrustStorePath(String trustedCaPath) {
        this.trustedCaPath = trustedCaPath;
    }

    @Value("${server.tls.port:7002}")
    public void setServerTLSPort(int serverTLSPort) {
        this.serverTLSPort = serverTLSPort;
    }

    @Value("${server.ssl.key-store-password:}")
    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }
}
