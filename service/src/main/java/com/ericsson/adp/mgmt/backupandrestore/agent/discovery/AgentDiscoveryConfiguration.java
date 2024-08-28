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
package com.ericsson.adp.mgmt.backupandrestore.agent.discovery;

import com.ericsson.adp.mgmt.backupandrestore.kubernetes.KubernetesService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Decides which agent discovery service will be used.
 */
@Configuration
public class AgentDiscoveryConfiguration {

    private static final Logger log = LogManager.getLogger(AgentDiscoveryConfiguration.class);

    private boolean isAgentDiscoveryEnabled;
    private String agentIdLabel;
    private String agentScopeAnnotation;
    private KubernetesService kubernetesService;

    /**
     * Instantiates agent discovery service based on configuration.
     * @return AgentDiscoveryService
     */
    @Bean
    public AgentDiscoveryService getAgentDiscoveryService() {
        if (isAgentDiscoveryEnabled) {
            log.info("Enabling kubernetes agent discovery");
            return getKubernetesAgentDiscoveryService();
        }
        log.info("Not enabling kubernetes agent discovery");
        return new NoAgentDiscoveryService();
    }

    private KubernetesAgentDiscoveryService getKubernetesAgentDiscoveryService() {
        final KubernetesAgentDiscoveryService kubernetesAgentDiscoveryService = new KubernetesAgentDiscoveryService();
        kubernetesAgentDiscoveryService.setAgentIdLabel(agentIdLabel);
        kubernetesAgentDiscoveryService.setAgentScopeAnnotation(agentScopeAnnotation);
        kubernetesAgentDiscoveryService.setApi(kubernetesService.getKubernetesApi());
        kubernetesAgentDiscoveryService.setNamespace(kubernetesService.getOrchestratorNamespace());
        return kubernetesAgentDiscoveryService;
    }

    @Autowired
    public void setKubernetesService(final KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    @Value("${flag.enable.agent.discovery:false}")
    public void setIsAgentDiscoveryEnabled(final boolean isAgentDiscoveryEnabled) {
        this.isAgentDiscoveryEnabled = isAgentDiscoveryEnabled;
    }

    @Value("${kubernetes.agent.id.label:adpbrlabelkey}")
    public void setAgentIdLabel(final String agentIdLabel) {
        this.agentIdLabel = agentIdLabel;
    }

    @Value("${kubernetes.agent.scope.annotation:backupType}")
    public void setAgentScopeAnnotation(final String agentScopeAnnotation) {
        this.agentScopeAnnotation = agentScopeAnnotation;
    }

}
