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

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_DISABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.YangEnabledDisabled.DISABLED;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.PersistedBackup;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.BackupManagerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.PersistedBackupManager;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.exception.ActionNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupIdAlreadyExistsException;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidActionException;

public class BackupManagerTest {

    private BackupManager backupManager;
    private ExecuteFunction function;

    @Before
    public void setup() {
        function = new ExecuteFunction();
        final BackupManagerRepository repo = createMock(BackupManagerRepository.class);
        expect(repo.getPersistedSftpServers(anyString())).andReturn(new ArrayList<>()).anyTimes();
        expect(repo.getChildren(anyString())).andAnswer(Stream::empty).anyTimes();
        replay(repo);
        backupManager = new BackupManager(
                "123",
                new Housekeeping("123", null),
                new Scheduler("123", null),
                backupManager -> function.execute(),
                null,
                repo,
                new VirtualInformation());
    }

    @Test
    public void getActions_triesToModifyActions_isNotAbleTo() throws Exception {
        final List<Action> actions = backupManager.getActions();
        final int originalNumberOfActions = actions.size();

        actions.add(null);

        assertEquals(originalNumberOfActions, backupManager.getActions().size());
    }

    @Test
    public void getAgentVisibleBRMId() {
        assertEquals(backupManager.getBackupManagerId(), backupManager.getAgentVisibleBRMId());
        VirtualInformation virtualInformation = new VirtualInformation("DEFAULT", new ArrayList<>());
        BackupManager virtualBackupManager = new BackupManager(
                "DEFAULT-virtual",
                new Housekeeping("123", null),
                new Scheduler("123", null),
                backupManager -> function.execute(),
                null,
                null,
                virtualInformation
        );
        assertEquals("DEFAULT", virtualBackupManager.getAgentVisibleBRMId());
    }

    @Test
    public void getParent() {
        VirtualInformation virtualInformation = new VirtualInformation("DEFAULT", new ArrayList<>());
        BackupManagerRepository backupManagerRepository = createMock(BackupManagerRepository.class);
        expect(backupManagerRepository.getBackupManager("DEFAULT")).andReturn(backupManager);
        replay(backupManagerRepository);
        BackupManager virtualBackupManager = new BackupManager(
                "DEFAULT-virtual",
                new Housekeeping("123", null),
                new Scheduler("123", null),
                backupManager -> function.execute(),
                null,
                backupManagerRepository,
                virtualInformation
        );

        assertEquals(Optional.of(backupManager), virtualBackupManager.getParent());
        assertEquals(Optional.empty(), backupManager.getParent());
    }

    @Test
    public void test_getOwnedBackup() {
        VirtualInformation virtualInformation = new VirtualInformation("DEFAULT", new ArrayList<>());
        BackupManagerRepository backupManagerRepository2 = createMock(BackupManagerRepository.class);
        BackupManager virtualBackupManager = new BackupManager(
                "DEFAULT-virtual",
                new Housekeeping("123", null),
                new Scheduler("123", null),
                backupManager -> function.execute(),
                null,
                backupManagerRepository2,
                virtualInformation
        );
        BackupManager parent = createMock(BackupManager.class);
        Backup backup = createBackup("backup1");
        Backup backup2 = createBackup("backup2");
        List<Backup> parentBackupList = new ArrayList<>();
        parentBackupList.add(backup2);

        expect(backupManagerRepository2.getBackupManager("DEFAULT")).andReturn(parent).anyTimes();
        expect(backupManagerRepository2.getChildren(anyString())).andAnswer(Stream::empty).anyTimes();
        expect(parent.getBackups(eq(Ownership.READABLE))).andReturn(parentBackupList).anyTimes();
        replay(parent, backupManagerRepository2);

        virtualBackupManager.addBackup(backup, Ownership.OWNED);
        assertEquals(backup, virtualBackupManager.getBackup("backup1", Ownership.OWNED));
        assertThrows(BackupNotFoundException.class, () -> virtualBackupManager.getBackup("notOwned", Ownership.OWNED));
    }

    @Test
    public void getAction_actionId_returnsAction() throws Exception {
        final Action action = createMock(Action.class);
        expect(action.getActionId()).andReturn("123").anyTimes();
        replay(action);
        backupManager.addAction(action);
        assertEquals("123", backupManager.getAction("123").getActionId());
    }

    @Test(expected = ActionNotFoundException.class)
    public void getAction_inexstingActionId_throwsException() throws Exception {
        backupManager.getAction("12345");
    }

    @Test(expected = InvalidActionException.class)
    public void addAction_actionWithSameIdAlreadyExists_throwsException() throws Exception {
        final Action action = createMock(Action.class);
        expect(action.getActionId()).andReturn("123").anyTimes();
        replay(action);
        backupManager.addAction(action);
        backupManager.addAction(action);
    }

    @Test
    public void persist_backupManagerAndPersistFunction_persistFunctionIsCalled() throws Exception {
        backupManager.persist();
        assertTrue(function.wasExecuted());
    }

    @Test
    public void getScheduler_backupManager_returnsDefaultSchedulerInformation() {
        final Scheduler scheduler = backupManager.getScheduler();

        assertEquals(AdminState.UNLOCKED, scheduler.getAdminState());
        assertEquals("SCHEDULED_BACKUP", scheduler.getScheduledBackupName());
        assertNull(scheduler.getMostRecentlyCreatedAutoBackup());
        assertNull(scheduler.getNextScheduledTime());
        assertEquals(new ArrayList<>(), scheduler.getPeriodicEvents());
        assertEquals(DISABLED, scheduler.getAutoExport());
        assertEquals("", scheduler.getAutoExportPassword());
        assertNull(scheduler.getAutoExportUri());
    }

    @Test
    public void getBackups_triesToModifyListOfBackups_isNotAbleTo() throws Exception {
        List<Backup> backups = backupManager.getBackups(Ownership.READABLE);

        assertTrue(backups.isEmpty());
        backupManager.addBackup(createBackup("456456456"), Ownership.OWNED);
        assertEquals(1, backupManager.getBackups(Ownership.READABLE).size());

        backups = backupManager.getBackups(Ownership.READABLE);
        backups.add(createBackup("234234234"));

        assertEquals(1, backupManager.getBackups(Ownership.READABLE).size());
    }

    @Test
    public void getBackup_backupId_returnsBackup() throws Exception {
        backupManager.addBackup(createBackup("123"), Ownership.OWNED);
        assertEquals("123", backupManager.getBackup("123", Ownership.READABLE).getBackupId());
    }

    @Test(expected = BackupNotFoundException.class)
    public void getBackup_inexstingBackupId_throwsException() throws Exception {
        backupManager.getBackup("4321", Ownership.READABLE);
    }

    @Test(expected = BackupIdAlreadyExistsException.class)
    public void addBackup_triesToAddBackupWithExistingId_throwsExceptionThatIdExists() throws Exception {
        backupManager.addBackup(createBackup("123"), Ownership.OWNED);
        backupManager.addBackup(createBackup("123"), Ownership.OWNED);
    }

    @Test
    public void getBackupIndex_backupId_returnsIndexOfThatBackup() throws Exception {
        backupManager.addBackup(createBackup("123"), Ownership.OWNED);
        backupManager.addBackup(createBackup("456"), Ownership.OWNED);
        backupManager.addBackup(createBackup("789"), Ownership.OWNED);

        assertEquals(3, backupManager.getBackups(Ownership.READABLE).size());
        assertEquals(3, backupManager.getBackups(Ownership.OWNED).size());
        assertEquals(0, backupManager.getBackupIndex("123"));
        assertEquals(1, backupManager.getBackupIndex("456"));
        assertEquals(2, backupManager.getBackupIndex("789"));
    }

    @Test
    public void getBackupIndex_backupName_returnsIndexOfThatBackup() throws Exception {
        backupManager.addBackup(createBackup("123"), Ownership.OWNED);
        backupManager.addBackup(createBackup("456"), Ownership.OWNED);
        backupManager.addBackup(createBackup("789"), Ownership.OWNED);

        assertEquals("123", backupManager.getBackupByName("Name-" + "123", Ownership.OWNED).getBackupId());
    }

    @Test
    public void getHousekeepingInformation_newBackupManager_returnsDefaultValues() {
        assertEquals(1, backupManager.getHousekeeping().getMaxNumberBackups());
        assertEquals(AUTO_DELETE_ENABLED, backupManager.getHousekeeping().getAutoDelete());
    }

    @Test
    public void getHousekeepingInformation_updatedHousekeepingInformation_returnsUpdatedValues() {
        backupManager.getHousekeeping().setAutoDelete(AUTO_DELETE_DISABLED);
        backupManager.getHousekeeping().setMaxNumberBackups(2);
        assertEquals(2, backupManager.getHousekeeping().getMaxNumberBackups());
        assertEquals(AUTO_DELETE_DISABLED, backupManager.getHousekeeping().getAutoDelete());
    }

    @Test
    public void removeBackup_unknownBackup_doesNothing() throws Exception {
        backupManager.addBackup(createBackup("123"), Ownership.OWNED);
        backupManager.addBackup(createBackup("456"), Ownership.OWNED);

        backupManager.removeBackup(backupManager.getBackup("123", Ownership.READABLE));
        assertEquals(1, backupManager.getBackups(Ownership.READABLE).size());
        assertEquals("456", backupManager.getBackups(Ownership.READABLE).get(0).getBackupId());

        backupManager.removeBackup(createBackup("789"));

        assertEquals(1, backupManager.getBackups(Ownership.READABLE).size());
    }

    @Test
    public void isDefault_backupManagerWithIdDifferentFromDefault_isNotDefaultBackupManager() throws Exception {
        assertFalse(backupManager.isDefault());
    }

    @Test
    public void isDefault_backupManagerWithDefaultBackupManagerId_isDefaultBackupManager() throws Exception {
        assertTrue(new BackupManager(
                BackupManager.DEFAULT_BACKUP_MANAGER_ID,
                null, null, null, null, null, new VirtualInformation()).isDefault());
    }

    @Test
    public void isDefault_backupManagerWithDefaultProgressReport() {
        assertFalse(backupManager.isBackupManagerLevelProgressReportCreated());
    }

    @Test
    public void isDefault_backupManagerUpdateProgressReport() {
        backupManager.backupManagerLevelProgressReportSetCreated();
        assertTrue(backupManager.isBackupManagerLevelProgressReportCreated());
    }

    @Test
    public void backupManagerUpdateProgressReport_IsEmpty() {
        backupManager.backupManagerLevelProgressReportResetCreated();
        assertFalse(backupManager.isBackupManagerLevelProgressReportCreated());
    }

    @Test
    public void ownsAgent_defaultBackupManager_ownsAnyAgentRegardlessOfScope() throws Exception {
        backupManager = new BackupManager(BackupManager.DEFAULT_BACKUP_MANAGER_ID, null, null, null, null, null, null);

        assertTrue(backupManager.ownsAgent("", null));
        assertTrue(backupManager.ownsAgent("anyScope", null));
    }

    @Test
    public void ownsAgent_notDefaultBackupManager_ownsAgentThatMatchesScope() throws Exception {
        assertTrue(backupManager.ownsAgent("123", null));
    }

    @Test
    public void ownsAgent_notDefaultBackupManager_doesNotOwnAgentThatDoesNotMatchScope() throws Exception {
        assertFalse(backupManager.ownsAgent("456", null));
    }

    @Test
    public void ownsAgent_notDefaultBackupManagerAndAgentWithMultipleScopes_ownsAgentThatMatchesScope()  {
        assertTrue(backupManager.ownsAgent("abc;123;456", null));
    }

    @Test
    public void ownsAgent_notDefaultBackupManagerAndAgentWithMultipleScopes_doesNotOwnAgentThatDoesNotMatchScope() {
        assertFalse(backupManager.ownsAgent("456;789;abc", null));
    }

    @Test
    public void ownsAgent_scopeIsNull_doesNotOwnAgent() {
        assertFalse(backupManager.ownsAgent(null, null));
    }


    @Test
    public void tarballURIPayload_checkBackupIsInBackupManager(){
        final String timestamp = "-2021-02-03T10:23:26.885567Z.tar.gz";

        final String backupWithHyphen = "my-Backup";
        backupManager.addBackup(createBackup(backupWithHyphen), Ownership.OWNED);
        assertThrows(BackupIdAlreadyExistsException.class, () -> backupManager.assertBackupIsNotPresent(backupWithHyphen + timestamp));

        final String backupCreatedWithTimestamp = "myBackup-2021-02-02T14:00:02.99062Z";
        backupManager.addBackup(createBackup(backupCreatedWithTimestamp), Ownership.OWNED);
        assertThrows(BackupIdAlreadyExistsException.class, () -> backupManager.assertBackupIsNotPresent(backupCreatedWithTimestamp + timestamp));

        final String backupCreatedWithTimestamp2 = "myBackup-2021-02-02T14:00:02+01:00";
        backupManager.addBackup(createBackup(backupCreatedWithTimestamp2), Ownership.OWNED);
        assertThrows(BackupIdAlreadyExistsException.class, () -> backupManager.assertBackupIsNotPresent(backupCreatedWithTimestamp2 + timestamp));

        final String backupCreatedWithTimestampOffset2 = "myBackup-2021-02-02T14:00:02-01:00";
        backupManager.addBackup(createBackup(backupCreatedWithTimestampOffset2), Ownership.OWNED);
        assertThrows(BackupIdAlreadyExistsException.class, () -> backupManager.assertBackupIsNotPresent(backupCreatedWithTimestampOffset2 + timestamp));

        final String backupCreatedWithTimestampStartingWithHyphenAndAppendedTimestamp = "-2021-02-02T14:00:02.99062Z.tar.gz-2021-02-02T14:00:02.99062Z";
        backupManager.addBackup(createBackup(backupCreatedWithTimestampStartingWithHyphenAndAppendedTimestamp), Ownership.OWNED);
        assertThrows(BackupIdAlreadyExistsException.class, () -> backupManager.assertBackupIsNotPresent(backupCreatedWithTimestampStartingWithHyphenAndAppendedTimestamp + timestamp));

        final String backupCreatedWithTimestampStartingWithNoHyphen = "2021-02-03T10:23:26.885567Z.tar.gz";
        backupManager.addBackup(createBackup(backupCreatedWithTimestampStartingWithNoHyphen), Ownership.OWNED);
        assertThrows(BackupIdAlreadyExistsException.class, () -> backupManager.assertBackupIsNotPresent(backupCreatedWithTimestampStartingWithNoHyphen + timestamp));

        final String backupCreatedWithTimestampStartingWithHyphen = "-2021-02-03T10:23:26.885567Z.tar.gz";
        backupManager.addBackup(createBackup(backupCreatedWithTimestampStartingWithHyphen), Ownership.OWNED);
        assertThrows(BackupIdAlreadyExistsException.class, () -> backupManager.assertBackupIsNotPresent(backupCreatedWithTimestampStartingWithHyphen + timestamp));

    }

    @Test
    public void tarballURIPayload_checkBackupIsNotInBackupManager(){
        final String timestamp = "-2021-02-03T10:23:26.885567Z.tar.gz";
        final String backup = "my-Backup";
        assertDoesNotThrow(() -> backupManager.assertBackupIsNotPresent(backup + timestamp));
    }

    @Test
    public void reloadSucceeds_ValuesUpdated() throws IOException {
        final BackupManagerFileService fileService = createMock(BackupManagerFileService.class);
        final PersistedBackupManager persisted = createMock(PersistedBackupManager.class);

        expect(fileService.getPersistedBackupManager(backupManager.getBackupManagerId())).andReturn(Optional.of(persisted)).once();
        expect(persisted.getBackupManagerId()).andReturn(backupManager.getBackupManagerId()).once();
        final String newDomain = backupManager.getBackupDomain() + "loaded";
        expect(persisted.getBackupDomain()).andReturn(newDomain).once();
        final String newType = backupManager.getBackupType() + "loaded";
        expect(persisted.getBackupType()).andReturn(newType).once();

        replay(fileService, persisted);

        backupManager.reload(fileService);

        assertTrue(function.wasExecuted);
        assertEquals(backupManager.getBackupDomain(), newDomain);
        assertEquals(backupManager.getBackupType(), newType);
        verify(fileService, persisted);
    }

    @Test(expected = FileNotFoundException.class)
    public void reloadReadFails() throws IOException {
        final BackupManagerFileService fileService = createMock(BackupManagerFileService.class);

        expect(fileService.getPersistedBackupManager(backupManager.getBackupManagerId())).andReturn(Optional.empty()).once();

        replay(fileService);

        backupManager.reload(fileService);

        assertTrue(function.wasExecuted);
        verify(fileService);
    }

    @Test
    public void testEqualsReflexive() {
        assertTrue(backupManager.equals(backupManager));
    }

    @Test
    public void testEqualsWithNull() {
        assertFalse(backupManager.equals(null));
    }

    @Test
    public void testHashCodeConsistency() {
        int initialHashCode = backupManager.hashCode();
        assertEquals(initialHashCode, backupManager.hashCode());  // Repeated calls should return the same value
    }

    @Test
    public void testEqualObjectsHaveSameHashCode() {
        final BackupManagerRepository repo = createMock(BackupManagerRepository.class);
        final BackupManager backupManagerNew = new BackupManager(
                "123",
                new Housekeeping("123", null),
                new Scheduler("123", null),
                backupManager -> function.execute(),
                null,
                repo,
                new VirtualInformation());

        assertTrue(backupManager.equals(backupManagerNew));  // Ensure objects are equal
        assertNotEquals(backupManager.hashCode(), backupManagerNew.hashCode());
    }

    @Test
    public void testEqualsSymmetric() {
        final BackupManagerRepository repo = createMock(BackupManagerRepository.class);
        final BackupManager backupManagerNew = new BackupManager(
                "123",
                new Housekeeping("123", null),
                new Scheduler("123", null),
                backupManager -> function.execute(),
                null,
                repo,
                new VirtualInformation());
        assertTrue(backupManager.equals(backupManagerNew));
        assertTrue(backupManagerNew.equals(backupManager));
    }

    private Backup createBackup(final String backupId) {
        final Backup backup = createMock(Backup.class);
        expect(backup.getBackupId()).andReturn(backupId).anyTimes();
        expect(backup.getName()).andReturn("Name-" + backupId).anyTimes();
        replay(backup);
        return backup;
    }

    private class ExecuteFunction {

        private boolean wasExecuted;

        public void execute() {
            wasExecuted = true;
        }

        public boolean wasExecuted() {
            return wasExecuted;
        }

    }

}
