/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2019
 * <p>
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.backupandrestore;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.find;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.BackupManagerFileService;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupFileService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

public class OrchestratorInitializerTest {

    @Test
    public void initialize_applicationStartup_loadsBackupManagersThenNotifiesCMThenCreatesDefaultBackupManager() {
        final CMMediatorService cmMediatorService = createMock(CMMediatorService.class);
        final BackupManagerRepository backupManagerRepository = createMock(BackupManagerRepository.class);
        final BackupManagerFileService backupManagerFileService = createMock(BackupManagerFileService.class);
        final S3Config s3Config = createMock(S3Config.class);

        backupManagerRepository.initializeBackupManagers();
        expectLastCall();
        expect(backupManagerRepository.getBackupManagers()).andReturn(Collections.emptyList()).anyTimes();
        backupManagerRepository.createBackupManager("DEFAULT", false);
        expectLastCall();
        backupManagerRepository.createBackupManager("DEFAULT", "DEFAULT-bro", new ArrayList<>());
        expectLastCall();

        cmMediatorService.initCMMediator();
        expectLastCall();

        backupManagerRepository.finishInitialization();
        expectLastCall();

        backupManagerFileService.createDummyFile();
        expectLastCall();
        
        expect(s3Config.isEnabled()).andReturn(false);
        replay(cmMediatorService, backupManagerRepository, s3Config, backupManagerFileService);

        final OrchestratorInitializer orchestratorInitializer = new OrchestratorInitializer();
        orchestratorInitializer.setCmMediatorService(cmMediatorService);
        orchestratorInitializer.setBackupManagerRepository(backupManagerRepository);
        orchestratorInitializer.setMeterRegistry(createMeterRegistry());
        orchestratorInitializer.setS3Config(s3Config);
        orchestratorInitializer.setBackupManagerFileService(backupManagerFileService);

        orchestratorInitializer.initialize();
        verify(cmMediatorService, backupManagerRepository);
    }

    private MeterRegistry createMeterRegistry() {
        final CollectorRegistry prometheusRegistry = new CollectorRegistry(true);
        final MockClock clock = new MockClock();
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, prometheusRegistry, clock);
    }
}
