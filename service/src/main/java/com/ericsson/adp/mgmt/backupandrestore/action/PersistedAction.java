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
package com.ericsson.adp.mgmt.backupandrestore.action;

import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Represents Action to be persisted.
 */
public class PersistedAction extends ActionResponse implements Versioned<PersistedAction> {

    private Version<PersistedAction> version;

    private boolean isScheduledEvent;

    /**
     * Default constructor, to be used by Jackson.
     */
    public PersistedAction() {
    }

    /**
     * Creates PersistedAction based on Action.
     *
     * @param action
     *            to be persisted.
     */
    public PersistedAction(final Action action) {
        actionId = action.getActionId();
        name = action.getName();
        payload = action.getPayload();
        additionalInfo = action.getAdditionalInfo();
        startTime = DateTimeUtils.convertToString(action.getStartTime());
        lastUpdateTime = DateTimeUtils.convertToString(action.getLastUpdateTime());
        if (action.getCompletionTime() != null) {
            completionTime = DateTimeUtils.convertToString(action.getCompletionTime());
        }
        if (action.hasMessages()) {
            final StringBuilder resultInfo = new StringBuilder();
            action.getCopyOfMessages().forEach(resultInfo::append);
            setResultInfo(resultInfo.toString());
        }
        progressInfo = action.getProgressInfo();
        progressPercentage = action.getProgressPercentage();
        result = action.getResult();
        state = action.getState();
        version = action.getVersion();
        isScheduledEvent = action.isScheduledEvent();
    }

    @Override
    @JsonIgnore
    public Version<PersistedAction> getVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public void setVersion(final Version<PersistedAction> version) {
        this.version = version;
    }

    public boolean isScheduledEvent() {
        return isScheduledEvent;
    }

    public void setScheduledEvent(final boolean scheduledEvent) {
        isScheduledEvent = scheduledEvent;
    }
}
