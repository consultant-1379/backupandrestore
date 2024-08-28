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
package com.ericsson.adp.mgmt.brotestagent.agent.behavior;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.bro.api.fragment.BackupFragmentInformation;
import com.ericsson.adp.mgmt.brotestagent.util.PropertiesHelper;

public class LargeBackupFileAgentBehaviorTest {

    private static final Path ROOT_LOCATION = Paths.get(System.getProperty("java.io.tmpdir"), "large-backup-files");
    private static final Path BACKUP_FILE = ROOT_LOCATION.resolve("backup.txt");
    private static final Path CUSTOM_METADATA_FILE = ROOT_LOCATION.resolve("customMetadata.txt");
    private static final String BACKUP_TYPE = "DEFAULT";

    private LargeBackupFileAgentBehavior largeBackupFileAgentBehavior;

    @Before
    public void setUp() {
        PropertiesHelper.loadProperties("src/test/resources/application.properties");

        this.largeBackupFileAgentBehavior = new LargeBackupFileAgentBehavior();
    }

    @After
    public void teardown() throws Exception {
        Files.deleteIfExists(BACKUP_FILE);
        Files.deleteIfExists(CUSTOM_METADATA_FILE);
    }

    @Test
    public void doSomethingToCreateBackup_backupSizeAndNoCustomMetadata_createsBackupFileWithSpecifiedSizeAndReturnsFragmentInformation() throws Exception {
        setFileSize(5, 0);

        final List<BackupFragmentInformation> fragments = largeBackupFileAgentBehavior.doSomethingToCreateBackup(BACKUP_TYPE);

        assertEquals(1, fragments.size());

        final BackupFragmentInformation backupFragmentInformation = fragments.get(0);
        assertNotNull(backupFragmentInformation.getFragmentId());
        assertEquals("aaaaaaaaaas", backupFragmentInformation.getVersion());
        assertEquals(String.valueOf(5 * 1024 * 1024), backupFragmentInformation.getSizeInBytes());
        assertEquals(BACKUP_FILE.toString(), backupFragmentInformation.getBackupFilePath());
        assertFalse(backupFragmentInformation.getCustomMetadataFilePath().isPresent());

        assertTrue(BACKUP_FILE.toFile().exists());
        assertFalse(CUSTOM_METADATA_FILE.toFile().exists());
    }

    @Test
    public void doSomethingToCreateBackup_backupSizeAndCustomMetadataSize_createsBackupFileAndCustomMetadataFileAndReturnsFragmentInformation() throws Exception {
        setFileSize(10, 2);

        final List<BackupFragmentInformation> fragments = largeBackupFileAgentBehavior.doSomethingToCreateBackup(BACKUP_TYPE);

        assertEquals(1, fragments.size());

        final BackupFragmentInformation backupFragmentInformation = fragments.get(0);
        assertNotNull(backupFragmentInformation.getFragmentId());
        assertEquals("aaaaaaaaaas", backupFragmentInformation.getVersion());
        assertEquals(String.valueOf(10 * 1024 * 1024), backupFragmentInformation.getSizeInBytes());
        assertEquals(BACKUP_FILE.toString(), backupFragmentInformation.getBackupFilePath());
        assertTrue(backupFragmentInformation.getCustomMetadataFilePath().isPresent());
        assertEquals(CUSTOM_METADATA_FILE.toString(), backupFragmentInformation.getCustomMetadataFilePath().get());

        assertTrue(BACKUP_FILE.toFile().exists());
        assertTrue(CUSTOM_METADATA_FILE.toFile().exists());
    }

    @Test
    public void doSomethingToCreateBackup_noBackupSize_returnsEmptyFragmentList() throws Exception {
        setFileSize(0, 5);

        final List<BackupFragmentInformation> fragments = largeBackupFileAgentBehavior.doSomethingToCreateBackup(BACKUP_TYPE);

        assertTrue(fragments.isEmpty());
        assertFalse(BACKUP_FILE.toFile().exists());
        assertFalse(CUSTOM_METADATA_FILE.toFile().exists());
    }

    private void setFileSize(final int backupFileSize, final int customMetadataFileSize) {
        PropertiesHelper.setProperty("large.backup.file.agent.backup.size", String.valueOf(backupFileSize));
        PropertiesHelper.setProperty("large.backup.file.agent.custom.metadata.size", String.valueOf(customMetadataFileSize));
    }
}
