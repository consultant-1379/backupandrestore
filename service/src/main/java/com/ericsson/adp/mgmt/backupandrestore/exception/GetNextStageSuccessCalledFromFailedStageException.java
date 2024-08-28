/*
 *  ******************************************************************************
 *  COPYRIGHT Ericsson 2020
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 *  *******************************************************************************
 *
 */

package com.ericsson.adp.mgmt.backupandrestore.exception;

/**
 * Exception that occurs when an attempt is made to call
 */
public class GetNextStageSuccessCalledFromFailedStageException extends RuntimeException {

    private static final long serialVersionUID = 244695882261949343L;

    /**
     * Creates exception.
     *
     * @param nameOfFailedStage - failed stage name.
     */
    public GetNextStageSuccessCalledFromFailedStageException(final String nameOfFailedStage) {
        super("Attempt to call getNextStageSuccess on a " + nameOfFailedStage);
    }
}
