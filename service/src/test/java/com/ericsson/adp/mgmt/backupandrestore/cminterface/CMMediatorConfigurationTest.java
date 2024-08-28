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
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CMMediatorConfigurationTest {

    @Test
    public void getCMUrl_baseUrlAndApiSuffix_buildsCMUrl() throws Exception {
        final CMMediatorConfiguration configuration = new CMMediatorConfiguration();
        configuration.setTlsEnabled(false);
        configuration.setUrl("http://localhost:5003/cm");
        configuration.setUrlTLS("https://localhost:5004/cm");
        configuration.setApiSuffix("b");

        assertEquals("http://localhost:5003/cm/b", configuration.getCMUrl());
    }

    @Test
    public void getCMUrl_baseUrlAndApiSuffix_buildsTLSUrl() throws Exception {
        final CMMediatorConfiguration configuration = new CMMediatorConfiguration();
        configuration.setTlsEnabled(true);
        configuration.setUrl("http://localhost:5003/cm");
        configuration.setUrlTLS("https://localhost:5004/cm");
        configuration.setApiSuffix("b");

        assertEquals("https://localhost:5004/cm/b", configuration.getCMUrl());
    }

}
