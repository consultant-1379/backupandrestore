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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ericsson.adp.mgmt.bro.api.exception.TimedOutDataChannelException;
import com.ericsson.adp.mgmt.data.BackupData;
import com.google.protobuf.Empty;

import io.grpc.ClientCall;

public class BackupDataStreamTest {

    @Test
    public void onNext_streamDoesNotAcceptMessagesAtFirst_waitsUntilStreamIsReadyToSendMessages() throws Exception {
        final IntermittentClientCallStub clientCallStub = new IntermittentClientCallStub();
        final BackupData message = BackupData.getDefaultInstance();

        new BackupDataStream(clientCallStub).onNext(message);

        assertTrue(clientCallStub.startedConnection());
        assertEquals(message, clientCallStub.getMessage());
    }

    @Test(expected = TimedOutDataChannelException.class)
    public void onNext_streamIsNotReadyToAcceptMessages_waitsForThirtySecondsThenThrowsException() throws Exception {
        new BackupDataStream(new NeverReadyClientCallStub()).onNext(BackupData.getDefaultInstance());
    }

    @Test
    public void onCompleted_streamIsOpen_closesStream() throws Exception {
        final ClientCallStub clientCallStub = new ClientCallStub();

        new BackupDataStream(clientCallStub).onCompleted();

        assertTrue(clientCallStub.startedConnection());
        assertTrue(clientCallStub.completedConnection());
    }

    @Test
    public void onError_streamIsOpen_closesStreamWithError() throws Exception {
        final ClientCallStub clientCallStub = new ClientCallStub();
        final RuntimeException exception = new RuntimeException("Q");

        new BackupDataStream(clientCallStub).onError(exception);

        assertTrue(clientCallStub.startedConnection());
        assertEquals(exception, clientCallStub.getError());
    }

    private class ClientCallStub extends ClientCall<BackupData, Empty> {

        private BackupData message;
        private Throwable error;
        private boolean startedConnection;
        private boolean completedConnection;

        @Override
        public void start(final Listener<Empty> arg0, final io.grpc.Metadata arg1) {

            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
                arg0.onMessage(Empty.getDefaultInstance());
            }).start();

            startedConnection = true;
        }

        @Override
        public void sendMessage(final BackupData arg0) {
            message = arg0;
        }

        @Override
        public void halfClose() {
            completedConnection = true;
        }

        @Override
        public void cancel(final String arg0, final Throwable arg1) {
            error = arg1;
        }

        @Override
        public void request(final int arg0) {
            assertEquals(1, arg0);
        }

        public BackupData getMessage() {
            return message;
        }

        public boolean startedConnection() {
            return startedConnection;
        }

        public boolean completedConnection() {
            return completedConnection;
        }

        public Throwable getError() {
            return error;
        }

    }

    private class IntermittentClientCallStub extends ClientCallStub {

        private boolean isReady;

        @Override
        public boolean isReady() {
            isReady = !isReady;
            return isReady;
        }

    }

    private class NeverReadyClientCallStub extends ClientCallStub {

        @Override
        public boolean isReady() {
            return false;
        }

    }
}
