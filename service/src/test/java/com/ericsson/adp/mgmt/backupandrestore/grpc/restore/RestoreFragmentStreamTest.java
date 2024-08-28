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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.exception.TimedOutDataChannelException;
import com.ericsson.adp.mgmt.data.RestoreData;
import io.grpc.stub.ServerCallStreamObserver;

public class RestoreFragmentStreamTest {

    @Test
    public void onNext_streamDoesNotAcceptMessagesAtFirst_waitsUntilStreamIsReadyToSendMessages() throws Exception {
        final IntermittentServerCallStreamObserverStub stub = new IntermittentServerCallStreamObserverStub();
        final RestoreData message = RestoreData.getDefaultInstance();

        new RestoreFragmentStream(stub, 5).onNext(message);

        assertEquals(message, stub.getMessage());
    }

    @Test(expected = TimedOutDataChannelException.class)
    public void onNext_streamIsNotReadyToAcceptMessages_waitsForTimeoutThenThrowsException() throws Exception {
        new RestoreFragmentStream(new NeverReadyServerCallStreamObserverStub(), 5).onNext(RestoreData.getDefaultInstance());
    }

    @Test
    public void onCompleted_streamIsOpen_closesStream() throws Exception {
        final ServerCallStreamObserverStub stub = new ServerCallStreamObserverStub();

        new RestoreFragmentStream(stub, 5).onCompleted();

        assertTrue(stub.completedConnection());
    }

    @Test
    public void onError_streamIsOpen_closesStreamWithError() throws Exception {
        final ServerCallStreamObserverStub stub = new ServerCallStreamObserverStub();
        final RuntimeException exception = new RuntimeException("Q");

        new RestoreFragmentStream(stub, 5).onError(exception);

        assertEquals(exception, stub.getError());
    }

    private class ServerCallStreamObserverStub extends ServerCallStreamObserver<RestoreData> {

        private RestoreData message;
        private Throwable error;
        private boolean completedConnection;

        @Override
        public void onCompleted() {
            this.completedConnection = true;
        }

        @Override
        public void onError(final Throwable error) {
            this.error = error;
        }

        @Override
        public void onNext(final RestoreData message) {
            this.message = message;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public void setCompression(final String compression) {

        }

        @Override
        public void setOnCancelHandler(final Runnable handler) {

        }

        @Override
        public void disableAutoInboundFlowControl() {

        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void request(final int arg0) {

        }

        @Override
        public void setMessageCompression(final boolean messageCompression) {

        }

        @Override
        public void setOnReadyHandler(final Runnable handler) {

        }

        public RestoreData getMessage() {
            return message;
        }

        public Throwable getError() {
            return error;
        }

        public boolean completedConnection() {
            return completedConnection;
        }

    }

    private class IntermittentServerCallStreamObserverStub extends ServerCallStreamObserverStub {

        private boolean isReady;

        @Override
        public boolean isReady() {
            isReady = !isReady;
            return isReady;
        }

    }

    private class NeverReadyServerCallStreamObserverStub extends ServerCallStreamObserverStub {

        @Override
        public boolean isReady() {
            return false;
        }

    }
}
