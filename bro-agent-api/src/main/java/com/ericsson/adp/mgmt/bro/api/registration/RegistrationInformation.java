/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.adp.mgmt.bro.api.registration;

import com.ericsson.adp.mgmt.bro.api.exception.InvalidRegistrationInformationException;
import com.ericsson.adp.mgmt.bro.api.exception.InvalidRegistrationInformationException.ErrorMessage;

import java.util.ArrayList;
import java.util.List;

import com.ericsson.adp.mgmt.metadata.AgentFeature;
import com.ericsson.adp.mgmt.control.BackendType;

/**
 * Holds agent's registration information.
 */
public class RegistrationInformation {

    private String agentId = "";
    private String scope = "";
    private String apiVersion = "";
    private SoftwareVersion softwareVersion;
    private List<AgentFeature> agentFeatures = new ArrayList<>();
    private BackendType backendType = BackendType.BRO;

    /**
     * Provides a constructor that can be used to create a registration information object. This
     * information is sent to the orchestrator during registration.
     *
     * @param agentId The id of the agent
     * @param scope The scope this agent will be a part of. Scope here refers to the backup type in
     * the Ericsson BRM.
     * @param apiVersion The version of the bro api in use
     * @param softwareVersion The software version information to register with
     */
    public RegistrationInformation(final String agentId, final String scope,
                                   final String apiVersion, final SoftwareVersion softwareVersion) {
        this.agentId = agentId;
        this.scope = scope;
        this.apiVersion = apiVersion;
        this.softwareVersion = softwareVersion;
    }

    /**
     * Provides a constructor that can be used to create a registration information object. This
     * information is sent to the orchestrator during registration.
     *
     * @param agentId The id of the agent
     * @param scope The scope this agent will be a part of. Scope here refers to the backup type in
     * the Ericsson BRM.
     * @param apiVersion The version of the bro api in use
     * @param softwareVersion The software version information to register with
     * @param agentFeatures indicates the feature set of the agent
     * @param backendType where the fragments will be stored
     */
    public RegistrationInformation(final String agentId, final String scope,
                                   final String apiVersion, final SoftwareVersion softwareVersion,
                                   final List<AgentFeature> agentFeatures, final BackendType backendType) {
        this(agentId, scope, apiVersion, softwareVersion);
        this.agentFeatures = agentFeatures;
        this.backendType = backendType;
    }

    /**
     * Provides a constructor that can be used to create a registration information object. This
     * information is sent to the orchestrator during registration. If this constructor is called
     * then the following must be set
     *
     * agentId, scope, apiVersion, softwareVersion
     */
    public RegistrationInformation() {
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(final String agentId) {
        this.agentId = agentId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(final String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public SoftwareVersion getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(final SoftwareVersion softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public List<AgentFeature> getAgentFeatures() {
        return agentFeatures;
    }

    public void setAgentFeatures(final List<AgentFeature> agentFeatures) {
        this.agentFeatures = agentFeatures;
    }

    public BackendType getBackendType() {
        return backendType;
    }

    public void setBackendType(final BackendType backendType) {
        this.backendType = backendType;
    }

    private void validateSoftwareVersion() {
        validateField(softwareVersion, ErrorMessage.BLANK, ErrorMessage.SOFTWARE_VERSION_IS_NULL);
        softwareVersion.validate();
    }

    /**
     * @throws InvalidRegistrationInformationException if any field is invalid.
     */
    public void validate() {
        validateField(agentId, ErrorMessage.AGENT_ID_IS_BLANK, ErrorMessage.AGENT_ID_IS_NULL);
        validateField(apiVersion, ErrorMessage.BLANK, ErrorMessage.API_VERSION_IS_NULL);
        validateFieldNull(scope, ErrorMessage.SCOPE_IS_NULL);
        validateSoftwareVersion();
    }

    private static <T> void validateFieldNull(final T field,
                                          final ErrorMessage errorMessageNull) {
        if (field == null) {
            throw new InvalidRegistrationInformationException(errorMessageNull);
        }
    }

    private static <T> void validateField(final T field, final ErrorMessage errorMessageBlank,
                                          final ErrorMessage errorMessageNull) {
        validateFieldNull(field, errorMessageNull);
        if (field instanceof String && ((String) field).isEmpty()) {
            throw new InvalidRegistrationInformationException(errorMessageBlank);
        }
    }

    @Override
    public String toString() {
        return "RegistrationInformation{" +
                "agentId='" + agentId + '\'' +
                ", scope='" + scope + '\'' +
                ", apiVersion='" + apiVersion + '\'' +
                ", softwareVersion=" + softwareVersion + '\'' +
                ", agentFeatures=" + agentFeatures + '\'' +
                ", backendType=" + backendType +
                '}';
    }
}
