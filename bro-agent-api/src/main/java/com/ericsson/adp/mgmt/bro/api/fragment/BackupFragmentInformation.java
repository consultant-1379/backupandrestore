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
package com.ericsson.adp.mgmt.bro.api.fragment;

import java.util.Optional;

/**
 * Holds the fragment information, as well as where it is located.
 */
public class BackupFragmentInformation extends FragmentInformation {

    private String backupFilePath;
    private Optional<String> customMetadataFilePath = Optional.empty();

    /**
     * @return the backupFilePath
     */
    public String getBackupFilePath() {
        return backupFilePath;
    }

    /**
     * @param backupFilePath
     *            the backupFilePath to set
     */
    public void setBackupFilePath(final String backupFilePath) {
        this.backupFilePath = backupFilePath;
    }

    /**
     * @return the customMetadataFilePath
     */
    public Optional<String> getCustomMetadataFilePath() {
        return customMetadataFilePath;
    }

    /**
     * @param customMetadataFilePath
     *            the customMetadataFilePath to set
     */
    public void setCustomMetadataFilePath(final Optional<String> customMetadataFilePath) {
        this.customMetadataFilePath = customMetadataFilePath;
    }

}
