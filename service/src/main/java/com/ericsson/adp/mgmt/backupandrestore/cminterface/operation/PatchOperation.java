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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.operation;

/**
 * Represents operations in a CM Patch.
 */
public enum PatchOperation {

    ADD("add"), REPLACE("replace"), REMOVE("remove");

    private final String representation;
    /**
     * Patch operations.
     * @param string how CM expects it.
     */
    PatchOperation(final String string) {
        this.representation = string;
    }

    public String getStringRepresentation() {
        return representation;
    }
}
