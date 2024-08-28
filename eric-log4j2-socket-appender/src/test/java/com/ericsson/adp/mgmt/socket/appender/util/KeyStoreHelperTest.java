/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.socket.appender.util;

import org.apache.logging.log4j.core.net.ssl.StoreConfigurationException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;


public class KeyStoreHelperTest {
        private Path certLocation = Path.of("src/test/resources/cmmclientcert.pem");
        private Path keyLocation = Path.of("src/test/resources/clientprivkey.key");
        private Path caCertLocation = Path.of("src/test/resources/ca.pem");
        private Path keyStoreDir = Path.of("src/test/resources/test.p12");


    @Test
    public void populateKeystore_testForSSLConfig() throws NoSuchAlgorithmException, StoreConfigurationException, CertificateException, KeyStoreException, KeyException, IOException {
        Assert.assertNotNull(KeyStoreHelper.populateKeystore(new StreamSslConfiguration(keyLocation, certLocation, caCertLocation, keyStoreDir, "siptls", "LT")));
    }

    @Test(expected = NullPointerException.class)
    public void populateKeyStore_testForNullSSLConfig() throws NoSuchAlgorithmException, StoreConfigurationException, CertificateException, KeyStoreException, KeyException, IOException {
        Assert.assertNull(KeyStoreHelper.populateKeystore(new StreamSslConfiguration(null, null, null, null, "siptls", "LT")));
    }
}
