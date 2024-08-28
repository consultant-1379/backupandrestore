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
package com.ericsson.adp.mgmt.backupandrestore.agent.state;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;
import org.junit.Before;
import org.junit.Test;

public class PreparingBackupStateTest {

    private PreparingBackupState state;
    private CreateBackupJob job;
    private AgentInputStream inputStream;

    @Before
    public void setup() {
        job = createMock(CreateBackupJob.class);
        state = new PreparingBackupState(getRegistrationMessage(), job);
        inputStream = createMock(AgentInputStream.class);
    }

    @Test
    public void executeBackup_preparingBackupState_movesToExecutingBackupStateAndTriggersBackupExecution() throws Exception {
        inputStream.executeBackup();
        expectLastCall();
        replay(job, inputStream);

        final AgentStateChange stateChange = state.executeBackup(inputStream);

        assertEquals(ExecutingBackupState.class, stateChange.getNextState().getClass());
    }

    @Test
    public void cancelAction_preparingBackupState_movesToCancellingActionState() {
        replay(inputStream);

        final AgentStateChange stateChange = state.cancelAction(inputStream);
        assertEquals(CancelingActionState.class, stateChange.getNextState().getClass());
    }

    private Register getRegistrationMessage() {
        final SoftwareVersionInfo softwareVersionInfo = SoftwareVersionInfo.newBuilder().setProductName("ProductName")
            .setProductNumber("ProductNumber").setRevision("Revision").setProductionDate("ProductionDate").setDescription("Description")
            .setType("Type").build();

        return Register.newBuilder().setAgentId("test-agent").setSoftwareVersionInfo(softwareVersionInfo).setApiVersion(ApiVersion.API_V3_0.getStringRepresentation()).setScope("Alpha").build();
    }
}
