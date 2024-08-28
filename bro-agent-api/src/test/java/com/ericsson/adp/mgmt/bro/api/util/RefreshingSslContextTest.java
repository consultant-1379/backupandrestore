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
package com.ericsson.adp.mgmt.bro.api.util;

import com.ericsson.adp.mgmt.bro.api.agent.CertWatcher;
import com.ericsson.adp.mgmt.bro.api.agent.OrchestratorConnectionInformation;
import com.google.common.io.Files;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RefreshingSslContextTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void run_test() throws IOException, InterruptedException {
        final File f = folder.newFile("test");
        final AtomicBoolean wasCalled1 = new AtomicBoolean(false); // Tracking variable for no-op callback
        // A no-op callback to test basic cert watcher behaviour
        final Supplier<Optional<Throwable>> testCallback = () -> {
            wasCalled1.set(true);
            return Optional.empty();
        };

        OrchestratorConnectionInformation information = new OrchestratorConnectionInformation(
                "host",
                22,
                "fakeCa",
                "src/test/resources/certPath_with_DR-D1123-133_format/ca/CertWatcherFakeCa.pem",
                "src/test/resources/certPath_with_DR-D1123-133_format/certs/CertWatcherFakeCert.pem",
                "src/test/resources/certPath_with_DR-D1123-133_format/certs/CertWatcherFakeKey.pem"
        );

        final CertWatcher certWatcher = new CertWatcher(information, 10, TimeUnit.MILLISECONDS);
        certWatcher.addCallback(testCallback); // Add the test callback to the callback list
        certWatcher.updateLastRead(); // Init the watcher
        certWatcher.start(); // Start the watcher

        assertFalse(wasCalled1.get()); // Assert starting the watcher doesn't cause a callback call
        assertTrue(touch("src/test/resources/certPath_with_DR-D1123-133_format/ca/ca.crt"));
        assertTrue(touch("src/test/resources/certPath_with_DR-D1123-133_format/certs/tls.crt"));
        assertTrue(touch("src/test/resources/certPath_with_DR-D1123-133_format/certs/tls.key"));

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(wasCalled1::get); // wait for callback calls
        certWatcher.stop(); // Stop the watcher, since it'll be started by the SSL context in the next phase

        // Now actually testing the SSL context
        wasCalled1.set(false); // Reset the no-op callback tracking variable
        final AtomicBoolean wasCalled2 = new AtomicBoolean(false);
        // Construct a basic context provider - we don't actually care about the SSL part here, just the underlying
        //rebuild logic
        final Supplier<SslContextBuilder> builderSupplier = () -> {
            wasCalled2.set(true);
            return SslContextBuilder.forClient();
        };
        // Construct the actual context
        final RefreshingSslContext context = new RefreshingSslContext(builderSupplier, certWatcher);
        assertTrue(wasCalled2.get()); // Assert that the builder supplier is called directly by context on construction
        assertFalse(wasCalled1.get()); // Assert the watcher hasn't erroneously called it's callback set due to being started
        wasCalled2.set(false); // Reset the builder supplier tracking flag
        assertTrue(touch("src/test/resources/certPath_with_DR-D1123-133_format/ca/ca.crt"));
        assertTrue(touch("src/test/resources/certPath_with_DR-D1123-133_format/certs/tls.crt"));
        assertTrue(touch("src/test/resources/certPath_with_DR-D1123-133_format/certs/tls.key"));

        // Wait for both callbacks to be called
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> wasCalled1.get() && wasCalled2.get());

        // Check that remove callback works as expected
        assertTrue(context.getWatcher().removeCallback(testCallback)); // no-op callback is in list, remove and return true
        assertFalse(context.getWatcher().removeCallback(testCallback)); // no-po callback isn't in list, return false
    }

    private static boolean touch(final String path) {
        File f = new File(path);
        try {
            // To make sure the difference between consecutive calls to touch is significant
            await().atMost(250, TimeUnit.MILLISECONDS).until(() -> false);
        } catch (Exception e) {
            // We do nothing here because we expect to wait the full 250 ms
        }
        try {
            Files.touch(f);
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Failed to touch filed: " + path);
            return false;
        }
        return f.setLastModified(System.currentTimeMillis());
    }
}
