/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/

package com.ericsson.adp.mgmt.backupandrestore.job;

import static com.ericsson.adp.mgmt.backupandrestore.action.ResultType.FAILURE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupRepository;
import com.ericsson.adp.mgmt.backupandrestore.exception.NotImplementedException;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.exception.TimedOutHousekeepingException;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.data.Metadata;

/**
 * Responsible for housekeeping job.
 */
@Service
public class HousekeepingJob extends JobWithStages<HousekeepingJob> {

    // Waits by default for 120 seconds before throws the timeout exception
    private static final String TIMEOUT_HOUSEKEEPING_DELETE = "120";
    private static final Logger log = LogManager.getLogger(HousekeepingJob.class);

    private int timeoutBackupDelete = Integer.parseInt(TIMEOUT_HOUSEKEEPING_DELETE);

    private BackupRepository backupRepository;
    private ReentrantLock accessControl;
    private boolean executeAsTask;

    /**
     * Initiate the Housekeeping job instance
     */
    public HousekeepingJob() {
        super();
        accessControl = new ReentrantLock();
    }

    @Override
    protected void triggerJob() {
        jobStage.trigger();
    }

    protected void setBackupRepository(final BackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }

    /**
     * Run the housekeeping job to delete the first backup based on their status/creation
     * @return List of backups choose to be deleted.
     * @throws InterruptedException Is there any thread interruption will cancel the action
     * @throws TimedOutHousekeepingException if can delete the backups
     */
    public List<String> executeHousekeeping() throws InterruptedException {
        int maxNumberBackups = getMaxNumberBackups();
        final List<String> backupsProcessed = new ArrayList<>();
        int failedBackups = 0;

        // Is the action was called from a another action, then
        // we need to decrement the maxNumberbackups to create the
        // space required to execute the original action
        final List<Backup> backupsOrdered = backupRepository.getBackupsForAutoDeletion(
                backupManager.getBackupManagerId(), getAction().isExecutedAsTask() ? --maxNumberBackups  : maxNumberBackups );
        log.debug("Backups to be removed {}", backupsOrdered);
        if (!backupsOrdered.isEmpty()) {
            for (int toRemove = 0; toRemove < backupsOrdered.size(); toRemove++) {
                if (isLockedAllowed()) {
                    final CreateActionRequest actionRequest = createDeleteActionRequest(backupsOrdered.get(toRemove));
                    accessControl.unlock();
                    final ResultType result = actionService.executeAndWait(backupManager, actionRequest);
                    if (result == FAILURE) {
                        failedBackups++;
                    }
                    backupsProcessed.add(String.format("%s - %s", backupsOrdered.get(toRemove).getBackupId(), result));
                } else {
                    accessControl.unlock();
                    throw new TimedOutHousekeepingException(timeoutBackupDelete, backupsOrdered.get(toRemove).toString());
                }
            }
            if (failedBackups > 0) {
                fail();
            }
        }
        log.debug("Backups processed {}", backupsOrdered);
        return backupsProcessed;
    }


    /**
     * Creates a delete action request to remove an initial backup
     *
     * @param backup
     *            Backup selected to be removed
     * @return an ActionRequest including the backup to delete
     */
    private CreateActionRequest createDeleteActionRequest(final Backup backup) {
        final CreateActionRequest actionRequest = new CreateActionRequest();
        actionRequest.setAction(ActionType.HOUSEKEEPING_DELETE_BACKUP);
        actionRequest.setPayload(new BackupNamePayload(backup.getName(), Optional.empty()));
        return actionRequest;
    }

    @Override
    protected void completeJob() {
        if (!jobStage.isStageSuccessful()) {
            log.warn("Housekeeping not executed <{}>", backupManager.getBackupManagerId());
        }
    }

    @Override
    protected void fail() {
        super.fail();
        getAction().setState(ActionStateType.FINISHED);
        getAction().setResult(FAILURE);
        log.error("Housekeeping <{}> failed", backupManager.getBackupManagerId());
    }

    /**
     * Validates if the maximum number of backups was reach  or not.
     * @return true if the backup maximum number of backups allowed was reached
     */
    public boolean isMaxNumberBackups() {
        return backupRepository.isMaxBackupLimitReached(backupManager.getBackupManagerId(),
                isExecuteAsTask() ?  getMaxNumberBackups() - 1 : getMaxNumberBackups());
    }

    public int getMaxNumberBackups() {
        return getAction().getMaximumManualBackupsStored();
    }

    public String getAutoDelete() {
        return getAction().getAutoDelete();
    }
    /**
     * If the Housekeeping was call from a backup
     * it needs to continue the call to the original
     * Backup Action
     */
    public void continueCreateBackup() {
        throw new NotImplementedException();
    }

    public boolean isStageSuccessful() {
        return true;
    }
    /**
     * Where data regarding fragment is stored.
     *
     * @param metadata contains fragment information.
     * @return where fragment data is stored.
     */
    @Override
    public FragmentFolder getFragmentFolder(final Metadata metadata) {
        return null;
    }

    @Value("${housekeeping.timeout.backup.delete:"  + TIMEOUT_HOUSEKEEPING_DELETE + "}")
    public void setTimeoutBackupDelete(final int timeoutBackupDelete) {
        this.timeoutBackupDelete = timeoutBackupDelete;
    }

    @Override
    protected void monitor() {
        log.info("JobStage<{}> Waiting for backups to be deleted: ", jobStage.getClass().getSimpleName());
    }

    private  ReentrantLock getAccessControl() {
        return accessControl;
    }

    protected void setAccessControl(final ReentrantLock accessControl) {
        this.accessControl = accessControl;
    }

    private boolean isLockedAllowed() throws InterruptedException {
        return getAccessControl().tryLock(timeoutBackupDelete, TimeUnit.SECONDS);
    }

    public boolean isExecuteAsTask() {
        return executeAsTask;
    }

    public void setExecuteAsTask(final boolean executeAsTask) {
        this.executeAsTask = executeAsTask;
    }

    /**
     * Update the backup manager settings for housekeeping
     * AutoDelete to execute housekeeping on create backup or import
     * Maximum number of backups to maintain
     */
    public void updateBackupManagerHousekeeping() {
        log.info("BackupManager's housekeeping <{}> current value: <auto-delete:{} max-stored-manual-backups:{}> " +
                "new value: <auto-delete:{} max-stored-manual-backups:{}>",
                backupManager.getBackupManagerId(),
                backupManager.getHousekeeping().getAutoDelete(),
                backupManager.getHousekeeping().getMaxNumberBackups(),
                getAutoDelete(),
                getMaxNumberBackups());
        if ( (!backupManager.getHousekeeping().getAutoDelete().equalsIgnoreCase(getAutoDelete())) ||
                (backupManager.getHousekeeping().getMaxNumberBackups() != getMaxNumberBackups()) ) {
            backupManager.getHousekeeping().setMaxNumberBackups(getMaxNumberBackups());
            backupManager.getHousekeeping().setAutoDelete(getAutoDelete());
            backupManager.getHousekeeping().persist();
        }
    }
}
