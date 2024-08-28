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
package com.ericsson.adp.mgmt.backupandrestore.job;

import java.util.List;
import java.util.Optional;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionFileService;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;


/**
 * Responsible for submitting jobs.
 */
public interface JobExecutor {
    /**
     * Submits an action for execution.
     * @param backupManager target of action.
     * @param action to be executed.
     */
    void execute(final BackupManager backupManager, final Action action);

    /**
     * Execute an action and waits until it finished completely
     * @param backupManager backupManager
     * @param action Action to be executed
     * @return ResultType indicating whether the result of the work was successful or a failure.
     */
    @Deprecated(since = "05/11/2020")
    ResultType executeAndWait(final BackupManager backupManager, final Action action);

    /**
     * Get a collection  of jobs running in the executor
     * @return a collection of jobs running in the execution threads at the time of calling. If empty, no job is running.
     */
    List<Job> getRunningJobs();

    /**
     * Set the current job. Deprecated as implementers are responsible for managing which job is running, this is a
     * holdover from original implementation
     * @param currentJob - The job which should be registered as currently running
     * @deprecated
     * */
    @Deprecated(since = "05/11/2020")
    void setCurrentJob(final Optional<Job> currentJob);

    /***
     * Executors require a job factory to convert actions to executable jobs. This allows implementers to expose
     * control of the job factory in use to executor interface consumers.
     * @param jobFactory - the factory to use to convert actions to jobs
     * */
    void setJobFactory(final JobFactory jobFactory);

    /***
     * Executors requires ActionFileService to persist actions to file.
     * @param actionFileService - Responsible for writing/reading actions to/from json files
     * */
    void setActionFileService(final ActionFileService actionFileService);

    /***
     * Executors requires CMMediatorService to persist actions to CMM.
     * @param cmMediatorService - Configuration Management Mediator Service.
     * */
    void setCmMediatorService(final CMMediatorService cmMediatorService);
}
