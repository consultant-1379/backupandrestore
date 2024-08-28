package com.ericsson.adp.mgmt.backupandrestore.persist;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
public class PVCPersistProviderTest {

    public static final String BACKUP_FILE_DUMMY_NAME = "dummy.txt";

    private PersistProvider pvcPersistProvider;

    @Rule
    public TemporaryFolder tempDir;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() throws IOException {
        pvcPersistProvider = new PVCPersistProvider();
        tempDir= new TemporaryFolder();
        tempDir.create();
    }

    @Test
    public void write_dummy_Filecreated() throws IOException {
        Path dummyLocation = tempDir.getRoot().toPath();
        pvcPersistProvider.setReservedSpace(dummyLocation);
        pvcPersistProvider.createDummyFile(1);
        assertTrue(Files.exists(dummyLocation.resolve(BACKUP_FILE_DUMMY_NAME)));
        pvcPersistProvider.deleteDummyFile();
        assertFalse(Files.exists(dummyLocation.resolve(BACKUP_FILE_DUMMY_NAME)));
    }

    @Test
    public void write_dummy_exception() {
        Path mockPath = createMock(Path.class);
        Path dummyFile = createMock(Path.class);
        // expectedException.expect(FilePersistenceException.class);
        File filetmp = createMock(File.class);
        expect (filetmp.exists()).andReturn(false);
        expect(mockPath.resolve(anyString())).andReturn(dummyFile);
        expect (dummyFile.toFile()).andReturn(filetmp);
        expect (dummyFile.getParent()).andReturn(null);
        replay (mockPath, dummyFile, filetmp);
        pvcPersistProvider.setReservedSpace(mockPath);
        pvcPersistProvider.createDummyFile(1);
    }
}
