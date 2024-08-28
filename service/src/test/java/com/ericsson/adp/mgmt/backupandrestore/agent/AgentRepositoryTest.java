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

import static com.ericsson.adp.mgmt.backupandrestore.agent.VBRMAutoCreate.NONE;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.function.Consumer;

import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.agent.exception.AlreadyRegisteredAgentException;
import com.ericsson.adp.mgmt.backupandrestore.agent.exception.RegisteringAgentWithoutIdException;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;

public class AgentRepositoryTest {

    private BackupManagerRepository backupManagerRepository;
    private AgentRepository agentRepository;
    private MeterRegistry meterRegistry;
    private MockedAgentFactory agentMock;

    @Before
    public void setup() {
        backupManagerRepository = createMock(BackupManagerRepository.class);
        meterRegistry = createMeterRegistry();

        agentRepository = new AgentRepository();
        agentRepository.setBackupManagerRepository(backupManagerRepository);
        agentRepository.setMeterRegistry(meterRegistry);
        agentRepository.setVbrmAutoCreate(NONE);
        agentMock = new MockedAgentFactory();
    }

    @Test
    public void addAgent_newAgent_holdsAgentAndNotifiesBackupManagerRepository() throws Exception {
        final Agent agent = agentMock.mockedAgent("123");
        mockBackupManagerRepository(repository -> {
            backupManagerRepository.createBackupManager("alpha");
            expectLastCall();
        });

        agentRepository.addAgent(agent);

        assertEquals(1, agentRepository.getAgents().size());
        assertEquals(agent, agentRepository.getAgents().get(0));

        verify(backupManagerRepository);
    }

    @Test
    public void addAgent_agentWithMultipleScopes_holdsAgentAndNotifiesBackupManagerRepositoryOfEveryScope() throws Exception {
        final Agent agent = agentMock.mockedAgent("123", "a;b;c");

        mockBackupManagerRepository(repository -> {
            backupManagerRepository.createBackupManager("a");
            expectLastCall();
            backupManagerRepository.createBackupManager("b");
            expectLastCall();
            backupManagerRepository.createBackupManager("c");
            expectLastCall();
        });

        agentRepository.addAgent(agent);

        assertEquals(1, agentRepository.getAgents().size());
        assertEquals(agent, agentRepository.getAgents().get(0));

        verify(backupManagerRepository);
    }

    @Test
    public void metric_addsAndRemovesAgentAndChecksMetric() {
        final Agent agent = agentMock.mockedAgent("123", "a;b;c");

        agentRepository.addAgent(agent);

        var meter = meterRegistry
                .find("bro.registered.agents")
                .tag("agent", agent.getAgentId())
                .gauge();
        assertNotNull(meter);
        assertEquals(Double.valueOf(1.0), Double.valueOf(meter.value()));

        agentRepository.removeAgent(agent);
        meter = meterRegistry
                .find("bro.registered.agents")
                .tag("agent", agent.getAgentId())
                .gauge();
        assertNull(meter);
    }

    @Test
    public void getAgents_repositoryHoldingAgents_retrievesAllAgents() throws Exception {
        mockBackupManagerRepository(repository -> {
            backupManagerRepository.createBackupManager(anyObject());
            expectLastCall().anyTimes();
        });

        agentRepository.addAgent(agentMock.mockedAgent("123"));
        agentRepository.addAgent(agentMock.mockedAgent("456"));

        assertEquals(2, agentRepository.getAgents().size());
    }

    @Test
    public void removeAgent_alreadyExistingAgent_stopsHoldingAgent() throws Exception {
        mockBackupManagerRepository(repository -> {
            backupManagerRepository.createBackupManager(anyObject());
            expectLastCall().anyTimes();
        });

        final Agent agent = agentMock.mockedAgent("123");

        agentRepository.addAgent(agent);
        agentRepository.addAgent(agentMock.mockedAgent("456"));

        assertEquals(2, agentRepository.getAgents().size());

        agentRepository.removeAgent(agent);

        assertEquals(1, agentRepository.getAgents().size());
        assertNotEquals(agent, agentRepository.getAgents().get(0));
    }

    @Test
    public void getAgents_newAgent_isNotAbleToAddAgentToListUnlessGoingThroughAdddAgentMethod() throws Exception {
        agentRepository.getAgents().add(agentMock.mockedAgent("123"));

        assertTrue(agentRepository.getAgents().isEmpty());
    }

    @Test
    public void addAgent_holdsAgentWithIdAndTriesToAddAnotherAgentWithTheSameId_throwsException() throws Exception {
        mockBackupManagerRepository(repository -> {
            backupManagerRepository.createBackupManager(anyObject());
            expectLastCall();
        });

        agentRepository.addAgent(agentMock.mockedAgent("123"));

        assertEquals(1, agentRepository.getAgents().size());

        try {
            agentRepository.addAgent(agentMock.mockedAgent("123"));
            fail();
        } catch (final AlreadyRegisteredAgentException e) {
            verify(backupManagerRepository);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void addAgent_unrecognizedAgent_throwsException() throws Exception {
        mockBackupManagerRepository();

        final Agent agent = agentMock.mockedAgent();
        expect(agent.getAgentId()).andThrow(new UnsupportedOperationException());
        replay(agent);

        try {
            agentRepository.addAgent(agent);
            fail();
        } catch (final RegisteringAgentWithoutIdException e) {
            verify(backupManagerRepository);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void addAgent_agentWithoutId_throwsException() throws Exception {
        mockBackupManagerRepository();

        try {
            agentRepository.addAgent(agentMock.mockedAgent(""));
            fail();
        } catch (final RegisteringAgentWithoutIdException e) {
            verify(backupManagerRepository);
        } catch (final Exception e) {
            fail();
        }
    }

    private void mockBackupManagerRepository(final Consumer<BackupManagerRepository> actions) {
        actions.accept(backupManagerRepository);
        replay(backupManagerRepository);
    }

    private void mockBackupManagerRepository() {
        mockBackupManagerRepository(repository -> {});
    }

    private MeterRegistry createMeterRegistry(){
        final CollectorRegistry prometheusRegistry = new CollectorRegistry(true);
        final MockClock clock = new MockClock();
        final MeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, prometheusRegistry,clock);
        return meterRegistry;
    }

}
