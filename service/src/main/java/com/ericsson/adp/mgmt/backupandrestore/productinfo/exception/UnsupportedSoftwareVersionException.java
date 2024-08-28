/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.productinfo.exception;

/**
 * Trying to restore backup with incompatible Software Version
 */
public class UnsupportedSoftwareVersionException extends RuntimeException {

    private static final long serialVersionUID = 7050184302305121374L;

    /**
     * Creates Exception
     */
    public UnsupportedSoftwareVersionException() {
        super("Trying to restore backup with incompatible Software Version");
    }
}
