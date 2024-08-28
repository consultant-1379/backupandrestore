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
package com.ericsson.adp.mgmt.backupandrestore.ssl;

import com.ericsson.adp.mgmt.backupandrestore.exception.KeyStoreGenerationException;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class OSMNKeyStoreConfigurationTest extends SystemTest {

    // NOTE: Using LT values as it doens't make a difference for purpose of tests
    @Value("${security.logtransformer.keystore.path:}")
    private String keyStorePath="";
    @Value("${security.logtransformer.keystore.password:}")
    private String password="";
    @Value("${security.logtransformer.client.key.path:}")
    private String privateKeyPath="";
    @Value("${security.logtransformer.client.cert.path:}")
    private String certPath="";
    @Value("${security.siptls.root.ca.path:}")
    private String rootCaPath="";

    @Test
    public void tryToBuild_invalidKeyPath_buildsWithJustCA() {
        final KeyStoreConfiguration config = new OSMNKeyStoreConfiguration(keyStorePath, password, "", certPath, rootCaPath);
        assertFalse(config.isValidConfiguration());
        assertEquals(1, config.getAuthorities().size());
    }

    @Test
    public void tryToBuild_invalidCertPath_buildsWithJustCA() {
        final KeyStoreConfiguration config = new OSMNKeyStoreConfiguration(keyStorePath, password, privateKeyPath, "", rootCaPath);
        assertFalse(config.isValidConfiguration());
        assertEquals(1, config.getAuthorities().size());
    }

    @Test(expected = KeyStoreGenerationException.class)
    public void tryToBuild_invalidCAPath_throwsKeystoreGenerationException() {
        new OSMNKeyStoreConfiguration(keyStorePath, password, privateKeyPath, certPath, "");
    }
}
