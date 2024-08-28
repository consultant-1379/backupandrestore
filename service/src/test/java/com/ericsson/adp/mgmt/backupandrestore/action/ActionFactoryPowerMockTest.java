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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;

import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.ESAPIValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ ActionFactory.class, Random.class })
public class ActionFactoryPowerMockTest {

    private ActionFactory factory;

    @Before
    public void setup() throws Exception {
        final Random random = createMock(Random.class);
        expect(random.nextInt(65536)).andReturn(5);
        expect(random.nextInt(65536)).andReturn(5);
        expect(random.nextInt(65536)).andReturn(5);
        expect(random.nextInt(65536)).andReturn(5);
        expect(random.nextInt(65536)).andReturn(5);
        expect(random.nextInt(65536)).andReturn(10);

        PowerMock.expectNew(Random.class).andReturn(random);
        PowerMock.replay(Random.class);
        replay(random);
        factory = new ActionFactory();
        factory.setActionRepository(createNiceMock(ActionRepository.class));
        factory.setEsapiValidator(createNiceMock(ESAPIValidator.class));
        factory.setIdValidator(createNiceMock(IdValidator.class));
    }

    @Test
    public void createAction_backupManagerAlreadyHasActions_generatesNewActionId() throws Exception {
        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName("myBackup");
        final CreateActionRequest request = new CreateActionRequest();
        request.setAction(ActionType.CREATE_BACKUP);
        request.setPayload(payload);
        final BackupManager backupManager = mockBackupManager("123");

        final Action action = factory.createAction(backupManager, request);
        assertEquals("10", action.getActionId());
    }

    private BackupManager mockBackupManager(final String backupManagerId) {
        final Action action = createMock(Action.class);
        expect(action.getActionId()).andReturn("5").anyTimes();

        final BackupManager backupManager = createMock(BackupManager.class);
        final Backup backup = createMock(Backup.class);
        expect(backupManager.getBackupManagerId()).andReturn(backupManagerId).anyTimes();
        expect(backupManager.getBackup(anyString(), anyObject(Ownership.class))).andReturn(backup).anyTimes();
        expect(backupManager.getActions()).andReturn(Arrays.asList(action)).anyTimes();
        replay(action, backupManager);
        return backupManager;
    }

}
