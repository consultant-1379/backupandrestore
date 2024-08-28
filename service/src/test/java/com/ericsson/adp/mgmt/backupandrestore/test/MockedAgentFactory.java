/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.test;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.CancelingActionState;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.RecognizedState;
import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;

import java.util.function.Consumer;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V2_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;


public class MockedAgentFactory {

    /**
     * Generic method for making agent mocks
     * @return the mock agent
     */
    public Agent mockedAgent(){
        final Agent agent = createMock(Agent.class);
        return agent;
    }
    /**
     * Generic method for making agents with a string ID
     * @param agentId The string ID given to the agent
     * @return the mock agent
     */
    public Agent mockedAgent(final String agentId) {
        final Agent agent = mockedAgent(agentId, "alpha");
        return agent;
    }
    /**
     * Generic method for making agents with an int ID
     * @param agentId The int ID given to the agent
     * @return the mock agent
     */
    public Agent mockedAgent(final int agentId) {
        final Agent agent = createNiceMock(Agent.class);
        expect(agent.getAgentId()).andReturn(String.valueOf(agentId)).anyTimes();
        replay(agent);
        return agent;
    }

    /**
     * Generic method for making agents with an int ID
     * @param agentId The int ID given to the agent
     * @param action A consumer that performs the agent action
     * @return the mock agent
     */
    public Agent mockedAgent(final int agentId, Consumer<Agent> action, RecognizedState agentState, boolean failOnJobStage) {
        final Agent agent = createNiceMock(Agent.class);
        if (failOnJobStage) {
            expect(agent.getState()).andReturn(agentState);
        }
        expect(agent.getAgentId()).andReturn(String.valueOf(agentId)).anyTimes();
        action.accept(agent);
        expectLastCall();
        replay(agent);
        return agent;
    }

    /**
     * Generic method for making agents with a specific software version
     * @param softwareVersion The software version specified in BackupRepositoryTest
     * @return the mock agent
     */
    public Agent mockedAgent(final SoftwareVersion softwareVersion) {
        final Agent agent = createMock(Agent.class);
        expect(agent.getSoftwareVersion()).andReturn(softwareVersion);
        replay(agent);
        return agent;
    }

    /**
     * Generic method for making agents with a string ID,a boolean failure case and a backup job
     * @param agentId String ID given to agent
     * @param isAgentFailureCase Boolean to determine if agent fails or not
     * @param job Job for backup
     * @return the mock agent
     */
    public Agent mockedAgent(final String agentId, final boolean isAgentFailureCase, final CreateBackupJob job, final boolean agentDisconnects) {
        final Agent agent = createMock(Agent.class);

        if (isAgentFailureCase) {
            expect(agent.getState()).andReturn(createMock(CancelingActionState.class));
        }
        expect(agent.getAgentId()).andReturn(agentId).anyTimes();
        agent.prepareForBackup(job);
        expectLastCall();
        agent.executeBackup();
        expectLastCall();
        expect(agent.getApiVersion()).andReturn(API_V3_0).anyTimes();
        agent.executeBackupPostAction();
        expectLastCall();
        if (isAgentFailureCase) {
            expect(agent.isConnectionCancelled()).andReturn(agentDisconnects).anyTimes();
            expectLastCall();
            agent.cancelAction();
        } else {
            agent.finishAction();
        }
        expectLastCall();
        replay(agent);
        return agent;
    }

    /**
     * Generic method for making agents with any String ID and any scope
     * @param id The String ID given to the agent
     * @param scope The generic scope given to the agent
     * @return the mock agent
     */
    public Agent mockedAgent(final String id, final String scope) {
        final Agent agent = createMock(Agent.class);
        expect(agent.getAgentId()).andReturn(id).anyTimes();
        expect(agent.getScope()).andReturn(scope).anyTimes();
        expect(agent.getScopeWithDefault()).andReturn("DEFAULT;" + scope).anyTimes();
        expect(agent.getApiVersion()).andReturn(API_V3_0).anyTimes();
        agent.executeBackupPostAction();
        expectLastCall();
        replay(agent);
        return agent;
    }

    /**
     * Generic method for creating an agent for executing restores pre or post action, with a specific api version
     * @param agentId Int ID given to agent
     * @param apiVersion The API version specified in the class that calls the method
     * @return the mock agent
     */
    public Agent mockedAgentExecutingBackup(final int agentId, final ApiVersion apiVersion, final RecognizedState agentState, final boolean failOnJobStage) {
        final Agent agent = createNiceMock(Agent.class);
        expect(agent.getAgentId()).andReturn(String.valueOf(agentId)).anyTimes();
        expect(agent.getApiVersion()).andReturn(apiVersion).anyTimes();

        if (failOnJobStage) {
            expect(agent.getState()).andReturn(agentState);
        }
        if (!apiVersion.equals(API_V2_0)) {
            agent.executeBackup();
        } else if(apiVersion.equals(API_V2_0)) {
            agent.prepareForBackup(anyObject());
        }
        expectLastCall();
        replay(agent);
        return agent;
    }

    /**
     * Generic method for creating an agent for executing backups pre or post action, with a specific api version
     * @param agentId Int ID given to agent
     * @param apiVersion The API version specified in the class that calls the method
     * @return the mock agent
     */
    public Agent mockedAgentBackupPostAction(final int agentId, final ApiVersion apiVersion, final boolean postActionFails) {
        final Agent agent = createNiceMock(Agent.class);
        expect(agent.getAgentId()).andReturn(String.valueOf(agentId)).anyTimes();
        expect(agent.getApiVersion()).andReturn(apiVersion).anyTimes();
        if (postActionFails) {
            expect(agent.getState()).andReturn(createMock(RecognizedState.class));
        }
        if (!apiVersion.equals(API_V2_0)) {
            agent.executeBackupPostAction();
            expectLastCall();
        }
        replay(agent);
        return agent;
    }

    /**
     * Generic method for creating an agent with an int ID and a boolean shouldCancelAction
     * @param agentId int ID given to the Agent
     * @param shouldCancelAction boolean for whether or not the agent fails
     * @return the mock agent
     */
    public Agent mockedAgent(final int agentId, final boolean shouldCancelAction, final RecognizedState agentState) {
        final Agent agent = createNiceMock(Agent.class);
        expect(agent.getApiVersion()).andReturn(ApiVersion.API_V3_0).anyTimes();
        expect(agent.getAgentId()).andReturn(String.valueOf(agentId)).anyTimes();

        expect(agent.getState()).andReturn(agentState).anyTimes();


        if(shouldCancelAction) {
            agent.cancelAction();
            expectLastCall();
        }
        replay(agent);
        return agent;
    }

    /**
     * Generic method for making agents with an int ID,a job to create backups and an API version
     * @param agentId Int ID given to the Agent
     * @param apiVersion API version specified in the class that calls the method
     * @param job Job for backup
     * @return the mock agent
     */
    public Agent mockedAgent(final int agentId, final CreateBackupJob job, final ApiVersion apiVersion) {
        final Agent agent = createNiceMock(Agent.class);
        expect(agent.getAgentId()).andReturn(String.valueOf(agentId)).anyTimes();
        expect(agent.getApiVersion()).andReturn(apiVersion).anyTimes();
        if(ApiVersion.API_V3_0.equals(apiVersion)) {
            //Agent V2_0 needs to fall through to Execute, so never calls prepare for backup in PreparingBackupJobStage.
            agent.prepareForBackup(job);
            expectLastCall();
        }
        replay(agent);
        return agent;
    }

    /**
     * Generic method for making agents with an int ID and a restore job
     * @param job Job for restore
     * @param agentId Int ID given to the agent
     * @return the mock agent
     */
    public Agent mockedAgent(final int agentId, final RestoreJob job, final RecognizedState agentStateWhenFail, final boolean failOnJobStage) {
        final Agent agent = createNiceMock(Agent.class);
        expect(agent.getAgentId()).andReturn(String.valueOf(agentId)).anyTimes();

        if (failOnJobStage) {
            expect(agent.getState()).andReturn(agentStateWhenFail).anyTimes();
        }
        agent.prepareForRestore(job);
        expectLastCall();
        replay(agent);
        return agent;
    }
}
