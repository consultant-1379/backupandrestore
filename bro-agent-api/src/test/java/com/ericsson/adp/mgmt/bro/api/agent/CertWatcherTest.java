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

package com.ericsson.adp.mgmt.bro.api.agent;

import com.google.common.io.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.util.concurrent.TimeUnit;


import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CertWatcherTest {
    private OrchestratorConnectionInformation information;
    private OrchestratorConnectionInformation tlsInformation;

    @Before
    public void createFilesAndChannelInformation(){
        information = new OrchestratorConnectionInformation(
                "host",
                22,
                "fakeCa",
                "src/test/resources/certPath_without_format/ca/CertWatcherFakeCa.pem",
                "src/test/resources/certPath_without_format/certs/CertWatcherFakeCert.pem",
                "src/test/resources/certPath_without_format/certs/CertWatcherFakeKey.pem"
        );

        assertTrue(touch(information.getCertificateAuthorityPath()));
        assertTrue(touch(information.getClientCertificatePath()));
        assertTrue(touch(information.getClientPrivKeyPath()));

        tlsInformation = new OrchestratorConnectionInformation(
                "host",
                22,
                "fakeCa",
                "src/test/resources/certPath_without_format/ca/CertWatcherFakeCa.pem"
        );
    }

//    @Test
//    public void testTlsInformation() {
//        final CertWatcher watcher = new CertWatcher(
//                tlsInformation,
//                5, TimeUnit.SECONDS
//        );
//        assertTrue(tlsInformation.isTlsEnabled());
//        assertFalse(tlsInformation.isMTlsEnabled());
//        watcher.start();
//        watcher.updateLastRead();
//        assertTrue(watcher.isUpToDate());
//        assertTrue(touch(tlsInformation.getCertificateAuthorityPath()));
//        assertFalse(watcher.isUpToDate());
//        watcher.updateLastRead();
//        assertTrue(watcher.isUpToDate());
//    }
//
//    @Test
//    public void filesMarkedRead_UpdateNotRequired() {
//        final CertWatcher watcher = new CertWatcher(
//                information,
//                5, TimeUnit.SECONDS
//        );
//        watcher.updateLastRead();
//        assertTrue(watcher.isUpToDate());
//    }
//
//    @Test
//    public void filesMarkedRead_FilesModified_UpdateRequired() {
//        final CertWatcher watcher = new CertWatcher(
//                information,
//                5, TimeUnit.SECONDS
//        );
//        watcher.start();
//        watcher.updateLastRead();
//        assertTrue(watcher.isUpToDate());
//        assertTrue(touch(information.getClientCertificatePath()));
//        assertFalse(watcher.isUpToDate());
//        watcher.updateLastRead();
//        assertTrue(watcher.isUpToDate());
//    }
//
//    @Test
//    public void sslDisabled_updatedNotRequired() {
//        final CertWatcher watcher = new CertWatcher(new OrchestratorConnectionInformation(
//                "host",
//                22
//        ), 5, TimeUnit.SECONDS);
//        assertTrue(watcher.isUpToDate());
//        watcher.updateLastRead(); // to test this doesn't throw
//        assertTrue(watcher.isUpToDate());
//    }

    @After
    public void deleteFilesAndChannelInformation() {
        (new File(information.getCertificateAuthorityPath())).delete();
        (new File(information.getClientCertificatePath())).delete();
        (new File(information.getClientPrivKeyPath())).delete();
    }

    private static boolean touch(final String path) {
        try {
            // To make sure the difference between consecutive calls to touch is significant
            await().atMost(250, TimeUnit.MILLISECONDS).until(() -> false);
        } catch (Exception e) {
            // We do nothing here because we expect to wait the full 100 ms
        }
        try {
            Files.touch(new File(path));
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Failed to touch filed: " + path);
            return false;
        }
        try {
            // To make sure the difference between consecutive calls to touch is significant
            await().atMost(250, TimeUnit.MILLISECONDS).until(() -> false);
        } catch (Exception e) {
            // We do nothing here because we expect to wait the full 100 ms
        }
        return true;
    }
}