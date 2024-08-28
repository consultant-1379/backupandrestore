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
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;

import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMBackupJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMSoftwareVersionJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchRequest;

public class BackupPatchFactoryTest {

    private BackupPatchFactory backupPatchFactory;
    private BackupManagerPatchFactory backupManagerPatchFactory;
    private BackupManagerRepository backupManagerRepository;

    @Before
    public void setup() {
        backupManagerPatchFactory = EasyMock.createMock(BackupManagerPatchFactory.class);
        backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);

        backupPatchFactory = new BackupPatchFactory();
        backupPatchFactory.setBackupManagerPatchFactory(backupManagerPatchFactory);
        backupPatchFactory.setBackupManagerRepository(backupManagerRepository);
    }

    @Test
    public void getPatchToAddBackup_backup_patchToAddIt() throws Exception {
        EasyMock.expect(backupManagerPatchFactory.getPathToBackupManager("666")).andReturn("6");
        EasyMock.replay(backupManagerPatchFactory);

        final AddBackupPatch patch = backupPatchFactory.getPatchToAddBackup(mockBackupManager(), mockBackup("D"));

        final PatchRequest json = patch.toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals("add", json.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/6/backup/-", json.getOperations().get(0).getPath());

        final BRMBackupJson brmBackup = (BRMBackupJson) json.getOperations().get(0).getValue();
        assertEquals("D", brmBackup.getBackupId());
        assertEquals("E", brmBackup.getName());
        assertEquals("1984-01-02T03:04:05Z", brmBackup.getCreationTime());
        assertEquals(BackupCreationType.SCHEDULED, brmBackup.getCreationType());
        assertEquals("backup-corrupted", brmBackup.getCmRepresentationOfStatus());

        assertEquals(1, brmBackup.getSoftwareVersionJsons().size());

        final BRMSoftwareVersionJson brmSoftwareVersion = brmBackup.getSoftwareVersionJsons().get(0);
        assertEquals("1984-01-02T03:04:06Z", brmSoftwareVersion.getDate());
        assertEquals("G", brmSoftwareVersion.getDescription());
        assertEquals("H", brmSoftwareVersion.getProductName());
        assertEquals("I", brmSoftwareVersion.getProductNumber());
        assertEquals("J", brmSoftwareVersion.getProductRevision());
        assertEquals("K", brmSoftwareVersion.getType());
    }

    @Test
    public void getPatchToAddInitialBackup_backup_patchToAddInitial() throws Exception {
        EasyMock.expect(backupManagerPatchFactory.getPathToBackupManager("666")).andReturn("6");
        EasyMock.replay(backupManagerPatchFactory);

        final AddInitialBackupPatch patch = backupPatchFactory.getPatchToAddInitialBackup(mockBackupManager(), mockBackup("D"));

        final PatchRequest json = patch.toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals("add", json.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/6/backup", json.getOperations().get(0).getPath());

        final BRMBackupJson[] arrBRMBackup = (BRMBackupJson[]) json.getOperations().get(0).getValue();
        final BRMBackupJson brmBackup = arrBRMBackup[0];
        assertEquals("D", brmBackup.getBackupId());
        assertEquals("E", brmBackup.getName());
        assertEquals("1984-01-02T03:04:05Z", brmBackup.getCreationTime());
        assertEquals(BackupCreationType.SCHEDULED, brmBackup.getCreationType());
        assertEquals("backup-corrupted", brmBackup.getCmRepresentationOfStatus());

        assertEquals(1, brmBackup.getSoftwareVersionJsons().size());

        final BRMSoftwareVersionJson brmSoftwareVersion = brmBackup.getSoftwareVersionJsons().get(0);
        assertEquals("1984-01-02T03:04:06Z", brmSoftwareVersion.getDate());
        assertEquals("G", brmSoftwareVersion.getDescription());
        assertEquals("H", brmSoftwareVersion.getProductName());
        assertEquals("I", brmSoftwareVersion.getProductNumber());
        assertEquals("J", brmSoftwareVersion.getProductRevision());
        assertEquals("K", brmSoftwareVersion.getType());
    }

    @Test
    public void getPatchToUpdateBackup_backup_patchToUpdateIt() throws Exception {
        EasyMock.expect(backupManagerRepository.getBackupManager("666")).andReturn(mockBackupManager());

        EasyMock.expect(backupManagerPatchFactory.getPathToBackupManager("666")).andReturn("0");
        EasyMock.replay(backupManagerPatchFactory, backupManagerRepository);

        final UpdateBackupPatch patch = backupPatchFactory.getPatchToUpdateBackup(mockBackupManager(), mockBackup("D"));

        final PatchRequest json = patch.toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals("replace", json.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/0/backup/3/status", json.getOperations().get(0).getPath());
        assertEquals("backup-corrupted", json.getOperations().get(0).getValue());
    }

    @Test
    public void getPatchToDeleteBackup_backup_patchToDeleteIt() throws Exception {
        EasyMock.expect(backupManagerRepository.getBackupManager("666")).andReturn(mockBackupManager());

        EasyMock.expect(backupManagerPatchFactory.getPathToBackupManager("666")).andReturn("0");
        EasyMock.replay(backupManagerPatchFactory, backupManagerRepository);

        final DeleteBackupPatch patch = backupPatchFactory.getPatchToDeleteBackup("666", 3);

        final PatchRequest json = patch.toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals("remove", json.getOperations().get(0).getOperation());
        assertEquals("/ericsson-brm:brm/backup-manager/0/backup/3", json.getOperations().get(0).getPath());
    }

    private Backup mockBackup(final String id) {
        final Backup backup = EasyMock.createMock(Backup.class);
        EasyMock.expect(backup.getBackupId()).andReturn(id);
        EasyMock.expect(backup.getName()).andReturn("E");
        EasyMock.expect(backup.getCreationTime()).andReturn(getDateTime(1984, 1, 2, 3, 4, 5)).anyTimes();
        EasyMock.expect(backup.getCreationType()).andReturn(BackupCreationType.SCHEDULED).anyTimes();
        EasyMock.expect(backup.getStatus()).andReturn(BackupStatus.CORRUPTED).anyTimes();
        EasyMock.expect(backup.getSoftwareVersions()).andReturn(Arrays.asList(mockSoftwareVersion())).anyTimes();
        EasyMock.expect(backup.getBackupManagerId()).andReturn("666").anyTimes();
        EasyMock.expect(backup.getUserLabel()).andReturn("");
        EasyMock.replay(backup);
        return backup;
    }

    private SoftwareVersion mockSoftwareVersion() {
        final SoftwareVersion softwareVersion = EasyMock.createMock(SoftwareVersion.class);
        EasyMock.expect(softwareVersion.getDate()).andReturn("1984-01-02T03:04:06Z");
        EasyMock.expect(softwareVersion.getDescription()).andReturn("G");
        EasyMock.expect(softwareVersion.getProductName()).andReturn("H");
        EasyMock.expect(softwareVersion.getProductNumber()).andReturn("I");
        EasyMock.expect(softwareVersion.getProductRevision()).andReturn("J");
        EasyMock.expect(softwareVersion.getType()).andReturn("K");
        EasyMock.expect(softwareVersion.getSemanticVersion()).andReturn("5.0.0");
        EasyMock.expect(softwareVersion.getCommercialVersion()).andReturn("5.0.0");
        EasyMock.replay(softwareVersion);
        return softwareVersion;
    }

    private BackupManager mockBackupManager() {
        final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
        EasyMock.expect(backupManager.getBackups(eq(Ownership.READABLE))).andReturn(Arrays.asList(mockBackup("A"), mockBackup("B"), mockBackup("C"), mockBackup("D"), mockBackup("E")));
        EasyMock.expect(backupManager.getBackupIndex("D")).andReturn(3);
        expect(backupManager.getBackupManagerId()).andReturn("666").anyTimes();
        EasyMock.replay(backupManager);
        return backupManager;
    }

    private OffsetDateTime getDateTime(final int year, final int month, final int dayOfMonth, final int hour, final int minute, final int second) {
        final LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
        final ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(dateTime);
        return OffsetDateTime.of(dateTime, offset);
    }

}
