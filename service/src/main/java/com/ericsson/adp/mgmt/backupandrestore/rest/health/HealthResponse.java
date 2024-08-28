/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.backupandrestore.rest.health;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Encapsulates the health of the Backup and Restore Orchestrator.
 */
public class HealthResponse {

    private static final String STATUS = "Healthy";

    @JsonView(value = {HealthResponseView.V1.class})
    private String availability;

    /**
     * The progressUrl is not included in the response
     * when there is no on-going action
     */
    @JsonView(value = {HealthResponseView.V1.class})
    private Map<String, String> ongoingAction;

    @JsonView(value = {HealthResponseView.V1.class})
    private List<String> registeredAgents = new ArrayList<>();

    @JsonIgnore
    private final Optional<Action> action;

    /**
     * Creates new response object.
     */
    public HealthResponse() {
        action = Optional.empty();
    }

    /**
     * Creates new response object for when BRO is not running an {@link
     * com.ericsson.adp.mgmt.backupandrestore.action.Action}.
     *
     * @param registeredAgents ids of registered agents.
     */
    public HealthResponse(final List<String> registeredAgents) {
        action = Optional.empty();
        this.availability = AvailabilityState.AVAILABLE.toString();
        ongoingAction = new HashMap<>();
        this.registeredAgents.addAll(registeredAgents);
    }

    /**
     * Creates new response object for when BRO is running an {@link
     * com.ericsson.adp.mgmt.backupandrestore.action.Action}.
     *
     * @param action id of the currently running action.
     * @param backupManagerId id of the backup managers, if any.
     * @param registeredAgents ids of registered agents.
     */
    public HealthResponse(final Action action, final String backupManagerId,
                          final List<String> registeredAgents) {
        this.availability = AvailabilityState.BUSY.toString();
        ongoingAction = new HashMap<>();
        this.action = Optional.of(action);
        ongoingAction.put("actionId", action.getActionId());
        ongoingAction.put("backupManagerId", backupManagerId);
        this.registeredAgents.addAll(registeredAgents);
    }

    @JsonView(value = {HealthResponseView.V4.class, HealthResponseView.V1.class})
    public String getStatus() {
        return STATUS;
    }

    public String getAvailability() {
        return availability;
    }

    public Map<String, String> getOngoingAction() {
        return ongoingAction;
    }

    public List<String> getRegisteredAgents() {
        return registeredAgents;
    }

    public Optional<Action> getAction() {
        return this.action;
    }

    public void setRegisteredAgents(final List<String> registeredAgents) {
        this.registeredAgents = registeredAgents;
    }
}
