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

package com.ericsson.adp.mgmt.backupandrestore.agent.state;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V2_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V4_0;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.agent.exception.InvalidRegistrationMessageException;
import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidIdException;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

/**
 * Represents unknown agent.
 */
public class UnrecognizedState implements AgentState {

    private static final Logger log = LogManager.getLogger(UnrecognizedState.class);

    private static final List<String> SUPPORTED_API_VERSIONS = Arrays.asList(API_V2_0.getStringRepresentation(),
            API_V3_0.getStringRepresentation(), API_V4_0.getStringRepresentation());

    private final IdValidator idValidator;

    /**
     * Creates state.
     * @param idValidator to validate agentId.
     */
    public UnrecognizedState(final IdValidator idValidator) {
        this.idValidator = idValidator;
    }

    /**
     * Process message from Agent
     * @param AgentControl message
     */
    @Override
    public AgentStateChange processMessage(final AgentControl message) {
        if (isRegistrationMessage(message)) {
            final Register register = message.getRegister();
            log.info("Registering agent <{}>", register);
            validateRegistrationMessage(register);
            return new AgentStateChange(new RecognizedState(register),
                (agent, agentRepository) -> {
                    agentRepository.addAgent(agent);
                });
        }
        log.info("Unexpected message {} received for unrecognized agent", message);
        return new AgentStateChange(this);
    }

    @Override
    public String getAgentId() {
        log.error("Trying to get agentId from unrecognized agent");
        throw new UnsupportedOperationException();
    }

    @Override
    public ApiVersion getApiVersion() {
        log.error("Trying to get API version from unrecognized agent");
        throw new UnsupportedOperationException();
    }

    @Override
    public String getScope() {
        log.error("Trying to get scope from unrecognized agent");
        throw new UnsupportedOperationException();
    }

    @Override
    public SoftwareVersion getSoftwareVersion() {
        log.error("Trying to get software version from unrecognized agent");
        throw new UnsupportedOperationException();
    }

    @Override
    public AgentStateChange cancelAction(final AgentInputStream inputStream) {
        return new AgentStateChange(this);
    }

    @Override
    public RecognizedState resetState() {
        throw new UnsupportedOperationException("Unable to reset state because Agent <" + getAgentId() +
                "> is in state " + getClass().getSimpleName());
    }

    private void validateRegistrationMessage(final Register registrationMessage) {
        final StringBuilder errorMessage = new StringBuilder();

        if (!SUPPORTED_API_VERSIONS.contains(registrationMessage.getApiVersion())) {
            errorMessage.append("Invalid api version. Only " + SUPPORTED_API_VERSIONS + " are supported.\n");
        }

        try {
            idValidator.validateId(registrationMessage.getAgentId());
        } catch (final InvalidIdException e) {
            errorMessage.append(e.getMessage() + "\n");
        }

        if (hasInvalidSoftwareVersion(registrationMessage.getSoftwareVersionInfo())) {
            errorMessage.append("Software version information is invalid.\n");
        }

        if (!errorMessage.toString().isEmpty()) {
            throw new InvalidRegistrationMessageException(errorMessage.toString());
        }
    }

    private boolean hasInvalidSoftwareVersion(final SoftwareVersionInfo softwareVersion) {
        return softwareVersion.getProductName().isEmpty() ||
                softwareVersion.getProductNumber().isEmpty() ||
                softwareVersion.getRevision().isEmpty() ||
                softwareVersion.getProductionDate().isEmpty() ||
                softwareVersion.getDescription().isEmpty() ||
                softwareVersion.getType().isEmpty();
    }

    private boolean isRegistrationMessage(final AgentControl message) {
        return Action.REGISTER.equals(message.getAction()) &&
                AgentMessageType.REGISTER.equals(message.getAgentMessageType());
    }
}
