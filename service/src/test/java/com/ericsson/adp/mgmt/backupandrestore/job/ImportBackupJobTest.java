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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.Payload;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupImporter;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMClient;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.exception.SftpServerNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientImportProperties;
import com.ericsson.adp.mgmt.backupandrestore.rest.SftpServerTestConstant;

public class ImportBackupJobTest {

    private static final String BACKUP_MANAGER_ID = "123";
    private static final String TEST_SFTP_SERVER = "testServer";
    private ImportBackupJob job;
    private Backup backup;
    private BackupManager backupManager;
    private ActionRepository actionRepository;
    private Action action;
    private BackupImporter backupImporter;
    private PropertyChangeListener propertyReceivedListener;
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
        cmmClient = new CMMClient();
        cmmClient.setFlagEnabled(true);
        cmmClient.setMaxAttempts("5");
        cmmClient.setMaxDelay("3000");
        this.job = new ImportBackupJob();

        this.backup = createMock(Backup.class);
        expect(this.backup.getName()).andReturn("myBackup").anyTimes();

        this.backupManager = createMock(BackupManager.class);
        this.actionRepository = createMock(ActionRepository.class);
        this.backupManager.addBackup(this.backup, Ownership.OWNED);
        expect(this.backupManager.getBackups(Ownership.READABLE)).andReturn(new ArrayList<>(Arrays.asList(this.backup)));
        expect(this.backupManager.getBackupManagerId()).andReturn(BACKUP_MANAGER_ID).anyTimes();
        expect(this.backupManager.getBackups(Ownership.OWNED)).andReturn(Arrays.asList(this.backup));

        this.action = createMock(Action.class);

        final BackupFileService backupFileService = createMock(BackupFileService.class);
        expect(backupFileService.getBackupFolder(BACKUP_MANAGER_ID)).andReturn(Paths.get("src/test/")).anyTimes();

        final BackupLocationService backupLocationService = createMock(BackupLocationService.class);

        expect(backupLocationService.getBackupManagerLocation(BACKUP_MANAGER_ID)).andReturn(Paths.get("/backupdir")).anyTimes();

        backupImporter = createMock(BackupImporter.class);
        expect(backupImporter.importBackup(isA(ExternalClientImportProperties.class), isA(BackupManager.class), isA(PropertyChangeListener.class))).andReturn(this.backup);
        expectLastCall();

        replay(this.backup, backupLocationService, backupFileService);
        this.job.setBackupImporter(backupImporter);
        this.job.setBackupFileService(backupFileService);
        this.job.setBackupLocationService(backupLocationService);
        this.job.setBackupManager(backupManager);
        this.job.setActionRepository(actionRepository);
    }

    @Test
    public void triggerJob_externalClientPropertiesWithValidURI_importBackupFromExternalAgentsCalled() throws Exception {

        replay(this.backupManager);
        replay(this.backupImporter);

        expect(this.action.getPayload()).andReturn(getURIPayload(true));
        replay(this.action);
        job.setAction(this.action);

        job.triggerJob();
        verify(backupImporter);

        assertTrue(job.didFinish());
    }

    @Test
    public void triggerJob_externalClientPropertiesWithValidSftpServerName_importBackupFromExternalAgentsCalled() throws Exception {
        final SftpServer sftpServer = new SftpServer(SftpServerTestConstant.createSftpServerInformation(), BACKUP_MANAGER_ID, null);
        expect(backupManager.getSftpServer(TEST_SFTP_SERVER)).andReturn(Optional.of(sftpServer));
        replay(this.backupManager);
        replay(this.backupImporter);

        expect(this.action.getPayload()).andReturn(getSftpServerNamePayload());
        replay(this.action);
        job.setAction(this.action);

        job.triggerJob();
        verify(backupImporter);

        assertTrue(job.didFinish());
    }

    @Test(expected = SftpServerNotFoundException.class)
    public void triggerJob_externalClientPropertiesWithInvalidSftpServerName() throws Exception {
        expect(backupManager.getSftpServer(TEST_SFTP_SERVER)).andReturn(Optional.empty());
        replay(this.backupManager);
        replay(this.backupImporter);

        expect(this.action.getPayload()).andReturn(getSftpServerNamePayload());
        replay(this.action);
        job.setAction(this.action);

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

    private Payload getURIPayload(final boolean valid) {
        final ImportPayload payload = new ImportPayload();
        if (valid) {
            payload.setUri(URI.create("sftp://user@localhost:0/my/path"));
        } else {
            payload.setUri(URI.create("sftpuser@localhost:0/my/path"));
        }
        payload.setPassword("password");
        return payload;
    }

    private Payload getSftpServerNamePayload() {
        final ImportPayload payload = new ImportPayload();
        payload.setSftpServerName(TEST_SFTP_SERVER);
        payload.setBackupPath("myBackup");
        return payload;
    }

    @Test
    public void completeJob_job_resetCM(){
        CMMediatorService cmMediatorService = EasyMock.createMock(CMMediatorService.class);
        expect(cmMediatorService.getCMMClient()).andReturn(cmmClient);
        cmmClient.setFlagEnabled(true);
        cmmClient.setInitialized(true);
        job.setCmMediatorService(cmMediatorService);
        expect(this.action.getPayload()).andReturn(getURIPayload(true));
        expect(action.getActionId()).andReturn("demo");
        expect(cmMediatorService.isConfigurationinCMM()).andReturn(true);
        job.setAction(this.action);
        replay(cmMediatorService, backupManager, this.action);
        job.triggerJob();
        job.completeJob();
        verify(backup);
        cmmClient.setFlagEnabled(false);
        cmmClient.setInitialized(false);
    }

    @Test
    public void failJob_job_resetCM(){
        CMMediatorService cmMediatorService = EasyMock.createMock(CMMediatorService.class);
        expect(cmMediatorService.getCMMClient()).andReturn(cmmClient);
        cmmClient.setFlagEnabled(true);
        cmmClient.setInitialized(true);
        job.setCmMediatorService(cmMediatorService);
        expect(this.action.getPayload()).andReturn(getURIPayload(true));
        expect(action.getActionId()).andReturn("demo");
        expect(cmMediatorService.isConfigurationinCMM()).andReturn(true);
        job.setAction(this.action);
        replay(cmMediatorService, backupManager, this.action);
        job.triggerJob();
        job.fail();
        verify(backup);
        cmmClient.setFlagEnabled(false);
        cmmClient.setInitialized(false);
    }
}
