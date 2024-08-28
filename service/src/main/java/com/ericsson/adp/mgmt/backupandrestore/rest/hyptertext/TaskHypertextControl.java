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
package com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext;

/**
 * A class representing an operation's hypertext control
 */
public class TaskHypertextControl {
    private HypertextControl operation;

    /**
     * Default constructor, to be used by Jackson.
     */
    public TaskHypertextControl() {
    }

    /**
     * A class used to represent an operation's hypertext control
     * @param operation the operation's hypertext control
     */
    public TaskHypertextControl(final HypertextControl operation) {
        this.operation = operation;
    }

    public HypertextControl getOperation() {
        return operation;
    }
}
