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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.Fragment.Builder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class BackupMetadataWriterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private BackupMetadataWriter builder;
    private FragmentFolder fragmentFolder;

    @Before
    public void setUp() {
        builder = new BackupMetadataWriter();
        builder.setJsonService(new JsonService());
        fragmentFolder = createMock(FragmentFolder.class);
    }

    @Test
    public void storeFragment_metadataMessage_fragmentIsWritten() throws Exception {
        expect(fragmentFolder.getMetadataFile()).andReturn(folder.getRoot().toPath().resolve("Fragment.json"));
        expect(fragmentFolder.getRootFolder()).andReturn(folder.getRoot().toPath());
        replay(fragmentFolder);

        final Path fragmentLocation = folder.getRoot().toPath();
        builder.storeFragment(fragmentFolder, getMetadata(false));
        assertEquals(Arrays.asList("{\"fragmentId\":\"fragment\",\"version\":\"version\",\"sizeInBytes\":\"sizeInByte\",\"customInformation\":{}}"), Files.readAllLines(fragmentLocation.resolve("Fragment.json")));
    }

    @Test
    public void storeFragment_metadataMessageWithCustomInformation_fragmentIsWritten() throws Exception {
        expect(fragmentFolder.getMetadataFile()).andReturn(folder.getRoot().toPath().resolve("Fragment.json"));
        expect(fragmentFolder.getRootFolder()).andReturn(folder.getRoot().toPath());
        replay(fragmentFolder);

        final Path fragmentLocation = folder.getRoot().toPath();
        builder.storeFragment(fragmentFolder, getMetadata(true));
        assertEquals(Arrays.asList("{\"fragmentId\":\"fragment\",\"version\":\"version\",\"sizeInBytes\":\"sizeInByte\",\"customInformation\":{\"a\":\"2\"}}"), Files.readAllLines(fragmentLocation.resolve("Fragment.json")));
    }

    private Metadata getMetadata(final boolean hasCustomInformation) {
        final Builder fragmentBuilder = Fragment
                .newBuilder()
                .setFragmentId("fragment")
                .setSizeInBytes("sizeInByte")
                .setVersion("version");

        if(hasCustomInformation) {
            fragmentBuilder.putCustomInformation("a", "2");
        }

        final Fragment fragment = fragmentBuilder
                .build();
        return Metadata.newBuilder().setFragment(fragment).build();
    }

}
