/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021

 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.action;

import static com.ericsson.adp.mgmt.backupandrestore.action.ResultType.SUCCESS;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.ADD;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.REMOVE;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.REPLACE;
import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.ScheduledEventHandler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.EtagNotifIdBase;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.MediatorRequest;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.MediatorRequestPatch;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventRequestOrResponse;
import com.ericsson.adp.mgmt.backupandrestore.util.ESAPIValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;

public class MediatorServiceTest {

    private MediatorNotificationHandler service;
    private SftpServerNotificationPatchHandler sftpServerPatchHandler;
    private BackupManagerRepository repository;
    private ScheduledEventHandler eventHandler;
    private BackupManager backupManager;
    private ESAPIValidator esapiValidator;
    private IdValidator idValidator;
    private EtagNotifIdBase etagNotifId;

    private static final String SCHEDULER_PATH = "/ericsson-brm:brm/backup-manager/0/scheduler/";
    private static final String SCHEDULER_ADMIN_STATE_PATH = SCHEDULER_PATH + "admin-state";
    private static final String SCHEDULER_AUTO_EXPORT_URI_PATH = SCHEDULER_PATH + "auto-export-uri";
    private static final String SCHEDULER_SCHEDULED_BACKUP_NAME_PATH = SCHEDULER_PATH + "scheduled-backup-name";

    private static final String HOUSEKEEPING_PATH = "/ericsson-brm:brm/backup-manager/0/housekeeping/auto-delete";
    private static final String PERIODIC_EVENT_PATH = "/ericsson-brm:brm/backup-manager/0/scheduler/periodic-event/0";
    private static final String UPDATE_EVENT_PATH = "/ericsson-brm:brm/backup-manager/0/scheduler/periodic-event/0/hours";
    private static final String UPDATE_EVENT_ID_PATH = "/ericsson-brm:brm/backup-manager/0/scheduler/periodic-event/0/id";


    private static final URI SFTP_USER_LOCALHOST_222_REMOTE = URI.create("sftp://user@localhost:222/remote");

    @Before
    public void setUp() {
        etagNotifId = new EtagNotifIdBase("1111", 10, null);
        repository = createMock(BackupManagerRepository.class);
        eventHandler = createMock(ScheduledEventHandler.class);

        service = new MediatorNotificationHandler();
        service.setEtagNotifIdBase(etagNotifId);
        service.setEventHandler(eventHandler);

        sftpServerPatchHandler = new SftpServerNotificationPatchHandler();
        service.setSftpServerPatchHandler(sftpServerPatchHandler);

        backupManager = createMock(BackupManager.class);
        esapiValidator = new ESAPIValidator();
        idValidator = new IdValidator();
    }

    @Test
    public void handleMediatorRequest_requestToUpdateHousekeeping_createsActionAndPassesToJobExecutor() throws Exception {
        expect(backupManager.getHousekeeping()).andReturn(new Housekeeping("123", null));
        replay(backupManager);

        final MediatorRequest request = getRequest(REPLACE.getStringRepresentation(), HOUSEKEEPING_PATH, "disabled");
        final ActionService actionService = createMock(ActionService.class);

        expect(repository.getBackupManager(0)).andReturn(backupManager);
        expectLastCall();
        expect(actionService.executeAndWait(anyObject(), anyObject())).andReturn(SUCCESS).anyTimes();

        service.setActionService(actionService);
        service.setBackupManagerRepository(repository);
        replay(actionService, repository);

        service.handleMediatorRequest(request);
        verify(actionService, repository);
    }

    @Test
    public void handleMediatorRequest_requestToUpdateScheduler_UpdatesScheduler() throws Exception {
        expect(backupManager.getScheduler()).andReturn(new Scheduler("123", this::persistScheduler)).times(2);
        replay(backupManager);

        final MediatorRequest request = getRequest(REPLACE.getStringRepresentation(), SCHEDULER_ADMIN_STATE_PATH, "locked");
        expect(repository.getBackupManager(0)).andReturn(backupManager).times(1);
        expectLastCall();
        service.setBackupManagerRepository(repository);
        replay(repository);

        service.handleMediatorRequest(request);
        verify(repository);
    }

    @Test
    public void handleMediatorRequest_requestToAddSchedulerField_UpdatesScheduler() throws Exception {
        expect(backupManager.getScheduler()).andReturn(new Scheduler("123", this::persistScheduler)).times(2);
        replay(backupManager);

        final MediatorRequest request = getRequest(ADD.getStringRepresentation(), SCHEDULER_ADMIN_STATE_PATH, "locked");

        expect(repository.getBackupManager(0)).andReturn(backupManager).times(1);
        expectLastCall();
        service.setBackupManagerRepository(repository);
        replay(repository);

        service.handleMediatorRequest(request);
        verify(repository);
    }

    @Test
    public void handleMediatorRequest_requestToUpdateInvalidBackupName() throws Exception {
        final Scheduler scheduler = new Scheduler("123", this::persistExistingSchedulerConfig);
        expect(backupManager.getScheduler()).andReturn(scheduler);
        replay(backupManager);

        expect(repository.getBackupManager(0)).andReturn(backupManager);
        service.setEsapiValidator(esapiValidator);
        service.setIdValidator(idValidator);
        service.setBackupManagerRepository(repository);
        replay(repository);

        final MediatorRequest request = getRequest(ADD.getStringRepresentation(), SCHEDULER_SCHEDULED_BACKUP_NAME_PATH, "tes/t");

        service.handleMediatorRequest(request);
        verify(repository);

        assertEquals("SCHEDULED_BACKUP", scheduler.getScheduledBackupName());
    }

    @Test
    public void handleMediatorRequest_requestToUpdateEmptyBackupName() throws Exception {
        final Scheduler scheduler = new Scheduler("123", this::persistExistingSchedulerConfig);
        expect(backupManager.getScheduler()).andReturn(scheduler);
        replay(backupManager);

        expect(repository.getBackupManager(0)).andReturn(backupManager);
        service.setEsapiValidator(esapiValidator);
        service.setIdValidator(idValidator);
        service.setBackupManagerRepository(repository);
        replay(repository);

        final MediatorRequest request = getRequest(ADD.getStringRepresentation(), SCHEDULER_SCHEDULED_BACKUP_NAME_PATH, "");

        service.handleMediatorRequest(request);
        verify(repository);

        assertEquals("SCHEDULED_BACKUP", scheduler.getScheduledBackupName());
    }

    @Test
    public void handleMediatorRequest_requestToUpdateInvalidURI() throws Exception {
        final Scheduler scheduler = new Scheduler("123", this::persistExistingSchedulerConfig);
        expect(backupManager.getScheduler()).andReturn(scheduler);
        replay(backupManager);

        expect(repository.getBackupManager(0)).andReturn(backupManager);
        service.setEsapiValidator(esapiValidator);
        service.setIdValidator(idValidator);
        service.setBackupManagerRepository(repository);
        replay(repository);

        final MediatorRequest request = getRequest(ADD.getStringRepresentation(), SCHEDULER_AUTO_EXPORT_URI_PATH, "sftp://bro@@23.23");

        service.handleMediatorRequest(request);
        verify(repository);

        assertNull(scheduler.getAutoExportUri());
    }

    @Test
    public void handleMediatorRequest_requestToUpdateEmptyURI() throws Exception {
        final Scheduler scheduler = new Scheduler("123", this::persistExistingSchedulerConfig);
        expect(backupManager.getScheduler()).andReturn(scheduler);
        replay(backupManager);

        expect(repository.getBackupManager(0)).andReturn(backupManager);
        service.setEsapiValidator(esapiValidator);
        service.setIdValidator(idValidator);
        service.setBackupManagerRepository(repository);
        replay(repository);

        final MediatorRequest request = getRequest(ADD.getStringRepresentation(), SCHEDULER_AUTO_EXPORT_URI_PATH, "");

        service.handleMediatorRequest(request);
        verify(repository);

        assertNull(scheduler.getAutoExportUri());
    }

    @Test
    public void handleMediatorRequest_requestToUpdateValidURI() throws Exception {
        final Scheduler scheduler = new Scheduler("123", this::persistScheduler);
        expect(backupManager.getScheduler()).andReturn(scheduler).times(2);
        replay(backupManager);

        expect(repository.getBackupManager(0)).andReturn(backupManager).times(1);
        service.setEsapiValidator(esapiValidator);
        service.setIdValidator(idValidator);
        service.setBackupManagerRepository(repository);
        replay(repository);

        final MediatorRequest request = getRequest(ADD.getStringRepresentation(), SCHEDULER_AUTO_EXPORT_URI_PATH, SFTP_USER_LOCALHOST_222_REMOTE);

        service.handleMediatorRequest(request);
        verify(repository);

        assertEquals(SFTP_USER_LOCALHOST_222_REMOTE, scheduler.getAutoExportUri());
    }

    @Test
    public void handleMediatorRequest_requestToAddEvent_callsCreatePeriodicEvent() throws Exception {
        eventHandler = createMock(ScheduledEventHandler.class);

        final MediatorRequest request = getRequest(ADD.getStringRepresentation(), PERIODIC_EVENT_PATH, "{id=Id1, days=0, minutes=1, weeks=0, hours=0}");

        expect(repository.getBackupManager(0)).andReturn(backupManager);
        expectLastCall();

        expect(eventHandler.createPeriodicEvent(anyObject(BackupManager.class), anyObject(PeriodicEventRequestOrResponse.class), eq(false))).andReturn("Id1");
        expectLastCall();

        service.setEventHandler(eventHandler);
        service.setBackupManagerRepository(repository);
        replay(repository, eventHandler);

        service.handleMediatorRequest(request);
        verify(repository, eventHandler);
    }

    @Test
    public void handleMediatorRequest_requestToRemoveEvent_deletesPeriodicEvent() throws Exception {
        final PeriodicEvent event = new PeriodicEvent(PERIODIC_EVENT_PATH, this::persistEvent);
        event.setEventId("Id1");
        event.setHours(0);

        final Scheduler scheduler = new Scheduler("123", this::persistScheduler);
        scheduler.addPeriodicEvent(event);
        expect(backupManager.getScheduler()).andReturn(scheduler);
        replay(backupManager);
        eventHandler = createMock(ScheduledEventHandler.class);

        final MediatorRequest request = getRequest(REMOVE.getStringRepresentation(), PERIODIC_EVENT_PATH, null);

        expect(repository.getBackupManager(0)).andReturn(backupManager);
        expectLastCall();

        eventHandler.deletePeriodicEvent(anyObject(BackupManager.class), anyObject(PeriodicEvent.class), anyBoolean());
        expectLastCall();

        service.setEventHandler(eventHandler);
        service.setBackupManagerRepository(repository);
        replay(repository, eventHandler);

        service.handleMediatorRequest(request);
        verify(repository, eventHandler);
    }

    @Test
    public void handleMediatorRequest_requestToUpdateEvent_callUpdatePeriodicEvent() throws Exception {
        final PeriodicEvent event = new PeriodicEvent(PERIODIC_EVENT_PATH, this::persistEvent);
        event.setEventId("Id1");
        event.setHours(0);

        final Scheduler scheduler = new Scheduler("123", this::persistScheduler);
        scheduler.addPeriodicEvent(event);
        expect(backupManager.getScheduler()).andReturn(scheduler);
        replay(backupManager);
        eventHandler = createMock(ScheduledEventHandler.class);
        final MediatorRequest request = getRequest(REPLACE.getStringRepresentation(), UPDATE_EVENT_PATH, 1);

        expect(repository.getBackupManager(0)).andReturn(backupManager);
        expectLastCall();

        eventHandler.updatePeriodicEvent(anyObject(BackupManager.class), anyString(), anyObject(PeriodicEventRequestOrResponse.class));
        expectLastCall();

        service.setEventHandler(eventHandler);
        service.setBackupManagerRepository(repository);
        replay(repository, eventHandler);

        service.handleMediatorRequest(request);
        verify(repository, eventHandler);
    }

    @Test
    public void handleMediatorRequest_requestToUpdateEventId_callUpdatePeriodicEvent() throws Exception {
        final PeriodicEvent event = new PeriodicEvent(PERIODIC_EVENT_PATH, this::persistEvent);
        event.setEventId("Id1");
        event.setHours(0);

        final Scheduler scheduler = new Scheduler("123", this::persistScheduler);
        scheduler.addPeriodicEvent(event);
        expect(backupManager.getScheduler()).andReturn(scheduler);
        replay(backupManager);
        eventHandler = createMock(ScheduledEventHandler.class);
        final MediatorRequest request = getRequest(REPLACE.getStringRepresentation(), UPDATE_EVENT_ID_PATH, "Id2");

        expect(repository.getBackupManager(0)).andReturn(backupManager);
        expectLastCall();

        eventHandler.updatePeriodicEventId(anyObject(BackupManager.class), anyObject(PeriodicEvent.class), anyString());
        expectLastCall();

        service.setEventHandler(eventHandler);
        service.setBackupManagerRepository(repository);
        replay(repository, eventHandler);

        service.handleMediatorRequest(request);
        verify(repository, eventHandler);

    }

    @Test
    public void handleMediatorRequest_requestToAddNewValueToAFieldInEvent_callUpdatePeriodicEvent() throws Exception {
        final PeriodicEvent event = new PeriodicEvent(PERIODIC_EVENT_PATH, this::persistEvent);
        event.setEventId("Id1");
        event.setHours(0);

        final Scheduler scheduler = new Scheduler("123", this::persistScheduler);
        scheduler.addPeriodicEvent(event);
        expect(backupManager.getScheduler()).andReturn(scheduler);
        replay(backupManager);
        eventHandler = createMock(ScheduledEventHandler.class);

        final MediatorRequest request = getRequest(ADD.getStringRepresentation(), UPDATE_EVENT_PATH, 1);

        expect(repository.getBackupManager(0)).andReturn(backupManager);
        expectLastCall();

        eventHandler.updatePeriodicEvent(anyObject(BackupManager.class), anyString(), anyObject(PeriodicEventRequestOrResponse.class));
        expectLastCall();
        service.setEventHandler(eventHandler);
        service.setBackupManagerRepository(repository);
        replay(repository, eventHandler);

        service.handleMediatorRequest(request);
        verify(repository, eventHandler);
    }

    @Test
    public void handleMediatorRequest_requestToDeleteStopTime_callUpdatePeriodicEvent() {
        final PeriodicEvent event = new PeriodicEvent(PERIODIC_EVENT_PATH, this::persistEvent);
        event.setEventId("Id1");
        event.setHours(0);
        event.setStartTime(DateTimeUtils.convertToString(OffsetDateTime.now().minusDays(1)));
        event.setStopTime(DateTimeUtils.convertToString(OffsetDateTime.now()));

        final Scheduler scheduler = new Scheduler("123", this::persistScheduler);
        scheduler.addPeriodicEvent(event);
        expect(backupManager.getScheduler()).andReturn(scheduler);
        replay(backupManager);
        eventHandler = createMock(ScheduledEventHandler.class);

        final MediatorRequest request = getRequest(REMOVE.getStringRepresentation(), PERIODIC_EVENT_PATH + "/" + "stop-time", null);

        expect(repository.getBackupManager(0)).andReturn(backupManager);
        expectLastCall();

        eventHandler.updatePeriodicEvent(anyObject(BackupManager.class), anyString(), anyObject(PeriodicEventRequestOrResponse.class));
        expectLastCall();
        service.setEventHandler(eventHandler);
        service.setBackupManagerRepository(repository);
        replay(repository, eventHandler);

        service.handleMediatorRequest(request);
        verify(repository, eventHandler);
    }

    private MediatorRequest getRequest(final String operation, final String path, final Object value) {
        final MediatorRequestPatch patch = new MediatorRequestPatch();
        patch.setOp(operation);
        patch.setPath(path);
        patch.setValue(value);

        final List<MediatorRequestPatch> patchList = new ArrayList<>();
        patchList.add(patch);

        final MediatorRequest request = new MediatorRequest();
        request.setChangedBy("cmyp");
        request.setPatch(patchList);
        request.setBaseETag("1111");
        request.setConfigETag("1112");
        request.setNotifId(11);
        return request;
    }

    private void persistExistingSchedulerConfig(final Scheduler scheduler) {
        System.out.println("Persisted Existing Scheduler Configuration");
    }

    private void persistScheduler(final Scheduler scheduler) {
        System.out.println("Persisted Scheduler");
    }

    private void persistEvent(final PeriodicEvent event) {
        System.out.println("Persisted event");
    }

}
