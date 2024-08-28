/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.storage;

import com.ericsson.adp.mgmt.backupandrestore.backup.BackupFolder;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StorageMetadataFileServiceTest {

    private static final String BACKUP_MANAGER_ID = "brm";
    private static final String BACKUP_NAME = "testBackup";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private StorageMetadataFileService fileService;
    private BackupLocationService backupLocationService;

    @Before
    public void setup() {
        backupLocationService = createMock(BackupLocationService.class);

        final Path backupFolder = folder.getRoot().toPath().resolve(BACKUP_MANAGER_ID).resolve(BACKUP_NAME);
        expect(backupLocationService.getBackupFolder(BACKUP_MANAGER_ID, BACKUP_NAME))
                .andReturn(new BackupFolder(backupFolder));
        replay(backupLocationService);
        fileService = new StorageMetadataFileService();

        fileService.setJsonService(new JsonService());
        fileService.setBackupLocationService(backupLocationService);
    }

    @Test
    public void createStorageMetadataFile_backupManagerIdAndBackupName_createsStorageMetadataFile() {
        assertFalse(storageMetadataFileExists());

        fileService.createStorageMetadataFile(BACKUP_MANAGER_ID, BACKUP_NAME);

        assertTrue(storageMetadataFileExists());
    }

    private boolean storageMetadataFileExists() {
        return Files.exists(this.folder.getRoot().toPath()
                        .resolve(BACKUP_MANAGER_ID)
                        .resolve(BACKUP_NAME)
                        .resolve("brIntStorage.json"));
    }
}
