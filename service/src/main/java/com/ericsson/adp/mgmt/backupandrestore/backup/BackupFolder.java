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
package com.ericsson.adp.mgmt.backupandrestore.backup;

import com.ericsson.adp.mgmt.backupandrestore.exception.AgentIdsNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.data.Metadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Hold details about the contents of a backup folder
 */
public class BackupFolder {

    private final Path backupLocation;

    /**
     * Creates BackupFolder.
     *
     * @param backupLocation
     *            path to the backup folder.
     */
    public BackupFolder(final Path backupLocation) {
        this.backupLocation = backupLocation;
    }

    /**
     * Gets a list of IDs of the agents that participated in this backup.
     *
     * @return list of Agent IDs
     */
    @Deprecated
    public List<String> getAgentIdsForBackup() {
        try (Stream<Path> backupFolderContents = Files.list(getBackupLocation())) {
            return backupFolderContents.filter(path -> path.toFile().isDirectory())
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new AgentIdsNotFoundException(getBackupLocation(), e);
        }
    }

    /**
     * Gets a FragmentFolder contained under this BackupFolder
     *
     * @param metadata
     *            Metadata for the fragment
     * @return FragmentFolder for the specified fragment
     */
    public FragmentFolder getFragmentFolder(final Metadata metadata) {
        final Path fragment = backupLocation
                .resolve(metadata.getAgentId())
                .resolve(metadata.getFragment().getFragmentId());
        return new FragmentFolder(fragment);
    }

    /**
     * Returns the path to the backup folder
     *
     * @return path to backup folder
     */
    public Path getBackupLocation() {
        return backupLocation;
    }

}
