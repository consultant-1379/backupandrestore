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
package com.ericsson.adp.mgmt.backupandrestore.action;

/**
 * States in which an action can find itself.
 */
public enum ActionStateType {

    RUNNING("running"), FINISHED("finished");

    private final String cmRepresentation;

    /**
     * Creates creationType.
     * @param cmRepresentation how CM expects it.
     */
    ActionStateType(final String cmRepresentation) {
        this.cmRepresentation = cmRepresentation;
    }

    public String getCmRepresentation() {
        return cmRepresentation;
    }

}
