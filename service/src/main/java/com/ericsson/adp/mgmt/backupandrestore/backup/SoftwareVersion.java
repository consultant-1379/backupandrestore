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
package com.ericsson.adp.mgmt.backupandrestore.backup;

/**
 * Software Version Information.
 */
public class SoftwareVersion {

    private String productName;
    private String productNumber;
    private String productRevision;
    private String date;
    private String description;
    private String type;
    private String agentId;
    private String commercialVersion;
    private String semanticVersion;

    public String getCommercialVersion() {
        return commercialVersion;
    }

    public void setCommercialVersion(final String commercialVersion) {
        this.commercialVersion = commercialVersion;
    }

    public String getSemanticVersion() {
        return semanticVersion;
    }

    public void setSemanticVersion(final String semanticVersion) {
        this.semanticVersion = semanticVersion;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(final String productName) {
        this.productName = productName;
    }

    public String getProductNumber() {
        return productNumber;
    }

    public void setProductNumber(final String productNumber) {
        this.productNumber = productNumber;
    }

    public String getProductRevision() {
        return productRevision;
    }

    public void setProductRevision(final String productRevision) {
        this.productRevision = productRevision;
    }

    public String getDate() {
        return date;
    }

    public void setDate(final String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(final String agentId) {
        this.agentId = agentId;
    }

}
