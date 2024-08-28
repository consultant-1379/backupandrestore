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
package com.ericsson.adp.mgmt.backupandrestore.agent;

import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.action.Action.RESTORE;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.PREPARATION;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.EXECUTION;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.CANCEL_BACKUP_RESTORE;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.POST_ACTIONS;

import com.ericsson.adp.mgmt.control.FragmentListEntry;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import com.ericsson.adp.mgmt.control.RegisterAcknowledge;

import com.ericsson.adp.mgmt.metadata.AgentFeature;
import com.ericsson.adp.mgmt.metadata.Fragment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.action.CancelBackupRestore;
import com.ericsson.adp.mgmt.backupandrestore.restore.RestoreInformation;
import com.ericsson.adp.mgmt.control.Execution;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.PostActions;
import com.ericsson.adp.mgmt.control.Preparation;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds a stream to send messages to an agent.
 */
public class AgentInputStream {

    private static final Logger log = LogManager.getLogger(AgentInputStream.class);

    private final StreamObserver<OrchestratorControl> stream;

    private final List<AgentFeature> broFeatures = new ArrayList<>();

    /**
     * Creates agentInputStream with the stream to an agent.
     *
     * @param stream
     *            to agent.
     */
    public AgentInputStream(final StreamObserver<OrchestratorControl> stream
    ) {
        this.stream = stream;

        // For now this will add all the agent features. This will be addressed in later story
        setBroFeatures();
    }

    /**
     * Triggers backup preparation on agent.
     * @param backupName name of backup.
     * @param backupType type of the backup.
     */
    public void prepareForBackup(final String backupName, final String backupType) {
        sendMessage(getBackupPreparationMessage(backupName, backupType));
    }

    /**
     * Triggers backup execution on agent.
     */
    public void executeBackup() {
        sendMessage(getExecutionMessage(BACKUP));
    }

    /**
     * Triggers backup post action on agent
     */
    public void executeBackupPostAction() {
        sendMessage(getPostActionMessage(BACKUP));
    }

    /**
     * Triggers restore preparation on agent.
     * @param restoreInformation restoreInformation of agent.
     */
    public void prepareForRestore(final RestoreInformation restoreInformation) {
        sendMessage(getRestorePreparationMessage(restoreInformation));
    }

    /**
     * Send fragment list.
     * @param restoreInformation restoreInformation of agent.
     */
    public void sendFragmentList(final RestoreInformation restoreInformation) {
        final List<Fragment> backupFragments = restoreInformation.getBackupFragments();
        int index = backupFragments.size();
        for (final Fragment fragment : backupFragments) {
            if (index > 1) {
                log.debug("Sending Fragment: {}", index);
                sendMessage(getFragmentListEntry(fragment, false));
            } else {
                log.debug("Sending Last Fragment: {}", index);
                sendMessage(getFragmentListEntry(fragment, true));
            }
            index--;
        }

    }

    /**
     * Triggers restore execution on agent.
     */
    public void executeRestore() {
        sendMessage(getExecutionMessage(RESTORE));
    }

    /**
     * Triggers restore post action on agent.
     */
    public void executeRestorePostAction() {
        sendMessage(getPostActionMessage(RESTORE));
    }

    /**
     * Triggers cancel action on agent.
     * @param action which action is being canceled.
     */
    public void cancelAction(final Action action) {
        sendMessage(getCancelActionMessage(action));
    }

    /**
     * Triggers registration acknowledge message
     */
    public void acknowledge() {
        sendMessage(getRegistrationAcknowledgementMessage());
    }

    /**
     * Closes stream.
     */
    public void close() {
        try {
            stream.onCompleted();
        } catch (final Exception e) {
            log.error("Failed to close agent input stream", e);
        }
    }

    /**
     * Closes stream due to error.
     * @param exception reason to close.
     */
    public void close(final StatusRuntimeException exception) {
        try {
            stream.onError(exception);
        } catch (final Exception e) {
            log.error("Failed to close agent input stream due to error", e);
        }
    }

    private OrchestratorControl getBackupPreparationMessage(final String backupName, final String backupType) {
        return OrchestratorControl
                .newBuilder()
                .setAction(BACKUP)
                .setOrchestratorMessageType(PREPARATION)
                .setPreparation(Preparation.newBuilder()
                        .setBackupName(backupName)
                        .setBackupType(backupType)
                        .build())
                .build();
    }

    private OrchestratorControl getFragmentListEntry(final Fragment fragment, final boolean isLast) {
        final FragmentListEntry newFragmentListEntry = FragmentListEntry.newBuilder().setFragment(fragment).setLast(isLast).build();
        return OrchestratorControl
                .newBuilder()
                .setAction(RESTORE)
                .setOrchestratorMessageType(OrchestratorMessageType.FRAGMENT_LIST_ENTRY)
                .setFragmentListEntry(newFragmentListEntry)
                .build();
    }

    private OrchestratorControl getRestorePreparationMessage(final RestoreInformation restoreInformation) {
        return OrchestratorControl
                .newBuilder()
                .setAction(RESTORE)
                .setOrchestratorMessageType(PREPARATION)
                .setPreparation(restoreInformation.buildPreparationMessage())
                .build();
    }

    private OrchestratorControl getExecutionMessage(final Action action) {
        return OrchestratorControl
                .newBuilder()
                .setAction(action)
                .setOrchestratorMessageType(EXECUTION)
                .setExecution(Execution.newBuilder().build())
                .build();
    }

    private OrchestratorControl getPostActionMessage(final Action action) {
        return OrchestratorControl
                .newBuilder()
                .setAction(action)
                .setOrchestratorMessageType(POST_ACTIONS)
                .setPostActions(PostActions.newBuilder().build())
                .build();
    }

    private OrchestratorControl getCancelActionMessage(final Action action) {
        return OrchestratorControl
                .newBuilder()
                .setAction(action)
                .setOrchestratorMessageType(CANCEL_BACKUP_RESTORE)
                .setCancel(CancelBackupRestore.newBuilder().build())
                .build();
    }

    private OrchestratorControl getRegistrationAcknowledgementMessage() {
        return OrchestratorControl.newBuilder()
                .setOrchestratorMessageType(OrchestratorMessageType.REGISTER_ACKNOWLEDGE)
                .setRegisterAcknowledge(RegisterAcknowledge.newBuilder().setAcknowledgeMessage("Registered Agent")
                .addAllBroSupportedAgentFeature(broFeatures).build())
                .build();
    }

    private void sendMessage(final OrchestratorControl message) {
        log.info("Sending control message <{}>", message);
        stream.onNext(message);
    }

    private void setBroFeatures() {
        // For now this will add all the agent features. This will be addressed in later story
        broFeatures.addAll(EnumSet.allOf(AgentFeature.class)
                .stream().filter(a -> a != AgentFeature.UNRECOGNIZED).collect(Collectors.toList()).subList(0, 7));
    }

}
