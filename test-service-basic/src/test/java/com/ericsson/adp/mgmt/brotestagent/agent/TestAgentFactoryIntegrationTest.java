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

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ericsson.adp.mgmt.bro.api.agent.Agent;
import com.ericsson.adp.mgmt.brotestagent.test.GrpcServiceIntegrationTest;
import com.ericsson.adp.mgmt.brotestagent.util.PropertiesHelper;

import io.grpc.BindableService;

public class TestAgentFactoryIntegrationTest extends GrpcServiceIntegrationTest {

    @Test
    public void createTestAgent_orchestratorInformationAsProperties_createsTestAgent() throws Exception {
        PropertiesHelper.loadProperties("src/test/resources/application.properties");
        final Agent agent = TestAgentFactory.createTestAgent();
        assertNotNull(agent);
    }

    @Override
    protected List<BindableService> getServices() {
        return new ArrayList<>();
    }

}
