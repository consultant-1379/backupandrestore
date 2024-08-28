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

import static com.ericsson.adp.mgmt.action.Action.REGISTER;

import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.control.Register.Builder;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

/**
 * Converts RegistrationInformation to GRPC message.
 */
public class RegistrationMessageFactory {


    private RegistrationMessageFactory() {}

    /**
     * Converts RegistrationInformation to GRPC message.
     * @param registrationInformation to be converted.
     * @return an AgentControl message containing the registration message
     */
    public static AgentControl getRegistrationMessage(final RegistrationInformation registrationInformation) {
        final Register registerMessage = buildRegistrationMessage(registrationInformation);

        return AgentControl
                .newBuilder()
                .setAction(REGISTER)
                .setAgentMessageType(AgentMessageType.REGISTER)
                .setRegister(registerMessage)
                .build();
    }

    private static Register buildRegistrationMessage(final RegistrationInformation registrationInformation) {
        final Builder registerBuilder = Register
                .newBuilder()
                .setAgentId(registrationInformation.getAgentId())
                .setSoftwareVersionInfo(getSoftwareVersionInfo(registrationInformation.getSoftwareVersion()))
                .setApiVersion(registrationInformation.getApiVersion())
                .setScope(registrationInformation.getScope());

        if (registrationInformation.getApiVersion().equals("4.0")) {
            return registerBuilder
                    .setBackendType(registrationInformation.getBackendType())
                    .addAllAgentFeature(registrationInformation.getAgentFeatures())
                    .build();
        } else {
            return registerBuilder.build();
        }
    }

    private static SoftwareVersionInfo getSoftwareVersionInfo(final SoftwareVersion softwareVersion) {
        return SoftwareVersionInfo
                .newBuilder()
                .setProductName(softwareVersion.getProductName())
                .setProductNumber(softwareVersion.getProductNumber())
                .setRevision(softwareVersion.getRevision())
                .setProductionDate(softwareVersion.getProductionDate())
                .setDescription(softwareVersion.getDescription())
                .setType(softwareVersion.getType())
                .setCommercialVersion(softwareVersion.getCommercialVersion())
                .setSemanticVersion(softwareVersion.getSemanticVersion())
                .build();
    }

}
