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
package com.ericsson.adp.mgmt.backupandrestore.action.yang;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionService;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.EtagNotifIdBase;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangBackupNameActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangSftpServerActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangSftpServerNameInput;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangURIInput;

/**
 * Handles Yang Actions.
 */
@Service
public class YangActionService {

    private static Logger log = LogManager.getLogger(YangActionService.class);

    private ActionService actionService;
    private BackupManagerRepository backupManagerRepository;
    private EtagNotifIdBase etagNotifIdBase;

    /**
     * Creates an action to create a backup.
     * @param request action input.
     * @return id of action.
     */
    public Action createBackup(final YangBackupNameActionRequest request) {
        log.debug("Create Backup YP Backup manager <{}>: {}", getBackupManager(request).getBackupManagerId(),
                parseYangRequest(request, ActionType.CREATE_BACKUP));
        return actionService.handleActionRequest(getBackupManager(request).getBackupManagerId(), parseYangRequest(request, ActionType.CREATE_BACKUP));
    }

    /**
     * Creates an action to delete a backup.
     * @param request action input.
     * @return id of action.
     */
    public Action deleteBackup(final YangBackupNameActionRequest request) {
        log.debug("Delete Backup YP Backup manager <{}>: {}", getBackupManager(request).getBackupManagerId(),
                parseYangRequest(request, ActionType.DELETE_BACKUP));
        return actionService.handleActionRequest(getBackupManager(request).getBackupManagerId(), parseYangRequest(request, ActionType.DELETE_BACKUP));
    }

    /**
     * Creates an action to import a backup.
     * @param request action input.
     * @return id of action.
     */
    public Action importBackup(final YangSftpServerActionRequest request) {
        log.debug("Import Backup YP Backup manager <{}>: {}", getBackupManager(request).getBackupManagerId(),
                parseImportYangRequest(request, ActionType.IMPORT));
        return actionService.handleActionRequest(getBackupManager(request).getBackupManagerId(), parseImportYangRequest(request, ActionType.IMPORT));
    }

    /**
     * Creates an action to restore a backup.
     * @param request action input.
     * @return id of action.
     */
    public Action restore(final YangActionRequest request) {
        log.debug("Restore Backup YP Backup manager <{}>: {}", getBackupManager(request).getBackupManagerId(),
                parseYangRequest(request, ActionType.RESTORE));
        return actionService.handleActionRequest(getBackupManager(request).getBackupManagerId(), parseYangRequest(request, ActionType.RESTORE));
    }

    /**
     * Creates an action to export a backup.
     * @param request action input.
     * @return id of action.
     */
    public Action export(final YangSftpServerActionRequest request) {
        log.debug("Export Backup YP Backup manager <{}>: {}", getBackupManager(request).getBackupManagerId(),
                parseYangRequest(request, ActionType.EXPORT));
        return actionService.handleActionRequest(getBackupManager(request).getBackupManagerId(), parseExportYangRequest(request, ActionType.EXPORT));
    }

    // Setter for the logger (for testing purposes)
    public void setLogger(final Logger logger) {
        this.log = logger;
    }

    @Autowired
    public void setActionService(final ActionService actionService) {
        this.actionService = actionService;
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

    @Autowired
    public void setEtagNotifIdBase(final EtagNotifIdBase etagNotifIdBase) {
        this.etagNotifIdBase = etagNotifIdBase;
    }

    private BackupManager getBackupManager(final YangActionRequest request) {
        final int indexBackupManager = etagNotifIdBase.getIndexBackupManager(request.getContext(), request.getConfigETag());
        if (log.isDebugEnabled()) {
            final Map<Integer, String> positionIdMap = IntStream.range(0, backupManagerRepository
                    .getBackupManagers().size())
                    .boxed()
                    .collect(Collectors.toMap(
                        index -> index,
                        index -> (String) backupManagerRepository
                        .getBackupManagers().get(index).getBackupManagerId()
                        ));
            log.debug ("Backup Manager <{} - {} in {}>",
                    backupManagerRepository
                    .getBackupManagers()
                    .get(indexBackupManager).getBackupManagerId(),
                    indexBackupManager, positionIdMap
            );
        }
        try {
            return backupManagerRepository
                    .getBackupManagers()
                    .get(indexBackupManager);
        } catch (final UnprocessableYangRequestException e) {
            throw e;
        } catch (final Exception e) {
            throw new UnprocessableYangRequestException("Backup Manager not found", e);
        }
    }

    private Backup getBackup(final YangActionRequest request) {
        // gets the backup manager from BRO backupManager repository
        final BackupManager backupManager = getBackupManager(request);
        try {
            return backupManager
                    .getBackups(Ownership.READABLE)
                    .get(etagNotifIdBase.getIndexBackupManagerBackup(request.getContext(), request.getConfigETag()));
        } catch (final UnprocessableYangRequestException e) {
            throw e;
        } catch (final Exception e) {
            throw new UnprocessableYangRequestException("Backup not found", e);
        }
    }

    private CreateActionRequest parseYangRequest(final YangActionRequest request, final ActionType type) {
        final CreateActionRequest actionRequest = new CreateActionRequest();
        actionRequest.setAction(type);
        actionRequest.setPayload(new BackupNamePayload(getBackup(request).getBackupId(), Optional.empty()));
        return actionRequest;
    }

    private CreateActionRequest parseYangRequest(final YangBackupNameActionRequest request, final ActionType type) {
        final CreateActionRequest actionRequest = new CreateActionRequest();
        actionRequest.setAction(type);
        actionRequest.setPayload(new BackupNamePayload(request.getName(), Optional.empty()));
        return actionRequest;
    }

    private CreateActionRequest parseImportYangRequest(final YangSftpServerActionRequest request, final ActionType type) {
        final CreateActionRequest actionRequest = new CreateActionRequest();
        actionRequest.setAction(type);
        actionRequest.setPayload(getImportPayload(request));
        return actionRequest;
    }

    private CreateActionRequest parseExportYangRequest(final YangSftpServerActionRequest request, final ActionType type) {
        final CreateActionRequest actionRequest = new CreateActionRequest();
        actionRequest.setAction(type);
        actionRequest.setPayload(getExportPayload(request));
        return actionRequest;
    }

    private ExportPayload getExportPayload(final YangSftpServerActionRequest request) {
        final String backupName = getBackup(request).getBackupId();
        if (request.getInput() instanceof YangURIInput) {
            final YangURIInput uriInput = (YangURIInput) request.getInput();
            return new ExportPayload(backupName, uriInput.getUri(), uriInput.getPassword());
        } else {
            final YangSftpServerNameInput sftpServerNameInput = (YangSftpServerNameInput) request.getInput();
            return new ExportPayload(backupName, sftpServerNameInput.getSftpServerName());
        }
    }

    private ImportPayload getImportPayload(final YangSftpServerActionRequest request) {
        if (request.getInput() instanceof YangURIInput) {
            final YangURIInput uriInput = (YangURIInput) request.getInput();
            return new ImportPayload(uriInput.getUri(), uriInput.getPassword());
        } else {
            final YangSftpServerNameInput sftpServerNameInput = (YangSftpServerNameInput) request.getInput();
            return new ImportPayload(sftpServerNameInput.getBackupPath(), sftpServerNameInput.getSftpServerName());
        }
    }

}
