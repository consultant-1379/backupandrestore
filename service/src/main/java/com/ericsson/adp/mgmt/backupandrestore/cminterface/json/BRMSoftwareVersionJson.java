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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents SoftwareVersion in the BRM model.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BRMSoftwareVersionJson extends SoftwareVersion {

    /**
     * Default constructor, to be used by Jackson.
     */
    public BRMSoftwareVersionJson() {}

    /**
     * Creates json object.
     * @param softwareVersion to be represented.
     */
    public BRMSoftwareVersionJson(final SoftwareVersion softwareVersion) {
        setDescription(softwareVersion.getDescription());
        setProductName(softwareVersion.getProductName());
        setProductNumber(softwareVersion.getProductNumber());
        setProductRevision(softwareVersion.getProductRevision());
        setDate(parseProductionDate(softwareVersion.getDate()));
        setType(softwareVersion.getType());
        setCommercialVersion(softwareVersion.getCommercialVersion());
        setSemanticVersion(softwareVersion.getSemanticVersion());
    }

    @Override
    @JsonProperty("product-name")
    public String getProductName() {
        return super.getProductName();
    }

    @Override
    @JsonProperty("product-number")
    public String getProductNumber() {
        return super.getProductNumber();
    }

    @Override
    @JsonProperty("product-revision")
    public String getProductRevision() {
        return super.getProductRevision();
    }

    @Override
    @JsonProperty("production-date")
    public String getDate() {
        return super.getDate();
    }

    @Override
    @JsonIgnore
    public String getAgentId() {
        return super.getAgentId();
    }

    @Override
    @JsonProperty("semantic-version")
    public String getSemanticVersion() {
        return super.getSemanticVersion();
    }

    @Override
    @JsonProperty("commercial-version")
    public String getCommercialVersion() {
        return super.getCommercialVersion();
    }

    private String parseProductionDate(final String date) {
        try {
            ZonedDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return date;
        } catch (final Exception e) {
            return ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }

    @Override
    public boolean equals(final Object otherObject) {
        boolean result = false;
        if (otherObject instanceof BRMSoftwareVersionJson) {
            final BRMSoftwareVersionJson other = (BRMSoftwareVersionJson) otherObject;
            result = getProductName().equals(other.getProductName()) &&
                    getProductNumber().equals(other.getProductNumber()) &&
                    getProductRevision().equals(other.getProductRevision()) &&
                    getDate().equals(other.getDate()) &&
                    getDescription().equals(other.getDescription()) &&
                    getType().equals(other.getType());
            if (getCommercialVersion() != null) {
                result = result && getCommercialVersion().equals(other.getCommercialVersion());
            }
            if (getSemanticVersion() != null) {
                result = result && getSemanticVersion().equals(other.getSemanticVersion());
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        int hashCode = getProductName().hashCode() +
                getProductNumber().hashCode() +
                getProductRevision().hashCode() +
                getDate().hashCode() +
                getDescription().hashCode() +
                getType().hashCode();
        if (getSemanticVersion() != null) {
            hashCode += getSemanticVersion().hashCode();
        }
        if (getCommercialVersion() != null) {
            hashCode += getCommercialVersion().hashCode();
        }
        return hashCode;
    }

}
