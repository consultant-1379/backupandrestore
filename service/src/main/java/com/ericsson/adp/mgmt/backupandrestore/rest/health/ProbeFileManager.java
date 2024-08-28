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

package com.ericsson.adp.mgmt.backupandrestore.rest.health;

import com.ericsson.adp.mgmt.backupandrestore.rest.HealthControllerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Class to handle periodically writing the BRO health status out to a temp file. To be used for
 * a kubernetes probe. NOTE: reading the file does not provide an accurate picture of the health
 * status of the service at time of reading, only an accurate picture of what the status was at
 * time of writing
 * */
@Service
public class ProbeFileManager {
    private static final Logger log = LogManager.getLogger(ProbeFileManager.class);
    private static final String REST_ACTIONS_TLS_REQUIRED = "required";

    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    private HealthControllerService healthController;
    private boolean enabled;
    private String healthStatusFolder;

    /**
     * Starts up the scheduled task to write probe files
     *
     * This is a post construct because otherwise we get null pointer exceptions trying to access
     * the health controller or other objects provided by Spring through annotated setters
     * */
    @PostConstruct
    public void init() {
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        executor.scheduleAtFixedRate(this::writeProbeFile, 0, 5, TimeUnit.SECONDS);
        log.info("Liveness probe file writing scheduled");
    }

    /**
    * Scheduled function which creates or overwrites the previous health status written to the k8s probe file
    * */
    public void writeProbeFile() {
        final String healthStatus = getHealthStatus();
        writeToProbeFile(healthStatus);
    }

    private String getHealthStatus() {
        try {
            final HealthResponse status = healthController.getHealth();
            return mapper.writeValueAsString(status);
        } catch (Exception e) {
            log.warn("Caught exception getting health: ", e);
            return "{\"status\" : \"error obtaining the health status\"}";
        }
    }

    /**
     * This writes the status to temp files, then swaps it into the LIVE/READY_PROBE_FILE_LOCATION,
     * replacing the old file there if it exists
     *
     * @param status - the current status of BRO
     * */
    private void writeToProbeFile(final String status) {
        if (enabled) {
            final Path liveProbeFileLocation = Paths.get(healthStatusFolder, "broLiveHealth.json");
            final Path readyProbeFileLocation = Paths.get(healthStatusFolder, "broReadyHealth.json");
            final Path tempLiveProbeLocation = Path.of(liveProbeFileLocation.toString() + ".new");
            final Path tempReadyProbeLocation = Path.of(readyProbeFileLocation.toString() + ".new");
            try (OutputStreamWriter fileOutput = new OutputStreamWriter(new FileOutputStream(tempLiveProbeLocation.toFile()))) {
                fileOutput.write(status);
                Files.move(tempLiveProbeLocation, liveProbeFileLocation, ATOMIC_MOVE, REPLACE_EXISTING);
            } catch (Exception e) {
                log.warn("Writing to liveness probe file failed", e);
            }
            try (OutputStreamWriter fileOutput = new OutputStreamWriter(new FileOutputStream(tempReadyProbeLocation.toFile()))) {
                fileOutput.write(status);
                Files.move(tempReadyProbeLocation, readyProbeFileLocation, ATOMIC_MOVE, REPLACE_EXISTING);
            } catch (Exception e) {
                log.warn("Writing to readiness probe file failed", e);
            }
        }
    }

    @Autowired
    public void setHealthController(final HealthControllerService healthController) {
        this.healthController = healthController;
    }

    @Value("${restActions.tlsRequired:optional}")
    public void setEnabled(final String enabled) {
        this.enabled = REST_ACTIONS_TLS_REQUIRED.equals(enabled);
    }

    @Value("${healthStatus.folder}")
    public void setHealthStatusFolder(final String healthStatusFolder) {
        this.healthStatusFolder = healthStatusFolder;
    }
}
