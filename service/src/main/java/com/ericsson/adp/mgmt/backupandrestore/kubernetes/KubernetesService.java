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
package com.ericsson.adp.mgmt.backupandrestore.kubernetes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.agent.discovery.KubernetesConfigurationException;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.openapi.Configuration;

/**
 * Provides access to the kubernetes api
 */
@Service
public class KubernetesService {

    private static final Logger log = LogManager.getLogger(KubernetesService.class);

    private CoreV1Api kubernetesApi;
    private String namespaceEnvironmentVariable;

    /**
     * Constructor - configures the kubernetes api
     */
    public KubernetesService() {
        setKubernetesClient();
    }

    public CoreV1Api getKubernetesApi() {
        return kubernetesApi;
    }

    public String getOrchestratorNamespace() {
        return System.getenv(this.namespaceEnvironmentVariable);
    }

    private void setKubernetesClient() {
        try {
            final ApiClient apiClient = Config.defaultClient();
            Configuration.setDefaultApiClient(apiClient);
            kubernetesApi = new CoreV1Api();
        } catch (final Exception e) {
            log.error("Failed to set Kubernetes client", e);
            throw new KubernetesConfigurationException(e);
        }
    }

    @Value("${kubernetes.namespace.environment.variable:ORCHESTRATOR_NAMESPACE}")
    public void setNamespaceEnvironmentVariable(final String namespaceEnvironmentVariable) {
        this.namespaceEnvironmentVariable = namespaceEnvironmentVariable;
    }
}
