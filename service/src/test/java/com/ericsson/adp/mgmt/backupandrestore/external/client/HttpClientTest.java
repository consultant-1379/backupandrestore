/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.external.client;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientImportProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.HttpConnection;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.exception.ImportExportException;
import static org.easymock.EasyMock.createMock;

public class HttpClientTest {

    private ExternalClientProperties externalClientProperties;
    private Action action;
    private CMMediatorService cmMediatorService;

    @Before
    public void setup() {
        action = createMock(Action.class);
        cmMediatorService = createMock(CMMediatorService.class);
        externalClientProperties = new ExternalClientImportProperties("http://localhost/path", "password", Paths.get("/backupdir"),
                Paths.get("/backupfile"));
    }

    @Test(expected = ImportExportException.class)
    public void connect_validInput_valid() {
        final HttpClient httpClient = new HttpClient();
        // final ArchiveService archiveService = new ArchiveService();
        // httpClient.setArchiveService(archiveService);
        final HttpConnection connection = httpClient.connect(externalClientProperties);
        assertEquals(connection.getClass(), HttpConnection.class);
    }
}
