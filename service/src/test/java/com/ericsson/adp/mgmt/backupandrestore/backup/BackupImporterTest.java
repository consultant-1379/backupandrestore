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

import static com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat.LEGACY;
import static com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat.TARBALL;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.archive.ArchiveUtils;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.exception.FileDirectoryException;
import com.ericsson.adp.mgmt.backupandrestore.exception.ImportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnexpectedBackupManagerException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientImportProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.HttpConnection;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.SftpConnection;
import com.ericsson.adp.mgmt.backupandrestore.util.BackupLimitValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.OSUtils;

public class BackupImporterTest {
    private final String REMOTE_PATH = "/remotepath";
    private final String BACKUP_DIR_PATH = "/backupdir";
    private final String BACKUP_FIL_DIR = "/backupfiledir";

    private BackupImporter backupImporter;
    private ExternalClientImportProperties externalClientImportProperties;
    private ExternalConnectionFactory externalConnectionFactory;
    private BackupManager backupManager;
    private BackupRepository backupRepository;
    private SftpConnection sftpConnection;
    private HttpConnection httpConnection;
    private BackupLimitValidator backupLimitValidator;
    private PropertyReceived propertyReceivedListener;


    @Before
    public void setup() {
        externalClientImportProperties = createMock(ExternalClientImportProperties.class);
        backupManager = createMock(BackupManager.class);
        externalConnectionFactory = createMock(ExternalConnectionFactory.class);
        backupRepository = createMock(BackupRepository.class);
        sftpConnection = createMock(SftpConnection.class);
        httpConnection = createMock(HttpConnection.class);
        backupLimitValidator = createMock(BackupLimitValidator.class);
        backupImporter = new BackupImporter();
        backupImporter.setExternalConnectionFactory(externalConnectionFactory);
        backupImporter.setBackupRepository(backupRepository);
        backupImporter.setBackupLimitValidator(backupLimitValidator);
        propertyReceivedListener = new PropertyReceived();
    }

    @Test
    public void importBackup_validInput_valid_legacy() throws Exception {
        URI uri = new URI("http://remotepath");
        expect(externalClientImportProperties.getImportFormat()).andReturn(LEGACY).times(3);
        expect(externalClientImportProperties.getUri()).andReturn(uri).times(2);
        expect(externalClientImportProperties.isUsingHttpUriScheme()).andReturn(false);
        expect(externalClientImportProperties.getExternalClientPath()).andReturn(REMOTE_PATH).anyTimes();
        expect(externalClientImportProperties.getUri()).andReturn(new URI("sftp://demo.com/backupId")).anyTimes();
        expect(sftpConnection.isDirectoryExists(REMOTE_PATH)).andReturn(true);
        expect(externalConnectionFactory.connect(externalClientImportProperties)).andReturn(sftpConnection).times(2);
        expect(sftpConnection.getBackupFileContent(REMOTE_PATH, LEGACY)).andReturn("mybackup");
        expect(sftpConnection.getMatchingBackUpTarballNames(REMOTE_PATH)).andReturn(new ArrayList<>());
        final Backup backup = createMock(Backup.class);
        backupLimitValidator.validateLimit(EasyMock.anyString());
        expectLastCall();
        expect(backupManager.getBackupManagerId()).andReturn("123").anyTimes();

        expect(backupRepository.importBackup("mybackup", backupManager)).andReturn(backup);
        expect(backup.getBackupManagerId()).andReturn("123").anyTimes();

        expect(backup.getBackupId()).andReturn("backupId");

        expect(externalClientImportProperties.getFolderToStoreBackupData()).andReturn(Paths.get(BACKUP_DIR_PATH));
        expect(externalClientImportProperties.getFolderToStoreBackupFile()).andReturn(Paths.get(BACKUP_FIL_DIR));

        sftpConnection.importBackupData(REMOTE_PATH, Paths.get(BACKUP_DIR_PATH), LEGACY);
        expectLastCall();
        
        sftpConnection.close();
        expectLastCall();

        replay(sftpConnection, externalConnectionFactory, externalClientImportProperties, backup, backupManager, backupRepository, backupLimitValidator);

        backupImporter.importBackup(externalClientImportProperties, backupManager, propertyReceivedListener);

        verify(sftpConnection, backup, backupManager);
    }

    @Test
    public void importBackup_valid_legacy_singleTarball() throws Exception {
//        assumeFalse("Skipping the test on Windows OS due to URI argument formatting issue.", OSUtils.isWindows());
        final String sourcePath = REMOTE_PATH + "/backupdata";
        final List<String> tarballNames = new ArrayList<>();
        final String tarballName = "backupdata-2020-10-21T10-01-20.5828125.tar.gz";
        final String localPath = BACKUP_FIL_DIR +"/backupdata.json";
        tarballNames.add(tarballName);

        expect(externalConnectionFactory.connect(externalClientImportProperties)).andReturn(sftpConnection);
        backupLimitValidator.validateLimit(EasyMock.anyString());
        expectLastCall();
        expect(backupManager.getBackupManagerId()).andReturn("123");

        expect(externalClientImportProperties.getImportFormat()).andReturn(LEGACY).times(1);
        expect(externalClientImportProperties.isUsingHttpUriScheme()).andReturn(false);
        expect(externalClientImportProperties.getExternalClientPath()).andReturn(sourcePath).anyTimes();
        expect(externalClientImportProperties.getUri()).andReturn(new URI("sftp://demo.com/backupId")).anyTimes();
        expect(sftpConnection.getMatchingBackUpTarballNames(sourcePath)).andReturn(tarballNames);
        expect(sftpConnection.isDirectoryExists(sourcePath)).andReturn(false);
        externalClientImportProperties.setImportFormat(TARBALL);
        expectLastCall();
        backupManager.assertBackupIsNotPresent(tarballName);
        expectLastCall();

        expect(externalClientImportProperties.getPassword()).andReturn("password");
        expect(externalClientImportProperties.getFolderToStoreBackupData()).andReturn(Paths.get(BACKUP_DIR_PATH)).times(2);
        expect(externalClientImportProperties.getFolderToStoreBackupFile()).andReturn(Paths.get(BACKUP_FIL_DIR));
        expect(sftpConnection.downloadBackupFile(isA(ExternalClientImportProperties.class), isA(PropertyReceived.class))).andReturn(localPath);

        expect(sftpConnection.getBackupFileContent(localPath, TARBALL)).andReturn(localPath);
        expect(externalClientImportProperties.getImportFormat()).andReturn(TARBALL).times(2);
        sftpConnection.importBackupData(localPath, Paths.get(BACKUP_DIR_PATH), TARBALL);
        expectLastCall();

        final Backup backup = createMock(Backup.class);
        expect(backup.getBackupId()).andReturn("backupdata");
        expect(backupRepository.importBackup(localPath, backupManager)).andReturn(backup);

        sftpConnection.close();
        expectLastCall();

        replay(sftpConnection, externalConnectionFactory, externalClientImportProperties, backup, backupManager, backupRepository, backupLimitValidator);

        assertEquals(backup, backupImporter.importBackup(externalClientImportProperties, backupManager, propertyReceivedListener));

        verify(sftpConnection, backup, backupManager);
    }

    @Test(expected = ImportException.class)
    public void importBackup_invalid_legacy_multipleTarballs_throwException() throws Exception {
        final List<String> tarballNames = new ArrayList<>();
        tarballNames.add("backupdata-2020-10-21T10-01-20.5828125+01-00.tar.gz");
        tarballNames.add("backupdata-2021-10-21T10-01-20.5828125+01-00.tar.gz");

        expect(externalConnectionFactory.connect(externalClientImportProperties)).andReturn(sftpConnection);
        backupLimitValidator.validateLimit(EasyMock.anyString());
        expectLastCall();
        expect(backupManager.getBackupManagerId()).andReturn("123");
        expect(externalClientImportProperties.getImportFormat()).andReturn(LEGACY).times(1);
        expect(externalClientImportProperties.isUsingHttpUriScheme()).andReturn(false);
        expect(externalClientImportProperties.getExternalClientPath()).andReturn(REMOTE_PATH).anyTimes();
        expect(externalClientImportProperties.getUri()).andReturn(new URI("sftp://demo.com/backupId")).anyTimes();

        expect(sftpConnection.getMatchingBackUpTarballNames(REMOTE_PATH)).andReturn(tarballNames);
        expect(sftpConnection.isDirectoryExists(REMOTE_PATH)).andReturn(false);

        replay(sftpConnection, externalConnectionFactory, externalClientImportProperties, backupManager, backupRepository, backupLimitValidator);

        backupImporter.importBackup(externalClientImportProperties, backupManager, propertyReceivedListener);

        verify(sftpConnection, backupLimitValidator, backupManager, externalClientImportProperties);
    }

    @Test(expected = ImportException.class)
    public void importBackup_invalid_legacy_noTarballFound_throwException() throws Exception {
        final List<String> tarballNames = new ArrayList<>();

        expect(externalConnectionFactory.connect(externalClientImportProperties)).andReturn(sftpConnection);
        backupLimitValidator.validateLimit(EasyMock.anyString());
        expectLastCall();
        expect(backupManager.getBackupManagerId()).andReturn("123");
        expect(externalClientImportProperties.getImportFormat()).andReturn(LEGACY).times(1);
        expect(externalClientImportProperties.isUsingHttpUriScheme()).andReturn(false);
        expect(externalClientImportProperties.getExternalClientPath()).andReturn(REMOTE_PATH).anyTimes();
        expect(externalClientImportProperties.getUri()).andReturn(new URI("sftp://demo.com/backupId")).anyTimes();

        expect(sftpConnection.getMatchingBackUpTarballNames(REMOTE_PATH)).andReturn(tarballNames);
        expect(sftpConnection.isDirectoryExists(REMOTE_PATH)).andReturn(false);

        replay(sftpConnection, externalConnectionFactory, externalClientImportProperties, backupManager, backupRepository, backupLimitValidator);

        backupImporter.importBackup(externalClientImportProperties, backupManager, propertyReceivedListener);

        verify(sftpConnection, backupLimitValidator, backupManager, externalClientImportProperties);
    }

    @Test
    public void importBackup_validInput_valid_tarball() throws Exception {
        Optional<String> backupid= Optional.of("backupId");
        URI uri = new URI("http://remotepath/backup1-2020-10-21T10-01-20.5828125+01-00.tar.gz");
        final String localPath = "/tmp/backup1-2020-10-21T10-01-20.5828125+01-00.tar.gz";
        backupManager.assertBackupIsNotPresent("backup1-2020-10-21T10-01-20.5828125+01-00.tar.gz");
        expectLastCall();

        expect(externalClientImportProperties.getImportFormat()).andReturn(TARBALL).times(3);
        expect(externalClientImportProperties.isUsingHttpUriScheme()).andReturn(false);
        expect(externalClientImportProperties.getUri()).andReturn(uri);
        expect(externalClientImportProperties.getExternalClientPath()).andReturn("backup1-2020-10-21T10-01-20.5828125+01-00.tar.gz").anyTimes();
        expect(sftpConnection.downloadBackupFile(externalClientImportProperties, propertyReceivedListener)).andReturn(localPath);
        expect(externalConnectionFactory.connect(externalClientImportProperties)).andReturn(sftpConnection).times(2);
        expect(sftpConnection.getBackupFileContent(localPath, TARBALL))
                .andReturn(localPath);

        final Backup backup = createMock(Backup.class);

        expect(backupManager.getBackupManagerId()).andReturn("123").anyTimes();
        expect(backupManager.getBackupID(EasyMock.anyString())).andReturn(backupid).anyTimes();

        expect(backupRepository.importBackup(localPath, backupManager)).andReturn(backup);
        expect(backup.getBackupManagerId()).andReturn("123").anyTimes();
        
        backupLimitValidator.validateLimit(EasyMock.anyString());
        expectLastCall();
        
        expect(backup.getBackupId()).andReturn("backupId");

        expect(externalClientImportProperties.getFolderToStoreBackupData()).andReturn(Paths.get(BACKUP_DIR_PATH));
        expect(externalClientImportProperties.getFolderToStoreBackupFile()).andReturn(Paths.get(BACKUP_FIL_DIR));

        sftpConnection.importBackupData(localPath, Paths.get(BACKUP_DIR_PATH), TARBALL);
        expectLastCall();
        
        sftpConnection.close();
        expectLastCall().anyTimes();

        replay(sftpConnection, externalConnectionFactory, externalClientImportProperties, backup, backupManager, backupRepository, backupLimitValidator);

        backupImporter.importBackup(externalClientImportProperties, backupManager, propertyReceivedListener);

        verify(sftpConnection, backup, backupManager);
    }

    @Test(expected = ImportException.class)
    public void importBackup_sftpServerIssueWhileAccessingForBackupFile_throwException() throws URISyntaxException {
        expect(externalClientImportProperties.getImportFormat()).andReturn(LEGACY).times(2);
        expect(externalClientImportProperties.getExternalClientPath()).andReturn(REMOTE_PATH).anyTimes();
        expect(externalClientImportProperties.isUsingHttpUriScheme()).andReturn(false);
        expect(sftpConnection.getMatchingBackUpTarballNames(REMOTE_PATH)).andReturn(new ArrayList<>());
        expect(sftpConnection.isDirectoryExists(REMOTE_PATH)).andReturn(true);
        expect(externalConnectionFactory.connect(externalClientImportProperties)).andReturn(sftpConnection);
        expect(externalClientImportProperties.getUri()).andReturn(new URI("sftp://demo.com/backupId")).anyTimes();

        backupLimitValidator.validateLimit(EasyMock.anyString());
        expectLastCall();
        expect(sftpConnection.getBackupFileContent(REMOTE_PATH, LEGACY)).andThrow(new ImportException("Failed while importing backup in BRO"));

        replay(sftpConnection, externalConnectionFactory, externalClientImportProperties, backupRepository, backupLimitValidator);

        backupImporter.importBackup(externalClientImportProperties, backupManager, propertyReceivedListener);
    }

    @Test(expected = UnexpectedBackupManagerException.class)
    public void importBackup_UnexpectedBackupManager_throwException() throws URISyntaxException {
        Optional<String> backupid= Optional.of("backupId");

        expect(externalClientImportProperties.getImportFormat()).andReturn(LEGACY).times(2);
        expect(externalClientImportProperties.getExternalClientPath()).andReturn(REMOTE_PATH).anyTimes();
        expect(externalClientImportProperties.isUsingHttpUriScheme()).andReturn(false);
        expect(sftpConnection.getMatchingBackUpTarballNames(REMOTE_PATH)).andReturn(new ArrayList<>());
        expect(sftpConnection.isDirectoryExists(REMOTE_PATH)).andReturn(true);
        expect(externalConnectionFactory.connect(externalClientImportProperties)).andReturn(sftpConnection);
        backupLimitValidator.validateLimit(EasyMock.anyString());
        expectLastCall();
        expect(sftpConnection.getBackupFileContent(REMOTE_PATH, LEGACY)).andReturn("Some String");
        expect(backupRepository.importBackup("Some String", backupManager)).andThrow(new UnexpectedBackupManagerException("1","2","3"));

        expect(externalClientImportProperties.getFolderToStoreBackupData()).andReturn(Path.of("somePath"));
        expect(externalClientImportProperties.getFolderToStoreBackupFile()).andReturn(Path.of("somePath"));
        expect(externalClientImportProperties.getUri()).andReturn(new URI("sftp://demo.com/backupId")).anyTimes();

        expect(backupManager.getBackupID(EasyMock.anyString())).andReturn(backupid).anyTimes();
        expect(backupManager.getBackupManagerId()).andReturn("123").anyTimes();

        ArchiveUtils archiveUtils = createMock(ArchiveUtils.class);
        archiveUtils.deleteFile(anyObject());
        expectLastCall().anyTimes();
        backupImporter.setArchiveUtils(archiveUtils);

        replay(sftpConnection, externalConnectionFactory, externalClientImportProperties, backupRepository, backupLimitValidator, archiveUtils, backupManager);

        backupImporter.importBackup(externalClientImportProperties, backupManager, propertyReceivedListener);
    }

    @Test(expected = FileDirectoryException.class)
    public void importBackup_FileDirectoryException_throwException() throws URISyntaxException {
        Optional<String> backupid= Optional.of("backupId");

        expect(externalClientImportProperties.getImportFormat()).andReturn(LEGACY).times(2);
        expect(externalClientImportProperties.getExternalClientPath()).andReturn(REMOTE_PATH).anyTimes();
        expect(externalClientImportProperties.isUsingHttpUriScheme()).andReturn(false);
        expect(sftpConnection.getMatchingBackUpTarballNames(REMOTE_PATH)).andReturn(new ArrayList<>());
        expect(sftpConnection.isDirectoryExists(REMOTE_PATH)).andReturn(true);
        expect(externalConnectionFactory.connect(externalClientImportProperties)).andReturn(sftpConnection);
        backupLimitValidator.validateLimit(EasyMock.anyString());
        expectLastCall();
        expect(sftpConnection.getBackupFileContent(REMOTE_PATH, LEGACY)).andReturn("Some String");
        expect(backupRepository.importBackup("Some String", backupManager)).andThrow(new FileDirectoryException("1"));

        expect(externalClientImportProperties.getFolderToStoreBackupData()).andReturn(Path.of("somePath"));
        expect(externalClientImportProperties.getFolderToStoreBackupFile()).andReturn(Path.of("somePath"));
        expect(externalClientImportProperties.getUri()).andReturn(new URI("sftp://demo.com/backupId")).anyTimes();

        expect(backupManager.getBackupID(EasyMock.anyString())).andReturn(backupid).anyTimes();
        expect(backupManager.getBackupManagerId()).andReturn("123").anyTimes();

        ArchiveUtils archiveUtils = createMock(ArchiveUtils.class);
        archiveUtils.deleteFile(anyObject());
        expectLastCall().anyTimes();
        backupImporter.setArchiveUtils(archiveUtils);

        replay(sftpConnection, externalConnectionFactory, externalClientImportProperties, backupRepository, backupLimitValidator, archiveUtils, backupManager);

        backupImporter.importBackup(externalClientImportProperties, backupManager, propertyReceivedListener);
    }

    @Test
    public void importBackup_http_validInput_valid() throws Exception {
        Optional<String> backupid= Optional.of("backupId");

        URI uri = new URI("http://remotepath");

        backupManager.assertBackupIsNotPresent(REMOTE_PATH);
        expectLastCall();
        expect(externalClientImportProperties.getImportFormat()).andReturn(TARBALL).times(3);
        expect(externalClientImportProperties.isUsingHttpUriScheme()).andReturn(true);
        expect(externalClientImportProperties.getUri()).andReturn(uri).times(2);
        backupLimitValidator.validateLimit(EasyMock.anyString());
        expectLastCall();
        expect(externalConnectionFactory.connect(externalClientImportProperties)).andReturn(httpConnection).times(2);
        expect(externalClientImportProperties.getExternalClientPath()).andReturn(REMOTE_PATH).anyTimes();
        expect(httpConnection.downloadBackupFile(externalClientImportProperties, propertyReceivedListener)).andReturn(REMOTE_PATH).anyTimes();
        expect(httpConnection.getBackupFileContent(REMOTE_PATH, TARBALL)).andReturn("mybackup").anyTimes();

        final Backup backup = createMock(Backup.class);

        expect(backupManager.getBackupManagerId()).andReturn("123").anyTimes();
        expect(backupManager.getBackupID(EasyMock.anyString())).andReturn(backupid).anyTimes();

        expect(backupRepository.importBackup("mybackup", backupManager)).andReturn(backup);
        expect(backup.getBackupManagerId()).andReturn("123").anyTimes();

        expect(backup.getBackupId()).andReturn("backupId");

        expect(externalClientImportProperties.getFolderToStoreBackupData()).andReturn(Paths.get(BACKUP_DIR_PATH));
        expect(externalClientImportProperties.getFolderToStoreBackupFile()).andReturn(Paths.get(BACKUP_FIL_DIR));

        httpConnection.importBackupData(REMOTE_PATH, Paths.get(BACKUP_DIR_PATH), TARBALL);
        expectLastCall();

        httpConnection.close();
        expectLastCall().anyTimes();

        replay(httpConnection, externalConnectionFactory, externalClientImportProperties, backup, backupManager, backupRepository, backupLimitValidator);

        backupImporter.importBackup(externalClientImportProperties, backupManager, propertyReceivedListener);

        verify(httpConnection, backup, backupManager);
    }

    class PropertyReceived implements PropertyChangeListener {
        private String property="";
        private String oldValue;
        private String newValue;

        public String getNewValue() {
            return newValue;
        }

        public String getOldValue() {
            return oldValue;
        }

        public String getProperty() {
            return property;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            property = evt.getPropertyName();
            oldValue = String.valueOf(evt.getOldValue());
            newValue = String.valueOf(evt.getNewValue());
        }
    }
}
