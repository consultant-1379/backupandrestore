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
package com.ericsson.adp.mgmt.backupandrestore.external.connection;

import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType.MANUAL;
import static com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat.LEGACY;
import static com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat.TARBALL;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_FILE_FOLDER_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_MONITOR_TIMEOUT_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.BACKUP_CONTENT;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.createCompressedFile;
import static com.ericsson.adp.mgmt.backupandrestore.util.TemporaryFolderProperties.createUnCompressedFile;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.powermock.api.easymock.PowerMock.expectNew;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.awaitility.Awaitility;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.powermock.api.easymock.PowerMock;
import com.amazonaws.util.Base64;
import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.archive.ArchiveUtils;
import com.ericsson.adp.mgmt.backupandrestore.archive.StreamingArchiveService;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.ServerLocalDefinition;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;
import com.ericsson.adp.mgmt.backupandrestore.exception.ExportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.ImportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.ImportExportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidSftpServerHostKeyException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientImportProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.SftpConnection.ProgressMonitor;
import com.ericsson.adp.mgmt.backupandrestore.kms.CMKeyPassphraseService;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import com.ericsson.adp.mgmt.backupandrestore.rest.SftpServerTestConstant;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumHash64;
import com.ericsson.adp.mgmt.backupandrestore.util.OSUtils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

public class SftpConnectionTest {
    private static final int DIR_PERMISSION = Integer.parseInt("750",8);
    private static final int FILE_PERMISSION = Integer.parseInt("640",8);
    private static final String REMOTE_PATH = File.separator + "remotepath";
    private static final String LOCAL_PATH = File.separator + "localpath";
    private static final String BACKUP_FILE_FOLDER = REMOTE_PATH + File.separator + "backupfile";
    private static final String BACKUP_DATA_FOLDER = REMOTE_PATH + File.separator + "backupdata";
    private static final String BACKUP_FILE = BACKUP_FILE_FOLDER + File.separator + "backup.json";
    private ExternalClientImportProperties externalClientImportProperties;
    private ChannelSftp sftpClient;
    FileWriter fileWriter;
    private Session session;
    private SftpATTRS attrs;
    private SftpConnection sftpConnection;
    private Backup backup;
    private ExternalClientProperties externalProperties;
    private JSch jsch;
    private ArchiveUtils archiveUtils;
    private PropertyReceived propertyReceivedListener;

    enum AuthenticationType{
        PASSWORD, PUBLIC_KEY;
    }

    @Rule
    public TemporaryFolder folderForBackupFile = new TemporaryFolder();

    @Rule
    public TemporaryFolder folderForBackupData = new TemporaryFolder();

    @Rule
    public TemporaryFolder folderForUnCompressedFile = new TemporaryFolder();

    @Rule
    public TemporaryFolder folderForBothBackups = new TemporaryFolder();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setup() throws JSchException, URISyntaxException {
        externalClientImportProperties = createMock(ExternalClientImportProperties.class);
        expect(externalClientImportProperties.getFolderToStoreBackupData()).andReturn(folderForBackupData.getRoot().toPath()).anyTimes();
        expect(externalClientImportProperties.getFolderToStoreBackupFile()).andReturn(folderForBackupFile.getRoot().toPath()).anyTimes();
        expect(externalClientImportProperties.getUri()).andReturn(new URI("")).anyTimes();
        replay(externalClientImportProperties);
        propertyReceivedListener = new PropertyReceived();

        jsch = createMock(JSch.class);
        sftpClient = getChannel();
        session = getSession(true);
        attrs = createMock(SftpATTRS.class);
        externalProperties = populateExternalClientProperties(AuthenticationType.PASSWORD, null);
        archiveUtils = createMock(ArchiveUtils.class);
        final SftpChannelManager manager = new SftpChannelManager();
        manager.setTimeout(10);
        manager.setJsch(jsch);
        manager.setClientProperties(externalProperties);
        archiveUtils.deleteFile(anyObject());
        expectLastCall().anyTimes();
        expect(archiveUtils.getProvider()).andReturn(new PersistProviderFactory().getPersistProvider()).anyTimes();
        sftpConnection = Mockito.spy(new SftpConnection(manager, archiveUtils));

        backup = createMock(Backup.class);

    }

    // @Test
    public void exportBackup_validInput_valid_usingSftpServerName() throws SftpException, IOException, JSchException, URISyntaxException {
        assumeFalse("Skipping the test on Windows OS.", OSUtils.isWindows());
        archiveUtils = new ArchiveUtils(); // Use a real archive utils here since we actually want to simulate the packing
        archiveUtils.setProvider(new PersistProviderFactory());
        final SftpChannelManager manager = new SftpChannelManager();
        jsch.removeAllIdentity();
        expectLastCall().anyTimes();
        HostKeyRepository hostKeyRepository = createMock(HostKeyRepository.class);
        hostKeyRepository.remove(anyString(), anyObject());
        expectLastCall().anyTimes();
        hostKeyRepository.add(anyObject(), anyObject());
        expectLastCall().anyTimes();
        expect(jsch.getHostKeyRepository()).andReturn(hostKeyRepository).anyTimes();
        jsch.addIdentity(anyString(), anyObject(), anyObject(), anyObject());
        expectLastCall().anyTimes();
        manager.setJsch(jsch);
        manager.setTimeout(10);

        final CMKeyPassphraseService passphraseService = createMock(CMKeyPassphraseService.class);
        expect(passphraseService.isEnabled()).andReturn(true).anyTimes();
        expect(passphraseService.getPassphrase(anyString())).andReturn("decryptedPrivateKey").anyTimes();
        manager.setCmKeyPassphraseService(passphraseService);
        final SftpServer sftpServer = new SftpServer(SftpServerTestConstant.createSftpServerInformation(), "DEFAULT", null);
        externalProperties = populateExternalClientProperties(AuthenticationType.PUBLIC_KEY, sftpServer);
        manager.setClientProperties(externalProperties);
        sftpConnection = new SftpConnection(manager, archiveUtils);
        backup = createMock(Backup.class);

        exportBackup(true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        folderForBackupData.newFile("mybackupfile.txt");
        final Path backupDataFolderPath = folderForBackupData.getRoot().toPath();

        final File backupFolder = folderForBackupFile.newFile("mybackup.json");
        final Path backupFile = backupFolder.toPath();
        expectFilePermissionSetup(DIR_PERMISSION, 2);
        expectFilePermissionSetup(FILE_PERMISSION, 2);
        expect(sftpClient.stat(EasyMock.anyString())).andReturn(attrs).anyTimes();
        expect(sftpClient.lstat(EasyMock.anyString())).andThrow(new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "Backup doesn't exist"))
        .anyTimes();
        expect(sftpClient.isConnected()).andReturn(true).anyTimes();
        expect(attrs.isDir()).andReturn(false).anyTimes();

        sftpClient.put(anyObject(InputStream.class), EasyMock.anyString(), EasyMock.anyInt());
        expectLastCall().anyTimes();

        expect(sftpClient.put(EasyMock.anyString(), anyObject(ProgressMonitor.class),
                EasyMock.anyInt())).andReturn(baos);

        sftpClient.setExtOutputStream(anyObject(OutputStream.class));
        expectLastCall();
        sftpClient.setExtOutputStream(null, false);
        expectLastCall();

        replay(sftpClient, attrs, backup, jsch, hostKeyRepository, session, passphraseService);
        sftpConnection.connect();
        sftpConnection.exportBackup(backupFile, backupDataFolderPath, REMOTE_PATH, "backup1", "default", backup, propertyReceivedListener);
        verify(sftpClient, attrs, backup);
    }

    @Test(expected = InvalidSftpServerHostKeyException.class)
    public void exportBackup_validInput_valid_usingSftpServerName_invalidHostKey() throws SftpException, IOException, JSchException, URISyntaxException {
        final SftpChannelManager manager = new SftpChannelManager();
        jsch.removeAllIdentity();
        expectLastCall().anyTimes();
        HostKeyRepository hostKeyRepository = createMock(HostKeyRepository.class);
        hostKeyRepository.remove(anyString(), anyObject());
        expectLastCall().anyTimes();
        expect(jsch.getHostKeyRepository()).andReturn(hostKeyRepository).anyTimes();
        jsch.addIdentity(anyString(), anyObject(), anyObject(), anyObject());
        expectLastCall().anyTimes();
        manager.setJsch(jsch);
        manager.setTimeout(10);

        final CMKeyPassphraseService passphraseService = createMock(CMKeyPassphraseService.class);
        expect(passphraseService.isEnabled()).andReturn(true).anyTimes();
        expect(passphraseService.getPassphrase(anyString())).andReturn("decryptedPrivateKey").anyTimes();
        manager.setCmKeyPassphraseService(passphraseService);
        final SftpServer sftpServer = new SftpServer(SftpServerTestConstant.createSftpServerInformation(), "DEFAULT", null);
        ServerLocalDefinition serverKeys = (sftpServer.getEndpoints().getEndpoint()[0])
                .getServerAuthentication()
                .getSshHostKeys()
                .getLocalDefinition();
        final String invalidHostKey = Base64.encodeAsString("ssh-rsa".getBytes());
        serverKeys.replaceKey(0, invalidHostKey);
        externalProperties = populateExternalClientProperties(AuthenticationType.PUBLIC_KEY, sftpServer);
        manager.setClientProperties(externalProperties);
        sftpConnection = new SftpConnection(manager, createMock(ArchiveUtils.class));
        backup = createMock(Backup.class);

        exportBackup(true);
        replay(sftpClient, attrs, backup, jsch, hostKeyRepository, session, passphraseService);
        sftpConnection.connect();
        verify(sftpClient, attrs, backup);
    }

    // @Test
    public void exportBackup_validInput_channelClose_reconnect() throws Exception {
        assumeFalse("Skipping the test on Windows OS.", OSUtils.isWindows());
        archiveUtils = new ArchiveUtils(); // Use a real archive utils here since we actually want to simulate the packing
        archiveUtils.setProvider(new PersistProviderFactory());
        final SftpChannelManager manager = new SftpChannelManager();
        manager.setTimeout(10);
        manager.setJsch(jsch);
        manager.setClientProperties(externalProperties);
        sftpConnection = new SftpConnection(manager, archiveUtils);
        backup = createMock(Backup.class);

        fileWriter = mockFileWriter();
        expectNew(FileWriter.class, File.class).andReturn(fileWriter);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        folderForBackupData.newFile("mybackupfile.txt");
        final Path backupDataFolder = folderForBackupData.getRoot().toPath();
        final File backup = folderForBackupFile.newFile("mybackup.json");
        final Path backupFile = backup.toPath();
        exportBackup(true);
        expectFilePermissionSetup(DIR_PERMISSION, 2);
        expectFilePermissionSetup(FILE_PERMISSION, 2);
        expect(sftpClient.stat(EasyMock.anyString())).andReturn(attrs).anyTimes();
        expect(sftpClient.lstat(EasyMock.anyString())).andThrow(new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "Backup doesn't exist"))
        .anyTimes();
        expect(sftpClient.isConnected()).andReturn(false).anyTimes();
        expect(attrs.isDir()).andReturn(false).anyTimes();

        sftpClient.setExtOutputStream(anyObject(OutputStream.class));
        expectLastCall();
        sftpClient.setExtOutputStream(null, false);
        expectLastCall();

        sftpClient.put(anyObject(InputStream.class), EasyMock.anyString(), EasyMock.anyInt());
        expectLastCall().anyTimes();

        expect(sftpClient.put(EasyMock.anyString(), anyObject(ProgressMonitor.class),
                EasyMock.anyInt())).andReturn(baos).anyTimes();

        replay(sftpClient, attrs, this.backup, jsch, session, fileWriter);
        sftpConnection.connect();

        baos.write("Hello World".getBytes());
        sftpConnection.exportBackup(backupFile, backupDataFolder, REMOTE_PATH, "backup1", "default", this.backup, propertyReceivedListener);

        verify(sftpClient, attrs, this.backup, session);
    }

    // @Test
    public void exportBackup_validInput_valid() throws SftpException, IOException, JSchException {
        assumeFalse("Skipping the test on Windows OS.", OSUtils.isWindows());
        archiveUtils = new ArchiveUtils(); // Use a real archive utils here since we actually want to simulate the packing
        archiveUtils.setProvider(new PersistProviderFactory());
        final SftpChannelManager manager = new SftpChannelManager();
        manager.setTimeout(10);
        manager.setJsch(jsch);
        manager.setClientProperties(externalProperties);
        sftpConnection = new SftpConnection(manager, archiveUtils);
        backup = createMock(Backup.class);

        exportBackup(true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        folderForBackupData.newFile("mybackupfile.txt");
        final Path backupDataFolderPath = folderForBackupData.getRoot().toPath();

        final File backupFolder = folderForBackupFile.newFile("mybackup.json");
        final Path backupFile = backupFolder.toPath();
        expectFilePermissionSetup(DIR_PERMISSION, 2);
        expectFilePermissionSetup(FILE_PERMISSION, 2);
        expect(sftpClient.stat(EasyMock.anyString())).andReturn(attrs).anyTimes();
        expect(sftpClient.lstat(EasyMock.anyString())).andThrow(new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "Backup doesn't exist"))
        .anyTimes();
        expect(sftpClient.isConnected()).andReturn(true).anyTimes();
        expect(attrs.isDir()).andReturn(false).anyTimes();

        sftpClient.put(anyObject(InputStream.class), EasyMock.anyString(), EasyMock.anyInt());
        expectLastCall().anyTimes();

        expect(sftpClient.put(EasyMock.anyString(), anyObject(ProgressMonitor.class),
                EasyMock.anyInt())).andReturn(baos);

        sftpClient.setExtOutputStream(anyObject(OutputStream.class));
        expectLastCall();
        sftpClient.setExtOutputStream(null, false);
        expectLastCall();

        replay(sftpClient, attrs, backup, jsch, session);
        sftpConnection.connect();
        sftpConnection.exportBackup(backupFile, backupDataFolderPath, REMOTE_PATH, "backup1", "default", backup, propertyReceivedListener);
        verify(sftpClient, attrs, backup);
    }

    @Test
    public void exportBackup_remoteFileExist_throwsException() throws SftpException, JSchException {
        final String backupName = "backup1";

        exceptionRule.expect(ExportException.class);
        expect(sftpClient.lstat(anyString())).andReturn(attrs).anyTimes();
        expect(sftpClient.stat(EasyMock.anyString())).andReturn(attrs).anyTimes();
        expect(attrs.isDir()).andReturn(true).anyTimes();
        exportBackup(true);
        replay(sftpClient, attrs, jsch, backup, session);
        sftpConnection.connect();
        sftpConnection.exportBackup(Paths.get("/backupfilepath"), Paths.get("/backupdatapath"), "/remotepath",
                backupName, "default", backup, propertyReceivedListener);
    }

    @Test
    public void exportBackup_SessionClosed_throwsException() throws JSchException {
        final String CONNECTION_ERROR_MESSAGE = "Unable to connect to sftp service";
        exceptionRule.expect(ImportExportException.class);
        exceptionRule.expectMessage(CONNECTION_ERROR_MESSAGE);
        session = getSession(false);
        expect(jsch.getSession(externalProperties.getUser(), externalProperties.getHost(),
                externalProperties.getPort())).andReturn(session).anyTimes();
        replay(sftpClient, attrs, jsch, backup, session);
        sftpConnection.connect();

    }

    @Test(expected = ExportException.class)
    public void exportBackup_FileDoesntExist_BadMessage_throwsException() throws SftpException, JSchException {
        final String backupName = "backup1";
        exportBackup(true);
        expect(sftpClient.lstat(anyString())).andThrow(new SftpException(ChannelSftp.SSH_FX_BAD_MESSAGE, "No File"))
        .anyTimes();
        expect(sftpClient.stat(EasyMock.anyString())).andReturn(attrs).anyTimes();

        expect(attrs.isDir()).andReturn(true).anyTimes();

        replay(sftpClient, attrs, jsch, backup, session);
        sftpConnection.connect();
        sftpConnection.exportBackup(Paths.get("/backupfilepath"), Paths.get("/backupdatapath"), "/remotepath",
                backupName, "default", backup, propertyReceivedListener);
    }

    @Test(expected = ExportException.class)
    public void exportBackup_DirectoryExist_throwsException() throws SftpException, JSchException {
        final String backupName = "backup1";
        exportBackup(true);
        expect(sftpClient.lstat(anyString())).andThrow(new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "No File"))
        .anyTimes();
        expect(sftpClient.stat(EasyMock.anyString())).andReturn(attrs).anyTimes();

        expect(attrs.isDir()).andReturn(true).anyTimes();

        replay(sftpClient, attrs, jsch, backup, session);
        sftpConnection.connect();
        sftpConnection.exportBackup(Paths.get("/backupfilepath"), Paths.get("/backupdatapath"), "/remotepath",
                backupName, "default", backup, propertyReceivedListener);
    }

    @Test(expected = ExportException.class)
    public void exportBackup_DirectoryExist_NoDirectoryException() throws SftpException, JSchException {
        assumeFalse("Skipping the test on Windows OS.", OSUtils.isWindows());
        final String backupName = "backup1";
        exportBackup(true);
        expectFilePermissionSetup(DIR_PERMISSION, 2);
        expect(sftpClient.lstat(anyString())).andThrow(new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "No File"))
        .anyTimes();
        expect(sftpClient.stat(EasyMock.anyString())).andThrow(new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "No directory"))
        .anyTimes();
        expect(attrs.isDir()).andReturn(true).anyTimes();
        expect(sftpClient.isConnected()).andReturn(true).anyTimes();

        replay(sftpClient, attrs, jsch, backup, session);
        sftpConnection.connect();
        sftpConnection.exportBackup(Paths.get("/backupfilepath"), Paths.get("/backupdatapath"), "/remotepath",
                backupName, "default", backup, propertyReceivedListener);
    }

    @Test(expected = ExportException.class)
    public void exportBackup_DirectoryDoesntExist_Exception() throws SftpException, JSchException {
        final String backupName = "backup1";
        exportBackup(true);
        expect(sftpClient.lstat(anyString())).andThrow(new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "No File"))
        .anyTimes();
        expect(sftpClient.stat(EasyMock.anyString())).andReturn(attrs).anyTimes();
        expect(attrs.isDir()).andReturn(true).anyTimes();

        replay(sftpClient, attrs, jsch, backup, session);
        sftpConnection.connect();
        sftpConnection.exportBackup(Paths.get("/backupfilepath"), Paths.get("/backupdatapath"), "/remotepath",
                backupName, "default", backup, propertyReceivedListener);
    }

    @Test(expected = ExportException.class)
    public void exportBackup_remoteBackupAlreadyExists_throwsException() throws SftpException, IOException {
        final Path backupDataFolder = folderForBackupData.getRoot().toPath();

        final File backup = folderForBackupFile.newFile("mybackup.json");
        final Path backupFile = backup.toPath();

        sftpClient.mkdir(EasyMock.anyString());
        expectLastCall().anyTimes();

        expect(this.backup.getCreationType()).andReturn(MANUAL).times(2);
        expect(this.backup.getName()).andReturn("backup1").anyTimes();
        expect(sftpClient.stat(EasyMock.anyString())).andReturn(attrs).anyTimes();
        expect(attrs.isDir()).andReturn(false).anyTimes();
        sftpClient.cd(EasyMock.anyString());
        expectLastCall().anyTimes();

        expect(sftpClient.lstat(EasyMock.anyString())).andThrow(new SftpException(2, "Backup doesn't exist"));
        sftpClient.put(anyObject(InputStream.class), EasyMock.anyString(), EasyMock.anyInt());
        expectLastCall().anyTimes();

        expect(sftpClient.isClosed()).andReturn(false).anyTimes();
        expect(sftpClient.lstat(EasyMock.anyString())).andReturn(attrs);

        final OffsetDateTime backupCreationTime = OffsetDateTime.now();
        expect(this.backup.getCreationTime()).andReturn(backupCreationTime).anyTimes();

        replay(sftpClient, attrs, this.backup);

        sftpConnection.exportBackup(backupFile, backupDataFolder, REMOTE_PATH, "backup1", "default", this.backup, propertyReceivedListener);

        sftpConnection.exportBackup(backupFile, backupDataFolder, REMOTE_PATH, "backup1", "default", this.backup, propertyReceivedListener);

        verify(sftpClient, attrs, this.backup);
    }

    @Test(expected = ExportException.class)
    public void exportBackup_remoteTarballAndLegacyExists_throwsException() throws SftpException, IOException, JSchException {
        createUnCompressedFile(folderForBothBackups);
        exportBackup(true);

        expect(sftpClient.stat(EasyMock.anyString())).andReturn(attrs).anyTimes();
        expect(sftpClient.lstat(EasyMock.anyString())).andReturn(attrs).anyTimes();
        expect(attrs.isDir()).andReturn(true).anyTimes();

        sftpClient.put(anyObject(InputStream.class), EasyMock.anyString(), EasyMock.anyInt());
        expectLastCall().anyTimes();
        replay(sftpClient, attrs, jsch, backup, session);
        sftpConnection.connect();
        sftpConnection.exportBackup(Paths.get("/backupfilepath"), Paths.get("/backupdatapath"), "/remotepath",
                "backup1", "default", backup, propertyReceivedListener);
    }

    @Test
    public void getBackupFileContent_validInput_returnBackupFileContent()
            throws SftpException, JSchException {
        final String expectedContent = "{\"backupId\":\"backup\"}";
        final InputStream backupFile = new ByteArrayInputStream(expectedContent.getBytes());

        final Vector<LsEntry> vector = new Vector<>();
        vector.add(mockFile("folder", true));
        vector.add(mockFile("randomFile.txt", false));
        vector.add(mockFile("backup.json", false));

        sftpClient.cd(BACKUP_FILE_FOLDER);
        expectLastCall();
        expect(sftpClient.ls(BACKUP_FILE_FOLDER)).andReturn(vector);
        expect(sftpClient.get(BACKUP_FILE)).andReturn(backupFile);
        expect(jsch.getSession(externalProperties.getUser(), externalProperties.getHost(),
                externalProperties.getPort())).andReturn(session);
        replay(sftpClient, jsch, session);
        sftpConnection.connect();
        final String obtainedContext = sftpConnection.getBackupFileContent(REMOTE_PATH, LEGACY);

        verify(sftpClient);

        assertEquals(expectedContent, obtainedContext);
    }

    @Test
    public void importBackupData_validInput_legacy() throws SftpException, JSchException {
        final Vector<LsEntry> vector = new Vector<>();
        final ChannelSftp.LsEntry lsEntry = createMock(ChannelSftp.LsEntry.class);
        vector.add(lsEntry);

        sftpClient.cd(BACKUP_DATA_FOLDER);
        expectLastCall();

        expect(sftpClient.ls(BACKUP_DATA_FOLDER)).andReturn(vector);

        expect(lsEntry.getFilename()).andReturn("backupup").times(2);

        expect(lsEntry.getAttrs()).andReturn(attrs);
        expect(attrs.isDir()).andReturn(false);

        final Path path = Paths.get(LOCAL_PATH + File.separator + "backupup");
        sftpClient.get(BACKUP_DATA_FOLDER + File.separator + "backupup", path.toString());
        expectLastCall();

        expect(jsch.getSession(externalProperties.getUser(), externalProperties.getHost(),
                externalProperties.getPort())).andReturn(session);
        replay(sftpClient, lsEntry, attrs, jsch, session);
        sftpConnection.connect();

        sftpConnection.importBackupData(REMOTE_PATH, Paths.get(LOCAL_PATH), LEGACY);

        verify(sftpClient, attrs, lsEntry);
    }

    @Test
    public void getBackupFileContent_validInput_returnBackupFileContent_tarball() throws IOException {
        createUnCompressedFile(folderForUnCompressedFile);
        replay(archiveUtils);
        final String obtainedContext = sftpConnection.getBackupFileContent(folderForUnCompressedFile.getRoot().getPath()
                + File.separator + "DEFAULT" + File.separator + "mybackup" + File.separator + BACKUP_FILE_FOLDER_NAME, TARBALL);
        System.out.println(folderForUnCompressedFile.getRoot().getPath() + File.separator + "DEFAULT" + File.separator
                + "mybackup");

        assertEquals(BACKUP_CONTENT, obtainedContext);
    }

    @Test(expected = ImportException.class)
    public void getBackupFileContent_backupFileDoesNotExist_throwsException() throws SftpException {
        final Vector<LsEntry> vector = new Vector<>();
        vector.add(mockFile("folder", true));
        vector.add(mockFile("randomFile.txt", false));
        sftpClient.cd(BACKUP_FILE_FOLDER);
        expectLastCall();
        expect(sftpClient.ls(BACKUP_FILE_FOLDER)).andReturn(vector);
        replay(sftpClient);
        sftpConnection.getBackupFileContent(REMOTE_PATH, LEGACY);
    }

    @Test
    public void getMatchingBackUpTarballNames__noTarballFoundDueToSftpException() throws SftpException, IOException, JSchException {
        exportBackup(true);
        expect(sftpClient.ls(REMOTE_PATH)).andThrow(new SftpException(ChannelSftp.SSH_FX_CONNECTION_LOST, "SFTP Exception"));
        replay(sftpClient, attrs, backup, jsch, session);
        sftpConnection.connect();
        List<String> result = sftpConnection.getMatchingBackUpTarballNames(BACKUP_DATA_FOLDER);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getMatchingBackUpTarballNames__noTarballFound() throws SftpException, IOException, JSchException {
        final Vector<LsEntry> tarballFiles = new Vector<>();
        tarballFiles.add(mockFile("backupdata", true));
        exportBackup(true);
        expect(sftpClient.ls(REMOTE_PATH)).andReturn(tarballFiles);
        replay(sftpClient, attrs, backup, jsch, session);
        sftpConnection.connect();
        List<String> result = sftpConnection.getMatchingBackUpTarballNames(BACKUP_DATA_FOLDER);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getMatchingBackUpTarballNames_multipleNamesFound() throws SftpException, IOException, JSchException {
        final Vector<LsEntry> tarballFiles = new Vector<>();
        tarballFiles.add(mockFile("backupdata-1.tar.gz", false));
        tarballFiles.add(mockFile("backupdata-2.tar.gz", false));
        exportBackup(true);
        expect(sftpClient.ls(REMOTE_PATH)).andReturn(tarballFiles);
        replay(sftpClient, attrs, backup, jsch, session);
        sftpConnection.connect();
        List<String> result = sftpConnection.getMatchingBackUpTarballNames(BACKUP_DATA_FOLDER);
        assertEquals(2, result.size());
    }

    @Test
    public void close_withValidSessionAndChannel_valid() throws Exception {
        expect(jsch.getSession(externalProperties.getUser(), externalProperties.getHost(),
                externalProperties.getPort())).andReturn(session);
        replay(sftpClient, session, jsch);
        sftpConnection.connect();
        sftpConnection.close();

        verify(sftpClient, session);
    }

    @Test
    public void sftpConnection_MonitorSystemOutProgressMonitor_test_emptyBehavior() {
        final SftpChannelManager manager = new SftpChannelManager();
        manager.setTimeout(10);
        manager.setJsch(jsch);
        manager.setClientProperties(externalProperties);
        sftpConnection = new SftpConnection(manager, archiveUtils);
        ProgressMonitor progressMonitor = sftpConnection.new ProgressMonitor(100, () -> false); // Whether this finishes or not doesn't matter
        progressMonitor.init(SftpProgressMonitor.PUT, BACKUP_FILE, BACKUP_DATA_FOLDER, 100);
        progressMonitor.count(1000);
        assertEquals("0%", progressMonitor.getLastPercent());
        progressMonitor.end();
    }

    @Test
    public void sftpConnection_propertyChange_validProperty_closed() {
        final SftpChannelManager manager = new SftpChannelManager();
        manager.setTimeout(10);
        manager.setJsch(jsch);
        manager.setClientProperties(externalProperties);
        sftpConnection = new SftpConnection(manager, archiveUtils);
        sftpConnection.propertyChange(new PropertyChangeEvent(this, BACKUP_MONITOR_TIMEOUT_NAME,
                false, true));
    }

    @Test
    public void sftpConnection_MonitorSystemOutProgressMonitor_test_maxpercent() {
        final long kbytes_100=100 * 1024;
        final long kbytes_10=10 * 1024;
        final long kbytes_6=6 * 1024;
        final long bytes_512=512;
        final SftpChannelManager manager = new SftpChannelManager();
        manager.setTimeout(10);
        manager.setJsch(jsch);
        manager.setClientProperties(externalProperties);
        sftpConnection = new SftpConnection(manager, archiveUtils);
        ProgressMonitor progressMonitor = sftpConnection.new ProgressMonitor(kbytes_100, () -> false); // Whether this finishes doesn't matter
        // it estimates 90 of the file, if more information is sent, it stop increasing the percent.
        progressMonitor.init(SftpProgressMonitor.PUT, BACKUP_FILE, BACKUP_DATA_FOLDER, 0);
        progressMonitor.count(kbytes_6);
        assertEquals("7%", progressMonitor.getLastPercent());
        progressMonitor.count(kbytes_10);
        progressMonitor.count(kbytes_10);
        progressMonitor.count(kbytes_10);
        assertEquals("40%", progressMonitor.getLastPercent());
        progressMonitor.count(kbytes_10);
        progressMonitor.count(kbytes_10);
        progressMonitor.count(kbytes_10);
        progressMonitor.count(kbytes_10);
        progressMonitor.count(kbytes_10);
        assertEquals("96%", progressMonitor.getLastPercent());
        progressMonitor.count(kbytes_100);
        progressMonitor.end();
    }

    @Test
    public void sftpConnection_MonitorSystemOutProgressMonitor_test_listener_isTimeout() throws InterruptedException {
        final long kbytes_100=100 * 1024;
        final long kbytes_6=6 * 1024;
        final long kbytes_10=10 * 1024;

        final SftpChannelManager manager = new SftpChannelManager();
        manager.setTimeout(10);
        manager.setJsch(jsch);
        manager.setClientProperties(externalProperties);
        sftpConnection = new SftpConnection(manager, archiveUtils);
        sftpConnection.setTimeoutBytesReceivedSeconds(3);
        ProgressMonitor progressMonitor = sftpConnection.new ProgressMonitor(kbytes_100, () -> false); // Whether this finishes doesn't matter
        progressMonitor.addListener(propertyReceivedListener);
        progressMonitor.init(SftpProgressMonitor.PUT, BACKUP_FILE, BACKUP_DATA_FOLDER, 0);
        progressMonitor.count(kbytes_6);
        Thread.sleep(1000);
        // Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> bytesReceivedLister.getProperty().isBlank() );
        assertEquals("7%", progressMonitor.getLastPercent());
        progressMonitor.count(kbytes_10);
        Thread.sleep(1000);
        progressMonitor.count(kbytes_6);
        Thread.sleep(1000);
        progressMonitor.count(kbytes_6);
        Thread.sleep(1000);
        progressMonitor.count(kbytes_6);
        Thread.sleep(1000);
        progressMonitor.count(kbytes_6);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> propertyReceivedListener.getProperty().equals(BACKUP_MONITOR_TIMEOUT_NAME) );
        // validate the value of the property received
        assertEquals(propertyReceivedListener.getNewValue(), "true");
    }

    @Test
    public void sftpConnection_MonitorSystemOutProgressMonitor_test_listener_isNotTimeout() throws InterruptedException {
        final long kbytes_100=100 * 1024;
        final long kbytes_6=6 * 1024;
        final long kbytes_10=10 * 1024;

        final SftpChannelManager manager = new SftpChannelManager();
        manager.setTimeout(10);
        manager.setJsch(jsch);
        manager.setClientProperties(externalProperties);
        sftpConnection = new SftpConnection(manager, archiveUtils);
        sftpConnection.setTimeoutBytesReceivedSeconds(3);
        ProgressMonitor progressMonitor = sftpConnection.new ProgressMonitor(kbytes_100, () -> false); // Whether this finishes doesn't matter
        progressMonitor.addListener(propertyReceivedListener);
        progressMonitor.init(SftpProgressMonitor.PUT, BACKUP_FILE, BACKUP_DATA_FOLDER, 0);
        progressMonitor.count(kbytes_6);
        Thread.sleep(1000);
        assertEquals("7%", progressMonitor.getLastPercent());
        progressMonitor.count(kbytes_10);
        Thread.sleep(2000);
        assertEquals(propertyReceivedListener.getProperty(), "");
        // validate the new value (expect null as is not timeout yet)
        assertEquals(propertyReceivedListener.getNewValue(), null);
    }

    private void expectFilePermissionSetup(final int permission, final int countOfNewFiles) throws SftpException {
        sftpClient.chmod(EasyMock.eq(permission), EasyMock.anyString());
        expectLastCall().times(countOfNewFiles);
    }

    private LsEntry mockFile(final String name, final boolean isFolder) {
        final SftpATTRS fileAttributes = createMock(SftpATTRS.class);
        expect(fileAttributes.isDir()).andReturn(isFolder).anyTimes();

        final ChannelSftp.LsEntry file = createMock(ChannelSftp.LsEntry.class);
        expect(file.getAttrs()).andReturn(fileAttributes).anyTimes();
        expect(file.getFilename()).andReturn(name).anyTimes();

        replay(fileAttributes, file);
        return file;
    }

    private ExternalClientProperties populateExternalClientProperties(final AuthenticationType authType, final SftpServer sftpServer) throws URISyntaxException {
        final ExternalClientProperties externalClientProperties = createMock(ExternalClientProperties.class);
        expect(externalClientProperties.getHost()).andReturn("127.0.0.1").anyTimes();
        expect(externalClientProperties.getPort()).andReturn(22).anyTimes();
        expect(externalClientProperties.getUser()).andReturn("user").anyTimes();
        if (authType.equals(AuthenticationType.PASSWORD)) {
            expect(externalClientProperties.getUri()).andReturn(new URI("sftp://user@127.0.0.1:22/bro_test")).anyTimes();
            expect(externalClientProperties.getPassword()).andReturn("password").anyTimes();
            expect(externalClientProperties.getSftpServer()).andReturn(null).anyTimes();
        } else {
            expect(externalClientProperties.getUri()).andReturn(new URI("sftp://user@127.0.0.1:22/bro_test")).anyTimes();
            expect(externalClientProperties.getPassword()).andReturn(null).anyTimes();
            expect(externalClientProperties.getSftpServer()).andReturn(sftpServer).anyTimes();
        }
        replay(externalClientProperties);
        return externalClientProperties;
    }

    private Session getSession(final boolean connected) throws JSchException {
        final Session session = createMock(Session.class);
        session.setPassword(EasyMock.anyString());
        expectLastCall().anyTimes();
        session.setSocketFactory(EasyMock.isA(JSchSocketFactory.class));
        expectLastCall().anyTimes();
        session.connect(EasyMock.anyInt());
        expectLastCall().anyTimes();
        session.disconnect();
        expectLastCall().anyTimes();
        expect(session.isConnected()).andReturn(connected).anyTimes();
        session.setConfig(EasyMock.anyString(), EasyMock.anyString());
        expectLastCall().anyTimes();
        expect(session.openChannel(EasyMock.anyString())).andReturn(sftpClient).anyTimes();
        return session;
    }

    private ChannelSftp getChannel() throws JSchException {
        final ChannelSftp channel = createMock(ChannelSftp.class);
        channel.disconnect();
        expectLastCall().anyTimes();
        channel.setBulkRequests(EasyMock.anyInt());
        expectLastCall().anyTimes();
        channel.connect(EasyMock.anyInt());
        expectLastCall().anyTimes();
        return channel;
    }

    private void exportBackup(final boolean isConnected) throws JSchException, SftpException {
        session = getSession(isConnected);
        expect(backup.getCreationType()).andReturn(MANUAL);
        final String backupName = "backup1";
        expect(backup.getName()).andReturn(backupName).anyTimes();
        sftpClient.mkdir(EasyMock.anyString());
        expectLastCall().anyTimes();
        sftpClient.cd(EasyMock.anyString());
        expectLastCall().anyTimes();

        expect(backup.getCreationTime()).andReturn(OffsetDateTime.now());
        expect(jsch.getSession(externalProperties.getUser(), externalProperties.getHost(),
                externalProperties.getPort())).andReturn(session).anyTimes();
    }

    private FileWriter mockFileWriter () throws IOException {
        FileWriter mock = PowerMock.createMock(FileWriter.class);
        mock.write(EasyMock.anyString());
        expectLastCall().anyTimes();
        mock.close();
        expectLastCall().anyTimes();
        return mock;
    }

    @Test(expected = ImportException.class)
    public void downloadBackupFile_externalClientProperties_invalid() throws JSchException {
        expect(jsch.getSession(externalProperties.getUser(), externalProperties.getHost(),
                externalProperties.getPort())).andReturn(session).anyTimes();

        expect(session.openChannel("sftp")).andReturn(sftpClient).anyTimes();
        replay(jsch, session);
        sftpConnection.connect();
        sftpConnection.downloadBackupFile(externalClientImportProperties, propertyReceivedListener);
    }

    @Test
    public void downloadBackupFile_externalClientProperties_valid() throws JSchException, SftpException, IOException {

        expect(jsch.getSession(externalProperties.getUser(), externalProperties.getHost(),
                externalProperties.getPort())).andReturn(session).anyTimes();

        expect(session.openChannel("sftp")).andReturn(sftpClient).anyTimes();
        expect(sftpClient.lstat(anyString())).andReturn(attrs).anyTimes();

        byte[] bytes = new byte[20];
        InputStream inputStream = new ByteArrayInputStream(bytes);

        expect(sftpClient.get(anyString())).andReturn(inputStream).anyTimes();
        expect(attrs.getSize()).andReturn(12345L).anyTimes();

        replay(jsch, session, sftpClient, attrs, archiveUtils);

        Mockito.doReturn(List.of(folderForBackupFile.newFile("backupData"))).when(sftpConnection).downloadRemotePath(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.doReturn(true).when(sftpConnection).checksumMatches(Mockito.anyString(), Mockito.anyString());

        sftpConnection.connect();

        sftpConnection.downloadBackupFile(externalClientImportProperties, propertyReceivedListener);
        verify(jsch, session, sftpClient, attrs, archiveUtils);
    }

    @Test(expected = ImportException.class)
    public void downloadBackupFile_externalClientProperties_notGzip_invalid() throws JSchException, SftpException, IOException {

        expect(jsch.getSession(externalProperties.getUser(), externalProperties.getHost(),
                externalProperties.getPort())).andReturn(session).anyTimes();

        expect(session.openChannel("sftp")).andReturn(sftpClient).anyTimes();
        expect(sftpClient.lstat(anyString())).andReturn(attrs).anyTimes();

        byte[] bytes = new byte[20];
        InputStream inputStream = new ByteArrayInputStream(bytes);

        expect(sftpClient.get(anyString())).andReturn(inputStream).anyTimes();
        expect(sftpClient.get(anyString(), anyObject(ProgressMonitor.class))).andReturn(inputStream).anyTimes();
        expect(attrs.getSize()).andReturn(12345L).anyTimes();

        replay(jsch, session, sftpClient, attrs, archiveUtils);

        sftpConnection.connect();

        sftpConnection.downloadBackupFile(externalClientImportProperties, propertyReceivedListener);
    }

    @Test(expected = ImportException.class)
    public void downloadBackupFile_backupFileNameIsEmpty_invalid() throws JSchException, SftpException, IOException {
        expect(jsch.getSession(externalProperties.getUser(), externalProperties.getHost(),
                externalProperties.getPort())).andReturn(session).anyTimes();
        expect(session.openChannel("sftp")).andReturn(sftpClient).anyTimes();
        expect(sftpClient.lstat(anyString())).andReturn(attrs).anyTimes();

        byte[] bytes = new byte[20];
        InputStream inputStream = new ByteArrayInputStream(bytes);

        expect(sftpClient.get(anyString())).andReturn(inputStream).anyTimes();

        expect(attrs.getSize()).andReturn(12345L).anyTimes();

        replay(jsch, session, sftpClient, attrs, archiveUtils);

        Mockito.doReturn(List.of(folderForBackupData.newFile("notFoundFile"))).when(sftpConnection).downloadRemotePath(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        sftpConnection.connect();
        sftpConnection.downloadBackupFile(externalClientImportProperties, propertyReceivedListener);
    }

    @Test(expected = ImportException.class)
    public void downloadBackupFile_checksumGotException_invalid() throws JSchException, SftpException, IOException {

        expect(jsch.getSession(externalProperties.getUser(), externalProperties.getHost(),
                externalProperties.getPort())).andReturn(session).anyTimes();

        expect(session.openChannel("sftp")).andReturn(sftpClient).anyTimes();

        expect(sftpClient.lstat(anyString())).andReturn(attrs).anyTimes();

        expect(sftpClient.get(anyString())).andThrow(new SftpException(1, "")).anyTimes();

        expect(attrs.getSize()).andReturn(12345L).anyTimes();

        replay(jsch, session, sftpClient, attrs, archiveUtils);

        Mockito.doReturn(List.of(folderForBackupFile.newFile("backupData"))).when(sftpConnection).downloadRemotePath(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        sftpConnection.connect();
        sftpConnection.downloadBackupFile(externalClientImportProperties, propertyReceivedListener);
    }

    @Test(expected = ImportException.class)
    public void downloadBackupFile_checksumDoesNotMatch_invalid() throws JSchException, SftpException, IOException {

        expect(jsch.getSession(externalProperties.getUser(), externalProperties.getHost(),
                externalProperties.getPort())).andReturn(session).anyTimes();

        expect(session.openChannel("sftp")).andReturn(sftpClient).anyTimes();

        expect(sftpClient.lstat(anyString())).andReturn(attrs).anyTimes();

        byte[] bytes = "1806c3b46e47805".getBytes();
        InputStream inputStream = new ByteArrayInputStream(bytes);
        GZIPInputStream gzipInputStream = createMock(GZIPInputStream.class);

        expect(sftpClient.get(anyString())).andReturn(inputStream).anyTimes();
        expect(sftpClient.get(anyString(), anyObject(ProgressMonitor.class))).andReturn(gzipInputStream).anyTimes();
        expect(attrs.getSize()).andReturn(12345L).anyTimes();
        replay(jsch, session, sftpClient, attrs, archiveUtils);

        Mockito.doReturn(List.of(folderForBackupFile.newFile("backupData"))).when(sftpConnection).downloadRemotePath(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        sftpConnection.connect();
        sftpConnection.downloadBackupFile(externalClientImportProperties, propertyReceivedListener);
    }

    @Test
    public void unpackTarStream_getCheckSum_valid() throws IOException {
        List<Path> files;
        ChecksumHash64 initialChecksum = new ChecksumHash64();
        ChecksumHash64 calculatedChecksum = new ChecksumHash64();
        File compressedFile = createCompressedFile(true, folderForBothBackups);
        String sourceCheckSum = initialChecksum.createCheckSum(compressedFile);
        // Using a "real" archive utils here rather than a mock since this is effectively a system test, not a unit test
        final ArchiveUtils utils = new ArchiveUtils();
        utils.setProvider(new PersistProviderFactory());
        StreamingArchiveService archiveService = new StreamingArchiveService(utils);
        InputStream sourceStream = new FileInputStream(compressedFile);
        sourceStream.close();

        sourceStream = new FileInputStream(compressedFile);
        try (TarArchiveInputStream inputStream = archiveService.openTarGzipInput (sourceStream, calculatedChecksum)) {
            files=archiveService.unpackTarStream(inputStream,
                    folderForBackupData.getRoot().toPath(),
                    folderForBackupFile.getRoot().toPath(), (b) -> {});
        }
        assertEquals(sourceCheckSum, calculatedChecksum.getStringValue());
        Optional<String> jsonFile = files.stream()
                .map(p -> p.toAbsolutePath().toString())
                .filter(fileName -> fileName.endsWith("mybackup.json"))
                .findFirst();
        assertTrue(jsonFile.isPresent()); // Don't assert on file ordering as unpack order isn't defined

        sourceStream.close();
        files.forEach(p -> p.toFile().delete());
    }

    private byte[] getHeaderStream(final InputStream inputStream) throws IOException {
        final int BLOCK_SIZE = 10;
        final byte[] buffer = new byte[BLOCK_SIZE];
        inputStream.read(buffer);
        return buffer;
    }

    @Test(expected = ImportException.class )
    public void getFileSizeInSFTP_NoFile_Exception() throws JSchException, SftpException {

        expect(jsch.getSession(externalProperties.getUser(), externalProperties.getHost(),
                externalProperties.getPort())).andReturn(session).anyTimes();

        expect(session.openChannel("sftp")).andReturn(sftpClient).anyTimes();

        expect(sftpClient.lstat(anyString())).andThrow(new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "No File"))
                .anyTimes();

        replay(jsch, session, sftpClient, attrs);
        sftpConnection.connect();
        sftpConnection.downloadBackupFile(externalClientImportProperties, propertyReceivedListener);
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
            if (evt.getPropertyName().equals(BACKUP_MONITOR_TIMEOUT_NAME)) {
                property = evt.getPropertyName();
                oldValue = String.valueOf(evt.getOldValue());
                newValue = String.valueOf(evt.getNewValue());
            }
        }
    }
}
