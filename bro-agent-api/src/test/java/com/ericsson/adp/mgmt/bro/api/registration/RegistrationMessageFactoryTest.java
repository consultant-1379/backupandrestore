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
package com.ericsson.adp.mgmt.bro.api.registration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.bro.api.test.RegistrationInformationUtil;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.BackendType;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

public class RegistrationMessageFactoryTest {

    @Test
    public void getRegistrationMessage_getRegistrationInformation_getRegistrationMessage() {
        final RegistrationInformation registrationInfo = RegistrationInformationUtil.getTestRegistrationInformation();

        final AgentControl agentControl = RegistrationMessageFactory.getRegistrationMessage(registrationInfo);
        assertEquals(Action.REGISTER, agentControl.getAction());
        assertEquals(AgentMessageType.REGISTER, agentControl.getAgentMessageType());
        assertTrue(agentControl.hasRegister());

        final Register register = agentControl.getRegister();
        assertEquals("123", register.getAgentId());
        assertEquals("4.0", register.getApiVersion());
        assertEquals("scope", register.getScope());

        final SoftwareVersionInfo softwareVersionInfo = register.getSoftwareVersionInfo();
        assertEquals("description", softwareVersionInfo.getDescription());
        assertEquals("productionDate", softwareVersionInfo.getProductionDate());
        assertEquals("productName", softwareVersionInfo.getProductName());
        assertEquals("productNumber", softwareVersionInfo.getProductNumber());
        assertEquals("type", softwareVersionInfo.getType());
        assertEquals("revision", softwareVersionInfo.getRevision());
        assertEquals("semanticVersion", softwareVersionInfo.getSemanticVersion());
        assertEquals("commercialVersion", softwareVersionInfo.getCommercialVersion());
    }

    @Test
    public void getRegistrationMessage_getRegistrationInformationUsingConstructor_getRegistrationMessage() {
        final BackendType backendType = BackendType.OBJECT_STORAGE;
        final RegistrationInformation registrationInfo = RegistrationInformationUtil.getTestRegistrationInformationUsingConstructors(backendType);

        final AgentControl agentControl = RegistrationMessageFactory.getRegistrationMessage(registrationInfo);
        assertEquals(Action.REGISTER, agentControl.getAction());
        assertEquals(AgentMessageType.REGISTER, agentControl.getAgentMessageType());
        assertTrue(agentControl.hasRegister());

        final Register register = agentControl.getRegister();
        assertEquals("123", register.getAgentId());
        assertEquals("4.0", register.getApiVersion());
        assertEquals("scope", register.getScope());
        assertEquals(backendType, register.getBackendType());

        final SoftwareVersionInfo softwareVersionInfo = register.getSoftwareVersionInfo();
        assertEquals("description", softwareVersionInfo.getDescription());
        assertEquals("productionDate", softwareVersionInfo.getProductionDate());
        assertEquals("productName", softwareVersionInfo.getProductName());
        assertEquals("productNumber", softwareVersionInfo.getProductNumber());
        assertEquals("type", softwareVersionInfo.getType());
        assertEquals("revision", softwareVersionInfo.getRevision());
        assertEquals("semanticVersion", softwareVersionInfo.getSemanticVersion());
        assertEquals("commercialVersion", softwareVersionInfo.getCommercialVersion());
    }

    @Test
    public void getRegistrationMessage_getRegistrationInformationUsingConstructorWithDefaultBackendType_getRegistrationMessage() {
        final RegistrationInformation registrationInfo = RegistrationInformationUtil.getTestRegistrationInformationUsingWithDefaultBackendTypeAndEmptyFeatureList();

        final AgentControl agentControl = RegistrationMessageFactory.getRegistrationMessage(registrationInfo);
        assertEquals(Action.REGISTER, agentControl.getAction());
        assertEquals(AgentMessageType.REGISTER, agentControl.getAgentMessageType());
        assertTrue(agentControl.hasRegister());

        final Register register = agentControl.getRegister();
        assertEquals("123", register.getAgentId());
        assertEquals("4.0", register.getApiVersion());
        assertEquals("scope", register.getScope());
        assertTrue(register.getAgentFeatureList().isEmpty());
        assertEquals(BackendType.BRO, register.getBackendType());

        final SoftwareVersionInfo softwareVersionInfo = register.getSoftwareVersionInfo();
        assertEquals("description", softwareVersionInfo.getDescription());
        assertEquals("productionDate", softwareVersionInfo.getProductionDate());
        assertEquals("productName", softwareVersionInfo.getProductName());
        assertEquals("productNumber", softwareVersionInfo.getProductNumber());
        assertEquals("type", softwareVersionInfo.getType());
        assertEquals("revision", softwareVersionInfo.getRevision());
        assertEquals("semanticVersion", softwareVersionInfo.getSemanticVersion());
        assertEquals("commercialVersion", softwareVersionInfo.getCommercialVersion());
    }

}
