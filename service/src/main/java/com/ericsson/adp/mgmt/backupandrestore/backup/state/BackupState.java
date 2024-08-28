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

import com.ericsson.adp.mgmt.backupandrestore.exception.DataChannelException;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.Metadata;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Process message from agent
 */
public abstract class BackupState {

    private static final Logger log = LogManager.getLogger(BackupState.class);

    protected CreateBackupJob job;
    protected Optional<Metadata> metadata = Optional.empty();

    /**
     * Create a BackupState without metadata
     * @param job CreateBackupJob responsible for fragment
     */
    public BackupState(final CreateBackupJob job) {
        this.job = job;
    }

    /**
     * Create a BackupState with optional metadata
     * @param job CreateBackupJob responsible for fragment
     * @param metadata fragment metadata
     */
    public BackupState(final CreateBackupJob job, final Optional<Metadata> metadata) {
        this(job);
        this.metadata = metadata;
    }

    /**
     * Process BackupDataChannel messages
     *
     * @param message
     *            sent by agent through BackupDataChannel
     * @param streamId
     *            id of the BackupDataChannel
     * @return Backup state
     * @throws IOException
     *             Thrown if the custom or data directory for a specific backup is failed to be created
     */
    public abstract BackupState processMessage(final BackupData message, final String streamId) throws IOException;

    /**
     * Finish backup
     */
    public abstract void close();

    /**
     * Update the job with fragment's success and closes any lingering opened files.
     * @return BackupState next state.
     */
    public BackupState complete() {
        try {
            close();
            this.updateJobWithFragmentSuccess(this.metadata
                    .orElseThrow(() -> new DataChannelException("Trying to complete a fragment without metadata")));
            return new BackupCompleteState(job, metadata);
        } catch (final Exception e) {
            log.error("Failed to successfully complete data channel", e);
            return fail();
        }
    }

    /**
     * Update the Job with fragment's failure, closes any lingering opened files.
     * @return BackupState next state.
     */
    public BackupState fail() {
        try {
            close();
        } catch (final Exception e) {
            log.error("Exception failing data channel", e);
        } finally {
            this.metadata.ifPresent(this::updateJobWithFragmentFailure);
        }
        return new BackupFailedState(job);
    }

    /**
     * @param message
     *            sent from agent
     * @return true if the message is of type metadata
     */
    protected boolean isMetadataMessage(final BackupData message) {
        return DataMessageType.METADATA.equals(message.getDataMessageType()) && message.hasMetadata();
    }

    /**
     * @param message
     *            sent from agent
     * @return true if the message is of type backupFileChunk
     */
    protected boolean isBackupFileMessage(final BackupData message) {
        return DataMessageType.BACKUP_FILE.equals(message.getDataMessageType()) && message.hasBackupFileChunk();
    }

    /**
     * @param message
     *            sent from agent
     * @return true if the message is of type customMetadataChunk
     */
    protected boolean isCustomMetadataMessage(final BackupData message) {
        return DataMessageType.CUSTOM_METADATA_FILE.equals(message.getDataMessageType()) && message.hasCustomMetadataFileChunk();
    }

    /**
     *
     * @param checksum
     *            the value of the checksum field from the message
     * @return true if the message contains a file chunk
     */
    protected boolean containsFileChunk(final String checksum) {
        return checksum == null || checksum.isEmpty();
    }

    /**
     *
     * @param checksum
     *            the value of the checksum field from the message
     * @param content
     *            the value of the content field from the message
     * @return true if the message contains a file name
     */
    protected boolean isFileName(final String checksum, final ByteString content) {
        return (checksum == null || checksum.isEmpty()) && (content == null || content.isEmpty());
    }

    private void updateJobWithFragmentFailure(final Metadata metadata) {
        this.job.fragmentFailed(metadata.getAgentId(), metadata.getFragment().getFragmentId());
    }

    private void updateJobWithFragmentSuccess(final Metadata metadata) {
        this.job.fragmentSucceeded(metadata.getAgentId(), metadata.getFragment().getFragmentId());
    }

}
