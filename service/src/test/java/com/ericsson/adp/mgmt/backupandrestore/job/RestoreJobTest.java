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
package com.ericsson.adp.mgmt.backupandrestore.job;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus.COMPLETE;
import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus.CORRUPTED;
import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus.INCOMPLETE;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.backup.*;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMClient;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.exception.AgentsNotAvailableException;
import com.ericsson.adp.mgmt.backupandrestore.exception.JobFailedException;
import com.ericsson.adp.mgmt.backupandrestore.exception.RestoreDownloadException;
import com.ericsson.adp.mgmt.backupandrestore.exception.SemanticVersionNullValueException;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStage;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.RestoreJobStage;
import com.ericsson.adp.mgmt.backupandrestore.productinfo.ProductInfoService;
import com.ericsson.adp.mgmt.backupandrestore.productinfo.exception.MissingFieldsInConfigmapException;
import com.ericsson.adp.mgmt.backupandrestore.productinfo.exception.UnableToRetrieveDataFromConfigmapException;
import com.ericsson.adp.mgmt.backupandrestore.productinfo.exception.UnsupportedSoftwareVersionException;
import com.ericsson.adp.mgmt.backupandrestore.restore.FragmentFileService;
import com.ericsson.adp.mgmt.backupandrestore.restore.RestoreInformation;
import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import com.ericsson.adp.mgmt.control.Preparation;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.metadata.Fragment;

import io.kubernetes.client.openapi.ApiException;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class RestoreJobTest {

    private static final String BACKUP_MANAGER_ID = "BACKUP_MANAGER_ID";

    private RestoreJob job;
    private BackupLocationService backupLocationService;
    private ActionRepository actionRepository;
    private BackupManager backupManager;
    private BackupManager parentManager;
    private Action action;
    private CMMediatorService cmMediatorService;
    private JobStage<RestoreJob> stage;
    private MockedAgentFactory agentMock;
    private CMMClient cmmClient;

    @Before
    public void setup() {
        job = new RestoreJob();
        agentMock = new MockedAgentFactory();
        backupManager = createMock(BackupManager.class);
        parentManager = createMock(BackupManager.class);
        actionRepository = createMock(ActionRepository.class);
        expect(backupManager.getBackupManagerId()).andReturn(BACKUP_MANAGER_ID).anyTimes();
        expect(backupManager.getParent()).andReturn(Optional.empty()).anyTimes();
        expect(parentManager.getBackupManagerId()).andReturn("PARENT").anyTimes();
        backupLocationService = createMock(BackupLocationService.class);
        action = createMock(Action.class);
        expect(action.getActionId()).andReturn("1").anyTimes();

        cmMediatorService = EasyMock.createMock(CMMediatorService.class);

        stage = EasyMock.createMock(JobStage.class);

        ReflectionTestUtils.setField(stage, "wasTriggerCalled", new AtomicBoolean(true));

        job.setBackupLocationService(backupLocationService);
        job.setBackupManager(backupManager);
        job.setAction(action);
        job.setAgents(Arrays.asList(agentMock.mockedAgent("1"), agentMock.mockedAgent("2")));
        job.setCmMediatorService(cmMediatorService);
        job.setJobStage(stage);
        job.setActionRepository(actionRepository);
        cmmClient = new CMMClient();
        cmmClient.setFlagEnabled(true);
        cmmClient.setMaxAttempts("5");
        cmmClient.setMaxDelay("3000");
        expect(cmMediatorService.getCMMClient()).andReturn(cmmClient);
    }

    @Test
    public void createRestoreInformation_agentId_returnsRestoreInformationWithDataRelatedToAgent() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("1", COMPLETE, true, "5.1.2")).anyTimes();
        expect(backupManager.getAgentVisibleBRMId()).andReturn(BACKUP_MANAGER_ID).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
        .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final RestoreJobStage restoreJobStage = createMock(RestoreJobStage.class);
        ReflectionTestUtils.setField(restoreJobStage, "wasTriggerCalled", new AtomicBoolean(true));
        restoreJobStage.trigger();

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("PRODUCT");
        expect(productInfoService.getProductMatchType()).andReturn("ANY");

        replay(action, backupManager, fragmentFileService, backupLocationService, restoreJobStage, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);
        job.setJobStage(restoreJobStage);

        job.triggerJob();

        final RestoreInformation restoreInformation = job.createRestoreInformation("1");
        final Preparation message = restoreInformation.buildPreparationMessage();
        assertEquals("test", message.getBackupName());
        assertEquals(2, message.getFragmentCount());
    }

    @Test
    public void createRestoreInformation_agentId_backupOwnedByParent() throws Exception {
        final BackupManager backupManager;
        backupManager = createMock(BackupManager.class);

        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("1", COMPLETE, true, "5.1.2")).anyTimes();
        expect(backupManager.getAgentVisibleBRMId()).andReturn("PARENT").anyTimes();
        expect(backupManager.isVirtual()).andReturn(true).anyTimes();
        expect(backupManager.ownsBackup("test")).andReturn(false).anyTimes();
        expect(backupManager.getParent()).andReturn(Optional.of(parentManager)).anyTimes();
        expect(backupManager.getBackupManagerId()).andReturn(BACKUP_MANAGER_ID).anyTimes();
        expect(parentManager.getAgentVisibleBRMId()).andReturn("PARENT").anyTimes();

        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments("PARENT", "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final RestoreJobStage restoreJobStage = createMock(RestoreJobStage.class);
        ReflectionTestUtils.setField(restoreJobStage, "wasTriggerCalled", new AtomicBoolean(true));
        restoreJobStage.trigger();

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("PRODUCT");
        expect(productInfoService.getProductMatchType()).andReturn("ANY");
        expectLastCall();

        Metadata metadata = Metadata.newBuilder().setAgentId("1")
                .setFragment(Fragment.newBuilder().setFragmentId("1").setSizeInBytes("bytes").setVersion("version"))
                .setBackupName("test").build();
        BackupFolder backupFolder = new BackupFolder(Path.of("/"));
        expect(backupLocationService.getBackupFolder("PARENT", "test")).andReturn(backupFolder);
        expect(backupLocationService.getFragmentFolder(metadata, "PARENT", "test")).andReturn(createMock(FragmentFolder.class));

        replay(action, backupManager, parentManager, fragmentFileService, backupLocationService, restoreJobStage, productInfoService);
        job.setBackupManager(backupManager);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);
        job.setJobStage(restoreJobStage);

        job.triggerJob();
        job.getFragmentFolder(metadata);

        final RestoreInformation restoreInformation = job.createRestoreInformation("1");
        final Preparation message = restoreInformation.buildPreparationMessage();
        assertEquals("test", message.getBackupName());
        assertEquals(2, message.getFragmentCount());
    }

    @Test
    public void createRestoreInformation_agentId_backupOwnedByVBRM() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("1", COMPLETE, true, "5.1.2")).anyTimes();
        expect(backupManager.getAgentVisibleBRMId()).andReturn(BACKUP_MANAGER_ID).anyTimes();
        expect(backupManager.isVirtual()).andReturn(true).anyTimes();
        expect(backupManager.ownsBackup("test")).andReturn(true).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final RestoreJobStage restoreJobStage = createMock(RestoreJobStage.class);
        ReflectionTestUtils.setField(restoreJobStage, "wasTriggerCalled", new AtomicBoolean(true));
        restoreJobStage.trigger();

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("PRODUCT");
        expect(productInfoService.getProductMatchType()).andReturn("ANY");
        expectLastCall();

        Metadata metadata = Metadata.newBuilder().setAgentId("1")
                .setFragment(Fragment.newBuilder().setFragmentId("1").setSizeInBytes("bytes").setVersion("version"))
                .setBackupName("test").build();
        BackupFolder backupFolder = new BackupFolder(Path.of("/"));
        expect(backupLocationService.getBackupFolder(BACKUP_MANAGER_ID, "test")).andReturn(backupFolder);
        expect(backupLocationService.getFragmentFolder(metadata, BACKUP_MANAGER_ID, "test")).andReturn(createMock(FragmentFolder.class)).times(2);

        replay(action, backupManager, fragmentFileService, backupLocationService, restoreJobStage, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);
        job.setJobStage(restoreJobStage);

        job.triggerJob();
        job.getFragmentFolder(metadata);

        final RestoreInformation restoreInformation = job.createRestoreInformation("1");
        final Preparation message = restoreInformation.buildPreparationMessage();
        assertEquals("test", message.getBackupName());
        assertEquals(2, message.getFragmentCount());
    }

    @Test(expected = RestoreDownloadException.class)
    public void triggerJob_restoringIncompleteBackup_throwsException() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", INCOMPLETE, false, "5.1.2")).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1", "2");
        replay(action, backupManager, backupLocationService);

        job.triggerJob();
    }

    @Test(expected = RestoreDownloadException.class)
    public void triggerJob_restoringCorruptBackup_throwsException() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", CORRUPTED, false, "5.1.2")).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1", "2");
        replay(action, backupManager, backupLocationService);

        job.triggerJob();
    }

    @Test(expected = AgentsNotAvailableException.class)
    public void triggerJob_oneAgentNotAvailable_throwsException() {
        expect(action.getBackupName()).andReturn("test");
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2"));
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1", "2", "3");
        replay(action, backupManager, backupLocationService);

        job.triggerJob();
    }

    @Test
    public void completeJob_restoreFinishedSuccessfully_resetsCM() throws Exception {
        expect(stage.isStageSuccessful()).andReturn(true);
        cmMediatorService.prepareCMMediator(false);
        EasyMock.expectLastCall().anyTimes();

        replay(stage, cmMediatorService);

        job.completeJob();

        verify(cmMediatorService);
    }

    @Test
    public void completeJob_restoreFinishedUnsuccessfully_resetsCMThenThrowsException() throws Exception {
        expect(stage.isStageSuccessful()).andReturn(false);
        cmMediatorService.prepareCMMediator(false);
        expectLastCall().anyTimes();

        replay(stage, cmMediatorService);

        try {
            job.completeJob();
            fail();
        } catch (final JobFailedException e) {
            EasyMock.verify(cmMediatorService);
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void fail_jobFailed_resetsCM() throws Exception {
        stage.trigger();
        expectLastCall();
        cmMediatorService.prepareCMMediator(false);
        expectLastCall().anyTimes();

        expect(stage.moveToFailedStage()).andReturn(stage);
        expect(stage.getProgressPercentage()).andReturn(0.98d);

        action.setProgressPercentage(0.98d);
        expectLastCall();
        action.persist();
        expectLastCall();

        replay(stage, cmMediatorService, action);

        job.fail();

        verify(stage, cmMediatorService, action);
    }

    @Test
    public void fail_jobFailed_markBackupAsCorrupted() throws Exception {
        stage.trigger();
        expectLastCall().anyTimes();
        backupManager.backupManagerLevelProgressReportResetCreated();
        expectLastCall().anyTimes();
        cmMediatorService.prepareCMMediator(false);
        expectLastCall().anyTimes();

        expect(stage.moveToFailedStage()).andReturn(stage);
        expect(stage.getProgressPercentage()).andReturn(0.0d);

        action.setProgressPercentage(0.0d);
        expectLastCall();
        action.persist();
        expectLastCall();

        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        final Backup backup = createMock(Backup.class);
        expect(backup.getBackupId()).andReturn("test").anyTimes();
        expect(backup.getStatus()).andReturn(COMPLETE).anyTimes();
        expect(backup.getSoftwareVersions()).andReturn(Arrays.asList(getSoftwareVersion("APPLICATION_INFO", "abc", "1.0.0"))).anyTimes();

        backup.setStatus(CORRUPTED);
        expectLastCall();
        backup.persist();
        expectLastCall();
        expect(backup.getName()).andReturn("test").anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(backup).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getProductMatchType()).andReturn("EXACT_MATCH");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "1.0.0")).anyTimes();
        expect(productInfoService.getSelectedMatchType()).andReturn("PRODUCT");
        expectLastCall();

        replay(stage, cmMediatorService, action, backupManager, backupLocationService, productInfoService, backup);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        job.markBackupAsCorrupted();

        job.fail();

        verify(stage, cmMediatorService, action, backupManager, productInfoService, backup);
    }

    @Test(expected = UnsupportedSoftwareVersionException.class)
    public void validateProductNumber_productNumberMismatchForExactMatch_throwsException() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
        .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("PRODUCT");
        expect(productInfoService.getProductMatchType()).andReturn("EXACT_MATCH");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "123", "1.0.0")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();
    }

    @Test
    public void validateProductNumber_productNumberMatchesForExactMatch_noExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
        .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("PRODUCT");
        expect(productInfoService.getProductMatchType()).andReturn("EXACT_MATCH");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "1.0.0")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, productInfoService);
    }

    @Test(expected = UnsupportedSoftwareVersionException.class)
    public void validateSemVer_productNumberMatchesForExactMatch_NullValue_UnsupportedSoftwareVersionException() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "1.0.0")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
        .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("1.1.1");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", null)).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, productInfoService);
    }

    @Test(expected = UnableToRetrieveDataFromConfigmapException.class)
    public void validateSemVer_productNumberMatchesForExactMatch_NoConfigMap_UnsupportedSoftwareVersionException() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "1.1.1")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
        .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("1.1.1");
        expect(productInfoService.getAppProductInfo()).andThrow(new ApiException());

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, productInfoService);
    }

    @Test
    public void validateSemVer_semVerMatchesForGreaterThan_noExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("5.1.1");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "5.1.3")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, productInfoService);
    }

    @Test(expected = UnsupportedSoftwareVersionException.class)
    public void validateSemVer_semVerIsLowerThanLowestAllowed_exceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductMatchType()).andReturn("ANY");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("5.2.2");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "5.3.2")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();
    }

    @Test(expected = UnsupportedSoftwareVersionException.class)
    public void validateSemVer_semVerIsGreaterThanConfigMapSemVer_exceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductMatchType()).andReturn("ANY");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("5.1.0");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "5.1.1")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();
    }

    @Test
    public void validateSemVer_semVerMatchesForGreaterThan_noInterferenceFromProductMatchType() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductMatchType()).andReturn("EXACT_MATCH").anyTimes();
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("1.9.9");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "invalid", "5.1.5")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, productInfoService);
    }

    @Test
    public void validateProductNumber_ProductNumberMatchesForExactMatch_noInterferenceFromSemVerMatchType() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("PRODUCT");
        expect(productInfoService.getProductMatchType()).andReturn("EXACT_MATCH");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "2.0.0")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, productInfoService);
    }

    @Test
    public void validateSemVer_boundary_noExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("5.1.2");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "5.1.2")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, productInfoService);
    }

    @Test
    public void validateSemVer_checkMajorVersion_noExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("4.1.2");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "6.1.2")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, productInfoService);
    }

    @Test
    public void validateSemVer_checkMinorVersion_noExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("5.0.2");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "5.2.2")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, productInfoService);
    }

    @Test(expected = UnsupportedSoftwareVersionException.class)
    public void validateSemVer_checkMajorVersion_exceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductMatchType()).andReturn("ANY");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("6.1.2");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "7.1.2")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();
    }

    @Test(expected = UnableToRetrieveDataFromConfigmapException.class)
    public void validateSemVer_ProductInfoRetrievalFailed_ExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "4.3031.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductMatchType()).andReturn("ANY");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("4.3030.2");
        expect(productInfoService.getAppProductInfo()).andThrow(new NullPointerException("null"));

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();
    }

    @Test
    public void validateSemVer_checkUnusualMinorVersion_noExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "4.3031.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("4.3030.2");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "4.3357.0")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, productInfoService);
    }

    @Test
    public void validateSemVer_checkBackupSemverWithOnlyMajorAndMinorVersion_noExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "4.0")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("4.0.0");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "4.0")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();
        
        verify(action, backupManager, productInfoService);
    }

    @Test
    public void validateSemVer_checkProductInfoSemverWithOnlyMajorAndMinorVersion_noExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "4.0.0")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("4.0");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "4.0")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, productInfoService);
    }

    @Test(expected = UnsupportedSoftwareVersionException.class)
    public void validateSemVer_checkUnusualMinorVersion_exceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "4.2762.52")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductMatchType()).andReturn("ANY");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("4.3357.2");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "4.3358.0")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();
    }

    @Test
    public void validateSemVer_checkLowestAllowedVersion_noExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        expect(backupLocationService.getAgentIdsForBackup(anyObject(), anyString())).andReturn(Arrays.asList("1"));

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance())).anyTimes();

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("ANY");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("5.1.1");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "5.1.3")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
    }

    @Test
    public void validateSemVer_checkLowestAllowedVersionIgnoresConfigmap_noExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        expect(backupLocationService.getAgentIdsForBackup(anyObject(), anyString())).andReturn(Arrays.asList("1"));

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance())).anyTimes();

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("ANY");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("5.1.1");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "5.0.0")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
    }

    @Test(expected = UnsupportedSoftwareVersionException.class)
    public void validateSemVer_checkLowestAllowedVersionFailsRestore_exceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.0")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("ANY");
        expect(productInfoService.getProductMatchType()).andReturn("ANY");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("5.1.1");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "5.2.0")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();
    }

    @Test(expected = NumberFormatException.class)
    public void validateCompareVersionMethod_incorrectFormat_exceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductMatchType()).andReturn("ANY");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("5.1.2-ep1");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", "5.1.2")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();
    }

    @Test
    public void compareSemanticVersion_NullValues_exceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("SEMVER");
        expect(productInfoService.getSemVerMatchType()).andReturn("GREATER_THAN");
        expect(productInfoService.getProductMatchType()).andReturn("ANY");
        expect(productInfoService.getProductLowestAllowedVersion()).andReturn("5.1.2");
        expect(productInfoService.getAppProductInfo()).andReturn(getSoftwareVersion("APPLICATION_INFO", "abc", null)).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();
    }

    @Test(expected = UnableToRetrieveDataFromConfigmapException.class)
    public void validateProductNumber_exceptionWhileFetchingProductInfo_throwsException() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
                .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("PRODUCT");
        expect(productInfoService.getProductMatchType()).andReturn("EXACT_MATCH");
        expect(productInfoService.getAppProductInfo()).andThrow(new MissingFieldsInConfigmapException("boo"));

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();
    }

    @Test
    public void validateProductNumber_productNumberMatchesForListType_noExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
        .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("PRODUCT");
        expect(productInfoService.getProductMatchType()).andReturn("LIST");
        expect(productInfoService.getProductNumberList()).andReturn(Arrays.asList("abc")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();
        
        verify(action, backupManager, productInfoService);
    }

    @Test(expected = UnsupportedSoftwareVersionException.class)
    public void validateProductNumber_productNumberMismatchForListType_noExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
        .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("PRODUCT");
        expect(productInfoService.getProductMatchType()).andReturn("LIST");
        expect(productInfoService.getProductNumberList()).andReturn(Arrays.asList("123")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();
    }

    @Test
    public void validateProductNumber_productNumberPresentInList_noExceptionThrown() throws Exception {
        expect(action.getBackupName()).andReturn("test").anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(backupManager.getBackup("test", Ownership.READABLE)).andReturn(mockBackup("APPLICATION_INFO", COMPLETE, false, "5.1.2")).anyTimes();
        expect(backupManager.isVirtual()).andReturn(false).anyTimes();
        setAgentsThatParticipatedInBackup("test", "1");

        final FragmentFileService fragmentFileService = createMock(FragmentFileService.class);
        expect(fragmentFileService.getFragments(BACKUP_MANAGER_ID, "test", "1"))
        .andReturn(Arrays.asList(Fragment.getDefaultInstance(), Fragment.getDefaultInstance()));

        final ProductInfoService productInfoService = createMock(ProductInfoService.class);
        expect(productInfoService.getSelectedMatchType()).andReturn("PRODUCT");
        expect(productInfoService.getProductMatchType()).andReturn("LIST");
        expect(productInfoService.getProductNumberList()).andReturn(Arrays.asList("123", "abc")).anyTimes();

        replay(action, backupManager, fragmentFileService, backupLocationService, productInfoService);
        job.setFragmentFileService(fragmentFileService);
        job.setProductInfoService(productInfoService);

        job.triggerJob();

        verify(action, backupManager, productInfoService);
    }

    private Backup mockBackup(final String agentID, final BackupStatus status, final boolean isVirtual, final String semVer) {
        final Backup backup = createMock(Backup.class);
        expect(backup.getBackupId()).andReturn("test").anyTimes();
        expect(backup.getStatus()).andReturn(status).anyTimes();
        if (isVirtual) {
            expect(backup.getName()).andReturn("test").anyTimes();
        }
        expect(backup.getSoftwareVersions()).andReturn(Arrays.asList(getSoftwareVersion(agentID, "abc", semVer))).anyTimes();
        replay(backup);
        return backup;
    }

    private SoftwareVersion getSoftwareVersion(final String agentID, final String productNumber, final String semVer) {
        final SoftwareVersion softwareVersion = new SoftwareVersion();
        softwareVersion.setAgentId(agentID);
        softwareVersion.setDate("");
        softwareVersion.setDescription("");
        softwareVersion.setProductName("");
        softwareVersion.setProductNumber(productNumber);
        softwareVersion.setProductRevision("");
        softwareVersion.setType("");
        softwareVersion.setSemanticVersion(semVer);

        return softwareVersion;
    }

    private void setAgentsThatParticipatedInBackup(final String backupId, final String... agentIds) {
        expect(backupLocationService.getAgentIdsForBackup(anyObject(), anyString())).andReturn(Arrays.asList(agentIds));
        expect(backupLocationService.getBackupFolder(BACKUP_MANAGER_ID, backupId)).andReturn(new BackupFolder(Path.of("path")));
    }
}
