/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.notification;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

/**
 * Sends notifications to the Message Bus Redis
 */
public class RedisNotifier implements Notifier {

    public static final String JSONSTRINGFORMAT = "ASAJSONSTRING";
    private static final String REDIS_KEY = "notification";
    private static final Logger log = LogManager.getLogger(RedisNotifier.class);

    private final String topic;
    private final String notificationFormat;
    private final RedisClient client;

    /**
     * Create an instance of a Redis notifier
     * @param client             the client used for sending message to the Redis server
     * @param topic              the stream name of Redis
     * @param notificationFormat the Redis notification format
     */
    public RedisNotifier(final RedisClient client,
                         final String topic,
                         final String notificationFormat) {
        this.client = client;
        this.topic = topic;
        this.notificationFormat = notificationFormat;
    }

    /**
     * Notifies that an action has started.
     *
     * @param action that has started
     */
    @Retryable( value = NotificationFailedException.class,
            maxAttemptsExpression = "${keyValueDatabaseRd.retry.maxAttempts:10}",
            backoff = @Backoff(delayExpression = "${keyValueDatabaseRd.retry.maxDelay:3000}"))
    public void notifyActionStarted(final Action action) {
        final Notification notification = new Notification(action, NotificationStatus.STARTED, Notification.NOTIFICATION_VERSION);
        sendNotification(notification);
    }

    /**
     * Notifies that an action has completed.
     *
     * @param action that has completed.
     */
    @Retryable( value = NotificationFailedException.class,
            maxAttemptsExpression = "${keyValueDatabaseRd.retry.maxAttempts:10}",
            backoff = @Backoff(delayExpression = "${keyValueDatabaseRd.retry.maxDelay:3000}"))
    public void notifyActionCompleted(final Action action) {
        final Notification notification = new Notification(action, NotificationStatus.COMPLETED, Notification.NOTIFICATION_VERSION);
        sendNotification(notification);
    }

    /**
     * Notifies that an action has failed.
     *
     * @param action that has failed.
     */
    @Retryable( value = NotificationFailedException.class,
            maxAttemptsExpression = "${keyValueDatabaseRd.retry.maxAttempts:10}",
            backoff = @Backoff(delayExpression = "${keyValueDatabaseRd.retry.maxDelay:3000}"))
    public void notifyActionFailed(final Action action) {
        final Notification notification = new Notification(action, NotificationStatus.FAILED, Notification.NOTIFICATION_VERSION);
        sendNotification(notification);
    }

    /**
     * Recovery action when @Retryable attempts fail when NotificationFailedException is thrown
     * @throws NotificationFailedException notificationFailedException
     * @param exception NotificationFailedException
     */
    @Recover
    public void connectionException(final NotificationFailedException exception) {
        log.error("Failed retry all attempts to send notification.");
        throw exception;
    }

    @Override
    public void sendNotification(final Notification notification) {
        final String notificationValue = getNotificationValue(notification);
        client.sendMessage(topic, REDIS_KEY, notificationValue);
        log.info("Sent < {}: {} > to topic <{}>", REDIS_KEY, notificationValue, topic);
    }

    private String getNotificationValue(final Notification notification) {
        if (JSONSTRINGFORMAT.equals(notificationFormat)) {
            return JsonService.toJsonString(notification);
        } else {
            // use the Notification::toString if the format
            // is set to ASASTRING
            return notification.toString();
        }
    }

}