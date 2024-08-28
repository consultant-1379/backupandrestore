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
package com.ericsson.adp.mgmt.backupandrestore.rest.backup;

/**
 * JSON request to update a backup.
 */
public class UpdateBackupRequest {

    private String userLabel;

    public String getUserLabel() {
        return userLabel;
    }

    public void setUserLabel(final String userLabel) {
        this.userLabel = userLabel;
    }

}
