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
package com.ericsson.adp.mgmt.backupandrestore.grpc.restore;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.ericsson.adp.mgmt.backupandrestore.exception.TimedOutDataChannelException;
import com.ericsson.adp.mgmt.data.RestoreData;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

/**
 * Wraps stream of restore data, adding flow control and timeout.
 */
public class RestoreFragmentStream implements StreamObserver<RestoreData> {

    private final ServerCallStreamObserver<RestoreData> restoreStream;
    private final int timeToWait;

    /**
     * Creates stream based on existing stream.
     * @param restoreStream restore data stream.
     * @param timeToWait time in seconds to wait.
     */
    public RestoreFragmentStream(final ServerCallStreamObserver<RestoreData> restoreStream, final int timeToWait) {
        this.restoreStream = restoreStream;
        this.timeToWait = timeToWait;
    }

    @Override
    public void onNext(final RestoreData message) {
        waitForStreamToBeReady();
        this.restoreStream.onNext(message);
    }

    @Override
    public void onCompleted() {
        this.restoreStream.onCompleted();
    }

    @Override
    public void onError(final Throwable throwable) {
        this.restoreStream.onError(throwable);
    }

    private void waitForStreamToBeReady() {
        final Instant startWaitingInstant = Instant.now();
        while (!this.restoreStream.isReady()) {
            if (ChronoUnit.SECONDS.between(startWaitingInstant, Instant.now()) > this.timeToWait) {
                throw new TimedOutDataChannelException(this.timeToWait);
            }
        }
    }

}
