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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.BackupManagerFileService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnexpectedBackupManagerException;
import com.ericsson.adp.mgmt.backupandrestore.productinfo.ProductInfoService;
import com.ericsson.adp.mgmt.backupandrestore.util.BackupLimitValidator;

/**
 * Persists and retrieves backups.
 */
@Service
public class BackupRepository {

    private static final Logger log = LogManager.getLogger(BackupRepository.class);

    private BackupLimitValidator backupLimitValidator;
    private BackupFileService backupFileService;
    private CMMediatorService cmMediatorService;
    private ProductInfoService productInfoService;
    private BackupManagerFileService backupManagerFileService;
    private BackupManagerRepository backupManagerRepository;


    /**
     * Creates backup.
     * @param backupManager owner of backup.
     * @param action action triggering backup.
     * @param agents agents to participate in backup.
     * @return backup
     */
    public Backup createBackup(final BackupManager backupManager, final Action action, final List<Agent> agents) {
        backupLimitValidator.validateLimit(backupManager.getBackupManagerId());

        final SoftwareVersion appProductInfo = getAppProductInfo();
        final BackupCreationType creationType = getCreationType(action.isScheduledEvent());
        final Optional<OffsetDateTime> creationTime = action.getBackupCreationTime();
        final Backup backup = new Backup(action.getBackupName(), backupManager.getBackupManagerId(), creationTime, creationType, this::persist);
        agents.stream().map(Agent::getSoftwareVersion).forEach(backup::addSoftwareVersion);
        backup.addSoftwareVersion(appProductInfo);

        persistNewBackup(backupManager, backup, ActionType.CREATE_BACKUP, Optional.of(agents));

        return backup;
    }

    /**
     * Imports Backup
     * @param content of file
     * @param backupManager owner of backup.
     * @return backup from file
     */
    public Backup importBackup(final String content, final BackupManager backupManager) {
        backupLimitValidator.validateLimit(backupManager.getBackupManagerId());
        final Backup backup = new Backup(backupFileService.readBackup(content), this::persist);

        if (!backupManager.getBackupManagerId().equalsIgnoreCase(backup.getBackupManagerId())) {
            throw new UnexpectedBackupManagerException(backupManager.getBackupManagerId(), backup.getBackupManagerId(),
                    backup.backupId);
        }
        persistNewBackup(backupManager, backup, ActionType.IMPORT, Optional.empty());
        return backup;
    }

    /**
     * Gets persisted (and therefore owned) backups for a backupManager.
     * @param backupManagerId owner of backups.
     * @return list of backups.
     */
    public List<Backup> getBackups(final String backupManagerId) {
        return backupFileService
                .getBackups(backupManagerId)
                .stream()
                .map(persistedBackup -> new Backup(persistedBackup, this::persist))
                .sorted(Comparator.comparing(Backup::getCreationTime))
                .collect(Collectors.toList());
    }

    /**
     * Sort the owned backups by Status and creation time and return a list
     * @param backupManagerId  backup manager owner of backups.
     * @param maxNumberBackups maximum number of backups allowed
     * @return List of backups sorted by status and creation time
     */
    public List<Backup> getBackupsForAutoDeletion(final String backupManagerId, final int maxNumberBackups) {
        final int totalBackups = getBackups(backupManagerId).size();
        if (totalBackups > maxNumberBackups) {
            return backupFileService
                    .getBackups(backupManagerId)
                    .stream()
                    .map(persistedBackup -> new Backup(persistedBackup, this::persist))
                    .sorted((final Backup backup1, final Backup backup2) -> backup1.compareByStatusCreationTime(backup2))
                    .limit((long) totalBackups - maxNumberBackups)
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Deletes a backup.
     * @param backup to be deleted.
     * @param backupManager owner of backup.
     */
    public void deleteBackup(final Backup backup, final BackupManager backupManager) {
        final int ownedIndex = backupManager.getBackupIndex(backup.getBackupId());
        backupFileService.deleteBackup(backupManager.getBackupManagerId(), backup.getBackupId());
        cmMediatorService.deleteBackupAndWait(backupManager.getBackupManagerId(), ownedIndex);
        // Also remove from any child vBRMs
        backupManagerRepository.getChildren(backupManager.getBackupManagerId()).forEach(vBRM -> {
            final int index = vBRM.getBackupIndex(backup.getBackupId());
            cmMediatorService.deleteBackupAndWait(vBRM.getBackupManagerId(), index);
            vBRM.removeBackup(backup);
        });
        // Remove the backup from the parent BRM
        backupManager.removeBackup(backup);
    }

    private SoftwareVersion getAppProductInfo() {
        try {
            return productInfoService.getAppProductInfo();
        } catch (final Exception exception) {
            log.warn("The application product information could not be retrieved: <{}>. " +
                    "The BRO product information will be used instead.", exception.getMessage());
            return productInfoService.getOrchestratorProductInfo();
        }
    }

    private void persist(final Backup backup) {
        backupFileService.writeToFile(backup);
        final BackupManager owner = backupManagerRepository.getBackupManager(backup.getBackupManagerId());
        cmMediatorService.updateBackupAndWait(owner, backup);
        backupManagerRepository.getChildren(owner.getBackupManagerId()).forEach(vBRM -> {
            log.debug("Updating vbrm {} backup {} information in CM", vBRM.getBackupManagerId(), backup.getName());
            cmMediatorService.updateBackupAndWait(vBRM, backup);
        });
    }

    private void persistNewBackup(
            final BackupManager backupManager,
            final Backup backup,
            final ActionType actionType,
            final Optional<List<Agent>> agents) {
        backupManager.addBackup(backup, Ownership.OWNED);
        backupFileService.writeToFile(backup);
        agents.ifPresent(a -> backupManagerFileService.backupAgentCreateBackupDirectory(backupManager.getBackupManagerId(), backup.getName(), a));
        if (ActionType.CREATE_BACKUP == actionType) {
            final List<BackupManager> backupManagers = backupManagerRepository.getBackupManagers();
            backupManagerFileService.backupBROConfig(
                    backupManager.getBackupManagerId(), backup.getName(), backupManagers);
        }
        cmMediatorService.addBackup(backupManager, backup);
        backupManagerRepository.getChildren(backupManager.getBackupManagerId()).forEach(vBRM -> {
            vBRM.addBackup(backup, Ownership.READABLE);
            cmMediatorService.addBackup(vBRM, backup);
        });
    }

    /**
     * Corrupt backup if incomplete. Writes to persistence layer if status is updated.
     * @param backup object
     * @return true if the backup was marked corrupt, false otherwise
     */
    public boolean corruptBackupIfIncomplete(final Backup backup) {
        if (backup.getStatus().equals(BackupStatus.INCOMPLETE)) {
            backup.setStatus(BackupStatus.CORRUPTED);
            backupFileService.writeToFile(backup);
            return true;
        }
        return false;
    }

    private BackupCreationType getCreationType(final boolean scheduledEvent) {
        return scheduledEvent ? BackupCreationType.SCHEDULED : BackupCreationType.MANUAL;
    }

    @Autowired
    public void setBackupLimitValidator(final BackupLimitValidator backupLimitValidator) {
        this.backupLimitValidator = backupLimitValidator;
    }

    @Autowired
    public void setBackupFileService(final BackupFileService backupFileService) {
        this.backupFileService = backupFileService;
    }

    @Autowired
    public void setCmMediatorService(final CMMediatorService cmMediatorService) {
        this.cmMediatorService = cmMediatorService;
    }

    @Autowired
    public void setProductInfoService(final ProductInfoService productInfoService) {
        this.productInfoService = productInfoService;
    }

    @Autowired
    public void setBackupManagerFileService(final BackupManagerFileService backupManagerFileService) {
        this.backupManagerFileService = backupManagerFileService;
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

    /**
     * Validates if the backups limits is reached or not.
     * Used on Housekeeping
     * @param backupManagerId backupManager to validate
     * @param numberBackupsExpected Compare againts the Number of backups
     * @return true if MaxNumber of Backups is reached
     */
    public boolean isMaxBackupLimitReached(final String backupManagerId, final int numberBackupsExpected) {
        return backupLimitValidator.isMaxNumberOfBackupsReached(backupManagerId, numberBackupsExpected);
    }

}
