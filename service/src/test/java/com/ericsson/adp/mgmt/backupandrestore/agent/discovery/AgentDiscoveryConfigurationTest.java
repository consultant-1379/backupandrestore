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
import io.kubernetes.client.openapi.apis.CoreV1Api;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;

public class AgentDiscoveryConfigurationTest {

    private AgentDiscoveryConfiguration agentDiscoveryConfiguration;

    @Before
    public void setup() {
        this.agentDiscoveryConfiguration = new AgentDiscoveryConfiguration();
    }

    @Test
    public void getAgentDiscoveryService_notEnabled_noAgentDiscoveryService() throws Exception {
        assertTrue(this.agentDiscoveryConfiguration.getAgentDiscoveryService() instanceof NoAgentDiscoveryService);
    }

    @Test
    public void getAgentDiscoveryService_enabled_kubernetesAgentDiscoveryService() throws Exception {
        final KubernetesService kubernetesService = createMock(KubernetesService.class);
        expect(kubernetesService.getKubernetesApi()).andReturn(new CoreV1Api());
        expect(kubernetesService.getOrchestratorNamespace()).andReturn("namespace");
        replay(kubernetesService);

        this.agentDiscoveryConfiguration.setIsAgentDiscoveryEnabled(true);
        this.agentDiscoveryConfiguration.setKubernetesService(kubernetesService);

        assertTrue(this.agentDiscoveryConfiguration.getAgentDiscoveryService() instanceof KubernetesAgentDiscoveryService);
    }

}
