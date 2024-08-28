/*------------------------------------------------------------------------------
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
package com.ericsson.adp.mgmt.backupandrestore.archive;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.Deflater;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.springframework.core.env.Environment;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.exception.ExportException;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProvider;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumHash64;
import com.ericsson.adp.mgmt.backupandrestore.util.CustomTarArchiveInputStream;


public class StreamingArchiveServiceTest {
    private static final String byte_875 = "875 B";
    private static final String onekbyte = "1.0 kB";
    private static final String mbyte_1 = "1.0 MB";
    private static final String mbyte_3_9 = "3.9 MB";
    private static final String gbyte_4_3 = "4.3 GB";
    private static final String tbyte_7 = "7.0 TB";
    private StreamingArchiveService archiveService;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setup() {
        ArchiveUtils utilsMock = EasyMock.createMock(ArchiveUtils.class);
        final PersistProvider provider = new PersistProviderFactory().getPersistProvider();
        expect(utilsMock.getProvider()).andReturn(provider).anyTimes();
        archiveService = new StreamingArchiveService(utilsMock);
        replay(utilsMock);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void deleteFile_null_noException(){
        final ArchiveUtils underTest = new ArchiveUtils();
        underTest.setProvider(new PersistProviderFactory());
        underTest.deleteFile(null);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void cleanUpDirectory_path_valid() throws IOException {
        final List<File> created = new ArrayList<>();
        tempFolder.create();
        created.add(tempFolder.newFolder("grandparent", "parent", "child"));
        created.add(tempFolder.newFile("1.txt"));
        created.add(tempFolder.newFile("2.txt"));

        final ArchiveUtils underTest = new ArchiveUtils();
        underTest.setProvider(new PersistProviderFactory());
        underTest.deleteFile(Path.of(tempFolder.getRoot().getAbsolutePath()));
        underTest.deleteFile(Paths.get("invalid"));
        assertFalse(created.stream().anyMatch(File::exists));
        tempFolder.delete();
    }

    @Test
    public void unpackTarStream_tarArchiveStream_Null() throws IOException {
        CustomTarArchiveInputStream tarArchiveInputStream = createMock(CustomTarArchiveInputStream.class);
        final Path dirBackupData = Paths.get("BackupData");
        final Path dirBackupFile = Paths.get("BackupFile");
        expect(tarArchiveInputStream.getNextTarEntry()).andReturn(null);
        tarArchiveInputStream.readRemainingBytes();
        expectLastCall();
        replay(tarArchiveInputStream);
        List<Path> list = archiveService.unpackTarStream(tarArchiveInputStream, dirBackupData, dirBackupFile, (b) -> {});
        assertTrue(list.isEmpty());
    }

    @Test
    public void unpackTarStream_tarArchiveStream_valid() throws IOException {
        TemporaryFolder temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();
        final Path dirBackupData = temporaryFolder.newFile("BackupData").toPath();
        final Path dirBackupFile = temporaryFolder.newFolder("BackupFile").toPath();

        CustomTarArchiveInputStream tarArchiveInputStream = createMock(CustomTarArchiveInputStream.class);

        TarArchiveEntry tarArchiveEntry = createMock(TarArchiveEntry.class);
        expect(tarArchiveInputStream.getNextTarEntry()).andReturn(tarArchiveEntry).times(2);
        expect(tarArchiveInputStream.getNextTarEntry()).andReturn(null);
        tarArchiveInputStream.readRemainingBytes();
        expectLastCall();
        expect(tarArchiveEntry.getName()).andReturn("backupfile");
        expect(tarArchiveEntry.getName()).andReturn("backupdata").anyTimes();
        expect(tarArchiveEntry.isDirectory()).andReturn(true).anyTimes();
        replay(tarArchiveInputStream, tarArchiveEntry);
        archiveService.unpackTarStream(tarArchiveInputStream, dirBackupData, dirBackupFile, (b) -> {});
        verify(tarArchiveInputStream, tarArchiveEntry);
        temporaryFolder.delete();
    }

    @Test(expected = ExportException.class)
    public void openTarGzipOutput_throwsException() throws IOException {
        final MockedStatic<SpringContext> mockedSpringContext;

        mockedSpringContext = mockStatic(SpringContext.class);

        when(SpringContext.getBean(Environment.class)).thenReturn(Optional.empty());
        final OutputStream outputStream = createMock(OutputStream.class);
        final ChecksumHash64 hash64 = createMock(ChecksumHash64.class);
        replay(outputStream, hash64);
        archiveService.openTarGzipOutput(outputStream, hash64);
    }

    @Test
    public void getCompressionLevelValue() {
        assertEquals(Deflater.NO_COMPRESSION, archiveService.getCompressionLevelValue("no_compression"));
        assertEquals(Deflater.BEST_COMPRESSION, archiveService.getCompressionLevelValue("best_compression "));
        assertEquals(Deflater.DEFAULT_COMPRESSION, archiveService.getCompressionLevelValue("DEFAULT_COMPRESSION"));
        assertEquals(Deflater.BEST_SPEED, archiveService.getCompressionLevelValue(" best_speed"));
        assertThrows(ExportException.class, () -> archiveService.getCompressionLevelValue("UNKNOWN_COMPRESSION"));
    }
}
