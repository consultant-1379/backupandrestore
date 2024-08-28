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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.operation;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerInformation;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;

/**
 * Patch to update a backupManager in the configuration.
 */
public class UpdateBackupManagerPatch extends BRMConfigurationPatch {

    private final BackupManager backupManager;

    /**
     * Creates Patch.
     * @param backupManager with new information.
     * @param pathToBackupManager where to update.
     */
    protected UpdateBackupManagerPatch(final BackupManager backupManager, final String pathToBackupManager) {
        super(PatchOperation.REPLACE, pathToBackupManager);
        this.backupManager = backupManager;
    }

    @Override
    public List<PatchOperationJson> getJsonOfOperations() {
        final List<SftpServerInformation> listSftpServer = getSftpServers();
        final PatchOperationJson patchSftpServerOperation = createOperationJson(path + "/sftp-server", listSftpServer);
        patchSftpServerOperation.setOperation(listSftpServer.isEmpty() ?
                PatchOperation.ADD.getStringRepresentation() :
                    PatchOperation.REPLACE.getStringRepresentation() );
        return Arrays.asList(
                createOperationJson(path + "/backup-domain", getValidValue(backupManager.getBackupDomain())),
                createOperationJson(path + "/backup-type", getValidValue(backupManager.getBackupType())),
                patchSftpServerOperation
                );
    }

    private List<SftpServerInformation> getSftpServers() {
        return backupManager.getSftpServers().stream().map(SftpServerInformation::new).collect(Collectors.toList());
    }

    private String getValidValue(final String value) {
        return Optional.ofNullable(value).orElse("");
    }

}
