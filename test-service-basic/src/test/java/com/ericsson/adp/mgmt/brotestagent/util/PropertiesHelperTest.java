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
package com.ericsson.adp.mgmt.brotestagent.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

public class PropertiesHelperTest {

    private List<String> value = new ArrayList<>();

    @After
    public void tearDown() {
        this.value.clear();
    }

    @Test
    public void getProperty_unknownProperty_defaultValue() throws Exception {
        assertEquals("54321", PropertiesHelper.getProperty("random.property", "54321"));
    }

    @Test
    public void getPropertyValueList_commaSeparatedProperty_arrayOfValues() {
        PropertiesHelper.loadProperties("./src/test/resources/application.properties");
        this.value = new ArrayList<>(PropertiesHelper.getPropertyValueList("test.agent.fragment.backup.data.path", "54321"));
        assertEquals("./src/test/resources/backup.txt", this.value.get(0));
        assertEquals("./src/test/resources/CustomMetadata.txt", this.value.get(1));
        assertEquals("./src/test/resources/backup2.txt", this.value.get(2));
    }

    @Test
    public void getPropertyValueList_commaSeparatedProperty_defaultValue() {
        PropertiesHelper.loadProperties("./src/test/resources/application.properties");
        this.value = new ArrayList<>(PropertiesHelper.getPropertyValueList("random.property", "54321"));
        assertEquals("54321", this.value.get(0));
    }

    @Test
    public void getPropertyValueList_commaSeparatedProperty_emptyValue() {
        PropertiesHelper.loadProperties("./src/test/resources/application.properties");
        this.value = new ArrayList<>(PropertiesHelper.getPropertyValueList("test.agent.fragment.custom.backup.data.path", "54321"));
        assertEquals("./src/test/resources/CustomMetadata.txt", this.value.get(0));
        assertEquals(0, this.value.get(1).length());
        assertEquals("./src/test/resources/CustomMetadataDownload.txt", this.value.get(2));
    }

    @Test
    public void loadProperties_notAFile_doesntThrowException() throws Exception {
        try {
            PropertiesHelper.loadProperties("nothing");
        } catch (final Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void loadProperties_propertiesFilePath_loadsProperties() throws Exception {
        PropertiesHelper.loadProperties("./src/test/resources/application.properties");

        assertEquals("127.0.0.1", PropertiesHelper.getProperty("orchestrator.host", "127.0.0.1"));
    }

}
