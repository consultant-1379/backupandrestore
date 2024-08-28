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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.action.Action.REGISTER;

import java.util.function.Consumer;

import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentRepository;
import com.ericsson.adp.mgmt.backupandrestore.agent.exception.InvalidRegistrationMessageException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidIdException;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

public class UnrecognizedStateTest {

    private UnrecognizedState state;

    @Before
    public void setup() {
        final IdValidator idValidator = EasyMock.createMock(IdValidator.class);
        idValidator.validateId(EasyMock.anyObject());
        EasyMock.expectLastCall();
        EasyMock.replay(idValidator);
        state = new UnrecognizedState(idValidator);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getAgentId_unrecognizedState_shouldNotGetAgentId() throws Exception {
        state.getAgentId();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getScope_unrecognizedState_throwsException() throws Exception {
        state.getScope();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getSoftwareVersion_unrecognizedState_shouldNotGetSoftwareVersion() throws Exception {
        state.getSoftwareVersion();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void executeBackup_unrecognizedState_throwsException() throws Exception {
        state.executeBackup(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void prepareForRestore_unrecognizedState_throwsException() throws Exception {
        state.prepareForRestore(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void executeRestore_unrecognizedState_throwsException() throws Exception {
        state.executeRestore(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void executeRestorePostAction_unrecognizedState_throwsException() throws Exception {
        state.executeRestorePostAction(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void resetState_unrecognizedState_throwsException() throws Exception {
        state.resetState();
    }

    @Test
    public void processMessage_registrationMessage_nextStateIsRecognized() throws Exception {
        final AgentStateChange stateChange = state.processMessage(getRegistrationMessage());

        assertTrue(stateChange.getNextState() instanceof RecognizedState);
        final RecognizedState nextState = (RecognizedState) stateChange.getNextState();
        assertEquals("123", nextState.getAgentId());

        final Agent agent = new MockedAgentFactory().mockedAgent();
        final AgentRepository repository = EasyMock.createMock(AgentRepository.class);

        expect(agent.getApiVersion()).andReturn(ApiVersion.API_V4_0);
        agent.sendAcknowledgeRegistrationMessage();
        expectLastCall();
        
        repository.addAgent(agent);
        EasyMock.expectLastCall();
        EasyMock.replay(agent, repository);
        stateChange.postAction(agent, repository);
        EasyMock.verify(repository);
    }

    @Test
    public void processMessage_notRegistrationMessage_staysUnrecognized() throws Exception {
        final AgentStateChange stateChange = state.processMessage(AgentControl.newBuilder().setAction(BACKUP).build());

        assertEquals(UnrecognizedState.class, stateChange.getNextState().getClass());
        assertEquals(state, stateChange.getNextState());
    }

    @Test(expected = InvalidRegistrationMessageException.class)
    public void processMessage_registrationMessageWithoutProductName_throwsException() throws Exception {
        testMissingSoftwareVersionInfoPart(builder -> builder.setProductName(""));
    }

    @Test(expected = InvalidRegistrationMessageException.class)
    public void processMessage_registrationMessageWithoutProductNumber_throwsException() throws Exception {
        testMissingSoftwareVersionInfoPart(builder -> builder.setProductNumber(""));
    }

    @Test(expected = InvalidRegistrationMessageException.class)
    public void processMessage_registrationMessageWithoutRevision_throwsException() throws Exception {
        testMissingSoftwareVersionInfoPart(builder -> builder.setRevision(""));
    }

    @Test(expected = InvalidRegistrationMessageException.class)
    public void processMessage_registrationMessageWithoutProductionDate_throwsException() throws Exception {
        testMissingSoftwareVersionInfoPart(builder -> builder.setProductionDate(""));
    }

    @Test(expected = InvalidRegistrationMessageException.class)
    public void processMessage_registrationMessageWithoutDescription_throwsException() throws Exception {
        testMissingSoftwareVersionInfoPart(builder -> builder.setDescription(""));
    }

    @Test(expected = InvalidRegistrationMessageException.class)
    public void processMessage_registrationMessageWithoutType_throwsException() throws Exception {
        testMissingSoftwareVersionInfoPart(builder -> builder.setType(""));
    }

    @Test(expected = InvalidRegistrationMessageException.class)
    public void processMessage_messageWithInvalidApiVersion_throwsException() throws Exception {
        state.processMessage(getRegistrationMessage("123", "1.0", "Alpha"));
    }

    @Test(expected = InvalidRegistrationMessageException.class)
    public void processMessage_messageWithInvalidAgentId_throwsException() throws Exception {
        final IdValidator idValidator = EasyMock.createMock(IdValidator.class);
        idValidator.validateId(EasyMock.anyObject());
        EasyMock.expectLastCall().andThrow(new InvalidIdException(""));
        EasyMock.replay(idValidator);
        state = new UnrecognizedState(idValidator);

        state.processMessage(getRegistrationMessage("123/45", "2.0", "Alpha"));
    }

    private void testMissingSoftwareVersionInfoPart(final Consumer<SoftwareVersionInfo.Builder> changeSoftwareVersionInfo) {
        final SoftwareVersionInfo.Builder softwareVersionInfoBuilder = getSoftwareVersionInfoBuilder();
        changeSoftwareVersionInfo.accept(softwareVersionInfoBuilder);

        state.processMessage(wrapMessage(getRegisterBuilder(softwareVersionInfoBuilder.build()).build()));

        fail();
    }

    private AgentControl getRegistrationMessage() {
        return getRegistrationMessage("123", "2.0", "Alpha");
    }

    private AgentControl getRegistrationMessage(final String agentId, final String apiVersion, final String scope) {
        final Register registerMessage =
                Register
                .newBuilder()
                .setAgentId(agentId)
                .setSoftwareVersionInfo(getSoftwareVersionInfoBuilder().build())
                .setApiVersion(apiVersion)
                .setScope(scope)
                .build();

        return wrapMessage(registerMessage);
    }

    private AgentControl wrapMessage(final Register registerMessage) {
        return AgentControl.newBuilder().setAction(REGISTER).setAgentMessageType(AgentMessageType.REGISTER).setRegister(registerMessage)
                .build();
    }

    private SoftwareVersionInfo.Builder getSoftwareVersionInfoBuilder() {
        return SoftwareVersionInfo.newBuilder().setProductName("ProductName").setProductNumber("ProductNumber").setRevision("Revision")
                .setProductionDate("ProductionDate").setDescription("Description").setType("Type");
    }

    private Register.Builder getRegisterBuilder(final SoftwareVersionInfo softwareVersionInfo) {
        return Register.newBuilder().setAgentId("123").setSoftwareVersionInfo(softwareVersionInfo).setApiVersion("456").setScope("Alpha");
    }

}
