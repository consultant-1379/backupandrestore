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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.ericsson.adp.mgmt.control.FragmentListEntry;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.action.CancelBackupRestore;
import com.ericsson.adp.mgmt.bro.api.exception.FailedToDownloadException;
import com.ericsson.adp.mgmt.bro.api.fragment.BackupFragmentInformation;
import com.ericsson.adp.mgmt.bro.api.fragment.FragmentInformation;
import com.ericsson.adp.mgmt.bro.api.grpc.GrpcServiceIntegrationTest;
import com.ericsson.adp.mgmt.bro.api.registration.SoftwareVersion;
import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.bro.api.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.ControlInterfaceGrpc;
import com.ericsson.adp.mgmt.control.Execution;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import com.ericsson.adp.mgmt.control.PostActions;
import com.ericsson.adp.mgmt.control.Preparation;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.CustomMetadataFileChunk;
import com.ericsson.adp.mgmt.data.DataInterfaceGrpc.DataInterfaceImplBase;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;
import com.google.protobuf.ByteString;

import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;

public class RestoreIntegrationTest extends GrpcServiceIntegrationTest {

    private static final String RESTORED_FILE_NAME = "restoredFile.txt";
    private static final String CUSTOM_METADATA_FILE_NAME = "customMetadataFile.txt";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Agent agent;
    private SoftwareVersion softwareVersion;
    private List<FragmentInformation> fragmentList = new ArrayList<>();
    private DataService dataService;
    private ControlService controlService;
    RestoreTestAgentBehavior agentBehavior = new RestoreTestAgentBehavior();

    @Before
    public void setUp() {
        final OrchestratorGrpcChannel orchGrpcChannel = new OrchestratorGrpcChannel(this.channel);

        this.agent = new Agent(this.agentBehavior, orchGrpcChannel);

        orchGrpcChannel.establishControlChannel(new TestOrchestratorStreamObserver(this.agent));

        this.agent.process(getPreparationMessage());
        agent.process(getFragmentListEntry(Fragment.newBuilder().setFragmentId("id").setSizeInBytes("size").setVersion("version").build(), false));
        agent.process(getFragmentListEntry(Fragment.newBuilder().setFragmentId("id2").setSizeInBytes("size2").setVersion("version2").putAllCustomInformation(getCustomInformation()).build(), true));
        assertTrue(this.agentBehavior.preparedForRestore());
        assertTrue(this.controlService.getMessage().getStageComplete().getSuccess());
    }

    @Test
    public void restore_fragment_downloadsFragment() throws Exception {
        this.agent.process(getExecutionMessage());

        final AgentControl agentControlMessageExecutionComplete = this.controlService.getMessage();
        assertTrue(agentControlMessageExecutionComplete.getStageComplete().getSuccess());
        assertEquals("Success", agentControlMessageExecutionComplete.getStageComplete().getMessage());

        assertEquals(Arrays.asList("bytes"), Files.readAllLines(getRestoreLocation().resolve(RESTORED_FILE_NAME)));

        assertEquals("R1", this.softwareVersion.getRevision());
        assertEquals("id", this.softwareVersion.getProductNumber());

        assertEquals(2, this.fragmentList.size());

        final FragmentInformation firstFragment = this.fragmentList.get(0);
        assertEquals("id", firstFragment.getFragmentId());
        assertTrue(firstFragment.getCustomInformation().isEmpty());

        final FragmentInformation secondFragment = this.fragmentList.get(1);
        assertEquals("id2", secondFragment.getFragmentId());
        assertEquals(getCustomInformation(), secondFragment.getCustomInformation());
        assertTrue(this.agentBehavior.executedRestore());

        this.agent.process(getPostActionMessage());
        assertTrue(this.agentBehavior.performedPostRestore());
    }

    @Test
    public void restore_receivedCancelMessageAfterPostActions_cancelActionsExecuted() throws Exception {
        this.agent.process(getExecutionMessage());

        final AgentControl agentControlMessageExecutionComplete = this.controlService.getMessage();
        assertTrue(agentControlMessageExecutionComplete.getStageComplete().getSuccess());
        assertEquals("Success", agentControlMessageExecutionComplete.getStageComplete().getMessage());

        assertEquals(Arrays.asList("bytes"), Files.readAllLines(getRestoreLocation().resolve(RESTORED_FILE_NAME)));

        assertEquals("R1", this.softwareVersion.getRevision());
        assertEquals("id", this.softwareVersion.getProductNumber());

        assertEquals(2, this.fragmentList.size());

        final FragmentInformation firstFragment = this.fragmentList.get(0);
        assertEquals("id", firstFragment.getFragmentId());
        assertTrue(firstFragment.getCustomInformation().isEmpty());

        final FragmentInformation secondFragment = this.fragmentList.get(1);
        assertEquals("id2", secondFragment.getFragmentId());
        assertEquals(getCustomInformation(), secondFragment.getCustomInformation());
        assertTrue(this.agentBehavior.executedRestore());

        this.agent.process(getPostActionMessage());
        assertTrue(this.agentBehavior.performedPostRestore());

        this.agent.process(getCancelMessage());
        assertTrue(this.agentBehavior.cancelledAction());
    }

    @Test
    public void restore_receivedCancelMessageAfterExecution_notPerformedPostRestore() throws Exception {
        this.agent.process(getExecutionMessage());

        final AgentControl agentControlMessageExecutionComplete = this.controlService.getMessage();
        assertTrue(agentControlMessageExecutionComplete.getStageComplete().getSuccess());
        assertEquals("Success", agentControlMessageExecutionComplete.getStageComplete().getMessage());

        assertEquals(Arrays.asList("bytes"), Files.readAllLines(getRestoreLocation().resolve(RESTORED_FILE_NAME)));

        assertEquals("R1", this.softwareVersion.getRevision());
        assertEquals("id", this.softwareVersion.getProductNumber());

        assertEquals(2, this.fragmentList.size());

        final FragmentInformation firstFragment = this.fragmentList.get(0);
        assertEquals("id", firstFragment.getFragmentId());
        assertTrue(firstFragment.getCustomInformation().isEmpty());

        final FragmentInformation secondFragment = this.fragmentList.get(1);
        assertEquals("id2", secondFragment.getFragmentId());
        assertEquals(getCustomInformation(), secondFragment.getCustomInformation());
        assertTrue(this.agentBehavior.executedRestore());

        this.agent.process(getCancelMessage());
        assertTrue(this.agentBehavior.cancelledAction());

        this.agent.process(getPostActionMessage());
        assertFalse(this.agentBehavior.performedPostRestore());
    }

    @Test
    public void restore_receivedCancelMessageAfterPreparation_notExecutedRestore() throws Exception {
        this.agent.process(getCancelMessage());
        assertTrue(this.agentBehavior.cancelledAction());

        this.agent.process(getExecutionMessage());
        assertFalse(this.agentBehavior.executedRestore());
    }

    @Test
    public void restore_errorOnFragmentDownload_failsRestore() {
        this.dataService.setDataServiceOption(DataServiceOption.SEND_ERROR);

        this.agent.process(getExecutionMessage());

        final AgentControl agentControlMessageExecutionComplete = this.controlService.getMessage();
        assertFalse(agentControlMessageExecutionComplete.getStageComplete().getSuccess());
        assertEquals("Error at Orchestrator while downloading data during restore",
                agentControlMessageExecutionComplete.getStageComplete().getMessage());
        assertTrue(this.agentBehavior.executedRestore());

        this.agent.process(getPostActionMessage());
        assertTrue(this.agentBehavior.performedPostRestore());
    }

    @Test
    public void restore_backupDataDoesNotMatchChecksum_failsRestore() {
        this.dataService.setDataServiceOption(DataServiceOption.INCORRECT_CHECKSUM);

        this.agent.process(getExecutionMessage());

        final AgentControl agentControlMessageExecutionComplete = this.controlService.getMessage();
        assertFalse(agentControlMessageExecutionComplete.getStageComplete().getSuccess());

        final String expectedMessage = "The checksum for the file:" + RESTORED_FILE_NAME +
                " did not match the received value from the orchestrator";
        assertEquals(expectedMessage, agentControlMessageExecutionComplete.getStageComplete().getMessage());
        assertTrue(this.agentBehavior.executedRestore());

        this.agent.process(getPostActionMessage());
        assertTrue(this.agentBehavior.performedPostRestore());
    }

    @Test
    public void restore_customMetadataDoesNotMatchChecksum_failsRestore() {
        this.dataService.setDataServiceOption(DataServiceOption.INCORRECT_CHECKSUM_CUSTOM_METADATA);

        this.agent.process(getExecutionMessage());

        final AgentControl agentControlMessageExecutionComplete = this.controlService.getMessage();
        assertFalse(agentControlMessageExecutionComplete.getStageComplete().getSuccess());

        final String expectedMessage = "The checksum for the file:" + CUSTOM_METADATA_FILE_NAME +
                " did not match the received value from the orchestrator";
        assertEquals(expectedMessage, agentControlMessageExecutionComplete.getStageComplete().getMessage());
        assertTrue(this.agentBehavior.executedRestore());

        this.agent.process(getPostActionMessage());
        assertTrue(this.agentBehavior.performedPostRestore());
    }

    private class RestoreTestAgentBehavior extends TestAgentBehavior {

        private boolean executedRestore;

        @Override
        public void executeRestore(final RestoreExecutionActions restoreExecutionActions) {
            final FragmentInformation fragment = new BackupFragmentInformation();

            fragment.setFragmentId("backupFragmentId");
            fragment.setSizeInBytes("sizeInBytes");
            fragment.setVersion("version");

            RestoreIntegrationTest.this.softwareVersion = restoreExecutionActions.getSoftwareVersion();
            RestoreIntegrationTest.this.fragmentList = restoreExecutionActions.getFragmentList();
            this.executedRestore = true;
            try {
                restoreExecutionActions.downloadFragment(fragment, getRestoreLocation().toString());
                restoreExecutionActions.restoreComplete(true, "Success");
            } catch (final FailedToDownloadException e) {
                restoreExecutionActions.restoreComplete(false, e.getMessage());
            }
        }

        @Override
        public boolean executedRestore() {
            return this.executedRestore;
        }
    }

    private Map<String, String> getCustomInformation() {
        final Map<String, String> customInformation = new HashMap<>();
        customInformation.put("b", "2");
        customInformation.put("c", "3");
        return customInformation;
    }

    private OrchestratorControl getPreparationMessage() {
        return OrchestratorControl
                .newBuilder()
                .setAction(Action.RESTORE)
                .setOrchestratorMessageType(OrchestratorMessageType.PREPARATION)
                .setPreparation(getPreparation())
                .build();
    }

    private Preparation getPreparation() {
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(Fragment.newBuilder().build());
        return Preparation
                .newBuilder()
                .setBackupName("IntegrationTestForRestoreActions")
                .setSoftwareVersionInfo(getSoftwareVersionInfo())
                .addAllFragment(fragments)
                .build();
    }

    private SoftwareVersionInfo getSoftwareVersionInfo() {
        return SoftwareVersionInfo.newBuilder().setProductName("name").setDescription("description")
                .setProductNumber("id").setProductionDate("date").setRevision("R1").setType("type").build();
    }

    private OrchestratorControl getExecutionMessage() {
        return OrchestratorControl
                .newBuilder()
                .setAction(Action.RESTORE)
                .setOrchestratorMessageType(OrchestratorMessageType.EXECUTION)
                .setExecution(Execution.newBuilder().build())
                .build();
    }

    private OrchestratorControl getPostActionMessage() {
        return OrchestratorControl.newBuilder()
                .setOrchestratorMessageType(OrchestratorMessageType.POST_ACTIONS)
                .setAction(Action.RESTORE)
                .setPostActions(PostActions.newBuilder().build())
                .build();
    }

    private OrchestratorControl getCancelMessage() {
        return OrchestratorControl.newBuilder()
                .setOrchestratorMessageType(OrchestratorMessageType.CANCEL_BACKUP_RESTORE)
                .setAction(Action.RESTORE)
                .setCancel(CancelBackupRestore.newBuilder().build())
                .build();
    }

    private OrchestratorControl getFragmentListEntry(final Fragment fragment, final boolean lastFragment) {
        return OrchestratorControl.newBuilder().setFragmentListEntry(FragmentListEntry.newBuilder()
                .setLast(lastFragment)
                .setFragment(fragment).build()).build();
    }

    @Override
    protected List<BindableService> getServices() {
        this.dataService = new DataService();
        this.controlService = new ControlService();
        return Arrays.asList(this.dataService, this.controlService);
    }

    private class ControlService extends ControlInterfaceGrpc.ControlInterfaceImplBase {

        private final List<AgentControl> messages = new LinkedList<>();

        @Override
        public StreamObserver<AgentControl> establishControlChannel(final StreamObserver<OrchestratorControl> responseObserver) {
            return new StreamObserver<AgentControl>() {
                @Override
                public void onNext(final AgentControl value) {
                    ControlService.this.messages.add(value);
                }

                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(final Throwable t) {

                }
            };
        }

        public AgentControl getMessage() {
            Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> this.messages.size() == 1);
            final AgentControl message = this.messages.remove(0);
            return message;
        }

    }

    private class DataService extends DataInterfaceImplBase {

        private DataServiceOption dataServiceOption = DataServiceOption.VALID_BACKUP_FILE;

        @Override
        public void restore(final Metadata request, final StreamObserver<RestoreData> responseObserver) {
            switch (this.dataServiceOption) {
                case VALID_BACKUP_FILE:
                    sendValidBackupFile(responseObserver);
                    break;
                case SEND_ERROR:
                    sendError(responseObserver);
                    break;
                case INCORRECT_CHECKSUM:
                    sendIncorrectChecksum(responseObserver);
                    break;
                case INCORRECT_CHECKSUM_CUSTOM_METADATA:
                    sendIncorrectChecksumForCustomMetadata(responseObserver);
                    break;
            }

        }

        public void setDataServiceOption(final DataServiceOption dataServiceOption) {
            this.dataServiceOption = dataServiceOption;
        }

        private void sendValidBackupFile(final StreamObserver<RestoreData> responseObserver) {
            sendBackupFileName(RESTORED_FILE_NAME, responseObserver);
            sendBackupFileContent(ByteString.copyFrom("bytes".getBytes()), responseObserver);
            sendBackupChecksum(getChecksum(), responseObserver);
            responseObserver.onCompleted();
        }

        private void sendError(final StreamObserver<RestoreData> responseObserver) {
            sendBackupFileName(RESTORED_FILE_NAME, responseObserver);
            sendBackupFileContent(ByteString.copyFrom("bytes".getBytes()), responseObserver);
            responseObserver.onError(new RuntimeException());
        }

        private void sendIncorrectChecksum(final StreamObserver<RestoreData> responseObserver) {
            sendBackupFileName(RESTORED_FILE_NAME, responseObserver);
            sendBackupFileContent(ByteString.copyFrom("bytes".getBytes()), responseObserver);
            sendBackupChecksum("invalidChecksum", responseObserver);
            responseObserver.onCompleted();
        }

        private void sendIncorrectChecksumForCustomMetadata(final StreamObserver<RestoreData> responseObserver) {
            sendBackupFileName(RESTORED_FILE_NAME, responseObserver);
            sendBackupFileContent(ByteString.copyFrom("bytes".getBytes()), responseObserver);
            sendBackupChecksum(getChecksum(), responseObserver);
            sendCustomMetadataFileName(CUSTOM_METADATA_FILE_NAME, responseObserver);
            sendCustomMetadataFileContent(ByteString.copyFrom("bytes".getBytes()), responseObserver);
            sendCustomMetadataChecksum("invalidChecksum", responseObserver);
            responseObserver.onCompleted();
        }

        private void sendBackupFileName(final String fileName, final StreamObserver<RestoreData> responseObserver) {
            responseObserver.onNext(RestoreData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                    .setBackupFileChunk(BackupFileChunk.newBuilder().setFileName(fileName).build()).build());
        }

        private void sendCustomMetadataFileName(final String fileName, final StreamObserver<RestoreData> responseObserver) {
            responseObserver.onNext(RestoreData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                    .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setFileName(fileName).build()).build());
        }

        private void sendBackupFileContent(final ByteString bytes, final StreamObserver<RestoreData> responseObserver) {
            responseObserver.onNext(RestoreData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                    .setBackupFileChunk(BackupFileChunk.newBuilder().setContent(bytes).build()).build());
        }

        private void sendCustomMetadataFileContent(final ByteString bytes, final StreamObserver<RestoreData> responseObserver) {
            responseObserver.onNext(RestoreData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                    .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setContent(bytes).build()).build());
        }

        private void sendBackupChecksum(final String checksum, final StreamObserver<RestoreData> responseObserver) {
            responseObserver.onNext(RestoreData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                    .setBackupFileChunk(BackupFileChunk.newBuilder().setChecksum(checksum).build()).build());
        }

        private void sendCustomMetadataChecksum( final String checksum, final StreamObserver<RestoreData> responseObserver) {
            responseObserver.onNext(RestoreData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                    .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setChecksum(checksum).build()).build());
        }
    }

    private enum DataServiceOption {
        VALID_BACKUP_FILE,
        SEND_ERROR,
        INCORRECT_CHECKSUM,
        INCORRECT_CHECKSUM_CUSTOM_METADATA
    }

    private String getChecksum() {
        final ChecksumCalculator calculator = new ChecksumCalculator();
        calculator.addBytes("bytes".getBytes());
        return calculator.getChecksum();
    }

    private class TestOrchestratorStreamObserver extends OrchestratorStreamObserver {

        public TestOrchestratorStreamObserver(final Agent agent) {
            super(agent);
        }

        @Override
        public void onError(final Throwable throwable) {

        }
    }

    private Path getRestoreLocation() {
        return this.folder.getRoot().toPath();
    }

}
