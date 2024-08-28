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
 * Doesn't send notifications.
 */
public class NoNotifiersService implements NotificationService {

    @Override
    public void notifyAllActionStarted(final Action action) {
        //Does nothing
    }

    @Override
    public void notifyAllActionCompleted(final Action action) {
        //Does nothing
    }

    @Override
    public void notifyAllActionFailed(final Action action) {
        ///Does nothing
    }

}
