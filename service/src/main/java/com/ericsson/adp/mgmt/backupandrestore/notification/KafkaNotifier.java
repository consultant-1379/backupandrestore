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
package com.ericsson.adp.mgmt.backupandrestore.notification;

import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.KAFKA;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;

/**
 * Sends notifications to Kafka topic.
 * we need a new KafkaNotifier each time to use refreshed SSL certs
 */
public class KafkaNotifier implements Notifier {

    private static final Logger log = LogManager.getLogger(KafkaNotifier.class);
    private static final String NOTIFICATION_VERSION = "1.0.0";
    private String topic;
    private KafkaTemplate<String, Notification> kafkaTemplate;
    private KeyStoreService keyStoreService;
    private Properties props;
    private int partitions;
    private short replicationFactor;

    @Override
    @Retryable( value = NotificationFailedException.class,
            maxAttemptsExpression = "${kafka.retry.maxAttempts:10}", backoff = @Backoff(delayExpression = "${kafka.retry.maxDelay:3000}"))
    public void notifyActionStarted(final Action action) {
        log.debug("Sending notification for 'STARTED' action.");
        final Notification notification = new Notification(action, NotificationStatus.STARTED, NOTIFICATION_VERSION);
        sendNotification(notification);
    }

    @Override
    @Retryable( value = NotificationFailedException.class,
            maxAttemptsExpression = "${kafka.retry.maxAttempts:10}", backoff = @Backoff(delayExpression = "${kafka.retry.maxDelay:3000}"))
    public void notifyActionCompleted(final Action action) {
        log.debug("Sending notification for 'COMPLETED' action.");
        final Notification notification = new Notification(action, NotificationStatus.COMPLETED, NOTIFICATION_VERSION);
        sendNotification(notification);
    }

    @Override
    @Retryable( value = NotificationFailedException.class,
            maxAttemptsExpression = "${kafka.retry.maxAttempts:10}", backoff = @Backoff(delayExpression = "${kafka.retry.maxDelay:3000}"))
    public void notifyActionFailed(final Action action) {
        log.debug("Sending notification for 'FAILED' action.");
        final Notification notification = new Notification(action, NotificationStatus.FAILED, NOTIFICATION_VERSION);
        sendNotification(notification);
    }

    /**
     * Recovery action when @Retryable attempts fail when NotificationFailedException is thrown
     * @throws NotificationFailedException notificationFailedException
     * @param exception NotificationFailedException
     */
    @Recover
    public void connectionException(final NotificationFailedException exception) {
        log.error("Failed retry attempts to send notification to Message Bus KF.");
        throw exception;
    }

    /**
     * Notifies status of an action. Retries in case of exception.
     * Throws NotificationFailedException.
     * @param notification status of action
     */
    public void sendNotification(final Notification notification) {
        try {
            final CompletableFuture<SendResult<String, Notification>> result = kafkaTemplate.send(topic, notification);
            result.get();
            log.info("Sent notification: {}", notification);
        } catch (final InterruptedException exception) {
            log.warn("send notification interrupted {}", notification.getActionId(), exception);
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            log.warn("Failed to send notification: {}. Checking Topic and refreshing SSL context.", notification, e);
            createTopicIfNotExists(notification);
            keyStoreService.regenerateKeyStoreForAlias(KAFKA);
            throw new NotificationFailedException(notification, e);
        } finally {
            kafkaTemplate.getProducerFactory().reset();
        }
    }

    /**
     * Creates Topic in Kafka
     * @param notification status of action
     */
    public void createTopicIfNotExists(final Notification notification) {
        try (Admin admin = Admin.create(props);) {
            final Collection<TopicListing> topicListings = admin.listTopics().listings().get();
            final Optional<String> existingTopic = topicListings.stream().map(TopicListing::name)
                    .filter(name -> name.equals(topic))
                    .findFirst();
            if (existingTopic.isPresent()) {
                log.info("Topic found: {}", existingTopic.get());
            } else {
                log.info("Creating Topic <{}>. Partitions: {}, ReplicationFactor: {}", topic, partitions, replicationFactor);
                final NewTopic newTopic = new NewTopic(topic, partitions, replicationFactor);
                final CreateTopicsResult result = admin.createTopics(Collections.singleton(newTopic));
                final KafkaFuture<Void> future = result.values().get(topic);
                future.get();
                log.info("Topic <{}> has been successfully created.", topic);
            }
        } catch (final InterruptedException exception) {
            log.warn("Creating topic interrupted for {}", notification.getActionId(), exception);
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            log.info("Failed to create topic <{}> in kafka: {}", topic, exception);
        }
    }

    public void setTopic(final String topic) {
        this.topic = topic;
    }

    public void setKafkaTemplate(final KafkaTemplate<String, Notification> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * @param keyStoreService the keyStoreService to set
     */
    @Autowired
    public void setKeyStoreService(final KeyStoreService keyStoreService) {
        this.keyStoreService = keyStoreService;
    }

    public void setPartitions(final int partitions) {
        this.partitions = partitions;
    }

    public void setReplicationFactor(final short replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public void setProps(final Properties properties) {
        this.props = properties;
    }

}
