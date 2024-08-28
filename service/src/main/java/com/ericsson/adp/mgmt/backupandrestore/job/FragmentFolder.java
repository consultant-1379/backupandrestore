/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job;

import java.nio.file.Path;

/**
 * Holds information about the location of the root of a fragment as well as the folder that data and custom metadata files are to be stored under.
 *
 */
public class FragmentFolder {

    private static final String CUSTOM_METADATA_FOLDER = "customMetadata/";
    private static final String DATA_FILE_FOLDER = "data/";
    private static final String METADATA_FILE = "Fragment.json";

    private final Path rootFolder;

    /**
     * Creates a fragmentFolder, this provides a centralized location to define the data and custom metadata folders. They will be relative to the
     * supplied root folder.
     *
     * @param rootFolder
     *            The root folder path that a backup should be stored in.
     */
    public FragmentFolder(final Path rootFolder) {
        this.rootFolder = rootFolder;
    }

    public Path getCustomMetadataFileFolder() {
        return rootFolder.resolve(CUSTOM_METADATA_FOLDER);
    }

    public Path getDataFileFolder() {
        return rootFolder.resolve(DATA_FILE_FOLDER);
    }

    public Path getRootFolder() {
        return rootFolder;
    }

    public Path getMetadataFile() {
        return rootFolder.resolve(METADATA_FILE);
    }

}
