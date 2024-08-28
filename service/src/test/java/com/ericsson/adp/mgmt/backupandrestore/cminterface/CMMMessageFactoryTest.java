/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.SchedulerPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.UpdateSchedulerPatch;

public class CMMMessageFactoryTest {
    private CMMMessageFactory cmmMessageFactory;
    private SchedulerPatchFactory schedulerPatchFactory;

    @Before
    public void setUp() throws Exception {
        cmmMessageFactory= new CMMMessageFactory();
        schedulerPatchFactory = createMock(SchedulerPatchFactory.class);
        cmmMessageFactory.setSchedulerPatchFactory(schedulerPatchFactory);
    }

    @Test
    public void getMessageToUpdateScheduler_message_created() {
        final Scheduler scheduler = createMock(Scheduler.class);
        final UpdateSchedulerPatch patch = createMock(UpdateSchedulerPatch.class);
        expect(schedulerPatchFactory.getPatchToUpdateScheduler(scheduler)).andReturn(patch).anyTimes();
        replay (schedulerPatchFactory, scheduler, patch);
        CMMMessage message = cmmMessageFactory.getMessageToUpdateScheduler(scheduler, (m, e) -> Optional.empty(), CMMClient.RETRY_INDEFINITELY);
        assertTrue(message.getResource().equals("configurations/ericsson-brm"));
        assertTrue(message.getConfigurationPatch() instanceof UpdateSchedulerPatch);
    }

}
