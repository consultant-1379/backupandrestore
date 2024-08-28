/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Fire and forget monitor for {@link Job}.
 */
public class JobMonitor {
    private final ScheduledExecutorService executorService;
    /**
     * @param job - the job to monitor
     * @param intervalInSeconds - how often to call job.monitor()
     */
    protected JobMonitor(final Job job, final int intervalInSeconds) {
        executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(job::monitor, intervalInSeconds, intervalInSeconds, TimeUnit.SECONDS);
    }

    /**
     * Stops the {@link JobMonitor}
     */
    public void stop() {
        executorService.shutdown();
    }
}
