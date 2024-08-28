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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api.APIlistNamespacedPodRequest;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;

public class KubernetesAgentDiscoveryServiceTest {

    private static final String NAMESPACE = "namespace";
    private static final String LABEL = "label";
    private static final String ANNOTATION = "annotation";

    private KubernetesAgentDiscoveryService agentDiscoveryService;
    private BackupManager backupManager;
    private List<Agent> registeredAgents;
    private CoreV1Api api;
    private MockedAgentFactory agentMock;

    @Before
    public void setup() {
        this.api = createMock(CoreV1Api.class);

        this.agentDiscoveryService = new KubernetesAgentDiscoveryService();
        this.agentDiscoveryService.setApi(this.api);
        this.agentDiscoveryService.setNamespace(NAMESPACE);
        this.agentDiscoveryService.setAgentIdLabel(LABEL);
        this.agentDiscoveryService.setAgentScopeAnnotation(ANNOTATION);

        this.backupManager = createMock(BackupManager.class);
        agentMock = new MockedAgentFactory();
        this.registeredAgents = Arrays.asList(agentMock.mockedAgent("1"), agentMock.mockedAgent("2"));
    }

    @Test
    public void validateRegisteredAgents_defaultBackupManagerWithAllExpectedAgentsRegistered_agentsAreValid() throws Exception {
        mockApi(Arrays.asList(mockPod("1"), mockPod("2")));
        mockDefaultBackupManager();

        this.agentDiscoveryService.validateRegisteredAgents(backupManager, registeredAgents);

        verify(api);
    }

    @Test
    public void validateRegisteredAgents_defaultBackupManagerWithAllExpectedAgentsWithDifferentScopesRegistered_agentsAreValid() throws Exception {
        mockApi(Arrays.asList(mockPod("1"), mockPod("2", "A")));
        mockDefaultBackupManager();

        this.agentDiscoveryService.validateRegisteredAgents(backupManager, registeredAgents);

        verify(api);
    }

    @Test(expected = InvalidRegisteredAgentsException.class)
    public void validateRegisteredAgents_defaultBackupManagerWithSomeExpectedAgentsNotRegistered_throwsException() throws Exception {
        mockApi(Arrays.asList(mockPod("1"), mockPod("2"), mockPod("3")));
        mockDefaultBackupManager();

        this.agentDiscoveryService.validateRegisteredAgents(backupManager, registeredAgents);
    }

    @Test(expected = InvalidRegisteredAgentsException.class)
    public void validateRegisteredAgents_defaultBackupManagerWithMoreRegisteredThanExpectedAgents_throwsException() throws Exception {
        mockApi(Arrays.asList(mockPod("1")));
        mockDefaultBackupManager();

        this.agentDiscoveryService.validateRegisteredAgents(backupManager, registeredAgents);
    }

    @Test
    public void validateRegisteredAgents_scopedBackupManagerWithAllExpectedAgentsOfThatScopeRegistered_agentsAreValid() throws Exception {
        mockApi(Arrays.asList(mockPod("1", "X"), mockPod("2", "X"), mockPod("3", "Y"), mockPod("3")));
        mockBackupManager("X");

        this.agentDiscoveryService.validateRegisteredAgents(backupManager, registeredAgents);

        verify(api);
    }

    @Test(expected = InvalidRegisteredAgentsException.class)
    public void validateRegisteredAgents_scopedBackupManagerWithSomeExpectedAgentsOfThatScopeNotRegistered_throwsException() throws Exception {
        mockApi(Arrays.asList(mockPod("1", "Q"), mockPod("2", "Q"), mockPod("3", "Q")));
        mockBackupManager("Q");

        this.agentDiscoveryService.validateRegisteredAgents(backupManager, registeredAgents);
    }

    @Test(expected = InvalidRegisteredAgentsException.class)
    public void validateRegisteredAgents_scopedBackupManagerWithMoreRegisteredThanExpectedAgents_throwsException() throws Exception {
        mockApi(Arrays.asList(mockPod("1", "W")));
        mockBackupManager("W");

        this.agentDiscoveryService.validateRegisteredAgents(backupManager, registeredAgents);
    }

    @Test(expected = InvalidRegisteredAgentsException.class)
    public void validateRegisteredAgents_oneAgentInScopeHasNoAnnotations_throwsException() throws Exception {
        mockApi(Arrays.asList(mockPod("1", "W"), mockPod("2", null, false)));
        mockBackupManager("W");

        this.agentDiscoveryService.validateRegisteredAgents(backupManager, registeredAgents);
    }

    private void mockBackupManager(final String scope) {
        expect(this.backupManager.ownsAgent(eq(scope), anyObject(String.class))).andReturn(true).anyTimes();
        expect(this.backupManager.ownsAgent(anyObject(String.class), anyObject(String.class))).andReturn(false).anyTimes();
        expect(this.backupManager.ownsAgent(eq(null), anyObject(String.class))).andReturn(false).anyTimes();
        replay(backupManager);
    }

    private void mockDefaultBackupManager() {
        expect(this.backupManager.ownsAgent(eq(null), anyObject(String.class))).andReturn(true).anyTimes();
        expect(this.backupManager.ownsAgent(anyObject(String.class), anyObject(String.class))).andReturn(true).anyTimes();
        replay(backupManager);
    }

    private void mockApi(final List<V1Pod> pods) throws ApiException {
        APIlistNamespacedPodRequest expectedRequest = EasyMock.mock(APIlistNamespacedPodRequest.class);
        expect(api.listNamespacedPod(NAMESPACE)).andReturn(expectedRequest);
        expect(expectedRequest.pretty(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.allowWatchBookmarks(EasyMock.anyBoolean())).andReturn(expectedRequest);
        expect(expectedRequest._continue(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.fieldSelector(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.labelSelector(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.limit(EasyMock.anyInt())).andReturn(expectedRequest);
        expect(expectedRequest.resourceVersion(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.resourceVersionMatch(EasyMock.anyString())).andReturn(expectedRequest);
        expect(expectedRequest.sendInitialEvents(EasyMock.anyBoolean())).andReturn(expectedRequest);
        expect(expectedRequest.timeoutSeconds(EasyMock.anyInt())).andReturn(expectedRequest);
        expect(expectedRequest.watch(EasyMock.anyBoolean())).andReturn(expectedRequest);
        expect(expectedRequest.execute()).andReturn(mockApiResponse(pods));
        replay(api, expectedRequest);
    }

    private V1PodList mockApiResponse(final List<V1Pod> pods) {
        final V1PodList podList = createMock(V1PodList.class);
        expect(podList.getItems()).andReturn(pods);
        replay(podList);
        return podList;
    }

    private V1Pod mockPod(final String agentId) {
        return mockPod(agentId, null, true);
    }

    private V1Pod mockPod(final String agentId, final String scope) {
        return mockPod(agentId, scope, true);
    }

    private V1Pod mockPod(final String agentId, final String scope, final boolean hasAnnotations) {
        final Map<String, String> labels = new HashMap<>();
        labels.put(LABEL, agentId);

        Map<String, String> annotations = null;
        if (hasAnnotations) {
            annotations = new HashMap<>();
            if (scope != null) {
                annotations.put(ANNOTATION, scope);
            }
        }

        final V1ObjectMeta objectMetadata = createMock(V1ObjectMeta.class);
        expect(objectMetadata.getLabels()).andReturn(labels);
        expect(objectMetadata.getAnnotations()).andReturn(annotations);

        final V1Pod pod = createMock(V1Pod.class);
        expect(pod.getMetadata()).andReturn(objectMetadata).anyTimes();

        replay(pod, objectMetadata);
        return pod;
    }

}
