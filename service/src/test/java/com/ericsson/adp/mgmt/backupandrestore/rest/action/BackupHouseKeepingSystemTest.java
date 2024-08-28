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
package com.ericsson.adp.mgmt.backupandrestore.rest.action;

import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.DELETE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V2_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_DISABLED;
import static org.junit.Assert.assertEquals;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestAgent;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestBackupFragment;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestWithAgents;

public class BackupHouseKeepingSystemTest extends SystemTestWithAgents {

    @Autowired
    private BackupManagerRepository backupManagerRepository;
    private SystemTestAgent agent;
    private CreateActionRequest backupRequest;
    private final static String BACKUP_MANAGER="HOUSEKEEPING_TEST_S";
    protected static final String HOUSEKEEPING_BACKUP_MANAGER_URL = V1_BASE_URL + "backup-manager/" + BACKUP_MANAGER + "/";
    protected static final String HOUSEKEEPING_BACKUP_MANAGER_ACTION = HOUSEKEEPING_BACKUP_MANAGER_URL + "action";

    @Before
    public void setup() {
        backupManagerRepository.createBackupManager(BACKUP_MANAGER);
        UpdateHousekeeping(2, AUTO_DELETE_DISABLED);
        agent = createTestAgent("backupLimitAgent", BACKUP_MANAGER, API_V2_0);
        backupRequest = getRandomBackupMessage();
        doBackup(backupRequest);
    }

    @After
    public void resetSettings() {
        UpdateHousekeeping(1, AUTO_DELETE_DISABLED);
    }

    @Test
    public void backup_orchestratorAtBackupLimit_failsAction() throws Exception {
        UpdateHousekeeping(1, AUTO_DELETE_DISABLED);
        final int maxNumberBackups=backupManagerRepository.getBackupManager(BACKUP_MANAGER).getHousekeeping().getMaxNumberBackups();
        final String actionId = restTemplate.postForObject(HOUSEKEEPING_BACKUP_MANAGER_ACTION, getRandomBackupMessage(), CreateActionResponse.class).getActionId();
        waitForActionToFinish(BACKUP_MANAGER, actionId);
        assertActionFailed(BACKUP_MANAGER, actionId);

        final ActionResponse action = restTemplate.getForObject(HOUSEKEEPING_BACKUP_MANAGER_ACTION + "/" + actionId, ActionResponse.class);
        backupRequest.setAction(DELETE_BACKUP);
        final String deleteActionId = restTemplate.postForObject(HOUSEKEEPING_BACKUP_MANAGER_ACTION, backupRequest, CreateActionResponse.class).getActionId();
        waitForActionToFinish(BACKUP_MANAGER, deleteActionId);

        assertEquals("Failed to create/import backup as maximum limit of <" + maxNumberBackups + "> already exceeded", action.getAdditionalInfo());
    }

    @Test
    public void backup_orchestratorAtBackupLimitHasABackupDeleted_isAbleToPerformANewBackup() throws Exception {
        UpdateHousekeeping(1, AUTO_DELETE_DISABLED);

        backupRequest.setAction(DELETE_BACKUP);
        final String deleteActionId = restTemplate.postForObject(HOUSEKEEPING_BACKUP_MANAGER_ACTION, backupRequest, CreateActionResponse.class).getActionId();
        waitForActionToFinish(BACKUP_MANAGER, deleteActionId);
        final String actionId = doBackup(getRandomBackupMessage());
        assertActionIsSuccessfullyCompleted(BACKUP_MANAGER, actionId);
    }

    private String doBackup(final CreateActionRequest request) {
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(HOUSEKEEPING_BACKUP_MANAGER_ACTION, request, CreateActionResponse.class).getActionId();
        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(BACKUP_MANAGER, backupId);
        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("backupLimitAgent", backupId, "2",
                "backupFile", Arrays.asList("3"), BACKUP_MANAGER);

        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();
        agent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(BACKUP_MANAGER, actionId);

        return actionId;
    }

    private void UpdateHousekeeping(final int maxNumberBackups, final String autoDelete) {
        backupManagerRepository.getBackupManager(BACKUP_MANAGER).getHousekeeping().setAutoDelete(autoDelete);
        backupManagerRepository.getBackupManager(BACKUP_MANAGER).getHousekeeping().setMaxNumberBackups(maxNumberBackups);
    }

}
