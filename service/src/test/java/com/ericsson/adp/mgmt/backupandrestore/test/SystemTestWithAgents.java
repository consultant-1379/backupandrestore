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
package com.ericsson.adp.mgmt.backupandrestore.test;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_DISABLED;
import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;

public abstract class SystemTestWithAgents extends SystemTest {

    private static final Logger log = LogManager.getLogger(SystemTestWithAgents.class);

    protected List<SystemTestAgent> agents;
    protected static final String REPLACE_OPERATION="replace";
    protected static final String ADD_OPERATION="add";
    protected static final String REMOVE_OPERATION="remove";

    @Autowired
    private BackupManagerRepository backupManagerRepository;

    @Before
    public void setupAgents() {
        agents = new ArrayList<>();
        backupManagerRepository.getBackupManager(BackupManager.DEFAULT_BACKUP_MANAGER_ID)
        .getHousekeeping().setAutoDelete(AUTO_DELETE_DISABLED);
        backupManagerRepository.getBackupManager(BackupManager.DEFAULT_BACKUP_MANAGER_ID)
        .getHousekeeping().setMaxNumberBackups(100);
    }

    @After
    public void teardownAgents() {
        try {
            agents.forEach(SystemTestAgent::shutdown);
            log.info("Waiting for agents to shutdown...");
            waitUntil(() -> getRegisteredAgents().isEmpty());
        } catch (final Exception e) {

        }
    }

    protected SystemTestAgent createTestAgent(final String agentId, final ApiVersion apiVersion) {
        final SystemTestAgent agent = new SystemTestAgent(agentId);
        registerAgent(agent, apiVersion);
        return agent;
    }

    protected SystemTestAgent createTestAgent(final String agentId, final String scope, final ApiVersion apiVersion) {
        final SystemTestAgent agent = new SystemTestAgent(agentId, scope);
        registerAgent(agent, apiVersion);
        return agent;
    }

    protected SystemTestAgent createSubAgent() {
        final SystemTestAgent agent = new SystemTestAgent();
        agents.add(agent);
        return agent;
    }

    protected void assertActionIsSuccessfullyCompleted(final String actionId) {
        assertActionIsSuccessfullyCompleted(BackupManager.DEFAULT_BACKUP_MANAGER_ID, actionId);
    }

    protected void assertActionIsSuccessfullyCompleted(final String backupManagerId, final String actionId) {
        final ActionResponse action = getAction(backupManagerId, actionId);
        assertEquals(ResultType.SUCCESS, action.getResult());
        assertEquals(ActionStateType.FINISHED, action.getState());
        assertEquals(Double.valueOf(1.0), action.getProgressPercentage());
    }

    protected void assertActionFailed(final String actionId) {
        assertActionFailed(BackupManager.DEFAULT_BACKUP_MANAGER_ID, actionId);
    }

    protected void assertActionFailed(final String backupManagerId, final String actionId) {
        final ActionResponse action = getAction(backupManagerId, actionId);
        assertEquals(ResultType.FAILURE, action.getResult());
        assertEquals(ActionStateType.FINISHED, action.getState());
    }

    private void registerAgent(final SystemTestAgent agent, final ApiVersion apiVersion) {
        agents.add(agent);

        agent.register(apiVersion);
        waitUntilAgentIsRegistered(agent.getAgentId());
    }

    protected JSONObject getJsonNotification() throws JSONException {
        return new JSONObject()
                .put("configName", "ericsson-brm")
                .put("event", "configUpdated")
                .put("configETag", "33")
                .put("baseETag", "22")
                .put("notifCreateTime", "")
                .put("notifId", "4")
                .put("changedBy", "test");
    }

    protected JSONArray getPatch(final String backupManager, final String element, final Object value, final String operation, final String yangOperation) throws JSONException {
        final JSONArray patches = new JSONArray();
        final int position=getBackupManagerPosition(backupManager);
        patches.put(getPatchContent (position, element, value, operation, yangOperation));
        return patches;
    }

    protected JSONObject getPatchContent(final int position, final String element, final Object value, final String operation, final String yangOperation) throws JSONException {
        return new JSONObject()
                .put ("value",value)
                .put ("op", operation)
                .put ("path","/ericsson-brm:brm/backup-manager/"+ position + yangOperation +"/"+element)
                .put ("backupManagerIndex",position)
                .put ("housekeepingElement",element);
    }

    protected JSONObject getAddEventPatchContent(final int position, final String id, final int hours, final String operation, final String yangOperation, final int eventPosition) throws JSONException {
        return new JSONObject()
                .put ("value","{id=" + id + ",days=0,hours=" + hours + ",weeks=0,minutes=0,start-time="+ OffsetDateTime.now().toString() + ",stop-time=" + OffsetDateTime.now().plusHours(1) + "}")
                .put ("op", operation)
                .put ("path","/ericsson-brm:brm/backup-manager/"+ position + yangOperation +"/"+ eventPosition)
                .put ("backupManagerIndex",position);
    }

    protected JSONObject getAddEventPatchContent_Array(final int position, final String id, final int hours, final String operation, final String yangOperation, final int eventPosition) throws JSONException {
        return new JSONObject()
                .put ("value","[{id=" + id + ",days=0,hours=" + hours + ",weeks=0,minutes=30}, {id=" + id + "-2,days=0,hours=" + hours + ",weeks=0,minutes=30}]")
                .put ("op", operation)
                .put ("path","/ericsson-brm:brm/backup-manager/"+ position + yangOperation)
                .put ("backupManagerIndex",position);
    }

    protected JSONObject getRemoveEventPatchContent(final int position, final String operation, final String yangOperation, final int eventPosition) throws JSONException {
        return new JSONObject()
                .put ("value","")
                .put ("op", operation)
                .put ("path","/ericsson-brm:brm/backup-manager/"+ position + yangOperation +"/"+ eventPosition)
                .put ("backupManagerIndex",position);
    }

    protected JSONObject getRemoveStopTimetPatchContent(final int position, final String operation, final String yangOperation, final int eventPosition) throws JSONException {
        return new JSONObject()
                .put ("op", operation)
                .put ("path","/ericsson-brm:brm/backup-manager/"+ position + yangOperation +"/"+ eventPosition +"/"+ "stop-time")
                .put ("backupManagerIndex",position);
    }

    protected JSONObject getUpdateEventPatchContent(final int position, final String element, final int value, final String operation, final String yangOperation, final int eventPosition) throws JSONException {
        return new JSONObject()
                .put ("value", value)
                .put ("op", operation)
                .put ("path","/ericsson-brm:brm/backup-manager/"+ position + yangOperation +"/"+ eventPosition +"/"+ element)
                .put ("backupManagerIndex",position)
                .put ("housekeepingElement",element);
    }

    protected int getBackupManagerPosition (final String backupManager){
        for (int intBackup=0; intBackup < backupManagerRepository.getBackupManagers().size(); intBackup++) {
            if (backupManagerRepository.getBackupManagers().get(intBackup).getBackupManagerId().equalsIgnoreCase(backupManager)) {
                return intBackup;
            }
        }
        return -1;
    }

    protected int getPeriodicEventPosition (final String backupManager, final String eventId){
        for (int intBackup=0; intBackup < getScheduler(backupManager).getPeriodicEvents().size(); intBackup++) {
            if (getScheduler(backupManager).getPeriodicEvents().get(intBackup).getEventId().equalsIgnoreCase(eventId)) {
                return intBackup;
            }
        }
        return -1;
    }

    protected Scheduler getScheduler(final String backupManager) {
        return backupManagerRepository.getBackupManager(backupManager).getScheduler();
    }
}
