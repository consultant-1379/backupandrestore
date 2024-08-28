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

package com.ericsson.adp.mgmt.bro.api.registration;

import com.ericsson.adp.mgmt.bro.api.exception.InvalidSoftwareVersionException;
import com.ericsson.adp.mgmt.bro.api.exception.InvalidSoftwareVersionException.ErrorMessage;

/**
 * Holds agent's software version.
 */
public class SoftwareVersion {

    private String productName;
    private String productNumber;
    private String revision;
    private String productionDate;
    private String description;
    private String type;
    private String commercialVersion = "0.0.0";
    private String semanticVersion = "0.0.0";

    /**
     * Provides a constructor for the software version. This is sent to the orchestrator during
     * registration and is stored alongside any backups created. If this constructor is called then
     * the following must be set
     *
     * productName, productNumber, revision, productionDate, description, type
     */
    public SoftwareVersion() {
    }

    /**
     * Provides a constructor for the software version. This is sent to the orchestrator during
     * registration and is stored alongside any backups created.
     *
     * @param productName The name of the product
     * @param productNumber The product number for example "APR20140/1"
     * @param revision The revision of the product or example "R1A"
     * @param productionDate The production date of the software
     * @param description The software description
     * @param type The type of software
     */
    public SoftwareVersion(final String productName, final String productNumber,
        final String revision, final String productionDate,
        final String description, final String type) {
        this.productName = productName;
        this.productNumber = productNumber;
        this.revision = revision;
        this.productionDate = productionDate;
        this.description = description;
        this.type = type;
    }

    /**
     * Provides a constructor for the software version. This is sent to the orchestrator during
     * registration and is stored alongside any backups created.
     *
     * @param productName The name of the product
     * @param productNumber The product number for example "APR20140/1"
     * @param revision The revision of the product or example "R1A"
     * @param productionDate The production date of the software
     * @param description The software description
     * @param type The type of software
     * @param commercialVersion commercial version
     * @param semanticVersion semantic version
     */
    public SoftwareVersion(final String productName, final String productNumber,
        final String revision, final String productionDate,
        final String description, final String type, final String commercialVersion, final String semanticVersion) {
        this(productName, productNumber, revision, productionDate, description, type);
        this.commercialVersion = commercialVersion;
        this.semanticVersion = semanticVersion;
    }

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

    public String getRevision() {
        return revision;
    }

    public void setRevision(final String revision) {
        this.revision = revision;
    }

    public String getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(final String productionDate) {
        this.productionDate = productionDate;
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

    /**
     * Validate an instance.
     *
     * @throws InvalidSoftwareVersionException if any field is invalid.
     */
    public void validate() {
        validateField(description, ErrorMessage.DESCRIPTION_IS_BLANK, ErrorMessage.DESCRIPTION_IS_NULL);
        validateField(productionDate, ErrorMessage.PRODUCTION_DATE_IS_BLANK, ErrorMessage.PRODUCTION_DATE_IS_NULL);
        validateField(productName, ErrorMessage.PRODUCT_NAME_IS_BLANK, ErrorMessage.PRODUCT_NAME_IS_NULL);
        validateField(productNumber, ErrorMessage.PRODUCT_NUMBER_IS_BLANK, ErrorMessage.PRODUCT_NUMBER_IS_NULL);
        validateField(revision, ErrorMessage.REVISION_IS_BLANK, ErrorMessage.REVISION_IS_NULL);
        validateField(type, ErrorMessage.TYPE_IS_BLANK, ErrorMessage.TYPE_IS_NULL);
        validateField(commercialVersion, ErrorMessage.COMMERCIALVERSION_IS_BLANK, ErrorMessage.COMMERCIALVERSION_IS_NULL);
        validateField(semanticVersion, ErrorMessage.SEMANTICVERSION_IS_BLANK, ErrorMessage.SEMANTICVERSION_IS_NULL);
    }

    private static void validateField(final String field, final ErrorMessage errorMessageBlank,
                                          final ErrorMessage errorMessageNull) {
        if (field == null) {
            throw new InvalidSoftwareVersionException(errorMessageNull);
        } else if (field.isEmpty()) {
            throw new InvalidSoftwareVersionException(errorMessageBlank);
        }
    }

    @Override
    public String toString() {
        return "SoftwareVersion{" +
                "productName='" + productName + '\'' +
                ", productNumber='" + productNumber + '\'' +
                ", revision='" + revision + '\'' +
                ", productionDate='" + productionDate + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", commercialVersion='" + commercialVersion + '\'' +
                ", semanticVersion='" + semanticVersion + '\'' +
                '}';
    }
}
