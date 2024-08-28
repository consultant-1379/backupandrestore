/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.backup;

import java.util.Optional;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.TaskResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Full JSON response of a V4 backup.
 */
@JsonInclude(Include.NON_DEFAULT)
public class V4BackupResponse extends BackupResponse {

    protected TaskResponse task;

    /**
     * Default constructor, to be used by Jackson.
     */
    public V4BackupResponse() {}

    /**
     * Full JSON response of a V4 backup.
     * @param backup the backup resource
     */
    public V4BackupResponse(final Backup backup) {
        this(backup, Optional.empty());
    }

    /**
     * Full JSON response of a V4 backup.
     * @param backup the backup resource
     * @param action the backup action associated with the backup resource
     */
    public V4BackupResponse(final Backup backup, final Optional<Action> action) {
        super(backup);
        if (action.isPresent()) {
            task = new TaskResponse(action.get());
        }
    }

    @Override
    @JsonProperty("id")
    public String getBackupId() {
        return super.getBackupId();
    }

    @Override
    @JsonIgnore
    public String getUserLabel() {
        return super.getUserLabel();
    }

    public TaskResponse getTask() {
        return task;
    }
}
