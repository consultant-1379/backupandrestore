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
package com.ericsson.adp.mgmt.backupandrestore.action.payload;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Payload for actions that require a backup.
 */
public class BackupNamePayload implements Payload {

    private String backupName;
    @JsonIgnore
    private final Optional<OffsetDateTime> creationTime;

    /**
     * Empty constructor, to be used by Jackson.
     */
    public BackupNamePayload() {
        creationTime = Optional.empty();
    }

    /**
     * Create payload with backupName and creationTime
     *
     * @param backupName
     *            name of the backup to be used in action.
     * @param creationTime
     *            date-time to set for created Backup object
     */
    public BackupNamePayload(final String backupName, final Optional<OffsetDateTime> creationTime) {
        this.backupName = backupName;
        this.creationTime = creationTime;
    }

    public String getBackupName() {
        return backupName;
    }

    public void setBackupName(final String backupName) {
        this.backupName = backupName;
    }

    /**
     * Returns creationTime wrapped in Option if present or else empty optional
     *
     * @return Optional<LocalDateTime>
     */
    public Optional<OffsetDateTime> getCreationTime() {
        return creationTime;
    }

    @Override
    public String toString() {
        return "BackupNamePayload [backupName=" + backupName + ", creationTime=" + creationTime + "]";
    }

}
