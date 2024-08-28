/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.backupandrestore.util;

import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidIdException;

/**
 * Responsible for common id validation.
 */
@Service
public class IdValidator {

    /**
     * Validates an id.
     * @param idToBeValidated to be validated.
     */
    public void validateId(final String idToBeValidated) {
        if (!isValidId(idToBeValidated)) {
            throw new InvalidIdException("<" + idToBeValidated + "> is an invalid id");
        }
    }

    /**
     * Validates an id.
     * @param idToBeValidated to be validated.
     * @return isValid
     */
    public boolean isValidId(final String idToBeValidated) {
        return !(idToBeValidated == null || idToBeValidated.isBlank() || containsInvalidCharacters(idToBeValidated)
                || "..".equals(idToBeValidated) || ".".equals(idToBeValidated));
    }

    private boolean containsInvalidCharacters(final String idToBeValidated) {
        return idToBeValidated.contains("/") || idToBeValidated.contains("\\") || idToBeValidated.contains("~");
    }

}
