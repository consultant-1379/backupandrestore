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

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_DISABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupLimitExceededException;

public class BackupLimitValidatorTest {

    private BackupLimitValidator backupLimitValidator;
    private BackupManagerRepository backupManagerRepository;

    @Before
    public void setup() {
        backupLimitValidator = new BackupLimitValidator();
        backupManagerRepository = createMock(BackupManagerRepository.class);
    }

    @Test
    public void validateLimit_validBackupLimit_validLimit() {

        final BackupManager backupManager = createMock(BackupManager.class);
        final Backup backup = createMock(Backup.class);
        expect(backupManager.getHousekeeping()).andReturn(new Housekeeping(2, AUTO_DELETE_DISABLED, null, null)).anyTimes();
        expect(backupManagerRepository.getBackupManagers()).andReturn(Arrays.asList(backupManager)).anyTimes();
        expect(backupManager.getBackups(Ownership.OWNED)).andReturn(Arrays.asList(backup));
        expect (backupManager.getBackupManagerId()).andReturn(BackupManager.DEFAULT_BACKUP_MANAGER_ID).anyTimes();
        expect(backupManagerRepository.getBackupManager(EasyMock.anyString())).andReturn(backupManager).anyTimes();

        replay(backupManagerRepository, backupManager);

        backupLimitValidator.setBackupManagerRepository(backupManagerRepository);
        backupLimitValidator.validateLimit(backupManager.getBackupManagerId());
    }

    @Test(expected = BackupLimitExceededException.class)
    public void validateLimit_invalidBackupLimit_throwsException() {

        final BackupManager backupManager = createMock(BackupManager.class);
        final Backup backup = createMock(Backup.class);
        expect (backupManager.getBackupManagerId()).andReturn(BackupManager.DEFAULT_BACKUP_MANAGER_ID).anyTimes();
        expect(backupManager.getHousekeeping()).andReturn(new Housekeeping(1, AUTO_DELETE_DISABLED, null, null)).anyTimes();

        expect(backupManagerRepository.getBackupManager(EasyMock.anyString())).andReturn(backupManager).anyTimes();
        expect(backupManager.getBackups(Ownership.OWNED)).andReturn(Arrays.asList(backup, backup));

        replay(backupManagerRepository, backupManager);

        backupLimitValidator.setBackupManagerRepository(backupManagerRepository);

        backupLimitValidator.validateLimit(backupManager.getBackupManagerId());
    }

    @Test
    public void validateLimit_isMaxNumberOfBackups_true() {

        final BackupManager backupManager = createMock(BackupManager.class);
        final Backup backup = createMock(Backup.class);

        expect(backupManagerRepository.getBackupManagers()).andReturn(Arrays.asList(backupManager)).anyTimes();
        expect(backupManager.getBackups(Ownership.OWNED)).andReturn(Arrays.asList(backup, backup, backup));
        expect(backupManager.getHousekeeping()).andReturn(new Housekeeping(1, AUTO_DELETE_ENABLED, null, null));
        expect(backupManagerRepository.getBackupManager("1")).andReturn(backupManager).anyTimes();
        replay(backupManagerRepository, backupManager);

        backupLimitValidator.setBackupManagerRepository(backupManagerRepository);
        // Maximum Number Backups allowed is 1 and there are 3 backups
        assertTrue(backupLimitValidator.isMaxNumberOfBackupsReached("1", 2));

    }

    @Test
    public void validateLimit_isMaxNumberOfBackups_false() {

        final BackupManager backupManager = createMock(BackupManager.class);
        final Backup backup = createMock(Backup.class);

        expect(backupManagerRepository.getBackupManagers()).andReturn(Arrays.asList(backupManager)).anyTimes();
        expect(backupManager.getBackups(Ownership.OWNED)).andReturn(Arrays.asList(backup));
        expect(backupManager.getHousekeeping()).andReturn(new Housekeeping(1, AUTO_DELETE_ENABLED, null, null));
        expect(backupManagerRepository.getBackupManager("1")).andReturn(backupManager).anyTimes();
        replay(backupManagerRepository, backupManager);

        backupLimitValidator.setBackupManagerRepository(backupManagerRepository);
        // Maximum Number Backups allowed is 1 and there are 1 backups
        assertFalse(backupLimitValidator.isMaxNumberOfBackupsReached("1", 1));
    }
}
