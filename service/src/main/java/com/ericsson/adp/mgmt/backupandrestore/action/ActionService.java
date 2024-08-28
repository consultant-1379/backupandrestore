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

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.job.JobExecutor;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.UpdateHousekeepingRequest;

/**
 * Creates and submits action for execution.
 */
@Service
public class ActionService {

    private static final Logger log = LogManager.getLogger(ActionService.class);
    private BackupManagerRepository backupManagerRepository;
    private ActionRepository actionRepository;
    private JobExecutor jobExecutor;
    /**
     * Creates and submits action for execution.
     * @param backupManagerId backupManager on which the action will be performed.
     * @param request action input.
     * @return action.
     */
    public Action handleActionRequest(final String backupManagerId, final CreateActionRequest request) {
        final BackupManager backupManager = backupManagerRepository.getBackupManager(backupManagerId);
        // Build the action from the request
        final Action action = actionRepository.createAction(backupManager, request);
        // Hand it over to the executor, which takes care of persisting it if it's able to be run (no ongoing job)
        log.debug("Action request to execute <{}> to [{}]", request, backupManagerId);
        jobExecutor.execute(backupManager, action);
        return action;
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

    @Autowired
    public void setActionRepository(final ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    @Autowired
    public void setJobExecutor(final JobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    /**
     * Validates if the action needs to execute the housekeeping tasks before
     * the execution of the current Action
     * @param backupManager backupManager on which the housekeeping will be performed.
     * @return Result indicating if it was a success or failed action
     */
    public ResultType executeHousekeeping(final BackupManager backupManager) {
        ResultType result = ResultType.SUCCESS;

        if (AUTO_DELETE_ENABLED.equals(backupManager.getHousekeeping().getAutoDelete())) {
            log.debug("Init Housekeeping execution");
            final Action housekeeping = actionRepository.createAction(backupManager, getHousekeepingRequest(backupManager));
            result = jobExecutor.executeAndWait(backupManager, housekeeping);
            log.debug("Finishing Housekeeping execution");
        }
        return result;
    }

    /**
     * Creates and submits action for execution.
     * @param backupManager backupManager on which the action will be performed.
     * @param request action input.
     * @return Result indicating if it was a success or failed action
     */
    public ResultType executeAndWait (final BackupManager backupManager, final CreateActionRequest request) {
        final Action action = actionRepository.createAction(backupManager, request);
        final ResultType result = jobExecutor.executeAndWait(backupManager, action);
        log.info("Finished Action execution {} : <{}> final result:{}", action.getName(), action.getActionId(), result);
        return result;
    }

    /**
     * Create a housekeeping request used as additional task complement
     * decrement the MaxNumber of backups if the task will create a new backup.
     * @param backupManager  Backup manager executing the housekeeping
     * @return the request with the values required.
     */
    private UpdateHousekeepingRequest getHousekeepingRequest(final BackupManager backupManager) {
        final  UpdateHousekeepingRequest housekeepingRequest = getHousekeepingRequest(
                backupManager.getHousekeeping().getAutoDelete(),
                backupManager.getHousekeeping().getMaxNumberBackups());
        housekeepingRequest.setExecuteAstask(true);
        return housekeepingRequest;
    }

    private UpdateHousekeepingRequest getHousekeepingRequest(final String autoDeleteEnabled,
                                                             final int maximumManualBackupsNumberStore ) {
        final  UpdateHousekeepingRequest housekeepingRequest = new UpdateHousekeepingRequest();
        housekeepingRequest.setAction(ActionType.HOUSEKEEPING);
        housekeepingRequest.setAutoDelete(autoDeleteEnabled);
        housekeepingRequest.setMaximumManualBackupsNumberStored(maximumManualBackupsNumberStore);
        housekeepingRequest.setExecuteAstask(false);
        return housekeepingRequest;
    }

}
