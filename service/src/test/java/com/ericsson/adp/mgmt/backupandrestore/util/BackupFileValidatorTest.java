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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.PersistedBackup;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidBackupFileException;

public class BackupFileValidatorTest {

    private BackupFileValidator backupFileValidator;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        this.backupFileValidator = new BackupFileValidator();
    }

    @Test
    public void validateBackupFile_validFileContent_validContent() {
        backupFileValidator.validateBackupFile(getPersistedBackup("123", BackupStatus.INCOMPLETE, "backup123"));
    }

    @Test
    public void validateBackupFile_invalidBackupId_validateExceptionMessage() {

        expectedException.expect(InvalidBackupFileException.class);
        expectedException.expectMessage("Invalid backup file:: backupId attribute is invalid - can neither be null nor empty");

        backupFileValidator.validateBackupFile(getPersistedBackup(null, BackupStatus.INCOMPLETE, "backup123"));
    }

    @Test
    public void validateBackupFile_invalidName_validateExceptionMessage() {

        expectedException.expect(InvalidBackupFileException.class);
        expectedException.expectMessage("Invalid backup file:: name attribute is invalid - can neither be null nor empty");

        backupFileValidator.validateBackupFile(getPersistedBackup("123", BackupStatus.INCOMPLETE, ""));

    }

    @Test
    public void validateBackupFile_invalidStatus_validateExceptionMessage() {

        expectedException.expect(InvalidBackupFileException.class);
        expectedException.expectMessage("Invalid backup file:: status attribute is invalid - can neither be null nor empty");

        backupFileValidator.validateBackupFile(getPersistedBackup("123", null, "backup123"));

    }

    @Test
    public void validateBackupFile_invalidBackupIdAndStatus_validateExceptionMessage() {

        expectedException.expect(InvalidBackupFileException.class);
        expectedException.expectMessage(
                "Invalid backup file:: backupId attribute is invalid - can neither be null nor empty, status attribute is invalid - can neither be null nor empty");

        backupFileValidator.validateBackupFile(getPersistedBackup(null, null, "backup123"));

    }

    private PersistedBackup getPersistedBackup(final String backupId, final BackupStatus status, final String name) {
        final PersistedBackup persistedBackup = new PersistedBackup();
        persistedBackup.setBackupId(backupId);
        persistedBackup.setStatus(status);
        persistedBackup.setName(name);
        return persistedBackup;
    }

}
