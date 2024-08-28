/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.brotestagent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ericsson.adp.mgmt.bro.api.agent.Agent;
import com.ericsson.adp.mgmt.brotestagent.agent.TestAgentFactory;
import com.ericsson.adp.mgmt.brotestagent.util.PropertiesHelper;

/**
 * Starts the Backup and Restore Agent.
 */
public class BackupAndRestoreAgent {

    private static final Logger log = LogManager.getLogger(BackupAndRestoreAgent.class);

    private static ExecutorService executorService;
    private static Agent agent;

    /**
     * Prevents external instantiation.
     */
    private BackupAndRestoreAgent() {
    }

    /**
     * Starts the Backup and Restore Agent.
     *
     * @param args
     *            optional, first argument should be the location of the properties file.
     */
    public static void main(final String[] args) {
        loadProperties(args);
        keepApplicationAlive();

        agent = TestAgentFactory.createTestAgent();
    }

    public static Agent getAgent() {
        return agent;
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Kills agent.
     */
    public static void killAgent() {
        log.info("Terminating agent");
        executorService.shutdownNow();
        executorService = null;
        agent = null;
    }

    private static void keepApplicationAlive() {
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> log.info("Keeping application alive"));
    }

    private static void loadProperties(final String[] args) {
        if (args.length == 0) {
            log.warn("No properties file received, using defaults");
            PropertiesHelper.clear();
        } else {
            // If it includes more than one properties file
            for (final String file : args) {
                PropertiesHelper.loadProperties(file);
            }
        }
    }

}
