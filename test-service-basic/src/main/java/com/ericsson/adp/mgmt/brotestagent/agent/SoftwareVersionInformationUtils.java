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
package com.ericsson.adp.mgmt.brotestagent.agent;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ericsson.adp.mgmt.bro.api.registration.SoftwareVersion;
import com.ericsson.adp.mgmt.brotestagent.util.PropertiesHelper;

/**
 * Provides Software Version Information
 */
public class SoftwareVersionInformationUtils {

    private static final Logger log = LogManager.getLogger(SoftwareVersionInformationUtils.class);

    private static final String SOFTWARE_VERSION_DESCRIPTION_PROPERTY = "test.agent.softwareVersion.description";
    private static final String SOFTWARE_VERSION_PRODUCTION_DATE_PROPERTY = "test.agent.softwareVersion.productionDate";
    private static final String SOFTWARE_VERSION_PRODUCT_NAME_PROPERTY = "test.agent.softwareVersion.productName";
    private static final String SOFTWARE_VERSION_PRODUCT_NUMBER_PROPERTY = "test.agent.softwareVersion.productNumber";
    private static final String SOFTWARE_VERSION_TYPE_PROPERTY = "test.agent.softwareVersion.type";
    private static final String SOFTWARE_VERSION_REVISION_PROPERTY = "test.agent.softwareVersion.revision";
    private static final String SOFTWARE_VERSION_COMMERCIAL_VERSION = "test.agent.softwareVersion.commercialVersion";
    private static final String SOFTWARE_VERSION_SEMANTIC_VERSION = "test.agent.softwareVersion.semanticVersion";

    private static final String DEFAULT_SOFTWARE_VERSION_DESCRIPTION = "No Description";
    private static final String DEFAULT_SOFTWARE_VERSION_PRODUCTION_DATE = "No date";
    private static final String DEFAULT_SOFTWARE_VERSION_TYPE = "type";
    private static final String DEFAULT_SOFTWARE_VERSION_PRODUCT_NAME = "Test Agent";
    private static final String DEFAULT_SOFTWARE_VERSION_PRODUCT_NUMBER = "2";
    private static final String DEFAULT_SOFTWARE_VERSION_REVISION = "Nope";
    private static final String DEFAULT_SOFTWARE_VERSION_COMMERCIAL_VERSION = "CommercialVersion";
    private static final String DEFAULT_SOFTWARE_VERSION_SEMANTIC_VERSION = "SemanticVersion";

    /**
     * To hide the implicit public constructor
     */
    private SoftwareVersionInformationUtils() {}

    /**
     * Provides Software Version Information
     * @return softwareVersion information
     */
    public static SoftwareVersion getSoftwareVersion() {
        final SoftwareVersion softwareVersion = new SoftwareVersion();
        softwareVersion.setDescription(PropertiesHelper.getProperty(SOFTWARE_VERSION_DESCRIPTION_PROPERTY, DEFAULT_SOFTWARE_VERSION_DESCRIPTION));
        softwareVersion
        .setProductionDate(PropertiesHelper.getProperty(SOFTWARE_VERSION_PRODUCTION_DATE_PROPERTY, DEFAULT_SOFTWARE_VERSION_PRODUCTION_DATE));
        softwareVersion.setProductName(PropertiesHelper.getProperty(SOFTWARE_VERSION_PRODUCT_NAME_PROPERTY, DEFAULT_SOFTWARE_VERSION_PRODUCT_NAME));
        softwareVersion
        .setProductNumber(PropertiesHelper.getProperty(SOFTWARE_VERSION_PRODUCT_NUMBER_PROPERTY, DEFAULT_SOFTWARE_VERSION_PRODUCT_NUMBER));
        softwareVersion.setType(PropertiesHelper.getProperty(SOFTWARE_VERSION_TYPE_PROPERTY, DEFAULT_SOFTWARE_VERSION_TYPE));
        softwareVersion.setRevision(PropertiesHelper.getProperty(SOFTWARE_VERSION_REVISION_PROPERTY, DEFAULT_SOFTWARE_VERSION_REVISION));
        softwareVersion.setCommercialVersion(PropertiesHelper.getProperty(SOFTWARE_VERSION_COMMERCIAL_VERSION,
                DEFAULT_SOFTWARE_VERSION_COMMERCIAL_VERSION));
        softwareVersion.setSemanticVersion(PropertiesHelper.getProperty(SOFTWARE_VERSION_SEMANTIC_VERSION,
                DEFAULT_SOFTWARE_VERSION_SEMANTIC_VERSION));
        return softwareVersion;
    }

    /**
     * Validates if software version information obtained during restore is compatible
     * @param softwareVersion information sent by orchestrator during restore
     * @return true if softwareVersion is compatible
     */
    public static boolean isCompatibleSoftwareVersion(final SoftwareVersion softwareVersion) {
        log.info("Performing Software Validation for revision: {}", softwareVersion.getRevision());
        //this is just a sample validation.
        return getSoftwareVersion().getRevision().equals(softwareVersion.getRevision()) &&
                getSoftwareVersion().getProductName().equals(softwareVersion.getProductName()) &&
                getSoftwareVersion().getProductNumber().equals(softwareVersion.getProductNumber());
    }
}
