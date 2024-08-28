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
package com.ericsson.adp.mgmt.backupandrestore.action;

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType.RUNNING;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.CREATE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.DELETE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.EXPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.HOUSEKEEPING;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.IMPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.RESTORE;
import static com.ericsson.adp.mgmt.backupandrestore.action.ResultType.NOT_AVAILABLE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.EmptyPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.PasswordSafeExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.PasswordSafeImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.Payload;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidActionException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidBackupNameException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidIdException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidNumberOfBackupsAllowed;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnprocessableEntityException;
import com.ericsson.adp.mgmt.backupandrestore.job.Job;
import com.ericsson.adp.mgmt.backupandrestore.rest.SftpServerTestConstant;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.ESAPIValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;

public class ActionFactoryTest {

    private static final URI TEST_GOOD = URI.create("sftp://bork@bork:22/iamdoggo");
    private static final URI TEST_BAD = URI.create("qwe");
    private static final URI SFTP_USER_LOCALHOST_22_ERICSSON = URI.create("sftp://user@localhost:22/ericsson");
    private static final URI SFTP_USER_LOCALHOST_22_REMOTE = URI.create("sftp://user@localhost:22/remote");
    private static final String BACKUP_TARBALL_NAME = "myBackup-2022-07-28T15:11:19.123456Z.tar.gz";

    private ActionFactory factory;
    private BackupManager backupManager;

    @Before
    public void setup() {
        backupManager = mockBackupManager("123");

        factory = new ActionFactory();
        factory.setActionRepository(EasyMock.createNiceMock(ActionRepository.class));
        factory.setEsapiValidator(new ESAPIValidator());
        factory.setIdValidator(new IdValidator());
    }

    @Test(expected = InvalidActionException.class)
    public void createAction_nullAction_throwsException() throws Exception {
        factory.createAction(mockBackupManager("123"), getActionRequest());
    }

    @Test
    public void createAction_validRequestForCreateBackup_createActionForCreateBackup() throws Exception {
        final CreateActionRequest request = getActionRequest(CREATE_BACKUP, "1");

        final Action action = factory.createAction(backupManager, request);

        assertTrue(Integer.valueOf(action.getActionId()) >= 0);
        assertTrue(Integer.valueOf(action.getActionId()) <= 65535);
        assertEquals(CREATE_BACKUP, action.getName());
        assertEquals(request.getPayload(), action.getPayload());
        assertEquals(NOT_AVAILABLE, action.getResult());
        assertEquals(RUNNING, action.getState());
    }

    @Test
    public void createAction_validRequestForDeleteBackup_createActionForDeleteBackup() throws Exception {
        final CreateActionRequest request = getActionRequest(DELETE_BACKUP, "2");

        final Action action = factory.createAction(backupManager, request);

        assertNotNull(action.getActionId());
        assertEquals(DELETE_BACKUP, action.getName());
        assertEquals(request.getPayload(), action.getPayload());
        assertEquals(NOT_AVAILABLE, action.getResult());
        assertEquals(RUNNING, action.getState());
    }

    @Test
    public void createAction_validRequestForCreateBackup_actionThatCanBePersisted() throws Exception {
        final ActionRepository repository = createMock(ActionRepository.class);
        factory.setActionRepository(repository);

        final Action action = factory.createAction(backupManager, getActionRequest(CREATE_BACKUP, "3"));

        repository.persist(action);
        EasyMock.expectLastCall();
        EasyMock.replay(repository);

        action.persist();

        EasyMock.verify(repository);
    }

    @Test(expected = InvalidIdException.class)
    public void createAction_requestWithBackupNameThatFailsCommonValidation_throwException() throws Exception {
        final IdValidator validator = createMock(IdValidator.class);
        validator.validateId("failedId");
        expectLastCall().andThrow(new InvalidIdException(""));
        replay(validator);
        factory.setIdValidator(validator);

        factory.createAction(backupManager, getActionRequest(CREATE_BACKUP, "failedId"));
    }

    @Test(expected = InvalidBackupNameException.class)
    public void createAction_requestWithBackupNameThatFailsESAPIValidation_throwException() throws Exception {
        final ESAPIValidator validator = createMock(ESAPIValidator.class);
        validator.validateBackupName("otherFailedId");
        expectLastCall().andThrow(new InvalidBackupNameException(""));
        replay(validator);
        factory.setEsapiValidator(validator);

        factory.createAction(backupManager, getActionRequest(CREATE_BACKUP, "otherFailedId"));
    }

    @Test(expected = InvalidActionException.class)
    public void createAction_payloadWithUriForCreateBackup_throwException() throws Exception {
        final CreateActionRequest request = getImportActionRequest(TEST_GOOD, "password");
        request.setAction(ActionType.CREATE_BACKUP);

        factory.createAction(backupManager, request);
    }

    @Test
    public void createAction_validRequestForCreateRestore_createActionForCreateRestore() throws Exception {
        final CreateActionRequest request = getActionRequest(RESTORE, "4");

        final Action action = factory.createAction(backupManager, request);

        assertNotNull(action.getActionId());
        assertEquals(RESTORE, action.getName());
        assertEquals(request.getPayload(), action.getPayload());
        assertEquals(NOT_AVAILABLE, action.getResult());
        assertEquals(RUNNING, action.getState());
    }

    @Test(expected = InvalidActionException.class)
    public void createAction_payloadWithUriForRestore_throwException() throws Exception {
        final CreateActionRequest request = getImportActionRequest(TEST_BAD, "password");
        request.setAction(ActionType.RESTORE);

        factory.createAction(backupManager, request);
    }

    @Test(expected = InvalidActionException.class)
    public void createAction_inexistentBackupForRestore_throwException() {
        final String inexistentBackup = "inexistentBackup";

        backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn("123").anyTimes();
        expect(backupManager.getBackup(anyString(), anyObject(Ownership.class))).andThrow(new BackupNotFoundException(inexistentBackup));
        replay(backupManager);

        factory.createAction(backupManager, getActionRequest(RESTORE, inexistentBackup));
    }

    @Test(expected = InvalidNumberOfBackupsAllowed.class)
    public void createAction_invalidNumberOfMaximumBackups_throwException() {
        final CreateActionRequest request = getCreateActionRequest();
        request.setAction(HOUSEKEEPING);
        request.setMaximumManualBackupsNumberStored(655536);
        request.setAutoDelete(AUTO_DELETE_ENABLED);
        factory.createAction(backupManager, request);
    }

    @Test(expected = UnprocessableEntityException.class)
    public void createAction_noNumberOfMaximumBackups_noAutoDelete_throwException() {
        final CreateActionRequest request = getCreateActionRequest();
        request.setAction(HOUSEKEEPING);
        request.setMaximumManualBackupsNumberStored(null);
        request.setAutoDelete(null);
        factory.createAction(backupManager, request);
    }

    @Test(expected = InvalidActionException.class)
    public void createAction_payloadWithUriForDeleteBackup_throwException() throws Exception {
        final CreateActionRequest request = getImportActionRequest(TEST_BAD, "password");
        request.setAction(ActionType.DELETE_BACKUP);

        factory.createAction(backupManager, request);
    }

    @Test(expected = InvalidActionException.class)
    public void createAction_inexistentBackupForDelete_throwException() {
        final String inexistentBackup = "inexistentBackup";

        backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn("123").anyTimes();
        expect(backupManager.getBackup(anyString(), anyObject(Ownership.class))).andThrow(new BackupNotFoundException(inexistentBackup));
        replay(backupManager);

        factory.createAction(backupManager, getActionRequest(DELETE_BACKUP, inexistentBackup));
    }

    @Test
    public void createAction_validURIImportPayload_createAction() {
        final CreateActionRequest request = getImportActionRequest(SFTP_USER_LOCALHOST_22_REMOTE, "password");

        final Action action = factory.createAction(backupManager, request);

        assertNotNull(action.getActionId());
        assertEquals(ActionType.IMPORT, action.getName());
        assertEquals(SFTP_USER_LOCALHOST_22_REMOTE, ((PasswordSafeImportPayload) action.getPayload()).getUri());
        assertEquals("password", ((PasswordSafeImportPayload) action.getPayload()).getPassword());
        assertNull(((PasswordSafeImportPayload) action.getPayload()).getSftpServerName());
        assertNull(((PasswordSafeImportPayload) action.getPayload()).getBackupPath());
        assertEquals(NOT_AVAILABLE, action.getResult());
        assertEquals(RUNNING, action.getState());
    }

    @Test
    public void createAction_validSftpServerNameImportPayload_createAction() {
        final String sftpServerName = SftpServerTestConstant.SFTP_SERVER_NAME;
        final List<String> validBackupNames = List.of(BACKUP_TARBALL_NAME, "myBackup");

        validBackupNames.forEach(backupName -> {
            final CreateActionRequest request = getImportActionRequest(backupName, sftpServerName);
            final Action action = factory.createAction(backupManager, request);
            assertNotNull(action.getActionId());
            assertEquals(ActionType.IMPORT, action.getName());
            assertEquals(sftpServerName, ((ImportPayload) action.getPayload()).getSftpServerName());
            assertEquals(backupName, ((ImportPayload) action.getPayload()).getBackupPath());
            assertNull(((PasswordSafeImportPayload) action.getPayload()).getUri());
            assertNull(((PasswordSafeImportPayload) action.getPayload()).getPassword());
            assertEquals(NOT_AVAILABLE, action.getResult());
            assertEquals(RUNNING, action.getState());
        });
    }

    @Test(expected = InvalidActionException.class)
    public void createAction_invalidSftpServerNameImportPayload_inexistentSftpServerName() {
        final CreateActionRequest request = getImportActionRequest(BACKUP_TARBALL_NAME, "inexistentSftpServerName");
        factory.createAction(backupManager, request);
    }

    @Test(expected = InvalidActionException.class)
    public void createAction_invalidSftpServerNameImportPayload_emptySftpServerName() {
        final CreateActionRequest request = getImportActionRequest(BACKUP_TARBALL_NAME, "");
        factory.createAction(backupManager, request);
    }

    @Test(expected = InvalidActionException.class)
    public void createAction_invalidSftpServerNameImportPayload_nullSftpServerName() {
        final CreateActionRequest request = getImportActionRequest(BACKUP_TARBALL_NAME, null);
        factory.createAction(backupManager, request);
    }

    @Test(expected = InvalidBackupNameException.class)
    public void createAction_invalidSftpServerNameImportPayload_invalidBackupName() {
        final CreateActionRequest request = getImportActionRequest("myB@ckup-2022-07-28T15:11:19.123456Z.tar.gz", SftpServerTestConstant.SFTP_SERVER_NAME);
        factory.createAction(backupManager, request);
    }

    @Test
    public void createAction_validEmptyPayload_UpdateAction() {
        final CreateActionRequest request = getCreateActionRequest();
        request.setAutoDelete(AUTO_DELETE_ENABLED);
        request.setMaximumManualBackupsNumberStored(1);

        backupManager = createMock(BackupManager.class);
        expect(backupManager.getActions()).andReturn(new ArrayList<>()).anyTimes();
        expect(backupManager.getBackupManagerId()).andReturn("123").anyTimes();
        replay(backupManager);


        final Action action = factory.createAction(backupManager, request);

        assertNotNull(action.getActionId());
        assertEquals(HOUSEKEEPING, action.getName());
        assertEquals(NOT_AVAILABLE, action.getResult());
        assertEquals(RUNNING, action.getState());
        assertEquals("", ((EmptyPayload)action.getPayload()).getData());
        assertEquals("Payload []", ((EmptyPayload)action.getPayload()).toString());
    }



    @Test(expected = InvalidBackupNameException.class)
    public void createAction_invalidLegacyBackupNameForImportBackup_throwsInvalidBackupNameException() throws Exception{

        final String backupNameTooBig = String.valueOf(new char[201]).replace('\0','a');
        final ESAPIValidator validator = createMock(ESAPIValidator.class);
        validator.validateURI(EasyMock.anyObject());
        validator.validateBackupName(backupNameTooBig);
        expectLastCall().andThrow(new InvalidBackupNameException(""));
        replay(validator);
        factory.setEsapiValidator(validator);

        factory.createAction(backupManager, getImportActionRequest(URI.create(TEST_GOOD+"/"+backupNameTooBig),
                "password"));

    }

    @Test(expected = InvalidBackupNameException.class)
    public void createAction_invalidBackupNameForImportBackup_throwsInvalidBackupNameException() throws Exception{
        final String backupNameTooBig = String.valueOf(new char[201]).replace('\0','a') +
                "-2021-03-10T14:59:56.029093Z.tar.gz";
        final ESAPIValidator validator = createMock(ESAPIValidator.class);
        validator.validateURI(EasyMock.anyObject());
        validator.validateBackupName(EasyMock.anyString());
        expectLastCall().andThrow(new InvalidBackupNameException(""));
        replay(validator);
        factory.setEsapiValidator(validator);
        factory.createAction(backupManager, getImportActionRequest(URI.create(TEST_GOOD+"/"+backupNameTooBig),
                "password"));

    }

    @Test(expected = InvalidActionException.class)
    public void createAction_invalidRequestForImportBackup_throwsInvalidActionException() throws Exception {
        factory.createAction(backupManager, getImportActionRequest(URI.create(""), ""));
    }

    @Test(expected = InvalidActionException.class)
    public void createAction_nullPasswordInImportBackupRequest_throwsInvalidActionException() throws Exception {
        factory.createAction(backupManager, getImportActionRequest(URI.create("sftp://user@localhost:22/qwe"), ""));
    }

    @Test
    public void createAction_createActionURIExportPayload_valid() throws Exception {
        final CreateActionRequest request = getExportActionRequest("myBackup", SFTP_USER_LOCALHOST_22_ERICSSON, "myPassword");
        final Action action = factory.createAction(backupManager, request);

        assertNotNull(action.getActionId());
        assertEquals(EXPORT, action.getName());
        assertEquals("myBackup", ((PasswordSafeExportPayload) action.getPayload()).getBackupName());
        assertEquals(SFTP_USER_LOCALHOST_22_ERICSSON, ((PasswordSafeExportPayload) action.getPayload()).getUri());
        assertEquals("myPassword", ((PasswordSafeExportPayload) action.getPayload()).getPassword());
        assertEquals(NOT_AVAILABLE, action.getResult());
        assertEquals(RUNNING, action.getState());
    }

    @Test
    public void createAction_createActionSftpServerNameExportPayload_valid() throws Exception {
        final String sftpServerName = SftpServerTestConstant.SFTP_SERVER_NAME;
        final CreateActionRequest request = getExportActionRequest("myBackup", sftpServerName);
        final Action action = factory.createAction(backupManager, request);

        assertNotNull(action.getActionId());
        assertEquals(EXPORT, action.getName());
        assertEquals("myBackup", ((PasswordSafeExportPayload) action.getPayload()).getBackupName());
        assertEquals(sftpServerName, ((ExportPayload) action.getPayload()).getSftpServerName());
        assertNull(((PasswordSafeExportPayload) action.getPayload()).getUri());
        assertNull(((PasswordSafeExportPayload) action.getPayload()).getPassword());
        assertEquals(NOT_AVAILABLE, action.getResult());
        assertEquals(RUNNING, action.getState());
    }

    @Test(expected = InvalidActionException.class)
    public void createAction_createActionSftpServerNameExportPayload_inExistentSftpServerName() throws Exception {
        final CreateActionRequest request = getExportActionRequest("myBackup", "inexistentSftpServerName");
        factory.createAction(backupManager, request);
    }

    @Test(expected = InvalidActionException.class)
    public void createAction_inexistentBackupForExport_throwException() {
        final String inexistentBackup = "inexistentBackup";

        backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn("123").anyTimes();
        expect(backupManager.getBackup(inexistentBackup, Ownership.OWNED)).andThrow(new BackupNotFoundException(inexistentBackup));
        replay(backupManager);

        factory.createAction(backupManager, getExportActionRequest(inexistentBackup, SFTP_USER_LOCALHOST_22_ERICSSON, "myPassword"));
    }

    @Test
    public void createAction_anotherHousekeepingActionExists_housekeepingDeleteActionCreated() {
        final Job job = createMock(Job.class);
        final Action action = createMock(Action.class);
        expect(action.getName()).andReturn(ActionType.HOUSEKEEPING);
        replay(action);

        expect(job.getAction()).andReturn(action);
        expect(job.getActionId()).andReturn("123");
        final CreateActionRequest request = getActionRequest(ActionType.HOUSEKEEPING_DELETE_BACKUP, "secondBackup");
        final Action createdAction = factory.createAction(backupManager, request);

        assertTrue(Integer.valueOf(createdAction.getActionId()) >= 0);
        assertTrue(Integer.valueOf(createdAction.getActionId()) <= 65535);
        assertEquals(ActionType.HOUSEKEEPING_DELETE_BACKUP, createdAction.getName());
        assertEquals(request.getPayload(), createdAction.getPayload());
        assertEquals(NOT_AVAILABLE, createdAction.getResult());
        assertEquals(RUNNING, createdAction.getState());
    }

    private BackupManager mockBackupManager(final String backupManagerId) {
        final BackupManager backupManager = createMock(BackupManager.class);
        final Backup backup = createMock(Backup.class);
        expect(backupManager.getBackupManagerId()).andReturn(backupManagerId).anyTimes();
        expect(backupManager.getBackup(anyString(), anyObject(Ownership.class))).andReturn(backup).anyTimes();
        expect(backupManager.getActions()).andReturn(new ArrayList<>()).anyTimes();
        expect(backupManager.getSftpServers()).andReturn(List.of(mockSftpServer())).anyTimes();
        replay(backupManager);
        return backupManager;
    }

    private SftpServer mockSftpServer() {
        SftpServer sftpServer = createMock(SftpServer.class);
        expect(sftpServer.getName()).andReturn(SftpServerTestConstant.SFTP_SERVER_NAME).anyTimes();
        replay(sftpServer);
        return sftpServer;
    }

    private CreateActionRequest getActionRequest() {
        return new CreateActionRequest();
    }

    private CreateActionRequest getActionRequest(final ActionType actionType, final Payload payload) {
        final CreateActionRequest request = getActionRequest();
        request.setAction(actionType);
        request.setPayload(payload);
        return request;
    }

    private CreateActionRequest getActionRequest(final ActionType actionType, final String backupName) {
        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName(backupName);
        return getActionRequest(actionType, payload);
    }

    private CreateActionRequest getImportActionRequest(final URI uri, final String password) {
        final ImportPayload payload = new ImportPayload(uri, password);
        return getActionRequest(IMPORT, payload);
    }

    private CreateActionRequest getExportActionRequest(final String backupName, final URI uri, final String password) {
        final ExportPayload payload = new ExportPayload(backupName, uri, password);
        return getActionRequest(EXPORT, payload);
    }

    private CreateActionRequest getImportActionRequest(String backupName, final String sftpServerName) {
        final ImportPayload payload = new ImportPayload(backupName, sftpServerName);
        return getActionRequest(IMPORT, payload);
    }

    private CreateActionRequest getExportActionRequest(final String backupName, final String sftpServerName) {
        final ExportPayload payload = new ExportPayload(backupName, sftpServerName);
        return getActionRequest(EXPORT, payload);
    }

    private CreateActionRequest getCreateActionRequest() {
        return getActionRequest(HOUSEKEEPING, new EmptyPayload());
    }

}
