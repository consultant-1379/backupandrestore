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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.easymock.EasyMock;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;

public class BRMSoftwareVersionJsonTest {

    @Test
    public void new_softwareVersionWithValidProductionDate_jsonWithThatDate() throws Exception {
        assertEquals("1984-01-02T03:04:05Z", new BRMSoftwareVersionJson(mockSoftwareVersion("1984-01-02T03:04:05Z")).getDate());
        assertEquals("1984-01-02T03:04:05+01:00", new BRMSoftwareVersionJson(mockSoftwareVersion("1984-01-02T03:04:05+01:00")).getDate());
    }

    @Test
    public void new_softwareVersionWithInvalidProductionDate_jsonWithCurrentDate() throws Exception {
        final BRMSoftwareVersionJson json = new BRMSoftwareVersionJson(mockSoftwareVersion("1984-01-02T03:04:05"));

        assertNotEquals("1984-01-02T03:04:05Z", json.getDate());
        assertNotEquals("1984-01-02T03:04:05", json.getDate());
    }

    @Test
    public void equalityTest() {
        final var version1 = new BRMSoftwareVersionJson(makeSoftwareVersion("1984-01-02T03:04:05Z"));
        final var version2 = new BRMSoftwareVersionJson(makeSoftwareVersion("1984-01-02T03:04:05Z"));
        assertEquals(version1, version2);
        assertEquals(version1.hashCode(), version2.hashCode());
        version2.setDescription("test");
        assertNotEquals(version1, version2);
        assertNotEquals(version1.hashCode(), version2.hashCode());
    }

    private SoftwareVersion mockSoftwareVersion(final String productionDate) {
        final SoftwareVersion softwareVersion = EasyMock.createNiceMock(SoftwareVersion.class);
        EasyMock.expect(softwareVersion.getDate()).andReturn(productionDate);
        EasyMock.replay(softwareVersion);
        return softwareVersion;
    }

    private SoftwareVersion makeSoftwareVersion(final String productionDate) {
        final SoftwareVersion version = new SoftwareVersion();
        version.setDate(productionDate);
        version.setDescription("some description");
        version.setProductName("some name");
        version.setProductNumber("some number");
        version.setType("some type");
        version.setProductRevision("some revision");
        return version;
    }
}
