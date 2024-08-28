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
package com.ericsson.adp.mgmt.brotestagent.agent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.bro.api.agent.Agent;
import com.ericsson.adp.mgmt.bro.api.agent.AgentBehavior;
import com.ericsson.adp.mgmt.bro.api.agent.AgentFactory;
import com.ericsson.adp.mgmt.bro.api.agent.OrchestratorConnectionInformation;
import com.ericsson.adp.mgmt.brotestagent.agent.behavior.TestAgentBehavior;
import com.ericsson.adp.mgmt.brotestagent.util.PropertiesHelper;

/**
 * Responsible for creating test agent.
 */
public class TestAgentFactory {

    private static final Logger log = LogManager.getLogger(TestAgentFactory.class);
    private static final String ORCHESTRATOR_PORT_PROPERTY = "orchestrator.port";
    private static final String ORCHESTRATOR_HOST_PROPERTY = "orchestrator.host";
    private static final String DEFAULT_ORCHESTRATOR_HOST = "127.0.0.1";
    private static final String DEFAULT_ORCHESTRATOR_PORT = "3000";
    private static final String AGENT_BEHAVIOR_CLASS_PROPERTY = "test.agent.agentBehavior";
    private static final String DEFAULT_AGENT_BEHAVIOR_CLASS = "com.ericsson.adp.mgmt.brotestagent.agent.behavior.TestAgentBehavior";
    private static final String CERTIFICATE_AUTHORITY_NAME_PROPERTY = "siptls.ca.name";
    private static final String CERTIFICATE_AUTHORITY_PATH_PROPERTY = "siptls.ca.path";
    private static final String CLIENT_CERTIFICATE_CHAIN_FILE_PATH_PROPERTY = "siptls.client.certificateChainFilePath";
    private static final String CLIENT_PRIVATE_KEY_FILE_PATH_PROPERTY = "siptls.client.privateKeyFilePath";
    private static final String FLAG_GLOBAL_SECURITY_PROPERTY = "flag.global.security";
    private static final String DEFAULT_CERTIFICATE_AUTHORITY_NAME = "foo.test.google.fr";
    private static final String DEFAULT_CERTIFICATE_AUTHORITY_PATH = "src/test/resources/ExampleCertificates/ca.pem";
    private static final String DEFAULT_CLIENT_CERTIFICATE_CHAIN_FILE_PATH = null;
    private static final String DEFAULT_PRIVATE_KEY_FILE_PATH = null;
    private static final String DEFAULT_FLAG_GLOBAL_SECURITY = "true";
    private static final String MAX_INBOUND_MESSAGE_SIZE_PROPERTY = "test.agent.maxInboundMessageSize";
    private static final String MAX_RETRIES_WAITING_ACK = "test.agent.maxRetriesToReceiveACK";
    private static final String DEFAULT_MAX_INBOUND_MESSAGE_SIZE = "4194304";
    private static final String DEFAULT_NUMBER_SECONDS_TO_WAIT_FOR_ACK = "10";

    /**
     * Prevents external instantiation.
     */
    private TestAgentFactory() {
    }

    /**
     * Creates test agent.
     *
     * @return test agent.
     */

    public static Agent createTestAgent() {
        OrchestratorConnectionInformation orchestratorConnectionInformation;
        if (getGlobalSecurityEnabled().equalsIgnoreCase("true")) {
            orchestratorConnectionInformation = new OrchestratorConnectionInformation(
                    getOrchestratorHost(),
                    getOrchestratorPort(),
                    getCertificateAuthorityName(),
                    getCertificateAuthorityPath(),
                    getCertificateChainFilePath(),
                    getPrivateKeyFilePath(),
                    getMaxInboundMessageSize(),
                    getNumberSecondsToWaitForACK());
        } else {
            orchestratorConnectionInformation = new OrchestratorConnectionInformation(
                    getOrchestratorHost(),
                    getOrchestratorPort(),
                    getMaxInboundMessageSize(),
                    getNumberSecondsToWaitForACK());
        }
        return AgentFactory.createAgent(orchestratorConnectionInformation, getAgentBehavior());
    }

    private static AgentBehavior getAgentBehavior() {
        final String agentBehavior = PropertiesHelper.getProperty(AGENT_BEHAVIOR_CLASS_PROPERTY, DEFAULT_AGENT_BEHAVIOR_CLASS);
        if (!agentBehavior.isEmpty()) {
            try {
                return (AgentBehavior) Class.forName(agentBehavior).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                log.info("Testing class <{}> not found - switching to DEFAULT", agentBehavior);
            }
        }
        return new TestAgentBehavior();
    }

    private static String getOrchestratorHost() {
        return PropertiesHelper.getProperty(ORCHESTRATOR_HOST_PROPERTY, DEFAULT_ORCHESTRATOR_HOST);
    }

    private static Integer getOrchestratorPort() {
        return Integer.valueOf(PropertiesHelper.getProperty(ORCHESTRATOR_PORT_PROPERTY, DEFAULT_ORCHESTRATOR_PORT));
    }

    private static String getCertificateAuthorityName() {
        return PropertiesHelper.getProperty(CERTIFICATE_AUTHORITY_NAME_PROPERTY, DEFAULT_CERTIFICATE_AUTHORITY_NAME);
    }

    private static String getCertificateAuthorityPath() {
        return PropertiesHelper.getProperty(CERTIFICATE_AUTHORITY_PATH_PROPERTY, DEFAULT_CERTIFICATE_AUTHORITY_PATH);
    }

    private static String getGlobalSecurityEnabled() {
        return PropertiesHelper.getProperty(FLAG_GLOBAL_SECURITY_PROPERTY, DEFAULT_FLAG_GLOBAL_SECURITY);
    }

    private static String getCertificateChainFilePath() {
        return PropertiesHelper.getProperty(CLIENT_CERTIFICATE_CHAIN_FILE_PATH_PROPERTY, DEFAULT_CLIENT_CERTIFICATE_CHAIN_FILE_PATH);
    }

    private static String getPrivateKeyFilePath() {
        return PropertiesHelper.getProperty(CLIENT_PRIVATE_KEY_FILE_PATH_PROPERTY, DEFAULT_PRIVATE_KEY_FILE_PATH);
    }

    private static int getMaxInboundMessageSize() {
        return Integer.parseInt(PropertiesHelper.getProperty(MAX_INBOUND_MESSAGE_SIZE_PROPERTY, DEFAULT_MAX_INBOUND_MESSAGE_SIZE));
    }

    private static int getNumberSecondsToWaitForACK() {
        return Integer.parseInt(PropertiesHelper.getProperty(MAX_RETRIES_WAITING_ACK, DEFAULT_NUMBER_SECONDS_TO_WAIT_FOR_ACK));
    }
}
