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
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;

/**
 * Notification to be sent indicating the progress of an Action.
 */
public class Notification {

    public static final String NOTIFICATION_VERSION = "1.0.0";
    private String version;
    private ActionType action;
    private String actionId;
    private String backupManagerId;
    private NotificationStatus status;

    /**
     * Default constructor, to be used by Jackson.
     */
    public Notification() {}

    /**
     * Creates notification
     * @param action that triggered notification.
     * @param status status of action.
     * @param version the br-internal-notification version
     */
    public Notification(final Action action, final NotificationStatus status, final String version) {
        this.version = version;
        this.action = action.getName();
        this.actionId = action.getActionId();
        this.backupManagerId = action.getBackupManagerId();
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(final ActionType action) {
        this.action = action;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(final String actionId) {
        this.actionId = actionId;
    }

    public String getBackupManagerId() {
        return backupManagerId;
    }

    public void setBackupManagerId(final String backupManagerId) {
        this.backupManagerId = backupManagerId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(final NotificationStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format(
                "Notification [version=%s, action=%s, actionId=%s, backupManagerId=%s, status=%s]",
                version, action, actionId, backupManagerId, status);
    }

}
