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

import com.ericsson.adp.mgmt.bro.api.util.CertificateType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Used to monitor a set of files and call a set of provided callbacks when any are modified
 * */
public class CertWatcher {
    private static final Logger log = LogManager.getLogger(CertWatcher.class);
    private final Map<Path, Optional<Instant>> lastReadTimes = new ConcurrentHashMap<>();
    private final List<Supplier<Optional<Throwable>>> callbacks = new CopyOnWriteArrayList<>();
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private final long period;
    private final TimeUnit unit;

    private ScheduledFuture<?> watchFuture;
    private final OrchestratorConnectionInformation orchestratorConnectionInformation;

    private final Map<String, CertificateType> originalCertificatePaths;

    /**
     * Constructs a cert watcher
     * @param orchestratorConnectionInformation - set of paths to watch
     * @param period - the period to poll the paths provided to check for changes
     * @param unit - the unit of time which, when paired with period, defines the polling interval
     * */
    public CertWatcher(final OrchestratorConnectionInformation orchestratorConnectionInformation, final long period, final TimeUnit unit) {
        orchestratorConnectionInformation.getValidPaths().stream().map(Path::of).collect(Collectors.toList()).forEach(this::initMap);
        this.orchestratorConnectionInformation = orchestratorConnectionInformation;
        this.originalCertificatePaths = orchestratorConnectionInformation.getOriginalCertificatePaths();
        executor.setRemoveOnCancelPolicy(true);
        this.period = period;
        this.unit = unit;
    }

    /**
     * Start the asynchronous monitoring loop. May not be called twice in a row without an intermediate call to stop
     * */
    public synchronized void start() {
        assert watchFuture == null;
        watchFuture = executor.scheduleAtFixedRate(() -> {
            try {
                for (final String path : originalCertificatePaths.keySet()) {
                    final Optional<String> selectedPath = CertificateHandler.checkCertsInPathAndTakePreference(orchestratorConnectionInformation,
                            path, originalCertificatePaths.get(path));

                    if (selectedPath.isPresent() && !lastReadTimes.containsKey(Path.of(selectedPath.get()))) {
                        // create a new entry for .key or .crt path
                        log.info("Adding file to watch {}", selectedPath.get());
                        lastReadTimes.put(Path.of(selectedPath.get()), Optional.of(Instant.now()));
                    }
                }

                if (!this.isUpToDate()) {
                    callbacks.stream().map(Supplier::get).filter(Optional::isPresent)
                            .forEach(t -> log.warn("Cert change callback produced: " + t));
                    this.updateLastRead();
                }
            } catch (Exception e) {
                log.info(e);
            }
        }, 0, period, unit);


    }

    /**
     * Used to stop the asynchronous monitoring loop. May not be called without a prior call to start
     * */
    public synchronized void stop() {
        assert watchFuture != null;
        watchFuture.cancel(true);
        watchFuture = null;
    }

    /**
     * Add a callback to the set of callbacks to be called when a monitored file is modified.
     *
     * @param callback - a callback that may optionally return any exception thrown during the call of the
     *                 callback - this throwable is logged as a warning, but does not interfere with future
     *                 iterations of the monitoring loop
     * */
    public void addCallback(final Supplier<Optional<Throwable>> callback) {
        this.callbacks.add(callback);
    }

    /**
     * Remove the passed callback from the set of callbacks called when a monitored file is modified
     *
     * @param callback - the callback to remove
     * @return a boolean indicating whether the passed callback was present in the callback list
     * */
    public boolean removeCallback(final Supplier<Optional<Throwable>> callback) {
        return callbacks.remove(callback);
    }

    private void initMap(final Path toWatch) {
        try {
            lastReadTimes.put(toWatch, Optional.empty());
        } catch (Exception e) {
            log.debug("Failed to initialise read time map store for file {}", toWatch, e);
        }
    }

    /**
     * Update the watchers last read time
     * */
    public void updateLastRead() {
        final Instant now = Instant.now();
        log.debug("Updating certificate last read time to {}", now);
        lastReadTimes.keySet().forEach(k -> lastReadTimes.put(k, Optional.of(now)));
    }

    /**
     * Returns true if the certificates defined in the channel information were last modified before
     * the last call to updateLastRead()
     * @return true if certificates are up to date, false if channel refresh is required
     * */
    public boolean isUpToDate() {
        return lastReadTimes.entrySet()
                .stream()
                .filter(e -> e.getValue().isPresent())
                .noneMatch(e -> {
                    try {
                        final Instant lastModified = Files.getLastModifiedTime(e.getKey()).toInstant();
                        log.debug("File {} last modified at time {}, last read {}", e.getKey(), lastModified, e.getValue().get());
                        return lastModified.isAfter(e.getValue().get());
                    } catch (IOException ioException) {
                        return false;
                    }
                });
    }

    public OrchestratorConnectionInformation getOrchestratorConnectionInformation() {
        return orchestratorConnectionInformation;
    }

}
