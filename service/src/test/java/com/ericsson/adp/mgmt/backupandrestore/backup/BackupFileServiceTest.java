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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.backupandrestore.exception.DeleteBackupException;
import com.ericsson.adp.mgmt.backupandrestore.exception.JsonParsingException;
import com.ericsson.adp.mgmt.backupandrestore.util.BackupFileValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BackupFileServiceTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private BackupFileService fileService;
    private Path fileLocation;

    @Before
    public void setup() {
        fileLocation = Paths.get(folder.getRoot().getAbsolutePath());

        fileService = new BackupFileService();
        fileService.setJsonService(new JsonService());
        fileService.setBackupManagersLocation(fileLocation.toString());
    }

    @Test
    public void writeToFile_backupThatWasAlreadyWritten_writesAgainOverridingOldInformation() throws Exception {
        final String backupManagerId = "qpwoei";
        final Backup backup = getBackup("bnm", backupManagerId);

        fileService.writeToFile(backup);

        backup.setUserLabel("Updated_USER_LABEL");
        backup.setStatus(BackupStatus.CORRUPTED);

        fileService.writeToFile(backup);

        final Path file = fileLocation.resolve(backupManagerId).resolve("backups").resolve(backup.getBackupId() + ".json");
        assertTrue(file.toFile().exists());

        final String fileContents = Files.readAllLines(file).stream().collect(Collectors.joining(""));

        final PersistedBackup persistedBackup = new ObjectMapper().readValue(fileContents, PersistedBackup.class);
        assertEquals(backup.getBackupId(), persistedBackup.getBackupId());
        assertEquals(backup.getName(), persistedBackup.getName());
        assertEquals(DateTimeUtils.convertToString(backup.getCreationTime().toLocalDateTime()), persistedBackup.getCreationTime());
        assertEquals(backup.getCreationType(), persistedBackup.getCreationType());
        assertEquals(backup.getStatus(), persistedBackup.getStatus());
        assertEquals(backup.getUserLabel(), persistedBackup.getUserLabel());
        assertEquals(2, persistedBackup.getSoftwareVersions().size());
    }

    @Test
    public void getBackups_backupManagerIdAndPersistedFiles_backupsWithInformationReadFromFile() throws Exception {
        final String backupManagerId = "qwe";
        final Backup backup = getBackup("fgh", backupManagerId);
        fileService.writeToFile(backup);
        fileService.writeToFile(getBackup("rtd", backupManagerId));

        final List<PersistedBackup> backups = fileService.getBackups(backupManagerId);

        assertEquals(2, backups.size());

        final PersistedBackup obtainedBackup = backups.stream().filter(persistedBackup -> backup.getBackupId().equals(persistedBackup.getBackupId()))
                .findFirst().get();
        assertEquals(backup.getBackupId(), obtainedBackup.getBackupId());
        assertEquals(backup.getName(), obtainedBackup.getName());
        assertEquals(DateTimeUtils.convertToString(backup.getCreationTime()), obtainedBackup.getCreationTime());
        assertEquals(backup.getCreationType(), obtainedBackup.getCreationType());
        assertEquals(BackupStatus.INCOMPLETE, obtainedBackup.getStatus());
        assertEquals(backup.getUserLabel(), obtainedBackup.getUserLabel());
        assertEquals(2, obtainedBackup.getSoftwareVersions().size());
    }

    @Test
    public void getBackups_backupManagerIdWithoutPersistedFiles_emptyList() throws Exception {
        final String backupManagerId = "qwe";

        Files.createDirectories(fileLocation.resolve(backupManagerId).resolve("backups"));

        final List<PersistedBackup> backups = fileService.getBackups(backupManagerId);

        assertTrue(backups.isEmpty());
    }

    @Test
    public void getBackups_validAndInvalidFiles_readsValidFilesAndReadsInvalidFilesAsIncomplete() throws Exception {
        final String backupManagerId = "z";
        final Backup backup = getBackup("x", backupManagerId);

        fileService.writeToFile(backup);
        Files.write(fileLocation.resolve(backupManagerId).resolve("backups").resolve("y.json"), "".getBytes());

        final List<PersistedBackup> backups = fileService.getBackups(backupManagerId);

        assertEquals(2, backups.size());

        assertEquals(BackupStatus.INCOMPLETE,backups.get(1).status);
    }

    @Test
    public void getBackups_backupManagerIdWithoutAnyFolder_emptyList() throws Exception {
        final String backupManagerId = "123";

        final List<PersistedBackup> backups = fileService.getBackups(backupManagerId);

        assertTrue(backups.isEmpty());
    }

    @Test
    public void deleteBackup_existingBackupManagerIdAndBackupId_deletesBackup() throws Exception {
        final String backupManagerId = "qpwoei";
        final Backup backup = getBackup("mznxbc", backupManagerId);

        fileService.writeToFile(backup);

        final Path file = fileLocation.resolve(backupManagerId).resolve("backups").resolve(backup.getBackupId() + ".json");
        assertTrue(file.toFile().exists());

        fileService.deleteBackup(backupManagerId, backup.getBackupId());

        assertFalse(file.toFile().exists());
    }

    @Test(expected = DeleteBackupException.class)
    public void deleteBackup_inexistingBackupManagerIdAndBackupId_throwsException() throws Exception {
        fileService.deleteBackup("1", "2");
    }

    @Test
    public void readBackup_backupFileContent_returnPersistedBackup() throws Exception {
        final OffsetDateTime creationTime = OffsetDateTime.now();
        final Backup backup = getBackup("backupId", "DEFAULT", creationTime);
        fileService.writeToFile(backup);

        final Path file = fileLocation.resolve("DEFAULT").resolve("backups").resolve("backupId.json");
        final String content = readAllBytes(file.toString());

        final BackupFileValidator backupFileValidator = createMock(BackupFileValidator.class);
        backupFileValidator.validateBackupFile(EasyMock.anyObject());
        expectLastCall();
        replay(backupFileValidator);

        fileService.setBackupFileValidator(backupFileValidator);
        final PersistedBackup persistedBackup = fileService.readBackup(content);

        assertEquals(backup.getBackupId(), persistedBackup.getBackupId());
        assertEquals(backup.getUserLabel(), persistedBackup.getUserLabel());
        assertEquals(DateTimeUtils.convertToString(backup.getCreationTime()), persistedBackup.getCreationTime());
        assertEquals(2, persistedBackup.getSoftwareVersions().size());
    }

    @Test(expected = JsonParsingException.class)
    public void readBackup_withBadJson_throwsException() throws Exception {
        final Path file = fileLocation.resolve("backupId.json");
        Files.write(file, "...............".getBytes());
        final String content = readAllBytes(file.toString());

        final BackupFileValidator backupFileValidator = new BackupFileValidator();
        fileService.setBackupFileValidator(backupFileValidator);

        fileService.readBackup(content);
    }

    private String readAllBytes(final String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    private Backup getBackup(final String id, final String managerId) {
        final Backup backup = new Backup(id, managerId, Optional.empty(), BackupCreationType.MANUAL, null);
        backup.setUserLabel("UserLabel");
        backup.addSoftwareVersion(new SoftwareVersion());
        backup.addSoftwareVersion(new SoftwareVersion());
        return backup;
    }

    private Backup getBackup(final String id, final String managerId, final OffsetDateTime creationTime) {
        final Backup backup = new Backup(id, managerId, Optional.of(creationTime), BackupCreationType.MANUAL, null);
        backup.setUserLabel("UserLabel");
        backup.addSoftwareVersion(new SoftwareVersion());
        backup.addSoftwareVersion(new SoftwareVersion());
        return backup;
    }

}
