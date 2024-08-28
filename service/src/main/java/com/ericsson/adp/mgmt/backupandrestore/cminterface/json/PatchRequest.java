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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents json to submit a patch to a CM Configuration.
 */
public class PatchRequest {

    private List<PatchOperationJson> operations = new ArrayList<>();
    private String baseETag = "";

    /**
     * Default constructor, to be used by Jackson.
     */
    public PatchRequest() {}

    /**
     * Creates Patch Request.
     * @param operations to apply.
     */
    public PatchRequest(final List<PatchOperationJson> operations) {
        this (operations, "");
    }

    /**
     * Creates Patch Request.
     * @param operations to apply.
     * @param baseEtag eTag to be sent to patch
     */
    public PatchRequest(final List<PatchOperationJson> operations, final String baseEtag) {
        this.operations = operations;
        this.baseETag = baseEtag;
    }

    @JsonProperty("patch")
    public List<PatchOperationJson> getOperations() {
        return operations;
    }

    public void setOperations(final List<PatchOperationJson> operations) {
        this.operations = operations;
    }

    public String getBaseETag() {
        return baseETag;
    }

    public void setBaseETag(final String baseETag) {
        this.baseETag = baseETag;
    }

    @Override
    public String toString() {
        return "PatchRequest{" +
                "operations=" + operations.toString() +
                ", baseETag='" + baseETag + '\'' +
                '}';
    }
}
