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

import com.ericsson.adp.mgmt.backupandrestore.util.SetTimeouts;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.awaitility.Awaitility;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.google.protobuf.ByteString;

public class SystemTestBackupFragment {

    protected final String agentId;
    protected final String backupName;
    protected final String fragmentId;
    protected final String fileName;
    protected final List<String> fileChunks = new ArrayList<>();
    protected final FragmentFolder fragmentFolder;
    protected final Map<String, String> customInformation = new HashMap<>();

    public SystemTestBackupFragment(final String agentId, final String backupName, final String fragmentId, final String fileName,
                                    final List<String> fileChunks) {
        this.agentId = agentId;
        this.backupName = backupName;
        this.fragmentId = fragmentId;
        this.fileName = fileName;
        this.fileChunks.addAll(fileChunks);
        fragmentFolder = new FragmentFolder(
                IntegrationTest.BACKUP_DATA_LOCATION.resolve(BackupManager.DEFAULT_BACKUP_MANAGER_ID).resolve(backupName).resolve(agentId).resolve(fragmentId));
    }

    public SystemTestBackupFragment(final String agentId, final String backupName, final String fragmentId, final String fileName,
                                    final List<String> fileChunks, final String backupManagerId) {
        this.agentId = agentId;
        this.backupName = backupName;
        this.fragmentId = fragmentId;
        this.fileName = fileName;
        this.fileChunks.addAll(fileChunks);
        fragmentFolder = new FragmentFolder(
                IntegrationTest.BACKUP_DATA_LOCATION.resolve(backupManagerId).resolve(backupName).resolve(agentId).resolve(fragmentId));
    }

    public void sendThroughAgent(final SystemTestAgent agent) {
        agent.sendMetadata(agentId, backupName, fragmentId, customInformation);
        agent.sendDataFileName(fileName);
        agent.sendData(fileChunks);
        agent.sendDataChecksum(calculateChecksum(fileChunks));
        waitUntil(() -> fragmentFolder.getDataFileFolder().resolve(fileName).toFile().exists());
    }

    public List<RestoreData> getExpectedRestoreMessages() {
        final List<RestoreData> restoreMessages = new ArrayList<>();
        restoreMessages.add(getFileNameMessage());
        restoreMessages.addAll(getChunkMessages());
        restoreMessages.add(getChecksumMessage());
        return restoreMessages;
    }

    public String getBackedUpMetadata() throws Exception {
        return readFile(fragmentFolder.getMetadataFile());
    }

    public String getBackedUpFileContent() throws Exception {
        return readFile(fragmentFolder.getDataFileFolder().resolve(fileName));
    }

    public Optional<String> getBackedUpCustomMetadataContent() throws Exception {
        return Optional.empty();
    }

    public String getFileName() {
        return fileName;
    }

    public List<String> getFileChunks() {
        return fileChunks;
    }

    public Optional<String> getCustomMetadataFileName() {
        return Optional.empty();
    }

    public List<String> getCustomMetadataChunks() {
        return new ArrayList<>();
    }

    public FragmentFolder getFragmentFolder() {
        return fragmentFolder;
    }

    public void addCustomInformation(final String key, final String value) {
        customInformation.put(key, value);
    }

    protected String readFile(final Path path) throws Exception {
        return new String(Files.readAllBytes(path));
    }

    protected String calculateChecksum(final List<String> chunks) {
        final ChecksumCalculator calculator = new ChecksumCalculator();
        chunks.forEach(chunk -> calculator.addBytes(chunk.getBytes()));
        return calculator.getChecksum();
    }

    protected void waitUntil(final Callable<Boolean> condition) {
        Awaitility.await().atMost(SetTimeouts.TIMEOUT_SECONDS, TimeUnit.SECONDS).until(condition);
    }

    private RestoreData getFileNameMessage() {
        return RestoreData
                .newBuilder()
                .setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setFileName(fileName).build())
                .build();
    }

    private List<RestoreData> getChunkMessages() {
        return fileChunks.stream().map(this::getChunkMessage).collect(Collectors.toList());
    }

    private RestoreData getChunkMessage(final String chunk) {
        return RestoreData
                .newBuilder()
                .setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setContent(ByteString.copyFromUtf8(chunk)).build())
                .build();
    }

    private RestoreData getChecksumMessage() {
        return RestoreData
                .newBuilder()
                .setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setChecksum(calculateChecksum(fileChunks)).build())
                .build();
    }

}
