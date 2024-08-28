/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.notification;

import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.REDIS;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;

import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;

/**
 * A class used for sending a message to a specified
 * stream in the Redis server
 */
public class RedisClient {
    private static final Logger log = LogManager.getLogger(RedisClient.class);
    private final String hostName;
    private final String port;
    private final String tlsPort;
    private final boolean isSSLEnabled;
    private final KeyStoreService keyStoreService;
    private final boolean isACLEnabled;
    private final String username;
    private final String password;
    private RedissonClient redissonClient;


    /**
     * The constructor of the Redis client
     * @param hostName            the host name of the redis server
     * @param port                the port of the redis server
     * @param tlsPort             the tls port of the redis server
     * @param isSSLEnabled        the flag that indicates if TLS is enabled
     * @param keyStoreService     the key store service
     * @param isACLEnabled        the flag that indicats if the ACL feature is enabled
     * @param username            the username of the user in the ACL
     * @param password            the password of the above user
     */
    public RedisClient(final String hostName,
                       final String port,
                       final String tlsPort,
                       final boolean isSSLEnabled,
                       final KeyStoreService keyStoreService,
                       final boolean isACLEnabled,
                       final String username,
                       final String password) {
        this.hostName = hostName;
        this.port = port;
        this.tlsPort = tlsPort;
        this.isSSLEnabled = isSSLEnabled;
        this.keyStoreService = keyStoreService;
        this.isACLEnabled = isACLEnabled;
        this.username = username;
        this.password = password;
    }

    /**
     * Set the redisson client for redis notifier
     */
    public final void setRedissonClient() {
        if (isSSLEnabled) {
            keyStoreService.regenerateKeyStoreForAlias(REDIS);
        }
        redissonClient = getRedissonClient();
    }

    /**
     * Get the redis client
     *
     * @return a redis client
     */
    public final RedissonClient getRedissonClient() {
        final Config config = new Config();
        final ClusterServersConfig serversConfig = config
                .setCodec(new StringCodec())
                .useClusterServers();
        try {
            if (isSSLEnabled) {
                log.info("SSL is enabled");
                final KeyStoreConfiguration keyStoreConfiguration = keyStoreService.getKeyStoreConfig(REDIS);
                serversConfig
                        .addNodeAddress(String.format("rediss://%s:%s", hostName, tlsPort));
                serversConfig
                        .setSslKeystore(keyStoreConfiguration.getKeyStoreFile().toUri().toURL())
                        .setSslKeystorePassword(keyStoreConfiguration.getKeyStorePassword())
                        .setSslTruststore(keyStoreConfiguration.getKeyStoreFile().toUri().toURL())
                        .setSslTruststorePassword(keyStoreConfiguration.getKeyStorePassword());
            } else {
                serversConfig
                        .addNodeAddress(String.format("redis://%s:%s", hostName, port));
            }
            if (isACLEnabled) {
                log.info("ACL is enabled");
                serversConfig.setUsername(username)
                        .setPassword(password);
            }
            return Redisson.create(config);
        } catch (final Exception e) {
            log.error("The redis connection failed, try to reconnect again", e);
            throw new NotificationFailedException(e);
        }
    }

    /**
     * Sends a key-value pair message to a Redis stream
     * @param topic the name of the Redis stream
     * @param key the key of the message
     * @param value the value of the message
     */
    public void sendMessage(final String topic, final String key, final String value) {
        if (redissonClient == null) {
            setRedissonClient();
        }
        try {
            redissonClient.getStream(topic).add(StreamAddArgs.entry(key, value));
        } catch (Exception e) {
            log.warn("Failed to send notification: < {}: {} >. Refreshing SSL context.", key, value);
            setRedissonClient();
            throw new NotificationFailedException(value, e);
        }
    }
}
