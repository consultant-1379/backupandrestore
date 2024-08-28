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
import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus.CORRUPTED;
import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus.INCOMPLETE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;

public class BackupTest {

    private ExecuteFunction function;
    private Backup backup;

    @Before
    public void setup() {
        function = new ExecuteFunction();
        backup = new Backup("123", "456", Optional.empty(), MANUAL, backup -> function.execute());
    }

    @Test
    public void new_backupInformation_createsBackup() {
        backup.setUserLabel("Label");
        backup.addSoftwareVersion(getSoftwareVersion());

        assertEquals("123", backup.getBackupId());
        assertEquals("456", backup.getBackupManagerId());
        assertEquals("123", backup.getName());
        assertEquals(MANUAL, backup.getCreationType());
        assertEquals(INCOMPLETE, backup.getStatus());
        assertEquals("Label", backup.getUserLabel());
        assertNotNull(backup.getCreationTime());
        assertNotNull(backup.getSoftwareVersions().get(0).getDate());
        assertEquals("Description", backup.getSoftwareVersions().get(0).getDescription());
        assertEquals("ProductName", backup.getSoftwareVersions().get(0).getProductName());
        assertEquals("P123", backup.getSoftwareVersions().get(0).getProductNumber());
        assertEquals("productRevision", backup.getSoftwareVersions().get(0).getProductRevision());
        assertEquals("type", backup.getSoftwareVersions().get(0).getType());
    }

    @Test
    public void new_persistedBackup_createsBackup() {
        final PersistedBackup persistedBackup = getPersistedBackup();
        backup = new Backup(persistedBackup, backup -> function.execute());

        assertEquals("qwe", backup.getBackupId());
        assertEquals("asd", backup.getBackupManagerId());
        assertEquals("zxc", backup.getName());
        assertEquals(SCHEDULED, backup.getCreationType());
        assertEquals(CORRUPTED, backup.getStatus());
        assertEquals("rty", backup.getUserLabel());
        assertEquals(DateTimeUtils.parseToOffsetDateTime(persistedBackup.getCreationTime()), backup.getCreationTime());
        assertEquals(2, backup.getSoftwareVersions().size());
    }

    @Test
    public void persist_persistBackup_backupPersisted() throws Exception {
        backup.persist();
        assertTrue(function.wasExecuted());
    }

    @Test
    public void testAddActionLock() throws Exception {
        final Action actionOne = createActionMock("1234", ActionType.EXPORT, "BRM");
        backup.setActionLock(actionOne);
        assertEquals(backup.getActionLock().get(), actionOne);

        // Attempt to lock the backup again with another action
        final Action actionTwo = createActionMock("1234", ActionType.HOUSEKEEPING_DELETE_BACKUP, "BRM");
        backup.setActionLock(actionTwo);
        
        // Check that the lock is still owned by the first action
        assertEquals(backup.getActionLock().get(), actionOne);
    }

    @Test
    public void testRemoveActionLock_validAction() throws Exception {
        final Action actionOne = createActionMock("1234", ActionType.EXPORT, "BRM");

        backup.setActionLock(actionOne);
        assertEquals(backup.getActionLock().get(), actionOne);

        // remove the actionLock
        backup.removeActionLock(actionOne);

        // Verify the lock is removed
        assertTrue(backup.getActionLock().isEmpty());
    }

    @Test
    public void testRemoveActionLock_invalidAction() throws Exception {
        final Action actionOne = createActionMock("1234", ActionType.EXPORT, "BRM");

        backup.setActionLock(actionOne);
        assertEquals(backup.getActionLock().get(), actionOne);

        final Action actionTwo = createActionMock("1234", ActionType.HOUSEKEEPING_DELETE_BACKUP, "BRM");

        // remove the actionLock
        backup.removeActionLock(actionTwo);

        // Verify the existing lock is not removed
        assertEquals(backup.getActionLock().get(), actionOne);
    }

    private Action createActionMock(final String actionId, final ActionType type, final String backupManagerId) {
        Action action = EasyMock.createMock(Action.class);
        EasyMock.expect(action.getActionId()).andReturn(actionId).anyTimes();
        EasyMock.expect(action.getBackupManagerId()).andReturn(backupManagerId).anyTimes();
        EasyMock.expect(action.getName()).andReturn(type).anyTimes();
        EasyMock.replay(action);
        return action;
    }

    private SoftwareVersion getSoftwareVersion() {
        final SoftwareVersion softwareVersion = new SoftwareVersion();
        softwareVersion.setDate(OffsetDateTime.now(ZoneId.systemDefault()).toString());
        softwareVersion.setDescription("Description");
        softwareVersion.setProductName("ProductName");
        softwareVersion.setProductNumber("P123");
        softwareVersion.setProductRevision("productRevision");
        softwareVersion.setType("type");
        return softwareVersion;
    }

    private PersistedBackup getPersistedBackup() {
        final PersistedBackup persistedBackup = new PersistedBackup();
        persistedBackup.setBackupId("qwe");
        persistedBackup.setBackupManagerId("asd");
        persistedBackup.setCreationTime(DateTimeUtils.convertToString(OffsetDateTime.now(ZoneId.systemDefault())));
        persistedBackup.setCreationType(SCHEDULED);
        persistedBackup.setName("zxc");
        persistedBackup.setSoftwareVersions(Arrays.asList(getSoftwareVersion(), getSoftwareVersion()));
        persistedBackup.setStatus(CORRUPTED);
        persistedBackup.setUserLabel("rty");
        return persistedBackup;
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
