/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.adp.mgmt.backupandrestore.rest.scheduler;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class PeriodicEventRequestOrResponseTest {

        @Test
        public void isPeriodZero () {
                PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
                assertTrue(request.isPeriodZero());

                request.setHours(0);
                request.setMinutes(0);
                request.setDays(0);
                request.setWeeks(0);
                assertTrue(request.isPeriodZero());

                request.setHours(1);
                assertFalse(request.isPeriodZero());
        }

        @Test
        public void isEmptyBody () {
                PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
                assertTrue(request.isEmptyBody());
        }

}
