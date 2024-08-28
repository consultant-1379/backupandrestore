package com.ericsson.adp.mgmt.backupandrestore.backup.state;

import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.data.BackupData;
import org.easymock.EasyMock;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BackupFailedStateTest {

    @Test
    public void processMessage_failedStateGotMessage_logsMessageAndContinuesInFailedState() throws Exception {
        BackupFailedState failedState = new BackupFailedState(null);

        BackupState nextState = failedState.processMessage(BackupData.getDefaultInstance(), "dummyStreamId");

        assertEquals(failedState, nextState);
    }

    @Test
    public void complete_failedState_doesNotUpdateJob() throws Exception {
        CreateBackupJob job = EasyMock.createMock(CreateBackupJob.class);
        EasyMock.replay(job);

        BackupFailedState failedState = new BackupFailedState(job);

        failedState.complete();

        EasyMock.verify(job);
    }

}
