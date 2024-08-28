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
package com.ericsson.adp.mgmt.backupandrestore.job;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.PROGRESS_MONITOR_CURRENT_PERCENTAGE;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.Payload;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupExporter;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupFolder;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMClient;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.exception.SftpServerNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientExportProperties;

public class ExportBackupJobTest {

    private static final String TEST_SFTP_SERVER = "testServer";
    private ExportBackupJob job;
    private Backup backup;
    private Backup backup1;
    private BackupManager backupManager;
    private Action action;
    private BackupExporter backupExporter;
    private ActionRepository actionRepository;
    private static final OffsetDateTime creationTime = OffsetDateTime.now();
    private PropertyChangeListener propertyReceivedListener;
    private CMMediatorService cmMediatorService;
    private CMMClient cmmClient;
    @Before
    public void setUp() throws Exception {
        propertyReceivedListener = new PropertyChangeListener()
        {   @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                return;
            }
        };
        cmMediatorService = EasyMock.createMock(CMMediatorService.class);
        job = new ExportBackupJob();
        cmmClient = new CMMClient();
        cmmClient.setFlagEnabled(true);
        cmmClient.setMaxAttempts("5");
        cmmClient.setMaxDelay("3000");
        backup = createMock(Backup.class);
        expect(backup.getName()).andReturn("backup").anyTimes();
        expect(backup.getCreationTime()).andReturn(creationTime).anyTimes();
        expect(backup.getCreationType()).andReturn(BackupCreationType.MANUAL).anyTimes();
        expect(cmMediatorService.getCMMClient()).andReturn(cmmClient);
        replay(backup, cmMediatorService);
        backup1 = createMock(Backup.class);
        expect(backup1.getName()).andReturn("backup1").anyTimes();
        expect(backup1.getCreationType()).andReturn(BackupCreationType.SCHEDULED).anyTimes();
        replay(backup1);

        backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn("123").anyTimes();

        action = createMock(Action.class);
        actionRepository = createMock(ActionRepository.class);

        backupExporter = createMock(BackupExporter.class);

        final BackupFileService backupFileService = createMock(BackupFileService.class);
        final BackupLocationService backupLocationService = createMock(BackupLocationService.class);

        expect(backupLocationService.getBackupFolder(backupManager, "backup")).andReturn(new BackupFolder(Paths.get("/mypath")));
        expect(backupFileService.getBackupFolder("123")).andReturn(Paths.get("src/test/")).anyTimes();
        backupExporter.exportBackup(isA(ExternalClientExportProperties.class), isA(PropertyChangeListener.class));
        expectLastCall();

        expect(backupFileService.getBackupFilePath("123", "backup")).andReturn(Paths.get("/backupfile.json"));

        replay(backupLocationService, backupFileService, backupExporter);
        job.setBackupExporter(backupExporter);
        job.setBackupFileService(backupFileService);
        job.setBackupLocationService(backupLocationService);
        job.setBackupManager(backupManager);
        job.setActionRepository(actionRepository);
        job.setCmMediatorService(cmMediatorService);
    }

    @Test
    public void triggerJob_externalClientPropertiesWithValidURI_importBackupFromExternalAgentsCalled() throws Exception {

        expect(action.getPayload()).andReturn(getURIPayload(true, "backup")).anyTimes();

        expect(backupManager.getBackup("backup", Ownership.OWNED)).andReturn(backup);
        expect(action.isScheduledEvent()).andReturn(false).once();

        job.setAction(action);
        replay(action);

        replay(backupManager);
        job.triggerJob();
        verify(backupExporter);

        assertTrue(job.didFinish());
    }

    @Test
    public void triggerJob_externalClientPropertiesWithValidSftpServerName_importBackupFromExternalAgentsCalled() throws Exception {

        expect(action.getPayload()).andReturn(getSftpServerNamePayload("backup")).anyTimes();

        expect(backupManager.getBackup("backup", Ownership.OWNED)).andReturn(backup);
        final SftpServer sftpServer = createMock(SftpServer.class);
        expect(backupManager.getSftpServer(TEST_SFTP_SERVER)).andReturn(Optional.of(sftpServer));
        expect(action.isScheduledEvent()).andReturn(false).once();

        job.setAction(action);
        replay(action);

        replay(backupManager);
        job.triggerJob();
        verify(backupExporter);

        assertTrue(job.didFinish());
    }

    @Test(expected = SftpServerNotFoundException.class)
    public void triggerJob_externalClientPropertiesWithInexistentSftpServerName() throws Exception {

        expect(action.getPayload()).andReturn(getSftpServerNamePayload("backup")).anyTimes();

        expect(backupManager.getBackup("backup", Ownership.OWNED)).andReturn(backup);
        expect(backupManager.getSftpServer(TEST_SFTP_SERVER)).andReturn(Optional.empty());
        expect(action.isScheduledEvent()).andReturn(false).once();

        job.setAction(action);
        replay(action);

        replay(backupManager);
        job.triggerJob();
    }

    @Test
    public void completeJob_isScheduledEvent_updateAdditionalInfo() throws Exception {
        expect(action.getPayload()).andReturn(getURIPayload(true, "backup1")).anyTimes();

        expect(backupManager.getBackup("backup1", Ownership.OWNED)).andReturn(backup1);
        expect(action.isScheduledEvent()).andReturn(true).anyTimes();
        action.setAdditionalInfo(anyString());
        expectLastCall();

        job.setAction(action);
        replay(action);
        replay(backupManager);

        job.completeJob();

        verify(action);
        verify(backup);
    }

    @Test
    public void completeJob_isNotScheduledEvent_updateAdditionalInfo() throws Exception {
        expect(action.getPayload()).andReturn(getURIPayload(true, "backup")).anyTimes();

        expect(backupManager.getBackup("backup", Ownership.OWNED)).andReturn(backup);
        expect(action.isScheduledEvent()).andReturn(false).anyTimes();
        action.setAdditionalInfo(anyString());
        expectLastCall();

        job.setAction(action);
        replay(action);
        replay(backupManager);

        job.completeJob();
        verify(action);
        verify(backup);
    }

    @Test(expected = IllegalArgumentException.class)
    public void triggerJob_externalClientPropertiesWithInvalidURI_throwsIllegalArgumentException() throws Exception {

        expect(action.getPayload()).andReturn(getURIPayload(false, "backup")).anyTimes();
        expect(backupManager.getBackup("backup", Ownership.READABLE)).andReturn(backup);
        job.setAction(action);
        replay(action);
        replay(backupManager);
        job.triggerJob();
    }

    @Test
    public void validatePropertyChangeEvent_doubleReceived() {
        action.setProgressPercentage(EasyMock.anyDouble());
        expectLastCall();
        action.setProgressPercentage(EasyMock.anyDouble());
        expectLastCall();
        actionRepository.enqueueProgressReport(EasyMock.anyObject());
        expectLastCall();
        replay(action, actionRepository);
        job.setActionRepository(actionRepository);
        job.setAction(action);
        job.propertyChange ( new PropertyChangeEvent( this, PROGRESS_MONITOR_CURRENT_PERCENTAGE, 70.0, 90.0));
    }

    private Payload getURIPayload(final boolean valid, final String backupName) throws URISyntaxException {
        final ExportPayload payload = new ExportPayload();
        payload.setUri(URI.create(valid ? "sftp://user@localhost:0/my/path" : ":://adsjfklsa"));
        payload.setPassword("password");
        payload.setBackupName(backupName);
        return payload;
    }

    private Payload getSftpServerNamePayload(final String backupName){
        final ExportPayload payload = new ExportPayload();
        payload.setSftpServerName(TEST_SFTP_SERVER);
        payload.setBackupName(backupName);
        return payload;
    }
}
