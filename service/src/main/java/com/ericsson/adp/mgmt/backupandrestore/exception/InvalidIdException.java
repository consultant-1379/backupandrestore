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
 * To be used when trying to create an entity with an invalid id.
 */
public class InvalidIdException extends UnprocessableEntityException {

    private static final long serialVersionUID = -2027605818702547561L;

    /**
     * Creates exception.
     * @param message what happened.
     */
    public InvalidIdException(final String message) {
        super(message);
    }

}
