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
package com.ericsson.adp.mgmt.backupandrestore.backup;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.exception.ImportExportException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientImportProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.client.SftpClient;
import com.ericsson.adp.mgmt.backupandrestore.external.client.HttpClient;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.SftpConnection;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.ExternalConnection;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.HttpConnection;
import com.ericsson.adp.mgmt.backupandrestore.util.ExternalClientPropertiesValidator;

public class ExternalConnectionFactoryTest {

    @Test
    public void connect_validInput_valid() {

        final ExternalClientImportProperties externalClientProperties = createMock(ExternalClientImportProperties.class);
        final ExternalClientPropertiesValidator externalClientPropertiesValidator = createMock(ExternalClientPropertiesValidator.class);
        final SftpClient sftpClient = createMock(SftpClient.class);

        externalClientPropertiesValidator.validateExternalClientProperties(externalClientProperties);
        expectLastCall();

        expect(externalClientProperties.isUsingHttpUriScheme()).andReturn(false);

        final SftpConnection sftpConnection = createMock(SftpConnection.class);

        expect(sftpClient.connect(externalClientProperties)).andReturn(sftpConnection);
        replay(sftpClient, sftpConnection, externalClientProperties);

        final ExternalConnectionFactory externalConnectionFactory = new ExternalConnectionFactory();
        externalConnectionFactory.setSftpClient(sftpClient);
        externalConnectionFactory.setExternalClientPropertiesValidator(externalClientPropertiesValidator);

        final ExternalConnection connection = externalConnectionFactory.connect(externalClientProperties);

        assertTrue(connection instanceof SftpConnection);

    }

    @Test(expected = ImportExportException.class)
    public void connect_invalidClientProperties_throwsException() {

        final ExternalClientImportProperties externalClientProperties = createMock(ExternalClientImportProperties.class);
        final ExternalClientPropertiesValidator externalClientPropertiesValidator = createMock(ExternalClientPropertiesValidator.class);

        externalClientPropertiesValidator.validateExternalClientProperties(externalClientProperties);
        expectLastCall().andThrow(new ImportExportException("host attribute is invalid - can neither be 0 nor null nor empty"));

        replay(externalClientPropertiesValidator);

        final ExternalConnectionFactory externalConnectionFactory = new ExternalConnectionFactory();
        externalConnectionFactory.setExternalClientPropertiesValidator(externalClientPropertiesValidator);

        externalConnectionFactory.connect(externalClientProperties);

    }

    @Test(expected = ImportExportException.class)
    public void connect_issueWithSftpConnection_throwsException() {

        final ExternalClientImportProperties externalClientProperties = createMock(ExternalClientImportProperties.class);
        final ExternalClientPropertiesValidator externalClientPropertiesValidator = createMock(ExternalClientPropertiesValidator.class);
        final SftpClient sftpClient = createMock(SftpClient.class);

        externalClientPropertiesValidator.validateExternalClientProperties(externalClientProperties);
        expectLastCall();

        expect(externalClientProperties.isUsingHttpUriScheme()).andReturn(false);

        final SftpConnection sftpConnection = createMock(SftpConnection.class);

        expect(sftpClient.connect(externalClientProperties)).andThrow(new ImportExportException("Unable to connect to host localhost on port 333"));
        replay(sftpClient, sftpConnection, externalClientProperties);

        final ExternalConnectionFactory externalConnectionFactory = new ExternalConnectionFactory();
        externalConnectionFactory.setSftpClient(sftpClient);
        externalConnectionFactory.setExternalClientPropertiesValidator(externalClientPropertiesValidator);

        externalConnectionFactory.connect(externalClientProperties);

    }

    @Test
    public void connect_http_validInput_valid() {

        final ExternalClientImportProperties externalClientProperties = createMock(ExternalClientImportProperties.class);
        final ExternalClientPropertiesValidator externalClientPropertiesValidator = createMock(ExternalClientPropertiesValidator.class);
        final HttpClient httpClient = createMock(HttpClient.class);

        externalClientPropertiesValidator.validateExternalClientProperties(externalClientProperties);
        expectLastCall();

        expect(externalClientProperties.isUsingHttpUriScheme()).andReturn(true);

        final HttpConnection httpConnection = createMock(HttpConnection.class);

        expect(httpClient.connect(externalClientProperties)).andReturn(httpConnection);
        replay(httpClient, httpConnection, externalClientProperties);

        final ExternalConnectionFactory externalConnectionFactory = new ExternalConnectionFactory();
        externalConnectionFactory.setHttpClient(httpClient);
        externalConnectionFactory.setExternalClientPropertiesValidator(externalClientPropertiesValidator);

        final ExternalConnection connection = externalConnectionFactory.connect(externalClientProperties);

        assertTrue(connection instanceof HttpConnection);

    }

}
