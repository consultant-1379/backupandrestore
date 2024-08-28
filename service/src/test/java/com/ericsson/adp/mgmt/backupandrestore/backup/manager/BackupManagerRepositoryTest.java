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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.agent.VBRMAutoCreate;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.BackupManagerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.HousekeepingFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.PersistedBackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.VirtualInformationFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.ScheduledEventHandler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerInformation;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEventFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerFileService;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupManagerNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.exception.FilePersistenceException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidIdException;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProvider;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;

import io.micrometer.core.instrument.MeterRegistry;

public class BackupManagerRepositoryTest {

    private BackupManagerRepository backupManagerRepository;
    private BackupManagerFileService fileService;
    private HousekeepingFileService housekeepingFileService;
    private SchedulerFileService schedulerFileService;
    private SftpServerFileService sftpServerFileService;
    private VirtualInformationFileService virtualInformationFileService;
    private PeriodicEventFileService periodicEventFileService;
    private CalendarEventFileService calendarEventFileService;
    private ScheduledEventHandler eventHandler;
    private CMMediatorService cmMediatorService;
    private IdValidator idValidator;
    private PersistedBackupManager persistedBackupManager;
    private Optional<PersistedBackupManager> optionalPersisted;
    private Logger mockLogger;

    @Before
    public void setup() {
        fileService = createMock(BackupManagerFileService.class);
        housekeepingFileService = createMock(HousekeepingFileService.class);
        schedulerFileService = createMock(SchedulerFileService.class);
        eventHandler = createMock(ScheduledEventHandler.class);
        cmMediatorService = createMock(CMMediatorService.class);
        periodicEventFileService = createMock(PeriodicEventFileService.class);
        calendarEventFileService = createMock(CalendarEventFileService.class);
        idValidator = createMock(IdValidator.class);
        persistedBackupManager = createMock(PersistedBackupManager.class);
        virtualInformationFileService = createMock(VirtualInformationFileService.class);
        sftpServerFileService = createMock(SftpServerFileService.class);
        optionalPersisted = Optional.of(persistedBackupManager);
        final ActionRepository actionRepository = createMock(ActionRepository.class);
        final BackupRepository backupRepository = createMock(BackupRepository.class);
        expect(actionRepository.getActions(anyObject())).andReturn(new ArrayList<>()).anyTimes();
        final Backup backup1 = createMock(Backup.class);
        final Backup backup2 = createMock(Backup.class);
        expect(backup1.getBackupId()).andReturn("backup1").anyTimes();
        expect(backup2.getBackupId()).andReturn("backup2").anyTimes();
        expect(backupRepository.getBackups(anyObject())).andReturn(Arrays.asList(backup1, backup2)).anyTimes();
        expect(backupRepository.corruptBackupIfIncomplete(anyObject(Backup.class))).andReturn(true).anyTimes();
        idValidator.validateId(anyObject());
        expectLastCall().anyTimes();
        replay(actionRepository, backupRepository, idValidator, backup1, backup2);

        backupManagerRepository = new BackupManagerRepository();
        backupManagerRepository.setBackupManagerFileService(fileService);
        backupManagerRepository.setHousekeepingFileService(housekeepingFileService);
        backupManagerRepository.setSchedulerFileService(schedulerFileService);
        backupManagerRepository.setPeriodicEventFileService(periodicEventFileService);
        backupManagerRepository.setCalendarEventFileService(calendarEventFileService);
        backupManagerRepository.setScheduledEventHandler(eventHandler);
        backupManagerRepository.setCmMediatorService(cmMediatorService);
        backupManagerRepository.setActionRepository(actionRepository);
        backupManagerRepository.setBackupRepository(backupRepository);
        backupManagerRepository.setIdValidator(idValidator);
        backupManagerRepository.setVirtualInformationFileService(virtualInformationFileService);
        backupManagerRepository.setSftpServerFileService(sftpServerFileService);
    }

    @Test
    public void failResetActionTest() {
        final BackupManagerRepository brmRepo = new BackupManagerRepository();
        final Logger originalLogger = LogManager.getLogger(BackupManagerRepository.class);

        mockLogger = EasyMock.createMock(Logger.class);
        brmRepo.setLogger(mockLogger);
        mockLogger.debug(EasyMock.anyString(), EasyMock.anyString());
        expectLastCall().anyTimes();
        mockLogger.info(EasyMock.anyString(), EasyMock.anyString());
        expectLastCall().anyTimes();
        mockLogger.info(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString());
        expectLastCall().anyTimes();
        mockLogger.debug(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString());
        expectLastCall().anyTimes();
        expect(mockLogger.isDebugEnabled()).andReturn(true).anyTimes();

        // Simple mocked action
        final Action action = createMock(Action.class);
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(action.getBackupManagerId()).andReturn("DEFAULT-bro").anyTimes();
        expect(action.getActionId()).andReturn("12345").anyTimes();
        replay(action);

        // We create BRMs in this test, so we need this
        final ActionRepository actionRepository = createMock(ActionRepository.class);
        expect(actionRepository.getActions("DEFAULT")).andReturn(List.of()).once();
        expect(actionRepository.getActions("DEFAULT-bro")).andReturn(List.of()).once();
        replay(actionRepository);

        // We mostly don't care what the provider receives, since we're testing the BRM repo here, not the Reset job.
        final PersistProvider provider = createMock(PersistProvider.class);
        expect(provider.exists(anyObject(Path.class))).andReturn(false).anyTimes();
        try {
            expect(provider.walk(anyObject(), anyInt())).andAnswer(Stream::empty).anyTimes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        replay(provider, mockLogger);

        // As above
        final PersistProviderFactory providerFactory = createMock(PersistProviderFactory.class);
        expect(providerFactory.getPersistProvider()).andReturn(provider).once();
        replay(providerFactory);

        // This asserts the right BRM is being used for the reset action, which asserts the BRM repo set it up right
        expect(fileService.getBackupManagerFolder("DEFAULT")).andReturn(Path.of("bro/DEFAULT")).anyTimes();
        fileService.writeToFile(anyObject());
        expectLastCall().times(2);

        expect(periodicEventFileService.createPeriodicEventsDirectory(anyObject(BackupManager.class))).andReturn(true).anyTimes();
        expect(calendarEventFileService.createCalendarEventsDirectory(anyObject(BackupManager.class))).andReturn(true).anyTimes();
        replay(fileService, periodicEventFileService, calendarEventFileService);

        brmRepo.setIdValidator(idValidator);
        brmRepo.setSchedulerFileService(schedulerFileService);
        brmRepo.setBackupManagerFileService(fileService);
        brmRepo.setVirtualInformationFileService(virtualInformationFileService);
        brmRepo.setHousekeepingFileService(housekeepingFileService);
        brmRepo.setPeriodicEventFileService(periodicEventFileService);
        brmRepo.setCalendarEventFileService(calendarEventFileService);
        brmRepo.setCmMediatorService(cmMediatorService);
        brmRepo.setProviderFactory(providerFactory);
        brmRepo.setActionRepository(actionRepository);
        brmRepo.createBackupManager("DEFAULT");

        brmRepo.failResetAction(action);
        EasyMock.reset(mockLogger);
        brmRepo.setLogger(originalLogger);
    }

    @Test
    public void getBackupManagers_noPersistedBackupManagers_holdsNoBackupManagers() throws Exception {
        mockPersistence(new ArrayList<>());

        final List<BackupManager> backupManagers = backupManagerRepository.getBackupManagers();

        assertTrue(backupManagers.isEmpty());
        verify(fileService, cmMediatorService, housekeepingFileService);
    }

    @Test
    public void getBackupManagers_getBackupManagersListTwiceInARow_returnsTheSameList() throws Exception {
        mockPersistence(new ArrayList<>());

        final List<BackupManager> firstList = backupManagerRepository.getBackupManagers();
        final List<BackupManager> secondList = backupManagerRepository.getBackupManagers();

        assertEquals(firstList, secondList);
    }

    @Test
    public void getBackupManagers_listHasBackupManagers_outsideUserCantModifyList() throws Exception {
        mockPersistence(new ArrayList<>());

        final List<BackupManager> list = backupManagerRepository.getBackupManagers();
        list.add(createMock(BackupManager.class));

        final List<BackupManager> backupManagers = backupManagerRepository.getBackupManagers();

        assertEquals(0, backupManagers.size());
    }

    @Test
    public void getBackupManagers_hasPersistedBackupManagerWithoutHousekeepingInfo_listBackupManagerWithHousekeepingInfo() throws Exception {
        mockPersistence(Arrays.asList(createPersistedBackupManager("a", false, 2)),
                () -> {
                    expect(schedulerFileService.getPersistedSchedulerInformation("a")).andReturn(new SchedulerInformation());
                    expectLastCall().anyTimes();
                    expect(eventHandler.getPeriodicEvents("a")).andReturn(new ArrayList<>());
                    expectLastCall().anyTimes();
                    expect(eventHandler.getCalendarEvents("a")).andReturn(new ArrayList<>());
                    eventHandler.schedulePeriodicEvents(anyObject());
                    expectLastCall().anyTimes();
                    eventHandler.scheduleCalendarEvents(anyObject());
                    expectLastCall();
                    schedulerFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    fileService.writeToFile(anyObject());
                    expectLastCall().times(2);
                    housekeepingFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    cmMediatorService.addBackupManager(anyObject());
                    expectLastCall().anyTimes();
                });

        final List<BackupManager> backupManagers = backupManagerRepository.getBackupManagers();
        final List<String> backupManagerIds = backupManagers.stream().map(BackupManager::getBackupManagerId).collect(Collectors.toList());

        assertEquals(2, backupManagerIds.size()); // Created, plus -bro vBRM
        assertTrue(backupManagerIds.contains("a"));
        assertEquals(2, backupManagers.get(0).getHousekeeping().getMaxNumberBackups());
        assertEquals(AUTO_DELETE_ENABLED, backupManagers.get(0).getHousekeeping().getAutoDelete());
        assertNotNull(backupManagers.get(0).getScheduler());
        assertEquals(0, backupManagers.get(0).getScheduler().getPeriodicEvents().size());
    }

    @Test
    public void getBackupManagers_hasPersistedBackupManagerWithoutHousekeepingInfoAndZeroBackups_listBackupManagerWithHousekeepingInfo() throws Exception {
        final BackupRepository backupRepository = createMock(BackupRepository.class);
        expect(backupRepository.getBackups(anyObject())).andReturn(new ArrayList<>()).anyTimes();
        replay(backupRepository);
        backupManagerRepository.setBackupRepository(backupRepository);

        mockPersistence(Arrays.asList(createPersistedBackupManager("a", false, 2)),
                () -> {
                    expect(schedulerFileService.getPersistedSchedulerInformation("a")).andReturn(new SchedulerInformation());
                    expect(eventHandler.getPeriodicEvents("a")).andReturn(new ArrayList<>());
                    expect(eventHandler.getCalendarEvents("a")).andReturn(new ArrayList<>());
                    eventHandler.schedulePeriodicEvents(anyObject());
                    expectLastCall();
                    eventHandler.scheduleCalendarEvents(anyObject());
                    expectLastCall();
                    schedulerFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    fileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    housekeepingFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    cmMediatorService.addBackupManager(anyObject());
                    expectLastCall().anyTimes();
                });

        final List<BackupManager> backupManagers = backupManagerRepository.getBackupManagers();
        final List<String> backupManagerIds = backupManagers.stream().map(BackupManager::getBackupManagerId).collect(Collectors.toList());

        assertEquals(2, backupManagerIds.size());
        assertTrue(backupManagerIds.contains("a"));
        assertEquals(1, backupManagers.get(0).getHousekeeping().getMaxNumberBackups());
        assertEquals(AUTO_DELETE_ENABLED, backupManagers.get(0).getHousekeeping().getAutoDelete());
        assertNotNull(backupManagers.get(0).getScheduler());
        assertEquals(0, backupManagers.get(0).getScheduler().getPeriodicEvents().size());
    }

    @Test
    public void getBackupManagers_hasPersistedBackupManagerWithBackupGreaterThanMaxBackup_listBackupManagerWithUpdatedMaxBackup() throws Exception {
        mockPersistence(Arrays.asList(createPersistedBackupManager("a", true, 1)),
                () -> {
                    expect(schedulerFileService.getPersistedSchedulerInformation("a")).andReturn(new SchedulerInformation());
                    expectLastCall();
                    expect(eventHandler.getPeriodicEvents("a")).andReturn(new ArrayList<>());
                    expect(eventHandler.getCalendarEvents("a")).andReturn(new ArrayList<>());
                    eventHandler.schedulePeriodicEvents(anyObject());
                    expectLastCall();
                    eventHandler.scheduleCalendarEvents(anyObject());
                    expectLastCall();
                    schedulerFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    fileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    housekeepingFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    cmMediatorService.addBackupManager(anyObject());
                    expectLastCall().anyTimes();
                });

        final List<BackupManager> backupManagers = backupManagerRepository.getBackupManagers();
        final List<String> backupManagerIds = backupManagers.stream().map(BackupManager::getBackupManagerId).collect(Collectors.toList());

        assertEquals(2, backupManagerIds.size());
        assertTrue(backupManagerIds.contains("a"));
        assertEquals(2, backupManagers.get(0).getHousekeeping().getMaxNumberBackups());
        assertEquals(AUTO_DELETE_ENABLED, backupManagers.get(0).getHousekeeping().getAutoDelete());
        assertNotNull(backupManagers.get(0).getScheduler());
        assertEquals(0, backupManagers.get(0).getScheduler().getPeriodicEvents().size());
    }

    @Test
    public void getBackupManagers_hasMultiplePersistedBackupManagers_listReturnsAllPersistedBackupManagers() throws Exception {
        mockPersistence(Arrays.asList(createPersistedBackupManager("a", true, 2), createPersistedBackupManager("b", true, 2), createPersistedBackupManager(BackupManager.DEFAULT_BACKUP_MANAGER_ID, true, 2)),
                () -> {
                    expect(schedulerFileService.getPersistedSchedulerInformation(anyObject())).andReturn(new SchedulerInformation());
                    expectLastCall().times(3);
                    expect(eventHandler.getPeriodicEvents(anyObject())).andReturn(new ArrayList<>()).anyTimes();
                    expect(eventHandler.getCalendarEvents(anyObject())).andReturn(new ArrayList<>()).anyTimes();
                    eventHandler.schedulePeriodicEvents(anyObject());
                    expectLastCall().times(3);
                    eventHandler.scheduleCalendarEvents(anyObject());
                    expectLastCall().times(3);
                    schedulerFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    fileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    housekeepingFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    cmMediatorService.addBackupManager(anyObject());
                    expectLastCall().anyTimes();
                });

        final List<BackupManager> backupManagers = backupManagerRepository.getBackupManagers();
        final List<String> backupManagerIds = backupManagers.stream().map(BackupManager::getBackupManagerId).collect(Collectors.toList());

        assertEquals(6, backupManagerIds.size());
        assertTrue(backupManagerIds.contains(BackupManager.DEFAULT_BACKUP_MANAGER_ID));
        assertTrue(backupManagerIds.contains("a"));
        assertTrue(backupManagerIds.contains("b"));
        assertNotNull(backupManagers.get(0).getScheduler());
        assertEquals(0, backupManagers.get(0).getScheduler().getPeriodicEvents().size());
    }

    @Test
    public void getBackupManager_backupManagerId_getBackupManager() throws Exception {
        mockPersistence(Arrays.asList(createPersistedBackupManager("123", true, 2)),
                () -> {
                    expect(schedulerFileService.getPersistedSchedulerInformation("123")).andReturn(new SchedulerInformation());
                    expect(eventHandler.getPeriodicEvents("123")).andReturn(new ArrayList<>());
                    expect(eventHandler.getCalendarEvents("123")).andReturn(new ArrayList<>());
                    eventHandler.schedulePeriodicEvents(anyObject());
                    expectLastCall();
                    eventHandler.scheduleCalendarEvents(anyObject());
                    expectLastCall();
                    schedulerFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    fileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    housekeepingFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    cmMediatorService.addBackupManager(anyObject());
                    expectLastCall().anyTimes();
                });

        final BackupManager obtainedManager = backupManagerRepository.getBackupManager("123");

        assertEquals("123", obtainedManager.getBackupManagerId());
        assertNotNull(obtainedManager.getScheduler());
        assertEquals(0, obtainedManager.getScheduler().getPeriodicEvents().size());
    }

    @Test
    public void getBackupManager_backupManagerWithActionsAndBackups_getBackupManagerWithActionsAndBackups() throws Exception {
        final ActionRepository actionRepository = createMock(ActionRepository.class);
        final BackupRepository backupRepository = createMock(BackupRepository.class);
        final ArrayList<Backup> dummyBakups = new ArrayList<>();
        final Backup dummy = createMock(Backup.class);
        expect(dummy.getBackupId()).andReturn("dummy").anyTimes();
        dummyBakups.add(dummy);
        final Action dummyAction1 = mockAction("dummyBk1", "123", 5);
        final Action dummyAction2 = mockAction("dummyBk2", "123", 6);
        expect(dummyAction1.getActionId()).andReturn("456").anyTimes();
        expect(dummyAction2.getActionId()).andReturn("789").anyTimes();
        dummyAction1.updateOperationsTotalMetric();
        expectLastCall();
        dummyAction2.updateOperationsTotalMetric();
        expectLastCall();
        replay(dummyAction1, dummyAction2, dummy);
        expect(actionRepository.getActions("123")).andReturn(Arrays.asList(dummyAction1, dummyAction2)).anyTimes();
        expect(actionRepository.getActions("123-bro")).andReturn(Collections.emptyList()).anyTimes();
        expect(backupRepository.getBackups("123")).andReturn(dummyBakups).anyTimes();
        Action dummyAction = anyObject(Action.class);
        expect(actionRepository.failActionIfRunning(dummyAction)).andReturn(dummyAction).anyTimes();
        expect(backupRepository.corruptBackupIfIncomplete(dummy)).andReturn(true);

        replay(actionRepository, backupRepository);

        backupManagerRepository.setActionRepository(actionRepository);
        backupManagerRepository.setBackupRepository(backupRepository);

        mockPersistence(Arrays.asList(createPersistedBackupManager("123", true, 2)),
                () -> {
                    expect(schedulerFileService.getPersistedSchedulerInformation("123")).andReturn(new SchedulerInformation());
                    expectLastCall();
                    expect(eventHandler.getPeriodicEvents("123")).andReturn(new ArrayList<>());
                    expect(eventHandler.getCalendarEvents("123")).andReturn(new ArrayList<>());
                    eventHandler.schedulePeriodicEvents(anyObject());
                    expectLastCall();
                    eventHandler.scheduleCalendarEvents(anyObject());
                    expectLastCall();
                    schedulerFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    fileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    housekeepingFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    cmMediatorService.addBackupManager(anyObject());
                });

        final BackupManager obtainedManager = backupManagerRepository.getBackupManager("123");

        assertEquals("123", obtainedManager.getBackupManagerId());
        assertEquals(2, obtainedManager.getActions().size());
        assertEquals(1, obtainedManager.getBackups(Ownership.READABLE).size());
        assertNotNull(obtainedManager.getScheduler());
        assertEquals(0, obtainedManager.getScheduler().getPeriodicEvents().size());

        // Verify that last action (dummyBk2) is present in bro.operation.info
        // and bro.operation.end.time metrics
        SpringContext.getBean(MeterRegistry.class).ifPresent(registry -> {
            assertNotNull(registry
                    .find("bro.operation.info")
                    .tag("action", "CREATE_BACKUP")
                    .tag("backup_type", "123")
                    .tag("backup_name", "dummyBk2")
                    .gauge());
            assertNotNull(registry
                    .find("bro.operation.end.time")
                    .tag("action", "CREATE_BACKUP")
                    .tag("backup_type", "123")
                    .gauge());
        }
        );
    }

    @Test(expected = BackupManagerNotFoundException.class)
    public void getBackupManager_inexistingBackupManagerId_throwsException() throws Exception {
        mockPersistence(new ArrayList<>());

        backupManagerRepository.getBackupManager("456");
    }

    @Test
    public void createBackupManager_defaultBackupManagerDoesNotExist_createsDefaultBackupManager() throws Exception {
        mockPersistence(new ArrayList<>(), () -> {
            fileService.writeToFile(anyObject(BackupManager.class));
            expectLastCall().anyTimes();
            expect(fileService.getBackupManagerFolder(anyObject())).andReturn(Paths.get("/DONT_EXIST"));
            housekeepingFileService.writeToFile(anyObject(Housekeeping.class));
            expectLastCall().anyTimes();
            schedulerFileService.getPersistedSchedulerInformation("DEFAULT");
            expectLastCall().andThrow(new IndexOutOfBoundsException()); // Mimic no scheduler information found (upgrade)
            schedulerFileService.writeToFile(anyObject(Scheduler.class));
            expectLastCall().anyTimes();
            cmMediatorService.addBackupManager(anyObject(BackupManager.class));
            expectLastCall().anyTimes();
        });

        backupManagerRepository.createBackupManager("DEFAULT");

        final List<BackupManager> backupManagers = backupManagerRepository.getBackupManagers();

        assertEquals(2, backupManagers.size());
        assertEquals("DEFAULT", backupManagers.get(0).getBackupManagerId());
        assertNotNull(backupManagers.get(0).getScheduler());
        assertEquals(0, backupManagers.get(0).getScheduler().getPeriodicEvents().size());
    }

    @Test
    public void createBackupManager_noScopeWhenDefaultBackupManagerAlreadyExists_doesNotCreateNewBackupManager() throws Exception {
        mockPersistence(Arrays.asList(createPersistedBackupManager(BackupManager.DEFAULT_BACKUP_MANAGER_ID, true, 2)),
                () -> {
                    expect(schedulerFileService.getPersistedSchedulerInformation(BackupManager.DEFAULT_BACKUP_MANAGER_ID)).andReturn(new SchedulerInformation());
                    expect(eventHandler.getPeriodicEvents(BackupManager.DEFAULT_BACKUP_MANAGER_ID)).andReturn(new ArrayList<>());
                    expect(eventHandler.getCalendarEvents(BackupManager.DEFAULT_BACKUP_MANAGER_ID)).andReturn(new ArrayList<>());
                    eventHandler.schedulePeriodicEvents(anyObject());
                    expectLastCall();
                    eventHandler.scheduleCalendarEvents(anyObject());
                    expectLastCall();
                    schedulerFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    fileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    housekeepingFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    cmMediatorService.addBackupManager(anyObject());
                    expectLastCall().anyTimes();
                });

        backupManagerRepository.createBackupManager("");

        final List<BackupManager> backupManagers = backupManagerRepository.getBackupManagers();

        assertEquals(2, backupManagers.size());
        assertEquals("DEFAULT", backupManagers.get(0).getBackupManagerId());
        assertNotNull(backupManagers.get(0).getScheduler());
        assertEquals(0, backupManagers.get(0).getScheduler().getPeriodicEvents().size());
    }

    @Test
    public void createBackupManager_agentWithNonDefaultScope_createsNewBackupManager() throws Exception {

        mockPersistence(new ArrayList<>(), () -> {
            fileService.writeToFile(anyObject(BackupManager.class));
            expectLastCall().anyTimes();
            expect(fileService.getBackupManagerFolder(anyObject())).andReturn(Paths.get("/DONT_EXIST")).anyTimes();
            expect(fileService.getPersistedBackupManager(anyObject())).andReturn(optionalPersisted).anyTimes();
            expect(persistedBackupManager.getBackupManagerId()).andReturn("1").anyTimes();
            housekeepingFileService.writeToFile(anyObject(Housekeeping.class));
            expectLastCall().anyTimes();
            schedulerFileService.getPersistedSchedulerInformation("1");
            expectLastCall().andThrow(new IndexOutOfBoundsException()).anyTimes();// Mimic behaviour of no scheduler information present
            schedulerFileService.writeToFile(anyObject(Scheduler.class));
            expectLastCall().anyTimes();
            cmMediatorService.addBackupManager(anyObject(BackupManager.class));
            expectLastCall().anyTimes();
        });

        backupManagerRepository.createBackupManager("1");

        final Set<String> backupManagerIds = backupManagerRepository.getBackupManagers().stream().map(BackupManager::getBackupManagerId).collect(Collectors.toSet());

        assertTrue(backupManagerIds.contains("1"));

        verify(fileService, cmMediatorService, housekeepingFileService, periodicEventFileService, calendarEventFileService, persistedBackupManager);
    }

    @Test(expected = InvalidIdException.class)
    public void createBackupManager_agentWithInvalidScope_throwsException() throws Exception {
        mockPersistence(new ArrayList<>());

        idValidator = createMock(IdValidator.class);

        idValidator.validateId(anyObject());
        expectLastCall().andThrow(new InvalidIdException(""));
        replay(idValidator);
        backupManagerRepository.setIdValidator(idValidator);

        backupManagerRepository.createBackupManager("/");
    }

    @Test
    public void createBackupManager_multipleAgentsWithTheSameNonDefaultScope_createsNewBackupManagerOnlyWhenHandlingFirstAgent() throws Exception {
        mockPersistence(new ArrayList<>(), () -> {
            fileService.writeToFile(anyObject(BackupManager.class));
            expectLastCall().anyTimes();
            expect(fileService.getBackupManagerFolder(anyObject())).andReturn(Paths.get("/DONT_EXIST")).anyTimes();
            housekeepingFileService.writeToFile(anyObject(Housekeeping.class));
            expectLastCall().anyTimes();
            schedulerFileService.writeToFile(anyObject(Scheduler.class));
            expectLastCall().anyTimes();
            schedulerFileService.getPersistedSchedulerInformation(anyString());
            expectLastCall().andThrow(new IndexOutOfBoundsException()); // Mimic no scheduler information found (upgrade)
            expectLastCall().anyTimes();
            cmMediatorService.addBackupManager(anyObject(BackupManager.class));
            expectLastCall().anyTimes();
        });

        backupManagerRepository.createBackupManager("1");
        backupManagerRepository.createBackupManager("1");
        backupManagerRepository.createBackupManager("1");
        backupManagerRepository.createBackupManager("1");
        backupManagerRepository.createBackupManager("1");

        final Set<String> backupManagerIds = backupManagerRepository.getBackupManagers().stream().map(BackupManager::getBackupManagerId).collect(Collectors.toSet());

        assertEquals(1, backupManagerIds.stream().filter("1"::equals).count());

        verify(fileService, cmMediatorService, housekeepingFileService);
    }

    @Test
    public void createBackupManager_multipleAgentsWithTheMultipleScopes_createsAllNecessaryBackupManagers() throws Exception {
        mockPersistence(Arrays.asList(createPersistedBackupManager(BackupManager.DEFAULT_BACKUP_MANAGER_ID, true, 2)), () -> {
            fileService.writeToFile(anyObject(BackupManager.class));
            expectLastCall().times(9);
            expect(fileService.getBackupManagerFolder(anyObject())).andReturn(Paths.get("/DONT_EXIST")).anyTimes();
            housekeepingFileService.writeToFile(anyObject(Housekeeping.class));
            expectLastCall().times(9);
            schedulerFileService.writeToFile(anyObject(Scheduler.class));
            expectLastCall().anyTimes();
            cmMediatorService.addScheduler(anyObject(Scheduler.class));
            expectLastCall().anyTimes();
            schedulerFileService.getPersistedSchedulerInformation("DEFAULT");
            expectLastCall().andThrow(new IndexOutOfBoundsException()); // Mimic no scheduler information found (upgrade)
            expect(schedulerFileService.getPersistedSchedulerInformation(anyObject())).andReturn(new SchedulerInformation());
            expectLastCall().anyTimes();
            expect(eventHandler.getPeriodicEvents(anyObject())).andReturn(new ArrayList<>());
            expectLastCall().times(9);
            eventHandler.schedulePeriodicEvents(anyObject());
            eventHandler.scheduleCalendarEvents(anyObject());
            expectLastCall().times(9);
            cmMediatorService.addBackupManager(anyObject(BackupManager.class));
            expectLastCall().times(9);
        });

        backupManagerRepository.createBackupManager("1");
        backupManagerRepository.createBackupManager("2");
        backupManagerRepository.createBackupManager("3");
        backupManagerRepository.createBackupManager("DEFAULT");
        backupManagerRepository.createBackupManager("");
        backupManagerRepository.createBackupManager("A");

        final Set<String> backupManagerIds = backupManagerRepository.getBackupManagers().stream().map(BackupManager::getBackupManagerId).collect(Collectors.toSet());

        assertTrue(backupManagerIds.contains("1"));
        assertTrue(backupManagerIds.contains("2"));
        assertTrue(backupManagerIds.contains("3"));
        assertTrue(backupManagerIds.contains("A"));

        verify(fileService, cmMediatorService, housekeepingFileService);
    }

    @Test
    public void createBackupManager_multipleBackupManagers_createsBackupManagerThatCanBePersisted() throws Exception {
        mockPersistence(Arrays.asList(createPersistedBackupManager(BackupManager.DEFAULT_BACKUP_MANAGER_ID, true, 2)), () -> {
            fileService.writeToFile(anyObject(BackupManager.class));
            expectLastCall().times(4);
            expect(fileService.getBackupManagerFolder(anyObject())).andReturn(Paths.get("/DONT_EXIST")).anyTimes();
            housekeepingFileService.writeToFile(anyObject(Housekeeping.class));
            expectLastCall().anyTimes();
            schedulerFileService.writeToFile(anyObject(Scheduler.class));
            expectLastCall().anyTimes();
            expect(schedulerFileService.getPersistedSchedulerInformation(BackupManager.DEFAULT_BACKUP_MANAGER_ID)).andReturn(new SchedulerInformation());
            expectLastCall().anyTimes();
            expect(eventHandler.getPeriodicEvents(BackupManager.DEFAULT_BACKUP_MANAGER_ID)).andReturn(new ArrayList<>());
            expectLastCall().anyTimes();
            expect(eventHandler.getCalendarEvents(BackupManager.DEFAULT_BACKUP_MANAGER_ID)).andReturn(new ArrayList<>());
            eventHandler.schedulePeriodicEvents(anyObject());
            expectLastCall().anyTimes();
            eventHandler.scheduleCalendarEvents(anyObject());
            expectLastCall();
            cmMediatorService.addBackupManager(anyObject(BackupManager.class));
            expectLastCall().anyTimes();
            cmMediatorService.updateBackupManager(anyObject(BackupManager.class));
            expectLastCall().anyTimes();
            schedulerFileService.getPersistedSchedulerInformation("AAA");
            expectLastCall().andThrow(new IndexOutOfBoundsException()); // Mimic no scheduler information found (upgrade)
        });

        backupManagerRepository.createBackupManager("AAA");
        backupManagerRepository.getBackupManager("AAA").persist();

        verify(fileService, cmMediatorService, housekeepingFileService);
    }

    @Test
    public void getIndex_hasMultiplePersistedBackupManagers_returnsIndexOfBackupManager() throws Exception {
        mockPersistence(Arrays.asList(createPersistedBackupManager("a", true, 2), createPersistedBackupManager("b", true, 2), createPersistedBackupManager(BackupManager.DEFAULT_BACKUP_MANAGER_ID, true, 2)),
                () -> {
                    expect(schedulerFileService.getPersistedSchedulerInformation(anyObject())).andReturn(new SchedulerInformation());
                    expectLastCall().times(6);
                    expect(eventHandler.getPeriodicEvents(anyObject())).andReturn(new ArrayList<>()).anyTimes();
                    expect(eventHandler.getCalendarEvents(anyObject())).andReturn(new ArrayList<>()).anyTimes();
                    eventHandler.schedulePeriodicEvents(anyObject());
                    expectLastCall().anyTimes();
                    eventHandler.scheduleCalendarEvents(anyObject());
                    expectLastCall().anyTimes();
                    schedulerFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    fileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    housekeepingFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    cmMediatorService.addBackupManager(anyObject());
                    expectLastCall().anyTimes();
                });

        assertEquals(1, backupManagerRepository.getIndex("b"));
    }

    @Test(expected = BackupManagerNotFoundException.class)
    public void getIndex_doesNotHoldBackupManager_throwsException() throws Exception {
        mockPersistence(Arrays.asList());

        assertEquals(1, backupManagerRepository.getIndex(""));
    }

    @Test
    public void getBackupManagers_hasPersistedBackupManagerWithoutSchedulerInfo_listBackupManagerWithoutSchedulerInfo() throws Exception {
        String brmID = "a";
        mockPersistence(Arrays.asList(createPersistedBackupManager(brmID, false, 2)),
                () -> {
                    expect(schedulerFileService.getPersistedSchedulerInformation(brmID)).andThrow(new FilePersistenceException(new Exception()));
                    expectLastCall();
                    schedulerFileService.writeToFile(anyObject()); // Scheduler information expected to be persisted on load of manager with missing scheduler information
                    expectLastCall().anyTimes();
                    eventHandler.schedulePeriodicEvents(anyObject(Scheduler.class));

                    expectLastCall().anyTimes();
                    eventHandler.scheduleCalendarEvents(anyObject(Scheduler.class));
                    expectLastCall();
                    cmMediatorService.addScheduler(anyObject(Scheduler.class));
                    expectLastCall().anyTimes();
                    fileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    housekeepingFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    cmMediatorService.addBackupManager(anyObject());
                    expectLastCall().anyTimes();
                });

        final List<BackupManager> backupManagers = backupManagerRepository.getBackupManagers();
        final List<String> backupManagerIds = backupManagers.stream().map(BackupManager::getBackupManagerId).collect(Collectors.toList());

        assertEquals(2, backupManagerIds.size());
        assertTrue(backupManagerIds.contains(brmID));
        assertEquals(2, backupManagers.get(0).getHousekeeping().getMaxNumberBackups());
        assertEquals(AUTO_DELETE_ENABLED, backupManagers.get(0).getHousekeeping().getAutoDelete());
    }

    @Test
    public void getBackupManagers_hasPersistedBackupManagerWithSchedulerAndPeriodicEvents_listBackupManagerWithSchedulerInfo() throws Exception {
        final PeriodicEvent event = new PeriodicEvent("a", null);
        event.setEventId("1");
        mockPersistence(Arrays.asList(createPersistedBackupManager("a", true, 2)),
                () -> {
                    expect(schedulerFileService.getPersistedSchedulerInformation("a")).andReturn(new SchedulerInformation(new Scheduler("a", null)));
                    expect(eventHandler.getPeriodicEvents("a")).andReturn(Arrays.asList(event));
                    expect(eventHandler.getCalendarEvents("a")).andReturn(new ArrayList<>());
                    eventHandler.schedulePeriodicEvents(anyObject());
                    expectLastCall();
                    eventHandler.scheduleCalendarEvents(anyObject());
                    expectLastCall();
                    schedulerFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    fileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    housekeepingFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    cmMediatorService.addBackupManager(anyObject());
                    expectLastCall().anyTimes();
                });

        final List<BackupManager> backupManagers = backupManagerRepository.getBackupManagers();
        final List<String> backupManagerIds = backupManagers.stream().map(BackupManager::getBackupManagerId).collect(Collectors.toList());

        assertEquals(2, backupManagerIds.size());
        assertTrue(backupManagerIds.contains("a"));
        assertEquals(2, backupManagers.get(0).getHousekeeping().getMaxNumberBackups());
        assertEquals(AUTO_DELETE_ENABLED, backupManagers.get(0).getHousekeeping().getAutoDelete());
        assertEquals("a", backupManagers.get(0).getScheduler().getBackupManagerId());
        assertEquals(AdminState.UNLOCKED, backupManagers.get(0).getScheduler().getAdminState());
        assertEquals("a", backupManagers.get(0).getScheduler().getPeriodicEvents().get(0).getBackupManagerId());
    }

    @Test
    public void createBackupManager_initializeBackupManagerFromDisk_Valid() throws IOException {
        Path fileLocation;
        BackupManagerFileService fileService;
        TemporaryFolder folder = new TemporaryFolder();
        folder.create();

        expect(housekeepingFileService.getPersistedHousekeepingInformation(EasyMock.anyString())).andReturn(new HousekeepingInformation(AUTO_DELETE_ENABLED, 10)).anyTimes();
        housekeepingFileService.writeToFile(anyObject(Housekeeping.class));
        expectLastCall().anyTimes();
        expect(schedulerFileService.getPersistedSchedulerInformation(EasyMock.anyString())).andReturn(new SchedulerInformation()).anyTimes();
        schedulerFileService.writeToFile(anyObject());
        expectLastCall().anyTimes();
        expect(periodicEventFileService.createPeriodicEventsDirectory(anyObject(BackupManager.class))).andReturn(true).anyTimes();
        expect(calendarEventFileService.createCalendarEventsDirectory(anyObject(BackupManager.class))).andReturn(true).anyTimes();

        final PersistedBackupManager persistedBackupManager = new PersistedBackupManager();
        persistedBackupManager.setBackupManagerId("456");
        final BackupManager backupManager = new BackupManager(
                persistedBackupManager,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                null, null, null, null,
                backupManagerRepository,
                new VirtualInformation());

        fileLocation = Paths.get(folder.getRoot().getAbsolutePath());
        fileService = new BackupManagerFileService();
        fileService.setJsonService(new JsonService());
        fileService.setBackupManagersLocation(fileLocation.toAbsolutePath().toString());
        backupManager.setBackupDomain("qwe");
        backupManager.setBackupType("abc");
        fileService.writeToFile(backupManager);
        replay(housekeepingFileService, schedulerFileService,periodicEventFileService, calendarEventFileService);
        backupManagerRepository.setBackupManagerFileService(fileService);
        backupManagerRepository.createBackupManager(persistedBackupManager.getBackupManagerId());

    }

    @Test
    public void initializeDataManagementAgentvBRMFromDisk_withvBRMAutoCreateNone_andDeleteVBRMIsTrue() throws IOException {
        final String brmId = "DEFAULT-bravo";

        TemporaryFolder folder = new TemporaryFolder();
        folder.create();
        Path brmsFolder = Paths.get(folder.getRoot().getAbsolutePath(), "backupManagers");
        final Path vBRMconfiguration = brmsFolder.resolve(brmId);
        Path backupsFolder = Paths.get(folder.getRoot().getAbsolutePath(), "backups");
        final Path vBRMbackups = backupsFolder.resolve(brmId);

        persistVBRM(brmId, brmsFolder, vBRMbackups);

        // Verify the persisted vBRM configuration and backup files exists
        assertTrue(Files.exists(vBRMconfiguration));
        assertTrue(Files.exists(vBRMbackups));

        // Set vBRMAutoCreate to NONE and the deleteVBRM to true
        backupManagerRepository.setDeleteVBRM(true);
        backupManagerRepository.setVbrmAutoCreate(VBRMAutoCreate.NONE);
        backupManagerRepository.initializeBackupManagers();

        // Verify the persisted vBRM is NOT read into memory during initialization
        assertTrue(backupManagerRepository.getBackupManagers().isEmpty());
        // Verify the persisted vBRM configuration files are deleted
        assertFalse(Files.exists(vBRMconfiguration));
        // Verify the persisted vBRM backup files are deleted
        assertFalse(Files.exists(vBRMbackups));
    }

    @Test
    public void initializeDataManagementAgentvBRMFromDisk_withvBRMAutoCreateNone_andDeleteVBRMIsFalse() throws IOException {
        final String brmId = "DEFAULT-bravo";

        TemporaryFolder folder = new TemporaryFolder();
        folder.create();
        Path brmsFolder = Paths.get(folder.getRoot().getAbsolutePath(), "backupManagers");
        final Path vBRMconfiguration = brmsFolder.resolve(brmId);
        Path backupsFolder = Paths.get(folder.getRoot().getAbsolutePath(), "backups");
        final Path vBRMbackups = backupsFolder.resolve(brmId);

        persistVBRM(brmId, brmsFolder, vBRMbackups);

        // Verify the persisted vBRM configuration and backup files exists
        assertTrue(Files.exists(vBRMconfiguration));
        assertTrue(Files.exists(vBRMbackups));

        // Set vBRMAutoCreate to NONE and the deleteVBRM to true
        backupManagerRepository.setDeleteVBRM(false);
        backupManagerRepository.setVbrmAutoCreate(VBRMAutoCreate.NONE);
        backupManagerRepository.initializeBackupManagers();

        // Verify the persisted vBRM is read into memory during initialization
        final List<BackupManager> backupManagers = backupManagerRepository.getBackupManagers();
        final List<String> backupManagerIds = backupManagers.stream().map(BackupManager::getBackupManagerId).collect(Collectors.toList());
        assertEquals(1, backupManagerIds.size());
        assertTrue(backupManagerIds.contains(brmId));
        // Verify the persisted vBRM configuration files still exist
        assertTrue(Files.exists(vBRMconfiguration));
        // Verify the persisted vBRM backup files still exist
        assertTrue(Files.exists(vBRMbackups));
    }

    private void persistVBRM(final String brmId, Path brmsFolder, final Path vBRMbackups) throws IOException {
        BackupManagerFileService fileService = new BackupManagerFileService();

        fileService.setJsonService(new JsonService());
        fileService.setBackupManagersLocation(brmsFolder.toAbsolutePath().toString());
        BackupLocationService backupLocationService = createMock(BackupLocationService.class);
        expect(backupLocationService.getBackupManagerLocation(brmId)).andReturn(vBRMbackups);
        fileService.setBackupLocationService(backupLocationService);
        backupManagerRepository.setBackupManagerFileService(fileService);

        VirtualInformation virtualInformation = new VirtualInformation(BackupManager.DEFAULT_BACKUP_MANAGER_ID, new ArrayList<>());
        expect(virtualInformationFileService.getVirtualInformation(brmId)).andReturn(Optional.of(virtualInformation)).anyTimes();
        expect(housekeepingFileService.getPersistedHousekeepingInformation(brmId)).andReturn((new HousekeepingInformation(AUTO_DELETE_ENABLED, 1)));
        housekeepingFileService.writeToFile(anyObject());
        expectLastCall();
        cmMediatorService.updateHousekeeping(anyObject(Housekeeping.class));
        expectLastCall();
        expect(schedulerFileService.getPersistedSchedulerInformation(brmId)).andReturn(new SchedulerInformation(new Scheduler(brmId, null)));
        PeriodicEvent event = new PeriodicEvent(brmId, null);
        event.setEventId("id1");
        expect(eventHandler.getPeriodicEvents(brmId)).andReturn(Arrays.asList(event));
        expect(eventHandler.getCalendarEvents(brmId)).andReturn(new ArrayList<>());
        expect(sftpServerFileService.getSftpServers(brmId)).andReturn(new ArrayList<>());
        eventHandler.schedulePeriodicEvents(anyObject());
        expectLastCall();
        eventHandler.scheduleCalendarEvents(anyObject());
        expectLastCall();

        replay(cmMediatorService, housekeepingFileService, schedulerFileService, sftpServerFileService,
                periodicEventFileService, calendarEventFileService, eventHandler, virtualInformationFileService,backupLocationService);

        final PersistedBackupManager persistedVBRM = new PersistedBackupManager();
        persistedVBRM.setBackupManagerId(brmId);

        final BackupManager vBRM = new BackupManager(
                persistedVBRM,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                null, null, null, null,
                backupManagerRepository,
                virtualInformation);
        vBRM.setBackupDomain("qwe");
        vBRM.setBackupType("abc");

        // Create a persistedVBRM
        fileService.writeToFile(vBRM);

        // Create a directory for the vBRM backups
        Files.createDirectories(vBRMbackups);
    }

    @Test
    public void initializeBROAgentVBRMFromDisk_withvBRMAutoCreateNone_andDeleteVBRMIsTrue() throws IOException {
        backupManagerRepository.setDeleteVBRM(true);
        backupManagerRepository.setVbrmAutoCreate(VBRMAutoCreate.NONE);
        String backupManagerId = "DEFAULT-bro";
        PeriodicEvent event = new PeriodicEvent(backupManagerId, null);
        event.setEventId("id1");
        mockPersistence(Arrays.asList(createPersistedBackupManager(backupManagerId, true, 2)),
                () -> {
                    expect(schedulerFileService.getPersistedSchedulerInformation(backupManagerId)).andReturn(new SchedulerInformation(new Scheduler(backupManagerId, null)));
                    expect(eventHandler.getPeriodicEvents(backupManagerId)).andReturn(Arrays.asList(event));
                    expect(eventHandler.getCalendarEvents(backupManagerId)).andReturn(new ArrayList<>());
                    eventHandler.schedulePeriodicEvents(anyObject());
                    expectLastCall();
                    eventHandler.scheduleCalendarEvents(anyObject());
                    expectLastCall();
                    schedulerFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    fileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    housekeepingFileService.writeToFile(anyObject());
                    expectLastCall().anyTimes();
                    cmMediatorService.addBackupManager(anyObject());
                    expectLastCall().anyTimes();
                });

        final List<BackupManager> backupManagers = backupManagerRepository.getBackupManagers();
        final List<String> backupManagerIds = backupManagers.stream().map(BackupManager::getBackupManagerId).collect(Collectors.toList());

        assertEquals(1, backupManagerIds.size());
        assertTrue(backupManagerIds.contains(backupManagerId));
    }

    public void mockPersistence(final List<PersistedBackupManager> persistedBackupManagers) {
        mockPersistence(persistedBackupManagers, () -> {});
    }

    public void mockPersistence(final List<PersistedBackupManager> persistedBackupManagers, final Runnable mocks) {
        mocks.run();
        expect(fileService.getBackupManagers()).andReturn(persistedBackupManagers);
        expect(virtualInformationFileService.getVirtualInformation(anyString())).andReturn(Optional.empty()).anyTimes();
        virtualInformationFileService.writeToFile(anyObject());
        expectLastCall().anyTimes();
        expect(periodicEventFileService.createPeriodicEventsDirectory(anyObject(BackupManager.class))).andReturn(true).anyTimes();
        expect(calendarEventFileService.createCalendarEventsDirectory(anyObject(BackupManager.class))).andReturn(true).anyTimes();
        replay(fileService, cmMediatorService, housekeepingFileService, schedulerFileService,
                periodicEventFileService, calendarEventFileService, eventHandler, persistedBackupManager, virtualInformationFileService, sftpServerFileService);
        backupManagerRepository.initializeBackupManagers();
    }

    private Action mockAction(final String backupName, final String backupManagerId, final int startOffset) {
        final Action action = createMock(Action.class);
        expect(action.getBackupName()).andReturn(backupName).anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getBackupManagerId()).andReturn(backupManagerId).anyTimes();
        expect(action.getResult()).andReturn(ResultType.SUCCESS).anyTimes();
        expect(action.getAdditionalInfo()).andReturn("").anyTimes();
        expect(action.getStartTime()).andReturn(OffsetDateTime.now().plus(startOffset, ChronoUnit.MINUTES)).anyTimes();
        expect(action.getCompletionTime()).andReturn(OffsetDateTime.now().plus(startOffset+1, ChronoUnit.MINUTES)).anyTimes();
        return action;
    }

    private PersistedBackupManager createPersistedBackupManager(final String id, final Boolean housekeepingExists, final int maxBackup) {
        final PersistedBackupManager persistedBackupManager = new PersistedBackupManager();
        persistedBackupManager.setBackupManagerId(id);

        expect(sftpServerFileService.getSftpServers(id)).andReturn(new ArrayList<>());

        if (housekeepingExists) {
            expect(housekeepingFileService.getPersistedHousekeepingInformation(persistedBackupManager.getBackupManagerId()))
            .andReturn(new HousekeepingInformation(AUTO_DELETE_ENABLED, maxBackup));

            if (maxBackup < 2) {
                housekeepingFileService.writeToFile(anyObject(Housekeeping.class));
                expectLastCall();
                cmMediatorService.updateHousekeeping(anyObject(Housekeeping.class));
            }
        } else {
            expect(housekeepingFileService.getPersistedHousekeepingInformation(persistedBackupManager.getBackupManagerId()))
            .andThrow(new IndexOutOfBoundsException());
            housekeepingFileService.writeToFile(anyObject(Housekeeping.class));
            expectLastCall();
            cmMediatorService.addHousekeeping(anyObject(Housekeeping.class));
            expectLastCall();
        }
        return persistedBackupManager;
    }

}