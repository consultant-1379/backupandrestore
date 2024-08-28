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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.partialMockBuilder;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.VirtualInformation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BackupManagerFileServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path backupManagersDir;
    private BackupManagerFileService fileService;

    @Before
    public void setup() throws Exception {
        String root = folder.getRoot().getAbsolutePath();
        backupManagersDir = Paths.get(root, "backupManagers");

        fileService = new BackupManagerFileService();
        fileService.setJsonService(new JsonService());
        fileService.setBackupManagersLocation(backupManagersDir.toAbsolutePath().toString());
    }

    @Test
    public void writeToFile_changeBackupManagerAndPersistItAgain_persistBackupManagerWithNewInformation() throws Exception {
        final PersistedBackupManager persistedBackupManager = new PersistedBackupManager();
        persistedBackupManager.setBackupManagerId("456");
        final BackupManager backupManager = new BackupManager(
                persistedBackupManager,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                null,
                null,
                fileService,
                null,
                new VirtualInformation("", new ArrayList<>()));
        backupManager.setBackupDomain("qwe");
        backupManager.setBackupType("abc");

        fileService.writeToFile(backupManager);

        backupManager.setBackupType("poi");

        fileService.writeToFile(backupManager);

        final Path file = backupManagersDir.resolve("456").resolve("backupManagerInformation.json");
        assertTrue(file.toFile().exists());

        final String fileContents = Files.readAllLines(file).stream().collect(Collectors.joining(""));

        final PersistedBackupManager persistedManager = new ObjectMapper().readValue(fileContents, PersistedBackupManager.class);
        assertEquals("456", persistedManager.getBackupManagerId());
        assertEquals("qwe", persistedManager.getBackupDomain());
        assertEquals("poi", persistedManager.getBackupType());
    }

    @Test
    public void getBackupManagers_noPersistedBackupManagers_emptyList() throws Exception {
        assertTrue(fileService.getBackupManagers().isEmpty());
    }

    @Test
    public void getPersistedBackupManager_newBackupManagerId_returnsEmptyOptional() throws Exception {
        createBackupManagerFile("1", "{\"backupType\":\"b\",\"backupDomain\":\"a\",\"id\":\"default\"}");
        final List<PersistedBackupManager> backupManagers = fileService.getBackupManagers();
        assertEquals(1, backupManagers.size());
        assertEquals("default", backupManagers.get(0).getBackupManagerId());

        final Optional<PersistedBackupManager> persistedBackupManagers = fileService.getPersistedBackupManager("DEFAULT");
        assertFalse(persistedBackupManagers.isPresent());
    }

    @Test
    public void getPersistedBackupManager_persistedBackupManagerId_returnsPersistedBackupManager() throws Exception {
        createBackupManagerFile("1", "{\"backupType\":\"b\",\"backupDomain\":\"a\",\"id\":\"default\"}");
        final List<PersistedBackupManager> backupManagers = fileService.getBackupManagers();
        assertEquals(1, backupManagers.size());
        assertEquals("default", backupManagers.get(0).getBackupManagerId());

        final Optional<PersistedBackupManager> persistedBackupManagers = fileService.getPersistedBackupManager("default");
        assertTrue(persistedBackupManagers.isPresent());
        assertEquals("default", persistedBackupManagers.get().getBackupManagerId());
    }


    @Test
    public void getBackupManagers_persistedBackupManagers_listWithAllPersistedBackupManagers() throws Exception {
        createBackupManagerFile("1", "{\"backupType\":\"b\",\"backupDomain\":\"a\",\"id\":\"1\"}");
        createBackupManagerFile("2", "{\"backupType\":\"d\",\"backupDomain\":\"c\",\"id\":\"2\"}");
        createBackupManagerFile("3", "{\"backupType\":\"d\",}");

        final List<PersistedBackupManager> backupManagers = fileService.getBackupManagers();

        assertEquals(2, backupManagers.size());

        final PersistedBackupManager firstBackupManager = backupManagers.stream().filter(manager -> "1".equals(manager.getBackupManagerId())).findFirst()
                .get();
        assertEquals("1", firstBackupManager.getBackupManagerId());
        assertEquals("a", firstBackupManager.getBackupDomain());
        assertEquals("b", firstBackupManager.getBackupType());

        final PersistedBackupManager secondBackupManager = backupManagers.stream().filter(manager -> "2".equals(manager.getBackupManagerId())).findFirst()
                .get();
        assertEquals("2", secondBackupManager.getBackupManagerId());
        assertEquals("c", secondBackupManager.getBackupDomain());
        assertEquals("d", secondBackupManager.getBackupType());
    }

    @Test
    public void getBackupManagerBackupsFolderSize_noBackupsForBackupManager_sizeZero() {
        final BackupLocationService backupLocationService = new BackupLocationService();
        backupLocationService.setBackupLocation("111");
        backupLocationService.setProvider(new PersistProviderFactory()); // Just use the PVC provider here
        fileService.setBackupLocationService(backupLocationService);
        final long backupFolderSize = fileService.getBackupManagerBackupsFolderSize("111");
        assertEquals(0, backupFolderSize);
    }

    @Test
    public void getBackupManagerBackupsFolderSize_backupsForBackupManagerExists_size100() throws IOException {

        final BackupLocationService mockedBackupLocationService = createMock(BackupLocationService.class);
        expect(mockedBackupLocationService.backupManagerLocationExists("112")).andReturn(true);
        expect(mockedBackupLocationService.getBackupManagerLocation("112")).andReturn(backupManagersDir);
        replay(mockedBackupLocationService);

        final BackupManagerFileService partiallyMockedBackupManagerFileService = partialMockBuilder(BackupManagerFileService.class)
                  .addMockedMethod("getFolderSize").createMock();
        expect(partiallyMockedBackupManagerFileService.getFolderSize(backupManagersDir)).andReturn((long) 100);
        replay(partiallyMockedBackupManagerFileService);
        partiallyMockedBackupManagerFileService.setBackupLocationService(mockedBackupLocationService);

        final long backupFolderSize = partiallyMockedBackupManagerFileService.getBackupManagerBackupsFolderSize("112");
        assertEquals(100, backupFolderSize);
        verify(partiallyMockedBackupManagerFileService);
    }

    @Test
    public void getBackupManagerBackupsFolderSize_backupsForBackupManagerExists_size50() throws IOException {

        createBackupManagerFile("111", "{\"backupType\":\"aa\",\"backupDomain\":\"bb\",\"id\":\"111\"}");

        final BackupLocationService backupLocationService = new BackupLocationService();
        backupLocationService.setBackupLocation(backupManagersDir.toString());
        backupLocationService.setProvider(new PersistProviderFactory()); // Just use the PVC persist provider
        fileService.setBackupLocationService(backupLocationService);

        final long backupFolderSize = fileService.getBackupManagerBackupsFolderSize("111");
        assertEquals(50, backupFolderSize);
    }

    @Test
    public void deleteBackupManager() throws IOException {
        createBackupManagerFile("DEFAULT-bravo", "{\"backupType\":\"aa\",\"backupDomain\":\"bb\",\"id\":\"DEFAULT-bravo\"}");
        Path backupManagerPath = backupManagersDir.resolve("DEFAULT-bravo");
        assertTrue(backupManagerPath.toFile().exists());
        fileService.delete("DEFAULT-bravo");
        assertFalse(backupManagerPath.toFile().exists());
    }

    @Test(expected = Test.None.class)
    public void deleteBackupManager_notExisting() throws IOException {
        Path backupManagerPath = backupManagersDir.resolve("DEFAULT-bravo");
        assertFalse(backupManagerPath.toFile().exists());
        fileService.delete("DEFAULT-bravo");
    }

    @Test
    public void deleteBackupManagerBackups() throws IOException {
        Path backupsDir = Paths.get(folder.getRoot().getAbsolutePath(), "backups", "DEFAULT-bravo");
        Files.createDirectories(backupsDir);
        assertTrue(backupsDir.toFile().exists());

        final BackupLocationService mockedBackupLocationService = createMock(BackupLocationService.class);
        expect(mockedBackupLocationService.backupManagerLocationExists("DEFAULT-bravo")).andReturn(true);
        expect(mockedBackupLocationService.getBackupManagerLocation("DEFAULT-bravo")).andReturn(backupsDir);
        replay(mockedBackupLocationService);
        fileService.setBackupLocationService(mockedBackupLocationService);

        fileService.deleteBackups("DEFAULT-bravo");
        assertFalse(backupsDir.toFile().exists());
    }

    private void createBackupManagerFile(final String id, final String content) throws IOException {
        final Path backupManagerFolder = backupManagersDir.resolve(id);
        final Path backupManagerFile = backupManagerFolder.resolve("backupManagerInformation.json");

        Files.createDirectories(backupManagerFolder);
        Files.write(backupManagerFile, content.getBytes());
    }

}
