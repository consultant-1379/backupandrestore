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
import com.ericsson.adp.mgmt.data.CustomMetadataFileChunk;
import com.ericsson.adp.mgmt.data.Metadata;

import java.util.Optional;

/**
 * stores customMetadata chunks
 */
public class BackupCustomMetadataState extends BackupState {

    private final FragmentFolder fragmentFolder;

    private BackupFileWriter customMetadataWriter;

    /**
     * @param fragmentFolder where to store fragment
     * @param job CreateBackupJob responsible for fragment
     * @param metadata fragment metadata
     */
    public BackupCustomMetadataState(final FragmentFolder fragmentFolder, final CreateBackupJob job, final Optional<Metadata> metadata) {
        super(job, metadata);
        this.fragmentFolder = fragmentFolder;
    }

    @Override
    public BackupState processMessage(final BackupData message, final String streamId) {
        if (isCustomMetadataMessage(message)) {
            final CustomMetadataFileChunk chunk = message.getCustomMetadataFileChunk();
            if (isFileName(chunk.getChecksum(), chunk.getContent())) {
                this.customMetadataWriter = new BackupFileWriter(
                        fragmentFolder.getCustomMetadataFileFolder(), chunk.getFileName(), job.getAwsConfig());
                return this;
            }
            if (containsFileChunk(chunk.getChecksum())) {
                customMetadataWriter.addChunk(chunk.getContent().toByteArray());
                return this;
            }
            customMetadataWriter.validateChecksum(chunk.getChecksum());
            customMetadataWriter.writeChecksumFile();
            customMetadataWriter.build();

            return this;
        }
        throw new BackupServiceException("Unexpected message received on data channel (streamId:{}) during backup", streamId);
    }

    @Override
    public void close() {
        if (customMetadataWriter != null) {
            customMetadataWriter.build();
        }
    }

}
