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
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.net.URI;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupImporterTest.PropertyReceived;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.exception.ExportException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientExportProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.HttpConnection;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.SftpConnection;

public class BackupExporterTest {

    private BackupExporter backupExporter;
    private ExternalConnectionFactory externalConnectionFactory;
    private ExternalClientExportProperties externalClientProperties;
    private SftpConnection sftpConnection;
    private HttpConnection httpConnection;
    private Backup backup;
    private PropertyReceived propertyReceivedListener;

    @Before
    public void setup() {
        backupExporter = new BackupExporter();

        externalConnectionFactory = createMock(ExternalConnectionFactory.class);
        externalClientProperties = createMock(ExternalClientExportProperties.class);
        sftpConnection = createMock(SftpConnection.class);
        httpConnection = createMock(HttpConnection.class);

        backup = createMock(Backup.class);
        backupExporter.setExternalConnectionFactory(externalConnectionFactory);
        propertyReceivedListener = new BackupImporterTest().new PropertyReceived();
    }

    // @Test
    public void exportBackup_validInput_valid() throws Exception {

        expect(externalConnectionFactory.connect(externalClientProperties)).andReturn(sftpConnection);

        sftpConnection.close();
        expectLastCall();

        expect(externalClientProperties.getBackupDataPath()).andReturn(Paths.get("/backupdatapath"));
        expect(externalClientProperties.getBackupFilePath()).andReturn(Paths.get("/backupfilepath"));
        expect(externalClientProperties.getExternalClientPath()).andReturn("/remotepath");
        expect(externalClientProperties.isUsingHttpUriScheme()).andReturn(false);
        expect(externalClientProperties.getBackupName()).andReturn("backup1");
        expect(externalClientProperties.getBackupManagerId()).andReturn("default");
        expect(externalClientProperties.getBackup()).andReturn(backup);
        sftpConnection.exportBackup(Paths.get("/backupfilepath"), Paths.get("/backupdatapath"), "/remotepath", "backup1", "default", backup, propertyReceivedListener);
        expectLastCall();

        replay(externalClientProperties, sftpConnection, externalConnectionFactory);

        backupExporter.exportBackup(externalClientProperties, propertyReceivedListener);

        verify(externalClientProperties, sftpConnection);

    }

    @Test(expected = ExportException.class)
    public void exportBackup_issueWhileGetSftpConnection_throwException() throws Exception {

        expect(externalConnectionFactory.connect(externalClientProperties))
        .andThrow(new ExportException("Failed while trying to export backup in SFTP Server"));

        sftpConnection.close();
        expectLastCall();

        replay(externalClientProperties, sftpConnection, externalConnectionFactory);

        backupExporter.exportBackup(externalClientProperties, propertyReceivedListener);

    }

    @Test(expected = ExportException.class)
    public void exportBackup_issueWithSftpWhileBackupdataUpload_throwException() throws Exception {
        expect(externalConnectionFactory.connect(externalClientProperties)).andReturn(sftpConnection);
        sftpConnection.close();
        expectLastCall();
        sftpConnection.exportBackup(Paths.get("/backupfilepath"), Paths.get("/backupdatapath"), "/remotepath", "backup1", "default", backup, propertyReceivedListener);
        expectLastCall().andThrow(new ExportException("Failed while trying to export backup in SFTP Server"));
        expect(externalClientProperties.getBackupFilePath()).andReturn(Paths.get("/backupfilepath"));
        expect(externalClientProperties.getBackupDataPath()).andReturn(Paths.get("/backupdatapath"));
        expect(externalClientProperties.getExternalClientPath()).andReturn("/remotepath");
        expect(externalClientProperties.isUsingHttpUriScheme()).andReturn(false);
        expect(externalClientProperties.getBackupName()).andReturn("backup1");
        expect(externalClientProperties.getBackupManagerId()).andReturn("default");
        expect(externalClientProperties.getBackup()).andReturn(backup);
        expect(backup.getCreationTime()).andReturn(OffsetDateTime.now(ZoneId.systemDefault()));
        replay(externalClientProperties, sftpConnection, externalConnectionFactory);
        backupExporter.exportBackup(externalClientProperties, propertyReceivedListener);
    }

    @Test
    public void exportBackup_http_validInput_valid() throws Exception {
        expect(externalConnectionFactory.connect(externalClientProperties)).andReturn(httpConnection);
        httpConnection.close();
        expectLastCall().anyTimes();
        expect(externalClientProperties.getBackupDataPath()).andReturn(Paths.get("/backupdatapath"));
        expect(externalClientProperties.getBackupFilePath()).andReturn(Paths.get("/backupfilepath"));
        expect(externalClientProperties.getUri()).andReturn(new URI("http://remotepath"));
        expect(externalClientProperties.isUsingHttpUriScheme()).andReturn(true);
        expect(externalClientProperties.getBackupName()).andReturn("backup1");
        expect(externalClientProperties.getBackupManagerId()).andReturn("default");
        expect(externalClientProperties.getBackup()).andReturn(backup);
        expect(backup.getCreationTime()).andReturn(OffsetDateTime.now(ZoneId.systemDefault()));
        httpConnection.exportBackup(Paths.get("/backupfilepath"), Paths.get("/backupdatapath"), "http://remotepath", "backup1", "default", backup, propertyReceivedListener);
        expectLastCall();
        replay(externalClientProperties, httpConnection, externalConnectionFactory);
        backupExporter.exportBackup(externalClientProperties, propertyReceivedListener);
        verify(externalClientProperties, httpConnection);
    }
}
