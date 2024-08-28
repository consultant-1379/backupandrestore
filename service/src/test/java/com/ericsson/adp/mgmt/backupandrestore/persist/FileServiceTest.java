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
package com.ericsson.adp.mgmt.backupandrestore.persist;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyInt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.backup.storage.StorageMetadata;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;

public class FileServiceTest {
    private PVCPersistProvider provider;
    private PersistProviderFactory providerFactory;

    @Before
    public void setup() {
        provider = createMock(PVCPersistProvider.class);
        providerFactory = createMock(PersistProviderFactory.class);
    }

    @Test
    public void readContentOfFile_File_valid() throws IOException {
        String content = "abc";
        File tempFile = File.createTempFile("pre", "suf");
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(content.getBytes());
        fos.close();
        String s = new FileService<StorageString>() {
            @Override
            protected List<Version<StorageString>> getVersions() {
                return List.of(new Version<>(p -> p, null, d -> d, null, 0));
            }
        }.readContentOfFile(tempFile.toPath());
        assertEquals("abc", s);
    }

    @Test
    public void getBRMName_test() {
        final String brmName = new FileService<StorageString>() {

            @Override
            protected List<Version<StorageString>> getVersions() {
                    return List.of(new Version<>(p -> p, null, d -> d, null, 0));
            }
        }.getBackupManagerId(Path.of("test", "brm"));
        assertEquals("brm", brmName);
    }

    @Test
    public void getFakePathSize_returnZero() {
        assertEquals(0, new FileService<StorageString>() {
            @Override
            protected List<Version<StorageString>> getVersions() {
                    return List.of(new Version<>(p -> p, null, d -> d, null, 0));
            }
        }.getFolderSize(Path.of("some/definitely/fake/path")));
    }

    @Test
    public void createDummyFile_created() {
        FileService fileService = new FileService<StorageString>() {
            @Override
            protected List<Version<StorageString>> getVersions() {
                    return List.of(new Version<>(p -> p, null, d -> d, null, 0));
            }
        };
        expect(providerFactory.getPersistProvider()).andReturn(provider);
        provider.createDummyFile(anyInt());
        expectLastCall();
        replay(providerFactory, provider);
        fileService.setProvider(providerFactory);
        fileService.createDummyFile();
    }

    @Test
    public void deleteDummyFile_created() throws Exception {
        FileService fileService = new FileService<StorageString>() {
            @Override
            protected List<Version<StorageString>> getVersions() {
                    return List.of(new Version<>(p -> p, null, d -> d, null, 0));
            }
        };
        expect(providerFactory.getPersistProvider()).andReturn(provider);
        provider.deleteDummyFile();
        expectLastCall();
        replay(providerFactory, provider);
        fileService.setProvider(providerFactory);
        fileService.deleteDummyFile();
    }
    
    class StorageString implements Versioned<StorageString> {

        private String version;
        private Version<StorageString> persistVersion;
        
        @Override
        public Version<StorageString> getVersion() {
            return persistVersion;
        }

        @Override
        public void setVersion(Version<StorageString> version) {
            this.persistVersion = version;
        }
        
    }
}
