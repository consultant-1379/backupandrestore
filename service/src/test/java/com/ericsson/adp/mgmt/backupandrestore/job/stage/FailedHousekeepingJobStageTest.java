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
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.job.HousekeepingJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

public class FailedHousekeepingJobStageTest {

    private FailedHousekeepingJobStage failedHousekeepingJobStage;
    private NotificationService notificationService;

    @Before
    public void setup() {
        final Action action = createMock(Action.class);
        final HousekeepingJob job = createMock(HousekeepingJob.class);
        expect(job.getAction()).andReturn(action);
        expect(job.getBackupManagerId()).andReturn("101");

        notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();

        replay(job, notificationService);

        failedHousekeepingJobStage = new FailedHousekeepingJobStage (new ArrayList(), job, notificationService);
    }

    @Test
    public void trigger_failedHousekeepingJobStage_validateStage() throws Exception {
        failedHousekeepingJobStage.trigger();
        assertTrue(failedHousekeepingJobStage.isStageSuccessful());
        assertTrue(failedHousekeepingJobStage.isJobFinished());
        assertEquals(failedHousekeepingJobStage, failedHousekeepingJobStage.moveToFailedStage());
        assertEquals(failedHousekeepingJobStage, failedHousekeepingJobStage.moveToNextStage());
    }

}
