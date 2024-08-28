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
package com.ericsson.adp.mgmt.backupandrestore.backup;

import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType.MANUAL;
import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType.SCHEDULED;
import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus.*;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.BackupManagerFileService;
import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupLimitExceededException;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnexpectedBackupManagerException;
import com.ericsson.adp.mgmt.backupandrestore.productinfo.ProductInfoService;
import com.ericsson.adp.mgmt.backupandrestore.util.BackupLimitValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;

import io.kubernetes.client.openapi.ApiException;

public class BackupRepositoryTest {

    private BackupRepository backupRepository;
    private BackupFileService backupFileService;
    private BackupLimitValidator backupLimitValidator;
    private CMMediatorService cmMediatorService;
    private ProductInfoService productInfoService;
    private BackupManagerFileService backupManagerFileService;
    private BackupManagerRepository backupManagerRepository;
    private SoftwareVersion softwareVersion;
    private MockedAgentFactory agentMock;

    @Before
    public void setup() {
        backupFileService = createMock(BackupFileService.class);
        backupLimitValidator = createMock(BackupLimitValidator.class);
        cmMediatorService = createMock(CMMediatorService.class);
        productInfoService = createMock(ProductInfoService.class);
        backupManagerFileService = createMock(BackupManagerFileService.class);
        backupManagerRepository = createMock(BackupManagerRepository.class);


        backupRepository = new BackupRepository();
        backupRepository.setBackupFileService(backupFileService);
        backupRepository.setBackupLimitValidator(backupLimitValidator);
        backupRepository.setCmMediatorService(cmMediatorService);
        backupRepository.setProductInfoService(productInfoService);
        backupRepository.setBackupManagerRepository(backupManagerRepository);
        backupRepository.setBackupManagerFileService(backupManagerFileService);

        softwareVersion = new SoftwareVersion();
        agentMock = new MockedAgentFactory();
    }

    @Test
    public void createBackup_backupManagerAndActionAndAgents_createsBackupThatCanBePersistedAndPersistsIt() throws Exception {
        final BackupManager backupManager = createMock(BackupManager.class);

        expect(backupManager.getBackupManagerId()).andReturn("qwe").anyTimes();
        expectLastCall();

        backupManager.addBackup(anyObject(), eq(Ownership.OWNED));
        expectLastCall().anyTimes();

        final SoftwareVersion productInfo = new SoftwareVersion();
        expect(productInfoService.getAppProductInfo()).andReturn(productInfo);

        cmMediatorService.addBackup(eq(backupManager), anyObject());
        expectLastCall().anyTimes();
        cmMediatorService.updateBackup(eq(backupManager), anyObject());
        expectLastCall().anyTimes();
        cmMediatorService.updateBackupAndWait(eq(backupManager), anyObject());
        expectLastCall();
        backupFileService.writeToFile(anyObject());
        expectLastCall().times(2);

        backupLimitValidator.validateLimit(anyString());
        expectLastCall().anyTimes();

        expect(backupManagerRepository.getChildren("qwe")).andAnswer(Stream::empty).anyTimes();
        expect(backupManagerRepository.getBackupManager("qwe")).andReturn(backupManager).once();
        expect(backupManagerRepository.getBackupManagers()).andReturn(List.of(backupManager)).anyTimes();

        replay(backupLimitValidator, cmMediatorService, backupFileService, backupManager, productInfoService, backupManagerRepository);

        final Backup backup = backupRepository.createBackup(backupManager, mockAction(false), Arrays.asList(agentMock.mockedAgent(softwareVersion)));
        backup.persist();

        verify(backupLimitValidator, cmMediatorService, backupFileService, backupManager, productInfoService);
        assertTrue(backup.softwareVersions.contains(productInfo));
        assertEquals(MANUAL, backup.getCreationType());
    }

    @Test
    public void createBackup_actionWithIsScheduledEventTrue_createsBackupWithScheduledCreationTypeAndPersistsIt() throws Exception {
        final BackupManager backupManager = createMock(BackupManager.class);

        expect(backupManager.getBackupManagerId()).andReturn("qwe").anyTimes();
        expectLastCall();

        backupManager.addBackup(anyObject(), eq(Ownership.OWNED));
        expectLastCall().anyTimes();

        final SoftwareVersion productInfo = new SoftwareVersion();
        expect(productInfoService.getAppProductInfo()).andReturn(productInfo);

        cmMediatorService.addBackup(eq(backupManager), anyObject());
        expectLastCall().anyTimes();
        cmMediatorService.updateBackup(eq(backupManager), anyObject());
        expectLastCall().anyTimes();
        cmMediatorService.updateBackupAndWait(eq(backupManager), anyObject());
        expectLastCall();
        backupFileService.writeToFile(anyObject());
        expectLastCall().times(2);

        backupLimitValidator.validateLimit(anyString());
        expectLastCall().anyTimes();

        expect(backupManagerRepository.getBackupManagers()).andReturn(List.of(backupManager)).anyTimes();
        expect(backupManagerRepository.getBackupManager("qwe")).andReturn(backupManager).anyTimes();
        expect(backupManagerRepository.getChildren("qwe")).andAnswer(Stream::empty).anyTimes();

        replay(backupLimitValidator, cmMediatorService, backupFileService, backupManager, productInfoService, backupManagerRepository);

        final Backup backup = backupRepository.createBackup(backupManager, mockAction(true), Arrays.asList(agentMock.mockedAgent(softwareVersion)));
        backup.persist();

        verify(backupLimitValidator, cmMediatorService, backupFileService, backupManager, productInfoService);
        assertTrue(backup.softwareVersions.contains(productInfo));
        assertEquals(SCHEDULED, backup.getCreationType());
    }

    @Test(expected = BackupLimitExceededException.class)
    public void createBackup_notAllowedToCreateBackup_throwsException() throws Exception {
        final BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn(BackupManager.DEFAULT_BACKUP_MANAGER_ID);
        backupLimitValidator.validateLimit(BackupManager.DEFAULT_BACKUP_MANAGER_ID);
        expectLastCall().andThrow(new BackupLimitExceededException(1));

        replay(backupLimitValidator, backupManager);

        backupRepository.createBackup(backupManager, null, null);
    }

    @Test
    public void createBackup_appProductInfoNotFound_usesOrchProductInfo() throws Exception {
        final BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn("qwe").anyTimes();
        backupManager.addBackup(anyObject(), eq(Ownership.OWNED));
        expectLastCall();

        backupLimitValidator.validateLimit(anyString());
        expectLastCall();

        final SoftwareVersion orchProductInfo = new SoftwareVersion();

        expect(productInfoService.getAppProductInfo()).andThrow(new ApiException());
        expect(productInfoService.getOrchestratorProductInfo()).andReturn(orchProductInfo);

        cmMediatorService.addBackup(eq(backupManager), anyObject());
        expectLastCall();

        backupFileService.writeToFile(anyObject());
        expectLastCall().times(1);

        expect(backupManagerRepository.getChildren("qwe")).andReturn(Stream.empty()).once();
        expect(backupManagerRepository.getBackupManagers()).andReturn(List.of(backupManager)).anyTimes();

        replay(backupLimitValidator, cmMediatorService, backupFileService, backupManager, productInfoService, backupManagerRepository);

        final Backup backup = backupRepository.createBackup(backupManager, mockAction(false), Arrays.asList(agentMock.mockedAgent(softwareVersion)));

        verify(backupLimitValidator, cmMediatorService, backupFileService, backupManager, productInfoService);

        assertTrue(backup.softwareVersions.stream().anyMatch(softwareVersion -> softwareVersion.equals(orchProductInfo)));
    }

    @Test
    public void importBackup_backupFileContentAndBackupManager_createsAndReturnsABackup() throws Exception {
        final BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn("qwe").anyTimes();
        backupManager.addBackup(anyObject(), eq(Ownership.OWNED));
        expectLastCall();

        backupLimitValidator.validateLimit("qwe");
        expectLastCall();

        cmMediatorService.addBackup(eq(backupManager), anyObject());
        expectLastCall().anyTimes();

        expect(backupFileService.readBackup("abc")).andReturn(getPersistedBackup("1", CORRUPTED));
        backupFileService.writeToFile(anyObject());
        expectLastCall();

        expect(backupManagerRepository.getChildren("qwe")).andReturn(Stream.empty()).once();

        replay(backupLimitValidator, cmMediatorService, backupFileService, backupManager, backupManagerRepository);

        backupRepository.importBackup("abc", backupManager);

        verify(backupLimitValidator, cmMediatorService, backupFileService, backupManager);
    }

    @Test(expected = UnexpectedBackupManagerException.class)
    public void importBackup_backupBelongingToAnotherBackupManager_throwsException() throws Exception {
        backupLimitValidator.validateLimit(anyString());
        expectLastCall();

        expect(backupFileService.readBackup("abc")).andReturn(getPersistedBackup("1", CORRUPTED));

        final BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn("notTheSameBackupManager").anyTimes();

        replay(backupLimitValidator, cmMediatorService, backupFileService, backupManager);

        backupRepository.importBackup("abc", backupManager);
    }

    @Test(expected = BackupLimitExceededException.class)
    public void importBackup_notAllowedToImportBackup_throwsException() throws Exception {
        final BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn(BackupManager.DEFAULT_BACKUP_MANAGER_ID);
        backupLimitValidator.validateLimit(anyObject()) ;
        expectLastCall().andThrow(new BackupLimitExceededException(1));
        replay(backupLimitValidator);

        backupRepository.importBackup(null, backupManager);
    }

    @Test
    public void deleteBackup_backupAndBackupManager_deletesBackupInFileSystemAndCM() throws Exception {
        cmMediatorService.deleteBackupAndWait("qwe", 3);
        expectLastCall();

        backupFileService.deleteBackup("qwe", "abc");
        expectLastCall();

        final Backup backup = createMock(Backup.class);
        expect(backup.getBackupId()).andReturn("abc").anyTimes();

        final BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn("qwe").anyTimes();
        backupManager.removeBackup(backup);
        expectLastCall();
        expect(backupManager.getBackupIndex("abc")).andReturn(3);

        expect(backupManagerRepository.getChildren("qwe")).andReturn(Stream.empty()).once();

        replay(backupLimitValidator, cmMediatorService, backupFileService, backupManager, backup, backupManagerRepository);

        backupRepository.deleteBackup(backup, backupManager);

        verify(backupLimitValidator, cmMediatorService, backupFileService, backupManager);
    }

    @Test
    public void getBackups_somePersistedBackupsAreIncomplete_marksIncompleteBackupsAsCorruptedAndGetBackupsOrderedByDate() throws Exception {
        final int YEAR = 2019;
        final int MONTH = 1 ;
        final int DAYOFMONTH = 1;
        final int HOUR = 1;
        final int MINUTE = 1;
        final int ONE_SECOND = 1;
        final int TWO_SECOND = 2;
        final int THREE_SECOND=3;

        final BackupManager manager = createMock(BackupManager.class);
        expect(manager.getBackupManagerId()).andReturn("qwe").anyTimes();

        expect(backupManagerRepository.getChildren("qwe")).andReturn(Stream.empty()).anyTimes();
        expect(backupManagerRepository.getBackupManager("qwe")).andReturn(manager);

        replay(manager, backupManagerRepository);

        expect(backupFileService.getBackups("1"))
                .andReturn(Arrays.asList(getPersistedBackup("1", INCOMPLETE, getDateTime(YEAR, MONTH, DAYOFMONTH, HOUR, MINUTE, THREE_SECOND)),
                        getPersistedBackup("2", CORRUPTED, getDateTime(YEAR, MONTH, DAYOFMONTH, HOUR, MINUTE, TWO_SECOND)),
                        getPersistedBackup("3", COMPLETE, getDateTime(YEAR, MONTH, DAYOFMONTH, HOUR, MINUTE, ONE_SECOND))));
        backupFileService.writeToFile(anyObject());
        expectLastCall();
        replay(backupFileService);

        final List<Backup> backups= backupRepository.getBackups("1");
        backups.stream().forEach(backup -> backupRepository.corruptBackupIfIncomplete(backup));

        assertEquals(3, backups.size());
        assertEquals("3", backups.get(0).getBackupId());
        assertEquals(COMPLETE, backups.get(0).getStatus());
        assertEquals("2", backups.get(1).getBackupId());
        assertEquals(CORRUPTED, backups.get(1).getStatus());
        assertEquals("1", backups.get(2).getBackupId());
        assertEquals(CORRUPTED, backups.get(2).getStatus());
        verify(backupFileService);
    }

    @Test
    public void getBackups_getBackupsOrderedByStatusandCreationTime_ListbackupsOrdered() throws Exception {
    	final int YEAR = 2019;
    	final int MONTH = 1 ; 
    	final int FIRST_DAYOFMONTH = 1;
    	final int FORTH_DAYOFMONTH = 1;
    	final int ELEVENTH_DAYOFMONTH = 1;
    	final int TWELTH_DAYOFMONTH = 1;
    	final int HOUR = 1;
    	final int MINUTE = 1;
    	final int SECOND = 1;
    	
        expect(backupFileService.getBackups("1"))
        .andReturn(Arrays.asList(
                getPersistedBackup("1", INCOMPLETE, getDateTime(YEAR, MONTH, TWELTH_DAYOFMONTH, HOUR, MINUTE, SECOND)),
                getPersistedBackup("2", CORRUPTED, getDateTime(YEAR, MONTH, ELEVENTH_DAYOFMONTH, HOUR, MINUTE, SECOND)),
                getPersistedBackup("3", COMPLETE, getDateTime(YEAR, MONTH, FORTH_DAYOFMONTH, HOUR, MINUTE, SECOND)),
                getPersistedBackup("4", COMPLETE, getDateTime(YEAR, MONTH, FIRST_DAYOFMONTH, HOUR, MINUTE, SECOND))
                )).anyTimes();

        backupFileService.writeToFile(anyObject());
        expectLastCall().times(2);
        replay(backupFileService);

        final List<Backup> backups= backupRepository.getBackupsForAutoDeletion("1",1);

        assertEquals(3, backups.size());
        assertEquals("2", backups.get(0).getBackupId());
        assertEquals(CORRUPTED, backups.get(0).getStatus());
        assertEquals("1", backups.get(1).getBackupId());
        assertEquals(INCOMPLETE, backups.get(1).getStatus());
        assertEquals("4", backups.get(2).getBackupId());
        assertEquals(COMPLETE, backups.get(2).getStatus());
        assertEquals(getDateTime(YEAR, MONTH, FIRST_DAYOFMONTH, HOUR, MINUTE, SECOND), backups.get(2).getCreationTime());
    }

    @Test
    public void getBackups_MaxNumberBackupReached_isReached() throws Exception {
        expect(backupLimitValidator.isMaxNumberOfBackupsReached("1", 1)).andReturn(true);
        expectLastCall();
        replay(backupLimitValidator);
        backupRepository.setBackupLimitValidator(backupLimitValidator);
        assertTrue(backupRepository.isMaxBackupLimitReached("1", 1));
    }

    private PersistedBackup getPersistedBackup(final String id, final BackupStatus status) {
        return getPersistedBackup(id, status, OffsetDateTime.now(ZoneId.systemDefault()));
    }

    private PersistedBackup getPersistedBackup(final String id, final BackupStatus status, final OffsetDateTime creationTime) {
        final PersistedBackup backup = new PersistedBackup();
        backup.setBackupId(id);
        backup.setStatus(status);
        backup.setCreationTime(DateTimeUtils.convertToString(creationTime));
        backup.setBackupManagerId("qwe");
        return backup;
    }

    private Action mockAction(final boolean isScheduled) {
        final Action action = createMock(Action.class);
        expect(action.getBackupName()).andReturn("myBackup").anyTimes();
        expect(action.getBackupCreationTime()).andReturn(Optional.empty());
        expect(action.isScheduledEvent()).andReturn(isScheduled);
        replay(action);

        return action;
    }

    private OffsetDateTime getDateTime(final int year, final int month, final int dayOfMonth, final int hour, final int minute, final int second) {
        final LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
        final ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(dateTime);
        return OffsetDateTime.of(dateTime, offset);
    }

}
