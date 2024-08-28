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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ericsson.adp.mgmt.bro.api.registration.SoftwareVersion;
import com.ericsson.adp.mgmt.brotestagent.util.PropertiesHelper;

public class SoftwareVersionInformationUtilsTest {

    @BeforeClass
    public static void setUp() {
        PropertiesHelper.loadProperties("src/test/resources/application.properties");
    }

    @Test
    public void getSoftwareVersion_propertiesFile_returnsSoftwareInformation() {
        final SoftwareVersion softwareVersion = SoftwareVersionInformationUtils.getSoftwareVersion();

        assertEquals("d", softwareVersion.getDescription());
        assertEquals("2019-09-13", softwareVersion.getProductionDate());
        assertEquals("f", softwareVersion.getProductName());
        assertEquals("g", softwareVersion.getProductNumber());
        assertEquals("i", softwareVersion.getRevision());
        assertEquals("h", softwareVersion.getType());
    }

    @Test
    public void isCompatibleSoftwareVersion_validVersion_returnsTrue() {
        assertTrue(SoftwareVersionInformationUtils.isCompatibleSoftwareVersion(SoftwareVersionInformationUtils.getSoftwareVersion()));
    }

    @Test
    public void isCompatibleSoftwareVersion_invalidVersion_returnsFalse() {
        final SoftwareVersion softwareVersion = new SoftwareVersion();
        softwareVersion.setProductNumber("number");
        softwareVersion.setProductName("name");
        softwareVersion.setRevision("revision");

        assertFalse(SoftwareVersionInformationUtils.isCompatibleSoftwareVersion(softwareVersion));
    }
}
