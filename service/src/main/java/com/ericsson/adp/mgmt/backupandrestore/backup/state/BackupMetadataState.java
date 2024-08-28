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

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.backup.BackupMetadataWriter;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.metadata.Fragment;

/**
 * Waits for metadata message
 */
public class BackupMetadataState extends BackupState {

    private static final Logger log = LogManager.getLogger(BackupMetadataState.class);

    private final BackupMetadataWriter backupMetadataWriter;
    private final IdValidator idValidator;

    /**
     * Creates state.
     * @param backupMetadataWriter used to write metadata.
     * @param job ongoing job.
     * @param idValidator to validate fragmentId.
     */
    public BackupMetadataState(final BackupMetadataWriter backupMetadataWriter, final CreateBackupJob job, final IdValidator idValidator) {
        super(job);
        this.backupMetadataWriter = backupMetadataWriter;
        this.idValidator = idValidator;
    }

    @Override
    public BackupState processMessage(final BackupData message, final String streamId) {
        if (isMetadataMessage(message)) {
            log.info("Received message {} from backup data channel (streamId:{})", message, streamId);
            validateMetadataMessage(message.getMetadata());

            this.metadata = Optional.ofNullable(message.getMetadata());
            final FragmentFolder fragmentFolder = job.getFragmentFolder(message.getMetadata());
            job.receiveNewFragment(message.getMetadata().getAgentId(), message.getMetadata().getFragment().getFragmentId());

            backupMetadataWriter.storeFragment(fragmentFolder, message.getMetadata());

            return new BackupFileDataState(fragmentFolder, job, this.metadata);
        }
        throw new BackupServiceException("Unexpected message received during backup", message.toString());
    }

    @Override
    public void close() {
        //Nothing to cleanup
    }

    private void validateMetadataMessage(final Metadata metadata) {
        if (metadata.getAgentId().isEmpty() || metadata.getBackupName().isEmpty() || hasInvalidFragmentInfo(metadata.getFragment())) {
            throw new BackupServiceException("Invalid Backup Metadata <" + metadata + ">");
        }
    }

    private boolean hasInvalidFragmentInfo(final Fragment fragment) {
        return hasInvalidFragmentId(fragment) || fragment.getSizeInBytes().isEmpty() || fragment.getVersion().isEmpty();
    }

    private boolean hasInvalidFragmentId(final Fragment fragment) {
        try {
            idValidator.validateId(fragment.getFragmentId());
            return false;
        } catch (final Exception e) {
            return true;
        }
    }

}
