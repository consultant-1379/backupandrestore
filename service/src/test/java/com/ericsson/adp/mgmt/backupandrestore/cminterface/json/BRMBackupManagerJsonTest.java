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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.easymock.EasyMock;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.util.YangEnabledDisabled;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BRMBackupManagerJsonTest {

    private final String SCHEDULED_BACKUP = "scheduled";

    @Test
    public void new_backupManager_createsFullRepresentationOfBackupManagerToBeSentToCM() throws Exception {
        final BRMBackupManagerJson brmBackupManager = new BRMBackupManagerJson(mockBackupManager(Arrays.asList(mockAction("1"))));

        assertEquals("A", brmBackupManager.getBackupManagerId());
        assertEquals("B", brmBackupManager.getBackupDomain());
        assertEquals("C", brmBackupManager.getBackupType());
        assertEquals(AdminState.UNLOCKED.getCmRepresentation(),
                brmBackupManager.getScheduler().getAdminState().getCmRepresentation());
        assertNotNull(YangEnabledDisabled.ENABLED.toString(), brmBackupManager.getHousekeeping().getAutoDelete());

        assertEquals(1, brmBackupManager.getBackups().size());
        final BRMBackupJson brmBackup = brmBackupManager.getBackups().get(0);
        assertEquals("D", brmBackup.getBackupId());
        assertEquals("E", brmBackup.getName());
        assertEquals("1984-01-02T03:04:05Z", brmBackup.getCreationTime());
        assertEquals(BackupCreationType.SCHEDULED, brmBackup.getCreationType());
        assertEquals("backup-corrupted", brmBackup.getCmRepresentationOfStatus());
        assertTrue("backup-corrupted", brmBackup.getProgressReports() == null);

        assertEquals(1, brmBackupManager.getProgressReports().size());
        final BRMProgressReportJson progressReport = brmBackupManager.getProgressReports().get(0);

        assertEquals(Integer.valueOf(1), progressReport.getId());
        assertEquals("1", progressReport.getActionId());
        assertEquals(ActionType.CREATE_BACKUP, progressReport.getName());
        assertEquals("add", progressReport.getAdditionalInfo());
        assertEquals("pro", progressReport.getProgressInfo());
        assertEquals(Double.valueOf(0.99), progressReport.getProgressPercentage());
        assertEquals(99, progressReport.getProgressPercentageAsInteger());
        assertEquals("not-available", progressReport.getCmRepresentationOfResult());
        assertEquals("", progressReport.getResultInfo());
        assertEquals("finished", progressReport.getCmRepresentationOfState());
        assertEquals("1985-01-02T03:04:05Z", progressReport.getStartTime());
        assertEquals("1986-01-02T03:04:05Z", progressReport.getLastUpdateTime());
        assertEquals("1987-01-02T03:04:05Z", progressReport.getCompletionTime());
    }

    @Test
    public void new_BRMProgressReportJson_fromJson_update() throws Exception {
        final BRMProgressReportJson progressReport = new ObjectMapper()
                .readValue(getBRMProgressReportJson().toString(),BRMProgressReportJson.class);

        progressReport.setActionId("2");
        assertEquals(Integer.valueOf(2), progressReport.getId());
        assertEquals("2", progressReport.getActionId());
        assertEquals(ActionType.CREATE_BACKUP, progressReport.getName());
    }

    @Test
    public void new_backupManagerWithMultipleActionsBelongingToItAndToItsBackups_onlyAddsLastOneBelongingToBackupManagerAsProgressReport() throws Exception {
        final BRMBackupManagerJson brmBackupManager = new BRMBackupManagerJson(mockBackupManager(Arrays.asList(mockAction("1","",ActionType.EXPORT),mockAction("2"), mockAction("3", "D"), mockAction("4", "otherBackup"))));

        assertEquals(1, brmBackupManager.getProgressReports().size());
        final BRMProgressReportJson progressReport = brmBackupManager.getProgressReports().get(0);

        assertEquals(Integer.valueOf(2), progressReport.getId());

        final BRMBackupJson brmBackup = brmBackupManager.getBackups().get(0);
        assertEquals(1, brmBackup.getProgressReports().size());
        assertEquals(Integer.valueOf(3), brmBackup.getProgressReports().get(0).getId());
    }

    @Test
    public void new_backupManagerWithMultipleActionsBelongingToItAndHousekeepingActions_onlyAddsLastOneBelongingToBackupManagerAndOmitHousekeeping() throws Exception {
        final BRMBackupManagerJson brmBackupManager = new BRMBackupManagerJson(mockBackupManager(Arrays.asList(mockAction("1"), mockAction("2", "D"), mockHousekeepingAction("3"))));

        assertEquals(1, brmBackupManager.getProgressReports().size());
        final BRMProgressReportJson progressReport = brmBackupManager.getProgressReports().get(0);

        assertEquals(Integer.valueOf(1), progressReport.getId());

        final BRMBackupJson brmBackup = brmBackupManager.getBackups().get(0);
        assertEquals(1, brmBackup.getProgressReports().size());
        assertEquals(Integer.valueOf(2), brmBackup.getProgressReports().get(0).getId());
    }

    @Test
    public void new_backupManagerWithMultipleActionsBelongingToItAndScheduledBackupActions_onlyAddsLastOneBelongingToBackupManagerScheduledBackup() throws Exception {
        final BRMBackupManagerJson brmBackupManager = new BRMBackupManagerJson(mockBackupManager(Arrays.asList(mockAction("1"), mockAction("2", "D"), mockAction("3", SCHEDULED_BACKUP))));

        assertEquals(1, brmBackupManager.getProgressReports().size());
        final BRMProgressReportJson progressReport = brmBackupManager.getProgressReports().get(0);

        assertEquals(Integer.valueOf(3), progressReport.getId());

        final BRMBackupJson brmBackup = brmBackupManager.getBackups().get(0);
        assertEquals(1, brmBackup.getProgressReports().size());
        assertEquals(Integer.valueOf(2), brmBackup.getProgressReports().get(0).getId());
    }

    private BackupManager mockBackupManager(final List<Action> actions) {
        final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
        EasyMock.expect(backupManager.getBackupManagerId()).andReturn("A");
        EasyMock.expect(backupManager.getBackupDomain()).andReturn("B");
        EasyMock.expect(backupManager.getBackupType()).andReturn("C");
        EasyMock.expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(Arrays.asList(mockBackup()));
        EasyMock.expect(backupManager.getActions()).andReturn(actions).anyTimes();
        EasyMock.expect(backupManager.getHousekeeping()).andReturn(new Housekeeping("A", null));
        EasyMock.expect(backupManager.getScheduler()).andReturn(new Scheduler("A", null));
        EasyMock.expect(backupManager.getSftpServers()).andReturn(new ArrayList<>());
        EasyMock.replay(backupManager);
        return backupManager;
    }

    private Backup mockBackup() {
        final Backup backup = EasyMock.createMock(Backup.class);
        EasyMock.expect(backup.getBackupId()).andReturn("D");
        EasyMock.expect(backup.getName()).andReturn("E");
        EasyMock.expect(backup.getCreationTime()).andReturn(getDateTime(1984, 1, 2, 3, 4, 5)).anyTimes();
        EasyMock.expect(backup.getCreationType()).andReturn(BackupCreationType.SCHEDULED).anyTimes();
        EasyMock.expect(backup.getStatus()).andReturn(BackupStatus.CORRUPTED).anyTimes();
        EasyMock.expect(backup.getSoftwareVersions()).andReturn(Arrays.asList()).anyTimes();
        EasyMock.expect(backup.getBackupManagerId()).andReturn("666").anyTimes();
        EasyMock.expect(backup.getUserLabel()).andReturn("");
        EasyMock.replay(backup);
        return backup;
    }

    private Action mockAction(final String id) {
        return mockAction(id, null);
    }

    private Action mockAction(final String id, final String backup) {
        return mockAction(id, backup, ActionType.CREATE_BACKUP);
    }

    private Action mockAction(final String id, final String backup, ActionType actionType) {
        final Action action = EasyMock.createMock(Action.class);
        EasyMock.expect(action.getActionId()).andReturn(id).anyTimes();
        EasyMock.expect(action.getName()).andReturn(actionType).anyTimes();
        EasyMock.expect(action.getAdditionalInfo()).andReturn("add").anyTimes();
        EasyMock.expect(action.getProgressInfo()).andReturn("pro").anyTimes();
        EasyMock.expect(action.getProgressPercentage()).andReturn(0.99).anyTimes();
        EasyMock.expect(action.getResult()).andReturn(ResultType.NOT_AVAILABLE).anyTimes();
        EasyMock.expect(action.getResultInfo()).andReturn("").anyTimes();
        EasyMock.expect(action.getState()).andReturn(ActionStateType.FINISHED).anyTimes();
        EasyMock.expect(action.getStartTime()).andReturn(getDateTime(1985, 1, 2, 3, 4, 5)).anyTimes();
        EasyMock.expect(action.getLastUpdateTime()).andReturn(getDateTime(1986, 1, 2, 3, 4, 5)).anyTimes();
        EasyMock.expect(action.getCompletionTime()).andReturn(getDateTime(1987, 1, 2, 3, 4, 5)).anyTimes();
        EasyMock.expect(action.getCopyOfMessages()).andReturn(Arrays.asList("")).anyTimes();
        EasyMock.expect(action.getAllMessagesAsSingleString()).andReturn("").anyTimes();
        expect(action.hasMessages()).andReturn(true).anyTimes();

        if(backup == null) {
            EasyMock.expect(action.isRestoreOrExport()).andReturn(false);
            EasyMock.expect(action.belongsToBackup(EasyMock.anyObject())).andReturn(false).anyTimes();
            EasyMock.expect(action.isPartOfHousekeeping()).andReturn(false);
            EasyMock.expect(action.isScheduledEvent()).andReturn(false).anyTimes();
        } else if(backup == SCHEDULED_BACKUP) {
            EasyMock.expect(action.isScheduledEvent()).andReturn(true).anyTimes();
            EasyMock.expect(action.isRestoreOrExport()).andReturn(false).anyTimes();
            EasyMock.expect(action.belongsToBackup(EasyMock.anyObject())).andReturn(false).anyTimes();
            EasyMock.expect(action.isPartOfHousekeeping()).andReturn(false);
        } else {
            EasyMock.expect(action.isRestoreOrExport()).andReturn(true);
            EasyMock.expect(action.belongsToBackup(backup)).andReturn(true).anyTimes();
            EasyMock.expect(action.belongsToBackup(EasyMock.anyObject())).andReturn(false).anyTimes();
            EasyMock.expect(action.isPartOfHousekeeping()).andReturn(false);
            EasyMock.expect(action.isScheduledEvent()).andReturn(false).anyTimes();
        }

        EasyMock.replay(action);
        return action;
    }

    private Action mockHousekeepingAction(final String id) {
        final Action action = EasyMock.createMock(Action.class);
        EasyMock.expect(action.getActionId()).andReturn(id).anyTimes();
        EasyMock.expect(action.getName()).andReturn(ActionType.HOUSEKEEPING_DELETE_BACKUP);
        EasyMock.expect(action.belongsToBackup(EasyMock.anyObject())).andReturn(false).anyTimes();
        EasyMock.expect(action.isRestoreOrExport()).andReturn(false);
        EasyMock.expect(action.isPartOfHousekeeping()).andReturn(true);
        EasyMock.expect(action.isScheduledEvent()).andReturn(false);

        EasyMock.replay(action);
        return action;
    }

    protected JSONObject getBRMProgressReportJson() throws JSONException {
        final JSONObject brmProgressReportJson = new JSONObject();
        brmProgressReportJson.put("action-id","101");
        brmProgressReportJson.put("action-name",ActionType.CREATE_BACKUP);
        brmProgressReportJson.put("progress-info","INFO");
        brmProgressReportJson.put("progress-percentage",80);
        brmProgressReportJson.put("result-info","result");
        brmProgressReportJson.put("time-action-completed","00:00");
        brmProgressReportJson.put("time-of-last-status-update","00:00");
        return brmProgressReportJson;
    }

    private OffsetDateTime getDateTime(final int year, final int month, final int dayOfMonth, final int hour, final int minute, final int second) {
        final LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
        final ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(dateTime);
        return OffsetDateTime.of(dateTime, offset);
    }
}
