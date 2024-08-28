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
package com.ericsson.adp.mgmt.backupandrestore.restore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.metadata.Fragment;

public class FragmentFileServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private FragmentFileService fileService;

    @Before
    public void setup() throws Exception {
        this.fileService = new FragmentFileService();
        this.fileService.setJsonService(new JsonService());
        this.fileService.setDataLocation(this.folder.getRoot().getAbsolutePath());

        createBackupFolder("backup_test", "agent1");
    }

    @Test
    public void getBackupFragments_returnsFragmentsBelongingToAgentId() throws Exception {
        final List<Fragment> fragments = fileService.getFragments("brm", "backup_test", "agent1");

        assertEquals(2, fragments.size());

        final Fragment firstFragment = fragments.stream().filter(fragment -> "Fragment1".equals(fragment.getFragmentId())).findFirst().get();
        assertEquals("Fragment1", firstFragment.getFragmentId());
        assertEquals("version1", firstFragment.getVersion());
        assertEquals("bytes1", firstFragment.getSizeInBytes());
        assertEquals("3", firstFragment.getCustomInformationMap().get("b"));

        final Fragment secondFragment = fragments.stream().filter(fragment -> "Fragment2".equals(fragment.getFragmentId())).findFirst().get();
        assertEquals("Fragment2", secondFragment.getFragmentId());
        assertEquals("version2", secondFragment.getVersion());
        assertEquals("bytes2", secondFragment.getSizeInBytes());
        assertTrue(secondFragment.getCustomInformationMap().isEmpty());
    }

    @Test
    public void getBackupFragments_invalidAgentDetailsSent_returnsEmptyFragmentList() throws Exception {
        final List<Fragment> fragments = fileService.getFragments("brm", "backup_test", "invalidAgentId");

        assertTrue(fragments.isEmpty());
    }

    @Test
    public void getBackupFragments_invalidBackupDetailsSent_returnsEmptyFragmentList() throws Exception {
        final List<Fragment> fragments = fileService.getFragments("brm", "invalidBackup", "invalidAgentId");

        assertTrue(fragments.isEmpty());
    }

    private void createBackupFolder(final String backupName, final String agentId) throws IOException {
        final Path path = this.folder.getRoot().toPath().resolve("brm").resolve(backupName).resolve(agentId);

        Files.createDirectories(path.resolve("Fragment1"));
        Files.createDirectories(path.resolve("Fragment2"));
        Files.createDirectories(path.resolve("Fragment3"));

        Files.write(path.resolve("Fragment1").resolve("Fragment.json"),
                "{\"fragmentId\":\"Fragment1\",\"version\":\"version1\",\"sizeInBytes\":\"bytes1\",\"customInformation\":{\"b\":\"3\"}}".getBytes());
        Files.write(path.resolve("Fragment2").resolve("Fragment.json"),
                "{\"fragmentId\":\"Fragment2\",\"version\":\"version2\",\"sizeInBytes\":\"bytes2\"}".getBytes());
        Files.write(path.resolve("Fragment3").resolve("Fragment.json"),
                "{\"fragmentId\":\"Fragment2\",}".getBytes());
    }

}
