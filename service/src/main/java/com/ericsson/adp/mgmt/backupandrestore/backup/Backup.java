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
package com.ericsson.adp.mgmt.backupandrestore.backup;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupResponse;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Stores Backup details.
 */
public class Backup extends BackupInformation implements Versioned<PersistedBackup> {

    private final OffsetDateTime creationTime;
    private final Consumer<Backup> persistFunction;
    private final AtomicBoolean backupLevelProgressReportCreated = new AtomicBoolean();
    private final AtomicReference<Optional<Action>> actionLock = new AtomicReference<>(Optional.empty());

    private Version<PersistedBackup> version;

    /**
     * Constructor for backup.
     *
     * @param name
     *            backup name
     * @param backupManagerId
     *            owner of backup
     * @param creationTime
     *            creation time of the backup
     * @param creationType of backup
     * @param persistFunction
     *            - how to persist a backup.
     */
    public Backup(final String name, final String backupManagerId, final Optional<OffsetDateTime> creationTime,
                     final BackupCreationType creationType,
                     final Consumer<Backup> persistFunction) {
        backupId = name;
        this.backupManagerId = backupManagerId;
        this.name = name;
        if (creationTime.isPresent()) {
            this.creationTime = creationTime.get();
        } else {
            this.creationTime = OffsetDateTime.now(ZoneId.systemDefault());
        }
        this.creationType = creationType;
        status = BackupStatus.INCOMPLETE;
        this.persistFunction = persistFunction;
    }

    /**
     * Constructor for backup, populating from persisted backup.
     *
     * @param backup
     *            - persisted backup
     * @param persistFunction
     *            how to persist a backup.
     */
    protected Backup(final PersistedBackup backup, final Consumer<Backup> persistFunction) {
        // if the backupId is not included, it takes the backup name instead
        backupId = backup.getBackupId().isBlank() ? backup.getName() : backup.getBackupId();
        backupManagerId = backup.getBackupManagerId();
        name = backup.getName();
        creationTime = DateTimeUtils.parseToOffsetDateTime(backup.getCreationTime());
        creationType = backup.getCreationType();
        userLabel = backup.getUserLabel();
        status = backup.getStatus();
        softwareVersions = backup.getSoftwareVersions();
        this.persistFunction = persistFunction;
        this.version = backup.getVersion();
    }

    /**
     * Persists backup.
     */
    public void persist() {
        persistFunction.accept(this);
    }

    /**
     * Adds Software Version to SoftwareVersionsList.
     *
     * @param softwareVersion
     *            to be added.
     */
    public void addSoftwareVersion(final SoftwareVersion softwareVersion) {
        softwareVersions.add(softwareVersion);
    }

    public OffsetDateTime getCreationTime() {
        return creationTime;
    }

    /**
     * Get json representation of Backup.
     *
     * @return Json Object.
     */
    public BackupResponse toJson() {
        final BackupResponse response = toSimplifiedJson();
        response.setCreationType(getCreationType());
        response.setUserLabel(getUserLabel());
        response.setSoftwareVersions(getSoftwareVersions());
        return response;
    }

    /**
     * Get json representation of minimal Backup information.
     *
     * @return json object.
     */
    public BackupResponse toSimplifiedJson() {
        final BackupResponse response = new BackupResponse();
        response.setBackupId(getBackupId());
        response.setName(getName());
        response.setCreationTime(DateTimeUtils.convertToString(getCreationTime()));
        response.setStatus(getStatus());
        return response;

    }

    /**
     * Compares the current backup with other backup to sort.
     * @param backup
     *            backup to be compared
     * @return -1 if status is incomplete or corrupt.
     *          1 if the status is complete.
     *         if the status are same, compares the backups creation time.
     *            1 if backup to compare was created early.
     *           -1 if backup to compare was created after the current backup.
     */
    public int compareByStatusCreationTime(final Backup backup) {
        if (getStatus().getPrecedence() > backup.getStatus().getPrecedence()) {
            return -1;
        } else if (getStatus().getPrecedence() < backup.getStatus().getPrecedence()) {
            return 1;
        } else { // If Status is the same, compare by date of creation
            if (backup.getCreationTime().isBefore(getCreationTime())) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    /**
     * Returns a flag value to indicate the backup level progress report has been created in CMM.
     * @return current value as represented by backupLevelProgressReportCreated
     */
    public boolean isBackupLevelProgressReportCreated() {
        return backupLevelProgressReportCreated.get();
    }

    /**
     * Sets a flag to indicate the backup level progress report has been created in CMM.
     */
    public void backupLevelProgressReportSetCreated() {
        backupLevelProgressReportCreated.compareAndSet(false, true);
    }

    /**
     * Set a flag to mark that the backup level progress report needs to be created in CMM.
     */
    public void backupLevelProgressReportResetCreated() {
        backupLevelProgressReportCreated.set(false);
    }

    /**
     * Updates the action locked on a backup if there is no other action
     * running on it.
     * The atomic CAS operation ensures no other thread can update
     * the ongoingAction when there is already an action running on
     * the backup. As such, this method effectively locks the backup
     * an action.
     * @param action the new action attempting to run on the backup
     * @return true if the new action is set as the ongoing action,
     * false if the update failed as there is an ongoing action already acting on the backup.
     */
    public boolean setActionLock(final Action action) {
        return actionLock.compareAndSet(Optional.empty(), Optional.of(action));
    }

    /**
     * Removes the action locked on the backup
     * Only the action running on the backup can remove itself from the backup.
     * In effect, this method unlocks the backup from an action.
     * @param action the action attempting to remove the ongoingAction.
     */
    public void removeActionLock(final Action action) {
        actionLock.get().ifPresent(currentAction -> {
            if (action.equals(currentAction)) {
                actionLock.set(Optional.empty());
            }
        });
    }

    /**
     * Get the ongoing action that is locking the backup
     * @return returns an Optional wrapping the action locking the backup.
     */
    public Optional<Action> getActionLock() {
        return actionLock.get();
    }

    @Override
    public String toString() {
        return String.format("%s - <%s>", getBackupId(), getStatus());
    }

    @Override
    @JsonIgnore
    public Version<PersistedBackup> getVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public void setVersion(final Version<PersistedBackup> version) {
        this.version = version;
    }
}
