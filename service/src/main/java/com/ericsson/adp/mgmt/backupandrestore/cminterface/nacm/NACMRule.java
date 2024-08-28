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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.nacm;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a rule belonging to a NACMRole.
 */
public class NACMRule {

    private String moduleName;
    private String action;
    private String name;
    private String tailfAcmContext;
    private String accessOperations;

    @JsonProperty("module-name")
    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(final String moduleName) {
        this.moduleName = moduleName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @JsonProperty("tailf-acm:context")
    public String getTailfAcmContext() {
        return tailfAcmContext;
    }

    public void setTailfAcmContext(final String tailfAcmContext) {
        this.tailfAcmContext = tailfAcmContext;
    }

    @JsonProperty("access-operations")
    public String getAccessOperations() {
        return accessOperations;
    }

    public void setAccessOperations(final String accessOperations) {
        this.accessOperations = accessOperations;
    }

}
