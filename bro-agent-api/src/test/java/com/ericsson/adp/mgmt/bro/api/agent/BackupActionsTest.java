/*
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 * ****************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.bro.api.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ericsson.adp.mgmt.bro.api.grpc.GrpcServiceIntegrationTest;
import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.DataInterfaceGrpc.DataInterfaceImplBase;
import com.google.protobuf.Empty;
import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class BackupActionsTest extends GrpcServiceIntegrationTest {

    private Agent agent;
    private DataService dataService;
    private ControlService controlService;
    private BackupActions backupActions;
    private ActionInformation actionInformation;

    @Before
    public void setup() {
        final OrchestratorGrpcChannel orchestratorGrpcChannel = new OrchestratorGrpcChannel(channel);
        agent = new Agent(new TestAgentBehavior(), orchestratorGrpcChannel);
        orchestratorGrpcChannel.establishControlChannel(new TestOrchestratorStreamObserver(agent));
        actionInformation = new ActionInformation("backup", "DEFAULT");
        backupActions = new BackupPreparationActions(agent, actionInformation);
    }

    @Test
    public void getBackupName_backupName() {
        assertEquals("backup", backupActions.getBackupName());
    }

    @Test
    public void getBackupType_backupType() {
        assertEquals("DEFAULT", backupActions.getBackupType());
    }

    @Test
    public void sendStageComplete_success_stageCompleteTrue() {
        backupActions.sendStageComplete(true, "success");
        assertTrue(controlService.getMessage().getStageComplete().getSuccess());
    }

    @Override
    protected List<BindableService> getServices() {
        dataService = new DataService();
        controlService = new ControlService();
        return Arrays.asList(dataService, controlService);
    }

    private class DataService extends DataInterfaceImplBase {
        @Override
        public StreamObserver<BackupData> backup(StreamObserver<Empty> responseObserver) {
            return new StreamObserver<BackupData>() {
                @Override
                public void onNext(final BackupData message) {
                    responseObserver.onCompleted();
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
