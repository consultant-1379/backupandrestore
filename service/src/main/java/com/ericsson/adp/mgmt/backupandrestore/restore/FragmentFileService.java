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
package com.ericsson.adp.mgmt.backupandrestore.restore;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.persist.FileService;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistedFragmentInformation;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.bro.api.fragment.FragmentInformation;
import com.ericsson.adp.mgmt.metadata.Fragment;

/**
 * Responsible for reading & collecting fragment information from backup.json file.
 */
@Service
public class FragmentFileService extends FileService<PersistedFragmentInformation> {

    private static final int DEPTH_OF_FILES = 2;
    private static final Path DEFAULT_DATA_LOCATION = Paths.get(System.getProperty("java.io.tmpdir"), "backups")
            .toAbsolutePath().normalize();
    protected Path dataLocation = DEFAULT_DATA_LOCATION;

    private final List<Version<PersistedFragmentInformation>> versions = List.of(getDefaultVersion(
        s -> jsonService.parseJsonString(s, PersistedFragmentInformation.class),
        p -> p.getFileName().toString().endsWith(JSON_EXTENSION)
    ));


    /**
     * Returns a list of fragments of a backup for a agent.
     * @param backupManagerId - backupManagerId of the backup.
     * @param backupName - backupName of the backup file being restored.
     * @param agentId - agentId of the agent responsible for the fragments.
     * @return - List of fragments.
     */
    public List<Fragment> getFragments(final String backupManagerId, final String backupName, final String agentId) {
        final Path backupFolder = getBackupPath(backupManagerId, backupName, agentId);
        if (exists(backupFolder)) {
            return readObjectsFromFiles(backupFolder)
                    .stream()
                    .map(PersistedFragmentInformation::getInfo)
                    .map(this::toFragment)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    protected int getMaximumDepth() {
        return DEPTH_OF_FILES;
    }

    @Override
    protected List<Version<PersistedFragmentInformation>> getVersions() {
        return versions;
    }

    private Fragment toFragment(final FragmentInformation fragmentInformation) {
        return Fragment
                .newBuilder()
                .setFragmentId(fragmentInformation.getFragmentId())
                .setSizeInBytes(fragmentInformation.getSizeInBytes())
                .setVersion(fragmentInformation.getVersion())
                .putAllCustomInformation(fragmentInformation.getCustomInformation())
                .build();
    }

    private Path getBackupPath(final String backupManagerId, final String backupName, final String agentId) {
        return dataLocation.resolve(backupManagerId).resolve(backupName).resolve(agentId);
    }

    /**
     * Sets data location
     * @param dataLocation where to store backup files.
     */
    @Value("${backup.location}")
    public void setDataLocation(final String dataLocation) {
        if (dataLocation != null && !dataLocation.isEmpty()) {
            this.dataLocation = Paths.get(dataLocation);
        }
    }

}
