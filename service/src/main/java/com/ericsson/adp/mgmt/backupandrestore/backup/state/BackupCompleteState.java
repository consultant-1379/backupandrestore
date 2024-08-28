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
package com.ericsson.adp.mgmt.backupandrestore.backup.state;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.Metadata;

/**
 * Represents data channel that was closed successfully.
 */
public class BackupCompleteState extends BackupState {

    private static final Logger log = LogManager.getLogger(BackupCompleteState.class);

    /**
     * Create a BackupCompleteState.
     * @param job CreateBackupJob responsible for fragment.
     * @param metadata fragment metadata.
     */
    public BackupCompleteState(final CreateBackupJob job, final Optional<Metadata> metadata) {
        super(job, metadata);
    }

    @Override
    public BackupState processMessage(final BackupData message, final String streamId) throws IOException {
        log.info("Ignoring message {} because data channel (streamId:{}) is closed", message, streamId);
        return this;
    }

    @Override
    public void close() {
        log.info("Data channel already closed");
    }

}
