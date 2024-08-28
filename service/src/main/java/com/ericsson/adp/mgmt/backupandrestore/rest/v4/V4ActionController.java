/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.v4;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.Payload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.SftpServerPayload;
import com.ericsson.adp.mgmt.backupandrestore.agent.exception.ResourceNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.TaskResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.V4ActionResponse;

/**
 * Responsible for action specific endpoints.
 */
@RestController
public class V4ActionController extends V4Controller {

    /**
     * Gets all tasks of a backupManager.
     * @param backupManagerId backupManager to get all tasks from.
     * @return all tasks of a backupManager.
     */
    @GetMapping("backup-managers/{backupManagerId}/tasks")
    public List<ActionResponse> getTasks(@PathVariable("backupManagerId") final String backupManagerId) {
        return getBackupManager(backupManagerId).getActions()
               .stream().map(Action::toSimplifiedJson)
               .collect(Collectors.toList());
    }

    /**
     * Get specific task from a backupManager.
     * @param backupManagerId backupManager in which to look for the task.
     * @param taskId action to look for.
     * @return request action.
     */
    @GetMapping("backup-managers/{backupManagerId}/tasks/{taskId}")
    public TaskResponse getTask(
            @PathVariable("backupManagerId") final String backupManagerId,
            @PathVariable("taskId") final String taskId) {
        final Action action = getBackupManager(backupManagerId)
                .getActions()
                .stream()
                .filter(task -> task.getActionId().equals(taskId))
                .findFirst()
                .orElseThrow(ResourceNotFoundException::new);
        return new TaskResponse(action, buildResourceUrl(action));
    }

    /**
     * Get last action from a backupManager.
     * @param backupManagerId backupManager in which to look for the action.
     * @return last task that took place in a backup manager.
     */
    @GetMapping("backup-managers/{backupManagerId}/last-task")
    public TaskResponse getLastTask(
                                      @PathVariable("backupManagerId") final String backupManagerId) {
        final Action action = getBackupManager(backupManagerId)
                              .getLastCompletedAction().orElseThrow(ResourceNotFoundException::new);
        return new TaskResponse(action, buildResourceUrl(action));
    }

    /**
     * Get imports from a backupManager.
     * @param backupManagerId backupManager in which to look for the imports.
     * @return imports from a backup manager.
     */
    @GetMapping("backup-managers/{backupManagerId}/imports")
    public List<V4ActionResponse> getImports(
                                             @PathVariable("backupManagerId") final String backupManagerId) {
        return getBackupManager(backupManagerId).getActions().stream()
                .filter(action -> action.getName().equals(ActionType.IMPORT))
                .map(action -> new V4ActionResponse(action.getActionId(), getUri(action.getPayload()),
                        getBackupPath(backupManagerId, action.getPayload())))
                .collect(Collectors.toList());
    }

    /**
     * Get an import from a backupManager.
     * @param backupManagerId backupManager in which to look for the import.
     * @param importId of an import to look for
     * @return requested import.
     */
    @GetMapping("backup-managers/{backupManagerId}/imports/{import-id}")
    public V4ActionResponse getImport(
                                      @PathVariable("backupManagerId") final String backupManagerId,
                                      @PathVariable("import-id") final String importId) {
        final Optional<Action> importAction = getBackupManager(backupManagerId)
                .getActions()
                .stream()
                .filter(action -> action.getName().equals(ActionType.IMPORT))
                .filter(action -> action.getActionId().equals(importId))
                .findFirst();

        if (importAction.isPresent()) {
            return new V4ActionResponse(importAction.get().getActionId(), getUri(importAction.get().getPayload()),
                    getBackupPath(backupManagerId, importAction.get().getPayload()),
                    new TaskResponse(importAction.get()));
        } else {
            throw new ResourceNotFoundException(ActionType.IMPORT, importId);
        }
    }

    /**
     * Get exports from a backupManager.
     * @param backupManagerId backupManager in which to look for the exports.
     * @param backupId id of backup in which to look for the exports
     * @return requested export.
     */
    @GetMapping("backup-managers/{backupManagerId}/backups/{backup-id}/exports")
    public List<V4ActionResponse> getExports(
                                             @PathVariable("backupManagerId") final String backupManagerId,
                                             @PathVariable("backup-id") final String backupId) {
        return getBackupManager(backupManagerId)
                .getActions()
                .stream()
                .filter(action -> action.getName().equals(ActionType.EXPORT))
                .filter(action -> action.getBackupName().equals(backupId))
                .map(action -> new V4ActionResponse(action.getActionId(), getUri(action.getPayload()),
                        backupManagerId, getBackupManager(backupManagerId).getBackup(backupId, Ownership.OWNED)))
                .collect(Collectors.toList());
    }

    /**
     * Get an export from a backupManager.
     * @param backupManagerId backupManager in which to look for the export.
     * @param backupId id of backup in which to look for the export
     * @param exportId export to look for
     * @return requested export.
     */
    @GetMapping("backup-managers/{backupManagerId}/backups/{backup-id}/exports/{export-id}")
    public V4ActionResponse getExport(
                                      @PathVariable("backupManagerId") final String backupManagerId,
                                      @PathVariable("backup-id") final String backupId,
                                      @PathVariable("export-id") final String exportId) {
        final Optional<Action> exportAction = getBackupManager(backupManagerId)
                .getActions()
                .stream()
                .filter(action -> action.getName().equals(ActionType.EXPORT))
                .filter(action -> action.getBackupName().equals(backupId))
                .filter(action -> action.getActionId().equals(exportId))
                .findFirst();
        if (exportAction.isPresent()) {
            return new V4ActionResponse(
                new TaskResponse(exportAction.get()),
                    getUri(exportAction.get().getPayload()),
                    exportAction.get().getBackupManagerId(),
                    getBackupManager(exportAction.get().getBackupManagerId()).getBackup(backupId, Ownership.OWNED)
                );
        } else {
            throw new ResourceNotFoundException(ActionType.EXPORT, exportId);
        }
    }

    /**
     * Get restores from a backupManager.
     * @param backupManagerId backupManager in which to look for the restores.
     * @param backupId id of backup in which to look for the restores
     * @return requested restores.
     */
    @GetMapping("backup-managers/{backupManagerId}/backups/{backup-id}/restores")
    public List<V4ActionResponse> getRestores(
                                              @PathVariable("backupManagerId") final String backupManagerId,
                                              @PathVariable("backup-id") final String backupId) {
        return getBackupManager(backupManagerId)
                .getActions()
                .stream()
                .filter(action -> action.getName().equals(ActionType.RESTORE))
                .filter(action -> action.getBackupName().equals(backupId))
                .map(action-> new V4ActionResponse(action.getActionId()))
                .collect(Collectors.toList());
    }

    /**
     * Get a restore from a backupManager.
     * @param backupManagerId backupManager in which to look for the restore.
     * @param backupId id of backup in which to look for the restore
     * @param restoreId of restore to look for
     * @return requested restore
     */
    @GetMapping("backup-managers/{backupManagerId}/backups/{backup-id}/restores/{restore-id}")
    public V4ActionResponse getRestore(
                                       @PathVariable("backupManagerId") final String backupManagerId,
                                       @PathVariable("backup-id") final String backupId,
                                       @PathVariable("restore-id") final String restoreId) {
        return new V4ActionResponse(new TaskResponse(getBackupManager(backupManagerId)
                .getActions()
                .stream()
                .filter(action -> action.getActionId().equals(restoreId))
                .filter(action -> action.getName().equals(ActionType.RESTORE))
                .filter(action -> action.getBackupName().equals(backupId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(ActionType.RESTORE, restoreId))));
    }

    private URI getUri(final Payload payload) {
        if (payload instanceof SftpServerPayload) {
            final SftpServerPayload uriPayload = (SftpServerPayload) payload;
            return uriPayload.getUri();
        } else {
            return null;
        }
    }


    private String getBackupPath(final String backupManagerId, final Payload payload) {
        final ImportPayload importPayload = (ImportPayload) payload;
        final String fileName = new File(importPayload.getUri().getPath()).getName();
        final Optional<String> backupId = BackupManager.filterBackupIDFromTarballFormat(fileName);
        String backupName = fileName;
        if (backupId.isPresent()) {
            backupName  = backupId.get();
        }
        return BACKUP_MANAGERS_PATH + backupManagerId + "/backups/" + backupName;
    }


}
