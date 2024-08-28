/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/

package com.ericsson.adp.mgmt.backupandrestore.rest;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assume.assumeFalse;

import org.easymock.EasyMock;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.rest.health.HealthResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.health.ProbeFileManager;
import com.ericsson.adp.mgmt.backupandrestore.util.OSUtils;

public class ProbeFileManagerTest {

    @Test
    public void testWritesWhenEnabled() {
        assumeFalse("Skipping the test on Windows OS.", OSUtils.isWindows());
        final HealthControllerService healthControllerService = createMock(HealthControllerService.class);
        final HealthResponse healthResponse = new HealthResponse();
        expect(healthControllerService.getHealth()).andReturn(healthResponse).anyTimes();
        replay(healthControllerService);
        final ProbeFileManager manager = new ProbeFileManager();
        manager.setEnabled("required");
        manager.setHealthController(healthControllerService);
        manager.setHealthStatusFolder(".");
        manager.writeProbeFile();

        verify(healthControllerService);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void testWhenHealthControllerNotSet() {
        // Calls writeProbeFile when the ProbeFileManager hasn't been initialised, and asserts this does not
        // throw an exception
        assumeFalse("Skipping the test on Windows OS.", OSUtils.isWindows());
        final ProbeFileManager manager = new ProbeFileManager();
        manager.setEnabled("required");
        manager.setHealthStatusFolder(".");
        manager.setHealthController(null);
        manager.writeProbeFile();
    }
}
