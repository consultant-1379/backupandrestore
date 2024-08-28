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
package com.ericsson.adp.mgmt.backupandrestore.rest.v4;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.v4.V4BackupManagerResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.v4.V4BackupManagersResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.TaskHypertextControlService;

/**
 * Responsible for backupManager specific endpoints.
 */
@RestController
public class V4BackupManagerController extends V4Controller {

    private TaskHypertextControlService taskService;

    /**
     * Gets all backupManagers.
     * @param request the HTTP Request
     * @return json response of all backupManagers.
     */
    @GetMapping("backup-managers")
    public V4BackupManagersResponse getBackupManagers(final HttpServletRequest request) {
        final Path backupManagersURI = Path.of(request.getRequestURI());
        final List<V4BackupManagerResponse> backupManagers = backupManagerRepository.getBackupManagers()
                .stream()
                .map(brm -> getV4BackupManagerResponse(backupManagersURI.resolve(brm.getBackupManagerId()), brm))
                .collect(Collectors.toList());
        return new V4BackupManagersResponse(backupManagers);
    }

    /**
     * Gets one backupManager.
     * @param backupManagerId of backupManager to look for.
     * @param request the HTTP Request
     * @return json response of requested backupManager.
     */
    @GetMapping("backup-managers/{backupManagerId}")
    public V4BackupManagerResponse getOneBackupManager(@PathVariable("backupManagerId") final String backupManagerId,
                                                       final HttpServletRequest request) {
        final Path backupManagersURI = Path.of(request.getRequestURI());
        return getV4BackupManagerResponse(backupManagersURI, getBackupManager(backupManagerId));
    }

    private V4BackupManagerResponse getV4BackupManagerResponse(final Path baseURI, final BackupManager brm) {
        return new V4BackupManagerResponse(brm,
                taskService.getOngoingTasks(baseURI.toString(), brm.getBackupManagerId()),
                taskService.getAvailableTasks(baseURI.toString(), brm.getBackupManagerId()));
    }

    @Autowired
    protected void setTaskService(final TaskHypertextControlService taskService) {
        this.taskService = taskService;
    }
}
