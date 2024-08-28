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
package com.ericsson.adp.mgmt.backupandrestore.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.client.HttpClient;
import com.ericsson.adp.mgmt.backupandrestore.external.client.SftpClient;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.ExternalConnection;
import com.ericsson.adp.mgmt.backupandrestore.util.ExternalClientPropertiesValidator;

/**
 * ExternalConnectionFactory is used by BackupImporter and BackupExporter.
 */
@Component
public class ExternalConnectionFactory {

    private ExternalClientPropertiesValidator externalClientPropertiesValidator;
    private SftpClient sftpClient;
    private HttpClient httpClient;

    /**
     * connect helps to validate external client properties and establish connection with SftpClient
     *
     * @param externalClientProperties
     *            externalClientProperties
     * @return ExternalConnection
     */
    protected ExternalConnection connect(final ExternalClientProperties externalClientProperties) {

        validateExternalClientProperties(externalClientProperties);
        if (externalClientProperties.isUsingHttpUriScheme()) {
            return httpClient.connect(externalClientProperties);
        } else {
            return sftpClient.connect(externalClientProperties);
        }
    }

    private void validateExternalClientProperties(final ExternalClientProperties externalClientProperties) {
        externalClientPropertiesValidator.validateExternalClientProperties(externalClientProperties);
    }

    /**
     * set the ExternalClientPropertiesValidator
     *
     * @param externalClientPropertiesValidator
     *            externalClientPropertiesValidator
     */
    @Autowired
    public void setExternalClientPropertiesValidator(final ExternalClientPropertiesValidator externalClientPropertiesValidator) {
        this.externalClientPropertiesValidator = externalClientPropertiesValidator;
    }

    /**
     * set sftpClient
     *
     * @param sftpClient
     *            the sftpClient to set
     */
    @Autowired
    public void setSftpClient(final SftpClient sftpClient) {
        this.sftpClient = sftpClient;
    }

    /**
     * set httpClient
     *
     * @param httpClient
     *            the httpClient to set
     */
    @Autowired
    public void setHttpClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }
}
