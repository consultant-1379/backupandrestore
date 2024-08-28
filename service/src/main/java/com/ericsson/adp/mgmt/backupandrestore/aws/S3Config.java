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
package com.ericsson.adp.mgmt.backupandrestore.aws;

import static com.amazonaws.services.s3.internal.SkipMd5CheckStrategy.DISABLE_GET_OBJECT_MD5_VALIDATION_PROPERTY;
import static com.amazonaws.services.s3.internal.SkipMd5CheckStrategy.DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY;
import static com.ericsson.adp.mgmt.backupandrestore.util.RestTemplateFactory.TLS_VERSIONS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.ericsson.adp.mgmt.backupandrestore.caching.Cached;
import com.ericsson.adp.mgmt.backupandrestore.exception.AWSException;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;

/**
 * Define a default AWS Client
 */
@Configuration
@SuppressWarnings("PMD.TooManyFields")
public class S3Config {
    private static final Logger log = LogManager.getLogger(S3Config.class);
    private static final String SIGNER_TYPE = "AWSS3V4SignerType";
    private static final int RETRIES_DELAY = 1000;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_UNEXPECTED_ERROR = 500;
    private static final int HTTP_SERVICE_UNAVAILABLE = 503;
    private static final Set<Integer> RETRYABLE_HTTP_CODES = Set.of(HTTP_UNEXPECTED_ERROR,
            HTTP_SERVICE_UNAVAILABLE, HTTP_TOO_MANY_REQUESTS);
    private boolean enabled;
    private String host;
    private String port;
    private String region;
    private String accessKeyName;
    private String secretKeyName;
    private Path credentialsDir = Path.of("");
    private AWSCredentials awsCredentials;
    private EndpointConfiguration endpointConfiguration;
    private String defaultBucketName;
    private KeyStoreService keyStoreService;
    private Cached<AmazonS3> cachedClient;
    private int retriesOperation;
    private int retriesStartUp;
    private int readConnectTimeout;
    private int connectionTimeout;

    /**
     * Gets a AWS client
     * @return an authenticated Amazon S3 client
     */
    public AmazonS3 getAWSClient() {
        if (!enabled) {
            throw new AWSException("S3Client is not enabled");
        }
        log.debug("Cache S3Client is null <{}>", cachedClient == null);
        if (cachedClient != null) {
            // if cached object is dropped as keystore regeneration,
            // it will be rebuilt by supplier in get() -> build() method
            if (validateS3Client(cachedClient.get())) {
                // if valid, return client
                return cachedClient.get();
            } else {
                // otherwise, invalidate the cached client (rebuild) and throw exception
                cachedClient.invalidate();
                throw new AWSException("Failed to connect with s3 service");
            }
        }
        // only create a cache container for storing clients when initialise
        final Cached<AmazonS3> newClient = new Cached<>(() -> {
            log.debug("Creating S3Client to communicate with S3 server at Region: <{}>", region);
            return AmazonS3ClientBuilder
                    .standard()
                    .withPathStyleAccessEnabled(true)
                    .withEndpointConfiguration(getEndPointConfiguration())
                    // The initial request will be waiting for more time than a regular call
                    .withClientConfiguration(getConfig(cachedClient == null))
                    .withCredentials(new AWSStaticCredentialsProvider(getCredentials()))
                    .build();
        });
        // If the OSMN keystore has been constructed, the OSMN keystore config is in use and it's refresh should invalidate this cache
        if (keyStoreService.getKeyStore(KeyStoreAlias.OSMN) != null) {
            keyStoreService.getKeyStoreConfig(KeyStoreAlias.OSMN).addListener(newClient);
        }
        // validate S3 client after create cache container and new client
        if (validateS3Client(newClient.get())) {
            cachedClient = newClient;
            return cachedClient.get();
        } else {
            throw new AWSException("Failed to connect with s3 service");
        }
    }

    private boolean validateS3Client (final AmazonS3 s3Client) {
        try {
            s3Client.listBuckets();
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    /**
     * Get configuration
     * @return the configuration of s3 client
     */
    private ClientConfiguration getConfig(final boolean isInitial) {
        final ClientConfiguration clientConfiguration = new ClientConfiguration()
                .withMaxConnections(10);
        setConfigRetryable(clientConfiguration, isInitial ? retriesStartUp : retriesOperation, isInitial);

        if (keyStoreService.getKeyStore(KeyStoreAlias.OSMN) != null) { // If the OSMN keystore exists, we should use it
            clientConfiguration.withProtocol(Protocol.HTTPS);
            try {
                clientConfiguration.getApacheHttpClientConfig().setSslSocketFactory(buildSocketFactory());
            } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyManagementException e) {
                throw new AWSException("Failed to construct SSL connection factory", e);
            }
        } else {
            clientConfiguration.withProtocol(Protocol.HTTP);
        }
        clientConfiguration.setSignerOverride(SIGNER_TYPE);
        return clientConfiguration;
    }

    private void setConfigRetryable(final  ClientConfiguration clientConfig, final int maxRetries, final boolean isInitial) {
        clientConfig.setConnectionTimeout(connectionTimeout);
        clientConfig.setSocketTimeout(readConnectTimeout);
        clientConfig.setRetryPolicy(getRetryPolicy(maxRetries, isInitial));
    }

    private RetryPolicy getRetryPolicy(final int maxRetries, final boolean isInitial) {

        final RetryPolicy.RetryCondition retryCondition = (request, clientException, retriesAttempted) -> {
            String errorMessage = clientException.getMessage();
            if (clientException instanceof AmazonServiceException) {
                final int code = ((AmazonServiceException) clientException).getStatusCode();
                final boolean shouldNotRetry = isInitial ? (code == HTTP_NOT_FOUND) : (!RETRYABLE_HTTP_CODES.contains(code));
                if (shouldNotRetry) {
                    return false;
                }
                errorMessage = ((AmazonServiceException) clientException).getErrorMessage();
            }
            if (errorMessage.contains("Your previous request to create the named bucket succeeded and you already own it")) {
                final String existingBuckets = getAWSClient().listBuckets().stream()
                    .map(bucket -> bucket.getName() + " created in " + bucket.getCreationDate() + " by " + bucket.getOwner())
                    .collect(Collectors.joining("\n"));
                log.error("Attempt to create a bucket failed. Found the following buckets {}", existingBuckets);
            }
            log.warn("<{} Connection> {} retry attempts to connect to S3: {}", isInitial ? "StartUp" : "Operation", retriesAttempted, errorMessage);
            return retriesAttempted < maxRetries;
        };
        return new RetryPolicy(retryCondition, (request, clientException, retriesAttempted) -> retriesAttempted * RETRIES_DELAY, maxRetries, true);
    }

    private SSLConnectionSocketFactory buildSocketFactory()
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        final SSLContextBuilder builder = new SSLContextBuilder();
        final KeyStore store = keyStoreService.getKeyStore(KeyStoreAlias.OSMN);
        final KeyStoreConfiguration configuration = keyStoreService.getKeyStoreConfig(KeyStoreAlias.OSMN);
        builder.loadTrustMaterial(store, null);
        if (configuration.getCert() != null) {
            builder.loadKeyMaterial(store, configuration.getKeyStorePassword().toCharArray());
        }
        return new SSLConnectionSocketFactory(builder.build(), TLS_VERSIONS,
                null, NoopHostnameVerifier.INSTANCE);
    }

    /**
     * Get Credentials
     * @return AWSCredentials
     */
    private AWSCredentials getCredentials() {
        return Objects.requireNonNullElseGet(awsCredentials, ()-> {
            String accessKey = accessKeyName;
            String secretKey = secretKeyName;
            try {
                accessKey = Files.readString(credentialsDir.resolve(accessKeyName));
                log.info("Read access key from " + credentialsDir.resolve(accessKeyName) + " successfully");
            } catch (Exception ignored) {
                // Note - We don't log the exception here as it may include the "path" that BRO just tried to read,
                // which might in fact be the access key itself - trying to avoid logging sensitive information
                log.warn("Failed to read access key from credentials directory, treating access key name as key contents");
            }

            try {
                secretKey = Files.readString(credentialsDir.resolve(secretKeyName));
                log.info("Read secret key from " + credentialsDir.resolve(secretKeyName) + " successfully");
            } catch (Exception ignored) {
                // Note - not logging the exception as above
                log.warn("Failed to read secret key from credentials directory, treating secret key name as key contents");
            }
            // NOTE: this caches the S3 credentials, under the assumption they do not change. I'm ok with this for now,
            // but it's possible in the future we'll want to no do this
            this.awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
            return this.awsCredentials;
        });
    }

    /**
     * Set credentials for config class
     * @param awsCredentials the aws credentials
     */
    public void setCredentials(final AWSCredentials awsCredentials) {
        if (awsCredentials == null) {
            throw new AWSException("Can't set a credential which is null");
        }
        this.awsCredentials = awsCredentials;
    }

    /**
     * set the endpointConfiguration
     * @param endpointConfiguration the endpointConfiguration
     */
    public void setEndpointConfiguration(final EndpointConfiguration endpointConfiguration) {
        if (endpointConfiguration == null) {
            throw new AWSException("Can't set an endpointConfiguration which is null");
        }
        this.endpointConfiguration = endpointConfiguration;
        final String[] list = endpointConfiguration.getServiceEndpoint().split(":");
        this.host = list[list.length - 2];
        this.port = list[list.length - 1];
        this.region = endpointConfiguration.getSigningRegion();
    }

    /**
     * Get Endpoint Configuration
     * @return EndpointConfiguration
     */
    private EndpointConfiguration getEndPointConfiguration() {
        return Objects.requireNonNullElseGet(endpointConfiguration, ()-> new EndpointConfiguration(getEndPoint(), region));
    }

    /**
     * Enable or disable the service
     * @param enabled boolean parameter to enable the service
     */
    @Value("${osmn.enabled:false}")
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Enable or disable md5 checksum on GET and PUT operations over OSMN
     * @param skipchecksum true or false to disable or enable checksum
     */
    @Value("${osmn.skipMD5CheckSum:true}")
    public void setSkipMd5Checksum (final boolean skipchecksum) {
        if (skipchecksum) {
            System.setProperty(DISABLE_GET_OBJECT_MD5_VALIDATION_PROPERTY, String.valueOf(skipchecksum));
            System.setProperty(DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY, String.valueOf(skipchecksum));
        } else {
            System.clearProperty(DISABLE_GET_OBJECT_MD5_VALIDATION_PROPERTY);
            System.clearProperty(DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY);
        }
    }

    /**
     * is the OSMN service enabled?
     * @return boolean default is false
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the default bucket name
     * @param defaultBucketName the bucket name
     */
    @Value("${osmn.bucketName:}")
    public void setDefaultBucketName(final String defaultBucketName) {
        this.defaultBucketName = defaultBucketName;
    }

    /**
     * Get the default bucket name
     * @return the default bucket name
     */
    public String getDefaultBucketName() {
        return defaultBucketName;
    }

    /**
     * Set Region for client
     * @param region String indicating the region
     */
    @Value("${osmn.region:}")
    public void setRegion(final String region) {
        if (region.isBlank()) {
            this.region = Regions.DEFAULT_REGION.getName();
        } else {
            this.region = region;
        }
        System.setProperty("osmn.region", this.region);
    }

    /**
     * Get the region of the bucket
     * @return the region of the bucket
     */
    public String getRegion() {
        return region;
    }

    /**
     * Set the host of OSMN server
     * @param host host name
     */
    @Value("${osmn.host:}")
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * Get the host
     * @return String host name
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the port of the osmn server
     * @param port indicating the port
     */
    @Value("${osmn.port:}")
    public void setPort(final String port) {
        this.port = port;
    }

    /**
     * Get Port
     * @return port number
     */
    public String getPort() {
        return port;
    }

    /**
     * Invalidate Cached clients
     */
    public void invalidateCachedClient() {
        this.cachedClient.invalidate();
    }

    /**
     * Set secret Key value
     * @param secretKeyName String secret key
     */
    @Value("${osmn.credentials.secretKeyName:}")
    public void setSecretKeyName(final String secretKeyName) {
        this.secretKeyName = secretKeyName;
    }

    /**
     * Return secret key
     * @return String with secret key
     */
    public String getSecretKey() {
        return getCredentials().getAWSSecretKey();
    }

    /**
     * Set the access key
     * @param accessKeyName access key
     */
    @Value("${osmn.credentials.accessKeyName:}")
    public void setAccessKeyName(final String accessKeyName) {
        this.accessKeyName = accessKeyName;
    }

    /**
     * Return the access key
     * @return String access key
     */
    public String getAccessKey() {
        return getCredentials().getAWSAccessKeyId();
    }

    /**
     * Get the end point
     * @return A URL that identifies a host and port
     */
    public String getEndPoint() {
        return host + ":" + port;
    }

    @Autowired
    public void setKeyStoreService(final KeyStoreService keyStoreService) {
        this.keyStoreService = keyStoreService;
    }

    @Value("${osmn.credentials.path:}")
    public void setCredentialsDir(final String directory) {
        this.credentialsDir = Path.of(directory);
    }

    @Value("${osmn.retries.operation:10}")
    public void setRetriesOperation(final int retriesOperation) {
        this.retriesOperation = retriesOperation;
    }

    @Value("${osmn.retries.startup:30}")
    public void setRetriesStartUp(final int retriesStartUp) {
        this.retriesStartUp = retriesStartUp;
    }

    @Value("${osmn.connection.readTimeOut:10000}")
    public void setConnectionReadTimeOut(final int readConnectTimeout) {
        this.readConnectTimeout = readConnectTimeout;
    }

    @Value("${osmn.connection.timeout:1000}")
    public void setConnectionTimeout(final int connectTimeout) {
        this.connectionTimeout = connectTimeout;
    }
}
