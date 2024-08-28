/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job.stage;

import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.action.Action.REGISTER;
import static com.ericsson.adp.mgmt.backupandrestore.action.ResultType.FAILURE;
import static com.ericsson.adp.mgmt.backupandrestore.action.ResultType.SUCCESS;
import static org.easymock.EasyMock.anyDouble;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEventFileService;
import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentRepository;
import com.ericsson.adp.mgmt.backupandrestore.agent.VBRMAutoCreate;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.CancelingActionState;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.ExecutingBackupState;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.PostActionBackupState;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.PreparingBackupState;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.RecognizedState;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.BackupManagerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.HousekeepingFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.VirtualInformationFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerFileService;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NoNotifiersService;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.control.StageComplete;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;

public class JobStagesStateMachineTest {

    private JobStage currentStage;

    @Test
    public void singleV2() throws InterruptedException {
        //Set up agents
        final List<Agent> mockAgents = Arrays.asList(createAgent("V2", ApiVersion.API_V2_0));

        //Create a Job
        final CreateBackupJobTest createBackupJob = new CreateBackupJobTest();
        final Action action = createMock(Action.class);
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.setProgressPercentage(anyDouble());
        expectLastCall().anyTimes();
        expect(action.getResult()).andReturn(SUCCESS).anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getBackupName()).andReturn(createBackupJob.getBackupName()).once();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        action.persist();
        expectLastCall().anyTimes();
        replay(action);
        createBackupJob.setAction(action);

        final BackupManager brm = createMock(BackupManager.class);
        expect(brm.getAgentVisibleBRMId()).andReturn("some-brm-id").anyTimes();
        replay(brm);
        createBackupJob.setBackupManager(brm);

        //set first stage (Fake REST/CMM Call)
        currentStage = new ExecutingBackupJobStageV2(mockAgents, createBackupJob, new NoNotifiersService());
        createBackupJob.setJobStage(currentStage);

        //Check V2 agent in correct starting state.
        final Agent agentV2 = (Agent)currentStage.getAgents().get(0);
        assertEquals(RecognizedState.class, agentV2.getState().getClass());
        assertEquals(ApiVersion.API_V2_0,agentV2.getApiVersion());

        currentStage.trigger();

        assertEquals(PreparingBackupState.class,agentV2.getState().getClass());

        assertJobStage(ExecutingBackupJobStageV2.class, createBackupJob.getJobStage().getClass());

        agentV2.processMessage(getStageCompleteMessage(BACKUP,true));

        assertJobStage(CompletedBackupJobStage.class, createBackupJob.getJobStage().getClass());
        assertEquals(RecognizedState.class, agentV2.getState().getClass());

        verify(action);
    }

    @Test
    public void onlyV2() throws InterruptedException {
        //Set up agents
        final List<Agent> mockAgents = Arrays.asList(createAgent("One", ApiVersion.API_V2_0), createAgent("Two", ApiVersion.API_V2_0));

        //Create a Job
        final CreateBackupJobTest createBackupJob = new CreateBackupJobTest();
        final Action action = createMock(Action.class);
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.setProgressPercentage(anyDouble());
        expectLastCall().anyTimes();
        expect(action.getResult()).andReturn(SUCCESS).anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getBackupName()).andReturn(createBackupJob.getBackupName()).once();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        action.persist();
        expectLastCall().anyTimes();
        replay(action);
        createBackupJob.setAction(action);

        final BackupManager brm = createMock(BackupManager.class);
        expect(brm.getAgentVisibleBRMId()).andReturn("some-brm-id").anyTimes();
        replay(brm);
        createBackupJob.setBackupManager(brm);

        //set first stage (Fake REST/CMM Call)
        currentStage = new ExecutingBackupJobStageV2(mockAgents, createBackupJob, new NoNotifiersService());
        createBackupJob.setJobStage(currentStage);

        final Agent agentOne = (Agent)currentStage.getAgents().get(0);
        assertEquals(RecognizedState.class, agentOne.getState().getClass());
        assertEquals(ApiVersion.API_V2_0,agentOne.getApiVersion());

        final Agent agentTwo = (Agent)currentStage.getAgents().get(1);
        assertEquals(RecognizedState.class, agentTwo.getState().getClass());
        assertEquals(ApiVersion.API_V2_0, agentTwo.getApiVersion());

        currentStage.trigger();

        assertEquals(PreparingBackupState.class, agentOne.getState().getClass());
        assertEquals(PreparingBackupState.class, agentTwo.getState().getClass());

        assertJobStage(ExecutingBackupJobStageV2.class, createBackupJob.getJobStage().getClass());

        agentOne.processMessage(getStageCompleteMessage(BACKUP,true));
        agentTwo.processMessage(getStageCompleteMessage(BACKUP,true));

        assertJobStage(CompletedBackupJobStage.class, createBackupJob.getJobStage().getClass());
        assertEquals(RecognizedState.class, agentOne.getState().getClass());
        assertEquals(RecognizedState.class, agentTwo.getState().getClass());

        verify(action);
    }

    @Test
    public void v2andv3() {
        //Set up agents
        final List<Agent> mockAgents = Arrays.asList(createAgent("V2", ApiVersion.API_V2_0), createAgent("V3", ApiVersion.API_V3_0));

        //Create a Job
        final CreateBackupJobTest createBackupJob = new CreateBackupJobTest();
        final Action action = createMock(Action.class);
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.setProgressPercentage(anyDouble());
        expectLastCall().anyTimes();
        expect(action.getResult()).andReturn(SUCCESS).anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getBackupName()).andReturn(createBackupJob.getBackupName()).once();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        action.persist();
        expectLastCall().anyTimes();
        replay(action);
        createBackupJob.setAction(action);

        final BackupManager brm = createMock(BackupManager.class);
        expect(brm.getAgentVisibleBRMId()).andReturn("some-brm-id").anyTimes();
        replay(brm);
        createBackupJob.setBackupManager(brm);

        //set first stage (Fake REST/CMM Call)
        currentStage = new PreparingBackupJobStage(mockAgents, createBackupJob, new NoNotifiersService());
        createBackupJob.setJobStage(currentStage);

        //Check V2 agent in correct starting state.
        final Agent agentV2 = (Agent)currentStage.getAgents().get(0);
        assertEquals(RecognizedState.class, agentV2.getState().getClass());
        assertEquals(ApiVersion.API_V2_0,agentV2.getApiVersion());

        //Check V3 agent in correct starting state.
        final Agent agentV3 = (Agent)currentStage.getAgents().get(1);
        assertEquals(RecognizedState.class, agentV3.getState().getClass());
        assertEquals(ApiVersion.API_V3_0,agentV3.getApiVersion());

        //Trigger backup process
        currentStage.trigger();

        assertEquals(RecognizedState.class, agentV2.getState().getClass());
        assertEquals(PreparingBackupState.class, agentV3.getState().getClass());

        //Expect message from V3 agent, no message from V2 agent required to progress to ExecutingStage.
        agentV3.processMessage(getStageCompleteMessage(BACKUP,true));

        assertEquals(PreparingBackupState.class,agentV2.getState().getClass());
        assertEquals(ExecutingBackupState.class, agentV3.getState().getClass());

        assertJobStage(ExecutingBackupJobStage.class, createBackupJob.getJobStage().getClass());

        //Expect message from V2 and V3 agent in order to progress to PostBackupJobStage.
        agentV2.processMessage(getStageCompleteMessage(BACKUP,true));
        agentV3.processMessage(getStageCompleteMessage(BACKUP,true));

        assertJobStage(PostActionBackupJobStage.class, createBackupJob.getJobStage().getClass());
        assertEquals(RecognizedState.class, agentV2.getState().getClass());
        assertEquals(PostActionBackupState.class, agentV3.getState().getClass());

        //Expect message from V3 agent, backup should succeed all agents return to RecognizedState.
        agentV3.processMessage(getStageCompleteMessage(BACKUP,true));
        assertEquals(RecognizedState.class, agentV2.getState().getClass());
        assertEquals(RecognizedState.class, agentV3.getState().getClass());

        verify(action);
    }

    @Test
    public void v3FailInPrep() {
        //Set up agents
        final List<Agent> mockAgents = Arrays.asList(createAgent("V3_1", ApiVersion.API_V3_0), createAgent("V3_2", ApiVersion.API_V3_0));

        //Create a Job
        final CreateBackupJobTest createBackupJob = new CreateBackupJobTest();
        final Action action = createMock(Action.class);
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.setProgressPercentage(anyDouble());
        expectLastCall().anyTimes();
        expect(action.getResult()).andReturn(FAILURE).anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getBackupName()).andReturn(createBackupJob.getBackupName()).once();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        action.persist();
        expectLastCall().anyTimes();

        expect(action.getProgressInfo()).andReturn(null).anyTimes();
        action.setProgressInfo(anyString());
        expectLastCall().anyTimes();

        replay(action);
        createBackupJob.setAction(action);

        final CMMediatorService cmMediatorService = createMock(CMMediatorService.class);
        createBackupJob.setCmMediatorService(cmMediatorService);
        final BackupManager brm = createMock(BackupManager.class);
        expect(brm.getAgentVisibleBRMId()).andReturn("some-brm-id").anyTimes();
        replay(brm);
        createBackupJob.setBackupManager(brm);

        //set first stage (Fake REST/CMM Call)
        currentStage = new PreparingBackupJobStage(mockAgents, createBackupJob, new NoNotifiersService());
        createBackupJob.setJobStage(currentStage);

        //Check V2 agent in correct starting state.
        final Agent agentV3_1 = (Agent)currentStage.getAgents().get(0);
        assertEquals(RecognizedState.class, agentV3_1.getState().getClass());
        assertEquals(ApiVersion.API_V3_0,agentV3_1.getApiVersion());

        //Check V3 agent in correct starting state.
        final Agent agentV3_2 = (Agent)currentStage.getAgents().get(1);
        assertEquals(RecognizedState.class, agentV3_2.getState().getClass());
        assertEquals(ApiVersion.API_V3_0,agentV3_2.getApiVersion());

        //Trigger backup process
        currentStage.trigger();

        assertEquals(PreparingBackupState.class, agentV3_1.getState().getClass());
        assertEquals(PreparingBackupState.class, agentV3_2.getState().getClass());

        //Expect message from V3 agent, no message from V2 agent required to progress to ExecutingStage.
        agentV3_1.processMessage(getStageCompleteMessage(BACKUP,true));
        agentV3_2.processMessage(getStageCompleteMessage(BACKUP,false));

        assertEquals(CancelingActionState.class,agentV3_1.getState().getClass());
        assertEquals(CancelingActionState.class, agentV3_2.getState().getClass());

        assertJobStage(FailedBackupJobStage.class, createBackupJob.getJobStage().getClass());

        //Expect message from V2 and V3 agent in order to progress to PostBackupJobStage.
        agentV3_1.processMessage(getStageCompleteMessage(BACKUP,true));
        agentV3_2.processMessage(getStageCompleteMessage(BACKUP,true));

        assertJobStage(FailedBackupJobStage.class, createBackupJob.getJobStage().getClass());
        assertEquals(RecognizedState.class, agentV3_1.getState().getClass());
        assertEquals(RecognizedState.class, agentV3_2.getState().getClass());

        //verify(action);
    }

    @Test
    public void v3FailInExecute() {
        //Set up agents
        final List<Agent> mockAgents = Arrays.asList(createAgent("V3_1", ApiVersion.API_V3_0), createAgent("V3_2", ApiVersion.API_V3_0));

        //Create a Job
        final CreateBackupJobTest createBackupJob = new CreateBackupJobTest();
        final Action action = createMock(Action.class);
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.setProgressPercentage(anyDouble());
        expectLastCall().anyTimes();
        expect(action.getResult()).andReturn(FAILURE).anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getBackupName()).andReturn(createBackupJob.getBackupName()).once();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        action.persist();
        expectLastCall().anyTimes();

        expect(action.getProgressInfo()).andReturn(null).anyTimes();
        action.setProgressInfo(anyString());
        expectLastCall().anyTimes();

        replay(action);
        createBackupJob.setAction(action);

        final CMMediatorService cmMediatorService = createMock(CMMediatorService.class);
        createBackupJob.setCmMediatorService(cmMediatorService);
        final BackupManager brm = createMock(BackupManager.class);
        expect(brm.getAgentVisibleBRMId()).andReturn("some-brm-id").anyTimes();
        replay(brm);
        createBackupJob.setBackupManager(brm);

        //set first stage (Fake REST/CMM Call)
        currentStage = new PreparingBackupJobStage(mockAgents, createBackupJob, new NoNotifiersService());
        createBackupJob.setJobStage(currentStage);

        //Check V2 agent in correct starting state.
        final Agent agentV3_1 = (Agent)currentStage.getAgents().get(0);
        assertEquals(RecognizedState.class, agentV3_1.getState().getClass());
        assertEquals(ApiVersion.API_V3_0,agentV3_1.getApiVersion());

        //Check V3 agent in correct starting state.
        final Agent agentV3_2 = (Agent)currentStage.getAgents().get(1);
        assertEquals(RecognizedState.class, agentV3_2.getState().getClass());
        assertEquals(ApiVersion.API_V3_0,agentV3_2.getApiVersion());

        //Trigger backup process
        currentStage.trigger();

        assertEquals(PreparingBackupState.class, agentV3_1.getState().getClass());
        assertEquals(PreparingBackupState.class, agentV3_2.getState().getClass());

        //Expect message from V3 agent, no message from V2 agent required to progress to ExecutingStage.
        agentV3_1.processMessage(getStageCompleteMessage(BACKUP,true));
        agentV3_2.processMessage(getStageCompleteMessage(BACKUP,true));

        assertEquals(ExecutingBackupState.class,agentV3_1.getState().getClass());
        assertEquals(ExecutingBackupState.class, agentV3_2.getState().getClass());

        assertJobStage(ExecutingBackupJobStage.class, createBackupJob.getJobStage().getClass());

        //Expect message from V2 and V3 agent in order to progress to PostBackupJobStage.
        agentV3_1.processMessage(getStageCompleteMessage(BACKUP,false));
        agentV3_2.processMessage(getStageCompleteMessage(BACKUP,true));

        assertJobStage(FailedBackupJobStage.class, createBackupJob.getJobStage().getClass());
        assertEquals(CancelingActionState.class, agentV3_1.getState().getClass());
        assertEquals(CancelingActionState.class, agentV3_2.getState().getClass());

        //Expect message from V3 agent, backup should succeed all agents return to RecognizedState.
        agentV3_1.processMessage(getStageCompleteMessage(BACKUP,true));
        agentV3_2.processMessage(getStageCompleteMessage(BACKUP,true));
        assertEquals(RecognizedState.class, agentV3_1.getState().getClass());
        assertEquals(RecognizedState.class, agentV3_2.getState().getClass());

        //verify(action);
    }

    @Test
    public void v3FailInPost() {
        //Set up agents
        final List<Agent> mockAgents = Arrays.asList(createAgent("V3_1", ApiVersion.API_V3_0), createAgent("V3_2", ApiVersion.API_V3_0));

        //Create a Job
        final CreateBackupJobTest createBackupJob = new CreateBackupJobTest();
        final Action action = createMock(Action.class);
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.setProgressPercentage(anyDouble());
        expectLastCall().anyTimes();
        expect(action.getResult()).andReturn(FAILURE).anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getBackupName()).andReturn(createBackupJob.getBackupName()).once();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        action.persist();
        expectLastCall().anyTimes();

        expect(action.getProgressInfo()).andReturn(null).anyTimes();
        action.setProgressInfo(anyString());
        expectLastCall().anyTimes();

        replay(action);
        createBackupJob.setAction(action);

        final CMMediatorService cmMediatorService = createMock(CMMediatorService.class);
        createBackupJob.setCmMediatorService(cmMediatorService);
        final BackupManager brm = createMock(BackupManager.class);
        expect(brm.getAgentVisibleBRMId()).andReturn("some-brm-id").anyTimes();
        replay(brm);
        createBackupJob.setBackupManager(brm);

        //set first stage (Fake REST/CMM Call)
        currentStage = new PreparingBackupJobStage(mockAgents, createBackupJob, new NoNotifiersService());
        createBackupJob.setJobStage(currentStage);

        //Check V2 agent in correct starting state.
        final Agent agentV3_1 = (Agent)currentStage.getAgents().get(0);
        assertEquals(RecognizedState.class, agentV3_1.getState().getClass());
        assertEquals(ApiVersion.API_V3_0,agentV3_1.getApiVersion());

        //Check V3 agent in correct starting state.
        final Agent agentV3_2 = (Agent)currentStage.getAgents().get(1);
        assertEquals(RecognizedState.class, agentV3_2.getState().getClass());
        assertEquals(ApiVersion.API_V3_0,agentV3_2.getApiVersion());

        //Trigger backup process
        currentStage.trigger();

        assertEquals(PreparingBackupState.class, agentV3_1.getState().getClass());
        assertEquals(PreparingBackupState.class, agentV3_2.getState().getClass());

        //Expect message from V3 agent, no message from V2 agent required to progress to ExecutingStage.
        agentV3_1.processMessage(getStageCompleteMessage(BACKUP,true));
        agentV3_2.processMessage(getStageCompleteMessage(BACKUP,true));

        assertEquals(ExecutingBackupState.class,agentV3_1.getState().getClass());
        assertEquals(ExecutingBackupState.class, agentV3_2.getState().getClass());

        assertJobStage(ExecutingBackupJobStage.class, createBackupJob.getJobStage().getClass());

        //Expect message from V2 and V3 agent in order to progress to PostBackupJobStage.
        agentV3_1.processMessage(getStageCompleteMessage(BACKUP,true));
        agentV3_2.processMessage(getStageCompleteMessage(BACKUP,true));

        assertJobStage(PostActionBackupJobStage.class, createBackupJob.getJobStage().getClass());
        assertEquals(PostActionBackupState.class, agentV3_1.getState().getClass());
        assertEquals(PostActionBackupState.class, agentV3_2.getState().getClass());

        //Expect message from V3 agent, backup should succeed all agents return to RecognizedState.
        agentV3_1.processMessage(getStageCompleteMessage(BACKUP,true));
        agentV3_2.processMessage(getStageCompleteMessage(BACKUP,false));

        assertJobStage(FailedBackupJobStage.class, createBackupJob.getJobStage().getClass());
        assertEquals(CancelingActionState.class, agentV3_1.getState().getClass());
        assertEquals(CancelingActionState.class, agentV3_2.getState().getClass());

        //Expect message from V3 agent, backup should succeed all agents return to RecognizedState.
        agentV3_1.processMessage(getStageCompleteMessage(BACKUP,true));
        agentV3_2.processMessage(getStageCompleteMessage(BACKUP,true));
        assertEquals(RecognizedState.class, agentV3_1.getState().getClass());
        assertEquals(RecognizedState.class, agentV3_2.getState().getClass());
        //verify(action);
    }

    @Test
    public void v3allPass() {
        //Set up agents
        final List<Agent> mockAgents = Arrays.asList(createAgent("V3_1", ApiVersion.API_V3_0), createAgent("V3_2", ApiVersion.API_V3_0));

        //Create a Job
        final CreateBackupJobTest createBackupJob = new CreateBackupJobTest();
        final Action action = createMock(Action.class);
        action.addMessage(anyString());
        expectLastCall().anyTimes();
        action.setProgressPercentage(anyDouble());
        expectLastCall().anyTimes();
        expect(action.getResult()).andReturn(SUCCESS).anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getBackupName()).andReturn(createBackupJob.getBackupName()).once();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        action.persist();
        expectLastCall().anyTimes();
        replay(action);
        createBackupJob.setAction(action);

        final BackupManager brm = createMock(BackupManager.class);
        expect(brm.getAgentVisibleBRMId()).andReturn("some-brm-id").anyTimes();
        replay(brm);
        createBackupJob.setBackupManager(brm);

        //set first stage (Fake REST/CMM Call)
        currentStage = new PreparingBackupJobStage(mockAgents, createBackupJob, new NoNotifiersService());
        createBackupJob.setJobStage(currentStage);

        //Check V2 agent in correct starting state.
        final Agent agentV3_1 = (Agent)currentStage.getAgents().get(0);
        assertEquals(RecognizedState.class, agentV3_1.getState().getClass());
        assertEquals(ApiVersion.API_V3_0,agentV3_1.getApiVersion());

        //Check V3 agent in correct starting state.
        final Agent agentV3_2 = (Agent)currentStage.getAgents().get(1);
        assertEquals(RecognizedState.class, agentV3_2.getState().getClass());
        assertEquals(ApiVersion.API_V3_0,agentV3_2.getApiVersion());

        //Trigger backup process
        currentStage.trigger();

        assertEquals(PreparingBackupState.class, agentV3_1.getState().getClass());
        assertEquals(PreparingBackupState.class, agentV3_2.getState().getClass());

        //Expect message from V3 agent, no message from V2 agent required to progress to ExecutingStage.
        agentV3_1.processMessage(getStageCompleteMessage(BACKUP,true));
        agentV3_2.processMessage(getStageCompleteMessage(BACKUP,true));

        assertEquals(ExecutingBackupState.class,agentV3_1.getState().getClass());
        assertEquals(ExecutingBackupState.class, agentV3_2.getState().getClass());

        assertJobStage(ExecutingBackupJobStage.class, createBackupJob.getJobStage().getClass());

        //Expect message from V2 and V3 agent in order to progress to PostBackupJobStage.
        agentV3_1.processMessage(getStageCompleteMessage(BACKUP,true));
        agentV3_2.processMessage(getStageCompleteMessage(BACKUP,true));

        assertJobStage(PostActionBackupJobStage.class, createBackupJob.getJobStage().getClass());
        assertEquals(PostActionBackupState.class, agentV3_1.getState().getClass());
        assertEquals(PostActionBackupState.class, agentV3_2.getState().getClass());

        //Expect message from V3 agent, backup should succeed all agents return to RecognizedState.
        agentV3_1.processMessage(getStageCompleteMessage(BACKUP,true));
        agentV3_2.processMessage(getStageCompleteMessage(BACKUP,true));
        assertEquals(RecognizedState.class, agentV3_1.getState().getClass());
        assertEquals(RecognizedState.class, agentV3_2.getState().getClass());

        verify(action);
    }

    private Agent createAgent(final String agentId, final ApiVersion apiVersion) {
        final AgentRepository agentRepository = new AgentRepository();
        agentRepository.setVbrmAutoCreate(VBRMAutoCreate.NONE);
        final BackupManagerRepository backupManagerRepository = new BackupManagerRepository();
        backupManagerRepository.setIdValidator(new IdValidator());
        final SchedulerFileService schedulerFileService = EasyMock.mock(SchedulerFileService.class);
        schedulerFileService.getPersistedSchedulerInformation(anyString());
        expectLastCall().andThrow(new NullPointerException());
        schedulerFileService.writeToFile(anyObject());
        expectLastCall().anyTimes();
        replay(schedulerFileService);
        backupManagerRepository.setSchedulerFileService(schedulerFileService);
        final VirtualInformationFileService virtualInformationFileService = EasyMock.mock(VirtualInformationFileService.class);
        backupManagerRepository.setVirtualInformationFileService(virtualInformationFileService);
        final BackupManagerFileService backupManagerFileService = EasyMock.mock(BackupManagerFileService.class);
        backupManagerFileService.writeToFile(anyObject());
        expectLastCall().anyTimes();
        expect(backupManagerFileService.getBackupManagerFolder(anyObject())).andReturn(Paths.get("/DONT_EXIST")).anyTimes();
        replay(backupManagerFileService);
        backupManagerRepository.setBackupManagerFileService(backupManagerFileService);
        final CMMediatorService cmMediatorService = EasyMock.mock(CMMediatorService.class);
        backupManagerRepository.setCmMediatorService(cmMediatorService);
        final HousekeepingFileService housekeepingService = EasyMock.mock(HousekeepingFileService.class);
        backupManagerRepository.setHousekeepingFileService(housekeepingService);
        final PeriodicEventFileService periodicEventFileService = EasyMock.mock(PeriodicEventFileService.class);
        final CalendarEventFileService calendarEventFileService = EasyMock.mock(CalendarEventFileService.class);
        backupManagerRepository.setPeriodicEventFileService(periodicEventFileService);
        backupManagerRepository.setCalendarEventFileService(calendarEventFileService);
        expect(periodicEventFileService.createPeriodicEventsDirectory(anyObject(BackupManager.class))).andReturn(true).times(2);
        expect(calendarEventFileService.createCalendarEventsDirectory(anyObject(BackupManager.class))).andReturn(true).times(2);
        replay(periodicEventFileService, calendarEventFileService);
        final ActionRepository actionRepository = EasyMock.mock(ActionRepository.class);
        expect(actionRepository.getActions(anyString())).andReturn(new ArrayList<>()).anyTimes();
        replay(actionRepository);
        backupManagerRepository.setActionRepository(actionRepository);
        agentRepository.setBackupManagerRepository(backupManagerRepository);
        agentRepository.setMeterRegistry(createMeterRegistry());
        final Agent agent = new Agent(EasyMock.mock(AgentInputStream.class), agentRepository,new IdValidator());
        final SoftwareVersionInfo softwareVersionInfo = SoftwareVersionInfo.newBuilder().setProductName("ProductName")
                .setProductNumber("ProductNumber").setRevision("Revision").setProductionDate("ProductionDate").setDescription("Description")
                .setType("Type").build();
        final Register registerMessage = Register.newBuilder().setAgentId(agentId).setSoftwareVersionInfo(softwareVersionInfo)
                .setApiVersion(apiVersion.getStringRepresentation())
                .setScope("Test").build();
        agent.processMessage(AgentControl.newBuilder().setAgentMessageType(AgentMessageType.REGISTER).setRegister(registerMessage).setAction(REGISTER).build());
        return agent;
    }

    private AgentControl getStageCompleteMessage(final com.ericsson.adp.mgmt.action.Action action, final boolean success) {
        final StageComplete stageComplete = StageComplete.newBuilder().setMessage("Ended").setSuccess(success).build();
        return AgentControl.newBuilder().setAction(action).setAgentMessageType(AgentMessageType.STAGE_COMPLETE).setStageComplete(stageComplete)
                .build();
    }

    public class CreateBackupJobTest extends CreateBackupJob {
        @Override
        public String getBackupName() {
            return "fake-backup-name";
        }

        @Override
        public String getBackupManagerId() {
            return "backup-manager-id";
        }

        @Override
        protected void setJobStage(final JobStage<CreateBackupJob> jobStage) {
            super.setJobStage(jobStage);
        }

        @Override
        protected void setAction(final Action action) {
            super.setAction(action);
        }

        @Override
        public void setBackupManager(final BackupManager backupManager) {
            super.setBackupManager(backupManager);
        }
    }


    private void assertJobStage(final Class<? extends JobStage> expected, final Class<? extends JobStage> actual) {
        assertEquals(expected.getName(), actual.getName());
    }

    private MeterRegistry createMeterRegistry(){
        final CollectorRegistry prometheusRegistry = new CollectorRegistry(true);
        final MockClock clock = new MockClock();
        final MeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, prometheusRegistry,clock);
        return meterRegistry;
    }
}
