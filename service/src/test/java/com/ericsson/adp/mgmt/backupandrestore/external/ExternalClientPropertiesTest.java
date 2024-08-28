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
package com.ericsson.adp.mgmt.backupandrestore.external;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.nio.file.Paths;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidURIException;

public class ExternalClientPropertiesTest {

    private Backup backup;
    private Action action;
    private CMMediatorService cmMediatorService;

    @Before
    public void setup() {
        backup = createMock(Backup.class);
        action = createMock(Action.class);
        cmMediatorService = createMock(CMMediatorService.class);
    }

    @Test
    public void externalClientImportPropertiesConstructor_validArgs_valid() {

        final ExternalClientImportProperties externalClientProperties = new ExternalClientImportProperties("sftp://user@localhost:22/path", "pwd",
                Paths.get("/backupdir"), Paths.get("/backupfileloc"));

        assertEquals(false, externalClientProperties.isUsingHttpUriScheme());
        assertEquals("sftp://user@localhost:22/path", externalClientProperties.getUri().toString());
        assertEquals("user", externalClientProperties.getUser());
        assertEquals("localhost", externalClientProperties.getHost());
        assertEquals(22, externalClientProperties.getPort());
        assertEquals("/path", externalClientProperties.getExternalClientPath());
        assertEquals("pwd", externalClientProperties.getPassword());
        assertEquals(Paths.get("/backupdir"), externalClientProperties.getFolderToStoreBackupData());
        assertEquals(Paths.get("/backupfileloc"), externalClientProperties.getFolderToStoreBackupFile());
    }

    @Test
    public void externalClientExportPropertiesConstructor_validArgs_valid() {

        final ExternalClientExportProperties externalClientProperties = new ExternalClientExportProperties("sftp://user@localhost:22/path", "pwd",
                Paths.get("/backupdir"), Paths.get("/backupfileloc"), "1", backup);

        expect(backup.getName()).andReturn("Backup1");
        replay(backup);
        assertEquals("Backup1", externalClientProperties.getBackupName());
        assertEquals(false, externalClientProperties.isUsingHttpUriScheme());
        assertEquals("sftp://user@localhost:22/path", externalClientProperties.getUri().toString());
        assertEquals("user", externalClientProperties.getUser());
        assertEquals("localhost", externalClientProperties.getHost());
        assertEquals(22, externalClientProperties.getPort());
        assertEquals("1", externalClientProperties.getBackupManagerId());
        assertEquals("/path", externalClientProperties.getExternalClientPath());
        assertEquals("pwd", externalClientProperties.getPassword());
        assertEquals(Paths.get("/backupdir"), externalClientProperties.getBackupDataPath());
        assertEquals(Paths.get("/backupfileloc"), externalClientProperties.getBackupFilePath());
        assertEquals(backup, externalClientProperties.getBackup());

    }

    @Test(expected = InvalidURIException.class)
    public void constructor_InvalidURI_throwException() {
        new ExternalClientImportProperties("sftuseruser@localhost:22/path", "pwd", Paths.get("/backupdir"), Paths.get("/backupfileloc"));
    }

}
