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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.operation;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMMessage;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMProgressReportJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchRequest;

public class ProgressReportPatchFactoryTest {

    private ProgressReportPatchFactory progressReportPatchFactory;

    @Before
    public void setup() {
        final BackupManagerPatchFactory backupManagerPatchFactory = createMock(BackupManagerPatchFactory.class);
        expect(backupManagerPatchFactory.getPathToBackupManager("999")).andReturn("9").anyTimes();

        final BackupPatchFactory backupPatchFactory = createMock(BackupPatchFactory.class);
        expect(backupPatchFactory.getPathToBackup("999", "qqqqqqqqqq")).andReturn("9/backup/1").anyTimes();
        expect(backupPatchFactory.getPathToPostBackup("999", "1")).andReturn("9/backup/1").anyTimes();
        replay(backupPatchFactory, backupManagerPatchFactory);

        progressReportPatchFactory = new ProgressReportPatchFactory();
        progressReportPatchFactory.setBackupManagerPatchFactory(backupManagerPatchFactory);
        progressReportPatchFactory.setBackupPatchFactory(backupPatchFactory);
    }

    @Test
    public void getPathToAddProgressReport_actionBelongingToABackupManager() throws Exception {
        final String path = progressReportPatchFactory.getPathToProgressReportBackupManager(mockAction(false, false));
        assertEquals("9/progress-report", path);
    }

    @Test
    public void getMessageToUploadProgressReport_actionBelongingToABackup() throws Exception {
        final BackupManagerRepository backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);
        final BackupPatchFactory backupFactory = new BackupPatchFactory();
        final ProgressReportPatchFactory progressReportPatchFactory = new ProgressReportPatchFactory();
        final BackupManagerPatchFactory backupManagerPatchFactory = new BackupManagerPatchFactory();
        expect (backupManagerRepository.getIndex("999")).andReturn(0);

        backupManagerPatchFactory.setBackupManagerRepository(backupManagerRepository);
        progressReportPatchFactory.setBackupManagerPatchFactory(backupManagerPatchFactory);
        // backupFactory.setProgressReportPatchFactory(progressReportPatchFactory);
        backupFactory.setBackupManagerPatchFactory(backupManagerPatchFactory);
        progressReportPatchFactory.setBackupPatchFactory(backupFactory);

        replay (backupManagerRepository);
        //final CMMMessage cmmMessage = progressReportPatchFactory.getMessageToUploadProgressReport(mockAction(false, false), -1);
        // final String path = cmmMessage.getConfigurationPatch().getPath();
        // assertEquals("/ericsson-brm:brm/backup-manager/0/progress-report", path);
    }

    @Test
    public void getPathToAddProgressReport_actionBelongingToABackup() throws Exception {
        final String path = progressReportPatchFactory.getPathToProgressReportBackup(mockAction(false, false));
        assertEquals("9/backup/1/progress-report", path);
    }

    @Test
    public void getPatchToAddProgressReport_actionBelongingToABackupManager_patchToAddIt() throws Exception {
        final List<ProgressReportPatch> patch = progressReportPatchFactory.getPatchToAddProgressReport(mockAction(false, false));

        final PatchRequest json = patch.get(0).toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals(1, patch.size());
        assertEquals("add", json.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/9/progress-report", json.getOperations().get(0).getPath());
        BRMProgressReportJson[] progressReportArr = new BRMProgressReportJson[1];
        progressReportArr = (BRMProgressReportJson[]) json.getOperations().get(0).getValue();
        BRMProgressReportJson progressReport = progressReportArr[0];
        assertEquals(Integer.valueOf(1), progressReport.getId());
        assertEquals(ActionType.CREATE_BACKUP, progressReport.getName());
        assertEquals("2", progressReport.getAdditionalInfo());
        assertEquals("3", progressReport.getProgressInfo());
        assertEquals(Double.valueOf(0.04), progressReport.getProgressPercentage());
        assertEquals(4, progressReport.getProgressPercentageAsInteger());
        assertEquals("not-available", progressReport.getCmRepresentationOfResult());
        assertEquals("", progressReport.getResultInfo());
        assertEquals("finished", progressReport.getCmRepresentationOfState());
        assertEquals("1984-01-02T03:04:05Z", progressReport.getStartTime());
        assertEquals("1984-01-02T03:04:06Z", progressReport.getLastUpdateTime());
        assertEquals("1984-01-02T03:04:07Z", progressReport.getCompletionTime());
        assertEquals(1, progressReport.getAdditionalInfoAsArray().length);
        assertEquals("2", progressReport.getAdditionalInfoAsArray()[0]);

    }

    @Test
    public void getPatchToAddProgressReport_actionBelongingToABackup_patchToAddIt() throws Exception {
        final List<ProgressReportPatch> patch = progressReportPatchFactory.getPatchToAddProgressReport(mockAction(true, false));

        final PatchRequest json = patch.get(0).toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals(1, patch.size());
        assertEquals("add", json.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/9/backup/1/progress-report", json.getOperations().get(0).getPath());

        BRMProgressReportJson[] progressReportArr = new BRMProgressReportJson[1];
        progressReportArr = (BRMProgressReportJson[]) json.getOperations().get(0).getValue();

        final BRMProgressReportJson progressReport = (BRMProgressReportJson) progressReportArr[0];
        assertEquals(Integer.valueOf(1), progressReport.getId());
    }

    @Test
    public void getPatchToAddProgressReport_actionIsScheduledEvent_patchToAddIt() throws Exception {
        final List<ProgressReportPatch> patch = progressReportPatchFactory.getPatchToAddProgressReport(mockAction(false, true));

        final PatchRequest backupManagerPathjson = patch.get(0).toJson();
        final PatchRequest SchedulerPathjson = patch.get(1).toJson();

        assertEquals(1, backupManagerPathjson.getOperations().size());
        assertEquals(1, SchedulerPathjson.getOperations().size());

        assertEquals(2, patch.size());
        assertEquals("add", backupManagerPathjson.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/9/progress-report", backupManagerPathjson.getOperations().get(0).getPath());

        assertEquals("replace", SchedulerPathjson.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/9/scheduler/progress-report/0", SchedulerPathjson.getOperations().get(0).getPath());

        BRMProgressReportJson[] progressReportArr = new BRMProgressReportJson[1];
        progressReportArr = (BRMProgressReportJson[]) backupManagerPathjson.getOperations().get(0).getValue();

        final BRMProgressReportJson progressReport = (BRMProgressReportJson) progressReportArr[0];
        assertEquals(Integer.valueOf(1), progressReport.getId());
    }

    @Test
    public void getPatchToUpdateProgressReport_actionBelongingToABackupManager_patchToUpdateIt() throws Exception {
        final List<UpdateProgressReportPatch> patch = progressReportPatchFactory.getPatchToUpdateProgressReport(mockAction(false, false));

        final PatchRequest json = patch.get(0).toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals(1, patch.size());
        assertEquals("replace", json.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/9/progress-report/0", json.getOperations().get(0).getPath());

        final BRMProgressReportJson progressReport = (BRMProgressReportJson) json.getOperations().get(0).getValue();
        assertEquals(Integer.valueOf(1), progressReport.getId());
    }

    @Test
    public void getPatchToUpdateProgressReport_actionBelongingToABackup_patchToUpdateIt() throws Exception {
        final List<UpdateProgressReportPatch> patch = progressReportPatchFactory.getPatchToUpdateProgressReport(mockAction(true, false));

        final PatchRequest json = patch.get(0).toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals(1, patch.size());
        assertEquals("replace", json.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/9/backup/1/progress-report/0", json.getOperations().get(0).getPath());

        final BRMProgressReportJson progressReport = (BRMProgressReportJson) json.getOperations().get(0).getValue();
        assertEquals(Integer.valueOf(1), progressReport.getId());
    }

    @Test
    public void getPatchToUpdateProgressReport_actionIsScheduledEvent_patchToUpdateIt() throws Exception {
        final List<UpdateProgressReportPatch> patch = progressReportPatchFactory.getPatchToUpdateProgressReport(mockAction(false, true));

        final PatchRequest backupManagerPathjson = patch.get(0).toJson();
        final PatchRequest SchedulerPathjson = patch.get(1).toJson();

        assertEquals(1, backupManagerPathjson.getOperations().size());
        assertEquals(1, SchedulerPathjson.getOperations().size());

        assertEquals(2, patch.size());
        assertEquals("replace", backupManagerPathjson.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/9/progress-report/0", backupManagerPathjson.getOperations().get(0).getPath());

        assertEquals("replace", SchedulerPathjson.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/9/scheduler/progress-report/0", SchedulerPathjson.getOperations().get(0).getPath());

        final BRMProgressReportJson progressReport = (BRMProgressReportJson) backupManagerPathjson.getOperations().get(0).getValue();
        assertEquals(Integer.valueOf(1), progressReport.getId());
    }

    private Action mockAction(final boolean belongsToABackup, final boolean isScheduledEvent) {
        final Action action = createMock(Action.class);
        expect(action.isRestoreOrExport()).andReturn(belongsToABackup).anyTimes();
        expect(action.getBackupManagerId()).andReturn("999").anyTimes();
        expect(action.getBackupName()).andReturn("qqqqqqqqqq");
        expect(action.getActionId()).andReturn("1").anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getAdditionalInfo()).andReturn("2").anyTimes();
        expect(action.getProgressInfo()).andReturn("3").anyTimes();
        expect(action.getProgressPercentage()).andReturn(0.04).anyTimes();
        expect(action.getResult()).andReturn(ResultType.NOT_AVAILABLE).anyTimes();
        expect(action.getResultInfo()).andReturn("").anyTimes();
        expect(action.getState()).andReturn(ActionStateType.FINISHED).anyTimes();
        expect(action.getStartTime()).andReturn(getDateTime(1984, 1, 2, 3, 4, 5)).anyTimes();
        expect(action.getLastUpdateTime()).andReturn(getDateTime(1984, 1, 2, 3, 4, 6)).anyTimes();
        expect(action.getCompletionTime()).andReturn(getDateTime(1984, 1, 2, 3, 4, 7)).anyTimes();
        expect(action.getCopyOfMessages()).andReturn(Arrays.asList("")).anyTimes();
        expect(action.getAllMessagesAsSingleString()).andReturn("").anyTimes();
        expect(action.isScheduledEvent()).andReturn(isScheduledEvent).anyTimes();
        expect(action.hasMessages()).andReturn(true).anyTimes();
        replay(action);
        return action;
    }

    private OffsetDateTime getDateTime(final int year, final int month, final int dayOfMonth, final int hour, final int minute, final int second) {
        final LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
        final ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(dateTime);
        return OffsetDateTime.of(dateTime, offset);
    }

    private BackupManager mockBackupManager(final String id, final String domain, final String type) {
        final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn(id);
        expect(backupManager.getBackupDomain()).andReturn(domain);
        expect(backupManager.getBackupType()).andReturn(type);
        expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(new ArrayList<>());
        expect(backupManager.getActions()).andReturn(new ArrayList<>()).anyTimes();
        expect(backupManager.getHousekeeping()).andReturn(new Housekeeping(id, null));
        expect(backupManager.getScheduler()).andReturn(new Scheduler(id, null));
        expect(backupManager.getSftpServers()).andReturn(new ArrayList<>());

        EasyMock.replay(backupManager);
        return backupManager;
    }
}
