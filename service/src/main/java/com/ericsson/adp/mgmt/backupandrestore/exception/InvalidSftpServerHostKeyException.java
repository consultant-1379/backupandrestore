/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
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
 * To be used when parsing persisted SFTP Server Host keys.
 */
public class InvalidSftpServerHostKeyException extends RuntimeException {

    private static final long serialVersionUID = 7867225968821622619L;

    /**
     * Creates exception.
     * @param message the exception message.
     */
    public InvalidSftpServerHostKeyException(final String message) {
        super(message);
    }

}
