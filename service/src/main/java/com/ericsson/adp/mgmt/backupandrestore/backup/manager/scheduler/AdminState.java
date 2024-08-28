/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler;

/**
 * Represents admin state of scheduler.
 */
public enum AdminState {
    LOCKED("locked"), UNLOCKED("unlocked");

    private final String modelRepresentation;

    /**
     * Creates AdminState.
     * @param modelRepresentation EOI model representation.
     */
    AdminState(final String modelRepresentation) {
        this.modelRepresentation = modelRepresentation;
    }

    public String getCmRepresentation() {
        return modelRepresentation;
    }
}
