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
package com.ericsson.adp.mgmt.backupandrestore.grpc.backup;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ericsson.adp.mgmt.data.BackupData;

import io.grpc.stub.StreamObserver;

/**
 * Represents data stream being opened with no ongoing jobs.
 */
public class UnexpectedBackupDataStream implements StreamObserver<BackupData> {

    private static final Logger log = LogManager.getLogger(UnexpectedBackupDataStream.class);

    @Override
    public void onCompleted() {
        log.error("Unexpected backup data stream completed");
    }

    @Override
    public void onError(final Throwable throwable) {
        log.error("Unexpected backup data stream ended due to error", throwable);
    }

    @Override
    public void onNext(final BackupData message) {
        log.error("Unexpected backup data stream received message <{}>", message);
    }

}
