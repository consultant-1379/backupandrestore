/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.exception;

/**
 * Represents failing to find a specific SFTP Server.
 */
public class SftpServerNotFoundException extends NotFoundException {

    private static final long serialVersionUID = -4339969411121453424L;

    /**
     * Creates exception.
     * @param sftpServerIndex id of action.
     */
    public SftpServerNotFoundException(final String sftpServerIndex) {
        super("SftpServer with index <" + sftpServerIndex + "> not found");
    }

    /**
     * Creates exception.
     * @param sftpServerName the sftp-server-name
     * @param backupManager the backup manager
     */
    public SftpServerNotFoundException(final String sftpServerName, final String backupManager) {
        super("SftpServer <" + sftpServerName + "> not found in backup manager <" + backupManager + ">.");
    }
}
