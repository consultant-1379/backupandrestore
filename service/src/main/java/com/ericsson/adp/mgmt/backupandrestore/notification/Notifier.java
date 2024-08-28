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

/**
 * Provides the methods to send an actions status to a message bus
 */
public interface Notifier {

    /**
     * @param notification send notification to a message bus
     */
    void sendNotification(Notification notification);

    /**
     * Notifies that an action has started.
     *
     * @param action that has started
     */
    void notifyActionStarted(Action action);

    /**
     * Notifies that an action has completed.
     *
     * @param action that has completed.
     */
    void notifyActionCompleted(Action action);

    /**
     * Notifies that an action has failed.
     *
     * @param action that has failed.
     */
    void notifyActionFailed(Action action);

}
