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
package com.ericsson.adp.mgmt.brotestagent.agent;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.bro.api.fragment.BackupFragmentInformation;
import com.ericsson.adp.mgmt.brotestagent.util.PropertiesHelper;

public class FragmentFactoryTest {

    private FragmentFactory fragmentFactory;
    private List<BackupFragmentInformation> fragmentList = new ArrayList<>();

    @Before
    public void setUp() throws IOException {
        PropertiesHelper.loadProperties("src/test/resources/application.properties");
        fragmentFactory = new FragmentFactory("a");
    }

    @Test
    public void getFragmentList_propertiesFile_backupFragmentInformationList() throws IOException {
        final String version = "0.0.0";

        fragmentList = new ArrayList<>(fragmentFactory.getFragmentList());

        assertEquals("a_1", fragmentList.get(0).getFragmentId());
        assertEquals("a_2", fragmentList.get(1).getFragmentId());
        assertEquals("a_3", fragmentList.get(2).getFragmentId());

        assertEquals(version, fragmentList.get(0).getVersion());
        assertEquals(version, fragmentList.get(1).getVersion());
        assertEquals(version, fragmentList.get(2).getVersion());

        assertEquals("./src/test/resources/backup.txt", fragmentList.get(0).getBackupFilePath());
        assertEquals("./src/test/resources/CustomMetadata.txt", fragmentList.get(1).getBackupFilePath());
        assertEquals("./src/test/resources/backup2.txt", fragmentList.get(2).getBackupFilePath());

        assertEquals(Optional.of("./src/test/resources/CustomMetadata.txt"), fragmentList.get(0).getCustomMetadataFilePath());
        assertEquals(Optional.empty(), fragmentList.get(1).getCustomMetadataFilePath());
        assertEquals(Optional.of("./src/test/resources/CustomMetadataDownload.txt"), fragmentList.get(2).getCustomMetadataFilePath());

        assertEquals(Long.toString(Files.size(Paths.get("./src/test/resources/backup.txt"))), fragmentList.get(0).getSizeInBytes());
        assertEquals(Long.toString(Files.size(Paths.get("./src/test/resources/CustomMetadata.txt"))), fragmentList.get(1).getSizeInBytes());
        assertEquals(Long.toString(Files.size(Paths.get("./src/test/resources/backup2.txt"))), fragmentList.get(2).getSizeInBytes());
    }

}
