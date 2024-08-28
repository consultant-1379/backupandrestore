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

import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.BRO;
import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.CMM_NOTIF;
import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.KAFKA;
import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.PM;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V3_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import org.assertj.core.util.Arrays;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangBackupNameActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangBackupNameInput;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupsResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagerResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagersResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.health.HealthResponse;
import com.ericsson.adp.mgmt.backupandrestore.ssl.BroKeyStoreConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;
import com.ericsson.adp.mgmt.backupandrestore.ssl.TomcatConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.util.SetTimeouts;

public abstract class SystemTest extends IntegrationTest {

    private static final ApiUrl[] V3_AND_LATER_BASE_URLS = {V3_BASE_URL, V4_BASE_URL};
    public static final URI SFTP_USER_LOCALHOST_222_REMOTE = URI.create("sftp://user@localhost:222/remote");
    protected TomcatConfiguration tomcatConfiguration;
    protected KeyStoreService keyStoreService;

    @Autowired
    protected BroKeyStoreConfiguration keyStoreConfiguration;

    @Autowired
    protected BackupManagerRepository backupManagerRepository;

    protected final TestRestTemplate restTemplate = new TestRestTemplate();

    protected void waitForActionToFinish(final String backupManagerId, final String id) {
        Awaitility.await().atMost(SetTimeouts.TIMEOUT_SECONDS, TimeUnit.SECONDS).until(() -> didActionFinish(backupManagerId, id));
    }

    protected void waitForActionToFinish(final String id) {
        waitForActionToFinish(BackupManager.DEFAULT_BACKUP_MANAGER_ID, id);
    }

    protected boolean didActionFinish(final String backupManagerId, final String id) {
        return getAction(backupManagerId, id).getState().equals(ActionStateType.FINISHED);
    }

    protected ActionResponse getAction(final String backupManagerId, final String id) {
        return restTemplate.getForObject(V1_BASE_URL + "backup-manager/" +  backupManagerId + "/action/" + id, ActionResponse.class);
    }

    protected ActionResponse getAction(final String id) {
        return getAction(BackupManager.DEFAULT_BACKUP_MANAGER_ID, id);
    }

    protected String getRandomBackupName() {
        return UUID.randomUUID().toString();
    }

    protected CreateActionRequest getRandomBackupMessage() {
        return getRandomCreateActionRequest(ActionType.CREATE_BACKUP);
    }

    protected CreateActionRequest getRandomCreateActionRequest(final ActionType actionType) {
        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName(getRandomBackupName());
        final CreateActionRequest request = new CreateActionRequest();
        request.setAction(actionType);
        request.setPayload(payload);
        request.setMaximumManualBackupsNumberStored(1);
        request.setAutoDelete(AUTO_DELETE_ENABLED);
        return request;
    }

    protected void waitUntilAgentIsRegistered(final String id) {
        waitUntil(() -> getRegisteredAgents().contains(id));
    }

    protected void waitUntilAgentIsNotRegistered(final String id) {
        waitUntil(() -> !getRegisteredAgents().contains(id));
    }

    protected List<String> getRegisteredAgents() {
        return restTemplate.getForObject(V1_BASE_URL + "health", HealthResponse.class).getRegisteredAgents();
    }

    protected String getScopedActionUrl(final ApiUrl baseUrl , final String scope) {
        final String backupManager = Arrays.asList(V3_AND_LATER_BASE_URLS).contains(baseUrl) ? "backup-managers" : "backup-manager";
        final String action = baseUrl.equals(V3_BASE_URL) ? "actions" : "action";
        return baseUrl + backupManager + "/" + scope + "/" + action;
    }

    protected String getScopedBackupUrl(final ApiUrl baseUrl, final String scope) {
        final String backupManager = Arrays.asList(V3_AND_LATER_BASE_URLS).contains(baseUrl) ? "backup-managers" : "backup-manager";
        final String backup = Arrays.asList(V3_AND_LATER_BASE_URLS).contains(baseUrl) ? "backups" : "backup";
        return baseUrl + backupManager + "/" + scope + "/" + backup;
    }

    protected void waitUntil(final Callable<Boolean> condition) {
        Awaitility.await().atMost(SetTimeouts.TIMEOUT_SECONDS, TimeUnit.SECONDS).until(condition);
    }

    protected void waitUntilBackupExists(final String id) {
        waitUntilBackupExists(BackupManager.DEFAULT_BACKUP_MANAGER_ID, id);
    }

    protected void waitUntilBackupExists(final String backupManagerId, final String id) {
        waitUntil(() -> HttpStatus.OK.equals(restTemplate.getForEntity(V1_BASE_URL + "backup-manager/" + backupManagerId + "/backup/" + id, BackupResponse.class).getStatusCode()));
    }

    protected YangBackupNameActionRequest getYangNamePayload(final String backupManagerId, final String backupName) {
        final YangBackupNameActionRequest payload = new YangBackupNameActionRequest();
        payload.setInput(new YangBackupNameInput(backupName));
        payload.setContext("/ericsson-brm:brm/backup-manager/" + getBackupManagerIndex(backupManagerId));
        return payload;
    }

    protected YangActionRequest getYangPayload(final String backupManagerId, final String backupName) {
        final YangActionRequest payload = new YangActionRequest();
        payload.setContext("/ericsson-brm:brm/backup-manager/" + getBackupManagerIndex(backupManagerId) + "/backup/" + getBackupIndex(backupManagerId, backupName));
        return payload;
    }

    protected <T> T put(final String url, final Object body, final Class<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body), responseType).getBody();
    }

    private int getBackupManagerIndex(final String backupManagerId) {
        final BackupManagersResponse backupManagers = restTemplate.getForObject(V1_BASE_URL + "backup-manager", BackupManagersResponse.class);
        return backupManagers
                .getBackupManagers()
                .stream()
                .map(BackupManagerResponse::getBackupManagerId)
                .collect(Collectors.toList())
                .indexOf(backupManagerId);
    }

    private int getBackupIndex(final String backupManagerId, final String backupName) {
        final BackupsResponse backups = restTemplate.getForObject(V1_BASE_URL + "backup-manager/" + backupManagerId + "/backup", BackupsResponse.class);
        return backups
                .getBackups()
                .stream()
                .map(BackupResponse::getBackupId)
                .collect(Collectors.toList())
                .indexOf(backupName);
    }

    protected void setTomcatConfiguration_SSL() {
        tomcatConfiguration.setGlobalTlsEnabled(true);
        tomcatConfiguration.setEnableCMM(true);
        tomcatConfiguration.setRestTlsPort (7002);
        tomcatConfiguration.setRestActionsTlsEnforced("required");
        expect(keyStoreService.getKeyStoreConfig(BRO)).andReturn(keyStoreConfiguration);
        expect(keyStoreService.getKeyStoreConfig(CMM_NOTIF)).andReturn(keyStoreConfiguration).anyTimes();
        expect(keyStoreService.getKeyStoreConfig(PM)).andReturn(keyStoreConfiguration).anyTimes();
        expect(keyStoreService.getKeyStoreConfig(KAFKA)).andReturn(null).anyTimes();
        expect(keyStoreService.getKeyStoreType()).andReturn("type").anyTimes();
        keyStoreService.generateKeyStores();
        expectLastCall().once().times(1, 2);
    }
}
