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
package com.ericsson.adp.mgmt.backupandrestore.util;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_MANAGER_CONFIG_BACKUP_FOLDER;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;

import com.ericsson.adp.mgmt.backupandrestore.persist.FileService;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;

/**
 * Responsible for writing/reading objects to/from files.
 * @param <T> Persisted Class
 */
public abstract class OrchestratorDataFileService<T extends Versioned<T>> extends FileService<T> {
    private static final Path DEFAULT_BACKUP_MANAGERS_LOCATION = Paths.get(System.getProperty("java.io.tmpdir"), BACKUP_MANAGER_CONFIG_BACKUP_FOLDER)
        .toAbsolutePath().normalize();
    protected Path backupManagersLocation = DEFAULT_BACKUP_MANAGERS_LOCATION;

    /**
     * Sets where backupManagers' information is stored.
     * @param backupManagersLocation where to store information.
     */
    @Value("${backup.managers.location}")
    public void setBackupManagersLocation(final String backupManagersLocation) {
        if (!backupManagersLocation.isEmpty()) {
            this.backupManagersLocation = Paths.get(backupManagersLocation);
        }
    }

    /**
     * Reserves support space if not created
    public void createDummyFile() {
    }
     */

    /**
     * Sets where Dummy file information is stored.
     * @param dummyLocation where to store information.
     */
    @Value("${backup.dummy.location}")
    public void setDummyLocation(final String dummyLocation) {
        if (dummyLocation != null && !dummyLocation.isEmpty()) {
            setReservedSpace(Paths.get(dummyLocation));
        }
    }
}
