/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Optional;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;

public class MediatorRequestPatchTest {

    private static final String ERICSSON_BRM = "/ericsson-brm:brm/backup-manager/1/";
    private MediatorRequestPatch patch;
    private JsonService jsonService;

    @Before
    public void setup() {
        patch = new MediatorRequestPatch();
        jsonService = new JsonService();
    }

    @Test
    public void getBackupManagerIndex_path_backupManagerIndex() {
        patch.setPath(ERICSSON_BRM + "housekeeping/max-stored-manual-backups");
        assertEquals(1, patch.getBackupManagerIndex());
    }

    @Test
    public void getPeriodicEventIndex_path_periodicEventIndex() {
        patch.setPath(ERICSSON_BRM + "scheduler/periodic-event/0/minutes");
        assertEquals(0, patch.getPeriodicEventIndex());
    }

    @Test
    public void getPeriodicEventIndex_pathFromCM_periodicEventIndex() {
        patch.setPath(ERICSSON_BRM + "scheduler/periodic-event/-");
        assertEquals(-1, patch.getPeriodicEventIndex());
    }

    @Test
    public void getPeriodicEventIndex_periodicEventPath_periodicEventIndex() {
        patch.setPath(ERICSSON_BRM + "scheduler/periodic-event/0/minutes");
        assertEquals(0, patch.getPeriodicEventIndex());
    }

    @Test
    public void getPeriodicEventIndex_housekeepingPath_defaultValue() {
        patch.setPath(ERICSSON_BRM + "housekeeping/max-stored-manual-backups");
        assertEquals(-1, patch.getPeriodicEventIndex());
    }

    @Test
    public void getUpdatedElement_housekeepingPath_updatedValue() {
        patch.setPath(ERICSSON_BRM + "housekeeping/max-stored-manual-backups");
        assertEquals("max-stored-manual-backups", patch.getUpdatedElement());
    }

    @Test
    public void getUpdatedElement_schedulerPath_updatedValue() {
        patch.setPath(ERICSSON_BRM + "scheduler/admin-state");
        assertEquals("admin-state", patch.getUpdatedElement());
    }

    @Test
    public void getUpdatedElement_periodicEventPath_updatedValue() {
        patch.setPath(ERICSSON_BRM + "scheduler/periodic-event/0/minutes");
        assertEquals("minutes", patch.getUpdatedElement());
    }

    @Test
    public void getUpdatedElement_addPeriodicEventPath_updatedValue() {
        patch.setPath(ERICSSON_BRM + "scheduler/periodic-event/0");
        assertEquals("periodic-event", patch.getUpdatedElement());
    }

    @Test
    public void getValue_addPeriodicEvent_value() throws JSONException {
        patch.setPath(ERICSSON_BRM + "scheduler/periodic-event/2");

        final Object value = new JSONObject()
                .put("id", "id")
                .put("days", 0)
                .put("hours", 5)
                .put("weeks", 0)
                .put("minutes", 0)
                .put("start-time", "2021-01-31T12:12:12-00:00");

        patch.setValue(value);
        assertEquals("periodic-event", patch.getUpdatedElement());
        assertEquals(2, patch.getPeriodicEventIndex());

        final Optional<BRMPeriodicEventJson> periodicEvent = jsonService.parseJsonString(String.valueOf(patch.getValue()), BRMPeriodicEventJson.class);
        assertEquals(5, periodicEvent.get().getHours());
        assertEquals("2021-01-31T12:12:12-00:00", periodicEvent.get().getStartTime());
        assertNull(periodicEvent.get().getStopTime());
    }

    @Test
    public void getValue_updatePeriodicEvent_value() {
        patch.setPath(ERICSSON_BRM + "scheduler/periodic-event/2/hours");
        patch.setValue(0);
        assertEquals("hours", patch.getUpdatedElement());
        assertEquals(2, patch.getPeriodicEventIndex());
        assertEquals(0, (Integer) patch.getValue());
    }
}
