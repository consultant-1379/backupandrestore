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

/**
 * Represents something went wrong while trying to send a notification.
 */
public class NotificationFailedException extends RuntimeException {

    private static final long serialVersionUID = 8695254477937534559L;

    /**
     * Creates exception.
     * @param notification to be sent.
     * @param cause root cause.
     */
    public NotificationFailedException(final Notification notification, final Throwable cause) {
        super("Failed to send notification " + notification, cause);
    }

    /**
     * Creates exception.
     * @param notification to be sent.
     * @param cause root cause.
     */
    public NotificationFailedException(final String notification, final Throwable cause) {
        super("Failed to send notification " + notification, cause);
    }

    /**
     * Creates exception.
     * @param cause root cause.
     */
    public NotificationFailedException(final Throwable cause) {
        super("Failed to send notification.", cause);
    }

}
