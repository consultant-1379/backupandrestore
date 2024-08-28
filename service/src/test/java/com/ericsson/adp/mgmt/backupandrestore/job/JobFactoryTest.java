/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job;

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.CREATE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.DELETE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.EXPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.IMPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.RESTORE;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ericsson.adp.mgmt.backupandrestore.backup.BackupFolder;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.storage.StorageMetadataFileService;
import com.ericsson.adp.mgmt.backupandrestore.restore.FragmentFileService;

public class JobFactoryTest {

    private JobFactory jobFactory;
    private MockedAgentFactory agentMock;

    @Before
    public void setup() {
        jobFactory = new JobFactory();
        agentMock = new MockedAgentFactory();
    }

    @Test
    public void createJob_createBackupActionAndBackupManagerId_createBackupJob() throws Exception {
        final AgentRepository agentRepository = createMock(AgentRepository.class);
        expect(agentRepository.getAgents()).andReturn(Arrays.asList(agentMock.mockedAgent("defaultTestAgent", ""), agentMock.mockedAgent("defaultTestAgent", ""))).anyTimes();
        replay(agentRepository);
        jobFactory.setAgentRepository(agentRepository);
        jobFactory.setStorageMetadataFileService(createMock(StorageMetadataFileService.class));

        final Action action = mockAction(CREATE_BACKUP);

        final Job job = jobFactory.createJob(mockDefaultBackupManager(), action);

        assertTrue(job instanceof CreateBackupJob);
        final CreateBackupJob createBackupJob = (CreateBackupJob) job;
        assertEquals(2, createBackupJob.getAgents().size());
    }

    @Test
    public void createJob_ResetConfigJob() {
        final BackupManager brm = createMock(BackupManager.class);
        final Action action = createMock(Action.class);
        final PersistProviderFactory providerFactory = createMock(PersistProviderFactory.class);
        expect(action.getName()).andReturn(RESTORE).anyTimes();
        expect(brm.getBackupManagerId()).andReturn("DEFAULT-bro").anyTimes();
        expect(brm.isVirtual()).andReturn(true).anyTimes();
        expect(providerFactory.getPersistProvider()).andReturn(null).anyTimes();
        replay(brm, action, providerFactory);
        jobFactory.setPersistProviderFactory(providerFactory);
        final Job created = jobFactory.createJob(brm, action);
        assertTrue(created instanceof ResetConfigJob);
    }

    @Test
    public void createJob_createBackupActionAndNotDefaultBackupManager_createBackupJobWithOnlyAgentsThatMatchScope() throws Exception {
        final List<Agent> agents = Arrays.asList(agentMock.mockedAgent("defaultTestAgent", ""), agentMock.mockedAgent("defaultTestAgent2", ""), agentMock.mockedAgent("defaultTestAgent3", "A"), agentMock.mockedAgent("defaultTestAgent4", "A"));
        final AgentRepository agentRepository = createMock(AgentRepository.class);
        expect(agentRepository.getAgents()).andReturn(agents).anyTimes();
        replay(agentRepository);
        jobFactory.setAgentRepository(agentRepository);
        jobFactory.setStorageMetadataFileService(createMock(StorageMetadataFileService.class));

        final Action action = mockAction(CREATE_BACKUP);

        final Job job = jobFactory.createJob(mockScopedBackupManager("A", agents, false, true), action);

        assertTrue(job instanceof CreateBackupJob);
        final CreateBackupJob createBackupJob = (CreateBackupJob) job;
        assertEquals(2, createBackupJob.getAgents().size());
        assertEquals("A", createBackupJob.getAgents().get(0).getScope());
    }

    @Test
    public void createJob_deleteBackupActionAndBackupManagerId_createBackupJob() throws Exception {
        final AgentRepository agentRepository = createMock(AgentRepository.class);
        expect(agentRepository.getAgents()).andReturn(Arrays.asList(agentMock.mockedAgent("defaultTestAgent", ""), agentMock.mockedAgent("defaultTestAgent", "")));
        replay(agentRepository);
        jobFactory.setAgentRepository(agentRepository);

        final Action action = mockAction(DELETE_BACKUP);

        final Job job = jobFactory.createJob(mockDefaultBackupManager(), action);

        assertTrue(job instanceof DeleteBackupJob);
    }

    @Test
    public void createJob_housekeepingDeleteBackupActionAndBackupManagerId_DeleteBackupJob() throws Exception {
        final AgentRepository agentRepository = createMock(AgentRepository.class);
        expect(agentRepository.getAgents()).andReturn(Arrays.asList(agentMock.mockedAgent("defaultTestAgent", ""), agentMock.mockedAgent("defaultTestAgent", "")));
        replay(agentRepository);
        jobFactory.setAgentRepository(agentRepository);

        final Action action = mockAction(ActionType.HOUSEKEEPING_DELETE_BACKUP);

        final Job job = jobFactory.createJob(mockDefaultBackupManager(), action);

        assertTrue(job instanceof DeleteBackupJob);
    }

    @Test
    public void createJob_housekeepingActionAndBackupManagerId_HousekeepingJob() throws Exception {
        final AgentRepository agentRepository = createMock(AgentRepository.class);
        expect(agentRepository.getAgents()).andReturn(Arrays.asList(agentMock.mockedAgent("defaultTestAgent", ""), agentMock.mockedAgent("defaultTestAgent", ""))).anyTimes();
        replay(agentRepository);
        jobFactory.setAgentRepository(agentRepository);

        final Action action = mockAction(ActionType.HOUSEKEEPING);

        final Job job = jobFactory.createJob(mockDefaultBackupManager(), action);

        assertTrue(job instanceof HousekeepingJob);
    }

    @Test
    public void createJob_createRestoreActionAndBackupManagerId_createRestoreJob() throws Exception {
        final AgentRepository agentRepository = createMock(AgentRepository.class);
        expect(agentRepository.getAgents()).andReturn(Arrays.asList(agentMock.mockedAgent("agentA", ""), agentMock.mockedAgent("agentB", ""))).anyTimes();
        replay(agentRepository);
        jobFactory.setAgentRepository(agentRepository);
        jobFactory.setFragmentFileService(createMock(FragmentFileService.class));
        final Action action = mockAction(RESTORE);
        jobFactory.setBackupLocationService(mockBackupLocationService("agentA", "agentB"));

        final Job job = jobFactory.createJob(mockDefaultBackupManager(), action);

        assertTrue(job instanceof RestoreJob);
        final RestoreJob createRestoreJob = (RestoreJob) job;
        assertEquals(2, createRestoreJob.getAgents().size());
    }

    @Test
    public void createJob_createRestoreActionAndNotDefaultBackupManagerId_createRestoreJobWithOnlyAgentsThatMatchScope() throws Exception {
        final List<Agent> agents = Arrays.asList(agentMock.mockedAgent("defaultTestAgent", ""), agentMock.mockedAgent("agentA", "B"), agentMock.mockedAgent("defaultTestAgent2", ""), agentMock.mockedAgent("agentB", "B"));
        final AgentRepository agentRepository = createMock(AgentRepository.class);
        expect(agentRepository.getAgents()).andReturn(agents).anyTimes();
        replay(agentRepository);
        jobFactory.setAgentRepository(agentRepository);
        jobFactory.setBackupLocationService(mockBackupLocationService("agentA", "agentB"));

        final Action action = mockAction(RESTORE);

        final Job job = jobFactory.createJob(mockScopedBackupManager("B", agents, false, true), action);

        assertTrue(job instanceof RestoreJob);
        final RestoreJob createRestoreJob = (RestoreJob) job;
        assertEquals(2, createRestoreJob.getAgents().size());
        assertEquals("B", createRestoreJob.getAgents().get(0).getScope());
        assertEquals("B", createRestoreJob.getAgents().get(1).getScope());
    }

    @Test
    public void createJob_createRestoreAction_virtualBackupManager() throws Exception {
        final List<Agent> agents = Arrays.asList(agentMock.mockedAgent("bravo", "DEFAULT-bravo"), agentMock.mockedAgent("echo", "DEFAULT-echo"));
        final AgentRepository agentRepository = createMock(AgentRepository.class);
        expect(agentRepository.getAgents()).andReturn(agents).anyTimes();
        replay(agentRepository);
        jobFactory.setAgentRepository(agentRepository);
        jobFactory.setBackupLocationService(mockBackupLocationService("bravo"));

        final Action action = mockAction(RESTORE);
        final BackupManager virtualBackupManager = mockScopedBackupManager("DEFAULT-bravo", agents, true, false);
        final Job job = jobFactory.createJob(virtualBackupManager, action);

        assertTrue(job instanceof RestoreJob);
        final RestoreJob createRestoreJob = (RestoreJob) job;
        assertEquals(1, createRestoreJob.getAgents().size());
        assertEquals("DEFAULT-bravo", createRestoreJob.getAgents().get(0).getScope());
    }

    @Test
    public void createJob_createRestoreWithNewAgentsInSameScope_createRestoreJobWithOnlyOriginalAgents() throws Exception {
        final List<Agent> originalAgents = Arrays.asList(agentMock.mockedAgent("agentA", "A"), agentMock.mockedAgent("agentB", "A"));
        final List<Agent> newAgents = Arrays.asList(agentMock.mockedAgent("agentC", "A"), agentMock.mockedAgent("agentD", "A"));
        final List<Agent> allAgents = new ArrayList<>(originalAgents);
        allAgents.addAll(newAgents);
        final AgentRepository agentRepository = createMock(AgentRepository.class);
        expect(agentRepository.getAgents()).andReturn(allAgents).anyTimes();

        replay(agentRepository);
        jobFactory.setAgentRepository(agentRepository);
        jobFactory.setBackupLocationService(mockBackupLocationService("agentA", "agentB"));

        final Action action = mockAction(RESTORE);
        final RestoreJob job = (RestoreJob) jobFactory.createJob(mockScopedBackupManager("A", allAgents, false, true), action);
        assertEquals(2, job.getAgents().size());
        final List<String> agentIds = job.getAgents().stream().map(Agent::getAgentId).collect(Collectors.toList());
        originalAgents.forEach(agent -> assertTrue(agentIds.contains(agent.getAgentId())));
    }

    @Test
    public void createJob_noAgentRegistered_createBackupWithoutAgentJob() throws Exception {

        final AgentRepository agentRepository = createMock(AgentRepository.class);
        expect(agentRepository.getAgents()).andReturn(new ArrayList<Agent>());
        replay(agentRepository);
        jobFactory.setAgentRepository(agentRepository);
        jobFactory.setStorageMetadataFileService(createMock(StorageMetadataFileService.class));

        final Action action = mockAction(CREATE_BACKUP);

        final Job job = jobFactory.createJob(mockDefaultBackupManager(), action);
        assertTrue(job instanceof AgentNotRegisteredJob);
    }

    @Test
    public void restoreJob_noAgentRegistered_createBackupWithoutAgentJob() throws Exception {

        final AgentRepository agentRepository = createMock(AgentRepository.class);
        expect(agentRepository.getAgents()).andReturn(new ArrayList<Agent>());
        replay(agentRepository);
        jobFactory.setAgentRepository(agentRepository);

        final Action action = mockAction(RESTORE);

        final Job job = jobFactory.createJob(mockDefaultBackupManager(), action);
        assertTrue(job instanceof AgentNotRegisteredJob);
    }

    @Test
    public void createJob_createExportAction_createExportJob() throws Exception {

        final Action action = mockAction(EXPORT);

        final Job job = jobFactory.createJob(mockDefaultBackupManager(), action);
        assertTrue(job instanceof ExportBackupJob);
    }

    @Test
    public void createJob_createImportAction_createImportJob() throws Exception {

        final Action action = mockAction(IMPORT);

        final Job job = jobFactory.createJob(mockDefaultBackupManager(), action);
        assertTrue(job instanceof ImportBackupJob);
    }

    private Action mockAction(final ActionType actionType) {
        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName("Test_Backup");
        final Action action = createMock(Action.class);
        expect(action.getName()).andReturn(actionType).anyTimes();
        expect(action.getBackupName()).andReturn("Test_Backup").anyTimes();
        expect(action.isExecutedAsTask()).andReturn(false).anyTimes();
        expect(action.getPayload()).andReturn(payload);
        expectLastCall();
        replay(action);
        return action;
    }

    private BackupManager mockDefaultBackupManager() {
        final BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        expect(backupManager.ownsAgent(anyObject(String.class), anyObject(String.class))).andReturn(true).anyTimes();
        replay(backupManager);
        return backupManager;
    }

    private BackupManager mockScopedBackupManager(final String scope, final List<Agent> agents, final boolean isVirtual, final boolean ownsBackup) {
        final BackupManager backupManager = createMock(BackupManager.class);
        final BackupManager parentManager = createMock(BackupManager.class);

        expect(backupManager.isVirtual()).andReturn(isVirtual).anyTimes();
        agents.forEach(a -> {
            expect(backupManager.ownsAgent(a.getScope(), a.getAgentId())).andReturn(scope.equals(a.getScope())).anyTimes();
        });
        expect(backupManager.getBackupManagerId()).andReturn(scope).anyTimes();

        if (isVirtual) {
            expect(backupManager.ownsBackup("Test_Backup")).andReturn(ownsBackup).anyTimes();
        }
        expect(backupManager.getParent()).andReturn(Optional.of(parentManager)).anyTimes();

        replay(backupManager);
        return backupManager;
    }

    private BackupLocationService mockBackupLocationService(final String... agentIdsForBackup) {
        final BackupManager backupManager = createMock(BackupManager.class);
        final BackupLocationService backupLocationService = createMock(BackupLocationService.class);
        expect(backupLocationService.getBackupFolder(backupManager, "backupFolder")).andReturn(new BackupFolder(Path.of("backupFolder"))).anyTimes();
        expect(backupLocationService.getAgentIdsForBackup(anyObject(), anyString())).andReturn(Arrays.asList(agentIdsForBackup));
        replay(backupLocationService);
        return backupLocationService;
    }

}
