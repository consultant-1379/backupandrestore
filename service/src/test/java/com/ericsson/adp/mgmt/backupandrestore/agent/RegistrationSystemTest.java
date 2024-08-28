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
package com.ericsson.adp.mgmt.backupandrestore.agent;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V4_0;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagerResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagersResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.UpdateBackupManagerRequest;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestAgent;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestWithAgents;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class RegistrationSystemTest extends SystemTestWithAgents {


    @Test
    public void register_multipleAgents_orchestratorAcceptsRegistrations() throws Exception {
        final String agentId_1 = "1";
        final String agentId_2 = "2";
        final String agentId_3 = "3";
        final String agentId_4 = "4";
        createTestAgent(agentId_1, API_V3_0);
        createTestAgent(agentId_2, API_V3_0);
        createTestAgent(agentId_3, API_V3_0);
        createTestAgent(agentId_4, API_V4_0);
        assertAllAgentsAreRegistered(agentId_1, agentId_2, agentId_3, agentId_4);
    }

    @Test
    public void register_agentRegistersAndGoesOfflineMultipleTimes_orchestratorHandlesRegistrationAndDisconnects() throws Exception {
        final String agentId = "1";
        SystemTestAgent agent = createTestAgent(agentId, API_V3_0);

        assertAllAgentsAreRegistered(agentId);

        agent.shutdown();
        waitUntilAgentIsNotRegistered(agentId);

        assertTrue(getRegisteredAgents().isEmpty());

        agent = createTestAgent(agentId, API_V3_0);

        assertAllAgentsAreRegistered(agentId);

        agent.shutdown();
        waitUntilAgentIsNotRegistered(agentId);

        assertTrue(getRegisteredAgents().isEmpty());
    }

    @Test
    public void register_multipleAgentsRegisteringAndDisconnecting_orchestratorMaintainsConsistentListOfAgents() throws Exception {
        final String agentId_1 = "1";
        final String agentId_2 = "2";
        final String agentId_3 = "3";
        final String agentId_4 = "4";
        final SystemTestAgent agentOne = createTestAgent(agentId_1, API_V3_0);
        final SystemTestAgent agentTwo = createTestAgent(agentId_2, API_V3_0);
        final SystemTestAgent agentThree = createTestAgent(agentId_3, API_V3_0);

        assertAllAgentsAreRegistered(agentId_1, agentId_2, agentId_3);

        agentTwo.shutdown();
        waitUntilAgentIsNotRegistered(agentId_2);

        createTestAgent(agentId_4, API_V3_0);

        assertAllAgentsAreRegistered(agentId_1, agentId_4, agentId_3);

        agentOne.shutdown();
        waitUntilAgentIsNotRegistered(agentId_1);

        agentThree.shutdown();
        waitUntilAgentIsNotRegistered(agentId_3);

        createTestAgent(agentId_1, API_V3_0);

        assertAllAgentsAreRegistered(agentId_1, agentId_4);
    }

    @Test
    public void register_agentWithInvalidScope_newBackupManagerForThatScopeIsCreated() throws Exception {
        final String agentId = "aaaaaaa";
        final SystemTestAgent agent = new SystemTestAgent(agentId, "..");
        this.agents.add(agent);
        agent.register(API_V3_0);

        waitUntil(() -> agent.getInputControlStream().getError() != null);
        assertTrue(agent.getInputControlStream().getError() instanceof StatusRuntimeException);

        final StatusRuntimeException registrationException = (StatusRuntimeException) agent.getInputControlStream().getError();
        assertEquals(Status.INVALID_ARGUMENT.getCode(), registrationException.getStatus().getCode());
    }

    @Test
    public void register_multipleAgentsWithTheSameNonDefaultScope_doesntRecreateOrOverwritersBackupManagerInformationWhenSecondAgentRegisters() throws Exception {
        final String agentId_1 = "a";
        final String agentId_2 = "b";
        createTestAgent(agentId_1, "myOtherNewScope", API_V3_0);

        final UpdateBackupManagerRequest request = new UpdateBackupManagerRequest();
        request.setBackupDomain("domain");
        request.setBackupType("type");
        restTemplate.postForObject(V1_BASE_URL + "backup-manager/myOtherNewScope", request, String.class);

        createTestAgent(agentId_2, "myOtherNewScope", API_V3_0);

        final BackupManagersResponse backupManagers = restTemplate.getForObject(V1_BASE_URL + "backup-manager", BackupManagersResponse.class);
        final List<String> backupManagerIds = backupManagers.getBackupManagers().stream().map(BackupManagerResponse::getBackupManagerId).collect(Collectors.toList());
        assertEquals(1, backupManagerIds.stream().filter("myOtherNewScope"::equals).count());

        final BackupManagerResponse backupManager = restTemplate.getForObject(V1_BASE_URL + "backup-manager/myOtherNewScope", BackupManagerResponse.class);
        assertEquals("myOtherNewScope", backupManager.getBackupManagerId());
        assertEquals("domain", backupManager.getBackupDomain());
        assertEquals("type", backupManager.getBackupType());
    }

    @Test
    public void register_agentWithIdAlreadyRegistered_doesNotRegisterWithStatusAlreadyExists() throws Exception {
        final String agentId = "sameId";
        createTestAgent(agentId, API_V3_0);
        final SystemTestAgent sameIdAgent = createTestAgent(agentId, API_V3_0);

        waitUntil(() -> sameIdAgent.getInputControlStream().getError() != null);
        assertTrue(sameIdAgent.getInputControlStream().getError() instanceof StatusRuntimeException);

        final StatusRuntimeException registrationException = (StatusRuntimeException) sameIdAgent.getInputControlStream().getError();
        assertEquals(Status.ALREADY_EXISTS.getCode(), registrationException.getStatus().getCode());
    }

    @Test
    public void register_agentWithoutId_doesNotRegisterWithStatusInvalidArgument() throws Exception {
        final SystemTestAgent agent = new SystemTestAgent("");
        this.agents.add(agent);
        agent.register(API_V3_0);

        waitUntil(() -> agent.getInputControlStream().getError() != null);
        assertTrue(agent.getInputControlStream().getError() instanceof StatusRuntimeException);

        final StatusRuntimeException registrationException = (StatusRuntimeException) agent.getInputControlStream().getError();
        assertEquals(Status.INVALID_ARGUMENT.getCode(), registrationException.getStatus().getCode());
    }

    @Test
    public void register_agentWithInvalidId_doesNotRegisterWithStatusInvalidArgument() throws Exception {
        final SystemTestAgent agent = new SystemTestAgent("../..");
        this.agents.add(agent);
        agent.register(API_V3_0);

        waitUntil(() -> agent.getInputControlStream().getError() != null);
        assertTrue(agent.getInputControlStream().getError() instanceof StatusRuntimeException);

        final StatusRuntimeException registrationException = (StatusRuntimeException) agent.getInputControlStream().getError();
        assertEquals(Status.INVALID_ARGUMENT.getCode(), registrationException.getStatus().getCode());
    }

    @Test
    public void register_agentsWithMultipleScopes_registersAgentsAndCreatesBackupManagersForEveryDistinctScope() throws Exception {
        final String agentId_1 = "a";
        final String agentId_2 = "b";
        createTestAgent(agentId_1, "first;second", API_V3_0);
        createTestAgent(agentId_2, "second;third", API_V3_0);

        assertAllAgentsAreRegistered(agentId_1, agentId_2);

        assertEquals("first", restTemplate.getForObject(V1_BASE_URL + "backup-manager/first", BackupManagerResponse.class).getBackupManagerId());
        assertEquals("second", restTemplate.getForObject(V1_BASE_URL + "backup-manager/second", BackupManagerResponse.class).getBackupManagerId());
        assertEquals("third", restTemplate.getForObject(V1_BASE_URL + "backup-manager/third", BackupManagerResponse.class).getBackupManagerId());
    }

    private void assertAllAgentsAreRegistered(final String... agentIds) {
        final List<String> registeredAgents = getRegisteredAgents();

        assertEquals(agentIds.length, registeredAgents.size());
        for(final String agentId : registeredAgents) {
            assertTrue(registeredAgents.contains(agentId));
        }
    }

}
