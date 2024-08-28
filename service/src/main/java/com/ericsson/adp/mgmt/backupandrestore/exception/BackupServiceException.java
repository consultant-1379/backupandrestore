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
 * Exception for the BackupService.
 */
public class BackupServiceException extends RuntimeException {

    private static final long serialVersionUID = 7066582548805906256L;

    private final String logMessage;

    /**
     * Constructor accepts exception message
     * @param message exception message
     */
    public BackupServiceException(final String message) {
        this(message, message);
    }

    /**
     * Constructor accepts exception message and logMessage
     * @param message exception message
     * @param logMessage message to be logged
     */
    public BackupServiceException(final String message, final String logMessage) {
        super(message);
        this.logMessage = logMessage;
    }

    /**
     * Constructor accepts exception message and Throwable cause
     * @param message exception message
     * @param cause Throwable cause
     */
    public BackupServiceException(final String message, final Throwable cause) {
        super(message, cause);
        this.logMessage = message;
    }

    public String getLogMessage() {
        return logMessage;
    }

}
