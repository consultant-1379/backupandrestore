/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.backupandrestore.util;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.DEFAULT_KEEPALIVE_SECONDS;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.DEFAULT_MAX_CONNECTIONS_PER_ROUTE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.DEFAULT_MAX_TOTAL_CONNECTIONS;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.ericsson.adp.mgmt.backupandrestore.caching.Cached;
import com.ericsson.adp.mgmt.backupandrestore.ssl.CMMRestKeyStoreConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;

/**
 * Factory for {@link RestTemplate}, optionally secured using the {@link CMMRestKeyStoreConfiguration}
 */
@Configuration
@ComponentScan("com.ericsson.adp.mgmt.backupandrestore.ssl")
public class RestTemplateFactory {
    // Public as used in all places BRO acts as a client - CMM, KMS (both through this rest template factory) and OSMN
    // at time of writing (07/09/21)
    public static final String[] TLS_VERSIONS = {"TLSv1.2", "TLSv1.3"};

    private static final IntPredicate validateStatusCode = (statusCode) ->
        statusCode == SC_SERVICE_UNAVAILABLE || statusCode == SC_INTERNAL_SERVER_ERROR;

    private static final int DEFAULT_MAX_RETRIES = 20;
    private static final long DEFAULT_RETRY_CONNECTION_ONFAIL_SECONDS = 30;

    private static final Logger log = LogManager.getLogger(RestTemplateFactory.class);
    private volatile KeyStoreService keyStoreService;

    /**
     * General rest template factory
     * @param forId - the id of the caller. Should be unique to a class of callers who all rely on the same
     * @param tlsSetting - The security setting for the rest template to be constructed
     * @return - A @{@link RestTemplate}
     * */
    public Cached<RestTemplate> getRestTemplate(final String forId, final Security tlsSetting) {
        if (tlsSetting == Security.NONE ) {
            return buildPlaintext(forId);
        } else {
            return buildSecured(forId, tlsSetting);
        }
    }

    private Cached<RestTemplate> buildPlaintext(final String forId) {
        return build(forId, this::getSimpleClientHttpClient);
    }

    private Cached<RestTemplate> build(final String forId, final Supplier<HttpClient> httpClientSupplier) {
        return new Cached<>(() -> {
            final RestTemplate template = new RestTemplate();
            log.debug("Building new rest template cache for {}", forId);
            final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClientSupplier.get());
            template.setRequestFactory(requestFactory);
            return template;
        });
    }

    private Cached<RestTemplate> buildSecured(final String forId, final Security tlsSetting) {
        final CMMRestKeyStoreConfiguration keyStoreConfiguration =
                (CMMRestKeyStoreConfiguration) keyStoreService.getKeyStoreConfig(KeyStoreAlias.CMM_REST);
        final Cached<RestTemplate> built = build(forId, () -> this.getHttpClient(tlsSetting, keyStoreConfiguration.getAlias()));
        keyStoreConfiguration.addListener(built);
        return built;
    }

    private HttpClient getHttpClient(final Security tlsSetting, final KeyStoreAlias alias) {
        return getHttpClient(tlsSetting, alias, DEFAULT_RETRY_CONNECTION_ONFAIL_SECONDS, validateStatusCode);
    }

    /**
     * Creates a httpClient object with mtls support
     * @param tlsSetting indicate the type of tls
     * @param alias keystore to be attached
     * @param retry_seconds how many seconds to wait for retry on service error
     * @param shouldRetry used to validate the response status code
     * @return httpClient an Http Client with tls support
     */
    @SuppressWarnings("PMD.CloseResource")
    protected HttpClient getHttpClient(final Security tlsSetting, final KeyStoreAlias alias,
                                       final long retry_seconds, final IntPredicate shouldRetry ) {
        try {
            final SSLConnectionSocketFactory csf =
                    new SSLConnectionSocketFactory(buildSSLContext(tlsSetting, alias), TLS_VERSIONS,
                            null, NoopHostnameVerifier.INSTANCE);

            final Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", csf)
                    .register("http", new PlainConnectionSocketFactory())
                    .build();

            final PoolingHttpClientConnectionManager connectionManager = getPoolingHttpClientConnectionManager(socketFactoryRegistry);
            return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy((response, context) -> TimeValue.ofSeconds(DEFAULT_KEEPALIVE_SECONDS))
                .setRetryStrategy(createServiceUnavailableRetryStrategy(retry_seconds, shouldRetry))
                .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | UnrecoverableKeyException e) {
            throw new IllegalStateException("Error loading key or trust material", e);
        } catch (CertificateExpiredException exception) {
            throw new IllegalStateException("Error certificate expired exception", exception);
        }
    }

    private HttpClient getSimpleClientHttpClient() {
        return getSimpleClientHttpClient(DEFAULT_RETRY_CONNECTION_ONFAIL_SECONDS, validateStatusCode);
    }

    /**
     * Creates a simple httpClient object without tls/mtls support
     * @param retry_seconds how many seconds to wait for retry on service error
     * @param shouldRetry used to validate the response status code
     * @return httpClient an Http Client
     */
    @SuppressWarnings("PMD.CloseResource")
    protected CloseableHttpClient getSimpleClientHttpClient(final long retry_seconds, final IntPredicate shouldRetry) {
        final PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(SSLContexts.createSystemDefault())
                        .setTlsVersions(TLS.V_1_2, TLS.V_1_3)
                        .build())
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.ofMinutes(1))
                        .build())
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                .setConnPoolPolicy(PoolReusePolicy.LIFO)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setSocketTimeout(Timeout.ofMinutes(1))
                        .setConnectTimeout(Timeout.ofMinutes(1))
                        .setTimeToLive(TimeValue.ofMinutes(10))
                        .build())
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy((response, context) -> TimeValue.ofSeconds(DEFAULT_KEEPALIVE_SECONDS))
                .setRetryStrategy(
                        createServiceUnavailableRetryStrategy(retry_seconds, shouldRetry))
                .build();
    }

    private PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager(final Registry<ConnectionSocketFactory> socketFactoryRegistry) {
        final PoolingHttpClientConnectionManager pooling = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        pooling.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
        pooling.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
        return pooling;
    }

    private HttpRequestRetryStrategy createServiceUnavailableRetryStrategy(final long retry_seconds, final IntPredicate shouldRetry) {
        return new HttpRequestRetryStrategy() {
            @Override
            public TimeValue getRetryInterval(final HttpResponse response, final int execCount, final HttpContext context) {
                return TimeValue.ofSeconds(retry_seconds);
            }

            @Override
            public boolean retryRequest(final HttpResponse response, final int executionCount, final HttpContext context) {
                final boolean retry = executionCount <= DEFAULT_MAX_RETRIES && shouldRetry.test(response.getCode());
                final String description = String.format("%s - %s of %s , status code %s - %s",
                        retry ? "Retry" : "Request",
                                executionCount, DEFAULT_MAX_RETRIES, response.getCode(),
                                response.getReasonPhrase());

                if (retry) {
                    log.warn(description);
                } else {
                    log.debug(description);
                }
                return retry;
            }

            @Override
            public boolean retryRequest(final HttpRequest request, final IOException exception, final int executionCount, final HttpContext context) {
                final String description = String.format("%s - %s of %s , IOException error %s",
                        executionCount <= DEFAULT_MAX_RETRIES ? "Retry" : "Request",
                                executionCount, DEFAULT_MAX_RETRIES, exception.getMessage());
                if (executionCount <= DEFAULT_MAX_RETRIES) {
                    log.warn(description);
                    return true;
                }
                log.debug(description);
                return false;
            }

        };
    }

    private SSLContext buildSSLContext(final Security tlsSetting, final KeyStoreAlias alias)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            CertificateExpiredException, KeyManagementException {
        final KeyStore store = keyStoreService.getKeyStore(alias);
        final KeyStoreConfiguration config = keyStoreService.getKeyStoreConfig(alias);
        final SSLContextBuilder builder = SSLContextBuilder.create();
        final TrustStrategy validatingTrustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
                for (final X509Certificate cert : chain) {
                    cert.checkValidity(new Date()); // This throws CertificateExpiredException or CertificateNotYetValidException if invalid
                }
                return true;
            }
        };
        builder.loadTrustMaterial(store, validatingTrustStrategy);
        if (tlsSetting == Security.MTLS) {
            builder.loadKeyMaterial(store, config.getKeyStorePassword().toCharArray());
        }
        return builder.build();
    }

    /**
     * @param keyStoreService the keyStoreService to set
     */
    @Autowired
    public void setKeyStoreService(final KeyStoreService keyStoreService) {
        this.keyStoreService = keyStoreService;
    }

    /**
     * Enum defining security configuration for rest template to be constructed
     * */
    public enum Security {
        NONE,
        TLS,
        MTLS
    }
}
