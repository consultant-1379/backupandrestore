/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.easymock.EasyMock;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.action.yang.UnprocessableYangRequestException;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMBackupManagerJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMEricssonbrmJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMSchedulerJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.AddProgressReportPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.BackupManagerPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.BackupPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.HousekeepingPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PeriodicEventPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.ProgressReportPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.SchedulerPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.UpdateProgressReportPatch;
import com.ericsson.adp.mgmt.backupandrestore.exception.CMMediatorException;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.backupandrestore.util.RestTemplateFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests {@link CMMediatorService}.addProgressReport scenarios.
 */
public class CMMediatorServiceAddProgressReportTest {

    private static final String TEST_URL = "http://localhost:5003/cm/api/v1";

    private CMMediatorService cmMediatorService;
    private ProgressReportPatchFactory progressReportPatchFactory;
    private BRMConfigurationUtil brmConfigurationService;
    private Action action;
    private BackupManagerRepository backupManagerRepository;
    private BackupManager backupManager;
    private CMMClient cmmClient;
    private CMMRestClient cmmRestClient;
    private RestTemplateFactory restTemplateFactory;

    private BackupManagerPatchFactory backupManagerPatchFactory;
    private BackupPatchFactory backupPatchFactory;
    private HousekeepingPatchFactory housekeepingPatchFactory;
    private SchedulerPatchFactory schedulerPatchFactory;
    private PeriodicEventPatchFactory periodicEventPatchFactory;
    private CMMMessageFactory cmmMessageFactory;
    private SchemaRequestFactory schemaRequestFactory;
    private CMMSubscriptionRequestFactory cmmSubscriptionRequestFactory;
    private JsonService jsonService;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static EtagNotifIdBase etagNotifIdBase;

    /**
     * Sets up initial conditions for @{@link CMMediatorService}.
     */
    @Before
    public void setup() {
        etagNotifIdBase = new EtagNotifIdBase();
        etagNotifIdBase.updateEtag("11111");
        backupManagerPatchFactory = createMock(BackupManagerPatchFactory.class);
        backupPatchFactory = createMock(BackupPatchFactory.class);
        progressReportPatchFactory = createMock(ProgressReportPatchFactory.class);
        housekeepingPatchFactory = createMock(HousekeepingPatchFactory.class);

        schedulerPatchFactory = createMock(SchedulerPatchFactory.class);
        periodicEventPatchFactory = createMock(PeriodicEventPatchFactory.class);
        schemaRequestFactory = createMock(SchemaRequestFactory.class);
        backupManagerRepository = createMock(BackupManagerRepository.class);
        cmmSubscriptionRequestFactory = createMock(CMMSubscriptionRequestFactory.class);
        jsonService = createMock(JsonService.class);
        expect(jsonService.parseJsonFromClassPathResource(anyObject(), anyObject())).andReturn(Optional.of("")).anyTimes();


        cmmMessageFactory = new CMMMessageFactory();
        cmmMessageFactory.setHousekeepingPatchFactory(housekeepingPatchFactory);
        cmmMessageFactory.setPeriodicEventPatchFactory(periodicEventPatchFactory);
        cmmMessageFactory.setSchedulerPatchFactory(schedulerPatchFactory);
        cmmMessageFactory.setBackupPatchFactory(backupPatchFactory);
        cmmMessageFactory.setBackupManagerPatchFactory(backupManagerPatchFactory);
        cmmMessageFactory.setSchemaRequestFactory(schemaRequestFactory);
        cmmMessageFactory.setBackupManagerRepository(backupManagerRepository);
        cmmMessageFactory.setCMMSubscriptionRequestFactory(cmmSubscriptionRequestFactory);
        cmmMessageFactory.setJsonService(jsonService);


        cmmClient = new CMMClient();
        cmmClient.setFlagEnabled(true);

        restTemplateFactory = new RestTemplateFactory();

        cmmRestClient = new CMMRestClient();
        cmmRestClient.setCmUrl(TEST_URL);
        cmmRestClient.setRestTemplateConfiguration(restTemplateFactory, false);

        cmmClient.setCmmRestClient(cmmRestClient);

        brmConfigurationService = createMock(BRMConfigurationUtil.class);
        cmMediatorService = new CMMediatorService(cmmClient, etagNotifIdBase, cmmMessageFactory);
        cmMediatorService.setCmmClient(cmmClient);
        cmMediatorService.setInitialize(true);
        cmMediatorService.setProgressReportPatchFactory(progressReportPatchFactory);
        cmMediatorService.setCMMMessageFactory(cmmMessageFactory);
        action = createMock(Action.class);
        expect(action.getActionId()).andReturn("TestActionId").anyTimes();

        backupManagerRepository = new TestBackupManagerRepository();
        cmMediatorService.setBackupManagerRepository(backupManagerRepository);
        cmMediatorService.setBrmConfigurationService(brmConfigurationService);
        backupManager = backupManagerRepository.getBackupManager(0);
        backupManager.backupManagerLevelProgressReportSetCreated();
        expectLastCall().anyTimes();
        backupManager.backupManagerLevelProgressReportResetCreated();
        expectLastCall().anyTimes();
        expect(backupManager.isBackupManagerLevelProgressReportCreated())
        .andReturn(true).anyTimes();
    }

    @After
    public void tearDown() {
        executorService.shutdown();
        cmmClient.stopProcessing();
        cmMediatorService.stopProcessingProgressReport();
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
    }

     /**
     * Tests the conditions where the {@link Action} is part of an {@link com.ericsson.adp.mgmt.backupandrestore.action.ActionType}.RESTORE or @{@link
     * com.ericsson.adp.mgmt.backupandrestore.action.ActionType}.EXPORT and the {@link com.ericsson.adp.mgmt.backupandrestore.backup.Backup} level
     * progress report exists in {@link BRMConfigurationUtil} The progress report should be updated.
     * @throws JSONException 
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Test
    public void addProgressReport_actionIsRestoreOrExportAndBackupLevelProgressReportAlreadyCreated_BackupLevelProgressReportUpdated()
            throws JsonMappingException, JsonProcessingException, JSONException {
        action = newAction();
        final TestUpdateProgressReportPatch brmConfigurationPatch = new TestUpdateProgressReportPatch(action);
        final String backupName = "abc";
        final Backup backup = createMock(Backup.class);
        BRMConfiguration brmConfiguration = new ObjectMapper().readValue(getJsonBRMConfiguration(false).toString(),BRMConfiguration.class);
        final BRMEricssonbrmJson brmEricConfiguration = new BRMEricssonbrmJson("ericsson-brm","ericsson-brm",brmConfiguration);
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricConfiguration));
        expect(action.getBackupName()).andReturn(backupName).anyTimes();
        backup.backupLevelProgressReportResetCreated();
        expectLastCall().times(2);
        expect(backup.isBackupLevelProgressReportCreated()).andReturn(true);
        expect(action.isScheduledEvent()).andReturn(false);
        expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(Arrays.asList(backup));
        expect(backupManager.getBackup(backupName, Ownership.READABLE)).andReturn(backup);

//        expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(true);
        // expect(backupManager.getBackup(backupName, Ownership.READABLE)).andReturn(backup);
        expect(progressReportPatchFactory.getPatchToUpdateProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch));
        brmConfigurationService.patch(brmConfigurationPatch);
        expectLastCall();

        replay(backup, action, backupManager, progressReportPatchFactory, brmConfigurationService);
        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, backupManager);
    }

    @Test
    public void addProgressReportBase_actionIsRestoreOrExportAndBackupLevelProgressReportAlreadyNotCreated() {
        Action action = newAction();
        final TestUpdateProgressReportPatch brmConfigurationPatch = new TestUpdateProgressReportPatch(action);
        final TestAddProgressReportPatch brmaddProgressConfigurationPatch = new TestAddProgressReportPatch(action);

        // final AddBackupPath addBackupPath = new AddBackupPath("/path/action/to/progress-report");
        final String backupName = "abc";
        expect(action.getBackupName()).andReturn(backupName).anyTimes();
        expect(action.isPartOfHousekeeping()).andReturn(true).anyTimes();
        expect(action.isScheduledEvent()).andReturn(false).times(2);
        final Backup backup = createMock(Backup.class);
        expect(backup.isBackupLevelProgressReportCreated()).andReturn(false);
        backup.backupLevelProgressReportSetCreated();
        expectLastCall();
        backup.backupLevelProgressReportResetCreated();
        expectLastCall().times(2);
        replay(action);
//        backupManager.backupManagerLevelProgressReportSetCreated();
//        expectLastCall();
////        expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(true);
        expect(backupManager.getBackup(backupName, Ownership.READABLE)).andReturn(backup).anyTimes();
        expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(Arrays.asList(backup)).anyTimes();

        
        BRMEricssonbrmJson brmEricssonbrmJson = getbrmEricConfiguration(action, true);
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricssonbrmJson));
        CMMMessage cmmMessage = createMock (CMMMessage.class);
        expect(cmmMessage.getResource()).andReturn("configurations/ericsson-brm").anyTimes();
        expect(cmmMessage.getHttpMethod()).andReturn(HttpMethod.PATCH).anyTimes();
        //expect(cmmMessage.getConfigurationPatch()).andReturn(addBackupPath).anyTimes();

        //expect(progressReportPatchFactory.getMessageToUploadProgressReport(action, -1)).andReturn(cmmMessage);
        expect(progressReportPatchFactory.getPatchToAddProgressReport(action)).andReturn(Arrays.asList(brmaddProgressConfigurationPatch)).times(2);

        brmConfigurationService.patch(brmaddProgressConfigurationPatch);
        expectLastCall();

        replay(backup, backupManager, progressReportPatchFactory, brmConfigurationService, cmmMessage);
        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, backupManager);
    }

    @Test
    public void addProgressReportBase_InitialEmptyConfiguration() {
        Action action = newAction();
        final TestUpdateProgressReportPatch brmConfigurationPatch = new TestUpdateProgressReportPatch(action);
        final TestAddProgressReportPatch brmaddProgressConfigurationPatch = new TestAddProgressReportPatch(action);

        // final AddBackupPath addBackupPath = new AddBackupPath("/path/action/to/progress-report");
        final String backupName = "abc";
        expect(action.getBackupName()).andReturn(backupName).anyTimes();
//        expect(action.belongsToBackup("D")).andReturn(true).anyTimes();
        expect(action.isPartOfHousekeeping()).andReturn(true).anyTimes();
//        expect(action.isScheduledEvent()).andReturn(false);
        final Backup backup = createMock(Backup.class);
        expect(backup.isBackupLevelProgressReportCreated()).andReturn(false);
        backup.backupLevelProgressReportSetCreated();
        expectLastCall();
        backup.backupLevelProgressReportResetCreated();
        expectLastCall();
        expect(backup.getBackupId()).andReturn(backupName);
        expect(backup.getName()).andReturn(backupName);
        expect(backup.getUserLabel()).andReturn("label");
        //expect(backupManager.getBackups(anyObject())).andReturn(null);

//        expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(true);
        expect(backupManager.getBackup(backupName, Ownership.READABLE)).andReturn(backup).anyTimes();
//        backupManager.backupManagerLevelProgressReportResetCreated();
//        expectLastCall().anyTimes();
//        EasyMock.expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(Arrays.asList(backup));

        BRMEricssonbrmJson brmEricssonbrmJson = getInitbrmEricConfiguration();
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricssonbrmJson)).times(1);
        CMMMessage cmmMessage = createMock (CMMMessage.class);
        expect(cmmMessage.getResource()).andReturn("configurations/ericsson-brm").anyTimes();
        expect(cmmMessage.getHttpMethod()).andReturn(HttpMethod.PATCH).anyTimes();

        expect(progressReportPatchFactory.getPatchToAddProgressReport(action)).andReturn(Arrays.asList(brmaddProgressConfigurationPatch)).times(2);

        brmConfigurationService.patch(brmaddProgressConfigurationPatch);
        expectLastCall();

        replay(backup, action, backupManager, progressReportPatchFactory, brmConfigurationService, cmmMessage);
        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, backupManager);
    }

   private Backup mockBackup(final boolean isBackupLevelProgressReportCreated) {
       final Backup backup = EasyMock.createMock(Backup.class);
       EasyMock.expect(backup.getBackupId()).andReturn("D").anyTimes();
       EasyMock.expect(backup.getName()).andReturn("E").anyTimes();
       EasyMock.expect(backup.getCreationTime()).andReturn(getDateTime(1984, 1, 2, 3, 4, 5)).anyTimes();
       EasyMock.expect(backup.getCreationType()).andReturn(BackupCreationType.SCHEDULED).anyTimes();
       EasyMock.expect(backup.getStatus()).andReturn(BackupStatus.CORRUPTED).anyTimes();
       EasyMock.expect(backup.getSoftwareVersions()).andReturn(Arrays.asList()).anyTimes();
       EasyMock.expect(backup.getBackupManagerId()).andReturn("666").anyTimes();
       EasyMock.expect(backup.getUserLabel()).andReturn("").anyTimes();
       backup.backupLevelProgressReportResetCreated();
       expectLastCall().anyTimes();
       expect(backup.isBackupLevelProgressReportCreated()).andReturn(isBackupLevelProgressReportCreated);
       EasyMock.replay(backup);
       return backup;
   }

   private BackupManager mockBackupManager(final List<Action> actions) {
       final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
       EasyMock.expect(backupManager.getBackupManagerId()).andReturn("A");
       EasyMock.expect(backupManager.getBackupDomain()).andReturn("B");
       EasyMock.expect(backupManager.getBackupType()).andReturn("C");
       EasyMock.expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(Arrays.asList(mockBackup(true)));
       EasyMock.expect(backupManager.getActions()).andReturn(actions).anyTimes();
       EasyMock.expect(backupManager.getHousekeeping()).andReturn(new Housekeeping("A", null));
       EasyMock.expect(backupManager.getScheduler()).andReturn(new Scheduler("A", null));
       EasyMock.expect(backupManager.getSftpServers()).andReturn(new ArrayList<>());
       backupManager.backupManagerLevelProgressReportResetCreated();
       expectLastCall().anyTimes();
       EasyMock.replay(backupManager);
       return backupManager;
   }

  private BRMEricssonbrmJson getbrmEricConfiguration(Action action, boolean emptyBackups) {
       final BRMBackupManagerJson brmBackupManager = new BRMBackupManagerJson(
               mockBackupManager(new ArrayList<>(Collections.singletonList(action)) ));
       final BRMSchedulerJson brmSchedulerJson = EasyMock.createMock(BRMSchedulerJson.class);
       final BRMEricssonbrmJson brmEricConfiguration = EasyMock.createMock(BRMEricssonbrmJson.class);
       final BRMConfiguration brmConfiguration = EasyMock.createMock(BRMConfiguration.class);
       final BRMJson brmJson = EasyMock.createMock(BRMJson.class);
       final BRMBackupManagerJson brmBackupJson = EasyMock.createMock(BRMBackupManagerJson.class);
       expect(brmBackupJson.getBackupManagerId()).andReturn("DEFAULT");
       expect(brmBackupJson.getProgressReports()).andReturn(Collections.emptyList()).anyTimes();
       expect(brmBackupJson.getBackups()).andReturn(emptyBackups ? Collections.emptyList() : brmBackupManager.getBackups()).anyTimes();
       expect(brmJson.getBackupManagers()).andReturn(Arrays.asList(brmBackupJson));
       expect(brmConfiguration.getBrm()).andReturn(brmJson).anyTimes();
       expect(brmEricConfiguration.getBRMConfiguration()).andReturn(brmConfiguration).anyTimes();
       expect(brmSchedulerJson.getProgressReports()).andReturn(null).anyTimes();
       expect(brmBackupJson.getScheduler()).andReturn(brmSchedulerJson).anyTimes();
       replay(brmEricConfiguration, brmJson, brmConfiguration, brmBackupJson, brmSchedulerJson);
       return brmEricConfiguration;
   }

   public BRMEricssonbrmJson getInitbrmEricConfiguration() {
       final BRMEricssonbrmJson brmEricConfiguration = EasyMock.createMock(BRMEricssonbrmJson.class);
       final BRMConfiguration brmConfiguration = EasyMock.createMock(BRMConfiguration.class);
       expect(brmConfiguration.getBrm()).andReturn(null).anyTimes();
       expect(brmEricConfiguration.getBRMConfiguration()).andReturn(brmConfiguration).anyTimes();
       replay(brmEricConfiguration, brmConfiguration);
       return brmEricConfiguration;
   }

    /**
    * Tests the conditions where the {@link Action} is part of an {@link com.ericsson.adp.mgmt.backupandrestore.action.ActionType}.RESTORE or @{@link
    * com.ericsson.adp.mgmt.backupandrestore.action.ActionType}.EXPORT and the {@link com.ericsson.adp.mgmt.backupandrestore.backup.Backup} level
    * progress report exists in {@link BRMConfigurationUtil} The progress report should be updated.
     * @throws JSONException 
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
    */
   @Test
   public void addProgressReport_actionIsRestoreOrExportAndBackupLevelProgressReportAlreadyCreated_ProgressReportNotEnqueuedYet_BackupLevelProgressReportUpdated()
           throws JsonMappingException, JsonProcessingException, JSONException {
       final Action action = newAction("DEFAULT","myAction", ActionType.RESTORE, 1.0);
       final TestUpdateProgressReportPatch brmConfigurationPatch = new TestUpdateProgressReportPatch(action);
       final String backupName = "abc";
       BRMConfiguration brmConfiguration = new ObjectMapper().readValue(getJsonBRMConfiguration(false).toString(),BRMConfiguration.class);
       final BRMEricssonbrmJson brmEricConfiguration = new BRMEricssonbrmJson("ericsson-brm","ericsson-brm",brmConfiguration);
       expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricConfiguration));

       expect(action.getBackupName()).andReturn(backupName).anyTimes();
       expect(action.isScheduledEvent()).andReturn(false);

       final Backup backup = createMock(Backup.class);
       // expect(backup.isBackupLevelProgressReportCreated()).andReturn(true);
//       backup.backupLevelProgressReportSetCreated();
//       expectLastCall();
       backup.backupLevelProgressReportResetCreated();
       expectLastCall().times(2);
       expect(backup.isBackupLevelProgressReportCreated()).andReturn(true);
//       expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(true);
       expect(backupManager.getBackup(backupName, Ownership.READABLE)).andReturn(backup);
       expect(backupManager.getBackups(Ownership.READABLE)).andReturn(Arrays.asList(backup));
       expect(progressReportPatchFactory.getPatchToUpdateProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch));
       brmConfigurationService.patch(brmConfigurationPatch);
       expectLastCall();

       final Action alreadyEnqueuedAction = newAction("DEFAULT","myAction", ActionType.RESTORE, 0.9);
       replay(action, alreadyEnqueuedAction, backup, backupManager, progressReportPatchFactory, brmConfigurationService);
       cmMediatorService.setLastEnqueuedBackupProgressReport(alreadyEnqueuedAction);
       cmMediatorService.addProgressReport(action);
       Awaitility.await().atMost(1, TimeUnit.SECONDS);
       verify(action, backupManager, brmConfigurationService);
   }

   /**
   * Tests the conditions where the {@link Action} is part of an {@link com.ericsson.adp.mgmt.backupandrestore.action.ActionType}.RESTORE or @{@link
   * com.ericsson.adp.mgmt.backupandrestore.action.ActionType}.EXPORT and the {@link com.ericsson.adp.mgmt.backupandrestore.backup.Backup} level
   * progress report exists in {@link BRMConfigurationUtil} but the same progress report is aready enqueued in CM queue. The progress report should NOT be updated.
 * @throws JSONException 
 * @throws JsonProcessingException 
 * @throws JsonMappingException 
   */
  @Test
  public void addProgressReport_actionIsRestoreOrExportAndBackupLevelProgressReportAlreadyCreated_SameProgressReportAlreadyEnqueued_BackupLevelProgressReportNotUpdated() throws JsonMappingException, JsonProcessingException, JSONException {
      BRMConfiguration brmConfiguration = new ObjectMapper().readValue(getJsonBRMConfiguration(false).toString(),BRMConfiguration.class);
      final BRMEricssonbrmJson brmEricConfiguration = new BRMEricssonbrmJson("ericsson-brm","ericsson-brm",brmConfiguration);
      final Action action = newAction("DEFAULT","myAction", ActionType.EXPORT, 1.0);
      final TestUpdateProgressReportPatch brmConfigurationPatch = new TestUpdateProgressReportPatch(action);
      final String backupName = "abc";
      expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricConfiguration));
      expect(action.getBackupName()).andReturn(backupName).anyTimes();
      expect(action.isScheduledEvent()).andReturn(false);
      final Backup backup = mockBackup(true); // must execute getPatchToUpdateProgressReport
      // expect(backup.isBackupLevelProgressReportCreated()).andReturn(true);
      expect(backupManager.getBackup(backupName, Ownership.OWNED)).andReturn(backup);
      // expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(true);
      expect(progressReportPatchFactory.getPatchToUpdateProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch));
      EasyMock.expect(backupManager.getBackups(eq(Ownership.OWNED))).andReturn(Arrays.asList(backup)).times(1,2);

      replay(action, backupManager, progressReportPatchFactory, brmConfigurationService);
      cmMediatorService.setLastEnqueuedBackupProgressReport(action);
      cmMediatorService.addProgressReport(action);
      Awaitility.await().atMost(1, TimeUnit.SECONDS);
      verify(action, backupManager);
  }

    /**
     * Tests the conditions where the {@link Action} is not part of an {@link com.ericsson.adp.mgmt.backupandrestore.action.ActionType}.RESTORE or
     *
     * @{@link com.ericsson.adp.mgmt.backupandrestore.action.ActionType}.EXPORT and the {@link BackupManager} level progress report does not exist in
     * {@link BRMConfigurationService} The progress report should be created.
     */
    @Test
    public void addProgressReport_actionIsNotRestoreOrExportAndBackupManagerLevelProgressReportNotYetCreated_BackupManagerLevelProgressReportCreated() {
        final TestAddProgressReportPatch brmConfigurationPatch = new TestAddProgressReportPatch(action);
        //final AddBackupPath addBackupPath = new AddBackupPath("/path/action/to/progress-report");
        
        CMMMessage cmmMessage = createMock (CMMMessage.class);
        expect(cmmMessage.getResource()).andReturn("configurations/ericsson-brm").anyTimes();
        expect(cmmMessage.getHttpMethod()).andReturn(HttpMethod.PATCH).anyTimes();
        //expect(cmmMessage.getConfigurationPatch()).andReturn(addBackupPath).anyTimes();
        //expect(progressReportPatchFactory.getMessageToUploadProgressReport(anyObject(), EasyMock.anyInt())).andReturn(cmmMessage).times(1);
//        backupManager.backupManagerLevelProgressReportSetCreated();
//        expectLastCall();
        expect(action.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
//        expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(false).anyTimes();
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.empty()).anyTimes();

        brmConfigurationService.patch(brmConfigurationPatch);
        expectLastCall();
        replay(brmConfigurationService);
        expect(progressReportPatchFactory.getPatchToAddProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch));

        expect(action.isRestoreOrExport()).andReturn(false);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
//        expect(action.isScheduledEvent()).andReturn(false);
//        backupManager.backupManagerLevelProgressReportSetCreated();
//        expectLastCall();
        replay(action, backupManager, progressReportPatchFactory, cmmMessage);
        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, progressReportPatchFactory, backupManager);
    }

    /**
     * Tests the conditions where the {@link Action} is not part of an {@link com.ericsson.adp.mgmt.backupandrestore.action.ActionType}.RESTORE or
     *
     * @{@link com.ericsson.adp.mgmt.backupandrestore.action.ActionType}.EXPORT and the {@link BackupManager} level progress report exists in {@link
     * BRMConfigurationService} The progress report should be updated.
     */
    @Test
    public void addProgressReport_actionIsNotRestoreOrExportAndBackupManagerLevelProgressReportAlreadyCreated_BackupManagerLevelProgressReportUpdated() {
        final Backup backup = createMock(Backup.class);
        final String backupName = "abc";

        action = newAction();
        //expect(action.getBackupManagerId()).andReturn("DEFAULT").times(2);
        // expect(action.isRestoreOrExport()).andReturn(false).anyTimes();
        // expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.isScheduledEvent()).andReturn(false).times(2);
        expect(action.getBackupName()).andReturn(backupName).anyTimes();

        replay(action);
        final TestUpdateProgressReportPatch brmConfigurationPatch = new TestUpdateProgressReportPatch(action);
        BRMEricssonbrmJson brmEricssonbrmJson = getbrmEricConfiguration(action, true);
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricssonbrmJson));
        expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(Arrays.asList(backup));
        expect(backupManager.getBackup(backupName, Ownership.READABLE)).andReturn(backup);
        backup.backupLevelProgressReportResetCreated();
        expectLastCall().times(2);
        expect(backup.isBackupLevelProgressReportCreated()).andReturn(true).times(2);

       // expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(true);
        expect(progressReportPatchFactory.getPatchToUpdateProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch));
        brmConfigurationService.patch(brmConfigurationPatch);
        expectLastCall();
        replay(backup, brmConfigurationService, backupManager, progressReportPatchFactory);
        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(brmConfigurationService, action, progressReportPatchFactory, backupManager);
    }

    /**
     * Tests the conditions where the {@link Action} is part of an {@link com.ericsson.adp.mgmt.backupandrestore.action.ActionType}.RESTORE or @{@link
     * com.ericsson.adp.mgmt.backupandrestore.action.ActionType}.EXPORT and the {@link com.ericsson.adp.mgmt.backupandrestore.backup.Backup} level
     * progress report does not exist in {@link BRMConfigurationService} The progress report should be created.
     */
    @Test
    public void addProgressReport_actionIsRestoreOrExportAndBackupLevelProgressReportNotYetCreated_BackupLevelProgressReportCreated() {
        final TestAddProgressReportPatch brmConfigurationPatch = new TestAddProgressReportPatch(action);
        // final AddBackupPath addBackupPath = new AddBackupPath("/path/action/to/progress-report");

        CMMMessage cmmMessage = createMock (CMMMessage.class);
        expect(cmmMessage.getResource()).andReturn("configurations/ericsson-brm").anyTimes();
        expect(cmmMessage.getHttpMethod()).andReturn(HttpMethod.PATCH).anyTimes();
        //expect(cmmMessage.getConfigurationPatch()).andReturn(addBackupPath).anyTimes();
        //expect(progressReportPatchFactory.getMessageToUploadProgressReport(anyObject(), EasyMock.anyInt())).andReturn(cmmMessage).times(1);
//        backupManager.backupManagerLevelProgressReportSetCreated();
//        expectLastCall();
        expect(action.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
//        expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(true).times(1);
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.empty()).times(1);

        expect(action.isRestoreOrExport()).andReturn(true);
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        final String backupName = "abc";
        expect(action.getBackupName()).andReturn(backupName);
        final Backup backup = createMock(Backup.class);
        expect(backup.isBackupLevelProgressReportCreated()).andReturn(false).times(2);
        backup.backupLevelProgressReportSetCreated();
        expectLastCall().once().anyTimes();
        replay(backup, cmmMessage);
        expect(backupManager.getBackup(backupName, Ownership.READABLE)).andReturn(backup);
        expect(progressReportPatchFactory.getPatchToAddProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch));
        brmConfigurationService.patch(brmConfigurationPatch);
        expectLastCall();
        // expect(action.isScheduledEvent()).andReturn(false);

        replay(action, backupManager, progressReportPatchFactory, brmConfigurationService);

        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, backupManager, progressReportPatchFactory, brmConfigurationService);
    }

    /**
     * Test the conditions where a {@link CMMediatorException} occurs and the {@link BackupManager} progress report exists. The report is recreated.
     * Validates if BMLevel Exist and action is Export progress Report exist, must Update
     * @throws JSONException 
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Test
    public void actionExport_BMPReportExist_Bck_PRNoExist_ProgressReportAdded()
            throws JsonMappingException, JsonProcessingException, JSONException {
        final TestAddProgressReportPatch brmConfigurationPatch = new TestAddProgressReportPatch(action);
        //final TestUpdateProgressReportPatch brmConfigurationPatch = new TestUpdateProgressReportPatch(action);
        brmConfigurationService.patch(brmConfigurationPatch);
        expectLastCall();
        setExport_ProgressReportTestCreated(false);
        expect(progressReportPatchFactory.getPatchToAddProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch));

        replay(brmConfigurationService, progressReportPatchFactory);
        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, backupManager);
    }

    @Test
    public void actionExport_BMPReportExist_Bck_PRNoExist_ProgressReportCreated()
            throws JsonMappingException, JsonProcessingException, JSONException {
        final TestAddProgressReportPatch brmConfigurationPatch = new TestAddProgressReportPatch(action);

        brmConfigurationService.patch(brmConfigurationPatch);
        expectLastCall();
        setExport_ProgressReportTestCreated(false);
        expect(progressReportPatchFactory.getPatchToAddProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch));
        replay(brmConfigurationService, progressReportPatchFactory);
        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, backupManager);
    }

    private void setExport_ProgressReportTestCreated(final boolean progressReportCreated)
            throws JsonMappingException, JsonProcessingException, JSONException {
        final String backupName = "abc";
        final Backup backup = createMock(Backup.class);
        BRMConfiguration brmConfiguration = new ObjectMapper().readValue(getJsonBRMConfiguration(true).toString(),BRMConfiguration.class);
        final BRMEricssonbrmJson brmEricConfiguration = new BRMEricssonbrmJson("ericsson-brm","ericsson-brm",brmConfiguration);
        CMMMessage cmmMessage = createMock (CMMMessage.class);
        expect(cmmMessage.getResource()).andReturn("configurations/ericsson-brm").anyTimes();
        expect(cmmMessage.getHttpMethod()).andReturn(HttpMethod.PATCH).anyTimes();

        expect(action.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
        expect(action.isRestoreOrExport()).andReturn(true).times(2,3);
        expect(action.getBackupName()).andReturn(backupName).times(1,2);

        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricConfiguration)).times(2);
        brmConfigurationService.pushSchemaAndConfiguration(false, false);
        expectLastCall();

        expect(action.isScheduledEvent()).andReturn(false).anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        backup.backupLevelProgressReportResetCreated();
        expectLastCall().anyTimes();
        backup.backupLevelProgressReportSetCreated();
        expectLastCall().times(0,1);
        expect(backup.isBackupLevelProgressReportCreated()).andReturn(progressReportCreated).times(2);
        replay(backup, cmmMessage);

        expect(backupManager.getBackup(backupName, Ownership.READABLE)).andReturn(backup).anyTimes();
        expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(Arrays.asList(backup)).anyTimes();

        replay(action, backupManager);
    }

    /**
     * Test the conditions where a {@link CMMediatorException} occurs and the {@link BackupManager} progress report exists. The report is recreated.
     * @throws JSONException 
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Test
    public void updateProgressReport_SchedulerLevelProgressReportNotEmpty_ProgressReportupdated() throws JsonMappingException, JsonProcessingException, JSONException {
        final TestUpdateProgressReportPatch brmConfigurationPatch = new TestUpdateProgressReportPatch(action);
        final Backup backup = createMock(Backup.class);
        final String backupName = "abc";

        BRMConfiguration brmConfiguration = new ObjectMapper().readValue(getJsonBRMConfiguration(false).toString(),BRMConfiguration.class);
        final BRMEricssonbrmJson brmEricConfiguration = new BRMEricssonbrmJson("ericsson-brm","ericsson-brm",brmConfiguration);

        CMMMessage cmmMessage = createMock (CMMMessage.class);
        expect(cmmMessage.getResource()).andReturn("configurations/ericsson-brm").anyTimes();
        expect(cmmMessage.getHttpMethod()).andReturn(HttpMethod.PATCH).anyTimes();
        expectLastCall();
        expect(action.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
        expect(action.isRestoreOrExport()).andReturn(true).anyTimes();
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricConfiguration)).times(2);
        brmConfigurationService.pushSchemaAndConfiguration(false, false);
        expectLastCall();

        expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(Arrays.asList(mockBackup(false)));
        expect(action.getBackupName()).andReturn(backupName).times(1,2);

        brmConfigurationService.patch(brmConfigurationPatch);
        expectLastCall();
        replay(brmConfigurationService);
        expect(progressReportPatchFactory.getPatchToUpdateProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch));
        expect(backupManager.getBackup(backupName, Ownership.READABLE)).andReturn(backup).times(1,2);
        expect(action.isScheduledEvent()).andReturn(true).anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backup.isBackupLevelProgressReportCreated()).andReturn(true);

        backup.backupLevelProgressReportSetCreated();
        expectLastCall();
        replay(backup, cmmMessage, action, backupManager, progressReportPatchFactory);
        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, backupManager);
    }

    @Test
    public void addProgressReport_TryAddProgressReport_GotException_404Error() {
        final TestAddProgressReportPatch brmConfigurationPatch = new TestAddProgressReportPatch(action);
        // set -1 to be able to control the progressPercentage
        final Action action = newAction("DEFAULT","myAction", ActionType.CREATE_BACKUP, -1.0);
        BRMEricssonbrmJson brmEricssonbrmJson = getInitbrmEricConfiguration();

        CMMMessage cmmMessage = createMock (CMMMessage.class);
        expect(cmmMessage.getResource()).andReturn("configurations/ericsson-brm").anyTimes();
        expect(cmmMessage.getHttpMethod()).andReturn(HttpMethod.PATCH).anyTimes();

//        expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(false).anyTimes();
        brmConfigurationService.patch(brmConfigurationPatch);
        expectLastCall();
//        backupManager.backupManagerLevelProgressReportSetCreated();
//        expectLastCall().times(4);
        expect(action.isScheduledEvent()).andReturn(false).anyTimes();

        // HttpClientErrorException
        expect(action.getProgressPercentage()).andReturn(0.0).times(1);
        expect(action.getProgressPercentage()).andReturn(1.0).times(1);

        // ResourceAccessException
        expect(action.getProgressPercentage()).andReturn(0.0).times(1);
        expect(action.getProgressPercentage()).andReturn(1.0).times(1);

        // will failed 4 times before it continue
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND,"Cannot find configuration 'ericsson-brm'")).times(1);
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST,"Cannot find configuration 'ericsson-brm'")).times(1);

        expect(brmConfigurationService.getEricssonBRMConfiguration()).andThrow(new ResourceAccessException("Resource Access Exception")).times(2);
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricssonbrmJson)); // to be take by 2nd call 
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricssonbrmJson));  // No exception
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andThrow(new NullPointerException("Missing test")).times(2);
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricssonbrmJson));  // No exception

        brmConfigurationService.deleteConfiguration();
        expectLastCall().andThrow(new CMMediatorException("Error"));
        brmConfigurationService.createConfiguration();
        expectLastCall();
        brmConfigurationService.patch(EasyMock.anyObject());
        expectLastCall();
        brmConfigurationService.pushSchemaAndConfiguration(false, false);
        expectLastCall().anyTimes();

        replay(brmConfigurationService);
        expect(progressReportPatchFactory.getPatchToAddProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch)).times(2);
        replay(action, backupManager, progressReportPatchFactory, cmmMessage);
        cmMediatorService.addProgressReport(action);
        cmMediatorService.addProgressReport(action);
        
        cmMediatorService.addProgressReport(action);

        // Exception (NullPointerException)
        cmMediatorService.addProgressReport(action);
        cmMediatorService.addProgressReport(action);

        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, backupManager);
    }

    @Test
    public void addProgressReport_TryAddProgressReport_GotException_RetrySucceed() {
        final TestAddProgressReportPatch brmConfigurationPatch = new TestAddProgressReportPatch(action);
        //final AddBackupPath addBackupPath = new AddBackupPath("/path/action/to/progress-report");
        
        CMMMessage cmmMessage = createMock (CMMMessage.class);
        expect(cmmMessage.getResource()).andReturn("configurations/ericsson-brm").anyTimes();
        expect(cmmMessage.getHttpMethod()).andReturn(HttpMethod.PATCH).anyTimes();
        //expect(cmmMessage.getConfigurationPatch()).andReturn(addBackupPath).anyTimes();
        //expect(progressReportPatchFactory.getMessageToUploadProgressReport(anyObject(), EasyMock.anyInt())).andReturn(cmmMessage).times(1);

        expect(action.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
//        expect(action.isScheduledEvent()).andReturn(false);
//        expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(false).times(1);
//        backupManager.backupManagerLevelProgressReportSetCreated();
//        expectLastCall();
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.empty()).times(2);
        // will be execute when first get CMMediatorException
        brmConfigurationService.deleteConfiguration();
        expectLastCall().andThrow(new CMMediatorException("Error"));
        brmConfigurationService.createConfiguration();
        expectLastCall();
        brmConfigurationService.patch(brmConfigurationPatch);
        expectLastCall();
        expect(action.isRestoreOrExport()).andReturn(true);
        final String backupName = "abc";
        expect(action.getBackupName()).andReturn(backupName);
        final Backup backup = createMock(Backup.class);
        replay(brmConfigurationService);
        expect(progressReportPatchFactory.getPatchToAddProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch)).times(2);
        expect(backupManager.getBackup(backupName, Ownership.OWNED)).andReturn(backup);
        replay(action, backupManager, progressReportPatchFactory, cmmMessage);
        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, backupManager);
    }

     /**
     * Tests the conditions where a {@link CMMediatorException} occurs and the @{@link BackupManager} level progress report does not exist. No action
     * is taken.
     * @throws JSONException 
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Test
    public void addProgressReport_BackupManagerLevelProgressReportExistActionIsNotRestoreOrExport_ProgressReportUpdated()
            throws JsonMappingException, JsonProcessingException, JSONException {
        final Action action = newAction("DEFAULT","myAction", ActionType.CREATE_BACKUP, 1.0);
        final Backup backup = mockBackup(true); // must execute getPatchToUpdateProgressReport

        //expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(true);
        final TestUpdateProgressReportPatch brmConfigurationPatch = new TestUpdateProgressReportPatch(action);
        BRMConfiguration brmConfiguration = new ObjectMapper().readValue(getJsonBRMConfiguration(false).toString(),BRMConfiguration.class);
        final BRMEricssonbrmJson brmEricConfiguration = new BRMEricssonbrmJson("ericsson-brm","ericsson-brm",brmConfiguration);
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricConfiguration));
        expect(progressReportPatchFactory.getPatchToUpdateProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch));
        expect(action.isScheduledEvent()).andReturn(false);
        expect(backupManager.getBackups(Ownership.OWNED)).andReturn(Arrays.asList(backup));
        brmConfigurationService.patch(brmConfigurationPatch);
        expectLastCall();
        replay(action, backupManager, progressReportPatchFactory, brmConfigurationService);
        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, backupManager);
    }

    @Test
    public void addProgressReport_BackupManagerLevelProgressReportExistActionIsNotRestoreOrExport_ProgressReportNotEnqueuedYet_ProgressReportUpdated()
            throws JsonMappingException, JsonProcessingException, JSONException {
        final Action action = newAction("DEFAULT","myAction", ActionType.CREATE_BACKUP, 1.0);
        BRMConfiguration brmConfiguration = new ObjectMapper().readValue(getJsonBRMConfiguration(false).toString(),BRMConfiguration.class);
        final BRMEricssonbrmJson brmEricConfiguration = new BRMEricssonbrmJson("ericsson-brm","ericsson-brm",brmConfiguration);
        final Backup backup = createMock(Backup.class);
        final TestUpdateProgressReportPatch brmConfigurationPatch = new TestUpdateProgressReportPatch(action);

        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricConfiguration));
        expect(backupManager.getBackups(eq(Ownership.OWNED))).andReturn(Arrays.asList(backup));
        brmConfigurationService.patch(brmConfigurationPatch);
        expectLastCall();
//        expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(true);
        expect(progressReportPatchFactory.getPatchToUpdateProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch));

        final Action alreadyEnqueuedAction = newAction("DEFAULT","myAction", ActionType.CREATE_BACKUP, 0.5);
        expect(action.isScheduledEvent()).andReturn(false);
        replay(action, alreadyEnqueuedAction, backupManager, progressReportPatchFactory, brmConfigurationService);

        cmMediatorService.setLastEnqueuedBRMProgressReport(alreadyEnqueuedAction);
        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, alreadyEnqueuedAction, backupManager);
    }

    @Test
    public void addProgressReport_TryUpdateProgressReport_GotException_RetrySucceed()
            throws JsonMappingException, JsonProcessingException, JSONException {
        new TestAddProgressReportPatch(action);
        final Backup backup = mockBackup(true); // must execute getPatchToUpdateProgressReport

        expect(action.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
        BRMConfiguration brmConfiguration = new ObjectMapper().readValue(getJsonBRMConfiguration(false).toString(),BRMConfiguration.class);
        final BRMEricssonbrmJson brmEricConfiguration = new BRMEricssonbrmJson("ericsson-brm","ericsson-brm",brmConfiguration);
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricConfiguration));
        
        expect(action.isRestoreOrExport()).andReturn(false).anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
//        expect(backupManager.isBackupManagerLevelProgressReportCreated()).andReturn(true);
        final TestUpdateProgressReportPatch brmConfigurationPatch = new TestUpdateProgressReportPatch(action);
        expect(progressReportPatchFactory.getPatchToUpdateProgressReport(action)).andReturn(Arrays.asList(brmConfigurationPatch)).times(2);
        expect(backupManager.getBackups(Ownership.OWNED)).andReturn(Arrays.asList(backup));

        // will be execute when first get CMMediatorException
        brmConfigurationService.deleteConfiguration();
        expectLastCall().andThrow(new CMMediatorException("Error"));
        brmConfigurationService.createConfiguration();
        expectLastCall();
        brmConfigurationService.patch(brmConfigurationPatch);
        expectLastCall();
        expect(action.isScheduledEvent()).andReturn(false);
        replay(brmConfigurationService);
        replay(action, backupManager, progressReportPatchFactory);
        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, backupManager);
    }

    @Test
    public void addProgressReport_actionIsCreateBackupAndSchedulerProgressReportNotCreated() {
        Action action = newAction("1", "TestActionId", ActionType.CREATE_BACKUP, 1.0);
        final TestAddProgressReportPatch brmAddProgressConfigurationPatch = new TestAddProgressReportPatch(action);

        final String backupName = "abc";
        expect(action.getBackupName()).andReturn(backupName).anyTimes();
        expect(action.isPartOfHousekeeping()).andReturn(true).anyTimes();
        expect(action.isScheduledEvent()).andReturn(true);
        replay(action);
//        backupManager.backupManagerLevelProgressReportSetCreated();
//        expectLastCall().anyTimes();
        final Backup backup = createMock(Backup.class);
        expect(backupManager.getBackup(backupName, Ownership.READABLE)).andReturn(backup).anyTimes();

        BRMEricssonbrmJson brmEricssonbrmJson = getbrmEricConfiguration(action, true);
        expect(brmConfigurationService.getEricssonBRMConfiguration()).andReturn(Optional.of(brmEricssonbrmJson));
        CMMMessage cmmMessage = createMock (CMMMessage.class);
        expect(cmmMessage.getResource()).andReturn("configurations/ericsson-brm").anyTimes();
        expect(cmmMessage.getHttpMethod()).andReturn(HttpMethod.PATCH).anyTimes();
        expect(progressReportPatchFactory.getPatchToAddProgressReport(action)).andReturn(Arrays.asList(brmAddProgressConfigurationPatch)).times(2);

        brmConfigurationService.patch(brmAddProgressConfigurationPatch);
        expectLastCall();

        replay(backup, backupManager, progressReportPatchFactory, brmConfigurationService, cmmMessage);
        cmMediatorService.addProgressReport(action);
        Awaitility.await().atMost(1, TimeUnit.SECONDS);
        verify(action, backupManager);
    }

    private Action newAction(final String backupmanagerId, final String actionId, final ActionType actionType, final double progressPercentage) {
        Action action = createMock(Action.class);
        expect(action.getActionId()).andReturn(actionId).anyTimes();
        expect(action.getName()).andReturn(actionType).anyTimes();

        if (actionType.equals(ActionType.RESTORE) || actionType.equals(ActionType.EXPORT)) {
            expect(action.isRestoreOrExport()).andReturn(true).anyTimes();
        } else {
            expect(action.isRestoreOrExport()).andReturn(false).anyTimes();
        }

        expect(action.getState()).andReturn(ActionStateType.FINISHED).anyTimes();
        expect(action.getResult()).andReturn(ResultType.NOT_AVAILABLE).anyTimes();
        expect(action.getProgressInfo()).andReturn("pro").anyTimes();
        expect(action.getAdditionalInfo()).andReturn("add").anyTimes();
        expect(action.getStartTime()).andReturn(getDateTime(1985, 1, 2, 3, 4, 5)).anyTimes();
        expect(action.getLastUpdateTime()).andReturn(getDateTime(1986, 1, 2, 3, 4, 5)).anyTimes();
        expect(action.getCompletionTime()).andReturn(getDateTime(1987, 1, 2, 3, 4, 5)).anyTimes();
        expect(action.hasMessages()).andReturn(true).anyTimes();
        expect(action.getAllMessagesAsSingleString()).andReturn("").anyTimes();
        expect(action.getBackupManagerId()).andReturn(backupmanagerId).anyTimes();
        expect(action.belongsToBackup("D")).andReturn(true).anyTimes();
        if (progressPercentage >= 0) {
            expect(action.getProgressPercentage()).andReturn(progressPercentage).anyTimes();
        }
        return action;
    }

    private Action newAction() {
        return newAction("DEFAULT", "TestActionId", ActionType.RESTORE, 1.0);
    }

    private OffsetDateTime getDateTime(final int year, final int month, final int dayOfMonth, final int hour, final int minute, final int second) {
        final LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
        final ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(dateTime);
        return OffsetDateTime.of(dateTime, offset);
    }

    protected JSONObject getEricssonJsonBRMConfiguration(final boolean emptyScheduler) throws JSONException {
        return new JSONObject()
                .put("name", "ericsson-brm")
                .put("title", "ericsson-brm")
                .put("data", getJsonBRMConfiguration(emptyScheduler));
    }

    protected JSONObject getJsonBRMConfiguration(final boolean emptyScheduler) throws JSONException {
        JSONArray brmBackupManagerArray = new JSONArray();
        JSONObject jsonHousekeeping = new JSONObject();
        JSONObject brmJson = new JSONObject();
        JSONObject brmBackupManager = new JSONObject();
        JSONObject brmdata = new JSONObject();
        JSONObject scheduler = new JSONObject();
        JSONArray progress = new JSONArray();
        JSONArray backups = new JSONArray();
        backups.put(getBackup());
        jsonHousekeeping.put("auto-delete", "enabled");
        jsonHousekeeping.put("max-stored-manual-backups", 1);
        brmBackupManager.put("backup-domain","");
        brmBackupManager.put("backup-type","");
        brmBackupManager.put("backup", backups);
        brmBackupManager.put("progress-report",new JSONArray());
        brmBackupManager.put("housekeeping",jsonHousekeeping);
        brmBackupManager.put("id","DEFAULT");
        if (!emptyScheduler) {
            progress.put(getBRMProgressReportJson());
            scheduler.put("progress-report", progress);
            brmBackupManager.put("scheduler", scheduler);
        }
        brmBackupManagerArray.put(brmBackupManager);

        brmJson.put("backup-manager",brmBackupManagerArray);
        brmdata.put("ericsson-brm:brm", brmJson);
        return brmdata;
   }

    protected JSONObject getBackup() throws JSONException {
        JSONObject backup = new JSONObject();
        backup.put("id", "1");
        backup.put("status", "backup-complete");
        backup.put("backup-name", "backup_1");
        backup.put("creation-time", "2024-02-27T10:07:49");
        backup.put("creation-type", "manual");
        return backup;
    }

    protected JSONObject getBRMProgressReportJson() throws JSONException {
        final JSONObject brmProgressReportJson = new JSONObject();
        brmProgressReportJson.put("action-id","101");
        brmProgressReportJson.put("action-name",ActionType.CREATE_BACKUP);
        brmProgressReportJson.put("progress-info","INFO");
        brmProgressReportJson.put("progress-percentage",80);
        brmProgressReportJson.put("progress-report",new JSONArray());
        brmProgressReportJson.put("result-info","result");
        brmProgressReportJson.put("time-action-completed","00:00");
        brmProgressReportJson.put("time-of-last-status-update","00:00");
        return brmProgressReportJson;
    }

    /**
     * A child of @{@link BackupManagerRepository} with predictable behavior for testing puposes.
     */
    private static class TestBackupManagerRepository extends BackupManagerRepository {
        final BackupManager backupManager = createMock(BackupManager.class);
        final List<BackupManager> backupManagerList = Arrays.asList(backupManager);

        @Override
        public BackupManager getBackupManager(final int backupManagerIndex) {
            return backupManager;
        }

        @Override
        public BackupManager getBackupManager(final String backupManagerId) {
            return backupManager;
        }

        @Override
        public List<BackupManager> getBackupManagers() {
            return backupManagerList;
        }
    }

    /**
     * A skeleton implementation of {@link UpdateProgressReportPatch} for testing purposes.
     */
    private static class TestUpdateProgressReportPatch extends UpdateProgressReportPatch {
        /**
         * Creates Patch.
         *
         * @param action to be updated.
         */
        public TestUpdateProgressReportPatch(final Action action) {
            super(action, "");
        }
    }

    /**
     * A skeleton implementation of {@link AddProgressReportPatch} for testing purposes.
     */
    private static class TestAddProgressReportPatch extends AddProgressReportPatch {
        /**
         * Creates Patch.
         *
         * @param action to be updated.
         */
        public TestAddProgressReportPatch(final Action action) {
            super(action, "");
        }
    }
}
