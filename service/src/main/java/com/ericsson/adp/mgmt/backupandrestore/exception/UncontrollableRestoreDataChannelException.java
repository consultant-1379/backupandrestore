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
package com.ericsson.adp.mgmt.backupandrestore.exception;

/**
 * To be used when restore data channel can't be controlled.
 */
public class UncontrollableRestoreDataChannelException extends RuntimeException {

    private static final long serialVersionUID = -722694481867162927L;

    /**
     * Creates exception.
     */
    public UncontrollableRestoreDataChannelException() {
        super("Unable to control restore data channel");
    }

}
