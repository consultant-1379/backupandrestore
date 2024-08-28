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

package com.ericsson.adp.mgmt.bro.api.agent;

import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.action.Action.CANCEL;
import static com.ericsson.adp.mgmt.action.Action.REGISTER;
import static com.ericsson.adp.mgmt.action.Action.RESTORE;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.CANCEL_BACKUP_RESTORE;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.EXECUTION;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.POST_ACTIONS;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.PREPARATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.Execution;
import com.ericsson.adp.mgmt.control.FragmentListEntry;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import com.ericsson.adp.mgmt.control.PostActions;
import com.ericsson.adp.mgmt.control.Preparation;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

import io.grpc.Status;

public class AgentTest {

    private static final Optional<String> REGISTRATION_ACK = Optional.of(OrchestratorMessageType.REGISTER_ACKNOWLEDGE.toString());
    private TestOrchestratorGrpcChannel channel;
    private TestAgentBehavior agentBehavior;
    private Agent agent;
    private ExecutorService fixedExecutor;
    private ScheduledExecutorService scheduledExecutor;

    @Before
    public void setUp() {
        channel = new TestOrchestratorGrpcChannel();
        agentBehavior = new TestAgentBehavior();
        agent = new Agent(agentBehavior, channel);
        fixedExecutor = Executors.newFixedThreadPool(1);
        scheduledExecutor = Executors.newScheduledThreadPool(1);
    }

    @After
    public void tearDown() {
        fixedExecutor.shutdown();
        scheduledExecutor.shutdown();
    }

    @Test
    public void shutdown_agent_shutsDownChannel() throws Exception {
        agent.shutdown();
        assertTrue(channel.isShutdown());
    }

    @Test
    public void process_backupMessage_executesBackup() {
        Agent backupAgent = agent;
        backupAgent.process(getPreparationMessage(BACKUP));
        assertTrue(agentBehavior.hasPreparedBackup());
        backupAgent.process(OrchestratorControl.newBuilder().setOrchestratorMessageType(EXECUTION)
            .setExecution(Execution.newBuilder().build()).setAction(BACKUP).build());
        assertTrue(agentBehavior.executedBackup());
        backupAgent.process(OrchestratorControl.newBuilder().setOrchestratorMessageType(POST_ACTIONS)
            .setPostActions(PostActions.newBuilder().build()).setAction(BACKUP).build());
        assertTrue(agentBehavior.hasPerformedPostBackup());
        final AgentControl agentControlMessageExecutionComplete = channel.getMessage();
        assertEquals(BACKUP, agentControlMessageExecutionComplete.getAction());
        assertEquals(AgentMessageType.STAGE_COMPLETE, agentControlMessageExecutionComplete.getAgentMessageType());
        assertEquals("Test Post backup.", agentControlMessageExecutionComplete.getStageComplete().getMessage());
        assertTrue(agentControlMessageExecutionComplete.getStageComplete().getSuccess());

        channel.reset();
        agentBehavior.reset();
        // Second Backup
        backupAgent.process(getPreparationMessage(BACKUP, "Backup1"));
        assertTrue(agentBehavior.hasPreparedBackup());
        backupAgent.process(OrchestratorControl.newBuilder().setOrchestratorMessageType(EXECUTION)
            .setExecution(Execution.newBuilder().build()).setAction(BACKUP).build());
        assertTrue(agentBehavior.executedBackup());
        backupAgent.process(OrchestratorControl.newBuilder().setOrchestratorMessageType(POST_ACTIONS)
            .setPostActions(PostActions.newBuilder().build()).setAction(BACKUP).build());
        assertTrue(agentBehavior.hasPerformedPostBackup());
    }

    @Test
    public void process_restoreMessage_stateChange() {
        agent.process(getPreparationMessage(RESTORE));
        agent.process(getFragmentListEntry());
        assertTrue(agentBehavior.preparedForRestore());

        agent.process(OrchestratorControl.newBuilder()
                .setOrchestratorMessageType(EXECUTION)
                .setExecution(Execution.newBuilder().build())
                .setAction(RESTORE).build());

        assertTrue(agentBehavior.executedRestore());

        agent.process(OrchestratorControl.newBuilder()
                .setOrchestratorMessageType(POST_ACTIONS)
                .setPostActions(PostActions.newBuilder().build())
                .setAction(RESTORE).build());
        assertTrue(agentBehavior.performedPostRestore());
    }

    @Test
    public void process_messageDifferentThanBackupAndRestores_doesNothing() {
        final OrchestratorControl orchestratorControlMessage = OrchestratorControl.newBuilder().setAction(CANCEL)
                .setOrchestratorMessageType(CANCEL_BACKUP_RESTORE).build();
        agent.process(orchestratorControlMessage);
        assertFalse(agentBehavior.hasPreparedBackup());
        assertFalse(agentBehavior.executedBackup());
        assertFalse(agentBehavior.hasPerformedPostBackup());
        assertFalse(agentBehavior.preparedForRestore());
        assertFalse(agentBehavior.executedRestore());
        assertFalse(agentBehavior.performedPostRestore());
    }

    @Test
    public void executeBackup_agentBehaviorThrowsException_catchesAndSendsStageCompleteFalse() throws Exception {
        agent = new Agent(new ExceptionAgentBehavior(), channel);

        agent.executeBackup(null);

        assertEquals(BACKUP, channel.getMessage().getAction());
        assertTrue(channel.getMessage().hasStageComplete());
    }

    @Test
    public void prepareForRestore_agentBehaviorThrowsException_catchesAndSendsStageCompleteFalse() throws Exception {
        agent = new Agent(new ExceptionAgentBehavior(), channel);

        agent.prepareForRestore(null);

        assertEquals(RESTORE, channel.getMessage().getAction());
        assertTrue(channel.getMessage().hasStageComplete());
    }

    @Test
    public void executeRestore_agentBehaviorThrowsException_catchesAndSendsStageCompleteFalse() throws Exception {
        agent = new Agent(new ExceptionAgentBehavior(), channel);

        agent.executeRestore(null);

        assertEquals(RESTORE, channel.getMessage().getAction());
        assertTrue(channel.getMessage().hasStageComplete());
    }

    @Test
    public void postRestore_agentBehaviorThrowsException_catchesAndSendsStageCompleteFalse() throws Exception {
        agent = new Agent(new ExceptionAgentBehavior(), channel);

        agent.postRestore(null);

        assertEquals(RESTORE, channel.getMessage().getAction());
        assertTrue(channel.getMessage().hasStageComplete());
    }

    @Test
    public void register_isRegisteringAgainInTheMiddleOfAnAction_cancelsCurrentActionAndRegistersAgain() throws Exception {
        // let the agent register and reach PREPARATION state
        final OrchestratorStreamObserver firstStream = new OrchestratorStreamObserver(agent);
        fixedExecutor.execute(() -> agent.register(firstStream));
        final ScheduledFuture<?> firstAck = scheduledExecutor.schedule(() -> firstStream.setLastMessageTypeReceived(REGISTRATION_ACK), 100, TimeUnit.MILLISECONDS);
        firstAck.get(1, TimeUnit.SECONDS);
        agent.process(getPreparationMessage(RESTORE));
        agent.process(getFragmentListEntry());
        assertTrue(agentBehavior.preparedForRestore());

        channel.reset();
        agentBehavior.reset();

        // Register the agent again
        final OrchestratorStreamObserver newStream = new OrchestratorStreamObserver(agent);
        fixedExecutor.execute(() -> agent.register(newStream));
        ScheduledFuture<?> secondAck = scheduledExecutor.schedule(() -> newStream.setLastMessageTypeReceived(REGISTRATION_ACK), 100, TimeUnit.MILLISECONDS);
        secondAck.get(1, TimeUnit.SECONDS);
        assertTrue(agentBehavior.cancelledAction());
        assertFalse(agentBehavior.preparedForRestore());

        agent.process(getPreparationMessage(RESTORE));
        agent.process(getFragmentListEntry());
        assertTrue(agentBehavior.preparedForRestore());
    }

    @Test
    public void cancelAction_agentBehaviorThrowsException_catchesAndSendsStageCompleteFalse() throws Exception {
        agent = new Agent(new ExceptionAgentBehavior(), channel);

        agent.cancelAction(null);

        assertEquals(RESTORE, channel.getMessage().getAction());
        assertTrue(channel.getMessage().hasStageComplete());
    }

    @Test
    public void register_agentWithBehaviorAndChannel_ackMessageReceivedAfter3Seconds() throws Exception {
        agent.setSecondsToRetryACK(2);
        OrchestratorStreamObserver orchestratorObserver = new OrchestratorStreamObserver(agent);
        orchestratorObserver.setLastMessageTypeReceived(Optional.empty());
        fixedExecutor.execute(() -> agent.register(orchestratorObserver));
        ScheduledFuture<?> future = scheduledExecutor.schedule(() -> orchestratorObserver.setLastMessageTypeReceived(REGISTRATION_ACK), 3, TimeUnit.SECONDS);
        future.get(4, TimeUnit.SECONDS);  // wait for the value to be changed
        assertTrue(channel.getNumberMessagesReceived() == 2);
        assertTrue(channel.createdNewStream());
        assertEquals(REGISTER, channel.getMessage().getAction());
        assertEquals(AgentMessageType.REGISTER, channel.getMessage().getAgentMessageType());
    }

    @Test
    public void register_agentWithBehaviorAndChannel_ackMessageNotReceived() throws Exception {
        agent.setSecondsToRetryACK(1);
        OrchestratorStreamObserver orchestratorObserver = new OrchestratorStreamObserver(agent);
        fixedExecutor.execute(() -> agent.register(orchestratorObserver));
        ScheduledFuture<?> future = scheduledExecutor.schedule(() -> orchestratorObserver.setLastMessageTypeReceived(Optional.empty()), 3, TimeUnit.SECONDS);
        future.get(4, TimeUnit.SECONDS);  // wait for the value to be changed

        assertTrue(channel.getNumberMessagesReceived() == 3);

        assertTrue(channel.createdNewStream());
        assertEquals(REGISTER, channel.getMessage().getAction());
        assertEquals(AgentMessageType.REGISTER, channel.getMessage().getAgentMessageType());
    }

    @Test
    public void toBackupAgent_createsBackupAgent_BackupAgentCreated() {
        assertTrue(agent instanceof Agent);
    }

    @Test
    public void register_v4agentWithBehaviorAndChannel_registrationRetryStopsAfterReceivingAPreparationMessage() throws Exception {
        OrchestratorStreamObserver orchestratorObserver = new OrchestratorStreamObserver(agent);
        agent.setSecondsToRetryACK(2);
        fixedExecutor.execute(() -> agent.register(orchestratorObserver));
        ScheduledFuture<?> future = scheduledExecutor.schedule(() -> orchestratorObserver.setLastMessageTypeReceived(Optional.of(OrchestratorMessageType.PREPARATION.toString())), 2100, TimeUnit.MILLISECONDS);
        future.get(3, TimeUnit.SECONDS);
        Thread.sleep(3000);
        assertTrue(channel.getNumberMessagesReceived() == 2);
    }

    @Test
    public void register_v4agentWithBehaviorAndChannel_onlySendsRegistrationMessageOnceWhenSecondsToRetryAckIsZero() throws Exception {
        OrchestratorStreamObserver orchestratorObserver = new OrchestratorStreamObserver(agent);
        agent.setSecondsToRetryACK(0);
        fixedExecutor.execute(() -> agent.register(orchestratorObserver));
        ScheduledFuture<?> future = scheduledExecutor.schedule(() -> orchestratorObserver.setLastMessageTypeReceived(REGISTRATION_ACK), 3, TimeUnit.SECONDS);
        future.get(4, TimeUnit.SECONDS);
        assertTrue(channel.getNumberMessagesReceived() == 1);
    }

    @Test
    public void register_v3agentWithBehaviorAndChannel_onlySendsRegistrationMessageOnce() throws Exception {
        OrchestratorStreamObserver orchestratorObserver = new OrchestratorStreamObserver(agent);
        agent.setSecondsToRetryACK(1);
        agent.setGrpcApiVersion(GrpcApiVersion.V3);
        fixedExecutor.execute(() -> agent.register(orchestratorObserver));
        ScheduledFuture<?> future = scheduledExecutor.schedule(() -> orchestratorObserver.setLastMessageTypeReceived(REGISTRATION_ACK), 3, TimeUnit.SECONDS);
        future.get(4, TimeUnit.SECONDS);
        assertTrue(channel.getNumberMessagesReceived() == 1);
    }

    @Test
    public void register_v4agentwithBehaviorAndChannel_registersAgainWithApiVersion3() throws Exception{
        OrchestratorStreamObserver orchestratorObserver = new OrchestratorStreamObserver(agent);
        agent.setSecondsToRetryACK(1);

        // verify that the agent initially uses apiVersion 4.0
        assertEquals(agent.getAgentGrpcApiVersion(), GrpcApiVersion.V4);

        fixedExecutor.execute(() -> agent.register(orchestratorObserver));
        ScheduledFuture<?> future = scheduledExecutor.schedule(() -> {
            orchestratorObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid api version. Only [2.0, 3.0] are supported.")
                                                .asRuntimeException());
        }, 100, TimeUnit.MILLISECONDS);
        future.get(3, TimeUnit.SECONDS);

        // verify that the agent is now using apiVersion 3.0
        assertEquals(agent.getAgentGrpcApiVersion(), GrpcApiVersion.V3);
        // The agent sends one message using apiVersion 4.0 and another using apiVersion 3.0
        assertTrue(channel.getNumberMessagesReceived() == 2);
    }

    private SoftwareVersionInfo getSoftwareVersionInfo() {
        return SoftwareVersionInfo.newBuilder().setProductName("name").setDescription("description")
                .setProductNumber("id").setProductionDate("date").setRevision("R1").setType("type").build();
    }

    private OrchestratorControl getPreparationMessage(final Action action) {
        return getPreparationMessage(action, "Backup");
    }

    private OrchestratorControl getPreparationMessage(final Action action, String backupName) {
        Preparation preparation = null;
        if (action.equals(BACKUP) ) {
            preparation = Preparation.newBuilder().setBackupName(backupName).setBackupType("DEFAULT").build();
        } else if(action.equals(RESTORE)) {
            final List<Fragment> fragments = new ArrayList<>();
            fragments.add(Fragment.newBuilder().build());
            preparation = Preparation.newBuilder().setBackupName("Backup")
                .setSoftwareVersionInfo(getSoftwareVersionInfo())
                    .addAllFragment(fragments)
                    .build();
        }

        assertNotNull(preparation);
        final OrchestratorControl orchestratorControlMessage = OrchestratorControl.newBuilder().setAction(action)
                .setOrchestratorMessageType(PREPARATION).setPreparation(preparation).build();
        return orchestratorControlMessage;
    }

    private OrchestratorControl getFragmentListEntry() {
        return OrchestratorControl.newBuilder().setFragmentListEntry(FragmentListEntry.newBuilder()
                        .setLast(true)
                .setFragment(Fragment.newBuilder().setFragmentId("id").setSizeInBytes("size").setVersion("version").build()).build()).build();
    }

    class TestOrchestratorGrpcChannel extends OrchestratorGrpcChannel {

        private boolean createdNewStream;
        private AgentControl message;
        private boolean isShutdown;
        private final AtomicBoolean isAcknowledge;
        private int numberMessagesReceived;

        protected TestOrchestratorGrpcChannel(final boolean asyncMsgReceived) {
            super(null);
            isAcknowledge = new AtomicBoolean(asyncMsgReceived);
        }

        protected TestOrchestratorGrpcChannel() {
        	this(false);
        }

        @Override
        public void establishControlChannel(final OrchestratorStreamObserver streamObserver) {
            this.createdNewStream = true;
        }

        @Override
        protected void sendControlMessage(final AgentControl message) {
            numberMessagesReceived++;
            this.message = message;
        }

        @Override
        protected void shutdown() {
            this.isShutdown = true;
        }

        public boolean createdNewStream() {
            return createdNewStream;
        }

        public AgentControl getMessage() {
            return message;
        }

        public boolean isShutdown() {
            return this.isShutdown;
        }

        public boolean isAcknowledge() {
            return isAcknowledge.get();
        }

        public void reset() {
            this.createdNewStream = false;
            this.message = null;
            this.isShutdown = false;
        }

        public int getNumberMessagesReceived() {
            return numberMessagesReceived;
        }

    }

    private class ExceptionAgentBehavior extends TestAgentBehavior {

        @Override
        public void executeBackup(final BackupExecutionActions backupExecutionActions) {
            throw new RuntimeException("Backup");
        }

        @Override
        public void prepareForRestore(final RestorePreparationActions restorePreparationActions) {
            throw new RuntimeException("Restore preparation");
        }

        @Override
        public void executeRestore(final RestoreExecutionActions restoreExecutionActions) {
            throw new RuntimeException("Restore execution");
        }

        @Override
        public void postRestore(final PostRestoreActions postRestoreActions) {
            throw new RuntimeException("Restore Post");
        }

        @Override
        public void cancelAction(final CancelActions cancelActions) {
            throw new RuntimeException("Cancel");
        }
    }
}
