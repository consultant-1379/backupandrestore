/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.ssl;

import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;
import com.ericsson.adp.mgmt.backupandrestore.util.OSUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.TLSCertUtils;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.stream.Collectors;

import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.BRO;
import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.KAFKA;
import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.PM;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;
import static org.mockito.ArgumentMatchers.anyString;


public class KeyStoreServiceTest extends SystemTest {

    @Autowired
    private KeyStoreService keyStoreService;

    @Test
    public void generateKeyStores_assertPathsOfNonEmptyConfigsExistTest() {
        assumeFalse("Skipping the test on Windows OS. Reference ADPPRG-39077.", OSUtils.isWindows());
        keyStoreService.generateKeyStores();
        keyStoreService.getKeyStoreConfigs().entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .forEach(e -> assertTrue(e.getValue().getKeyStoreFile().toFile().exists()));
    }

    @Test
    public void generateKeyStores_assertCertsAndAuthoritiesOfEmptyConfigsAreNull() {
        assumeFalse("Skipping the test on Windows OS. Reference ADPPRG-39077.", OSUtils.isWindows());
        keyStoreService.generateKeyStores();
        keyStoreService.getKeyStoreConfigs().entrySet().stream()
                .filter(e -> e.getValue().isEmpty())
                .forEach(e -> assertTrue(
                        e.getValue().getAuthorities().isEmpty()
                                && e.getValue().getCert() == null));
    }

    @Test
    public void generateKeyStores_assertKeysInConfigMapMatchKeystoreAliases() {
        assumeFalse("Skipping the test on Windows OS. Reference ADPPRG-39077.", OSUtils.isWindows());
        keyStoreService.generateKeyStores();
        keyStoreService.getKeyStoreConfigs().forEach((key, value) -> assertEquals(key, value.getAlias()));
    }

    @Test
    public void regenerateKeyStoreForAlias_KeyStoreExists() {
        assumeFalse("Skipping the test on Windows OS. Reference ADPPRG-39077.", OSUtils.isWindows());
        keyStoreService.regenerateKeyStoreForAlias(PM);
        assertTrue(keyStoreService.getKeyStoreConfig(PM).getKeyStoreFile().toFile().exists());
    }

    @Test
    public void regenerateKeyStoreForAlias_KeyStoreConfigIsNull_do_Nothing() {
        assumeFalse("Skipping the test on Windows OS. Reference ADPPRG-39077.", OSUtils.isWindows());
        assertTrue(keyStoreService.getKeyStoreConfig(KAFKA).isEmpty());
        keyStoreService.regenerateKeyStoreForAlias(KAFKA);
        assertTrue(keyStoreService.getKeyStoreConfig(KAFKA).isEmpty());
    }

    //Commented out due to intermittent failure in PCR - 2020-12-14
    /*@Test
    public void generateKeyStores_assertAllNonEmptyKeyStoreConfigsHavingAMatchingKeyStore() {
        assumeFalse("Skipping the test on Windows OS. Reference ADPPRG-39077.", OSUtils.isWindows());
        keyStoreService.generateKeyStores();
        keyStoreService.getKeyStoreConfigs().entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .forEach(e -> assertNotNull(keyStoreService.getKeyStore(e.getKey())));
    }*/

    @Test
    public void assertAtLeastOneNotNullKeyStoreConfig() {
        var nonEmpty = keyStoreService.getKeyStoreConfigs().entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toList());
        assertFalse(nonEmpty.isEmpty());
    }

    @Test
    public void assertPmConfigHasNoCAButIsNotEmpty() {
        assertFalse(keyStoreService.getKeyStoreConfig(KeyStoreAlias.PM).isEmpty());
        assertEquals(1, keyStoreService.getKeyStoreConfig(KeyStoreAlias.PM).getAuthorities().size());
    }


    @Test
    public void generateKeyStoreForAlias_expiredCert() {
        assumeFalse("Skipping the test on Windows OS. Reference ADPPRG-39077.", OSUtils.isWindows());

        try (MockedStatic<TLSCertUtils> mockedStatic = Mockito.mockStatic(TLSCertUtils.class)) {
            mockedStatic.when(() -> TLSCertUtils.checkCertsInPathAndTakePreference("src/test/resources/broca.pem", "ca")).thenReturn("ca.crt");
            mockedStatic.when(() -> TLSCertUtils.checkCertsInPathAndTakePreference("src/test/resources/server1.pem", "cert")).thenReturn("tls.crt");
            mockedStatic.when(() -> TLSCertUtils.checkCertsInPathAndTakePreference("src/test/resources/server1.key", "priv")).thenReturn("tls.key");
            mockedStatic.when(() -> TLSCertUtils.isCertificateExpired(anyString())).thenReturn(true);
            keyStoreService.regenerateKeyStoreForAlias(BRO);
            assertEquals(1, keyStoreService.getExpiredCertList().size());
        }
    }
}
