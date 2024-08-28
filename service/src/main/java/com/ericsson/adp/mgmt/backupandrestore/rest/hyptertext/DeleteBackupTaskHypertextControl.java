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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A class representing a DELETE BACKUP task hypertext control
 */
public class DeleteBackupTaskHypertextControl extends TaskHypertextControl{

    /**
     * Default constructor, to be used by Jackson.
     */
    public DeleteBackupTaskHypertextControl() {
    }

    /**
     * Constructor used to build a DELETE task hypertext control
     * @param operation the DELETE operation
     */
    public DeleteBackupTaskHypertextControl(final HypertextControl operation) {
        super(operation);
    }

    @Override
    @JsonProperty("delete")
    public HypertextControl getOperation() {
        return super.getOperation();
    }
}
