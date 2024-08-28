/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.bro.pm.actuator;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

import com.ericsson.adp.mgmt.backupandrestore.ssl.TomcatConfiguration;

/**
 * Configuration class used as the source of the customizer
 * bean for the Tomcat web server created for the metrics endpoint.
 * NOTE: This class is intentionally placed in a package that is not
 * a sub-package of com.ericsson.adp.mgmt.backupandrestore so that
 * Spring Boot does include the customizer bean in the initialization
 * of the main application context or Tomcat server.
 */
@ManagementContextConfiguration
public class TomcatPMServerConfiguration {

    private TomcatConfiguration tomcatConfiguration;

    /**
     * Creates an instance of the customizer for the PM/actuator's tomcat web server.
     * This customizer mainly captures the Tomcat connector for the actuator/metrics endpoint
     * and adds it to the TomcatConfiguration's list of connectors which are used
     * for reloading the SSL config in the future.
     * @return the customizer bean for the PM/actuator's tomcat web server
     */
    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> configurePMTomcat() {
        return factory -> {
            factory.addConnectorCustomizers((tomcatConnector -> {
                if (tomcatConfiguration.isGlobalTlsEnabled()) {
                    tomcatConfiguration.addConnector(tomcatConnector);
                }
            }));
        };
    }

    @Autowired
    public void setTomcatConfiguration(final TomcatConfiguration tomcatConfiguration) {
        this.tomcatConfiguration = tomcatConfiguration;
    }
}
