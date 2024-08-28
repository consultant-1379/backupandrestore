/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.cminterface.operation;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_DISABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static org.junit.Assert.assertEquals;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMHousekeepingJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchRequest;

public class HousekeepingPatchFactoryTest {

    private static final String HOUSEKEEPING_URL = "/ericsson-brm:brm/backup-manager/6/housekeeping/";
    private BackupManagerPatchFactory backupManagerPatchFactory;
    private HousekeepingPatchFactory housekeepingPatchFactory;

    @Before
    public void setup() {
        backupManagerPatchFactory = EasyMock.createMock(BackupManagerPatchFactory.class);
        housekeepingPatchFactory = new HousekeepingPatchFactory();
        EasyMock.expect(backupManagerPatchFactory.getPathToBackupManager("666")).andReturn("6");
        EasyMock.replay(backupManagerPatchFactory);
        housekeepingPatchFactory.setBackupManagerPatchFactory(backupManagerPatchFactory);
    }

    @Test
    public void getPatchToAddHousekeeping_housekeepingInformation_patchToAddIt() {
        final AddHousekeepingPatch patch = housekeepingPatchFactory.getPatchToAddHousekeeping(new Housekeeping("666", null));
        final PatchRequest json = patch.toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals("add", json.getOperations().get(0).getOperation());
        assertEquals(HOUSEKEEPING_URL, json.getOperations().get(0).getPath());

        final BRMHousekeepingJson brmhousekeeping = (BRMHousekeepingJson) json.getOperations().get(0).getValue();
        assertEquals(AUTO_DELETE_ENABLED, brmhousekeeping.getAutoDelete());
        assertEquals(1, brmhousekeeping.getMaxNumberBackups());
    }

    @Test
    public void getPatchToUpdateHousekeeping_housekeepingInformation_patchToUpdateIt() {
        final UpdateHousekeepingPatch patch = housekeepingPatchFactory.getPatchToUpdateHousekeeping(
                new Housekeeping(2, AUTO_DELETE_DISABLED, "666", null));
        final PatchRequest json = patch.toJson();

        assertEquals(2, json.getOperations().size());
        assertEquals("replace", json.getOperations().get(0).getOperation());
        assertEquals(HOUSEKEEPING_URL + "auto-delete", json.getOperations().get(0).getPath());
        assertEquals(AUTO_DELETE_DISABLED, json.getOperations().get(0).getValue());
    }
}
