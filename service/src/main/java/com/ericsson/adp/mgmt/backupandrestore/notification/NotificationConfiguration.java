/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2019
 * <p>
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.backupandrestore.notification;

import java.util.Properties;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.LoggingProducerListener;

import com.ericsson.adp.mgmt.backupandrestore.ssl.KafkaKeyStoreConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;

/**
 * Decides which notification service will be used.
 */
@Configuration
public class NotificationConfiguration {

    private static final Logger log = LogManager.getLogger(NotificationConfiguration.class);

    private KafkaConfiguration kafkaConfiguration;
    private boolean isNotificationEnabled;
    private boolean kafkaNotificationEnabled;
    private boolean redisNotificationEnabled;
    private String topic;
    private KafkaTemplate<String, Notification> kafkaTemplate;
    private boolean isSSLEnabled;
    private KeyStoreService keyStoreService;
    private String kafkaIdentificationAlgorithm;

    /**
     * Instantiates notification service based on configuration.
     * @return NotificationService
     */
    @Bean
    public NotificationService getNotificationService() {
        if (isNotificationEnabled && (kafkaNotificationEnabled || redisNotificationEnabled)) {
            log.info("Enabling Message Bus notifications");
            return new MultipleNotifierService();
        }
        log.info("Not enabling Message Bus notifications");
        return new NoNotifiersService();
    }

    /**
     * Instantiates KafkaNotifier based on configuration.
     * @return KafkaNotifier
     */
    @Bean
    @ConditionalOnExpression("${flag.enable.notification:false} && ${kafka.enabled:false}")
    public KafkaNotifier getKafkaNotifier() {
        final Properties properties = new Properties();
        final KafkaKeyStoreConfiguration kafkaKeyStoreConfiguration =
                (KafkaKeyStoreConfiguration) keyStoreService.getKeyStoreConfig(KeyStoreAlias.KAFKA);
        if (isSSLEnabled) {
            final String keystorePath = kafkaKeyStoreConfiguration.getKeyStoreFile().toAbsolutePath().toString();
            final String keystorePassword = kafkaKeyStoreConfiguration.getKeyStorePassword();
            final String keyStoreType = keyStoreService.getKeyStoreType();
            properties.put("security.protocol", "SSL");
            properties.put("bootstrap.servers", kafkaConfiguration.getHostname() + ":" +
                    kafkaConfiguration.getTlsPort());
            properties.setProperty("ssl.keystore.location", keystorePath);
            properties.setProperty("ssl.keystore.password", keystorePassword);
            properties.setProperty("ssl.keystore.type", keyStoreType);
            properties.setProperty("ssl.truststore.location", keystorePath);
            properties.setProperty("ssl.truststore.password", keystorePassword);
            properties.setProperty("ssl.truststore.type", keyStoreType);
            properties.setProperty("ssl.endpoint.identification.algorithm", kafkaIdentificationAlgorithm);
        } else {
            properties.put("bootstrap.servers", kafkaConfiguration.getHostname() + ":" +
                    kafkaConfiguration.getPort());
        }
        final KafkaNotifier notifier = new KafkaNotifier();
        notifier.setProps(properties);
        notifier.setTopic(topic);
        notifier.setKafkaTemplate(kafkaTemplate);
        notifier.setPartitions(kafkaConfiguration.getPartitions());
        notifier.setReplicationFactor(kafkaConfiguration.getReplicationFactor());
        return notifier;
    }

    /**
     * Instantiates RedisNotifier based on configuration.
     * @param hostName the Redis host name
     * @param port the Redis server's non-TLS port
     * @param tlsPort the Redis server's TLS port
     * @param isACLEnabled a flag that indicates if Redis ACL is enabled
     * @param username the ACL username
     * @param password the ACL password
     * @param notificationFormat the redis notification format
     * @return a RedisNotifier
     */
    @Bean
    @ConditionalOnExpression("${flag.enable.notification:false} && ${keyValueDatabaseRd.enabled:false}")
    public RedisNotifier getRedisNotifier(@Value("${keyValueDatabaseRd.hostname:eric-data-key-value-database-rd}")
                                          final String hostName,
                                          @Value("${keyValueDatabaseRd.port:6379}")
                                          final String port,
                                          @Value("${keyValueDatabaseRd.tlsPort:6380}")
                                          final String tlsPort,
                                          @Value("${kvdbrd.acl.enabled:true}")
                                          final boolean isACLEnabled,
                                          @Value("${kvdbrd.acl.username:}")
                                          final String username,
                                          @Value("${kvdbrd.acl.password:}")
                                          final String password,
                                          @Value("${keyValueDatabaseRd.notificationValueFormat:ASASTRING}")
                                          final String notificationFormat) {
        final RedisClient redisClient = new RedisClient(hostName,
                                                        port,
                                                        tlsPort,
                                                        isSSLEnabled,
                                                        keyStoreService,
                                                        isACLEnabled,
                                                        username,
                                                        password);
        return new RedisNotifier(redisClient, topic, notificationFormat);
    }

    @Value("${flag.enable.notification:false}")
    public void setIsNotificationEnabled(final boolean isNotificationEnabled) {
        this.isNotificationEnabled = isNotificationEnabled;
    }

    @Value("${kafka.enabled:false}")
    public void setKafkaNotificationEnabled(final boolean isKafkaEnabled) {
        this.kafkaNotificationEnabled = isKafkaEnabled;
    }

    @Value("${keyValueDatabaseRd.enabled:false}")
    public void setRedisNotificationEnabled(final boolean isRedisEnabled) {
        this.redisNotificationEnabled = isRedisEnabled;
    }

    @Value("${bro.notification.topic:bro-notification}")
    public void setTopic(final String topic) {
        this.topic = topic;
    }

    /**
     * Sets Kafka Template with custom ProducerListener
     * @param kafkaTemplate template
     */
    @Autowired
    public void setKafkaTemplate(final KafkaTemplate<String, Notification> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTemplate.setProducerListener(new LoggingProducerListener<String, Notification>() {

            @Override
            public void onError(final ProducerRecord<String, Notification> producerRecord,
                                final RecordMetadata recordMetadata, final Exception exception) {
                log.warn("Error thrown when sending notification message.", exception);
            }

        });
    }

    @Autowired
    public void setKafkaConfiguration(final KafkaConfiguration kafkaConfiguration) {
        this.kafkaConfiguration = kafkaConfiguration;
    }

    /**
     * If the ssl is enabled
     * @param SSLEnabled if the ssl is enabled.
     */
    @Value("${flag.global.security:true}")
    public void setSSLEnabled(final boolean SSLEnabled) {
        isSSLEnabled = SSLEnabled;
    }

    @Autowired
    public void setKeyStoreService(final KeyStoreService keyStoreService) {
        this.keyStoreService = keyStoreService;
    }

    @Value("${spring.kafka.properties.ssl.endpoint.identification.algorithm:}")
    public void setKafkaIdentificationAlgorithm(final String kafkaIdentificationAlgorithm) {
        this.kafkaIdentificationAlgorithm = kafkaIdentificationAlgorithm;
    }
}