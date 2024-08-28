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
package com.ericsson.adp.mgmt.bro.api.grpc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.bro.api.exception.TimedOutDataChannelException;
import com.ericsson.adp.mgmt.data.BackupData;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.Empty;

import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import static com.ericsson.adp.mgmt.bro.api.grpc.GRPCConfig.AGENT_DATA_CHANNEL_TIMEOUT_SECS;

/**
 * Wraps stream of backup data, adding flow control and timeout.
 */
public class BackupDataStream implements StreamObserver<BackupData> {

    private static final Logger log = LogManager.getLogger(BackupDataStream.class);
    private static final int SECONDS_TO_WAIT_FOR_RESPONSE_FROM_BRO = 2;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final ClientCall<BackupData, Empty> backupStream;

    /**
     * Creates stream based on existing stream.
     * @param backupStream backup data stream.
     */
    public BackupDataStream(final ClientCall<BackupData, Empty> backupStream) {
        this.backupStream = backupStream;
        startBackupStream();
    }

    @Override
    public void onNext(final BackupData message) {
        waitForStreamToBeReady();
        backupStream.sendMessage(message);
    }

    @Override
    public void onCompleted() {
        backupStream.request(1);
        backupStream.halfClose();
        Uninterruptibles.awaitUninterruptibly(latch, SECONDS_TO_WAIT_FOR_RESPONSE_FROM_BRO, TimeUnit.SECONDS);
    }

    @Override
    public void onError(final Throwable throwable) {
        backupStream.cancel(throwable.getMessage(), throwable);
    }

    private void startBackupStream() {
        backupStream.start(new ClientCall.Listener<Empty>() {

            @Override
            public void onHeaders(final Metadata headers) {
                super.onHeaders(headers);
            }

            @Override
            public void onMessage(final Empty message) {
                super.onMessage(message);
                log.debug("Received message from data channel");
                latch.countDown();
            }

            @Override
            public void onClose(final Status status, final Metadata trailers) {
                log.debug("Data channel connection closed with status <{}>", status);
                latch.countDown();
            }
        }, new io.grpc.Metadata());
    }

    private void waitForStreamToBeReady() {
        final Instant startWaitingInstant = Instant.now();
        while (!backupStream.isReady()) {
            if (ChronoUnit.SECONDS.between(startWaitingInstant, Instant.now()) > AGENT_DATA_CHANNEL_TIMEOUT_SECS.getValue()) {
                throw new TimedOutDataChannelException(AGENT_DATA_CHANNEL_TIMEOUT_SECS.getValue());
            }
        }
    }

}
