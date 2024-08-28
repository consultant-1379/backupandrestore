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
package com.ericsson.adp.mgmt.backupandrestore.action.yang;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easymock.EasyMock;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionService;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMSubscriptionRequestFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.EtagNotifIdBase;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMEricssonbrmJson;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangBackupNameActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangBackupNameInput;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangSftpServerActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangSftpServerNameInput;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangURIInput;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;

public class YangActionServiceTest {

    private static final String SFTP_SERVER_NAME = "testServer";
    private static final String BACKUP_TARBALL_NAME = "myBackup-2022-07-28T15:11:19.123456Z.tar.gz";

    private ActionServiceStub actionService;
    private YangActionService yangActionService;
    private Logger mockLogger;
    private EtagNotifIdBase etagNotifIdBase = new EtagNotifIdBase();

    private static final URI TEST_URI = URI.create("qwe");

    @Before
    public void setup() throws JSONException {
        BackupManagerRepository bmRepository = mockBackupManagerRepository();
        actionService = new ActionServiceStub();
        etagNotifIdBase.setConfiguration(getEricssonJsonBRMConfiguration().toString());
        etagNotifIdBase.updateEtag("111");
        etagNotifIdBase.setNotifId(1000);
        etagNotifIdBase.setBackupManagerRepository(bmRepository);

        yangActionService = new YangActionService();
        yangActionService.setActionService(actionService);
        yangActionService.setBackupManagerRepository(bmRepository);
        yangActionService.setEtagNotifIdBase(etagNotifIdBase);
    }

    @Test
    public void createBackup_yangActionRequest_createsCreateBackupAction() throws Exception {
        final Logger originalLogger = LogManager.getLogger(YangActionService.class);
        final YangBackupNameActionRequest request = new YangBackupNameActionRequest();
        mockLogger = EasyMock.createMock(Logger.class);
        expect(mockLogger.isDebugEnabled()).andReturn(true).anyTimes();
        mockLogger.debug(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject());
        expectLastCall().anyTimes();
        mockLogger.info(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject());
        expectLastCall().anyTimes();
        mockLogger.debug(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject());
        expectLastCall().anyTimes();
        yangActionService.setLogger(mockLogger);
        EasyMock.replay (mockLogger);
        request.setInput(new YangBackupNameInput("backup"));
        request.setContext("/ericsson-brm:brm/backup-manager/1");
        request.setConfigETag("11");

        final Action action = yangActionService.createBackup(request);

        assertEquals("123", action.getActionId());

        assertEquals("A", actionService.getBackupManagerId());
        assertEquals(ActionType.CREATE_BACKUP, actionService.getRequest().getAction());

        final BackupNamePayload payload = (BackupNamePayload) actionService.getRequest().getPayload();
        assertEquals("backup", payload.getBackupName());
        EasyMock.reset(mockLogger);
        yangActionService.setLogger(originalLogger);
    }

    @Test
    public void restore_yangActionRequest_createsRestoreAction() throws Exception {
        final YangActionRequest request = new YangActionRequest();
        request.setContext("/ericsson-brm:brm/backup-manager/2/backup/0");
        request.setConfigETag("11");

        final Action action = yangActionService.restore(request);

        assertEquals("123", action.getActionId());

        assertEquals("A", actionService.getBackupManagerId());
        assertEquals(ActionType.RESTORE, actionService.getRequest().getAction());

        final BackupNamePayload payload = (BackupNamePayload) actionService.getRequest().getPayload();
        assertEquals("backupAA", payload.getBackupName());
    }

    @Test(expected = UnprocessableYangRequestException.class)
    public void restore_backupNotFound_throwsException() throws Exception {
        final YangActionRequest request = new YangActionRequest();
        request.setContext("/ericsson-brm:brm/backup-manager/2/backup/10");

        yangActionService.restore(request);
    }

    @Test
    public void deleteBackup_yangActionRequest_createsDeleteBackupAction() throws Exception {
        final YangBackupNameActionRequest request = new YangBackupNameActionRequest();
        request.setInput(new YangBackupNameInput("backupToDelete"));
        request.setContext("/ericsson-brm:brm/backup-manager/3");
        request.setConfigETag("11");

        final Action action = yangActionService.deleteBackup(request);

        assertEquals("123", action.getActionId());

        assertEquals("A", actionService.getBackupManagerId());
        assertEquals(ActionType.DELETE_BACKUP, actionService.getRequest().getAction());

        final BackupNamePayload payload = (BackupNamePayload) actionService.getRequest().getPayload();
        assertEquals("backupToDelete", payload.getBackupName());
    }

    @Test
    public void deleteBackup_invalidContext_throwsException() throws Exception {
        final YangBackupNameActionRequest request = new YangBackupNameActionRequest();
        request.setInput(new YangBackupNameInput("backupToDelete"));
        request.setContext("nope");

        try {
            yangActionService.deleteBackup(request);
            fail();
        } catch (final UnprocessableYangRequestException e) {
            assertEquals("Invalid context", e.getMessage());
        } catch (final Exception e) {
            fail();
        }
    }

    @Test
    public void importBackup_yangURIActionRequest_createsImportBackupAction() throws Exception {
        final YangURIInput input = new YangURIInput();
        input.setUri(TEST_URI);
        input.setPassword("asd");

        final YangSftpServerActionRequest request = new YangSftpServerActionRequest();
        request.setInput(input);
        request.setContext("/ericsson-brm:brm/backup-manager/3");
        request.setConfigETag("11");

        final Action action = yangActionService.importBackup(request);

        assertEquals("123", action.getActionId());

        assertEquals("A", actionService.getBackupManagerId());
        assertEquals(ActionType.IMPORT, actionService.getRequest().getAction());

        final ImportPayload payload = (ImportPayload) actionService.getRequest().getPayload();
        assertEquals(TEST_URI, payload.getUri());
        assertEquals("asd", payload.getPassword());
    }

    @Test
    public void export_yangURIActionRequest_createsExportBackupAction() throws Exception {
        final YangURIInput input = new YangURIInput();
        input.setUri(TEST_URI);
        input.setPassword("asd");

        final YangSftpServerActionRequest request = new YangSftpServerActionRequest();
        request.setInput(input);
        request.setContext("/ericsson-brm:brm/backup-manager/2/backup/1");
        request.setConfigETag("11");

        final Action action = yangActionService.export(request);

        assertEquals("123", action.getActionId());

        // replaced from "C" to "A", because we are testing, the change from 2 to BM 0
        assertEquals("A", actionService.getBackupManagerId());
        assertEquals(ActionType.EXPORT, actionService.getRequest().getAction());

        final ExportPayload payload = (ExportPayload) actionService.getRequest().getPayload();
        assertEquals(TEST_URI, payload.getUri());
        assertEquals("asd", payload.getPassword());
        // change "backupCB" to "backupAA", because it translate all the calls to BM 0 and backup 0
        assertEquals("backupAA", payload.getBackupName());
    }

    @Test
    public void importBackup_yangSftpServerNameActionRequest_createsImportBackupAction() throws Exception {
        final YangSftpServerNameInput input = new YangSftpServerNameInput(SFTP_SERVER_NAME, BACKUP_TARBALL_NAME);

        final YangSftpServerActionRequest request = new YangSftpServerActionRequest(input);
        request.setContext("/ericsson-brm:brm/backup-manager/3");
        request.setConfigETag("11");

        final Action action = yangActionService.importBackup(request);

        assertEquals("123", action.getActionId());

        assertEquals("A", actionService.getBackupManagerId());
        assertEquals(ActionType.IMPORT, actionService.getRequest().getAction());

        final ImportPayload payload = (ImportPayload) actionService.getRequest().getPayload();
        assertEquals(SFTP_SERVER_NAME, payload.getSftpServerName());
        assertNull(payload.getUri());
        assertNull(payload.getPassword());
    }

    @Test
    public void export_yangSftpServerNameActionRequest_createsExportBackupAction() throws Exception {
        final YangSftpServerNameInput input = new YangSftpServerNameInput(SFTP_SERVER_NAME);

        final YangSftpServerActionRequest request = new YangSftpServerActionRequest();
        request.setInput(input);
        request.setContext("/ericsson-brm:brm/backup-manager/2/backup/1");
        request.setConfigETag("11");

        final Action action = yangActionService.export(request);

        assertEquals("123", action.getActionId());

        assertEquals("A", actionService.getBackupManagerId());
        assertEquals(ActionType.EXPORT, actionService.getRequest().getAction());

        final ExportPayload payload = (ExportPayload) actionService.getRequest().getPayload();
        assertEquals(SFTP_SERVER_NAME, payload.getSftpServerName());
        assertNull(payload.getUri());
        assertNull(payload.getPassword());
        assertEquals("backupAA", payload.getBackupName());
    }

    @Test(expected = UnprocessableYangRequestException.class)
    public void export_backupNotFound_throwsException() throws Exception {
        final YangURIInput input = new YangURIInput();
        input.setUri(URI.create("qwe"));
        input.setPassword("asd");

        final YangSftpServerActionRequest request = new YangSftpServerActionRequest();
        request.setInput(input);
        request.setContext("/ericsson-brm:brm/backup-manager/2/backup/8");

        yangActionService.export(request);
    }

    @Test
    public void export_invalidContext_throwsException() throws Exception {
        final YangSftpServerActionRequest request = new YangSftpServerActionRequest();
        request.setInput(new YangURIInput());
        request.setContext("/ericsson-brm:brm/backup-manager/2/backup");

        try {
            yangActionService.export(request);
            fail();
        } catch (final UnprocessableYangRequestException e) {
            assertEquals("Invalid context", e.getMessage());
        } catch (final Exception e) {
            fail();
        }
    }

    private BackupManagerRepository mockBackupManagerRepository() throws JSONException {
        final BackupManagerRepository backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);
        final BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupIndex(EasyMock.anyString())).andReturn(0).anyTimes();
        
        final CMMSubscriptionRequestFactory configurationRequestFactory = new CMMSubscriptionRequestFactory();
        final JsonService jsonService = new JsonService();
        configurationRequestFactory.setJsonService(jsonService);
        final Optional<BRMEricssonbrmJson> request =configurationRequestFactory.parseJsonStringToBRMConfiguration(getEricssonJsonBRMConfiguration().toString());
        EasyMock.expect(backupManagerRepository.getBackupManagers()).andReturn(mockBackupManagers()).anyTimes();
        backupManagerRepository.getLastEtagfromCMM();
        expectLastCall().anyTimes();
        expect(backupManagerRepository.getBRMConfiguration(EasyMock.anyString())).andReturn(request).anyTimes();
        expect(backupManagerRepository.getIndex(EasyMock.anyString())).andReturn(0).anyTimes(); // Always returns 0/DEFAULT
        expect (backupManagerRepository.getBackupManager(EasyMock.anyString())).andReturn(backupManager).anyTimes();
        EasyMock.replay(backupManagerRepository, backupManager);
        return backupManagerRepository;
    }

    private List<BackupManager> mockBackupManagers() {
        final List<BackupManager> backupManagers = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
            final String backupManagerId = String.valueOf((char) ('A' + i));
            EasyMock.expect(backupManager.getBackupManagerId()).andReturn(backupManagerId).anyTimes();
            EasyMock.expect(backupManager.getBackups(Ownership.READABLE)).andReturn(mockBackups(backupManagerId)).anyTimes();
            EasyMock.replay(backupManager);
            backupManagers.add(backupManager);
        }
        return backupManagers;
    }

    private List<Backup> mockBackups(final String backupManagerId) {
        final List<Backup> backups = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            final Backup backup = EasyMock.createMock(Backup.class);
            EasyMock.expect(backup.getBackupId()).andReturn("backup" + backupManagerId + (char) ('A' + i)).anyTimes();
            EasyMock.replay(backup);
            backups.add(backup);
        }
        return backups;
    }

    protected JSONObject getEricssonJsonBRMConfiguration() throws JSONException {
        return new JSONObject()
                .put("name", "ericsson-brm")
                .put("title", "ericsson-brm")
                .put("data", getJsonBRMConfiguration());
    }

    protected JSONObject getJsonBRMConfiguration() throws JSONException {
        JSONArray brmBackupManagerArray = new JSONArray();
        JSONObject jsonHousekeeping = new JSONObject();
        JSONObject brmJson = new JSONObject();
        JSONObject brmBackupManager = new JSONObject();
        JSONObject brmdata= new JSONObject();
        jsonHousekeeping.put("auto-delete", "enabled");
        jsonHousekeeping.put("max-stored-manual-backups", 1);
        brmBackupManagerArray.put(createBackupManager("DEFAULT",jsonHousekeeping));
        brmBackupManagerArray.put(createBackupManager("DEFAULT-bro",jsonHousekeeping));
        brmBackupManagerArray.put(createBackupManager("configuration-data",jsonHousekeeping));
        brmBackupManagerArray.put(createBackupManager("configuration-data-bro",jsonHousekeeping));
        brmJson.put("backup-manager",brmBackupManagerArray);
        brmdata.put("ericsson-brm:brm", brmJson);
        return brmdata;
   }

    private JSONObject createBackupManager(final String bmId, final JSONObject jsonHousekeeping) throws JSONException {
        JSONObject brmBackupManager = new JSONObject();
        JSONArray backups = new JSONArray();
        backups.put(createBackup("bckid1"));
        backups.put(createBackup("bckid2"));
        backups.put(createBackup("bckid3"));
        backups.put(createBackup("bckid4"));
        brmBackupManager.put("backup-domain","");
        brmBackupManager.put("backup-type","");
        brmBackupManager.put("backup",backups);
        brmBackupManager.put("progress-report",new JSONArray());
        brmBackupManager.put("housekeeping",jsonHousekeeping);
        brmBackupManager.put("id",bmId);
        return brmBackupManager;
    }

    private JSONObject createBackup(final String id) throws JSONException {
        JSONObject backup = new JSONObject();
        backup.put("id", id);
        backup.put("backup-name", id);
        return backup;
    }

    private class ActionServiceStub extends ActionService {

        private String backupManagerId;
        private CreateActionRequest request;

        @Override
        public Action handleActionRequest(final String backupManagerId, final CreateActionRequest request) {
            this.backupManagerId = backupManagerId;
            this.request = request;

            final Action action = EasyMock.createMock(Action.class);
            EasyMock.expect(action.getActionId()).andReturn("123");
            EasyMock.replay(action);
            return action;
        }

        public String getBackupManagerId() {
            return backupManagerId;
        }

        public CreateActionRequest getRequest() {
            return request;
        }
    }

}
