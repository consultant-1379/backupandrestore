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
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * Provides a way of sending notifications to multiple message bus services.
 */
public class MultipleNotifierService implements NotificationService {

    private List<Notifier> notifiers;

    @Override
    public void notifyAllActionStarted(final Action action) {
        for (final Notifier notifier : notifiers) {
            notifier.notifyActionStarted(action);
        }
    }

    @Override
    public void notifyAllActionCompleted(final Action action) {
        for (final Notifier notifier : notifiers) {
            notifier.notifyActionCompleted(action);
        }
    }

    @Override
    public void notifyAllActionFailed(final Action action) {
        for (final Notifier notifier : notifiers) {
            notifier.notifyActionFailed(action);
        }
    }

    /**
     * Sets Notifiers.
     * @param notifiers setting notifiers
     */
    @Autowired
    public void setNotifiers(final List<Notifier> notifiers) {
        this.notifiers = notifiers;
    }
}