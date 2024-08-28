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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler;

import static com.ericsson.adp.mgmt.backupandrestore.util.YangEnabledDisabled.DISABLED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.exception.FilePersistenceException;
import com.ericsson.adp.mgmt.backupandrestore.ssl.EncryptionService;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SchedulerFileServiceTest {
    private static final String TEST_AUTOEXPORT_PASSWORD = "testAutoExportPassword";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path fileLocation;
    private SchedulerFileService fileService;
    private final String nextScheduledTime = OffsetDateTime.now(ZoneId.systemDefault()).plusSeconds(2).toString();
    private BackupManagerRepository managerRepo;

    @Before
    public void setup() throws Exception {
        fileLocation = Paths.get(folder.getRoot().getAbsolutePath());
        managerRepo = createMock(BackupManagerRepository.class);
        fileService = new SchedulerFileService();
        fileService.setJsonService(new JsonService());
        fileService.setBackupManagersLocation(fileLocation.toAbsolutePath().toString());
        fileService.setEncryptor(new EncryptionService());
        fileService.setManagerRepository(managerRepo);
    }

    @Test
    public void writeToFile_scheduler_persistSchedulerInfo() throws Exception {
        final Scheduler scheduler = new Scheduler("123", null);
        scheduler.setAutoExportPassword(TEST_AUTOEXPORT_PASSWORD);
        scheduler.setNextScheduledTime(nextScheduledTime);

        fileService.writeToFile(scheduler);

        final Path file = fileLocation.resolve("123").resolve("schedulerInformation.json");
        assertTrue(file.toFile().exists());

        final String fileContents = Files.readAllLines(file).stream().collect(Collectors.joining(""));

        final SchedulerInformation schedulerInformation = new ObjectMapper().readValue(fileContents, SchedulerInformation.class);
        assertEquals(AdminState.UNLOCKED, schedulerInformation.getAdminState());
        assertEquals(SchedulerConstants.DEFAULT_SCHEDULED_BACKUP_NAME.toString(), schedulerInformation.getScheduledBackupName());
        assertNull(null, schedulerInformation.getMostRecentlyCreatedAutoBackup());
        assertEquals(nextScheduledTime, schedulerInformation.getNextScheduledTime());
        assertEquals(DISABLED, schedulerInformation.getAutoExport());
        assertNull(schedulerInformation.getAutoExportUri());

        // assert the test password isn't written to disc
        assertFalse(fileContents.contains(TEST_AUTOEXPORT_PASSWORD));
    }

    @Test
    public void getPersistedSchedulerInformation_validBackupManagerId_getsPersistedSchedulerInfo() throws Exception {
        final String jsonString = new JSONObject()
                .put("adminState", "UNLOCKED")
                .put("scheduledBackupName", "SCHEDULED_BACKUP")
                .put("mostRecentlyCreatedAutoBackup", "")
                .put("nextScheduledTime", nextScheduledTime)
                .toString();
        createSchedulerFile("123", jsonString);

        final SchedulerInformation schedulerInformation = fileService.getPersistedSchedulerInformation("123");
        assertEquals(AdminState.UNLOCKED, schedulerInformation.getAdminState());
        assertEquals(SchedulerConstants.DEFAULT_SCHEDULED_BACKUP_NAME.toString(), schedulerInformation.getScheduledBackupName());
        assertEquals("", schedulerInformation.getMostRecentlyCreatedAutoBackup());
        assertEquals(nextScheduledTime, schedulerInformation.getNextScheduledTime());
        assertEquals(DISABLED, schedulerInformation.getAutoExport());
        assertEquals("", schedulerInformation.getAutoExportPassword());
        assertNull(schedulerInformation.getAutoExportUri());

    }

    @Test(expected=FilePersistenceException.class)
    public void getPersistedSchedulerInformation_invalidBackupManagerId_filePersistenceException() throws Exception {
        fileService.getPersistedSchedulerInformation("456");
    }

    private void createSchedulerFile(final String id, final String content) throws IOException {
        final Path backupManagerFolder = fileLocation.resolve(id);
        final Path schedulerFile = backupManagerFolder.resolve("schedulerInformation.json");

        Files.createDirectories(backupManagerFolder);
        Files.write(schedulerFile, content.getBytes());
    }

    @Test
    public void repairDamagedSchedulerFile_newScheduler() throws IOException {
        final String jsonString = "INVALID_DATA";
        createSchedulerFile("123", jsonString);
        final Scheduler scheduler = new Scheduler("123",  schedulerInformation -> {
            assertEquals(AdminState.UNLOCKED, schedulerInformation.getAdminState());
            assertEquals(SchedulerConstants.DEFAULT_SCHEDULED_BACKUP_NAME.toString(), schedulerInformation.getScheduledBackupName());
            assertEquals(DISABLED, schedulerInformation.getAutoExport());
            assertEquals("", schedulerInformation.getAutoExportPassword());
            assertNull(schedulerInformation.getAutoExportUri());
        });
        expect(managerRepo.createAndPersistNewScheduler(anyString())).andReturn(scheduler).anyTimes();
        replay (managerRepo);
        fileService.getPersistedSchedulerInformation("123");
    }

}
