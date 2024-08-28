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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;

public class ExecutingRestoreStateTest {

    private ExecutingRestoreState state;
    private RestoreJob job;

    @Before
    public void setup() {
        this.job = createMock(RestoreJob.class);
        this.state = new ExecutingRestoreState(null, this.job);
    }

    @Test
    public void executeRestorePostAction_executingRestoreState_movesToPostActionStateAndTriggersPostAction() throws Exception {
        final AgentInputStream inputStream = createMock(AgentInputStream.class);
        inputStream.executeRestorePostAction();
        expectLastCall();
        replay(job, inputStream);

        final AgentStateChange stateChange = state.executeRestorePostAction(inputStream);
        stateChange.postAction(null, null);

        assertTrue(stateChange.getNextState() instanceof PostActionRestoreState);
        verify(inputStream);
    }

}
