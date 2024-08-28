/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.manager;

import java.util.ArrayList;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents the virtual component of a BRM. An empty parentId indicates a non-virtual BRM
 * */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VirtualInformation implements Versioned<VirtualInformation> {
    private String parentId;
    private List<String> agentIds;
    private Version<VirtualInformation> version;

    /**
     * Default constructor
     * */
    public VirtualInformation() { // Default constructor for JSON parsing
        this.parentId = "";
        this.agentIds = new ArrayList<>();
    }

    /**
     * Non-default constructor
     * @param parentId - the parent of the associated vBRM, or blank if not a vBRM. Cannot be null
     * @param agentIds - the list of agents associated with this vBRM, or empty. Cannot be null
     * */
    public VirtualInformation(final String parentId, final List<String> agentIds) {
        this.parentId = parentId;
        this.agentIds = agentIds;
    }

    public String getParentId() {
        return parentId;
    }

    /**
     * Set the parent ID.
     * @param parentId - the parent ID. Cannot be null
     * */
    public void setParentId(final String parentId) {
        this.parentId = parentId;
    }

    public List<String> getAgentIds() {
        return agentIds;
    }

    /**
     * Set the agent list.
     * @param agentIds - the agent IDs. Cannot be null
     * */
    public void setAgentIds(final List<String> agentIds) {
        this.agentIds = agentIds;
    }

    @Override
    @JsonIgnore
    public Version<VirtualInformation> getVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public void setVersion(final Version<VirtualInformation> version) {
        this.version = version;
    }
}
