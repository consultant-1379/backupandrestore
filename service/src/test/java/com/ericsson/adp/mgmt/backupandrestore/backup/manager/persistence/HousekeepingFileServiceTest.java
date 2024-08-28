/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_DISABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.VirtualInformation;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.HousekeepingInformation;
import com.ericsson.adp.mgmt.backupandrestore.exception.FilePersistenceException;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HousekeepingFileServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path fileLocation;
    private HousekeepingFileService fileService;
    private BackupManagerRepository managerRepo;

    @Before
    public void setup() throws Exception {
        fileLocation = Paths.get(folder.getRoot().getAbsolutePath());
        managerRepo = createMock(BackupManagerRepository.class);

        fileService = new HousekeepingFileService();
        fileService.setJsonService(new JsonService());
        fileService.setBackupManagersLocation(fileLocation.toAbsolutePath().toString());
        fileService.setManagerRepository(managerRepo);
    }

    @Test
    public void writeToFile_changeHousekeepingInformationAndPersistItAgain_persistHousekeepingWithNewInformation() throws Exception {
        final PersistedBackupManager persistedBackupManager = new PersistedBackupManager();
        persistedBackupManager.setBackupManagerId("456");
        final BackupManager backupManager = new BackupManager(
                persistedBackupManager,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new Housekeeping("456", null),
                null, null, null, null,
                new VirtualInformation());

        fileService.writeToFile(backupManager.getHousekeeping());

        final Path file = fileLocation.resolve("456").resolve("housekeepingInformation.json");
        assertTrue(file.toFile().exists());

        final String fileContents = Files.readAllLines(file).stream().collect(Collectors.joining(""));

        final HousekeepingInformation housekeepingInformation = new ObjectMapper().readValue(fileContents, HousekeepingInformation.class);
        assertEquals(1, housekeepingInformation.getMaxNumberBackups());
        assertEquals(AUTO_DELETE_ENABLED, housekeepingInformation.getAutoDelete());

        backupManager.getHousekeeping().setAutoDelete(AUTO_DELETE_DISABLED);
        backupManager.getHousekeeping().setMaxNumberBackups(2);

        fileService.writeToFile(backupManager.getHousekeeping());

        final String fileContents2 = Files.readAllLines(file).stream().collect(Collectors.joining(""));

        final HousekeepingInformation housekeepingInformation2 = new ObjectMapper().readValue(fileContents2, HousekeepingInformation.class);
        assertEquals(2, housekeepingInformation2.getMaxNumberBackups());
        assertEquals(AUTO_DELETE_DISABLED, housekeepingInformation2.getAutoDelete());
    }

    @Test
    public void getPersistedHouseKeepingInformation_validBackupManagerId_getsPersistedHouseKeepingInfo() throws Exception {
        final String jsonString = new JSONObject()
                .put("max-stored-manual-backups", 1)
                .put("auto-delete", AUTO_DELETE_ENABLED)
                .toString();
        createHousekeepingFile("123", jsonString);

        final HousekeepingInformation housekeepingInformation = fileService.getPersistedHousekeepingInformation("123");
        assertEquals(1, housekeepingInformation.getMaxNumberBackups());
        assertEquals(AUTO_DELETE_ENABLED, housekeepingInformation.getAutoDelete());
    }

    @Test(expected=FilePersistenceException.class)
    public void getPersistedHouseKeepingInformation_invalidBackupManagerId_getsPersistedHouseKeepingInfo() throws Exception {
        fileService.getPersistedHousekeepingInformation("456");
    }

    private void createHousekeepingFile(final String id, final String content) throws IOException {
        final Path backupManagerFolder = fileLocation.resolve(id);
        final Path housekeepingFile = backupManagerFolder.resolve("housekeepingInformation.json");

        Files.createDirectories(backupManagerFolder);
        Files.write(housekeepingFile, content.getBytes());
    }

    @Test
    public void repairDamagedHouseKeepingFile_newHouseKeeping() throws IOException {
        final String jsonString = "INVALID_DATA";
        createHousekeepingFile("123", jsonString);
        final Housekeeping housekeeping = new Housekeeping("123", hkinfo -> {
            assertEquals(AUTO_DELETE_ENABLED, hkinfo.getAutoDelete());
            assertEquals(1, hkinfo.getMaxNumberBackups());
        });
        expect(managerRepo.createHousekeeping(anyString())).andReturn(housekeeping).anyTimes();
        replay (managerRepo);
        fileService.getPersistedHousekeepingInformation("123");
    }
}
