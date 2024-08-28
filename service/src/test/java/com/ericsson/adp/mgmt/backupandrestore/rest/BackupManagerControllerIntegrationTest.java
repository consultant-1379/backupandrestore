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
package com.ericsson.adp.mgmt.backupandrestore.rest;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_MANAGER_CONFIG_BACKUP_FOLDER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.PersistedBackupManager;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.UpdateBackupManagerRequest;
import com.ericsson.adp.mgmt.backupandrestore.test.IntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BackupManagerControllerIntegrationTest extends IntegrationTest {

    private static final Path DEFAULT_BACKUP_MANAGER_INFORMATION_LOCATION =
            Paths.get(System.getProperty("java.io.tmpdir"), BACKUP_MANAGER_CONFIG_BACKUP_FOLDER).resolve("DEFAULT").resolve("backupManagerInformation.json");

    @Autowired
    private BackupManagerController controller;

    @Test
    public void updateBackupManager_backupManagerIdAndRequest_noContentResponseAndBackupManagerIsUpdated() throws Exception {
        final UpdateBackupManagerRequest request = new UpdateBackupManagerRequest();
        request.setBackupDomain("AbC");
        request.setBackupType("dEf");

        controller.updateBackupManager("DEFAULT", request);

        final Path file = DEFAULT_BACKUP_MANAGER_INFORMATION_LOCATION;
        assertTrue(file.toFile().exists());

        final String fileContents = Files.readAllLines(file).stream().collect(Collectors.joining(""));

        final PersistedBackupManager persistedManager = new ObjectMapper().readValue(fileContents, PersistedBackupManager.class);
        assertEquals("DEFAULT", persistedManager.getBackupManagerId());
        assertEquals("AbC", persistedManager.getBackupDomain());
        assertEquals("dEf", persistedManager.getBackupType());
    }

}
