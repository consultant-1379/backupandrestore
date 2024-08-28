/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.backupandrestore.agent.discovery;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;

/**
 * Uses Kubernetes API to look for all required agents.
 */
public class KubernetesAgentDiscoveryService implements AgentDiscoveryService {

    private CoreV1Api api;
    private String namespace;
    private String agentIdLabel;
    private String agentScopeAnnotation;

    @Override
    public void validateRegisteredAgents(final BackupManager backupManager,
        final List<Agent> registeredAgents) {
        final Set<String> expectedAgentIds = getExpectedAgentIds(backupManager);
        final Set<String> registeredAgentIds = getRegisteredAgentIds(
            registeredAgents);

        if (!expectedAgentIds.equals(registeredAgentIds)) {
            throw new InvalidRegisteredAgentsException(expectedAgentIds,
                registeredAgentIds);
        }
    }

    private Set<String> getExpectedAgentIds(final BackupManager backupManager) {
        try {
            /**
             * version 11 has added another new parameter and again it is not added to the end of the parameters, documenting here to clarify.
             * listNamespacedPod(String namespace, String pretty, Boolean allowWatchBookmarks, String _continue, String fieldSelector,
             * String labelSelector, Integer limit, String resourceVersion, String resourceVersionMatch, Integer timeoutSeconds, Boolean watch)
             * listNamespacedPod(String namespace, String pretty, Boolean allowWatchBookmarks, String _continue, String fieldSelector,
             * String labelSelector, Integer limit, String resourceVersion, ..........................., Integer timeoutSeconds, Boolean watch)
            **/
            return api
                .listNamespacedPod(this.namespace)
                .pretty(null)
                .allowWatchBookmarks(null)
                ._continue(null)
                .fieldSelector(null)
                .labelSelector(this.agentIdLabel)
                .limit(null)
                .resourceVersion(null)
                .resourceVersionMatch(null)
                .sendInitialEvents(null)
                .timeoutSeconds(null)
                .watch(null)
                .execute()
                .getItems()
                .stream()
                .map(pod -> {
                    final String agentId = pod.getMetadata().getLabels().get(this.agentIdLabel);
                    if (backupManager.ownsAgent(getScopeAnnotation(pod), agentId)) {
                        return Optional.of(agentId);
                    } else {
                        return Optional.<String>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        } catch (final ApiException e) {
            throw new InvalidRegisteredAgentsException(e);
        }
    }

    private String getScopeAnnotation(final V1Pod pod) {
        final Map<String, String> annotations = pod.getMetadata().getAnnotations();
        if (annotations == null) {
            return null;
        }
        return annotations.get(this.agentScopeAnnotation);
    }

    private Set<String> getRegisteredAgentIds(
        final List<Agent> registeredAgents) {
        return registeredAgents
            .stream()
            .map(Agent::getAgentId)
            .collect(Collectors.toSet());
    }

    public void setApi(final CoreV1Api api) {
        this.api = api;
    }

    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }

    public void setAgentIdLabel(final String agentIdLabel) {
        this.agentIdLabel = agentIdLabel;
    }

    public void setAgentScopeAnnotation(final String agentScopeAnnotation) {
        this.agentScopeAnnotation = agentScopeAnnotation;
    }

}
