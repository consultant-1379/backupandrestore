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

package com.ericsson.adp.mgmt.bro.api.test;

import com.ericsson.adp.mgmt.bro.api.registration.RegistrationInformation;
import com.ericsson.adp.mgmt.bro.api.registration.SoftwareVersion;
import com.ericsson.adp.mgmt.control.BackendType;
import com.ericsson.adp.mgmt.metadata.AgentFeature;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.stream.Collectors;

public class RegistrationInformationUtil {

    public static RegistrationInformation getTestRegistrationInformation() {
        final RegistrationInformation registrationInfo = new RegistrationInformation();
        final SoftwareVersion softwareVersion = new SoftwareVersion();
        softwareVersion.setDescription("description");
        softwareVersion.setProductionDate("productionDate");
        softwareVersion.setProductName("productName");
        softwareVersion.setProductNumber("productNumber");
        softwareVersion.setType("type");
        softwareVersion.setSemanticVersion("semanticVersion");
        softwareVersion.setCommercialVersion("commercialVersion");
        softwareVersion.setRevision("revision");
        registrationInfo.setAgentId("123");
        registrationInfo.setApiVersion("4.0");
        registrationInfo.setScope("scope");
        registrationInfo.setAgentFeatures(new ArrayList<>());
        registrationInfo.setBackendType(BackendType.BRO);
        registrationInfo.setSoftwareVersion(softwareVersion);
        return registrationInfo;
    }

    public static RegistrationInformation getTestRegistrationInformationUsingConstructors(final BackendType backendType) {
        final SoftwareVersion softwareVersion = getTestSoftwareVersion();
        final ArrayList<AgentFeature> agentFeatureList = new ArrayList<>(EnumSet.allOf(AgentFeature.class).stream().filter(a -> a != AgentFeature.UNRECOGNIZED).collect(
                Collectors.toList()));
        return new RegistrationInformation("123", "scope", "4.0", softwareVersion, agentFeatureList, backendType);
    }

    public static RegistrationInformation getTestRegistrationInformationUsingWithDefaultBackendTypeAndEmptyFeatureList() {
        final SoftwareVersion softwareVersion = getTestSoftwareVersion();
        return new RegistrationInformation("123", "scope", "4.0", softwareVersion);
    }

    public static RegistrationInformation getNullTestRegistrationInformation() {
        return new RegistrationInformation(null, null, null, null, null, null);
    }

    public static RegistrationInformation getBlankTestRegistrationInformation() {
        final SoftwareVersion softwareVersion = new SoftwareVersion("", "", "", "", "", "", "", "");
        return new RegistrationInformation("", "", "", softwareVersion, null, null);
    }

    public static RegistrationInformation getTestRegistrationInformationWithInvalidApiVersion() {
        final SoftwareVersion softwareVersion = getTestSoftwareVersion();
        return new RegistrationInformation("AwesomeAgent", "Telescope", "", softwareVersion, new ArrayList<>(), BackendType.BRO);
    }

    private static SoftwareVersion getTestSoftwareVersion() {
        return new SoftwareVersion("productName", "productNumber",
            "revision", "productionDate", "description", "type", "commercialVersion","semanticVersion");
    }
}
