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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.CustomMetadataFileChunk;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.metadata.Fragment;

public class BackupStateTest {

    private BackupStateStub state;

    @Before
    public void setUp() {
        state = new BackupStateStub();
    }

    @Test
    public void complete_haveMetadata_closesStateAndTellsJobThatFragmentWasCompletedSuccessfullyAndMovesToCompleteState() {
        final Metadata metadata = Metadata.newBuilder().setAgentId("A").setFragment(Fragment.newBuilder().setFragmentId("X").build()).build();

        final CreateBackupJob job = createMock(CreateBackupJob.class);
        job.fragmentSucceeded("A", "X");
        expectLastCall();
        replay(job);

        state = new BackupStateStub(job, metadata);

        final BackupState nextState = state.complete();

        assertTrue(state.isClosed());
        assertTrue(nextState instanceof BackupCompleteState);

        verify(job);
    }

    @Test
    public void complete_doesNotHaveMetadata_fails() {
        final BackupState nextState = state.complete();

        assertTrue(state.isClosed());
        assertTrue(nextState instanceof BackupFailedState);
    }

    @Test
    public void fail_haveMetadataOfFragment_tellCreateBackupJobThatFragmentFailed() {
        final Metadata metadata = Metadata.newBuilder().setAgentId("A").setFragment(Fragment.newBuilder().setFragmentId("X").build()).build();

        final CreateBackupJob job = createMock(CreateBackupJob.class);
        job.fragmentFailed("A", "X");
        expectLastCall();
        replay(job);

        state = new BackupStateStub(job, metadata);

        final BackupState nextState = state.fail();

        assertTrue(state.isClosed());
        assertTrue(nextState instanceof BackupFailedState);

        verify(job);
    }

    @Test
    public void fail_doNotHaveMetadataOfFragment_doNotTellCreateBackupJobThatFragmentFailed() {
        final CreateBackupJob job = createMock(CreateBackupJob.class);
        replay(job);

        state = new BackupStateStub(job);

        final BackupState nextState = state.fail();

        assertTrue(nextState instanceof BackupFailedState);

        verify(job);
    }

    @Test
    public void complete_closeThrowsException_failsFragment() {
        final Metadata metadata = Metadata.newBuilder().setAgentId("A").setFragment(Fragment.newBuilder().setFragmentId("X").build()).build();

        final CreateBackupJob job = createMock(CreateBackupJob.class);
        job.fragmentFailed("A", "X");
        expectLastCall();
        replay(job);

        state = new BackupStateExceptionStub(job, metadata);

        final BackupState nextState = state.complete();

        assertTrue(nextState instanceof BackupFailedState);

        verify(job);
    }

    @Test
    public void isMetadataMessage_nonEmptyMetadataMessage_returnsTrue() {
        final BackupData message = BackupData.newBuilder().setDataMessageType(DataMessageType.METADATA).setMetadata(Metadata.newBuilder().build()).build();
        assertTrue(state.isMetadataMessage(message));
        assertFalse(state.isBackupFileMessage(message));
    }

    @Test
    public void isMetadataMessage_emptyMetadataMessage_returnsFalse() {
        final BackupData message = BackupData.newBuilder().setDataMessageType(DataMessageType.METADATA).build();
        assertFalse(state.isMetadataMessage(message));
    }

    @Test
    public void isBackupFileMessage_nonEmptyBackupFileMessage_validateProperState() {
        final BackupData message = BackupData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE).setBackupFileChunk(BackupFileChunk.newBuilder().build()).build();
        assertTrue(state.isBackupFileMessage(message));
        assertFalse(state.isMetadataMessage(message));
        assertFalse(state.isCustomMetadataMessage(message));
    }

    @Test
    public void isBackupFileMessage_emptyMetadataMessage_returnsFalse() {
        final BackupData message = BackupData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE).build();
        assertFalse(state.isBackupFileMessage(message));
    }

    @Test
    public void isCustomMetadataMessage_nonEmptyCustomMetadataMessage_returnsTrue() {
        final BackupData message = BackupData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE).setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().build()).build();
        assertTrue(state.isCustomMetadataMessage(message));
    }

    @Test
    public void isCustomMetadataMessage_emptyMetadataMessage_returnsFalse() {
        final BackupData message = BackupData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE).build();
        assertFalse(state.isCustomMetadataMessage(message));
    }

    private class BackupStateStub extends BackupState {

        private boolean closed;

        public BackupStateStub() {
            super(null);
        }

        public BackupStateStub(final CreateBackupJob job) {
            super(job);
        }

        public BackupStateStub(final CreateBackupJob job, final Metadata metadata) {
            super(job, Optional.of(metadata));
        }

        @Override
        public BackupState processMessage(final BackupData message , final String streamId) {
            return null;
        }

        @Override
        public void close() {
            this.closed = true;
        }

        public boolean isClosed() {
            return closed;
        }

    }

    private class BackupStateExceptionStub extends BackupStateStub {

        public BackupStateExceptionStub(final CreateBackupJob job, final Metadata metadata) {
            super(job, metadata);
        }

        @Override
        public void close() {
            throw new RuntimeException("AAA");
        }

    }
}
