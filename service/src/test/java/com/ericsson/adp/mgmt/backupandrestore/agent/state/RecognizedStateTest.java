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
package com.ericsson.adp.mgmt.backupandrestore.agent.state;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static com.ericsson.adp.mgmt.action.Action.REGISTER;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.restore.RestoreInformation;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

public class RecognizedStateTest {

    private RecognizedState state;

    @Before
    public void setup() {
        state = new RecognizedState(getRegistrationMessage("123"));
    }

    @Test
    public void getAgentId_recognizedState_getsAgentIdFromRegistrationInformation() throws Exception {
        assertEquals("123", state.getAgentId());
    }

    @Test
    public void getScope_recognizedState_getsScopeFromRegistrationInformation() throws Exception {
        assertEquals("Alpha", state.getScope());
    }

    @Test
    public void getSoftwareVersion_unrecognizedState_shouldNotGetSoftwareVersion() throws Exception {
        final SoftwareVersion softwareVersion = state.getSoftwareVersion();
        assertEquals("123", softwareVersion.getAgentId());
        assertEquals("ProductName", softwareVersion.getProductName());
        assertEquals("ProductNumber", softwareVersion.getProductNumber());
        assertEquals("Revision", softwareVersion.getProductRevision());
        assertEquals("ProductionDate", softwareVersion.getDate());
        assertEquals("Description", softwareVersion.getDescription());
        assertEquals("Type", softwareVersion.getType());
    }

    @Test
    public void processMessage_registrationMessage_doesntUpdateCurrentInformation() throws Exception {
        assertEquals("123", state.getAgentId());

        final AgentStateChange stateChange = state.processMessage(getAgentControlMessage("456"));

        assertEquals("123", state.getAgentId());
        assertEquals(state, stateChange.getNextState());
    }

    @Test
    public void prepareForBackup_recognizedState_movesToPreparationBackupStateAndTriggersBackup() throws Exception {
        final CreateBackupJob job = createMock(CreateBackupJob.class);
        expect(job.getBackupName()).andReturn("b");
        final BackupManager brm = createMock(BackupManager.class);
        expect(job.getBackupManager()).andReturn(brm).anyTimes();
        expect(brm.getAgentVisibleBRMId()).andReturn("DEFAULT").anyTimes();
        final AgentInputStream inputStream = createMock(AgentInputStream.class);
        inputStream.prepareForBackup("b", "DEFAULT");
        expectLastCall();
        replay(job, inputStream, brm);

        final AgentStateChange stateChange = state.prepareForBackup(inputStream, job);
        stateChange.postAction(null, null);

        verify(inputStream);
        verify(job);
        assertEquals(PreparingBackupState.class, stateChange.getNextState().getClass());
        assertEquals("123", stateChange.getNextState().getAgentId());
    }

    @Test
    public void prepareForRestore_recognizedState_movesToPreparingRestoreStateAndTriggersRestore() throws Exception {
        final RestoreInformation restoreInformation = createMock(RestoreInformation.class);
        final RestoreJob job = createMock(RestoreJob.class);
        expect(job.createRestoreInformation(state.getAgentId())).andReturn(restoreInformation);
        final AgentInputStream inputStream = createMock(AgentInputStream.class);
        inputStream.prepareForRestore(restoreInformation);
        expectLastCall();
        replay(job, restoreInformation, inputStream);

        final AgentStateChange stateChange = state.prepareForRestore(inputStream, job);
        stateChange.postAction(null, null);
        verify(inputStream);
        verify(job);
        assertEquals(PreparingRestoreState.class, stateChange.getNextState().getClass());
        assertEquals("123", stateChange.getNextState().getAgentId());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void executeRestore_recognizedState_throwsException() throws Exception {
        state.executeRestore(null);
    }

    @Test
    public void resetState_recognizedState(){
        assertEquals(RecognizedState.class, state.resetState().getClass());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void executeRestorePostAction_recognizedState_throwsException() throws Exception {
        state.executeRestorePostAction(null);
    }

    private Register getRegistrationMessage(final String agentId) {
        final SoftwareVersionInfo softwareVersionInfo = SoftwareVersionInfo.newBuilder().setProductName("ProductName")
                .setProductNumber("ProductNumber").setRevision("Revision").setProductionDate("ProductionDate").setDescription("Description")
                .setType("Type").build();

        return Register.newBuilder().setAgentId(agentId).setSoftwareVersionInfo(softwareVersionInfo).setApiVersion("456").setScope("Alpha").build();
    }

    private AgentControl getAgentControlMessage(final String agentId) {
        return AgentControl.newBuilder().setAction(REGISTER).setAgentMessageType(AgentMessageType.REGISTER)
                .setRegister(getRegistrationMessage(agentId)).build();
    }

}
