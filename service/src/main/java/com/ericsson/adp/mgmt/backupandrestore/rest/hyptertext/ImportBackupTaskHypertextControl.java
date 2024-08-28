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
 * A class representing a IMPORT task hypertext control
 */
public class ImportBackupTaskHypertextControl extends TaskHypertextControl{

    /**
     * Default constructor, to be used by Jackson.
     */
    public ImportBackupTaskHypertextControl() {
    }

    /**
     * Constructor used to build a IMPORT task hypertext control
     * @param operation the IMPORT operation
     */
    public ImportBackupTaskHypertextControl(final HypertextControl operation) {
        super(operation);
    }

    @Override
    @JsonProperty("import")
    public HypertextControl getOperation() {
        return super.getOperation();
    }
}
