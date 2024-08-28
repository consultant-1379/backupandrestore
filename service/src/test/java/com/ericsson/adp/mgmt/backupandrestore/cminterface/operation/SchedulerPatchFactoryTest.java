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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.operation;

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState.LOCKED;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState.UNLOCKED;
import static com.ericsson.adp.mgmt.backupandrestore.util.YangEnabledDisabled.DISABLED;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMProgressReportJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMSchedulerJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.YangEnabledDisabled;

public class SchedulerPatchFactoryTest {

    private static final String SCHEDULER_URL = "/ericsson-brm:brm/backup-manager/6/scheduler";
    private BackupManagerPatchFactory backupManagerPatchFactory;
    private BackupManagerRepository backupManagerRepository;
    private SchedulerPatchFactory schedulerPatchFactory;

    @Before
    public void setup() {
        backupManagerPatchFactory = EasyMock.createMock(BackupManagerPatchFactory.class);
        backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);
        schedulerPatchFactory = new SchedulerPatchFactory();
        EasyMock.expect(backupManagerPatchFactory.getPathToBackupManager("666")).andReturn("6");
        EasyMock.expect(backupManagerRepository.getBackupManager("666")).andReturn(mockBackupManager());
        EasyMock.replay(backupManagerPatchFactory, backupManagerRepository);
        schedulerPatchFactory.setBackupManagerPatchFactory(backupManagerPatchFactory);
        schedulerPatchFactory.setBackupManagerRepository(backupManagerRepository);
    }

    @Test
    public void getPatchToAddScheduler_schedulerInformation_patchToAddIt() {
        final Scheduler scheduler = new Scheduler("666", null);
        scheduler.setAdminState(UNLOCKED);
        scheduler.setMostRecentlyCreatedAutoBackup("SCHEDULED_BACKUP_2020-09-30T12:23:22");
        scheduler.setNextScheduledTime("2020-09-30T11:23:22");
        scheduler.setAutoExport(YangEnabledDisabled.DISABLED);
        final AddSchedulerPatch patch = schedulerPatchFactory.getPatchToAddScheduler(scheduler);
        final PatchRequest json = patch.toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals("add", json.getOperations().get(0).getOperation());
        assertEquals(SCHEDULER_URL, json.getOperations().get(0).getPath());

        final BRMSchedulerJson brmSchedulerJson = (BRMSchedulerJson) json.getOperations().get(0).getValue();
        assertEquals(UNLOCKED.getCmRepresentation(), brmSchedulerJson.getAdminStateString());
        assertEquals("SCHEDULED_BACKUP", brmSchedulerJson.getScheduledBackupName());
        assertEquals("SCHEDULED_BACKUP_2020-09-30T12:23:22", brmSchedulerJson.getMostRecentlyCreatedAutoBackup());
        assertEquals("2020-09-30T11:23:22", brmSchedulerJson.getNextScheduledTime());
        assertEquals(DISABLED.toString(), brmSchedulerJson.getAutoExportString());
    }

    @Test
    public void getPatchToUpdateScheduler_schedulerInformation_patchToUpdateIt() {
        final Scheduler scheduler = new Scheduler("666", null);
        scheduler.setAdminState(AdminState.LOCKED);

        final PeriodicEvent event = new PeriodicEvent("666", null);
        event.setMinutes(5);
        event.setHours(0);
        event.setEventId("id1");
        event.setStartTime(DateTimeUtils.convertToString(OffsetDateTime.now()));
        scheduler.addPeriodicEvent(event);

        final UpdateSchedulerPatch patch = schedulerPatchFactory.getPatchToUpdateScheduler(scheduler);
        final PatchRequest json = patch.toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals("replace", json.getOperations().get(0).getOperation());
        assertEquals(SCHEDULER_URL, json.getOperations().get(0).getPath());
        final BRMSchedulerJson schedulerJson = (BRMSchedulerJson) json.getOperations().get(0).getValue();
        assertEquals(LOCKED.getCmRepresentation(), schedulerJson.getAdminStateString());
        assertEquals("SCHEDULED_BACKUP", schedulerJson.getScheduledBackupName());
        assertNull(schedulerJson.getMostRecentlyCreatedAutoBackup());
        assertNull(schedulerJson.getNextScheduledTime());
        assertNull(schedulerJson.getAutoExportUriString());
        assertNull(schedulerJson.getAutoExportPassword());
        assertEquals(Integer.valueOf(0), schedulerJson.getPeriodicEvents().get(0).getHours());
        assertEquals(Integer.valueOf(5), schedulerJson.getPeriodicEvents().get(0).getMinutes());
        assertEquals(1, schedulerJson.getProgressReports().size());

        final BRMProgressReportJson reportJson = schedulerJson.getProgressReports().get(0);
        assertEquals("1", reportJson.getActionId());
    }

    private BackupManager mockBackupManager() {
        final List<Action> actions = new ArrayList<>();
        actions.add(mockAction(true));
        final BackupManager brm = EasyMock.createMock(BackupManager.class);
        EasyMock.expect(brm.getActions()).andReturn(actions);
        EasyMock.replay(brm);
        return brm;
    }

    private Action mockAction(final boolean isScheduledEvent) {
        final Action action = EasyMock.createMock(Action.class);
        EasyMock.expect(action.isRestoreOrExport()).andReturn(false);
        EasyMock.expect(action.getBackupManagerId()).andReturn("666");
        EasyMock.expect(action.getBackupName()).andReturn("qqqqqqqqqq");
        EasyMock.expect(action.getActionId()).andReturn("1");
        EasyMock.expect(action.getName()).andReturn(ActionType.DELETE_BACKUP);
        EasyMock.expect(action.getAdditionalInfo()).andReturn("2");
        EasyMock.expect(action.getProgressInfo()).andReturn("3");
        EasyMock.expect(action.getProgressPercentage()).andReturn(0.04);
        EasyMock.expect(action.getResult()).andReturn(ResultType.NOT_AVAILABLE);
        EasyMock.expect(action.getResultInfo()).andReturn("");
        EasyMock.expect(action.getState()).andReturn(ActionStateType.FINISHED);
        EasyMock.expect(action.getStartTime()).andReturn(getDateTime(1984, 1, 2, 3, 4, 5));
        EasyMock.expect(action.getLastUpdateTime()).andReturn(getDateTime(1984, 1, 2, 3, 4, 6));
        EasyMock.expect(action.getCompletionTime()).andReturn(getDateTime(1984, 1, 2, 3, 4, 7)).anyTimes();
        EasyMock.expect(action.getCopyOfMessages()).andReturn(Arrays.asList(""));
        EasyMock.expect(action.getAllMessagesAsSingleString()).andReturn("");
        EasyMock.expect(action.isScheduledEvent()).andReturn(isScheduledEvent);
        expect(action.hasMessages()).andReturn(true);
        EasyMock.replay(action);
        return action;
    }


    private OffsetDateTime getDateTime(final int year, final int month, final int dayOfMonth, final int hour, final int minute, final int second) {
        final LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
        final ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(dateTime);
        return OffsetDateTime.of(dateTime, offset);
    }

}
