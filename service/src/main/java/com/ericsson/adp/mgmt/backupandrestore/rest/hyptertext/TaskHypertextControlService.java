/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************/
package com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.job.JobExecutor;


/**
 * A service responsible for determining the on-going
 * and available tasks of a backup manager
 */
@Service
public class TaskHypertextControlService {

    private JobExecutor jobExecutor;

    private AvailableTasksService availableTaskService;

    private TaskHypertextControlFactory hypertextControlFactory;

    /**
     * Get the hypertext control objects of the backup manager's ongoing tasks
     * @param baseURI the backup manager URI
     * @param brmId the backup manager id
     * @return the list of hypertext control objects of the backup manager's ongoing tasks
     */
    public List<TaskHypertextControl> getOngoingTasks(final String baseURI, final String brmId) {
        return jobExecutor.getRunningJobs().stream()
                .filter(job -> job.getBackupManagerId().equals(brmId))
                .map(job -> hypertextControlFactory.getOngoingTaskHyperTextControl(baseURI, job.getAction()))
                .collect(Collectors.toList());
    }

    /**
     * Get the hypertext control objects of the backup manager's available tasks
     * @param baseURI the backup manager URI
     * @param brmId the backup manager id
     * @return the list of hypertext control objects of the backup manager's available tasks
     */
    public List<TaskHypertextControl> getAvailableTasks(final String baseURI, final String brmId) {
        return availableTaskService.getAvailableTasks(brmId).stream()
                .map(task -> hypertextControlFactory.getAvailableTaskHyperTextControl(baseURI, task.getName(), task.getBackup()))
                .collect(Collectors.toList());
    }

    @Autowired
    public void setJobExecutor(final JobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    @Autowired
    protected void setAvailableTaskService(final AvailableTasksService availableTaskService) {
        this.availableTaskService = availableTaskService;
    }

    @Autowired
    protected void setHypertextControlFactory(final TaskHypertextControlFactory hypertextControlFactory) {
        this.hypertextControlFactory = hypertextControlFactory;
    }
}
