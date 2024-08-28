/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.VirtualInformation;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.PersistedBackupManager;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;

public class PeriodicEventSchedulerTest {

    private PeriodicEventScheduler eventHandler;
    private ActionService actionService;
    private Action action;
    private BackupManagerRepository backupManagerRepository;
    private CountDownLatch latch;

    @Before
    public void setUp() {
        eventHandler = new PeriodicEventScheduler();
        actionService = createMock(ActionService.class);
        action = createMock(Action.class);
        backupManagerRepository = createMock(BackupManagerRepository.class);

        eventHandler.setActionService(actionService);
        eventHandler.setBackupManagerRepository(backupManagerRepository);
        latch = new CountDownLatch(2);
    }

    @Test
    public void scheduleEvent_periodicEvent_executeActionAtScheduledTime() throws InterruptedException {
        expect(backupManagerRepository.getBackupManager("123")).andReturn(mockBackupManager("123", false)).anyTimes();

        expect(actionService.handleActionRequest(anyObject(String.class), anyObject(CreateActionRequest.class))).andReturn(action).times(1);
        expectLastCall();
        replay(actionService, backupManagerRepository);

        final PeriodicEvent event = createMock(PeriodicEvent.class);
        expect(event.getBackupManagerId()).andReturn("123").anyTimes();
        expect(event.getEventId()).andReturn("one").anyTimes();
        expect(event.getStartTime()).andReturn(DateTimeUtils.convertToString(OffsetDateTime.now())).anyTimes();
        expect(event.periodInSeconds()).andReturn(Long.valueOf(1)).anyTimes();
        expect(event.getStopTime()).andReturn(DateTimeUtils.convertToString((OffsetDateTime.now().plusSeconds(10)))).anyTimes();
        expect(event.getNextRun()).andReturn(OffsetDateTime.now().plusSeconds(1)).anyTimes();
        event.setNextRun(anyObject(OffsetDateTime.class));
        expectLastCall().anyTimes();
        replay(event);

        eventHandler.scheduleEvent(event);
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> latch.getCount() == 0);
        verify(actionService);
    }

    @Test
    public void scheduleEvent_periodicEventWithStopTime_doesNotExecuteAsStopTimeElapsed() throws InterruptedException {
        expect(backupManagerRepository.getBackupManager("123")).andReturn(mockBackupManager("123", false)).anyTimes();
        replay(actionService, backupManagerRepository);

        final PeriodicEvent event = createMock(PeriodicEvent.class);
        expect(event.getBackupManagerId()).andReturn("123").anyTimes();
        expect(event.getEventId()).andReturn("two").anyTimes();
        expect(event.getStartTime()).andReturn(DateTimeUtils.convertToString(OffsetDateTime.now())).anyTimes();
        expect(event.getStopTime()).andReturn(DateTimeUtils.convertToString(OffsetDateTime.now().plusSeconds(1))).anyTimes();
        expect(event.periodInSeconds()).andReturn(Long.valueOf(2)).anyTimes();
        expect(event.getNextRun()).andReturn(OffsetDateTime.now().plusSeconds(2)).anyTimes();
        event.setNextRun(anyObject(OffsetDateTime.class));
        expectLastCall().anyTimes();
        replay(event);

        eventHandler.scheduleEvent(event);
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> latch.getCount() == 0);
        verify(actionService);
    }

    @Test
    public void scheduleEvent_periodicEvent_doesNotExecuteActionBeforeScheduledTime() throws InterruptedException {
        expect(backupManagerRepository.getBackupManager("123")).andReturn(mockBackupManager("123", false)).anyTimes();
        replay(actionService, backupManagerRepository);

        final PeriodicEvent event = createMock(PeriodicEvent.class);
        expect(event.getBackupManagerId()).andReturn("123").anyTimes();
        expect(event.getEventId()).andReturn("three").anyTimes();
        expect(event.getStartTime()).andReturn(DateTimeUtils.convertToString(OffsetDateTime.now())).anyTimes();
        expect(event.getStopTime()).andReturn(DateTimeUtils.convertToString(OffsetDateTime.now().plusSeconds(10))).anyTimes();
        expect(event.periodInSeconds()).andReturn(Long.valueOf(2)).anyTimes();
        expect(event.getNextRun()).andReturn(OffsetDateTime.now().plusSeconds(2)).anyTimes();
        event.setNextRun(anyObject(OffsetDateTime.class));
        expectLastCall().anyTimes();
        replay(event);

        eventHandler.scheduleEvent(event);
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> latch.getCount() == 1);
        verify(actionService);
    }

    @Test
    public void scheduleEvent_adminStateLocked_doesNotExecuteAction() throws InterruptedException {
        expect(backupManagerRepository.getBackupManager("123")).andReturn(mockBackupManager("123", true)).anyTimes();
        replay(actionService, backupManagerRepository);

        final PeriodicEvent event = createMock(PeriodicEvent.class);
        expect(event.getBackupManagerId()).andReturn("123").anyTimes();
        expect(event.getEventId()).andReturn("four").anyTimes();
        expect(event.getStartTime()).andReturn(DateTimeUtils.convertToString(OffsetDateTime.now())).anyTimes();
        expect(event.getStopTime()).andReturn(DateTimeUtils.convertToString(OffsetDateTime.now().plusSeconds(10))).anyTimes();
        expect(event.periodInSeconds()).andReturn(Long.valueOf(1)).anyTimes();
        expect(event.getNextRun()).andReturn(OffsetDateTime.now().plusSeconds(1)).anyTimes();
        event.setNextRun(anyObject(OffsetDateTime.class));
        expectLastCall().anyTimes();
        replay(event);

        eventHandler.scheduleEvent(event);
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> latch.getCount() == 0);
        verify(actionService);
    }

    @Test
    public void scheduleEvent_actionCreationFails_scheduleContinuesToRun() {
        expect(backupManagerRepository.getBackupManager("123"))
                .andReturn(mockBackupManager("123", false)).anyTimes();
        // verify event runs at least twice despite thrown exception
        expect(actionService.handleActionRequest(anyString(), anyObject(CreateActionRequest.class)))
                .andThrow(new RuntimeException("Some runtime exception")).times(2, Integer.MAX_VALUE);
        replay(actionService, backupManagerRepository);

        final PeriodicEvent event = createMock(PeriodicEvent.class);
        expect(event.getBackupManagerId()).andReturn("123").anyTimes();
        expect(event.getEventId()).andReturn("five").anyTimes();
        expect(event.getStartTime()).andReturn(DateTimeUtils.convertToString(OffsetDateTime.now())).anyTimes();
        expect(event.getStopTime()).andReturn(DateTimeUtils.convertToString(OffsetDateTime.now().plusSeconds(5))).anyTimes();
        expect(event.periodInSeconds()).andReturn(Long.valueOf(1)).anyTimes();
        expect(event.getNextRun()).andReturn(OffsetDateTime.now().plusSeconds(1)).anyTimes();
        event.setNextRun(anyObject(OffsetDateTime.class));
        expectLastCall().anyTimes();
        replay(event);

        eventHandler.scheduleEvent(event);
        final OffsetDateTime start = OffsetDateTime.now();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> start.plusSeconds(5).isBefore(OffsetDateTime.now()));
        verify(actionService);
    }


    @Test
    public void scheduleTwoEvents_actionCancelFails_secondCancelRuns() {
        final AtomicInteger persistCallCount = new AtomicInteger(0);
        final Consumer<Scheduler> testPersist = s -> {
            if(persistCallCount.incrementAndGet() > 2) { // only throw after initial scheduling of both events
                throw new RuntimeException("Testing persist failure");
            }
        };

        final BackupManager backupManager = mockBackupManager("123", false, testPersist);

        expect(backupManagerRepository.getBackupManager("123")).andReturn(backupManager).anyTimes();
        expect(actionService.handleActionRequest(anyString(), anyObject(CreateActionRequest.class)))
                .andReturn(action).times(3);
        replay(actionService, backupManagerRepository);

        // Event 1 - Runs once, then is cancelled (and this cancel throws an exception)
        final OffsetDateTime startTime = OffsetDateTime.now().plusSeconds(3);
        final PeriodicEvent event1 = createMock(PeriodicEvent.class);
        expect(event1.getBackupManagerId()).andReturn("123").anyTimes();
        expect(event1.getEventId()).andReturn("six").anyTimes();
        expect(event1.getStartTime()).andReturn(DateTimeUtils.convertToString(startTime)).anyTimes();
        expect(event1.getStopTime()).andReturn(DateTimeUtils.convertToString(startTime.plusSeconds(1))).anyTimes();
        // 2 second period and stop time 1 second after start time ensures run once
        expect(event1.periodInSeconds()).andReturn(Long.valueOf(2)).anyTimes();
        expect(event1.getNextRun()).andReturn(startTime).anyTimes();
        event1.setNextRun(anyObject(OffsetDateTime.class));
        expectLastCall().anyTimes(); // Assert only run once
        replay(event1);

        // Event 2 - Runs twice, then is cancelled (and this cancel throws an exception)
        final PeriodicEvent event2 = createMock(PeriodicEvent.class);
        expect(event2.getBackupManagerId()).andReturn("123").anyTimes();
        expect(event2.getEventId()).andReturn("seven").anyTimes();
        expect(event2.getStartTime()).andReturn(DateTimeUtils.convertToString(startTime)).anyTimes();
        expect(event2.getStopTime()).andReturn(DateTimeUtils.convertToString(startTime.plusSeconds(3))).anyTimes();
        // 2 second period and stop time 3 second after start time ensures run twice
        expect(event2.periodInSeconds()).andReturn(Long.valueOf(2)).anyTimes();
        expect(event2.getNextRun()).andReturn(startTime).anyTimes();
        event2.setNextRun(anyObject(OffsetDateTime.class));
        expectLastCall().anyTimes(); // Assert run twice
        replay(event2);

        eventHandler.scheduleEvent(event1);
        eventHandler.scheduleEvent(event2);
        final OffsetDateTime start = OffsetDateTime.now();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> start.plusSeconds(9).isBefore(OffsetDateTime.now()));
        verify(actionService);
        // 2 calls for initial scheduling, 3 for event runs, 2 for event cancels. If this assert passes,
        // it verifies one cancel having an exception thrown does not stop the other from happening
        assertEquals(7, persistCallCount.get());
    }

    private BackupManager mockBackupManager(final String backupManagerId, final boolean schedulerLocker) {
        return mockBackupManager(backupManagerId, schedulerLocker, this::persist);
    }

    private BackupManager mockBackupManager(final String backupManagerId,
                                            final boolean schedulerLocker,
                                            final Consumer<Scheduler> persistFunction) {
        final PersistedBackupManager persistedBackupManager = new PersistedBackupManager();
        persistedBackupManager.setBackupManagerId(backupManagerId);
        final Scheduler scheduler = new Scheduler(backupManagerId, persistFunction);
        scheduler.setMostRecentlyCreatedAutoBackup("somebackup");
        if (schedulerLocker) {
            scheduler.setAdminState(AdminState.LOCKED);
        }
        final BackupManager backupManager = new BackupManager(
                persistedBackupManager,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                null, scheduler, null, null,
                null, new VirtualInformation());
        return backupManager;
    }

    private void persist(final Scheduler scheduler) {
        latch.countDown();
    }
}
