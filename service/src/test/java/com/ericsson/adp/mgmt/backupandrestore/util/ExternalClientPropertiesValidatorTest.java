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
package com.ericsson.adp.mgmt.backupandrestore.util;

import static org.easymock.EasyMock.createMock;

import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidExternalClientProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientExportProperties;

public class ExternalClientPropertiesValidatorTest {

    private ExternalClientPropertiesValidator validator;
    private Backup backup;

    @Before
    public void setUp() {
        validator = new ExternalClientPropertiesValidator();
        backup = createMock(Backup.class);
    }

    @Test
    public void validateExternalClientProperties_validData_valid() throws Exception {

        final ExternalClientExportProperties clientProperties = new ExternalClientExportProperties("sftp://user@localhost:22/my/path", "password",
                Paths.get("backupDataPath"), Paths.get("backupFilePath"), "1", backup);
        validator.validateExternalClientProperties(clientProperties);
    }

    @Test(expected = InvalidExternalClientProperties.class)
    public void validateExternalClientProperties_invalidUri_throwsException() throws Exception {

        final ExternalClientExportProperties clientProperties = new ExternalClientExportProperties("sftp://user", "password",
                Paths.get("backupDataPath"), Paths.get("backupFilePath"), "1", backup);
        validator.validateExternalClientProperties(clientProperties);
    }

    @Test(expected = InvalidExternalClientProperties.class)
    public void validateExternalClientProperties_portZero_throwsException() throws Exception {

        final ExternalClientExportProperties clientProperties = new ExternalClientExportProperties("sftp://user@localhost:0/my/path", "password",
                Paths.get("backupDataPath"), Paths.get("backupFilePath"), "1", backup);
        validator.validateExternalClientProperties(clientProperties);
    }

    @Test
    public void validateExternalClientProperties_http_validData_valid() throws Exception {

        final ExternalClientExportProperties clientProperties = new ExternalClientExportProperties("http://localhost/my/path", "",
                Paths.get("backupDataPath"), Paths.get("backupFilePath"), "1", backup);
        validator.validateExternalClientProperties(clientProperties);
    }
}
