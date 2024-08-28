/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupLimitExceededException;

/**
 * BackupLimitValidator helps to validate backup/import limit in Backup Manager
 */
@Component
public class BackupLimitValidator {

    private static final Logger log = LogManager.getLogger(BackupLimitValidator.class);
    private BackupManagerRepository backupManagerRepository;

    /**
     * Validates backup manager's backup limit size, if limited reached throw BackupLimitExceededException
     * @param backupManagerId Backup manager id to be validated
     */
    public void validateLimit(final String backupManagerId) {
        if (isLimitReached(backupManagerId)) {
            throw new BackupLimitExceededException(getMaxNumberBackups(backupManagerId));
        }
    }

    /**
     * Validates if limit is reached
     * @param backupManagerId backup manager id to be validated
     * @return true if limit is reached otherwise returns false
     */
    private boolean isLimitReached(final String backupManagerId) {
        final int totalNumberOfBackups = getNumberOfBackups(backupManagerId);
        log.debug("current number of backups in backup manager::{}, backup manager limit::{}", totalNumberOfBackups,
                getMaxNumberBackups(backupManagerId));
        return (totalNumberOfBackups >= getMaxNumberBackups (backupManagerId));
    }

    /**
     * Validates if the maximum number of backups stored is reached, compares with the value expected
     * @param backupManagerId id to look for.
     * @param numberBackupsExpected expected number of backups
     * @return true if limit is reached otherwise returns false
     */
    public boolean isMaxNumberOfBackupsReached(final String backupManagerId, final int numberBackupsExpected) {
        final int totalNumberOfBackups = getNumberOfBackups(backupManagerId);
        return (totalNumberOfBackups > numberBackupsExpected);
    }

    /**
     * Count the number of owned backups on an backup manager
     * @param backupManagerId id to look for backups to be deleted.
     * @return int Indicates the sum of all the backups in the backupManagers.
     */
    private int getNumberOfBackups(final String backupManagerId) {
        return backupManagerRepository.getBackupManager(backupManagerId).getBackups(Ownership.OWNED).size();
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

    private int getMaxNumberBackups(final String backupManagerId) {
        return backupManagerRepository.getBackupManager(backupManagerId).getHousekeeping().getMaxNumberBackups();
    }
}
