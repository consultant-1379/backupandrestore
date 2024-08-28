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
package com.ericsson.adp.mgmt.backupandrestore.agent.state;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentRepository;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Represents a change of state, with the possibility of executing an action after changing state.
 */
public class AgentStateChange {

    private final AgentState nextState;
    @SuppressWarnings("PMD.ImmutableField")
    private Optional<BiConsumer<Agent, AgentRepository>> postAction = Optional.empty();

    /**
     * Holds next state.
     * @param nextState next state.
     */
    public AgentStateChange(final AgentState nextState) {
        this.nextState = nextState;
    }

    /**
     * Holds next state and action to be executed after state changes.
     * @param nextState next state.
     * @param postAction action to be executed after state changes.
     */
    public AgentStateChange(final AgentState nextState, final Runnable postAction) {
        this(nextState);
        this.postAction = Optional.ofNullable((agent, agentRepository) -> postAction.run());
    }

    /**
     * Holds next state and action to be executed after state changes.
     * @param nextState next state.
     * @param postAction action to be executed after state changes.
     */
    public AgentStateChange(final AgentState nextState, final BiConsumer<Agent, AgentRepository> postAction) {
        this(nextState);
        this.postAction = Optional.ofNullable(postAction);
    }

    public AgentState getNextState() {
        return nextState;
    }

    /**
     * Execute action after state changes.
     * @param agent agent whose state has changed.
     * @param agentRepository access to agentRepository.
     */
    public void postAction(final Agent agent, final AgentRepository agentRepository) {
        postAction.ifPresent(action -> action.accept(agent, agentRepository));
    }

    @Override
    public String toString() {
        return "AgentStateChange{" +
            "nextState=" + nextState +
            ", postAction=" + postAction +
            '}';
    }
}
