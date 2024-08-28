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
package com.ericsson.adp.mgmt.backupandrestore.backup.state;

import com.ericsson.adp.mgmt.backupandrestore.backup.BackupFileWriter;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.Metadata;

import java.util.Optional;

/**
 * Stores BackupFile chunks
 */
public class BackupFileDataState extends BackupState {

    private final FragmentFolder fragmentFolder;
    private BackupFileWriter backupFileWriter;

    /**
     * @param fragmentFolder where to store fragment
     * @param job CreateBackupJob responsible for fragment
     * @param metadata fragment metadata
     */
    public BackupFileDataState(final FragmentFolder fragmentFolder, final CreateBackupJob job, final Optional<Metadata> metadata) {
        super(job, metadata);
        this.fragmentFolder = fragmentFolder;
    }

    @Override
    public BackupState processMessage(final BackupData message, final String streamId) {
        if (isBackupFileMessage(message)) {
            final BackupFileChunk chunk = message.getBackupFileChunk();
            if (isFileName(chunk.getChecksum(), chunk.getContent())) {
                this.backupFileWriter = new BackupFileWriter(
                        fragmentFolder.getDataFileFolder(), chunk.getFileName(), job.getAwsConfig(), getFragmentSize().orElse(0L));
                return this;
            }
            if (containsFileChunk(chunk.getChecksum())) {
                backupFileWriter.addChunk(chunk.getContent().toByteArray());
                updateTransferredBytes(chunk.getContent().size());
                return this;
            }
            backupFileWriter.validateChecksum(chunk.getChecksum());
            backupFileWriter.writeChecksumFile();
            backupFileWriter.build();

            return new BackupCustomMetadataState(fragmentFolder, job, metadata);
        }
        throw new BackupServiceException("Unexpected message received on data channel (streamId:{}) during backup", streamId);
    }

    private Optional<Long> getFragmentSize() {
        try {
            return metadata.map(value -> Long.valueOf(value.getFragment().getSizeInBytes()));
        } catch (NumberFormatException | NullPointerException e) {
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        if (backupFileWriter != null) {
            backupFileWriter.build();
        }
    }

    private void updateTransferredBytes(final int chunkLength) {
        metadata.ifPresent(value -> {
            job.updateAgentChunkSize(value.getAgentId(), chunkLength);
        });
    }

}
