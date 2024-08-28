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
package com.ericsson.adp.mgmt.backupandrestore.ssl;

import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;

import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;

public class TomcatConfigurationTest extends SystemTest{

    @Before
    public void setup() {
        tomcatConfiguration = new TomcatConfiguration();
        keyStoreService = mock(KeyStoreService.class);
        tomcatConfiguration.setKeyStoreService(keyStoreService);
    }

    @Test
    public void configureTomcat_globalTlsEnabled_configuresSsl() {
        keyStoreConfiguration.setValidConfiguration(true);
        setTomcatConfiguration_SSL();
        replay(keyStoreService);
        tomcatConfiguration.configureTomcat().customize(new TomcatServletWebServerFactory());
        verify(keyStoreService);
    }

    @Test
    public void configureTomcat_globalTlsNotEnabled_doesNothing() {
        tomcatConfiguration.setGlobalTlsEnabled(false);
        replay(keyStoreService);
        tomcatConfiguration.configureTomcat().customize(new TomcatServletWebServerFactory());
        verify(keyStoreService);
    }

    @Test
    public void refreshContext_globalTlsNotEnabled_doesNothing() {
        tomcatConfiguration.setGlobalTlsEnabled(false);
        replay(keyStoreService);
        tomcatConfiguration.refreshContext();
        verify(keyStoreService);
    }

    @Test
    public void refreshContext_connectorIsNull_doesNothing() {
        tomcatConfiguration.setGlobalTlsEnabled(true);
        replay(keyStoreService);
        tomcatConfiguration.refreshContext();
        verify(keyStoreService);
    }

    @Test
    public void configureTomcat_configured_ClientCertified_required() {
        keyStoreConfiguration.setValidConfiguration(true);
        setTomcatConfiguration_SSL();
        tomcatConfiguration.setverifyRestActionsClientCertificateEnforced("required");
        replay(keyStoreService);
        tomcatConfiguration.configureTomcat().customize(new TomcatServletWebServerFactory());
        verify(keyStoreService);
    }
}
