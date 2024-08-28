/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
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

public class ManyFragmentsBehaviorTest {
    private static final Path ROOT_LOCATION = Paths.get(System.getProperty("java.io.tmpdir"), "many-backup-files");
    private static final String BACKUP_TYPE = "DEFAULT";

    private ManyFragmentsBehavior manyFragmentsBehavior;

    @Before
    public void setUp() {
        PropertiesHelper.loadProperties("src/test/resources/application.properties");
        PropertiesHelper.setProperty("many.fragment.agent.fragment.count", "10");

        this.manyFragmentsBehavior = new ManyFragmentsBehavior();
    }

    @After
    public void teardown() throws Exception {
    }

    @Test
    public void doSomethingToCreateBackup_backupSizeAndNoCustomMetadata_createsBackupFileWithSpecifiedSizeAndReturnsFragmentInformation() throws Exception {
        final List<BackupFragmentInformation> fragments = manyFragmentsBehavior.doSomethingToCreateBackup(BACKUP_TYPE);

        assertEquals(10, fragments.size());

        for (int i = 0; i < fragments.size(); i++) {
            final BackupFragmentInformation backupFragmentInformation = fragments.get(i);
            assertNotNull(backupFragmentInformation.getFragmentId());
            assertEquals("aaaaaaaaaas", backupFragmentInformation.getVersion());
            assertEquals(String.valueOf(32), backupFragmentInformation.getSizeInBytes());
            assertEquals(ROOT_LOCATION.resolve(i + ".data").toString(), backupFragmentInformation.getBackupFilePath());
            assertFalse(backupFragmentInformation.getCustomMetadataFilePath().isPresent());
            assertTrue(Path.of(backupFragmentInformation.getBackupFilePath()).toFile().exists());
            Files.deleteIfExists(Path.of(backupFragmentInformation.getBackupFilePath()));
            assertFalse(Path.of(backupFragmentInformation.getBackupFilePath()).toFile().exists());
        }
    }
}
