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

import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerInformation;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.EtagNotifIdBase;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMBackupManagerJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchRequest;

public class BackupManagerPatchFactoryTest {

    private BackupManagerPatchFactory factory;
    private BackupManagerRepository backupManagerRepository;
    private EtagNotifIdBase etagNotifIdBase;

    @Before
    public void setup() {
        etagNotifIdBase = new EtagNotifIdBase();
        etagNotifIdBase.updateEtag("11111");
        backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);

        factory = new BackupManagerPatchFactory();
        factory.setBackupManagerRepository(backupManagerRepository);
    }

    @Test
    public void getPatchToAddBackupManager_backupManager_patchToAddIt() throws Exception {
        final AddBackupManagerPatch patch = factory.getPatchToAddBackupManager(mockBackupManager());

        final PatchRequest json = patch.toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals("add", json.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/-", json.getOperations().get(0).getPath());

        final BRMBackupManagerJson backupManagerJson = (BRMBackupManagerJson) json.getOperations().get(0).getValue();
        assertEquals("id", backupManagerJson.getBackupManagerId());
        assertEquals("domain", backupManagerJson.getBackupDomain());
        assertEquals("type", backupManagerJson.getBackupType());
        assertTrue(Optional.ofNullable(backupManagerJson.getBackups()).isEmpty());
        assertTrue(Optional.ofNullable(backupManagerJson.getProgressReports()).isEmpty());
    }

    @Test
    public void getPatchToUpdateBackupManager_backupManager_patchToUpdateIt() throws Exception {
        final BackupManager backupManager = mockBackupManager();

        EasyMock.expect(backupManagerRepository.getIndex("id")).andReturn(5);
        EasyMock.replay(backupManagerRepository);
        final UpdateBackupManagerPatch patch = factory.getPatchToUpdateBackupManager(backupManager);

        final PatchRequest json = patch.toJson();

        assertEquals(3, json.getOperations().size());
        assertEquals("replace", json.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/5/backup-domain", json.getOperations().get(0).getPath());
        assertEquals("domain", json.getOperations().get(0).getValue());

        assertEquals("replace", json.getOperations().get(1).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/5/backup-type", json.getOperations().get(1).getPath());
        assertEquals("type", json.getOperations().get(1).getValue());

        assertEquals("add", json.getOperations().get(2).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/5/sftp-server", json.getOperations().get(2).getPath());
        assertTrue(((List<SftpServerInformation>)json.getOperations().get(2).getValue()).isEmpty());
    }

    private BackupManager mockBackupManager() {
        final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
        EasyMock.expect(backupManager.getBackupManagerId()).andReturn("id");
        EasyMock.expect(backupManager.getBackupDomain()).andReturn("domain");
        EasyMock.expect(backupManager.getBackupType()).andReturn("type");
        EasyMock.expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(new ArrayList<>());
        EasyMock.expect(backupManager.getActions()).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.expect(backupManager.getHousekeeping()).andReturn(new Housekeeping("id", null));
        EasyMock.expect(backupManager.getScheduler()).andReturn(new Scheduler("id", null));
        EasyMock.expect(backupManager.getSftpServers()).andReturn(new ArrayList<>());
        EasyMock.replay(backupManager);
        return backupManager;
    }

}
