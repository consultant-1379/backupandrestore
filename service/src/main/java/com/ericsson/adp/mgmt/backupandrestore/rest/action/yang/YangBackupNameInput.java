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
package com.ericsson.adp.mgmt.backupandrestore.rest.action.yang;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Input with backup name.
 */
public class YangBackupNameInput {

    @JsonProperty("ericsson-brm:name")
    private String name;

    /**
     * Empty constructor, to be used by Jackson.
     */
    public YangBackupNameInput() {}

    /**
     * Creates input.
     * @param name backup name.
     */
    public YangBackupNameInput(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "YangBackupNameInput [name=" + name + "]";
    }

}
