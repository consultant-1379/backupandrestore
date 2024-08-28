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
package com.ericsson.adp.mgmt.backupandrestore.exception;

/**
 * BackupLimitExceededException represents exception if backup requested and its pre-defined backup limit exceeds.
 */
public class BackupLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 798213834312031208L;

    /**
     * Creates BackupLimitExceededException exception.
     *
     * @param limit
     *            limit.
     */
    public BackupLimitExceededException(final Integer limit) {
        super("Failed to create/import backup as maximum limit of <" + limit + "> already exceeded");
    }
}
