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
package com.ericsson.adp.mgmt.backupandrestore.action;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.easymock.EasyMock;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.job.QueueingJobExecutor;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;

public class ActionServiceTest {

    @Test
    public void handleActionRequest_requestToCreateAction_createsActionAndPassesToJobExecutor() throws Exception {
        final CreateActionRequest request = new CreateActionRequest();

        final BackupManager backupManager = createMock(BackupManager.class);

        final BackupManagerRepository backupManagerRepository = createMock(BackupManagerRepository.class);
        expect(backupManagerRepository.getBackupManager("bm")).andReturn(backupManager);

        final Action action = createMock(Action.class);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();

        final ActionRepository actionRepository = createMock(ActionRepository.class);
        expect(actionRepository.createAction(backupManager, request)).andReturn(action);

        final QueueingJobExecutor jobExecutor = createMock(QueueingJobExecutor.class);
        jobExecutor.execute(backupManager, action);
        expectLastCall().once();

        replay(backupManager, backupManagerRepository, action, actionRepository, jobExecutor);

        final ActionService actionService = new ActionService();
        actionService.setBackupManagerRepository(backupManagerRepository);
        actionService.setActionRepository(actionRepository);
        actionService.setJobExecutor(jobExecutor);

        assertEquals(action, actionService.handleActionRequest("bm", request));

        EasyMock.verify(jobExecutor, backupManager, action, actionRepository);
    }

}
