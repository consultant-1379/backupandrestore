/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.agent.exception;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.exception.NotFoundException;

/**
 * Represents failing to find a specific resource.
 */
public class ResourceNotFoundException  extends NotFoundException {

    private static final long serialVersionUID = 1875830028748301971L;

    /**
     * Creates exception.
     * @param resource failed to be found
     * @param actionId id of the action the resource related to
     */
    public ResourceNotFoundException(final ActionType resource, final String actionId) {
        super(resource.toString() + " resource <" + actionId + "> not found");
    }

    /**
     * Creates exception.
     * @param resource failed to be found
     */
    public ResourceNotFoundException(final ActionType resource) {
        super(resource.toString() + " resource not found");
    }

    /**
     * Creates exception.
     */
    public ResourceNotFoundException() {
        super("No resource found");
    }

}
