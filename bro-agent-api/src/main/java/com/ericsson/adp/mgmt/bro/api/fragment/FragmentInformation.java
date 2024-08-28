/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.bro.api.fragment;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds information for a fragment
 */
public class FragmentInformation {

    private String fragmentId;
    private String version;
    private String sizeInBytes;
    private Map<String, String> customInformation = new HashMap<>();

    public String getFragmentId() {
        return this.fragmentId;
    }

    public void setFragmentId(final String fragmentId) {
        this.fragmentId = fragmentId;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getSizeInBytes() {
        return this.sizeInBytes;
    }

    public void setSizeInBytes(final String sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public Map<String, String> getCustomInformation() {
        return customInformation;
    }

    public void setCustomInformation(final Map<String, String> customInformation) {
        this.customInformation = customInformation;
    }

}
