/**------------------------------------------------------------------------------
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
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SCHEMA_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BRO_HTTP_PORT;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BRO_TLS_CMM_NOTIF_PORT;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BRO_TLS_PORT;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;

import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.SchemaRequest;

/**
 * Base class for Mediator requests
 * @param <T> Either the MultivalueMap or Json string
 */
public abstract class MediatorRequestFactory<T> {
    protected boolean globalTlsEnabled;
    protected String broServiceName;
    protected String tlsCMMNotifPort;
    protected String brotlsPort;
    protected String broPort;

    /**
     * To build either an Json String or MultiValueMap HTTP entity
     * @return a HttpEntity
     */
    protected abstract HttpEntity<T> buildHttpEntity();

    /**
     * Get a request to be handled on Mediator
     * @return Request to be push to Mediator
     */
    public SchemaRequest<T> getRequestToCreateSchema() {
        return new SchemaRequest<>(SCHEMA_NAME, buildHttpEntity());
    }

    @Value("${bro.service.name}")
    protected void setBroServiceName(final String broServiceName) {
        this.broServiceName = broServiceName;
    }

    @Value("${server.port:" + BRO_HTTP_PORT + "}")
    public void setBroPort(final String serverPort) {
        this.broPort = serverPort;
    }

    @Value("${global.tls:false}")
    protected void setGlobalTlsEnabled(final boolean globalTlsEnabled) {
        this.globalTlsEnabled = globalTlsEnabled;
    }

    @Value("${cm.mediator.notif.tls.port:" + BRO_TLS_CMM_NOTIF_PORT + "}")
    public void setTlsCMMNotifPort(final String tlsCMMNotifPort) {
        this.tlsCMMNotifPort = tlsCMMNotifPort;
    }

    protected boolean isGlobalTlsEnabled() {
        return globalTlsEnabled;
    }

    protected String getBroServiceName() {
        return broServiceName;
    }

    public String getTlsCMMNotifPort() {
        return tlsCMMNotifPort;
    }

    public String getBroPort() {
        return broPort;
    }

    public String getBroTlsPort() {
        return brotlsPort;
    }

    @Value("${server.tls.port:" + BRO_TLS_PORT + "}")
    public void setTlsPort(final String brotlsPort) {
        this.brotlsPort = brotlsPort;
    }
}
