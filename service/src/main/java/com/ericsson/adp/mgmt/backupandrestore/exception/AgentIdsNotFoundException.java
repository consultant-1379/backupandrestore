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

import java.nio.file.Path;

/**
 * Represents an error where the Agent Ids could not be found for a backup.
 */
public class AgentIdsNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -5219686387723884392L;

    /**
     * Creates a new AgentIdsNotFoundException
     *
     * @param backupPath
     *            The path to the backup whose agent Ids cannot be found
     * @param cause
     *            The underlying exception
     */
    public AgentIdsNotFoundException(final Path backupPath, final Throwable cause) {
        super("An exception occurred while looking for agents under backup folder <" + backupPath + ">", cause);
    }

}
