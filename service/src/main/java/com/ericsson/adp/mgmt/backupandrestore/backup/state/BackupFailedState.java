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

import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.data.BackupData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Enters a failed state upon encountering any data channel errors.
 */
public class BackupFailedState extends BackupState {

    private static final Logger log = LogManager.getLogger(BackupFailedState.class);

    /**
     * Create a FailedState.
     * @param job Job to receive backup information from.
     */
    public BackupFailedState(final CreateBackupJob job) {
        super(job);
    }

    @Override
    public BackupState processMessage(final BackupData message, final String streamId) {
        log.info("Ignoring message {} because data channel (streamId:{}) is in failed state", message, streamId);
        return this;
    }

    @Override
    public void close() {
        //Not needed
    }

    @Override
    public BackupState complete() {
        log.error("Data channel closed with error because it is in a failed state");
        return this;
    }

}
