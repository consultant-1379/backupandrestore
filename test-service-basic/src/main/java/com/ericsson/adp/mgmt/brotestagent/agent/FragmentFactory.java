/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.brotestagent.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ericsson.adp.mgmt.bro.api.fragment.BackupFragmentInformation;
import com.ericsson.adp.mgmt.brotestagent.exception.FailedToCreateBackupException;
import com.ericsson.adp.mgmt.brotestagent.util.PropertiesHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements logic to create backup fragments
 */
public class FragmentFactory {
    private static final Logger log = LogManager.getLogger(FragmentFactory.class);
    private static final String BACKUP_DATA_PATH = "test.agent.fragment.backup.data.path";
    private static final String DEFAULT_BACKUP_DATA_PATH = "./src/test/resources/backup.txt";

    private static final String CUSTOM_METADATA_PATH = "test.agent.fragment.custom.backup.data.path";
    private static final String DEFAULT_CUSTOM_BACKUP_DATA_PATH = "./src/test/resources/CustomMetadata.txt";

    private final String agentId;

    /**
     * Creates factory that produces fragments for an agent.
     * @param agentId owner of fragments
     */
    public FragmentFactory(final String agentId) {
        this.agentId = agentId;
    }

    /**
     * Implements logic to create backup fragments
     * @return list of backup fragments
     */
    public List<BackupFragmentInformation> getFragmentList() {
        final List<BackupFragmentInformation> fragmentList = new ArrayList<>();
        final List<String> backupPath = getBackupFilePathList();
        if (backupPath.size() == 1 && backupPath.get(0).isBlank()) {
            log.warn("The given backup Path is empty");
            return List.of();
        }
        for (int i = 0; i < backupPath.size(); i++) {
            fragmentList.add(getFragment(i + 1, backupPath.get(i), getCustomMetadataFilePathList().get(i)));
        }

        return fragmentList;
    }

    private BackupFragmentInformation getFragment(final int fragmentNumber, final String backupPath, final String customMetadataPath) {
        final String fragmentId = agentId + "_" + fragmentNumber;
        final String sizeInBytes = getFileSizeInBytes(backupPath);
        final String setVersion = "0.0.0";

        final BackupFragmentInformation fragmentInformation = new BackupFragmentInformation();

        fragmentInformation.setFragmentId(fragmentId);
        fragmentInformation.setSizeInBytes(sizeInBytes);
        fragmentInformation.setVersion(setVersion);

        fragmentInformation.setBackupFilePath(backupPath);
        if (customMetadataPath.length() != 0) {
            fragmentInformation.setCustomMetadataFilePath(Optional.of(customMetadataPath));
        }
        return fragmentInformation;
    }

    private String getFileSizeInBytes(final String pathString) {
        try {
            final Path path = Paths.get(pathString);
            return Long.toString(Files.size(path));
        } catch (final IOException e) {
            throw new FailedToCreateBackupException("The file that was created for the backup has encountered a problem: " + pathString, e);
        }
    }

    private static List<String> getBackupFilePathList() {
        return PropertiesHelper.getPropertyValueList(BACKUP_DATA_PATH, DEFAULT_BACKUP_DATA_PATH);
    }

    private static List<String> getCustomMetadataFilePathList() {
        return PropertiesHelper.getPropertyValueList(CUSTOM_METADATA_PATH, DEFAULT_CUSTOM_BACKUP_DATA_PATH);
    }

}
