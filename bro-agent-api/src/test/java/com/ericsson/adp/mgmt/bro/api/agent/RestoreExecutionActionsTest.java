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

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

public class RestoreExecutionActionsTest {

    private RestoreExecutionActions restoreActions;
    private TestOrchestratorGrpcChannel channel;
    private RestoreInformation restoreInformation;
    private Agent agent;
    private final List<Fragment> fragmentList = Arrays.asList(getFragment());

    @Before
    public void setUp() {
        channel = new TestOrchestratorGrpcChannel();
        agent = new Agent(new TestAgentBehavior(), channel);
        restoreInformation = new RestoreInformation("Backup", getSoftwareInfo(), fragmentList, "DEFAULT");
        restoreActions = new RestoreExecutionActions(agent, restoreInformation);
    }

    @Test
    public void restoreComplete_stageCompleteTrue() {
        restoreActions.restoreComplete(true, "Success");
        assertTrue(this.channel.getMessage().getStageComplete().getSuccess());
    }

    private class TestOrchestratorGrpcChannel extends OrchestratorGrpcChannel {

        private final List<AgentControl> messages = new LinkedList<>();

        protected TestOrchestratorGrpcChannel() {
            super(null);
        }

        @Override
        protected void sendControlMessage(final AgentControl message) {
            this.messages.add(message);
        }

        public AgentControl getMessage() {
            if(messages.isEmpty()) {
                return null;
            }
            final AgentControl message = messages.remove(0);
            return message;
        }

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

}
