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
package com.ericsson.adp.mgmt.backupandrestore.test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import com.ericsson.adp.mgmt.backupandrestore.util.SetTimeouts;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.ControlInterfaceGrpc;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.control.StageComplete;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.CustomMetadataFileChunk;
import com.ericsson.adp.mgmt.data.DataInterfaceGrpc;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;

public class SystemTestAgent {

    private static final Logger log = LogManager.getLogger(SystemTestAgent.class);

    private final String agentId;
    private String scope = "";
    private ManagedChannel channel;
    private StreamObserver<AgentControl> outputControlStream;
    private StreamObserver<BackupData> backupDataChannel;
    private SystemTestAgentInputControlStream inputControlStream;
    private String backupName;
    private List<Fragment> fragments;
    private final Map<String, List<RestoreData>> restoredFragments = new HashMap<>();
    private Optional<OrchestratorControl> stage = Optional.empty();
    private Throwable dataChannelError;

    public SystemTestAgent() {
        this("-");
    }

    public SystemTestAgent(final String agentId) {
        this.agentId = agentId;
        channel = ManagedChannelBuilder.forAddress("127.0.0.1", 3000).usePlaintext().build();
    }

    public SystemTestAgent(final String agentId, final String scope) {
        this(agentId);
        this.scope = scope;
    }

    public void register(final ApiVersion apiVersion) {
        inputControlStream = new SystemTestAgentInputControlStream();
        inputControlStream.setAgent(this);
        outputControlStream = ControlInterfaceGrpc.newStub(channel).establishControlChannel(inputControlStream);

        final SoftwareVersionInfo softwareVersionInfo = SoftwareVersionInfo.newBuilder().setProductName("ProductName")
            .setProductNumber("ProductNumber").setRevision("Revision").setProductionDate("ProductionDate").setDescription("Description")
            .setType("Type").build();

        final Register registerMessage = Register.newBuilder().setAgentId(agentId).setSoftwareVersionInfo(softwareVersionInfo)
            .setApiVersion(apiVersion.getStringRepresentation())
            .setScope(scope).build();

        final AgentControl agentControl = AgentControl.newBuilder().setAction(Action.REGISTER).setAgentMessageType(AgentMessageType.REGISTER)
            .setRegister(registerMessage).build();

        outputControlStream.onNext(agentControl);
    }

    public void sendMetadata(final String agentId, final String backupName, final String fragmentId) {
        sendMetadata(agentId, backupName, fragmentId, new HashMap<>());
    }

    public void sendMetadata(final String agentId, final String backupName, final String fragmentId, final Map<String, String> customInformation) {
        final Metadata metadata = Metadata
            .newBuilder()
            .setAgentId(agentId)
            .setFragment(Fragment.newBuilder().setFragmentId(fragmentId).setSizeInBytes("bytes").setVersion("version")
                .putAllCustomInformation(customInformation).build())
            .setBackupName(backupName)
            .build();

        final BackupData message = BackupData.newBuilder().setMetadata(metadata).setDataMessageType(DataMessageType.METADATA).build();

        getBackupDataChannel().onNext(message);
    }

    public void sendDataFileName(final String fileName) {
        final BackupData fileNameMessage = BackupData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
            .setBackupFileChunk(BackupFileChunk.newBuilder().setFileName(fileName).build()).build();
        getBackupDataChannel().onNext(fileNameMessage);
    }

    public void sendData(final List<String> chunks) {
        chunks.forEach(content -> {
            final BackupFileChunk chunk = BackupFileChunk.newBuilder().setContent(ByteString.copyFrom(content.getBytes())).build();
            getBackupDataChannel().onNext(BackupData.newBuilder().setBackupFileChunk(chunk).setDataMessageType(DataMessageType.BACKUP_FILE).build());
        });
    }

    public void sendDataChecksum(final String checksum) {
        final BackupFileChunk chunk = BackupFileChunk.newBuilder().setChecksum(checksum).build();
        getBackupDataChannel().onNext(BackupData.newBuilder().setBackupFileChunk(chunk).setDataMessageType(DataMessageType.BACKUP_FILE).build());
    }

    public void sendCustomMetadataFileName(final String fileName) {
        final BackupData fileNameCustomDataMessage = BackupData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
            .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setFileName(fileName).build()).build();
        getBackupDataChannel().onNext(fileNameCustomDataMessage);
    }

    public void sendCustomMetadata(final List<String> customMetadataChunks) {
        customMetadataChunks.forEach(content -> {
            final CustomMetadataFileChunk chunk = CustomMetadataFileChunk.newBuilder().setContent(ByteString.copyFrom(content.getBytes())).build();
            getBackupDataChannel().onNext(
                BackupData.newBuilder().setCustomMetadataFileChunk(chunk).setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE).build());
        });
    }

    public void sendCustomMetadataChecksum(final String checksum) {
        final CustomMetadataFileChunk chunk = CustomMetadataFileChunk.newBuilder().setChecksum(checksum).build();
        getBackupDataChannel()
            .onNext(BackupData.newBuilder().setCustomMetadataFileChunk(chunk).setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE).build());
    }

    public void sendOnErrorOnDataChannel() {
        backupDataChannel.onError(new RuntimeException());
        backupDataChannel = null;
    }

    public void sendOnErrorOnControlChannel() {
        outputControlStream.onError(new RuntimeException());
        outputControlStream = null;
    }

    public void closeDataChannel() {
        if (backupDataChannel != null) {
            backupDataChannel.onCompleted();
            backupDataChannel = null;
        }
    }

    public void closeControlChannel() {
        if (outputControlStream != null) {
            outputControlStream.onCompleted();
            outputControlStream = null;
        }
    }

    public void sendStageCompleteMessage(final Action action, final boolean success) {
        //FIXME: Very rough, probably doesn't work in all cases.
        final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        final String callingMethod = trace[2].getMethodName();
        final String message = MessageFormat.format("[ Caller: {0}, AgentId: {1} ]", callingMethod, agentId);
        sendStageCompleteMessage(action, message, success);
    }

    private void sendStageCompleteMessage(final Action action, final String message, final boolean success) {
        if (outputControlStream != null) {
            outputControlStream.onNext(getStageCompleteMessage(action, message, success));
        }
    }

    public void shutdown() {
        closeDataChannel();
        closeControlChannel();

        if (channel != null) {
            channel.shutdownNow();
            try {
                channel.awaitTermination(SetTimeouts.TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                log.error("!!!Shutdown Failed!!!");
                e.printStackTrace();
            }
            channel = null;
        }

        backupName = null;
    }

    public String getBackupName() {
        return backupName;
    }

    public void clearBackupName() {
        backupName = null;
    }

    public boolean isParticipatingInAction() {
        return backupName != null;
    }

    public Map<String, List<RestoreData>> getRestoredFragments() {
        return restoredFragments;
    }

    public List<Fragment> getFragments() {
        return fragments;
    }

    public void downloadFragments() {
        sendStageCompleteMessage(Action.RESTORE, true);
        waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.EXECUTION);
        downloadFiles();
        sendStageCompleteMessage(Action.RESTORE, true);
        waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.POST_ACTIONS);
        sendStageCompleteMessage(Action.RESTORE,true);
    }

    public String getAgentId() {
        return agentId;
    }

    public Iterator<RestoreData> getRestoreDataIterator(final Metadata metadata) {
        return DataInterfaceGrpc.newBlockingStub(channel).restore(metadata);
    }

    public StreamObserver<BackupData> getBackupDataChannel() {
        if (backupDataChannel == null) {
            backupDataChannel = DataInterfaceGrpc.newStub(channel).backup(new OrchestratorMessageStream());
        }
        return backupDataChannel;
    }

    public boolean backupDataChannelIsOpen() {
        return backupDataChannel != null;
    }

    public SystemTestAgentInputControlStream getInputControlStream() {
        return inputControlStream;
    }

    public void waitUntilStageIsReached(final Action action, final OrchestratorMessageType stage) {
        Awaitility.await().atMost(SetTimeouts.TIMEOUT_SECONDS, TimeUnit.SECONDS).until(() ->
            this.stage.map(message -> action.equals(message.getAction()) && stage.equals(message.getOrchestratorMessageType())).orElse(false)
        );
    }

    public Throwable getDataChannelError() {
        return dataChannelError;
    }


    private void downloadFiles() {
        for (final String fragmentId : fragments.stream().map(Fragment::getFragmentId).collect(Collectors.toList())) {
            final List<RestoreData> messages = new ArrayList<>();
            final Iterator<RestoreData> iterator = getRestoreDataIterator(getMetadata(fragmentId));
            while (iterator.hasNext()) {
                messages.add(iterator.next());
            }
            restoredFragments.put(fragmentId, messages);
        }
    }

    private Metadata getMetadata(final String fragmentId) {
        return Metadata.newBuilder().setAgentId(agentId)
            .setFragment(Fragment.newBuilder().setFragmentId(fragmentId).setSizeInBytes("bytes").setVersion("version"))
            .setBackupName(backupName).build();
    }

    private AgentControl getStageCompleteMessage(final Action action, final String message, final boolean success) {
        final StageComplete stageComplete = StageComplete.newBuilder().setMessage("Ended: " + message).setSuccess(success).build();
        return AgentControl.newBuilder().setAction(action).setAgentMessageType(AgentMessageType.STAGE_COMPLETE).setStageComplete(stageComplete)
            .build();
    }

    public class SystemTestAgentInputControlStream implements StreamObserver<OrchestratorControl> {
        private SystemTestAgent agent;

        private Throwable error;

        public void setAgent(final SystemTestAgent agent) {
            this.agent = agent;
        }

        @Override
        public void onCompleted() {
            //Not needed
        }

        @Override
        public void onError(final Throwable error) {
            this.error = error;
        }

        @Override
        public void onNext(final OrchestratorControl message) {
            stage = Optional.of(message);
            if (Action.BACKUP.equals(message.getAction()) && OrchestratorMessageType.PREPARATION.equals(message.getOrchestratorMessageType())) {
                backupName = message.getPreparation().getBackupName();
            }
            if (Action.RESTORE.equals(message.getAction()) && OrchestratorMessageType.PREPARATION.equals(message.getOrchestratorMessageType())) {
                backupName = message.getPreparation().getBackupName();
                fragments = message.getPreparation().getFragmentList();
            }
        }

        public Throwable getError() {
            return error;
        }

    }

    private class OrchestratorMessageStream implements StreamObserver<Empty> {

        @Override
        public void onCompleted() {
            //Not needed
        }

        @Override
        public void onError(final Throwable error) {
            if (!(error instanceof StatusRuntimeException) || !((StatusRuntimeException) error).getStatus().getCode()
                .equals(Status.CANCELLED.getCode())) {
                backupDataChannel = null;
            }
            dataChannelError = error;
        }

        @Override
        public void onNext(final Empty arg0) {
            //Not needed
        }

    }

}
