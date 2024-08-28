/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job.stage;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.job.HousekeepingJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

public class CompletedHousekeepingJobStageTest {

    private CompletedHousekeepingJobStage completedHousekeepingJobStage;
    private NotificationService notificationService;

    @Before
    public void setup() {
        final Action action = createMock(Action.class);
        final HousekeepingJob job = createMock(HousekeepingJob.class);
        expect(job.getAction()).andReturn(action);
        expect(action.getBackupName()).andReturn("backup");
        job.updateBackupManagerHousekeeping();
        EasyMock.expectLastCall().anyTimes();

        notificationService = createMock(NotificationService.class);

        replay(job, notificationService);

        completedHousekeepingJobStage = new CompletedHousekeepingJobStage(new ArrayList<>(), job, notificationService);
    }

    @Test
    public void trigger_completedRestoreJobStage_doesNothing() throws Exception {
        completedHousekeepingJobStage.trigger();
        assertTrue(completedHousekeepingJobStage.isJobFinished());
        assertTrue(completedHousekeepingJobStage.isStageSuccessful());
        assertEquals(3, completedHousekeepingJobStage.getStageOrder());
        assertTrue(completedHousekeepingJobStage.moveToNextStage() instanceof CompletedHousekeepingJobStage);
        verify(notificationService);
    }


    @Test
    public void getProgressPercentage_completedRestoreJobStage_one() throws Exception {
        assertEquals(Double.valueOf(1.0d), Double.valueOf(completedHousekeepingJobStage.getProgressPercentage()));
    }

}
