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

import com.ericsson.adp.mgmt.data.Metadata;

/**
 * Attempting to start a data channel for agent/backup that doesn't match ongoing backup/restore.
 */
public class UnauthorizedDataChannelException extends RuntimeException {

    private static final long serialVersionUID = 3697047913533599693L;

    /**
     * Creates exception.
     *
     * @param backupName
     *            - backupname of current backup.
     * @param metadata
     *            from channel.
     */
    public UnauthorizedDataChannelException(final String backupName, final Metadata metadata) {
        super("Incoming data channel for <" + metadata + "> not allowed for backup <" + backupName + ">");
    }

}
