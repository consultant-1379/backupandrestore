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
package com.ericsson.adp.mgmt.backupandrestore.job;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.exception.AgentsNotAvailableException;

/**
 * AgentNotRegisteredJob used to trigger an exception if there are no registered agents
 */
public class AgentNotRegisteredJob extends Job {

    private static final Logger log = LogManager.getLogger(AgentNotRegisteredJob.class);

    @Override
    protected void triggerJob() {
        log.error("Tried to trigger job, which has no registered agents");
        throw new AgentsNotAvailableException("Failing job for not having any registered agents");
    }

    @Override
    protected boolean didFinish() {
        return true;
    }

    @Override
    protected void completeJob() {
        //do nothing
    }

    @Override
    protected void fail() {
        log.error("Failing job for not having any registered agents");
    }

}
