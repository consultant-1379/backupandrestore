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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.exception.FilePersistenceException;
import com.ericsson.adp.mgmt.backupandrestore.persist.PVCPersistProvider;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ActionFileServiceTest {

    private static final String BACKUP_NAME = "myBackup";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private ActionFileService fileService;
    private Path fileLocation;

    @Before
    public void setup() {
        fileLocation = Paths.get(folder.getRoot().getAbsolutePath());

        fileService = new ActionFileService();
        fileService.setJsonService(new JsonService());
        fileService.setBackupManagersLocation(fileLocation.toString());
    }

    @Test
    public void deleteFile() throws Exception {
        final String backupManagerId = "qwe";
        final Action action = getAction("fgh", backupManagerId);
        fileService.writeToFile(action);
        fileService.performCleanup(action);
        assertEquals(0, fileService.getActions(backupManagerId).size());
    }


    @Test
    public void writeToFile_actionThatWasAlreadyWritten_writesAgainOverridingOldInformation() throws Exception {
        final String backupManagerId = "qpwoei";
        final Action action = getAction("bnm", backupManagerId);

        fileService.writeToFile(action);

        action.setAdditionalInfo("qqqqqqqqqqqqqqqqqqqqqqqqqq");

        fileService.writeToFile(action);

        Path file = fileLocation.resolve(backupManagerId).resolve("actions").resolve(action.getActionId() + ".json");
        file=action.getVersion().fromBase(file);
        assertTrue(file.toFile().exists());

        final String fileContents = Files.readAllLines(file).stream().collect(Collectors.joining(""));

        final PersistedAction persistedAction = new ObjectMapper().readValue(fileContents, PersistedAction.class);
        assertEquals(action.getActionId(), persistedAction.getActionId());
        assertEquals(ActionType.CREATE_BACKUP, persistedAction.getName());
        assertTrue(persistedAction.getPayload() instanceof BackupNamePayload);
        assertEquals(BACKUP_NAME, ((BackupNamePayload) persistedAction.getPayload()).getBackupName());
        assertEquals("qqqqqqqqqqqqqqqqqqqqqqqqqq", persistedAction.getAdditionalInfo());
        assertEquals(DateTimeUtils.convertToString(action.getCompletionTime()), persistedAction.getCompletionTime());
        assertEquals(DateTimeUtils.convertToString(action.getLastUpdateTime()), persistedAction.getLastUpdateTime());
        assertEquals(DateTimeUtils.convertToString(action.getStartTime()), persistedAction.getStartTime());
        assertEquals("b", persistedAction.getProgressInfo());
        assertEquals(Double.valueOf(1.0), Double.valueOf(persistedAction.getProgressPercentage()));
        assertEquals(ResultType.SUCCESS, persistedAction.getResult());
        assertEquals("c", persistedAction.getResultInfo());
        assertEquals(ActionStateType.FINISHED, persistedAction.getState());
    }

    @Test
    public void writeToFile_actionn_writeFileException() throws Exception {
        PersistProviderFactory persistProviderFactory = createMock(PersistProviderFactory.class);
        PVCPersistProvider persistProvider = createMock(PVCPersistProvider.class);
        expectedException.expect(FilePersistenceException.class);
        final String backupManagerId = "qwe";
        final Action action = getAction("fgh", backupManagerId);
        expect(persistProviderFactory.getPersistProvider()).andReturn(persistProvider);
        persistProvider.write(anyObject(), anyObject(), anyObject());
        expectLastCall().andThrow(new FilePersistenceException(""));
        replay(persistProviderFactory, persistProvider);
        fileService.setProvider(persistProviderFactory);
        fileService.writeToFile(action);
    }

    @Test
    public void getActions_backupManagerIdAndPersistedFiles_actionsWithInformationReadFromFile() throws Exception {
        final String backupManagerId = "qwe";
        final Action action = getAction("fgh", backupManagerId);
        fileService.writeToFile(action);
        fileService.writeToFile(getAction("jgk", backupManagerId));
        final List<PersistedAction> actions = fileService.getActions(backupManagerId);

        assertEquals(2, actions.size());

        final PersistedAction obtainedAction = actions.stream().filter(persistedAction -> action.getActionId().equals(persistedAction.getActionId())).findFirst().get();

        assertEquals(action.getActionId(), obtainedAction.getActionId());
        assertEquals(action.getName(), obtainedAction.getName());
        assertTrue(obtainedAction.getPayload() instanceof BackupNamePayload);
        assertEquals(BACKUP_NAME, ((BackupNamePayload) obtainedAction.getPayload()).getBackupName());
        assertEquals(action.getAdditionalInfo(), obtainedAction.getAdditionalInfo());
        assertEquals(DateTimeUtils.convertToString(action.getCompletionTime()), obtainedAction.getCompletionTime());
        assertEquals(DateTimeUtils.convertToString(action.getLastUpdateTime()), obtainedAction.getLastUpdateTime());
        assertEquals(DateTimeUtils.convertToString(action.getStartTime()), obtainedAction.getStartTime());
        assertEquals(action.getProgressInfo(), obtainedAction.getProgressInfo());
        assertEquals(Double.valueOf(action.getProgressPercentage()), Double.valueOf(obtainedAction.getProgressPercentage()));
        assertEquals(action.getResult(), obtainedAction.getResult());
        assertEquals("c", obtainedAction.getResultInfo());
        assertEquals(action.getState(), obtainedAction.getState());
    }

    @Test
    public void getActions_backupManagerIdWithoutPersistedFiles_emptyList() throws Exception {
        final String backupManagerId = "qwe";

        Files.createDirectories(fileLocation.resolve("qwe").resolve("actions"));

        final List<PersistedAction> actions = fileService.getActions(backupManagerId);

        assertTrue(actions.isEmpty());
    }

    @Test
    public void getActions_backupManagerIdWithoutAnyFolder_emptyList() throws Exception {
        final String backupManagerId = "123";

        final List<PersistedAction> actions = fileService.getActions(backupManagerId);

        assertTrue(actions.isEmpty());
    }

    @Test
    public void getActions_validAndInvalidFiles_onlyReadsValidFiles() throws Exception {
        final String backupManagerId = "qwe";
        final Action action = getAction("fgh", backupManagerId);
        fileService.writeToFile(action);

        Files.write(fileLocation.resolve(backupManagerId).resolve("actions").resolve("y.json"), "".getBytes());

        final List<PersistedAction> actions = fileService.getActions(backupManagerId);

        assertEquals(1, actions.size());
        assertEquals("fgh", actions.get(0).getActionId());
        assertEquals(DateTimeUtils.convertToString(action.getStartTime()), actions.get(0).getStartTime());
    }

    private Action getAction(final String id, final ActionType actionType, final String backupManagerId) {
        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName(BACKUP_NAME);
        final ActionRequest actionRequest = new ActionRequest();
        actionRequest.setActionId(id);
        actionRequest.setAction(actionType);
        actionRequest.setPayload(payload);
        actionRequest.setBackupManagerId(backupManagerId);
        final Action action = new Action(actionRequest, null);
        action.setAdditionalInfo("a");
        action.setCompletionTime(OffsetDateTime.now().plus(5, ChronoUnit.MINUTES));
        action.setLastUpdateTime(OffsetDateTime.now().plus(4, ChronoUnit.MINUTES));
        action.setProgressInfo("b");
        action.setProgressPercentage(1.0);
        action.setResult(ResultType.SUCCESS);
        action.addMessage("c");
        action.setPayload(payload);
        action.setState(ActionStateType.FINISHED);
        return action;
    }

    private Action getAction(final String id, final String backupManagerId) {
        return getAction(id, ActionType.CREATE_BACKUP, backupManagerId);
    }

}
