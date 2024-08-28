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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.action.Action.RESTORE;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.PREPARATION;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.EXECUTION;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.POST_ACTIONS;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.CANCEL_BACKUP_RESTORE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.restore.RestoreInformation;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.Preparation;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class AgentInputStreamTest {

    private StreamStub stub;
    private AgentInputStream inputStream;

    @Before
    public void setup() {
        stub = new StreamStub();
        inputStream = new AgentInputStream(stub);
    }

    @Test
    public void prepareForBackup_openStream_sendPreparationBackupMessageThroughStream() throws Exception {
        inputStream.prepareForBackup("asd", "DEFAULT");

        assertNotNull(stub.getMessage());
        assertEquals(BACKUP, stub.getMessage().getAction());
        assertEquals(PREPARATION, stub.getMessage().getOrchestratorMessageType());
        assertEquals("asd", stub.getMessage().getPreparation().getBackupName());
        assertEquals("DEFAULT", stub.getMessage().getPreparation().getBackupType());
    }

    @Test
    public void executeBackup_openStream_sendExecuteBackupMessageThroughStream() throws Exception {
        inputStream.executeBackup();

        assertNotNull(stub.getMessage());
        assertEquals(BACKUP, stub.getMessage().getAction());
        assertEquals(EXECUTION, stub.getMessage().getOrchestratorMessageType());
    }

    @Test
    public void executeBackupPostAction_openStream_sendPostActionBackupMessageThroughStream() {
        inputStream.executeBackupPostAction();

        assertNotNull(stub.getMessage());
        assertEquals(BACKUP, stub.getMessage().getAction());
        assertEquals(POST_ACTIONS, stub.getMessage().getOrchestratorMessageType());
    }

    @Test
    public void prepareForRestore_openStream_sendPreparationRestoreMessageThroughStream() throws Exception {
        final RestoreInformation restoreInformation = mockRestoreInformation(ApiVersion.API_V3_0);
        inputStream.prepareForRestore(restoreInformation);

        assertNotNull(stub.getMessage());
        assertEquals(RESTORE, stub.getMessage().getAction());
        assertEquals(PREPARATION, stub.getMessage().getOrchestratorMessageType());
        assertEquals("test_Backup", stub.getMessage().getPreparation().getBackupName());
        assertEquals(2, stub.getMessage().getPreparation().getFragmentCount());
        assertEquals(stub.getMessage().getPreparation().getFragment(0),
                Fragment.newBuilder().setFragmentId("F1").setSizeInBytes("123").setVersion("V1").build());
        assertEquals(stub.getMessage().getPreparation().getFragment(1),
                Fragment.newBuilder().setFragmentId("F2").setSizeInBytes("123").setVersion("V2").build());
        assertEquals("name", stub.getMessage().getPreparation().getSoftwareVersionInfo().getProductName());
        assertEquals("description", stub.getMessage().getPreparation().getSoftwareVersionInfo().getDescription());
        assertEquals("123", stub.getMessage().getPreparation().getSoftwareVersionInfo().getProductNumber());
        assertEquals("Rev", stub.getMessage().getPreparation().getSoftwareVersionInfo().getRevision());
        assertEquals("type", stub.getMessage().getPreparation().getSoftwareVersionInfo().getType());
        assertEquals("date", stub.getMessage().getPreparation().getSoftwareVersionInfo().getProductionDate());
    }

    @Test
    public void sendFragmentList() throws Exception {
        final RestoreInformation restoreInformation = mockRestoreInformation(ApiVersion.API_V4_0);
        inputStream.sendFragmentList(restoreInformation);

        assertEquals(stub.getMessage().getFragmentListEntry().getFragment(),
                Fragment.newBuilder().setFragmentId("F1").setSizeInBytes("123").setVersion("V1").build());
    }

    @Test
    public void executeRestore_openStream_sendExecuteRestoreMessageThroughStream() throws Exception {
        inputStream.executeRestore();

        assertNotNull(stub.getMessage());
        assertEquals(RESTORE, stub.getMessage().getAction());
        assertEquals(EXECUTION, stub.getMessage().getOrchestratorMessageType());
    }

    @Test
    public void executeRestorePostAction_openStream_sendPostActionRestoreMessageThroughStream() throws Exception {
        inputStream.executeRestorePostAction();

        assertNotNull(stub.getMessage());
        assertEquals(RESTORE, stub.getMessage().getAction());
        assertEquals(POST_ACTIONS, stub.getMessage().getOrchestratorMessageType());
    }

    @Test
    public void cancelAction_action_sendCancelAction() throws Exception {
        inputStream.cancelAction(RESTORE);

        assertNotNull(stub.getMessage());
        assertEquals(RESTORE, stub.getMessage().getAction());
        assertEquals(CANCEL_BACKUP_RESTORE, stub.getMessage().getOrchestratorMessageType());
    }

    @Test
    public void close_openStream_closesStream() throws Exception {
        inputStream.close();

        assertTrue(stub.onCompletedWasCalled());
    }

    @Test
    public void close_failsToClose_doesntThrowException() throws Exception {
        final StreamExceptionStub stub = new StreamExceptionStub();
        final AgentInputStream inputStream = new AgentInputStream(stub);

        inputStream.close();

        assertTrue(stub.threwException());
    }

    @Test
    public void close_openStreamAndException_closesStreamSendingError() throws Exception {
        final StatusRuntimeException exception = Status.ABORTED.asRuntimeException();
        inputStream.close(exception);

        assertEquals(exception, stub.getThrowable());
    }

    @Test
    public void close_failsToSendErrorThroughStream_doesNotThrowException() throws Exception {
        final StreamExceptionStub stub = new StreamExceptionStub();
        final AgentInputStream inputStream = new AgentInputStream(stub);
        final StatusRuntimeException exception = Status.ABORTED.asRuntimeException();
        inputStream.close(exception);

        assertTrue(stub.threwException());
    }

    private RestoreInformation mockRestoreInformation(final ApiVersion apiVersion) {
        final RestoreInformation restoreInformation = createMock(RestoreInformation.class);
        final Agent agent = createMock(Agent.class);
        if ( apiVersion == ApiVersion.API_V4_0) {
            restoreInformation.setAgent(Optional.of(agent));
            expect(agent.getApiVersion()).andReturn(ApiVersion.API_V4_0);
            List<Fragment> fragments = new ArrayList<>();
            fragments.add(Fragment.newBuilder().setFragmentId("F1").setSizeInBytes("123").setVersion("V1").build());
            expect(restoreInformation.getBackupFragments()).andReturn(fragments);
            expect(restoreInformation.buildPreparationMessage()).andReturn(Preparation.newBuilder().setBackupName("test_Backup")
                    .setSoftwareVersionInfo(SoftwareVersionInfo.newBuilder().setDescription("description")
                            .setProductionDate("date").setProductName("name").setProductNumber("123").setRevision("Rev").setType("type").build())
                    .build());
        } else {
            expect(restoreInformation.buildPreparationMessage()).andReturn(Preparation.newBuilder().setBackupName("test_Backup")
                    .addAllFragment(getFragments()).setSoftwareVersionInfo(SoftwareVersionInfo.newBuilder().setDescription("description")
                            .setProductionDate("date").setProductName("name").setProductNumber("123").setRevision("Rev").setType("type").build())
                    .build());
        }

        replay(restoreInformation, agent);
        return restoreInformation;
    }

    private List<Fragment> getFragments() {
        final List<Fragment> fragments = new ArrayList<>();
        fragments.add(Fragment.newBuilder().setFragmentId("F1").setSizeInBytes("123").setVersion("V1").build());
        fragments.add(Fragment.newBuilder().setFragmentId("F2").setSizeInBytes("123").setVersion("V2").build());
        return fragments;
    }

    private class StreamStub implements StreamObserver<OrchestratorControl> {

        private boolean onCompletedWasCalled;
        private Throwable throwable;
        private OrchestratorControl message;

        @Override
        public void onCompleted() {
            onCompletedWasCalled = true;
        }

        @Override
        public void onError(final Throwable arg0) {
            throwable = arg0;
        }

        @Override
        public void onNext(final OrchestratorControl arg0) {
            message = arg0;
        }

        public boolean onCompletedWasCalled() {
            return onCompletedWasCalled;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public OrchestratorControl getMessage() {
            return message;
        }

    }

    private class StreamExceptionStub implements StreamObserver<OrchestratorControl> {

        private boolean threwException;

        @Override
        public void onCompleted() {
            threwException = true;
            throw new RuntimeException("Boo");
        }

        @Override
        public void onError(final Throwable arg0) {
            threwException = true;
            throw new RuntimeException("Boo again");
        }

        @Override
        public void onNext(final OrchestratorControl arg0) {
            //Does nothing
        }

        public boolean threwException() {
            return threwException;
        }

    }
}
