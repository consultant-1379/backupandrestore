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

import com.ericsson.adp.mgmt.backupandrestore.action.ActionService;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.V4ActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.V4CreateBackupRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.V4ImportExportRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.V4BackupResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Responsible for v4 backup specific endpoints.
 */
@RestController
public class V4BackupController extends V4Controller {

    @Autowired
    private ActionService actionService;

    /**
     * Gets all backups under a backupManager.
     * @param backupManagerId of backupManager to look for.
     * @return json list response of all backups of a backupManager.
     */
    @GetMapping("backup-managers/{backupManagerId}/backups")
    public List<V4BackupResponse> getBackupsForOneBackupManager(@PathVariable("backupManagerId") final String backupManagerId) {
        return getBackupManager(backupManagerId).getBackups(Ownership.READABLE).stream()
                .map(V4BackupResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Gets one backup.
     * @param backupManagerId of backupManager to look for.
     * @param backupId backup to look for.
     * @return json response of requested backup.
     */
    @GetMapping("backup-managers/{backupManagerId}/backups/{backupId}")
    public V4BackupResponse getOneBackupForOneBackupManager(
            @PathVariable("backupManagerId") final String backupManagerId,
            @PathVariable("backupId") final String backupId) {
        final BackupManager backupManager = getBackupManager(backupManagerId);
        final Backup backup = backupManager.getBackup(backupId, Ownership.READABLE);
        return new V4BackupResponse(backup, backupManager.getLastAction(backup, ActionType.CREATE_BACKUP));
    }

    /**
     * Creates a backup under a backup manager
     * @param backupManagerId of backupManager to look for.
     * @param requestDTO the create backup request object containing the backup name
     * @return json response containing the backup id.
     */
    @PostMapping("backup-managers/{backupManagerId}/backups")
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public CreateActionResponse createBackup(
             @PathVariable("backupManagerId") final String backupManagerId,
             @RequestBody final V4CreateBackupRequest requestDTO) {
        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName(requestDTO.getName());
        final CreateActionRequest request = new CreateActionRequest();
        request.setPayload(payload);
        request.setAction(ActionType.CREATE_BACKUP);
        return new CreateActionResponse(actionService.handleActionRequest(backupManagerId, request).getActionId());
    }

    /**
     * Restore a backup under a backup manager
     * @param backupManagerId of backupManager to look for.
     * @param backupId the name of the backup to be restored.
     * @return json response containing the restore id.
     */
    @PostMapping("backup-managers/{backupManagerId}/backups/{backupId}/restores")
    @ResponseStatus(value = HttpStatus.OK)
    public CreateActionResponse restoreBackup(
             @PathVariable("backupManagerId") final String backupManagerId,
             @PathVariable("backupId") final String backupId) {
        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName(backupId);
        final CreateActionRequest request = new CreateActionRequest();
        request.setPayload(payload);
        request.setAction(ActionType.RESTORE);
        return new CreateActionResponse(actionService.handleActionRequest(backupManagerId, request).getActionId());
    }

    /**
     * Exports a backup under a backup manager
     * @param backupManagerId of backupManager to look for.
     * @param backupId of backup to be exported.
     * @param requestDTO the export backup request object containing the URI and Password
     * @return Json response containing the export id and URI.
     */
    @PostMapping("backup-managers/{backupManagerId}/backups/{backupId}/exports")
    @ResponseStatus(value = HttpStatus.OK)
    public V4ActionResponse exportBackup(
             @PathVariable("backupManagerId") final String backupManagerId,
             @PathVariable("backupId") final String backupId,
             @RequestBody final V4ImportExportRequest requestDTO) {
        final ExportPayload payload = new ExportPayload();
        payload.setBackupName(backupId);
        payload.setPassword(requestDTO.getPassword());
        payload.setUri(requestDTO.getUri());
        final CreateActionRequest request = new CreateActionRequest();
        request.setPayload(payload);
        request.setAction(ActionType.EXPORT);
        return new V4ActionResponse(actionService.handleActionRequest(backupManagerId, request).getActionId(),
                requestDTO.getUri(), backupManagerId, getBackupManager(backupManagerId).getBackup(backupId, Ownership.OWNED));
    }


    /**
     * Import a backup under a backup manager
     * @param backupManagerId of backupManager to look for.
     * @param requestDTO the import backup request object containing the URI and Password
     * @return Json response containing the import id.
     */
    @PostMapping("backup-managers/{backupManagerId}/imports")
    @ResponseStatus(value = HttpStatus.OK)
    public V4ActionResponse importBackup(
             @PathVariable("backupManagerId") final String backupManagerId,
             @RequestBody final V4ImportExportRequest requestDTO) {
        final ImportPayload payload = new ImportPayload();
        payload.setPassword(requestDTO.getPassword());
        payload.setUri(requestDTO.getUri());
        final CreateActionRequest request = new CreateActionRequest();
        request.setPayload(payload);
        request.setAction(ActionType.IMPORT);
        return new V4ActionResponse(actionService.handleActionRequest(backupManagerId, request).getActionId());
    }

    /**
     * Deletes a backup under a backup manager
     * @param backupManagerId of backupManager to look for.
     * @param backupId of the backup
     */
    @DeleteMapping("backup-managers/{backupManagerId}/backups/{backupId}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteBackup(
             @PathVariable("backupManagerId") final String backupManagerId,
             @PathVariable("backupId") final String backupId) {
        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName(backupId);
        final CreateActionRequest request = new CreateActionRequest();
        request.setPayload(payload);
        request.setAction(ActionType.DELETE_BACKUP);
        actionService.handleActionRequest(backupManagerId, request);
    }

}
