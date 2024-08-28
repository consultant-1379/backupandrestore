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
package com.ericsson.adp.mgmt.backupandrestore.test;

import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.CONFIGURATION_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.HOUSEKEEPING_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.HOUSEKEEPING_BACKUP_MANAGER_ACTION;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.HOUSEKEEPING_BACKUP_MANAGER_URL_V3;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V3_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V2_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.URLMAPPING;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.MediatorNotificationHandler;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.EmptyPayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.HousekeepingInformation;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.EtagNotifIdBase;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.UpdateHousekeepingRequest;

/**
 * Created to group the use of common requirements to test Housekeeping
 */
public abstract class HousekeepingSystemTest extends SystemTestWithAgents {
    protected static final TestRestTemplate restTemplate = new TestRestTemplate();

    protected static final String URL_MEDIATOR=V3_BASE_URL + URLMAPPING;

    protected static final String AGENT_HOUSEKEEPING="backupHouseKeepingAgent";
    protected static final String AUTO_DELETE="auto-delete";
    protected static final String MAX_STORED_BACKUP="max-stored-manual-backups";
    protected static final String HOUSEKEEPING = "/housekeeping";
    protected static final String TEST_URL_EXIST = V1_BASE_URL + "/" + CONFIGURATION_URL;
    protected static final String BASE_ETAG_VALUE = "11111";
    protected static final String CONFIG_ETAG_VALUE = "99999";


    private SystemTestAgent agent;
    protected EtagNotifIdBase etagNotifIdBase;
    protected EtagNotifIdBase lastEtagNotifIdBase;
    protected String baseEtag;
    protected String configEtag;
    protected int notifId;
    protected HttpHeaders headers = new HttpHeaders();
    @Autowired
    private BackupManagerRepository backupManagerRepository;
    @Autowired
    private MediatorNotificationHandler cmMediatorHandler;

    @Before
    public void setup() {
        lastEtagNotifIdBase = new EtagNotifIdBase(BASE_ETAG_VALUE, 10, null);;
        etagNotifIdBase = new EtagNotifIdBase(BASE_ETAG_VALUE, 20, null);
        headers = new HttpHeaders();
        headers.add("ETag", CONFIG_ETAG_VALUE);
        cmMediatorHandler.setEtagNotifIdBase(lastEtagNotifIdBase);
        backupManagerRepository.createBackupManager(HOUSEKEEPING_BACKUP_MANAGER.toString());
        backupManagerRepository.getBackupManager(HOUSEKEEPING_BACKUP_MANAGER.toString()).getHousekeeping().setAutoDelete(AUTO_DELETE_ENABLED);
        backupManagerRepository.getBackupManager(HOUSEKEEPING_BACKUP_MANAGER.toString()).getHousekeeping().setMaxNumberBackups(1);
        agent = createTestAgent(AGENT_HOUSEKEEPING, HOUSEKEEPING_BACKUP_MANAGER.toString(), API_V2_0);
    }

    protected ResponseEntity<CreateActionResponse> getHouseKeepingResponse(final String autodelete, final int maxNumberBackup) {
        final UpdateHousekeepingRequest request;
        request = new UpdateHousekeepingRequest();
        request.setAction(ActionType.HOUSEKEEPING);
        request.setPayload(new EmptyPayload("data"));
        request.setAutoDelete(autodelete);
        request.setMaximumManualBackupsNumberStored(maxNumberBackup);
        final ResponseEntity<CreateActionResponse> response = restTemplate.postForEntity(HOUSEKEEPING_BACKUP_MANAGER_URL_V3 + "/housekeeping/", request, CreateActionResponse.class);
        return response;
    }

    protected String doBackup(final CreateActionRequest request) {
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(HOUSEKEEPING_BACKUP_MANAGER_ACTION.toString(), request, CreateActionResponse.class).getActionId();
        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(HOUSEKEEPING_BACKUP_MANAGER.toString(), backupId);
        final SystemTestBackupFragment fragment = new SystemTestBackupFragment(AGENT_HOUSEKEEPING, backupId, "2",
                "backupFile", Arrays.asList("3"), HOUSEKEEPING_BACKUP_MANAGER.toString());

        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();
        agent.sendStageCompleteMessage(BACKUP, true);
        waitForActionToFinish(HOUSEKEEPING_BACKUP_MANAGER.toString(), actionId);
        agent.clearBackupName(); // Mark the agent as no longer participating in an action
        return actionId;
    }

    protected int getNumberBackups(final String backupManagerId) {
        return backupManagerRepository.getBackupManager(backupManagerId).getBackups(Ownership.READABLE).size();
    }

    protected void wait5SecForActionToFinish(final String id) {
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> didActionFinish(HOUSEKEEPING_BACKUP_MANAGER.toString(), id));
    }

    protected HousekeepingInformation getHousekeeping(final int maxNumberBackups, final String autoDeleteEnable ) {
        return new HousekeepingInformation (autoDeleteEnable,maxNumberBackups);
    }

    protected void shutdownAgent() {
        agent.shutdown();
    }

    protected JSONArray getHousekeepingPatch(final String backupManager, final String element, final Object value) throws JSONException {
        final JSONArray patches = new JSONArray();
        final int position=getBackupManagerPosition(backupManager);
        patches.put(getPatchContent (position, element, value, REPLACE_OPERATION, HOUSEKEEPING));
        return patches;
    }

    protected JSONArray getHousekeepingPatches(final String backupManager, final String autodelete, final int maxstoredValue) throws JSONException {
        final JSONArray patches = new JSONArray();
        final int position=getBackupManagerPosition(backupManager);
        patches.put(getPatchContent (position, AUTO_DELETE, autodelete, REPLACE_OPERATION, HOUSEKEEPING));
        patches.put(getPatchContent (position, MAX_STORED_BACKUP, maxstoredValue, REPLACE_OPERATION, HOUSEKEEPING));
        return patches;
    }

    protected void setEtagFrombase() {
        baseEtag = etagNotifIdBase.getEtag();
        nextEtagNotifIdBase(etagNotifIdBase);
        configEtag = etagNotifIdBase.getEtag();
        notifId = etagNotifIdBase.getNotifId();
    }

    protected String getJsonHousekeepingString(final String backupManager, final String autodelete, final int maxstoredValue) throws JSONException {
        setEtagFrombase();
        return getJsonNotification ()
                .put("changedBy", "cmyp")
                .put("patch", getHousekeepingPatches(backupManager, autodelete, maxstoredValue))
                .put("baseETag", baseEtag)
                .put("configETag", configEtag)
                .put("notifId", notifId)
                .toString();
    }

    protected String getJsonsingleHousekeepingElement(final String backupManager, final String element, final Object value) throws JSONException {
        setEtagFrombase();
        return getJsonNotification ()
                .put("changedBy", "cmyp")
                .put("patch", getHousekeepingPatch(backupManager, element, value))
                .put("baseETag", baseEtag)
                .put("configETag", configEtag)
                .put("notifId", notifId)
                .toString();
    }

    protected void nextEtagNotifIdBase (final EtagNotifIdBase etagNotifId) {
        etagNotifId.updateEtag(String.valueOf(Integer.parseInt(etagNotifId.getEtag()) + 1 ));
        etagNotifId.setNotifId(etagNotifId.getNotifId() + 1);
    }
}
