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
package com.ericsson.adp.mgmt.backupandrestore.job;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ericsson.adp.mgmt.backupandrestore.exception.NotImplementedException;

/**
 * Job that was not implemented.
 */
public class NotImplementedJob extends Job {

    private static final Logger log = LogManager.getLogger(NotImplementedJob.class);

    @Override
    protected void triggerJob() {
        log.error("Tried to execute not implemented action");
        throw new NotImplementedException();
    }

    @Override
    protected boolean didFinish() {
        return true;
    }

    @Override
    protected void completeJob() {
        log.error("Completing not implemented job.");
    }

    @Override
    protected void fail() {
        log.error("Failing not implemented job");
    }

}
