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
import org.junit.Test;

public class SoftwareVersionTest {

    private static SoftwareVersion getSoftwareVersionAllSet() {
        final SoftwareVersion softwareVersion = new SoftwareVersion();
        softwareVersion.setDescription("description");
        softwareVersion.setProductionDate("productionDate");
        softwareVersion.setRevision("revision");
        softwareVersion.setProductNumber("productNumber");
        softwareVersion.setProductName("productName");
        softwareVersion.setType("type");
        softwareVersion.setSemanticVersion("semanticVersion");
        softwareVersion.setCommercialVersion("commercialVersion");
        return softwareVersion;
    }

    private static SoftwareVersion getSoftwareVersionAllBlank() {
        final SoftwareVersion softwareVersion = new SoftwareVersion();
        softwareVersion.setDescription("");
        softwareVersion.setProductionDate("");
        softwareVersion.setRevision("");
        softwareVersion.setProductNumber("");
        softwareVersion.setProductName("");
        softwareVersion.setType("");
        softwareVersion.setSemanticVersion("");
        softwareVersion.setCommercialVersion("");
        return softwareVersion;
    }

    @Test(expected = InvalidSoftwareVersionException.class)
    public void validate_InvalidSoftwareVersion_InvalidSoftwareVersionException() {
        SoftwareVersion softwareVersion = new SoftwareVersion();
        softwareVersion.validate();
    }

    @Test(expected = InvalidSoftwareVersionException.class)
    public void validate_DescriptionNull_InvalidSoftwareVersionException() {
        final SoftwareVersion softwareVersion = getSoftwareVersionAllSet();
        softwareVersion.setDescription(null);
        softwareVersion.validate();
    }

    @Test(expected = InvalidSoftwareVersionException.class)
    public void validate_ProductionDateNull_InvalidSoftwareVersionException() {
        final SoftwareVersion softwareVersion = getSoftwareVersionAllSet();
        softwareVersion.setProductionDate(null);
        softwareVersion.validate();
    }

    @Test(expected = InvalidSoftwareVersionException.class)
    public void validate_RevisionNull_InvalidSoftwareVersionException() {
        final SoftwareVersion softwareVersion = getSoftwareVersionAllSet();
        softwareVersion.setRevision(null);
        softwareVersion.validate();
    }

    @Test(expected = InvalidSoftwareVersionException.class)
    public void validate_ProductNameNull_InvalidSoftwareVersionException() {
        final SoftwareVersion softwareVersion = getSoftwareVersionAllSet();
        softwareVersion.setProductName(null);
        softwareVersion.validate();
    }

    @Test(expected = InvalidSoftwareVersionException.class)
    public void validate_AllBlank_InvalidSoftwareVersionException() {
        final SoftwareVersion softwareVersion = getSoftwareVersionAllBlank();
        softwareVersion.validate();
    }

    @Test(expected = InvalidSoftwareVersionException.class)
    public void validate_DescriptionBlank_InvalidSoftwareVersionException() {
        final SoftwareVersion softwareVersion = getSoftwareVersionAllSet();
        softwareVersion.setDescription("");
        softwareVersion.validate();
    }

    @Test(expected = InvalidSoftwareVersionException.class)
    public void validate_ProductionDateBlank_InvalidSoftwareVersionException() {
        final SoftwareVersion softwareVersion = getSoftwareVersionAllSet();
        softwareVersion.setProductionDate("");
        softwareVersion.validate();
    }

    @Test(expected = InvalidSoftwareVersionException.class)
    public void validate_RevisionBlank_InvalidSoftwareVersionException() {
        final SoftwareVersion softwareVersion = getSoftwareVersionAllSet();
        softwareVersion.setRevision("");
        softwareVersion.validate();
    }

    @Test(expected = InvalidSoftwareVersionException.class)
    public void validate_ProductNameBlank_InvalidSoftwareVersionException() {
        final SoftwareVersion softwareVersion = getSoftwareVersionAllSet();
        softwareVersion.setProductName("");
        softwareVersion.validate();
    }

    @Test
    public void softwareVersionValid() {
        final SoftwareVersion softwareVersion = getSoftwareVersionAllSet();
        softwareVersion.validate();
    }
}
