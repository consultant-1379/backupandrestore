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
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;

public class ActionRepositoryTest {

    private ActionRepository repository;
    private ActionFactory actionFactory;
    private ActionFileService actionFileService;
    private CMMediatorService cmMediatorService;
    private NotificationService notificationService;
    private BackupManagerRepository backupManagerRepository;

    @Before
    public void setup() {
        repository = new ActionRepository();
        actionFactory = EasyMock.createMock(ActionFactory.class);
        actionFileService = EasyMock.createMock(ActionFileService.class);
        cmMediatorService = EasyMock.createMock(CMMediatorService.class);
        notificationService = EasyMock.createMock(NotificationService.class);
        backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);

        repository.setActionFactory(actionFactory);
        repository.setActionFileService(actionFileService);
        repository.setCmMediatorService(cmMediatorService);
        repository.setNotificationService(notificationService);
        repository.setBackupManagerRepository(backupManagerRepository);
    }

    @Test
    public void createAction_requestToCreateActionOnABackupManager_createsAndPersistsAction() throws Exception {
        final CreateActionRequest request = EasyMock.createMock(CreateActionRequest.class);
        final Action action = EasyMock.createMock(Action.class);
        EasyMock.expect(action.isRestoreOrExport()).andReturn(false);
        EasyMock.expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        EasyMock.expect(action.isPartOfHousekeeping()).andReturn(false);

        final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
        EasyMock.expect(actionFactory.createAction(backupManager, request)).andReturn(action);

        EasyMock.replay(action, backupManager, actionFactory, actionFileService, cmMediatorService);

        assertEquals(action, repository.createAction(backupManager, request));


        EasyMock.verify(backupManager, actionFileService, cmMediatorService);
    }

    @Test
    public void performCleanup_checkForTimestamp() throws Exception {
        final Action oldAction = EasyMock.createMock(Action.class);
        EasyMock.expect(oldAction.getStartTime()).andReturn(OffsetDateTime.now().minusSeconds(30)).anyTimes();
        EasyMock.expect(oldAction.getBackupManagerId()).andReturn("test").times(2);

        final Action newAction = EasyMock.createMock(Action.class);
        EasyMock.expect(newAction.getStartTime()).andReturn(OffsetDateTime.now()).anyTimes();

        final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
        EasyMock.expect(backupManager.getActions()).andReturn(List.of(oldAction, newAction)).once();
        EasyMock.expect(backupManagerRepository.getBackupManagers()).andReturn(List.of(backupManager)).once();
        EasyMock.expect(backupManagerRepository.getBackupManager("test")).andReturn(backupManager).once();

        backupManager.removeAction(anyObject(Action.class));
        expectLastCall().once();

        actionFileService.performCleanup(anyObject(Action.class));
        expectLastCall().andAnswer(() -> {
            assertEquals("test", ((Action)getCurrentArguments()[0]).getBackupManagerId());
            return null;
        }).once();
        EasyMock.expect(oldAction.getAdditionalInfo()).andReturn("");

        actionFileService.createDummyFile();
        expectLastCall().once();

        EasyMock.replay(backupManager, oldAction, newAction, backupManagerRepository, actionFileService);
        repository.setActionFileService(actionFileService);
        repository.setMaxActions(1);
        repository.performActionCleanup();
        EasyMock.verify(backupManager, oldAction, newAction, backupManagerRepository, actionFileService);
        repository.setMaxActions(100);
    }

    @Test
    public void getActions_backupManagerId_getsAllActionsForBackupManagerOrderedByStartTime() throws Exception {
        EasyMock.
        expect(actionFileService.getActions("1"))
        .andReturn(Arrays.asList(getPersistedAction("1", ActionStateType.FINISHED, ActionType.CREATE_BACKUP, getDateTime(2019, 1, 1, 1, 1)), getPersistedAction("2", ActionStateType.FINISHED, ActionType.RESTORE, getDateTime(2019, 1, 1, 0, 1))));
        EasyMock.replay(actionFileService, notificationService);

        final List<Action> actions = repository.getActions("1");

        assertEquals(2, actions.size());
        assertEquals("2", actions.get(0).getActionId());
        assertEquals(ActionStateType.FINISHED, actions.get(0).getState());
        assertEquals("1", actions.get(1).getActionId());
        assertEquals(ActionStateType.FINISHED, actions.get(1).getState());
        EasyMock.verify(notificationService);
    }

    @Test
    public void getActions_somePersistedActionsAreRunning_marksActionsAsFinishedAndSendsNotificationOnlyIfRestoreOrBackup() throws Exception {
        EasyMock
        .expect(actionFileService.getActions("1"))
        .andReturn(Arrays.asList(
                getPersistedAction("1", ActionStateType.RUNNING, ActionType.CREATE_BACKUP),
                getPersistedAction("2", ActionStateType.RUNNING, ActionType.RESTORE),
                getPersistedAction("3", ActionStateType.RUNNING, ActionType.IMPORT),
                getPersistedAction("4", ActionStateType.FINISHED, ActionType.RESTORE),
                getPersistedAction("5", ActionStateType.RUNNING, ActionType.HOUSEKEEPING)
                ));

        final Capture<Action> firstActionSentForNotification = Capture.newInstance();
        final Capture<Action> secondActionSentForNotification = Capture.newInstance();

        notificationService.notifyAllActionFailed(EasyMock.capture(firstActionSentForNotification));
        expectLastCall();
        notificationService.notifyAllActionFailed(EasyMock.capture(secondActionSentForNotification));
        expectLastCall();
        actionFileService.writeToFile(anyObject());
        expectLastCall().times(4);
        EasyMock.replay(actionFileService, notificationService);

        final List<Action> actions = repository.getActions("1");
        actions.stream().forEach(action -> repository.failActionIfRunning(action));

        assertEquals(5, actions.size());
        assertEquals("1", actions.get(0).getActionId());
        assertEquals(ActionStateType.FINISHED, actions.get(0).getState());
        assertEquals("2", actions.get(1).getActionId());
        assertEquals(ActionStateType.FINISHED, actions.get(1).getState());
        assertEquals("3", actions.get(2).getActionId());
        assertEquals(ActionStateType.FINISHED, actions.get(2).getState());
        assertEquals("4", actions.get(3).getActionId());
        assertEquals(ActionStateType.FINISHED, actions.get(3).getState());
        assertEquals("5", actions.get(4).getActionId());
        assertEquals(ActionStateType.FINISHED, actions.get(4).getState());

        EasyMock.verify(actionFileService);

        assertEquals("1", firstActionSentForNotification.getValue().getActionId());
        assertEquals("2", secondActionSentForNotification.getValue().getActionId());
    }

    @Test
    public void persist_anyActionButHousekeepingAndRestore_persistsItToFileAndCM() throws Exception {
        final Action action = EasyMock.createMock(Action.class);
        EasyMock.expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        EasyMock.expect(action.isPartOfHousekeeping()).andReturn(false);
        actionFileService.writeToFile(action);
        expectLastCall();
        final CountDownLatch latch = new CountDownLatch(1);
        cmMediatorService.enqueueProgressReport(action);
        expectLastCall().andAnswer(new IAnswer<Void>() {
            @Override
            public Void answer() throws Throwable {
                latch.countDown();
                return null;
            }
        }).anyTimes();

        EasyMock.replay(action, actionFileService, cmMediatorService);

        repository.persist(action);
        latch.countDown();
        EasyMock.verify(actionFileService, cmMediatorService);
    }

    @Test
    public void persist_restoreActionThatIsFinished_persistsItToFileAndCM() throws Exception {
        final Action action = EasyMock.createMock(Action.class);
        EasyMock.expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        EasyMock.expect(action.getState()).andReturn(ActionStateType.FINISHED);
        EasyMock.expect(action.isPartOfHousekeeping()).andReturn(false);
        actionFileService.writeToFile(action);
        expectLastCall();

        final Action actionClone = EasyMock.createMock(Action.class);
        EasyMock.expect(action.getCopy()).andReturn(Optional.of(actionClone));
        EasyMock.expect(actionClone.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        EasyMock.expect(actionClone.isPartOfHousekeeping()).andReturn(false);
        final CountDownLatch latch = new CountDownLatch(1);
        cmMediatorService.enqueueProgressReport(action);
        expectLastCall().andAnswer(new IAnswer<Void>() {
            @Override
            public Void answer() throws Throwable {
                latch.countDown();
                return null;
            }
        }).anyTimes();

        EasyMock.replay(action, actionClone, actionFileService, cmMediatorService);

        repository.persist(action);

        latch.await();
        EasyMock.verify(actionFileService, cmMediatorService);
    }

    @Test
    public void persist_restoreActionThatIsRunning_onlyWritesToFile() throws Exception {
        final Action action = EasyMock.createMock(Action.class);
        EasyMock.expect(action.getName()).andReturn(ActionType.RESTORE);
        EasyMock.expect(action.getState()).andReturn(ActionStateType.RUNNING);

        actionFileService.writeToFile(action);
        expectLastCall();
        EasyMock.replay(action, actionFileService, cmMediatorService);

        repository.persist(action);

        EasyMock.verify(actionFileService, cmMediatorService);
    }

    @Test
    public void persist_housekeepingAction_onlyWritesToFile() throws Exception {
        final Action action = EasyMock.createMock(Action.class);
        EasyMock.expect(action.getName()).andReturn(ActionType.HOUSEKEEPING_DELETE_BACKUP).anyTimes();
        EasyMock.expect(action.isPartOfHousekeeping()).andReturn(true);
        EasyMock.expect(action.getState()).andReturn(ActionStateType.FINISHED);

        actionFileService.writeToFile(action);
        expectLastCall();
        EasyMock.replay(action, actionFileService, cmMediatorService);

        repository.persist(action);

        EasyMock.verify(actionFileService, cmMediatorService);
    }

    private PersistedAction getPersistedAction(final String id, final ActionStateType state, final ActionType name) {
        return getPersistedAction(id, state, name, OffsetDateTime.now());
    }

    private PersistedAction getPersistedAction(final String id, final ActionStateType state, final ActionType name, final OffsetDateTime startTime) {
        final PersistedAction action = new PersistedAction();
        action.setActionId(id);
        action.setState(state);
        action.setStartTime(DateTimeUtils.convertToString(startTime));
        action.setLastUpdateTime(DateTimeUtils.convertToString(OffsetDateTime.now()));
        action.setName(name);
        return action;
    }

    private OffsetDateTime getDateTime(final int year, final int month, final int dayOfMonth, final int hour, final int minute) {
        final LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute);
        final ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(dateTime);
        return OffsetDateTime.of(dateTime, offset);
    }
}
