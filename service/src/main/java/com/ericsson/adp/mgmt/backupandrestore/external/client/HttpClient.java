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
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.external.client;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import com.ericsson.adp.mgmt.backupandrestore.exception.ImportExportException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.HttpConnection;

/**
 * Its an HttpClient which can connect with Http Server
 *
 * For deprecation see ADPPRG-41214
 */
@Component
@Deprecated(since = "30/06/21")
public class HttpClient implements ExternalClient {
    /**
     * connect method connects with Http Server
     *
     * @param externalClientProperties
     *            externalClientProperties
     * @return HttpConnection
     *
     */
    @Override
    public HttpConnection connect(final ExternalClientProperties externalClientProperties) {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        //To not buffer data during Post(upload), causes problem for large files
        requestFactory.setBufferRequestBody(false);
        // blocked to be compliant with ADPPRG-41214
        // return new HttpConnection(new RestTemplate(requestFactory), archiveService)
        throw new ImportExportException ("http import/export is not supported");
    }
}
