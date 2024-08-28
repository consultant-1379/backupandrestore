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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ericsson.adp.mgmt.data.CustomMetadataFileChunk;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.google.protobuf.ByteString;

public class SystemTestBackupFragmentWithCustomMetadata extends SystemTestBackupFragment {

    private final String customMetadataFileName;
    private final List<String> customMetadataChunks = new ArrayList<>();

    public SystemTestBackupFragmentWithCustomMetadata(final String agentId, final String backupName, final String fragmentId, final String fileName,
                                    final List<String> fileChunks, final String customMetadataFileName, final List<String> customMetadataChunks) {
        super(agentId, backupName, fragmentId, fileName, fileChunks);
        this.customMetadataFileName = customMetadataFileName;
        this.customMetadataChunks.addAll(customMetadataChunks);
    }

    @Override
    public void sendThroughAgent(final SystemTestAgent agent) {
        agent.sendMetadata(agentId, backupName, fragmentId);
        agent.sendDataFileName(fileName);
        agent.sendData(fileChunks);
        agent.sendDataChecksum(calculateChecksum(fileChunks));
        waitUntil(() -> fragmentFolder.getDataFileFolder().resolve(fileName).toFile().exists());
        agent.sendCustomMetadataFileName(customMetadataFileName);
        agent.sendCustomMetadata(customMetadataChunks);
        agent.sendCustomMetadataChecksum(calculateChecksum(customMetadataChunks));
        waitUntil(() -> fragmentFolder.getCustomMetadataFileFolder().resolve(customMetadataFileName).toFile().exists());
    }

    @Override
    public List<RestoreData> getExpectedRestoreMessages() {
        final List<RestoreData> restoreMessages = super.getExpectedRestoreMessages();
        restoreMessages.add(getFileNameMessage());
        restoreMessages.addAll(getChunkMessages());
        restoreMessages.add(getChecksumMessage());
        return restoreMessages;
    }

    @Override
    public Optional<String> getBackedUpCustomMetadataContent() throws Exception {
        return Optional.of(readFile(fragmentFolder.getCustomMetadataFileFolder().resolve(customMetadataFileName)));
    }

    @Override
    public Optional<String> getCustomMetadataFileName() {
        return Optional.of(customMetadataFileName);
    }

    @Override
    public List<String> getCustomMetadataChunks() {
        return customMetadataChunks;
    }

    private RestoreData getFileNameMessage() {
        return RestoreData
                .newBuilder()
                .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setFileName(customMetadataFileName).build())
                .build();
    }

    private List<RestoreData> getChunkMessages() {
        return customMetadataChunks.stream().map(this::getChunkMessage).collect(Collectors.toList());
    }

    private RestoreData getChunkMessage(final String chunk) {
        return RestoreData
                .newBuilder()
                .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setContent(ByteString.copyFromUtf8(chunk)).build())
                .build();
    }

    private RestoreData getChecksumMessage() {
        return RestoreData
                .newBuilder()
                .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setChecksum(calculateChecksum(customMetadataChunks)).build())
                .build();
    }

}
