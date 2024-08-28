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
package com.ericsson.adp.mgmt.backupandrestore;

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager.DEFAULT_BACKUP_MANAGER_ID;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_VOLUME_STATS_AVAILABLE_BYTES;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_VOLUME_STATS_CAPACITY_BYTES;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_VOLUME_STATS_USED_BYTES;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.job.ResetConfigJob;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.BackupManagerFileService;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Performs initialization steps for the Orchestrator.
 */
@Service
public class OrchestratorInitializer {
    private static final Logger log = LogManager.getLogger(OrchestratorInitializer.class);
    private static final String PVC_TEMPLATE_NAME = "backup-data";
    private CMMediatorService cmMediatorService;
    private BackupManagerRepository backupManagerRepository;
    private MeterRegistry meterRegistry;
    private BackupManagerFileService backupManagerFileService;
    private S3Config s3Config;
    private long pvcSize;


    /**
     * Performs necessary steps to start up the Orchestrator.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        initMetrics();
        backupManagerRepository.initializeBackupManagers();
        backupManagerRepository.createBackupManager(DEFAULT_BACKUP_MANAGER_ID, false);
        // If not created by file, is created
        cmMediatorService.initCMMediator();
        backupManagerRepository.finishInitialization();
        // Create the DEFAULT BRMs reset vBRM
        backupManagerRepository.createBackupManager(
                BackupManager.DEFAULT_BACKUP_MANAGER_ID,
                BackupManager.DEFAULT_BACKUP_MANAGER_ID + ResetConfigJob.RESET_BRM_SUFFIX,
                new ArrayList<>());
        // If the last action was a reset, and it didn't succeed, rerun it
        backupManagerRepository.getBackupManagers().stream()
                .map(BackupManager::getLastAction)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparing(Action::getStartTime))
                .ifPresent(action -> {
                    if (action.getBackupManagerId().endsWith(ResetConfigJob.RESET_BRM_SUFFIX) &&
                            action.getName().equals(ActionType.RESTORE) &&
                            !action.getResult().equals(ResultType.SUCCESS)) {
                        backupManagerRepository.failResetAction(action);
                    }
                });
        if (!s3Config.isEnabled()) {
            buildPVCMetric();
            backupManagerFileService.createDummyFile();
        }
        log.info("Initialization finished");
    }

    private void initMetrics() {
        MetricsIds.stream()
        .filter(d -> !d.identification().equals("persistentvolumeclaim"))
            .forEach( metric -> metric.register());
    }

    private void buildPVCMetric() {
        // Metric for monitoring total size of PVC capacity
        METRIC_BRO_VOLUME_STATS_CAPACITY_BYTES.unRegister();
        Gauge.builder(METRIC_BRO_VOLUME_STATS_CAPACITY_BYTES.identification(), () -> pvcSize)
                .description(METRIC_BRO_VOLUME_STATS_CAPACITY_BYTES.description())
                .baseUnit("bytes")
                .tag("persistentvolumeclaim", getPVCName())
                .register(meterRegistry);

        // Metric for monitoring total size of files in PVC
        METRIC_BRO_VOLUME_STATS_USED_BYTES.unRegister();
        Gauge.builder(METRIC_BRO_VOLUME_STATS_USED_BYTES.identification()
                , backupManagerFileService, b  -> backupManagerFileService.getSumOfBackupUsedSize())
                .description(METRIC_BRO_VOLUME_STATS_USED_BYTES.description())
                .baseUnit("bytes")
                .tag("persistentvolumeclaim", getPVCName())
                .register(meterRegistry);

        // Metric for monitoring total size of free space in PVC
        METRIC_BRO_VOLUME_STATS_AVAILABLE_BYTES.unRegister();
        Gauge.builder(METRIC_BRO_VOLUME_STATS_AVAILABLE_BYTES.identification()
                , backupManagerFileService, b  -> pvcSize - backupManagerFileService.getSumOfBackupUsedSize())
                .description(METRIC_BRO_VOLUME_STATS_AVAILABLE_BYTES.description())
                .baseUnit("bytes")
                .tag("persistentvolumeclaim", getPVCName())
                .register(meterRegistry);
    }

    /**
     * Get the name of PVC
     * @return the name of PVC
     */
    public static String getPVCName() {
        String hostname = "hostname_placeHolder";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            log.warn("The hostname is unknown");
        }
        return PVC_TEMPLATE_NAME + "-" + hostname;
    }

    /**
     * set the pvc capacity from the configuration
     * @param pvcSize the pvc capacity
     */
    @Value("${bro.pvc.size:15Gi}")
    public void setPvcSize(String pvcSize) {
        pvcSize = pvcSize.replace("i", "B");
        this.pvcSize = DataSize.parse(pvcSize).toBytes();
    }

    @Autowired
    public void setCmMediatorService(final CMMediatorService cmMediatorService) {
        this.cmMediatorService = cmMediatorService;
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

    @Autowired
    public void setMeterRegistry(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Autowired
    public void setBackupManagerFileService(final BackupManagerFileService backupManagerFileService) {
        this.backupManagerFileService = backupManagerFileService;
    }

    @Autowired
    public void setS3Config(final S3Config s3Config) {
        this.s3Config = s3Config;
    }
}
