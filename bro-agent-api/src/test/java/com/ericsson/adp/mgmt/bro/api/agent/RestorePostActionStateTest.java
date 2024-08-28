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
package com.ericsson.adp.mgmt.bro.api.agent;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.action.CancelBackupRestore;
import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import com.ericsson.adp.mgmt.control.PostActions;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

public class RestorePostActionStateTest {

    private AgentState state;
    private Agent agent;
    private TestOrchestratorGrpcChannel channel;
    private final List<Fragment> fragmentList = Arrays.asList(getFragment());
    private RestoreInformation restoreInformation;

    @Before
    public void setUp() {
        this.channel = new TestOrchestratorGrpcChannel();
        this.agent = new Agent(new TestAgentBehavior(), this.channel);
        this.restoreInformation = new RestoreInformation("backup", getSoftwareInfo(), this.fragmentList, "DEFAULT");
        this.state = new RestorePostActionState(this.agent, this.restoreInformation);
    }

    @Test
    public void processMessage_postActionMessage_stateChangeToWaitingForActions() {
        final AgentState stateChange = this.state.processMessage(getPostActionMessage());
        assertTrue(stateChange instanceof WaitingForActionState);
    }

    @Test
    public void processMessage_CancelMessage_stateChangeToWaitingForActionState() {
        final AgentState stateChange = this.state.processMessage(OrchestratorControl.newBuilder()
                .setAction(Action.RESTORE)
                .setOrchestratorMessageType(OrchestratorMessageType.CANCEL_BACKUP_RESTORE)
                .setCancel(CancelBackupRestore.newBuilder().build())
                .build());

        assertTrue(stateChange instanceof WaitingForActionState);
    }

    @Test
    public void trigger_agent_stateChangeToWaitingForActions() {
        final AgentState stateChange = this.state.processMessage(getPostActionMessage());
        assertTrue(stateChange instanceof WaitingForActionState);

        stateChange.trigger();
        assertNull(this.channel.getMessage());
    }

    @Test
    public void finishAction_returnsFinishedActionState() {
        final AgentState stateChange  = state.finishAction();
        assertTrue(stateChange instanceof FinishedActionState);
    }

    private SoftwareVersionInfo getSoftwareInfo() {
        return SoftwareVersionInfo.newBuilder()
                .setProductName("Name")
                .setProductNumber("Number")
                .setDescription("Description")
                .setRevision("Revision")
                .setProductionDate("Date")
                .setType("Type")
                .build();
    }

    private Fragment getFragment() {
        return Fragment.newBuilder()
                .setFragmentId("id")
                .setSizeInBytes("size")
                .setVersion("version")
                .build();
    }

    private OrchestratorControl getPostActionMessage() {
        return OrchestratorControl.newBuilder()
                .setOrchestratorMessageType(OrchestratorMessageType.POST_ACTIONS)
                .setAction(Action.RESTORE)
                .setPostActions(PostActions.newBuilder().build())
                .build();
    }
}
