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

import org.springframework.stereotype.Component;

import com.ericsson.adp.mgmt.backupandrestore.backup.PersistedBackup;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidBackupFileException;

/**
 * BackupFileValidator validates backup file attributes
 */
@Component
public class BackupFileValidator {

    private static final String BACKUP_ID = "backupId";
    private static final String BACKUP_NAME = "name";
    private static final String BACKUP_STATUS = "status";
    private static final String ERROR_MESSAGE = " attribute is invalid - can neither be null nor empty";

    /**
     * validateBackupFile used for validation of backup file attributes
     *
     * @param persistedBackup
     *            persistedBackup
     */
    public void validateBackupFile(final PersistedBackup persistedBackup) {

        final StringBuilder errorMessageBuilder = new StringBuilder();

        validateBackupFileAttribute(persistedBackup.getBackupId(), BACKUP_ID, errorMessageBuilder);
        validateBackupFileAttribute(persistedBackup.getName(), BACKUP_NAME, errorMessageBuilder);
        validateBackupFileAttribute(persistedBackup.getStatus(), BACKUP_STATUS, errorMessageBuilder);

        if (errorMessageBuilder.length() > 0) {
            throw new InvalidBackupFileException(errorMessageBuilder.toString());
        }

    }

    private void validateBackupFileAttribute(final Object value, final String attributeName, final StringBuilder errorMessageBuilder) {

        if (value == null || (value instanceof String && ((String) value).isEmpty())) {
            addErrorMessage(attributeName, errorMessageBuilder);
        }

    }

    private void addErrorMessage(final String attributeName, final StringBuilder errorMessageBuilder) {
        addErrorMessagesIfNecessary(errorMessageBuilder);
        errorMessageBuilder.append(attributeName).append(ERROR_MESSAGE);
    }

    private void addErrorMessagesIfNecessary(final StringBuilder errorMessageBuilder) {

        if (errorMessageBuilder.length() > 0) {
            errorMessageBuilder.append(", ");
        }
    }

}
