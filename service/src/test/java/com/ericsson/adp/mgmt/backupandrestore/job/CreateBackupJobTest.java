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
package com.ericsson.adp.mgmt.backupandrestore.job;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.discovery.AgentDiscoveryService;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupFolder;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.storage.StorageMetadataFileService;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMClient;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.exception.JobFailedException;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnauthorizedDataChannelException;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStage;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.PreparingBackupJobStage;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import com.ericsson.adp.mgmt.control.StageComplete;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.metadata.Fragment;
import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.EXPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ResultType.SUCCESS;
import static com.ericsson.adp.mgmt.backupandrestore.util.YangEnabledDisabled.ENABLED;
import static org.easymock.EasyMock.anyDouble;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CreateBackupJobTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CreateBackupJob job;
    private List<Agent> agents;
    private Backup backup;
    private BackupManager backupManager;
    private Action action;
    private ActionRepository actionRepository;
    private StorageMetadataFileService storageMetadataFileService;
    private NotificationService notificationService;
    private MockedAgentFactory agentMock;
    private CMMediatorService cmMediatorService;
    private CMMClient cmmClient;

    private void setUp(final Boolean isAgentFailureCase) {
        cmmClient = new CMMClient();
        cmmClient.setFlagEnabled(true);
        cmmClient.setMaxAttempts("5");
        cmmClient.setMaxDelay("3000");
        job = new CreateBackupJob();
        agentMock = new MockedAgentFactory();
        cmMediatorService = createMock(CMMediatorService.class);
        job.setCmMediatorService(cmMediatorService);

        backup = createMock(Backup.class);
        expect(backup.getName()).andReturn("myBackup").anyTimes();

        backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn("123").anyTimes();
        expectLastCall();
        actionRepository = createMock(ActionRepository.class);
        action = createMock(Action.class);
        agents = Arrays.asList(agentMock.mockedAgent("1", isAgentFailureCase, job, false), agentMock.mockedAgent("2", isAgentFailureCase, job, false));
        final AgentDiscoveryService agentDiscoveryService = createMock(AgentDiscoveryService.class);
        agentDiscoveryService.validateRegisteredAgents(backupManager, agents);
        expectLastCall();

        final BackupRepository backupRepository = createMock(BackupRepository.class);
        expect(backupRepository.createBackup(backupManager, action, agents)).andReturn(backup);
        backupRepository.deleteBackup(backup, backupManager);
        expectLastCall();

        // Create a false scheduler for the backup manager with auto export enabled
        Scheduler scheduler = createMock(Scheduler.class);
        expect(scheduler.getAutoExport()).andReturn(ENABLED).anyTimes();
        expect(scheduler.getAutoExportUri()).andReturn(URI.create("http://user@fake.url:22/blah")).anyTimes();
        expect(scheduler.getAutoExportPassword()).andReturn("fake-pass").anyTimes();
        expect(scheduler.getSftpServerName()).andReturn("").anyTimes();

        expect(backupManager.getScheduler()).andReturn(scheduler).anyTimes();

        expect(cmMediatorService.getCMMClient()).andReturn(cmmClient).anyTimes();
        cmMediatorService.enqueueProgressReport(EasyMock.anyObject());
        expectLastCall().anyTimes();

        replay(backupManager, backupRepository, agentDiscoveryService, scheduler, cmMediatorService);

        notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionStarted(action);
        expectLastCall();

        storageMetadataFileService = createMock(StorageMetadataFileService.class);
        job.setStorageMetadataFileService(storageMetadataFileService);

        final JobStage<CreateBackupJob> stage = new PreparingBackupJobStage(agents, job, notificationService);
        job.setAgents(agents);
        job.setBackupManager(backupManager);
        job.setAction(action);
        job.setAgentDiscoveryService(agentDiscoveryService);
        job.setBackupRepository(backupRepository);
        job.setNotificationService(notificationService);
        job.setJobStage(stage);
        job.setActionRepository(actionRepository);
    }

    @Test
    public void triggerJob_backupManagerAction_createsBackupAndTriggersBackupActionsOnAgents() throws Exception {
        setUp(false);

        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        action.setProgressPercentage(0.17);
        expectLastCall();
        action.setProgressPercentage(0.33);
        expectLastCall();
        action.setProgressPercentage(0.5);
        expectLastCall();
        action.setProgressPercentage(0.67);
        expectLastCall();
        action.setProgressPercentage(0.83);
        expectLastCall();
        action.setProgressPercentage(1.0);
        expectLastCall();
        action.persist();
        expectLastCall().anyTimes();
        action.addMessage(anyString());
        expectLastCall().anyTimes();

        replay(backup, notificationService, action);
        job.triggerJob();
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));

        verify(backupManager);
        verify(backup);
        verify(action);
        agents.forEach(EasyMock::verify);
    }

    @Test
    public void didFinish_agentsWithBackupStillOngoing_didNotFinish() throws Exception {
        setUp(true);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        replay(notificationService, action);

        job.triggerJob();

        assertFalse(job.didFinish());
    }

    @Test
    public void didFinish_allAgentsFinishedAllStagesSuccessfully_didFinish() throws Exception {
        setUp(false);

        backup.persist();
        expectLastCall();
        replay(backup, notificationService);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        expectLastCall();
        action.setProgressPercentage(0.17);
        expectLastCall();
        action.setProgressPercentage(0.33);
        expectLastCall();
        action.setProgressPercentage(0.5);
        expectLastCall();
        action.setProgressPercentage(0.67);
        expectLastCall();
        action.setProgressPercentage(0.83);
        expectLastCall();
        action.setProgressPercentage(1.0);
        expectLastCall();
        action.persist();
        expectLastCall().times(6);
        action.addMessage(anyString());
        expectLastCall().times(6);
        replay(action);
        job.triggerJob();

        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));

        assertTrue(job.didFinish());
        verify(action);
    }

    @Test
    public void didFinish_allAgentsFinishedPreparationStageUnsuccessfully_didFinish() throws Exception {
        setUp(true);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getResult()).andReturn(SUCCESS).anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();

        expect(action.getProgressInfo()).andReturn(null).anyTimes();
        action.setProgressPercentage(anyDouble());
        expectLastCall().anyTimes();
        action.persist();
        expectLastCall().anyTimes();
        action.setProgressInfo(anyString());
        expectLastCall().anyTimes();
        replay(notificationService, action);

        job.triggerJob();

        job.updateProgress("1", getStageCompleteMessage(false));
        job.updateProgress("2", getStageCompleteMessage(false));
        //Stage complete for cancel stage
        job.updateProgress("1", getStageCompleteMessage(false));
        job.updateProgress("2", getStageCompleteMessage(false));

        assertTrue(job.didFinish());
    }

    @Test
    public void didFinish_oneOutOfTwoAgentsFinished_didNotFinish() throws Exception {
        setUp(true);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.setProgressPercentage(anyDouble());
        expectLastCall().anyTimes();
        action.persist();
        expectLastCall().anyTimes();
        replay(action, notificationService);

        job.triggerJob();

        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));

        assertFalse(job.didFinish());
    }

    @Test
    public void didFinish_twoAgentsFinishedPreparation_didNotFinish() throws Exception {
        setUp(false);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.setProgressPercentage(0.17);
        action.setProgressPercentage(0.33);
        action.persist();
        expectLastCall().anyTimes();
        replay(action, notificationService);

        job.triggerJob();

        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));

        assertFalse(job.didFinish());
    }

    @Test
    public void completeJob_backupFinishedAllStages_persistBackup() throws Exception {
        setUp(false);
        backup.setStatus(BackupStatus.COMPLETE);
        expectLastCall();
        backup.persist();
        expectLastCall().anyTimes();
        replay(backup);

        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup");
        expectLastCall();
        action.setProgressPercentage(0.17);
        expectLastCall();
        action.setProgressPercentage(0.33);
        expectLastCall();
        action.setProgressPercentage(0.5);
        expectLastCall();
        action.setProgressPercentage(0.67);
        expectLastCall();
        action.setProgressPercentage(0.83);
        expectLastCall();
        action.setProgressPercentage(1.0);
        expectLastCall();
        action.persist();
        expectLastCall().times(6);
        action.addMessage(EasyMock.anyString());
        expectLastCall().times(6);
        expect(action.isScheduledEvent()).andReturn(false);
        expectLastCall().atLeastOnce();
        expect(action.getName()).andReturn(EXPORT);
        expectLastCall().anyTimes();
        replay(action);

        job.triggerJob();

        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));

        job.completeJob();

        verify(backup);
    }

    @Test(expected = JobFailedException.class)
    public void completeJob_notAllAgentsSucceeded_throwsException() throws Exception {
        setUp(true);

        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.setProgressPercentage(anyDouble());
        expectLastCall().anyTimes();
        action.persist();
        expectLastCall().anyTimes();
        notificationService.notifyAllActionFailed(action);
        expectLastCall();
        expect(action.getProgressInfo()).andReturn(null).anyTimes();
        action.setProgressInfo(anyString());
        expectLastCall().anyTimes();

        replay(action, notificationService);

        job.triggerJob();

        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(false));

        job.completeJob();
    }

    @Test(expected = JobFailedException.class)
    public void completeJob_notAllStagesFinished_throwsException() throws Exception {
        setUp(true);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.setProgressPercentage(0.0);
        action.setProgressPercentage(0.17);
        action.setProgressPercentage(0.33);
        action.persist();
        expectLastCall().anyTimes();
        replay(action, backup);

        job.triggerJob();
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));

        job.completeJob();
    }

    @Test
    public void fail_backupFailed_setsBackupAsCorruptedAndUpdatesProgressPercentage() throws Exception {
        setUp(true);
        backup.setStatus(BackupStatus.CORRUPTED);
        expectLastCall();
        backup.persist();
        expectLastCall();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        action.setProgressPercentage(0.17);
        expectLastCall().times(2);
        action.persist();
        expectLastCall().times(2);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();
        action.addMessage(EasyMock.anyString());
        expectLastCall();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        replay(backup, action, notificationService);

        job.triggerJob();

        job.updateProgress("1", getStageCompleteMessage(true));

        job.fail();

        verify(backup);
        verify(action);
    }

    @Test
    public void getFragmentFolder_fragmentIdThatAlreadyExists_returnsWhereToStoreFragment() throws Exception {
        setUp(false);
        replay(backup, notificationService);

        final Path expectedLocation = Paths.get(folder.getRoot().getAbsolutePath());
        final Metadata metadata = getMetadata("1", "myBackup");
        final BackupLocationService backupLocationService = createMock(BackupLocationService.class);
        final FragmentFolder fragmentFolder = createMock(FragmentFolder.class);

        expect(backupLocationService.getFragmentFolder(metadata, backupManager.getBackupManagerId(), metadata.getBackupName() )).andReturn(fragmentFolder);
        expect(backupLocationService.getBackupFolder(backupManager, metadata.getBackupName())).andReturn(new BackupFolder(Paths.get(folder.getRoot().getAbsolutePath()).resolve("123"))).anyTimes();
        expect(fragmentFolder.getRootFolder()).andReturn(expectedLocation);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        replay(backupLocationService, action, fragmentFolder);
        job.triggerJob();
        job.setBackupLocationService(backupLocationService);

        assertEquals(fragmentFolder, job.getFragmentFolder(metadata));
    }

    @Test
    public void getFragmentFolder_metadataBelongingToOngoingBackup_returnsWhereToStoreFragmentAndCreatesFragmentFolder() throws Exception {
        setUp(false);
        replay(backup, notificationService);

        final Path expectedLocation = Paths.get(folder.getRoot().getAbsolutePath()).resolve("fragmentId");
        final Metadata metadata = getMetadata("1", "myBackup");
        final BackupLocationService backupLocationService = createMock(BackupLocationService.class);
        final BackupFolder backupFolder = createMock(BackupFolder.class);
        final FragmentFolder fragmentFolder = createMock(FragmentFolder.class);
        expect(backupLocationService.getFragmentFolder(metadata, backupManager.getBackupManagerId(), metadata.getBackupName())).andReturn(fragmentFolder);
        expect(backupLocationService.getBackupFolder(backupManager.getBackupManagerId(), metadata.getBackupName())).andReturn(new BackupFolder(Paths.get(folder.getRoot().getAbsolutePath()).resolve("123"))).anyTimes();
        expect(fragmentFolder.getRootFolder()).andReturn(expectedLocation);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();

        replay(backupLocationService, action, backupFolder, fragmentFolder);
        job.triggerJob();
        job.setBackupLocationService(backupLocationService);

        assertEquals(fragmentFolder, job.getFragmentFolder(metadata));
    }

    @Test(expected = UnauthorizedDataChannelException.class)
    public void getFragmentFolder_metadataFromAgentNotBelongingToBackup_throwsException() throws Exception {
        setUp(false);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        replay(backup, action, notificationService);

        job.triggerJob();

        job.getFragmentFolder(getMetadata("3", "myBackup"));
    }

    @Test(expected = UnauthorizedDataChannelException.class)
    public void getFragmentFolder_metadataFromBackupDifferentThanOngoingBackup_throwsException() throws Exception {
        setUp(false);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        replay(backup, action, notificationService);

        job.triggerJob();

        job.getFragmentFolder(getMetadata("1", "notMyBackup"));
    }

    @Test
    public void receiveNewFragment_fragmentReceivedInExecutionStage_jobDidNotFinish() {
        setUp(false);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.setProgressPercentage(0.17);
        action.setProgressPercentage(0.33);
        expectLastCall().anyTimes();
        action.setProgressPercentage(0.5);
        expectLastCall().anyTimes();
        action.persist();
        expectLastCall().anyTimes();
        replay(action, backup, notificationService);

        job.triggerJob();

        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.receiveNewFragment("1", "fragmentA");
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));

        assertFalse(job.didFinish());

        verify(backup);
    }

    @Test(expected = JobFailedException.class)
    public void fragmentFailed_fragmentFailedInExecutionStage_jobDidFinishButFailsToComplete() {
        setUp(true);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.setProgressPercentage(0.17);
        expectLastCall();
        action.setProgressPercentage(0.33);
        expectLastCall().anyTimes();
        action.setProgressPercentage(0.5);
        expectLastCall().anyTimes();
        action.persist();
        expectLastCall().anyTimes();
        replay(action, backup, notificationService);

        job.triggerJob();
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.receiveNewFragment("1", "fragmentA");
        job.fragmentSucceeded("1", "fragmentA");
        job.updateProgress("1", getStageCompleteMessage(true));

        job.receiveNewFragment("2", "fragmentA");
        job.fragmentFailed("2", "fragmentA");
        job.updateProgress("2", getStageCompleteMessage(true));
        //Cancel stage
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        assertTrue(job.didFinish());
        job.completeJob();

        verify(backup);
    }

    @Test
    public void fragmentSucceeded_allAgentsAndFragmentsSucceedInExecutionStage_jobDidFinishAndCompletes() {
        setUp(false);
        backup.persist();
        expectLastCall();
        backup.setStatus(BackupStatus.COMPLETE);
        expectLastCall();

        expect(action.getBackupName()).andReturn("myBackup");
        expectLastCall().anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        action.setProgressPercentage(0.17);
        expectLastCall();
        action.setProgressPercentage(0.33);
        expectLastCall().times(3);
        action.setProgressPercentage(0.5);
        expectLastCall().times(3);
        action.setProgressPercentage(0.67);
        expectLastCall();
        action.setProgressPercentage(0.83);
        expectLastCall();
        action.setProgressPercentage(1.0);
        expectLastCall();
        action.addMessage(EasyMock.anyString());
        expectLastCall().anyTimes();
        action.persist();
        expectLastCall().anyTimes();
        expect(action.isScheduledEvent()).andReturn(false).atLeastOnce();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        replay(action, backup, notificationService);

        job.triggerJob();
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.receiveNewFragment("1", "fragmentA");
        job.fragmentSucceeded("1", "fragmentA");
        job.updateProgress("1", getStageCompleteMessage(true));

        job.receiveNewFragment("2", "fragmentA");
        job.fragmentSucceeded("2", "fragmentA");
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));

        assertTrue(job.didFinish());
        job.completeJob();

        verify(action);
        verify(backup);
    }

    @Test
    public void handleUnexpectedDataChannel_metadataFromAgentNotParticipatingInJob_doesNothing() {
        setUp(false);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        action.persist();
        expectLastCall();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        action.setProgressPercentage(0.17d);
        expectLastCall();
        action.addMessage(EasyMock.anyString());
        expectLastCall();
        replay(action, notificationService);

        job.triggerJob();
        job.updateProgress("1", getStageCompleteMessage(true));
        job.handleUnexpectedDataChannel(Metadata.newBuilder().setAgentId("3").build());

        verify(action);
    }

    @Test
    public void handleUnexpectedDataChannel_metadataFromAgentParticipatingInJob_failsAgent() {
        setUp(true);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        action.setProgressPercentage(0.17d);
        expectLastCall().times(2);
        action.persist();
        expectLastCall().times(2);
        action.addMessage(EasyMock.anyString());
        expectLastCall();
        replay(action, notificationService);

        job.triggerJob();
        job.updateProgress("1", getStageCompleteMessage(true));
        job.handleUnexpectedDataChannel(Metadata.newBuilder().setAgentId("2").build());

        verify(action);
    }

    @Test
    public void agentFails_autoDeleteEnabled_expectedLocationDoesNotExist(){
        setUp(true);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.persist();
        expectLastCall().anyTimes();
        action.setProgressPercentage(anyDouble());
        expectLastCall().anyTimes();
        action.setState(anyObject());
        expectLastCall();
        action.setResult(anyObject());
        expectLastCall();
        expect(action.getProgressInfo()).andReturn(null).anyTimes();
        action.setProgressInfo(anyString());
        expectLastCall().anyTimes();

        replay(notificationService, action);
        backup.setStatus(BackupStatus.CORRUPTED);
        expectLastCall();
        backup.persist();
        expectLastCall().anyTimes();
        expect(backup.getBackupId()).andReturn("myBackup");
        replay(backup);

        final Path expectedLocation = Paths.get(folder.getRoot().getAbsolutePath()).resolve("fragmentId");
        final Metadata metadata = getMetadata("1", "myBackup");
        final BackupLocationService backupLocationService = createMock(BackupLocationService.class);
        final BackupFolder backupFolder = createMock(BackupFolder.class);
        final FragmentFolder fragmentFolder = createMock(FragmentFolder.class);
        expect(backupLocationService.getBackupFolder("123", metadata.getBackupName())).andReturn(backupFolder).anyTimes();
        expect(backupFolder.getFragmentFolder(metadata)).andReturn(fragmentFolder);
        expect(backupFolder.getBackupLocation()).andReturn(Paths.get(folder.getRoot().getAbsolutePath()).resolve("123")).anyTimes();
        expect(fragmentFolder.getRootFolder()).andReturn(expectedLocation).anyTimes();
        replay(backupLocationService, backupFolder, fragmentFolder);

        job.setAutoDeleteFailures(true);
        job.setBackupLocationService(backupLocationService);

        job.triggerJob();
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(false));
        job.fail();

        assertFalse(expectedLocation.toFile().exists());
    }

    @Test
    public void createBackup_Scheduled_AutoExportEnabled_ExportActionReturned(){
        setUp(false);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        action.setProgressPercentage(0.17);
        expectLastCall();
        action.setProgressPercentage(0.33);
        expectLastCall();
        action.setProgressPercentage(0.5);
        expectLastCall();
        action.setProgressPercentage(0.67);
        expectLastCall();
        action.setProgressPercentage(0.83);
        expectLastCall();
        action.setProgressPercentage(1.0);
        expectLastCall();
        action.persist();
        expectLastCall().anyTimes();
        action.addMessage(anyString());
        expectLastCall().anyTimes();

        // Make this backup a scheduled backup
        expect(action.isScheduledEvent()).andReturn(true).anyTimes();

        // As we're completing this job, these will be called
        backup.setStatus(anyObject(BackupStatus.class));
        expectLastCall().anyTimes();
        backup.persist();
        expectLastCall().anyTimes();

        replay(backup, notificationService, action);

        job.triggerJob();
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("1", getStageCompleteMessage(true));
        job.updateProgress("2", getStageCompleteMessage(true));
        job.completeJob();

        final List<CreateActionRequest> postExecActions = job.getPostExecutionActions();
        final Optional<CreateActionRequest> request = postExecActions.stream().filter(e -> e.getAction() == EXPORT).findFirst();
        assertTrue(request.isPresent());
        assertTrue(request.get().isScheduledEvent());
    }

    private StageComplete getStageCompleteMessage(final boolean success) {
        return StageComplete.newBuilder().setSuccess(success).setMessage("boo").build();
    }

    private Metadata getMetadata(final String agentId, final String backupName) {
        return Metadata.newBuilder().setAgentId(agentId)
                .setFragment(Fragment.newBuilder().setFragmentId("fragmentId").setSizeInBytes("bytes").setVersion("version"))
                .setBackupName(backupName).build();
    }
}
