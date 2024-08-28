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
package com.ericsson.adp.mgmt.backupandrestore.exception;

/**
 * To be used when timing out waiting for a delete backup job to be ready.
 */
public class TimedOutHousekeepingException extends RuntimeException {

    private static final long serialVersionUID = 7142872562241774742L;

    /**
     * Creates exception.
     * @param wait in seconds
     * @param backupName waiting to be deleted
     */
    public TimedOutHousekeepingException(final int wait, final String backupName) {
        super("Timeout exception waits " + wait + " seconds to delete the backup " + backupName);
    }

}
