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
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Responsible for CM Mediator configuration.
 */
@Configuration
public class CMMediatorConfiguration {

    private String url;
    private String urlTLS;
    private String apiSuffix;
    private boolean tlsEnabled;

    /**
     * Base URL to access CM Mediator.
     * @return CM url.
     */
    @Bean
    public String getCMUrl() {
        return (tlsEnabled ? urlTLS : url) + "/" + apiSuffix;
    }

    @Value("${cm.mediator.url:http://localhost:5003/cm}")
    public void setUrl(final String url) {
        this.url = url;
    }

    /**
     * @param urlTLS the urlTLS to set
     */
    @Value("${cm.mediator.url.tls:https://localhost:5004/cm}")
    public void setUrlTLS(final String urlTLS) {
        this.urlTLS = urlTLS;
    }

    @Value("${cm.mediator.api.suffix:api/v1}")
    public void setApiSuffix(final String apiSuffix) {
        this.apiSuffix = apiSuffix;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    @Value("${global.tls:true}")
    public void setTlsEnabled(final boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }
}
