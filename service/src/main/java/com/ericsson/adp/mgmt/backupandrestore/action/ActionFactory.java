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

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_DISABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;

import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.EmptyPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.PasswordSafeExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.PasswordSafeImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.Payload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.SftpServerPayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidActionException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidAutoDeleteValueException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidNumberOfBackupsAllowed;
import com.ericsson.adp.mgmt.backupandrestore.exception.NotImplementedException;
import com.ericsson.adp.mgmt.backupandrestore.exception.SftpServerNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnprocessableEntityException;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.ESAPIValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;

/**
 * Responsible for creating actions.
 */
@Service
public class ActionFactory {
    private static final Logger log = LogManager.getLogger(ActionFactory.class);
    private static final Integer MIN_NUM_BACKUPS = 0;
    private static final Integer MAX_NUM_BACKUPS = 65535;

    private static final int ACTION_ID_LIMIT = 65535;
    private static final String INVALID_PAYLOAD = "Invalid payload";
    private static final String URI_SCHEME_SFTP = "sftp";
    IntPredicate validMaxNumberBackups = maxbackups -> MIN_NUM_BACKUPS <= maxbackups && maxbackups < MAX_NUM_BACKUPS;

    private final Random randomNumberGenerator = new Random();
    private ActionRepository actionRepository;
    private ESAPIValidator esapiValidator;
    private IdValidator idValidator;

    /**
     * Creates an action based on a request.
     * @param backupManager owner of action.
     * @param request to create action.
     * @return action.
     */
    protected Action createAction(final BackupManager backupManager, final CreateActionRequest request) {
        validateCreateRequest(backupManager, request);
        return new Action(createActionRequest(request, backupManager), actionRepository::persist);
    }

    private String getNewId(final BackupManager backupManager) {
        if (backupManager.getActions().size() >= ACTION_ID_LIMIT) {
            throw new InvalidActionException("Maximum number of actions exceeded");
        }

        int newId = randomNumberGenerator.nextInt(ACTION_ID_LIMIT + 1);
        final Set<String> existingActionIds = backupManager.getActions().stream().map(Action::getActionId).collect(Collectors.toSet());

        while (existingActionIds.contains(String.valueOf(newId))) {
            newId = randomNumberGenerator.nextInt(ACTION_ID_LIMIT + 1);
        }

        return String.valueOf(newId);
    }

    /**
     * Validate createAction requests
     * @param backupManager owner of action.
     * @param request to create action.
     */
    private void validateCreateRequest(final BackupManager backupManager, final CreateActionRequest request) {
        if (request.getAction() == null) {
            throw new InvalidActionException("No action defined");
        }
        validateAction (backupManager, request);
    }

    /**
     * Validate each action based on Action Type
     * @param backupManager owner of action.
     * @param request to create action.
     */
    private void validateAction(final BackupManager backupManager, final CreateActionRequest request) {
        switch (request.getAction()) {
            case CREATE_BACKUP:
                validateActionWithBackupName(request);
                break;
            case RESTORE:
            case HOUSEKEEPING_DELETE_BACKUP:
            case DELETE_BACKUP:
                validateActionWithExistingBackup(backupManager, request);
                break;
            case IMPORT:
                validateActionWithImportPayload(backupManager, request);
                break;
            case EXPORT:
                validateActionWithExportPayload(backupManager, request);
                break;
            case HOUSEKEEPING:
                validateHousekeepingActionWithoutPayload(backupManager, request);
                break;
            default:
                throw new NotImplementedException();
        }
    }

    private void validateActionWithBackupName(final CreateActionRequest request) {
        if (!(request.getPayload() instanceof BackupNamePayload)) {
            throw new InvalidActionException(INVALID_PAYLOAD);
        }

        final BackupNamePayload backupNamePayload = (BackupNamePayload) request.getPayload();
        validateBackupName(backupNamePayload.getBackupName());
    }

    private void validateActionWithImportPayload(final BackupManager backupManager, final CreateActionRequest request) {
        if (!(request.getPayload() instanceof ImportPayload)) {
            throw new InvalidActionException(INVALID_PAYLOAD);
        }

        final ImportPayload importPayload = (ImportPayload) request.getPayload();

        validateSftpServerInformation(backupManager, request, importPayload);

        validateBackupName(importPayload);
    }

    private void validateActionWithExportPayload(final BackupManager backupManager, final CreateActionRequest request) {
        if (!(request.getPayload() instanceof ExportPayload)) {
            throw new InvalidActionException(INVALID_PAYLOAD);
        }

        final ExportPayload exportPayload = (ExportPayload) request.getPayload();

        validateBackupName(exportPayload.getBackupName());
        ensureBackupExists(backupManager, exportPayload.getBackupName(), request, Ownership.OWNED);

        validateSftpServerInformation(backupManager, request, exportPayload);
    }

    private void validateSftpServerInformation(final BackupManager backupManager, final CreateActionRequest request,
                                               final SftpServerPayload payload) {
        log.info("Validating SftpServer Information");
        if (payload.getUri() != null) {
            log.debug("Validating the payload's URI {}", payload.getUri());
            validateURI(request, payload.getUri());
            if (payload.getUri().getScheme().equals(URI_SCHEME_SFTP)) {
                validatePassword(request, payload.getPassword());
            }
        } else if (payload.getSftpServerName() != null) {
            log.debug("Validating the payload's sftp server name {}", payload.getSftpServerName());
            validateBRMHasSftpServer(backupManager, payload.getSftpServerName(), request);
        } else {
            throw new InvalidActionException("No sftp server information was provided for action of type " + request.getAction());
        }
    }

    private void validateBRMHasSftpServer(final BackupManager backupManager, final String sftpServerName, final CreateActionRequest request) {
        if (sftpServerName.isEmpty()) {
            throw new InvalidActionException("No sftp-server-name was provided for action of type " + request.getAction());
        }
        final Optional<SftpServer> optionalSftpServer = backupManager.getSftpServers().stream()
                .filter(sftpServer -> sftpServer.getName().equals(sftpServerName))
                .findFirst();
        if (optionalSftpServer.isPresent()) {
            log.debug("Successfully validated sftp server name {}", sftpServerName);
        } else {
            final String sftpServerException = new SftpServerNotFoundException(sftpServerName,
                    backupManager.getBackupManagerId()).getMessage();
            throw new InvalidActionException(sftpServerException);
        }
    }

    /**
     * To validate a partial update request for housekeeping config
     * @param backupManager the BRM owns the housekeeping
     * @param request the update request.
     */
    private void validateHousekeepingActionWithoutPayload(final BackupManager backupManager, final CreateActionRequest request) {
        if (request.getMaximumManualBackupsNumberStored() == null && request.getAutoDelete() == null) {
            log.error("Both autoDelete and maxStoredBackups are missing, " +
                "BRO can't process the request");
            throw new UnprocessableEntityException("Both autoDelete and maxStoredBackups are missing, BRO can't process the request");
        }
        if (request.getMaximumManualBackupsNumberStored() == null) {
            request.setMaximumManualBackupsNumberStored(backupManager.getHousekeeping().getMaxNumberBackups());
        } else {
            validateMaximumBackupsNumberStored(backupManager, request);
        }
        if (request.getAutoDelete() == null) {
            request.setAutoDelete(backupManager.getHousekeeping().getAutoDelete());
        } else {
            validateAutoDelete(request.getAutoDelete());
        }
    }

    private void validateActionWithExistingBackup(final BackupManager backupManager, final CreateActionRequest request) {
        validateActionWithBackupName(request);
        final BackupNamePayload payload = (BackupNamePayload) request.getPayload();
        ensureBackupExists(backupManager, payload.getBackupName(), request, Ownership.READABLE);
    }

    private void validateURI(final CreateActionRequest request, final URI uri) {
        if (uri.toString().isEmpty()) {
            throw new InvalidActionException("No URI was provided for action of type " + request.getAction());
        }
        esapiValidator.validateURI(uri);
        log.debug("Successfully validated URI {}", uri);
    }

    private void validatePassword(final CreateActionRequest request, final String password) {
        if (password == null || password.isEmpty()) {
            throw new InvalidActionException("No password was provided for action of type " + request.getAction());
        }
    }

    private void validateMaximumBackupsNumberStored(final BackupManager backupManager, final CreateActionRequest request) {
        if (!validMaxNumberBackups.test(request.getMaximumManualBackupsNumberStored())) {
            throw new InvalidNumberOfBackupsAllowed("Invalid number of backups <" + request.getMaximumManualBackupsNumberStored() + ">"
                    + " allowed for the Backup Manager <"
                    + backupManager.getBackupManagerId() + ">. Value range is <" + MIN_NUM_BACKUPS + "-" + MAX_NUM_BACKUPS + ">");
        }
    }

    private void validateAutoDelete(final String autoDelete) {
        if (!(AUTO_DELETE_ENABLED.equals(autoDelete) || AUTO_DELETE_DISABLED.equals(autoDelete))) {
            throw new InvalidAutoDeleteValueException("Invalid value supplied for auto delete. Should be either enabled or disabled");
        }
    }

    private void validateBackupName(final String backupName) {
        idValidator.validateId(backupName);
        esapiValidator.validateBackupName(backupName);
    }

    private void validateBackupName(final ImportPayload importPayload) {
        String backupFile = null;
        if (importPayload.getUri() != null) {
            backupFile = new File(importPayload.getUri().getPath()).getName();
        } else {
            backupFile = new File(importPayload.getBackupPath()).getName();
        }
        final Optional<String> backupNameOptional = BackupManager.filterBackupIDFromTarballFormat(backupFile);
        validateBackupName(backupNameOptional.orElse(backupFile));
    }

    private void ensureBackupExists(final BackupManager backupManager, final String backupName,
                                    final CreateActionRequest request, final Ownership ownership) {
        try {
            backupManager.getBackup(backupName, ownership);
        } catch (final BackupNotFoundException e) {
            if (request.getAction() == ActionType.HOUSEKEEPING_DELETE_BACKUP) {
                log.warn("No backup exists with the backup name of <{}> " +
                        "under the Backup Manager <{}> for action of type <{}>",
                        backupName, backupManager.getBackupManagerId(), request.getAction());
            } else {
                throw new InvalidActionException("No backup exists with the backup name of <" + backupName + "> under the Backup Manager <"
                        + backupManager.getBackupManagerId() + "> for action of type <" + request.getAction() + ">", e);
            }
        }
    }

    private ActionRequest createActionRequest(final CreateActionRequest request, final BackupManager backupManager) {
        final ActionRequest actionRequest = new ActionRequest();
        actionRequest.setActionId(getNewId(backupManager));
        actionRequest.setAction(request.getAction());
        actionRequest.setPayload(processPayload(request.getPayload()));
        actionRequest.setBackupManagerId(backupManager.getBackupManagerId());
        actionRequest.setExecuteAsTask(request.isExecutedAstask());
        actionRequest.setScheduledEvent(request.isScheduledEvent());
        if (request.isExecutedAstask()) {
            actionRequest.setAutoDelete(backupManager.getHousekeeping().getAutoDelete());
            actionRequest.setMaximumManualBackupsStored(backupManager.getHousekeeping().getMaxNumberBackups());
        } else {
            if (request.getAutoDelete() != null) {
                actionRequest.setAutoDelete(request.getAutoDelete());
            }
            if (request.getMaximumManualBackupsNumberStored() != null) {
                actionRequest.setMaximumManualBackupsStored(request.getMaximumManualBackupsNumberStored());
            }
        }
        return actionRequest;
    }

    private Payload processPayload(final Payload payload) {
        if (payload instanceof ImportPayload) {
            return new PasswordSafeImportPayload((ImportPayload) payload);
        } else if (payload instanceof ExportPayload) {
            return new PasswordSafeExportPayload((ExportPayload) payload);
        } else if (payload == null) {
            return new EmptyPayload("");
        }

        return payload;
    }

    @Autowired
    public void setActionRepository(final ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    @Autowired
    public void setEsapiValidator(final ESAPIValidator esapiValidator) {
        this.esapiValidator = esapiValidator;
    }

    @Autowired
    public void setIdValidator(final IdValidator idValidator) {
        this.idValidator = idValidator;
    }

}
