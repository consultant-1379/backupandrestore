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
package com.ericsson.adp.mgmt.backupandrestore.backup;

import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.persist.FileService;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistedFragmentInformation;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.bro.api.fragment.FragmentInformation;
import com.ericsson.adp.mgmt.data.Metadata;

/**
 * Parses Metadata and store fragment info to file
 */
@Service
public class BackupMetadataWriter extends FileService<PersistedFragmentInformation> {

    private final List<Version<PersistedFragmentInformation>> versions = List.of(getDefaultVersion(
        s -> jsonService.parseJsonString(s, PersistedFragmentInformation.class),
        p -> p.getFileName().toString().endsWith(JSON_EXTENSION))
    );

    /**
     * @param fragmentFolder folder in which the fragment is stored
     * @param metadata chunks
     */
    public void storeFragment(final FragmentFolder fragmentFolder, final Metadata metadata) {
        try {
            // When we write a new fragment, we just write the inner FragmentInformation of the PersistedFragmentInformation,
            // versioning is only relevant on the read side
            writeFile(
                fragmentFolder.getRootFolder(),
                fragmentFolder.getMetadataFile(),
                jsonService.toJsonString(getFragmentInformation(metadata)).getBytes(),
                getLatestVersion()
            );
        } catch (final Exception e) {
            throw new BackupServiceException("Exception while backup of metadata:", e);
        }
    }

    @Override
    protected List<Version<PersistedFragmentInformation>> getVersions() {
        return versions;
    }

    private FragmentInformation getFragmentInformation(final Metadata metadata) {
        final FragmentInformation fragmentInformation = new FragmentInformation();

        fragmentInformation.setFragmentId(metadata.getFragment().getFragmentId());
        fragmentInformation.setVersion(metadata.getFragment().getVersion());
        fragmentInformation.setSizeInBytes(metadata.getFragment().getSizeInBytes());
        fragmentInformation.setCustomInformation(metadata.getFragment().getCustomInformationMap());

        return fragmentInformation;
    }

}
