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
package com.ericsson.adp.mgmt.backupandrestore.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;

public class BackupLocationServiceTest {

    private BackupLocationService backupLocationService;
    private BackupManager backupManager;

    @Before
    public void setup() {
        backupLocationService = new BackupLocationService();
        backupLocationService.setBackupLocation("/");

        backupManager = EasyMock.createMock(BackupManager.class);
        EasyMock.expect(backupManager.getBackupManagerId()).andReturn("123");
        EasyMock.replay(backupManager);
    }

    @Test
    public void getBackupFolder_backupName_backupFolder() throws IOException {
        final Path location = Paths.get("/").resolve("123").resolve("456");
        assertEquals(location, backupLocationService.getBackupFolder("123", "456").getBackupLocation());
    }

    @Test
    public void getBackupManagerLocation_backupManagerId_validPath() {

        final Path path = backupLocationService.getBackupManagerLocation("123");
        assertEquals(Paths.get("/123"), path);
    }

    @Test
    public void getBackupLocation_validPath() {
        assertEquals(Paths.get("/"), backupLocationService.getBackupLocation());
    }
}
