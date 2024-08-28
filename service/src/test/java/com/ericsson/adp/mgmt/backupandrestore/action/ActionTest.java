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

import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.BACKUP_TYPE;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_SCHEDULED_OPERATION_ERROR_INFO;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.EmptyPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnprocessableEntityException;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;

public class ActionTest {

    private static final String BACKUP_NAME = "myBackup";
    private static final String OTHER_BACKUP_NAME = "otherBackup";
    private static final String ACTION_ID = "123";
    private static final String DUMMY_PASS = "dummyPass";

    private static final String MESSAGE_ONE = "Agent: eric-data-document-database-pg, Stage: PreparingBackupJobStage, success: true\nmessage: \"Preparation for backup is successful\"\n\n";
    private static final String MESSAGE_TWO = "Agent: eric-sec-ldap-server, Stage: PreparingBackupJobStage, success: true\nmessage: \"Preparation for backup is successful\"\n\n";
    private static final String MESSAGE_THREE = "Agent: eric-data-distributed-coordinator-ed, Stage: ExecutingBackupJobStage, success: true\nmessage: \"Preparation for backup is successful\"\n\n";
    private static final String MESSAGE_FOUR = "Agent: eric-sec-ldap-server, Stage: ExecutingBackupJobStage, success: true\nmessage: \"The LDAP Server service has completed a backup for vaggelis and the data has been sent to the orchestrator\"\n\n";
    private static final String MESSAGE_FIVE = "Agent: eric-data-distributed-coordinator-ed, Stage: ExecutingBackupJobStage, success: true\nmessage: \"The DCED service has completed a backup for vaggelis and the data has been sent to the orchestrator\"\n\n";
    private static final String MESSAGE_SIX = "Agent: eric-data-coordinator-zk, Stage: ExecutingBackupJobStage, success: true\nmessage: \"The DCZK service has completed a backup for vaggelis and the data has been sent to the orchestrator\"\n\n";

    private static final String MESSAGE_ONE_CLEAN = "Agent: eric-data-document-database-pg, Stage: PreparingBackupJobStage, success: true, message: Preparation for backup is successful";
    private static final String MESSAGE_TWO_CLEAN = "Agent: eric-sec-ldap-server, Stage: PreparingBackupJobStage, success: true, message: Preparation for backup is successful";
    private static final String MESSAGE_THREE_CLEAN = "Agent: eric-data-distributed-coordinator-ed, Stage: ExecutingBackupJobStage, success: true, message: Preparation for backup is successful";
    private static final String MESSAGE_FOUR_CLEAN = "Agent: eric-sec-ldap-server, Stage: ExecutingBackupJobStage, success: true, message: The LDAP Server service has completed a backup for vaggelis and the data has been sent to the orchestrator";
    private static final String MESSAGE_FIVE_CLEAN = "Agent: eric-data-distributed-coordinator-ed, Stage: ExecutingBackupJobStage, success: true, message: The DCED service has completed a backup for vaggelis and the data has been sent to the orchestrator";
    private static final String MESSAGE_SIX_CLEAN = "Agent: eric-data-coordinator-zk, Stage: ExecutingBackupJobStage, success: true, message: The DCZK service has completed a backup for vaggelis and the data has been sent to the orchestrator";

    private MockedStatic<SpringContext> mockedSpringContext;
    private MeterRegistry meterRegistry;

    @Before
    public void setUp() {
        mockedSpringContext = mockStatic(SpringContext.class);
        meterRegistry = createMeterRegistry();
        when(SpringContext.getBean(MeterRegistry.class)).thenReturn(Optional.of(meterRegistry));
    }

    @After
    public void tearDown() {
        // Close the static mock after each test
        mockedSpringContext.close();
    }

    @Test
    public void toString_nullValues_returnsCorrectString() {
        final Action action = createAction("1234", ActionType.CREATE_BACKUP, "DEFAULT");
        action.setCompletionTime(null);
        action.setAdditionalInfo(null);
        action.setResultInfo(null);
        action.setProgressInfo(null);
        action.setResult(ResultType.NOT_AVAILABLE);
        action.setState(ActionStateType.RUNNING);
        final String expected = String.format("Action{actionId='', name=CREATE_BACKUP', result=NOT_AVAILABLE', state=RUNNING', progressPercentage=0.0', startTime='%s', lastUpdateTime='%s'}", action.getStartTime(), action.getLastUpdateTime());
        assertEquals(expected, action.toString());
    }

    @Test
    public void toString_noNullValues_returnsCorrectString() {
        final Action action = createAction("1234", ActionType.CREATE_BACKUP, "DEFAULT");
        action.setAdditionalInfo("something went wrong");
        action.setResultInfo("result");
        action.setProgressInfo("progress");
        action.setCompletionTime(action.getLastUpdateTime());
        action.setResult(ResultType.NOT_AVAILABLE);
        action.setState(ActionStateType.RUNNING);
        final String expected = String.format("Action{actionId='', name=CREATE_BACKUP', result=NOT_AVAILABLE', state=RUNNING', progressPercentage=0.0', additionalInfo='something went wrong', progressInfo='progress', resultInfo='result', completionTime='%s', startTime='%s', lastUpdateTime='%s'}",action.getLastUpdateTime(), action.getStartTime(), action.getLastUpdateTime());
        assertEquals(expected, action.toString());
    }
    @Test
    public void new_idActionTypeAndPayload_createsAction() throws Exception {
        final BackupNamePayload payload = new BackupNamePayload();
        final ActionRequest actionRequest = createActionRequest(ActionType.CREATE_BACKUP, ACTION_ID);
        actionRequest.setPayload(payload);
        final Action action = new Action(actionRequest, null);

        assertEquals(ACTION_ID, action.getActionId());
        assertEquals(ActionType.CREATE_BACKUP, action.getName());
        assertEquals(payload, action.getPayload());
        assertEquals(ActionStateType.RUNNING, action.getState());
        assertEquals(ResultType.NOT_AVAILABLE, action.getResult());
        assertNotNull(action.getStartTime());
        assertEquals(action.getStartTime(), action.getLastUpdateTime());
        assertNull(action.getCompletionTime());
        assertEquals(Double.valueOf(0.0), Double.valueOf(action.getProgressPercentage()));
    }

    @Test
    public void persist_actionAndPersistFunction_persistFunctionIsCalled() throws Exception {
        final ExecuteFunction function = new ExecuteFunction();
        final Action action = new Action(new ActionRequest(), actionToPersist -> function.execute());
        action.persist();
        assertTrue(function.wasExecuted());
    }

    @Test
    public void getBackupName_actionContainsBackupPayload_returnsBackupName() throws Exception {
        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName(BACKUP_NAME);
        final ActionRequest actionRequest = createActionRequest(ActionType.CREATE_BACKUP, ACTION_ID);
        actionRequest.setPayload(payload);
        final Action action = new Action(actionRequest, null);
        assertEquals(BACKUP_NAME, action.getBackupName());
    }

    @Test
    public void getBackupName_actionContainsExportPayload_returnsBackupName() throws Exception {
        final ExportPayload payload = new ExportPayload();
        payload.setBackupName(BACKUP_NAME);
        final ActionRequest actionRequest = createActionRequest(ActionType.EXPORT, ACTION_ID);
        actionRequest.setPayload(payload);
        final Action action = new Action(actionRequest, null);
        assertEquals(BACKUP_NAME, action.getBackupName());
    }

    @Test(expected = UnprocessableEntityException.class)
    public void getBackupName_actionDoesNotContainBackupPayload_throwsException() throws Exception {
        final EmptyPayload payload = new EmptyPayload();
        final ActionRequest actionRequest = new ActionRequest();
        actionRequest.setPayload(payload);
        final Action action = new Action(actionRequest, null);
        action.getBackupName();
    }

    @Test
    public void belongsToABackup_actionsForExportAndRestore_true() throws Exception {
        assertTrue(new Action(createActionRequest(ActionType.RESTORE, ACTION_ID), null).isRestoreOrExport());
        assertTrue(new Action(createActionRequest(ActionType.EXPORT, ACTION_ID), null).isRestoreOrExport());
    }

    @Test
    public void belongsToABackup_actionsForCreateBackupAndImportAndDeleteBackup_false() throws Exception {
        assertFalse(new Action(createActionRequest(ActionType.CREATE_BACKUP, ACTION_ID), null).isRestoreOrExport());
        assertFalse(new Action(createActionRequest(ActionType.IMPORT, ACTION_ID), null).isRestoreOrExport());
        assertFalse(new Action(createActionRequest(ActionType.DELETE_BACKUP, ACTION_ID), null).isRestoreOrExport());
    }

    @Test
    public void belongsToBackup_restoreAction_trueIfItHasThatBackupName() throws Exception {
        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName(BACKUP_NAME);
        final ActionRequest actionRequest = createActionRequest(ActionType.RESTORE, ACTION_ID);
        actionRequest.setPayload(payload);
        final Action exportAction = new Action(actionRequest, null);

        assertTrue(exportAction.belongsToBackup(BACKUP_NAME));
        assertFalse(exportAction.belongsToBackup(OTHER_BACKUP_NAME));
    }

    @Test
    public void belongsToBackup_exportAction_trueIfItHasThatBackupName() throws Exception {
        final ExportPayload payload = new ExportPayload();
        payload.setBackupName(BACKUP_NAME);
        final ActionRequest actionRequest = createActionRequest(ActionType.EXPORT, ACTION_ID);
        actionRequest.setPayload(payload);
        final Action exportAction = new Action(actionRequest, null);

        assertTrue(exportAction.belongsToBackup(BACKUP_NAME));
        assertFalse(exportAction.belongsToBackup(OTHER_BACKUP_NAME));
    }

    @Test
    public void belongsToBackup_actionsForCreateBackupAndImportAndDeleteBackup_false() throws Exception {
        assertFalse(new Action(createActionRequest(ActionType.CREATE_BACKUP, ACTION_ID), null).belongsToBackup(""));
        assertFalse(new Action(createActionRequest(ActionType.IMPORT, ACTION_ID), null).belongsToBackup(""));
        assertFalse(new Action(createActionRequest(ActionType.DELETE_BACKUP, ACTION_ID), null).belongsToBackup(""));
    }

    @Test
    public void hasMessages_messagesIsEmpty_returnsFalse() {
        final Action action = new Action(createActionRequest(ActionType.CREATE_BACKUP, ACTION_ID), null);
        assertFalse(action.hasMessages());
    }

    @Test
    public void hasMessages_messagesIsNotEmpty_returnsTrue() {
        final Action action = new Action(createActionRequest(ActionType.CREATE_BACKUP, ACTION_ID), null);
        action.addMessage(MESSAGE_ONE);
        assertTrue(action.hasMessages());
    }

    @Test
    public void hasMessages_returns_formattedString() {
        final String expectedResult = "{" + MESSAGE_ONE_CLEAN + "}, {" + MESSAGE_TWO_CLEAN + "}, {" + MESSAGE_THREE_CLEAN + "}, {" + MESSAGE_FOUR_CLEAN + "}, {" + MESSAGE_FIVE_CLEAN + "}, {" + MESSAGE_SIX_CLEAN + "}";
        final Action action = new Action(createActionRequest(ActionType.CREATE_BACKUP, ACTION_ID), null);
        action.addMessage(MESSAGE_ONE);
        action.addMessage(MESSAGE_TWO);
        action.addMessage(MESSAGE_THREE);
        action.addMessage(MESSAGE_FOUR);
        action.addMessage(MESSAGE_FIVE);
        action.addMessage(MESSAGE_SIX);
        assertTrue(action.hasMessages());
        final String result = action.getAllMessagesAsSingleString();
        assertEquals(result, expectedResult);
    }

    @Test
    public void bro_operations_totalMetricIsIncremented() throws Exception {
        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName("DEFAULT");
        Metrics.addRegistry(meterRegistry);
        final ActionRequest actionRequest = new ActionRequest();
        actionRequest.setAction(ActionType.CREATE_BACKUP);
        actionRequest.setBackupManagerId("DEFAULT");
        actionRequest.setPayload(payload);
        actionRequest.setActionId("1");
        final Action action = new Action(actionRequest, null);
        action.updateOperationsTotalMetric();
        action.updateLastOperationInfoMetric();

        assertEquals(Double.valueOf(1.0), Double.valueOf(Metrics.counter("bro.operations.total",
                "action", ActionType.CREATE_BACKUP.name(), "status", "NOT_AVAILABLE", "backup_type", "DEFAULT").count()));
    }

    @Test
    public void createdScheduledAction_noActionRun_gaugeReturnsZero() throws Exception {
        final BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.getActions()).andReturn(new ArrayList<>());
        final BackupManagerRepository backupManagerRepo = createMock(BackupManagerRepository.class);
        expect(backupManagerRepo.getBackupManager("test-brm")).andReturn(backupManager);

        replay(backupManager, backupManagerRepo);

        final BackupNamePayload payload = new BackupNamePayload();
        final ActionRequest actionRequest = createActionRequest(ActionType.CREATE_BACKUP, ACTION_ID);
        actionRequest.setPayload(payload);
        actionRequest.setScheduledEvent(true);
        actionRequest.setBackupManagerId("test-brm");
        final Action action = new Action(actionRequest, null);

        assertEquals(ACTION_ID, action.getActionId());
        assertEquals(Double.valueOf(0.0), Double.valueOf(meterRegistry.find(METRIC_SCHEDULED_OPERATION_ERROR_INFO.identification())
                .tag(String.valueOf(BACKUP_TYPE), "test-brm")
                .tag("action", actionRequest.getAction().name())
                .gauge().value()));
    }

    @Test
    public void createdScheduledAction_failedScheduledBackup_gaugeReturnsOne() throws Exception {
        final Action existingAction1 = createMock(Action.class);
        expect(existingAction1.isScheduledEvent()).andReturn(true);
        expect(existingAction1.getStartTime()).andReturn(OffsetDateTime.now());
        expect(existingAction1.getName()).andReturn(ActionType.CREATE_BACKUP);
        expect(existingAction1.getResult()).andReturn(ResultType.FAILURE).anyTimes();

        // This is used to verify that the status of an on-going action is ignored by the gauge
        final Action existingAction2 = createMock(Action.class);
        expect(existingAction2.isScheduledEvent()).andReturn(true);
        expect(existingAction2.getStartTime()).andReturn(OffsetDateTime.now());
        expect(existingAction2.getName()).andReturn(ActionType.CREATE_BACKUP);
        expect(existingAction2.getResult()).andReturn(ResultType.NOT_AVAILABLE).anyTimes();

        final List<Action> actions = new ArrayList<>();
        actions.add(existingAction1);
        actions.add(existingAction2);

        final BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.getActions()).andReturn(actions);
        final BackupManagerRepository backupManagerRepo = createMock(BackupManagerRepository.class);
        expect(backupManagerRepo.getBackupManager("test-brm")).andReturn(backupManager);

        when(SpringContext.getBean(BackupManagerRepository.class)).thenReturn(Optional.of(backupManagerRepo));
        replay(existingAction1, existingAction2, backupManager, backupManagerRepo);

        final BackupNamePayload payload = new BackupNamePayload();
        final ActionRequest actionRequest = createActionRequest(ActionType.CREATE_BACKUP, ACTION_ID);
        actionRequest.setPayload(payload);
        actionRequest.setScheduledEvent(true);
        actionRequest.setBackupManagerId("test-brm");
        final Action action = new Action(actionRequest, null);

        assertEquals(ACTION_ID, action.getActionId());
        assertEquals(Double.valueOf(1.0), Double.valueOf(meterRegistry.find(METRIC_SCHEDULED_OPERATION_ERROR_INFO.identification())
                .tag(String.valueOf(BACKUP_TYPE), "test-brm")
                .tag("action", actionRequest.getAction().name())
                .gauge().value()));
    }

    @Test
    public void testEquality_sameIdSameBRMButDifferentActionType() {
        final Action actionOne = createAction("1234", ActionType.EXPORT, "DEFAULT");
        final Action actionTwo = createAction("1234", ActionType.HOUSEKEEPING_DELETE_BACKUP, "DEFAULT");
        assertFalse(actionOne.equals(actionTwo));
        assertFalse(actionOne.hashCode() == (actionTwo.hashCode()));
    }

    @Test
    public void testEquality_sameIdSameActionTypeButDifferentBRM() {
        final Action actionOne = createAction("1234", ActionType.HOUSEKEEPING_DELETE_BACKUP, "DEFAULT");
        final Action actionTwo = createAction("1234", ActionType.HOUSEKEEPING_DELETE_BACKUP, "configuration-data");
        assertFalse(actionOne.equals(actionTwo));
        assertFalse(actionOne.hashCode() == (actionTwo.hashCode()));
    }

    @Test
    public void testEquality_sameBRMSameActionTypeButDifferentId() {
        final Action actionOne = createAction("1234", ActionType.HOUSEKEEPING_DELETE_BACKUP, "DEFAULT");
        final Action actionTwo = createAction("5678", ActionType.HOUSEKEEPING_DELETE_BACKUP, "DEFAULT");
        assertFalse(actionOne.equals(actionTwo));
        assertFalse(actionOne.hashCode() == (actionTwo.hashCode()));
    }

    @Test
    public void test_hasKinBRM_Unrelated_BRMs() {
        final Action actionOne = createAction("1234", ActionType.IMPORT, "DEFAULT");
        final Action actionTwo = createAction("5678", ActionType.RESTORE, "NEW");
        final BackupManager backupManagerOne = createMock(BackupManager.class);
        final BackupManager backupManagerTwo = createMock(BackupManager.class);
        final BackupManagerRepository backupManagerRepo = createMock(BackupManagerRepository.class);

        expect(backupManagerRepo.getBackupManager("DEFAULT")).andReturn(backupManagerOne);
        expect(backupManagerRepo.getBackupManager("NEW")).andReturn(backupManagerTwo);

        ApplicationContext mockContext = createMock(ApplicationContext.class);
        expect(mockContext.getBean(BackupManagerRepository.class)).andReturn(backupManagerRepo);

        replay(backupManagerOne, backupManagerTwo, backupManagerRepo, mockContext);
        assertFalse(actionOne.hasKinBRM(actionTwo));

    }

    @Test
    public void test_isConfigBRMOf_Config_And_Normal_BRMs() {
        final Action actionOne = createAction("1234", ActionType.IMPORT, "DEFAULT");
        final Action actionTwo = createAction("5678", ActionType.RESTORE, "DEFAULT-bro");

        assertFalse(actionOne.isConfigBRMOf(actionTwo));
        assertTrue(actionTwo.isConfigBRMOf(actionOne));
    }

    private Action createAction(final String actionId, final ActionType type, final String backupManagerId) {
        PersistedAction persisted = new PersistedAction();
        persisted.setActionId(actionId);
        persisted.setName(type);
        persisted.setStartTime(OffsetDateTime.now().toString());
        persisted.setLastUpdateTime(OffsetDateTime.now().toString());
        final Action action = new Action(persisted, backupManagerId, null);
        return action;
    }

    private ActionRequest createActionRequest(final ActionType actionType, String actionId) {
        final ActionRequest actionRequest = new ActionRequest();
        actionRequest.setActionId(actionId);
        actionRequest.setAction(actionType);
        return actionRequest;
    }

    private MeterRegistry createMeterRegistry(){
        final CollectorRegistry prometheusRegistry = new CollectorRegistry(true);
        final MockClock clock = new MockClock();
        final MeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, prometheusRegistry,clock);
        return meterRegistry;
    }

    private class ExecuteFunction {

        private boolean wasExecuted;

        public void execute() {
            this.wasExecuted = true;
        }

        public boolean wasExecuted() {
            return wasExecuted;
        }

    }

}
