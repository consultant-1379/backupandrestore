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

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Test;

public class AgentStateChangeTest {

    @Test
    public void new_futureState_holdsFutureState() throws Exception {
        final AgentState state = EasyMock.createMock(AgentState.class);

        final AgentStateChange stateChange = new AgentStateChange(state);

        assertEquals(state, stateChange.getNextState());
    }

    @Test
    public void postAction_anyActionToBeExecuted_executesAction() throws Exception {
        final AgentState state = EasyMock.createMock(AgentState.class);

        final List<String> list = new ArrayList<>();
        final AgentStateChange stateChange = new AgentStateChange(state, (a, b) -> list.add("I'm a new element"));

        stateChange.postAction(null, null);

        assertEquals("I'm a new element", list.get(0));
    }

    @Test
    public void postAction_noPostActionIsSet_nothingHappens() throws Exception {
        final AgentState state = EasyMock.createMock(AgentState.class);

        final AgentStateChange stateChange = new AgentStateChange(state);

        stateChange.postAction(null, null);

        assertEquals(state, stateChange.getNextState());
    }

}
