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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.ericsson.adp.mgmt.control.BackendType;
import com.ericsson.adp.mgmt.metadata.AgentFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.bro.api.agent.AgentBehavior;
import com.ericsson.adp.mgmt.bro.api.agent.BackupExecutionActions;
import com.ericsson.adp.mgmt.bro.api.agent.RestoreExecutionActions;
import com.ericsson.adp.mgmt.bro.api.fragment.BackupFragmentInformation;
import com.ericsson.adp.mgmt.bro.api.fragment.FragmentInformation;
import com.ericsson.adp.mgmt.bro.api.registration.RegistrationInformation;
import com.ericsson.adp.mgmt.brotestagent.agent.FragmentFactory;
import com.ericsson.adp.mgmt.brotestagent.agent.SoftwareVersionInformationUtils;
import com.ericsson.adp.mgmt.brotestagent.util.PropertiesHelper;

/**
 * Holds behavior specific to this test agent.
 */
public class TestAgentBehavior implements AgentBehavior {

    private static final Logger log = LogManager.getLogger(TestAgentBehavior.class);

    private static final String AGENT_ID_PROPERTY = "test.agent.id";
    private static final String DEFAULT_AGENT_ID = "TestAgent";

    private static final String SCOPE_PROPERTY = "test.agent.scope";
    private static final String DEFAULT_SCOPE = "";

    private static final String API_VERSION_PROPERTY = "test.agent.apiVersion";
    private static final String DEFAULT_API_VERSION = "1";

    private static final String DOWNLOAD_LOCATION = "test.agent.download.location";
    private static final String DEFAULT_DOWNLOAD_LOCATION = "./src/test/resources/";

    private static final String BACKUP_TYPE_LIST = "test.agent.backuptype.list";

    private static final String BACKEND_TYPE = PropertiesHelper.getProperty("test.agent.backendType", "test.agent.backendType");

    private static final String AGENT_FEATURES = PropertiesHelper.getProperty("test.agent.agentFeatures", "test.agent.agentFeatures");

    @Override
    public RegistrationInformation getRegistrationInformation() {
        final RegistrationInformation registrationInfo = new RegistrationInformation();
        registrationInfo.setAgentId(getAgentId());
        registrationInfo.setApiVersion(PropertiesHelper.getProperty(API_VERSION_PROPERTY, DEFAULT_API_VERSION));
        registrationInfo.setScope(PropertiesHelper.getProperty(SCOPE_PROPERTY, DEFAULT_SCOPE));
        registrationInfo.setSoftwareVersion(SoftwareVersionInformationUtils.getSoftwareVersion());

        registrationInfo.setBackendType(BackendType.valueOf(BACKEND_TYPE));
        final String[] features = AGENT_FEATURES.split(",");
        final List<AgentFeature> listOfAgentFeatures = new ArrayList<>();
        Arrays.stream(features).forEach(feature -> {
            listOfAgentFeatures.add(AgentFeature.valueOf(feature.strip()));
        });
        registrationInfo.setAgentFeatures(listOfAgentFeatures);

        return registrationInfo;
    }

    @Override
    public void executeBackup(final BackupExecutionActions backupExecutionActions) {
        try {
            for (final BackupFragmentInformation fragment : doSomethingToCreateBackup(backupExecutionActions.getBackupType())) {
                backupExecutionActions.sendBackup(fragment);
            }
            backupExecutionActions.backupComplete(true, getBackupSuccessfulMessage(backupExecutionActions));
            log.info("Finished backup {}, for backup type {}", backupExecutionActions.getBackupName(), backupExecutionActions.getBackupType());
        } catch (final Exception e) {
            log.error("Backup Failed due to exception: ", e);
            backupExecutionActions.backupComplete(false, getBackupFailedMessage(backupExecutionActions, e));
        }
    }

    @Override
    public void executeRestore(final RestoreExecutionActions restoreExecutionActions) {
        if (!SoftwareVersionInformationUtils.isCompatibleSoftwareVersion(restoreExecutionActions.getSoftwareVersion())) {
            restoreExecutionActions.sendStageComplete(false, "Incompatible software version");
            log.error("Restore of backup {} failed due to incompatible software version", restoreExecutionActions.getBackupName());
        } else {
            try {
                for (final FragmentInformation fragmentInformation : restoreExecutionActions.getFragmentList()) {
                    restoreExecutionActions.downloadFragment(fragmentInformation, getDownloadLocation());
                }
                performCustomRestoreLogic(restoreExecutionActions.getBackupType());
                restoreExecutionActions.sendStageComplete(true,
                        "The test service has completed restore of backup: " + restoreExecutionActions.getBackupName());
                log.info("Restore of backup {} finished, for backup type {}", restoreExecutionActions.getBackupName(),
                        restoreExecutionActions.getBackupType());
            } catch (final Exception e) {
                log.error("Restore of backup {} failed due to exception ", restoreExecutionActions.getBackupName(), e);
                restoreExecutionActions.sendStageComplete(false, "Restore failed due to <" + e.getMessage() + ">");
            }
        }
    }

    /**
     * Restore specific logic
     *
     * @param backupType
     *            the backup type of the restore.
     */
    protected void performCustomRestoreLogic(final String backupType) {
        // To be implemented by agent developers
        log.info("Performing a restore for backup type {}", backupType);
    }

    /**
     * Backup specific logic
     *
     * @param backupType
     *            the type of backup to be created
     * @return list of fragments
     */
    protected List<BackupFragmentInformation> doSomethingToCreateBackup(final String backupType) {
        // To be implemented by agent developers
        // Perform some logic to generate the backup files and custom metadata files to be backed up
        // backup type is used here to decide on a different set of data to backup.
        final FragmentFactory fragmentsToBackup = new FragmentFactory(getAgentId());

        if (!getBackupTypeList().isEmpty() && !backupType.isEmpty()) {
            final BackupTypeItem backupTypeItem = Stream.of(getBackupTypeList().split("~")).map(BackupTypeItem::new)
                    .filter(e -> e.getName().equalsIgnoreCase(backupType)).findFirst().orElse(null);
            if (backupTypeItem != null) {
                log.info("Getting data for a backup of {}", backupTypeItem);
                return fragmentsToBackup.getFragmentList().subList(backupTypeItem.getInitialIndex(), backupTypeItem.getFinalIndex());
            } else {
                log.info("Invalid backupType defined performing a default backup for {}", backupType);
                return fragmentsToBackup.getFragmentList();
            }
        } else {
            log.info("No specific backupType found performing a default backup for {}", backupType);
            return fragmentsToBackup.getFragmentList();
        }

    }

    /**
     * Get the string including the backup type list
     *
     * @return A string defining backup type list
     */
    private String getBackupTypeList() {
        return PropertiesHelper.getProperty(BACKUP_TYPE_LIST, "");
    }

    private String getAgentId() {
        return PropertiesHelper.getProperty(AGENT_ID_PROPERTY, DEFAULT_AGENT_ID);
    }

    private String getBackupSuccessfulMessage(final BackupExecutionActions backupExecutionActions) {
        return "The test service has completed a backup for " + backupExecutionActions.getBackupName()
                + " and the data has been sent to the orchestrator";
    }

    private String getBackupFailedMessage(final BackupExecutionActions backupExecutionActions, final Exception exception) {
        return "The test service failed to complete a backup " + backupExecutionActions.getBackupName() + ", Cause: " + exception.getMessage()
                + " The test service will not retry to send the backup";
    }

    private String getDownloadLocation() {
        return PropertiesHelper.getProperty(DOWNLOAD_LOCATION, DEFAULT_DOWNLOAD_LOCATION);
    }

    private class BackupTypeItem {

        private final String name;
        private final int initialIndex;
        private final int finalIndex;

        /**
         * Receive a string including name, initial and final position
         *
         * @param definition
         */
        BackupTypeItem(final String definition) {
            // The string received is following the format, comma separated
            // name, initial index, final index
            final String[] values = definition.split(",");
            if (values.length <= 2) {
                throw new IllegalArgumentException(
                        "The backup type list in properties is not following the format [name, initial index, final index]");
            }
            this.name = values[0];
            this.initialIndex = Integer.valueOf(values[1]);
            this.finalIndex = Integer.valueOf(values[2]);
        }

        private String getName() {
            return name;
        }

        private int getInitialIndex() {
            return initialIndex;
        }

        private int getFinalIndex() {
            return finalIndex;
        }

        @Override
        public String toString() {
            return "BackupTypeItem [name=" + name + ", initial_index=" + initialIndex + ", final_index=" + finalIndex + "]";
        }
    }
}
