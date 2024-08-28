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
package com.ericsson.adp.mgmt.brotestagent.agent.behavior;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ericsson.adp.mgmt.control.BackendType;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.bro.api.agent.BackupExecutionActions;
import com.ericsson.adp.mgmt.bro.api.exception.FailedToTransferBackupException;
import com.ericsson.adp.mgmt.bro.api.fragment.BackupFragmentInformation;
import com.ericsson.adp.mgmt.bro.api.fragment.FragmentInformation;
import com.ericsson.adp.mgmt.bro.api.registration.RegistrationInformation;
import com.ericsson.adp.mgmt.brotestagent.test.RestoreExecutionActionsStub;
import com.ericsson.adp.mgmt.brotestagent.util.PropertiesHelper;

public class TestAgentBehaviorTest {

    private enum BackupType {
        TYPE_DEFAULT("DEFAULT"), TYPE_SUBSCRIBER("subscriber"), TYPE_CONFIGURATION("configuration"), TYPE_INVALID("INVALID"), TYPE_EMPTY(
                ""), TYPE_UNDEFINED("UNDEFINED");
        final private String description;

        BackupType(final String description) {
            this.description = description;
        }

        private String getDescription() {
            return this.description;
        }
    }

    private static final String BACKUP_NAME = "myTestBackup";
    private static final String BACKUP_TYPE_LIST = "test.agent.backuptype.list";
    private TestAgentBehavior agentBehavior;

    @Before
    public void setUp() {
        PropertiesHelper.loadProperties("src/test/resources/application.properties");
        PropertiesHelper.loadProperties("src/test/resources/backuptype.properties");

        this.agentBehavior = new TestAgentBehavior();
    }

    @Test
    public void getRegistrationInformation_valuesSetAsProperties_getRegistrationInformationFilledFromProperties() throws Exception {
        final RegistrationInformation registrationInformation = agentBehavior.getRegistrationInformation();

        assertEquals("a", registrationInformation.getAgentId());
        assertEquals("b", registrationInformation.getApiVersion());
        assertEquals("c", registrationInformation.getScope());
        assertEquals("d", registrationInformation.getSoftwareVersion().getDescription());
        assertEquals("2019-09-13", registrationInformation.getSoftwareVersion().getProductionDate());
        assertEquals("f", registrationInformation.getSoftwareVersion().getProductName());
        assertEquals("g", registrationInformation.getSoftwareVersion().getProductNumber());
        assertEquals("h", registrationInformation.getSoftwareVersion().getType());
        assertEquals("i", registrationInformation.getSoftwareVersion().getRevision());
        assertEquals(BackendType.BRO, registrationInformation.getBackendType());
    }

    @Test
    public void executeBackup_backupExecutionActions_sendsFragmentsAndCompletesBackupSuccessfully() throws Exception {
        final String expectedMessage = "The test service has completed a backup for " + BACKUP_NAME
                + " and the data has been sent to the orchestrator";

        final BackupExecutionActionsTest backupExecutionActions = new BackupExecutionActionsTest(BackupType.TYPE_DEFAULT);

        agentBehavior.executeBackup(backupExecutionActions);

        assertTrue(backupExecutionActions.isSuccessful());
        assertEquals(expectedMessage, backupExecutionActions.getMessage());

        assertEquals(3, backupExecutionActions.getFragments().size());

        final BackupFragmentInformation firstFragment = backupExecutionActions.getFragments().get(0);
        assertEquals("a_1", firstFragment.getFragmentId());
        assertEquals("0.0.0", firstFragment.getVersion());
        assertEquals(getSizeOfFile("./src/test/resources/backup.txt"), firstFragment.getSizeInBytes());
        assertEquals("./src/test/resources/backup.txt", firstFragment.getBackupFilePath());
        assertEquals(Optional.of("./src/test/resources/CustomMetadata.txt"), firstFragment.getCustomMetadataFilePath());

        final BackupFragmentInformation secondFragment = backupExecutionActions.getFragments().get(1);
        assertEquals("a_2", secondFragment.getFragmentId());
        assertEquals("0.0.0", secondFragment.getVersion());
        assertEquals(getSizeOfFile("./src/test/resources/CustomMetadata.txt"), secondFragment.getSizeInBytes());
        assertEquals("./src/test/resources/CustomMetadata.txt", secondFragment.getBackupFilePath());
        assertFalse(secondFragment.getCustomMetadataFilePath().isPresent());

        final BackupFragmentInformation thirdFragment = backupExecutionActions.getFragments().get(2);
        assertEquals("a_3", thirdFragment.getFragmentId());
        assertEquals("0.0.0", thirdFragment.getVersion());
        assertEquals(getSizeOfFile("./src/test/resources/backup2.txt"), thirdFragment.getSizeInBytes());
        assertEquals("./src/test/resources/backup2.txt", thirdFragment.getBackupFilePath());
        assertEquals(Optional.of("./src/test/resources/CustomMetadataDownload.txt"), thirdFragment.getCustomMetadataFilePath());
    }

    @Test
    public void executeBackup_backupExecutionActions_sendsFragmentsAndCompletesBackupSuccessfullyForASpecificBackupType() throws Exception {
        final String expectedMessage = "The test service has completed a backup for " + BACKUP_NAME
                + " and the data has been sent to the orchestrator";

        final BackupExecutionActionsTest backupExecutionActions = new BackupExecutionActionsTest(BackupType.TYPE_SUBSCRIBER);

        agentBehavior.executeBackup(backupExecutionActions);

        assertTrue(backupExecutionActions.isSuccessful());
        assertEquals(expectedMessage, backupExecutionActions.getMessage());

        assertEquals(1, backupExecutionActions.getFragments().size());
        final BackupFragmentInformation fragmentForTypeOne = backupExecutionActions.getFragments().get(0);
        assertEquals("a_1", fragmentForTypeOne.getFragmentId());

        final BackupExecutionActionsTest actionsForTypeTwo = new BackupExecutionActionsTest(BackupType.TYPE_CONFIGURATION);

        agentBehavior.executeBackup(actionsForTypeTwo);

        assertTrue(actionsForTypeTwo.isSuccessful());
        assertEquals(expectedMessage, actionsForTypeTwo.getMessage());

        assertEquals(1, actionsForTypeTwo.getFragments().size());
        final BackupFragmentInformation fragmentForTypeTwo = actionsForTypeTwo.getFragments().get(0);
        assertEquals("a_2", fragmentForTypeTwo.getFragmentId());
    }

    @Test
    public void executeBackup_backupExecutionActions_sendsFragmentsAndCompletesBackupSuccessfullyForEmptyBackupType() throws Exception {
        final String expectedMessage = "The test service has completed a backup for " + BACKUP_NAME
                + " and the data has been sent to the orchestrator";

        final BackupExecutionActionsTest backupExecutionActions = new BackupExecutionActionsTest(BackupType.TYPE_EMPTY);

        agentBehavior.executeBackup(backupExecutionActions);

        assertTrue(backupExecutionActions.isSuccessful());
        assertEquals(expectedMessage, backupExecutionActions.getMessage());

        assertEquals(3, backupExecutionActions.getFragments().size());

        final BackupFragmentInformation firstFragment = backupExecutionActions.getFragments().get(0);
        assertEquals("a_1", firstFragment.getFragmentId());
        assertEquals("0.0.0", firstFragment.getVersion());
        assertEquals(getSizeOfFile("./src/test/resources/backup.txt"), firstFragment.getSizeInBytes());
        assertEquals("./src/test/resources/backup.txt", firstFragment.getBackupFilePath());
        assertEquals(Optional.of("./src/test/resources/CustomMetadata.txt"), firstFragment.getCustomMetadataFilePath());

        final BackupFragmentInformation secondFragment = backupExecutionActions.getFragments().get(1);
        assertEquals("a_2", secondFragment.getFragmentId());
        assertEquals("0.0.0", secondFragment.getVersion());
        assertEquals(getSizeOfFile("./src/test/resources/CustomMetadata.txt"), secondFragment.getSizeInBytes());
        assertEquals("./src/test/resources/CustomMetadata.txt", secondFragment.getBackupFilePath());
        assertFalse(secondFragment.getCustomMetadataFilePath().isPresent());

        final BackupFragmentInformation thirdFragment = backupExecutionActions.getFragments().get(2);
        assertEquals("a_3", thirdFragment.getFragmentId());
        assertEquals("0.0.0", thirdFragment.getVersion());
        assertEquals(getSizeOfFile("./src/test/resources/backup2.txt"), thirdFragment.getSizeInBytes());
        assertEquals("./src/test/resources/backup2.txt", thirdFragment.getBackupFilePath());
        assertEquals(Optional.of("./src/test/resources/CustomMetadataDownload.txt"), thirdFragment.getCustomMetadataFilePath());
    }

    @Test
    public void executeBackup_backupExecutionActions_sendsFragmentsAndCompletesBackupFailForInvalidBackupTypeProperties() throws Exception {
        final String expectedMessage = "The test service failed to complete a backup " + BACKUP_NAME + ","
                + " Cause: The backup type list in properties is not following the format [name, initial index, final index] The test service will not retry to send the backup";

        PropertiesHelper.setProperty(BACKUP_TYPE_LIST, "subscriber,0,1~configuration,1,2~invalid,5");
        final BackupExecutionActionsTest backupExecutionActions = new BackupExecutionActionsTest(BackupType.TYPE_INVALID);

        agentBehavior.executeBackup(backupExecutionActions);
        assertTrue(!backupExecutionActions.isSuccessful());
        assertEquals(expectedMessage, backupExecutionActions.getMessage());
        assertEquals(0, backupExecutionActions.getFragments().size());
        // Reload the correct backup properties file
        PropertiesHelper.loadProperties("src/test/resources/backuptype.properties");
    }

    @Test
    public void executeBackup_backupExecutionActions_sendsFragmentsAndCompletesBackupSuccessfullyForUndefinedbackupType() throws Exception {
        final String expectedMessage = "The test service has completed a backup for " + BACKUP_NAME
                + " and the data has been sent to the orchestrator";

        final BackupExecutionActionsTest backupExecutionActions = new BackupExecutionActionsTest(BackupType.TYPE_UNDEFINED);

        agentBehavior.executeBackup(backupExecutionActions);
        assertTrue(backupExecutionActions.isSuccessful());
        assertEquals(expectedMessage, backupExecutionActions.getMessage());

        assertEquals(3, backupExecutionActions.getFragments().size());

        final BackupFragmentInformation firstFragment = backupExecutionActions.getFragments().get(0);
        assertEquals("a_1", firstFragment.getFragmentId());
        assertEquals("0.0.0", firstFragment.getVersion());
        assertEquals(getSizeOfFile("./src/test/resources/backup.txt"), firstFragment.getSizeInBytes());
        assertEquals("./src/test/resources/backup.txt", firstFragment.getBackupFilePath());
        assertEquals(Optional.of("./src/test/resources/CustomMetadata.txt"), firstFragment.getCustomMetadataFilePath());

        final BackupFragmentInformation secondFragment = backupExecutionActions.getFragments().get(1);
        assertEquals("a_2", secondFragment.getFragmentId());
        assertEquals("0.0.0", secondFragment.getVersion());
        assertEquals(getSizeOfFile("./src/test/resources/CustomMetadata.txt"), secondFragment.getSizeInBytes());
        assertEquals("./src/test/resources/CustomMetadata.txt", secondFragment.getBackupFilePath());
        assertFalse(secondFragment.getCustomMetadataFilePath().isPresent());

        final BackupFragmentInformation thirdFragment = backupExecutionActions.getFragments().get(2);
        assertEquals("a_3", thirdFragment.getFragmentId());
        assertEquals("0.0.0", thirdFragment.getVersion());
        assertEquals(getSizeOfFile("./src/test/resources/backup2.txt"), thirdFragment.getSizeInBytes());
        assertEquals("./src/test/resources/backup2.txt", thirdFragment.getBackupFilePath());
        assertEquals(Optional.of("./src/test/resources/CustomMetadataDownload.txt"), thirdFragment.getCustomMetadataFilePath());
    }

    @Test
    public void executeBackup_backupExecutionActions_sendsFragmentsAndCompletesBackupFailForInvalidBackupTypeLimits() throws Exception {
        final String expectedMessage = "The test service failed to complete a backup " + BACKUP_NAME;

        final BackupExecutionActionsTest backupExecutionActions = new BackupExecutionActionsTest(BackupType.TYPE_INVALID);

        agentBehavior.executeBackup(backupExecutionActions);
        assertTrue(!backupExecutionActions.isSuccessful());
        assertEquals(0, backupExecutionActions.getFragments().size());
        assert (backupExecutionActions.getMessage().startsWith(expectedMessage));
    }

    @Test
    public void executeBackup_backupExecutionActions_sendsFragmentsAndCompletesBackupSuccessfullyForEmptyBackupTypeList() throws Exception {
        final String expectedMessage = "The test service has completed a backup for " + BACKUP_NAME
                + " and the data has been sent to the orchestrator";

        PropertiesHelper.setProperty(BACKUP_TYPE_LIST, "");

        final BackupExecutionActionsTest backupExecutionActions = new BackupExecutionActionsTest(BackupType.TYPE_DEFAULT);

        agentBehavior.executeBackup(backupExecutionActions);
        assertTrue(backupExecutionActions.isSuccessful());
        assertEquals(expectedMessage, backupExecutionActions.getMessage());
        assertEquals(3, backupExecutionActions.getFragments().size());
        // Reloads the properties file to avoid any other test conflict
        PropertiesHelper.loadProperties("src/test/resources/backuptype.properties");
    }

    @Test
    public void executeRestore_restoreExecutionActions_downloadsAvailableFragmentsAndSuccessfullyCompletesRestore() throws Exception {
        final String expectedMessage = "The test service has completed restore of backup: " + BACKUP_NAME;

        final RestoreExecutionActionsStub restoreExecutionActions = new RestoreExecutionActionsStub(BACKUP_NAME,
                BackupType.TYPE_DEFAULT.getDescription());

        agentBehavior.executeRestore(restoreExecutionActions);

        assertEquals("./src/test/resources/", restoreExecutionActions.getRestoreLocationString());

        assertTrue(restoreExecutionActions.isSuccessful());
        assertEquals(expectedMessage, restoreExecutionActions.getMessage());

        assertEquals(3, restoreExecutionActions.getDownloadedFragments().size());
        final FragmentInformation downloadedFragment = restoreExecutionActions.getDownloadedFragments().get(0);
        assertEquals("X_0", downloadedFragment.getFragmentId());
        assertEquals("sizeInBytes", downloadedFragment.getSizeInBytes());
    }

    @Test
    public void executeRestore_throwsException_getFailureMessage() throws Exception {
        final RestoreExecutionActionsStub restoreExecutionActions = new RestoreExecutionActionsStub(BACKUP_NAME, true,
                BackupType.TYPE_DEFAULT.getDescription());

        agentBehavior.executeRestore(restoreExecutionActions);

        assertFalse(restoreExecutionActions.isSuccessful());
        assertEquals("Restore failed due to <oops>", restoreExecutionActions.getMessage());
    }

    private String getSizeOfFile(final String path) throws IOException {
        return String.valueOf(Files.size(Paths.get(path)));
    }

    private class BackupExecutionActionsTest extends BackupExecutionActions {

        private final List<BackupFragmentInformation> fragments = new ArrayList<>();
        private boolean successful;
        private String message;
        private final BackupType backupType;

        public BackupExecutionActionsTest(final BackupType backupType) {
            super(null, null);
            this.backupType = backupType;
        }

        @Override
        public void sendBackup(final BackupFragmentInformation fragmentInformation) throws FailedToTransferBackupException {
            this.fragments.add(fragmentInformation);
        }

        @Override
        public void backupComplete(final boolean success, final String message) {
            this.successful = success;
            this.message = message;
        }

        @Override
        public String getBackupName() {
            return BACKUP_NAME;
        }

        @Override
        public String getBackupType() {
            return this.backupType.getDescription();
        }

        public List<BackupFragmentInformation> getFragments() {
            return this.fragments;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }

    }

}
