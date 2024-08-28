/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.external.connection;

import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType.MANUAL;
import static com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat.LEGACY;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.BACKUP_CONTENT;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.BACKUP_DATA_FOLDER_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.BACKUP_FILE_FOLDER_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.BACKUP_FILE_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.BACKUP_ID;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.BACKUP_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.DATA_CONTENT;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.DATA_FILE_PATH;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.FRAGMENT_CONTENT;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.FRAGMENT_FILE_PATH;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.SEP;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.createBackupFolderWithContent;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.createCompressedFile;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.createEmptyBackupFolder;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.createUnCompressedFile;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.getBackupFile;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.stream.Stream;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.archive.ArchiveUtils;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.exception.ExportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.ImportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.ImportExportException;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import com.ericsson.adp.mgmt.backupandrestore.util.OSUtils;

public class HttpConnectionTest {
    private HttpConnection connection;
    private RestTemplate restTemplate;
    private ResponseEntity responseEntity;
    private HttpHeaders httpHeaders;
    private HttpClientErrorException httpClientErrorException;
    private File backupFile;
    private File backupDataFolder;
    private File backupFileEmptyFolder;
    private File backupDataEmptyFolder;
    private File compressed;
    private File uncompressedFolder;
    private URI uri;
    private Backup backup;
    private ArchiveUtils archiveUtils;

    @Rule
    public TemporaryFolder folderForBackup = new TemporaryFolder();
    @Rule
    public TemporaryFolder folderForEmptyBackup = new TemporaryFolder();
    @Rule
    public TemporaryFolder folderForCompressedFile = new TemporaryFolder();
    @Rule
    public TemporaryFolder folderForUnCompressedFile = new TemporaryFolder();

    @Before
    public void setup() throws URISyntaxException {
        restTemplate = createMock(RestTemplate.class);
        responseEntity = createMock(ResponseEntity.class);
        httpHeaders = createMock(HttpHeaders.class);
        httpClientErrorException = new HttpClientErrorException(HttpStatus.valueOf(HttpStatus.NOT_FOUND.value()));
        uri = new URI("http://localhost");
        // Using a "real" utils here as this was previously using a "real" ArchiveService so I assume file operations are expected
        archiveUtils = new ArchiveUtils();
        archiveUtils.setProvider(new PersistProviderFactory());
        backup = createMock(Backup.class);
        connection = new HttpConnection(restTemplate, archiveUtils);
    }

    @Test
    public void exportBackup_ok() throws Exception {
        // Export backupfile from /bro/backupManagers/DEFAULT/backups/
        // Export backupdata from /bro/backups/DEFAULT/mybackup
        assumeFalse("Skipping the test on Windows OS.", OSUtils.isWindows());
        backupDataFolder = createBackupFolderWithContent(folderForBackup);
        backupFile = getBackupFile();
        expect(restTemplate.postForEntity(EasyMock.eq(uri.toString()), EasyMock.anyObject(HttpEntity.class), EasyMock.eq(String.class)))
        .andReturn(responseEntity);
        expect(backup.getCreationType()).andReturn(MANUAL);
        expect(responseEntity.getStatusCode()).andReturn(HttpStatus.valueOf(HttpStatus.OK.value()));
        expect(backup.getCreationTime()).andReturn(OffsetDateTime.now());
        expect(backup.getName()).andReturn(BACKUP_NAME).anyTimes();
        replay(restTemplate, responseEntity, backup);
        connection.exportBackup(backupFile.toPath(), backupDataFolder.toPath(), uri.toString(), BACKUP_NAME, BACKUP_ID, backup);
        verify(restTemplate, responseEntity, backup);
    }

    @Test(expected = ExportException.class)
    public void exportBackup_throwsException() throws Exception {
        backupDataFolder = createBackupFolderWithContent(folderForBackup);
        backupFile = getBackupFile();
        expect(restTemplate.postForEntity(EasyMock.eq(uri.toString()), EasyMock.anyObject(HttpEntity.class), EasyMock.eq(String.class)))
        .andThrow(httpClientErrorException);
        expect(backup.getCreationType()).andReturn(MANUAL);
        expect(backup.getCreationTime()).andReturn(OffsetDateTime.now());
        expect(backup.getName()).andReturn(BACKUP_NAME).anyTimes();
        replay(restTemplate, backup);
        connection.exportBackup(backupFile.toPath(), backupDataFolder.toPath(), uri.toString(), BACKUP_NAME, BACKUP_ID, backup);
        verify(restTemplate, backup);
    }

    @Test
    public void downloadBackupFile_ok() throws Exception {
        compressed = createCompressedFile(true, folderForCompressedFile);
        expect(restTemplate.execute(EasyMock.eq(uri), EasyMock.eq(HttpMethod.GET), EasyMock.eq(null), EasyMock.anyObject()))
        .andReturn(compressed);
        replay(restTemplate, responseEntity, httpHeaders);
        final String backupPath = connection.downloadBackupFile(uri);
        verify(restTemplate, responseEntity, httpHeaders);
        final String dataFileName = backupPath + SEP + BACKUP_DATA_FOLDER_NAME + SEP + DATA_FILE_PATH;
        final File dataFile = new File(dataFileName);
        final String fragmentFileName = backupPath + SEP + BACKUP_DATA_FOLDER_NAME + SEP + FRAGMENT_FILE_PATH;
        final File fragmentFile = new File(fragmentFileName);
        final String backupFileName = backupPath + SEP + BACKUP_FILE_FOLDER_NAME + SEP + BACKUP_FILE_NAME;
        final File backupFile = new File(backupFileName);
        assertTrue(dataFile.exists());
        String content = new String(Files.readAllBytes(dataFile.toPath()), StandardCharsets.UTF_8);
        assertEquals(0, content.compareTo(DATA_CONTENT));
        assertTrue(fragmentFile.exists());
        content = new String(Files.readAllBytes(fragmentFile.toPath()), StandardCharsets.UTF_8);
        assertEquals(0, content.compareTo(FRAGMENT_CONTENT));
        assertTrue(backupFile.exists());
        content = new String(Files.readAllBytes(backupFile.toPath()), StandardCharsets.UTF_8);
        assertEquals(0, content.compareTo(BACKUP_CONTENT));
        // Delete downloaded file in testing
        try (Stream<Path> stream = Files.walk(Paths.get(backupPath.substring(0, backupPath.indexOf(BACKUP_ID))))) {
            stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @Test(expected = ImportException.class)
    public void downloadBackupFile_backupInvalid() throws Exception {
        compressed = createCompressedFile(false, folderForCompressedFile);
        expect(restTemplate.execute(EasyMock.eq(uri), EasyMock.eq(HttpMethod.GET), EasyMock.eq(null), EasyMock.anyObject()))
        .andReturn(compressed);
        ;
        replay(restTemplate, responseEntity, httpHeaders);
        connection.downloadBackupFile(uri);
        verify(restTemplate, responseEntity, httpHeaders);
    }

    @Test(expected = ImportException.class)
    public void downloadBackupFile_throwsException() {
        expect(restTemplate.execute(EasyMock.eq(uri), EasyMock.eq(HttpMethod.GET), EasyMock.eq(null), EasyMock.anyObject()))
        .andThrow(httpClientErrorException);
        replay(restTemplate);
        connection.downloadBackupFile(uri);
        verify(restTemplate);
    }

    @Test
    public void getBackupFileContent_ok() throws IOException {
        uncompressedFolder = createUnCompressedFile(folderForUnCompressedFile);
        final String content = connection.getBackupFileContent(uncompressedFolder.toString(), LEGACY);
        assertEquals(0, content.compareTo(BACKUP_CONTENT));
    }

    @Test(expected = ImportException.class)
    public void getBackupFileContent_throwsException() throws IOException {
        backupFileEmptyFolder = createEmptyBackupFolder(folderForEmptyBackup);
        connection.getBackupFileContent(backupFileEmptyFolder.getParent(), LEGACY);
    }

    @Test
    public void importBackupData_ok() throws IOException {
        // Download and import for coverage
        compressed = createCompressedFile(true, folderForCompressedFile);
        backupDataEmptyFolder = createEmptyBackupFolder(folderForEmptyBackup);
        expect(restTemplate.execute(EasyMock.eq(uri), EasyMock.eq(HttpMethod.GET), EasyMock.eq(null), EasyMock.anyObject()))
        .andReturn(compressed);
        replay(restTemplate, responseEntity, httpHeaders);
        final String localBackupPath = connection.downloadBackupFile(uri);
        verify(restTemplate, responseEntity, httpHeaders);
        final String dataFileName = backupDataEmptyFolder.getAbsolutePath() + SEP + DATA_FILE_PATH;
        final File dataFile = new File(dataFileName);
        final String fragmentFileName = backupDataEmptyFolder.getAbsolutePath() + SEP + FRAGMENT_FILE_PATH;
        final File fragmentFile = new File(fragmentFileName);
        // Import backup to /bro/backups/DEFAULT/mybackup
        connection.importBackupData(localBackupPath, backupDataEmptyFolder.toPath(), LEGACY);
        assertTrue(dataFile.exists());
        String content = new String(Files.readAllBytes(dataFile.toPath()), StandardCharsets.UTF_8);
        assertEquals(0, content.compareTo(DATA_CONTENT));
        assertTrue(fragmentFile.exists());
        content = new String(Files.readAllBytes(fragmentFile.toPath()), StandardCharsets.UTF_8);
        assertEquals(0, content.compareTo(FRAGMENT_CONTENT));
    }

    @Test(expected = ImportException.class)
    public void importBackupData_throwsException() throws ImportExportException {
        connection.importBackupData("/nonexistentpath", new File("/").toPath(), LEGACY);
    }

    @Test
    public void close_ok() {
        connection.close();
    }
}