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

import com.ericsson.adp.mgmt.backupandrestore.action.Action;

/**
 * Responsible for sending notifications.
 */
public interface NotificationService {

    /**
     * Notifies that an action has started.
     * @param action that has started
     */
    void notifyAllActionStarted(Action action);

    /**
     * Notifies that an action has completed.
     * @param action that has completed.
     */
    void notifyAllActionCompleted(Action action);

    /**
     * Notifies that an action has failed.
     * @param action that has failed.
     */
    void notifyAllActionFailed(Action action);

}
